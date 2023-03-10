package com.ezreal.order.service.impl.queue;

import com.alibaba.fastjson.JSON;
import com.ezreal.common.ErrorCode;
import com.ezreal.common.cache.redis.DistributedCacheService;
import com.ezreal.common.lock.redisson.DistributedLock;
import com.ezreal.common.lock.redisson.DistributedLockFactoryService;
import com.ezreal.common.model.cahce.GoodStockCache;
import com.ezreal.common.model.enums.OrderTaskStatus;
import com.ezreal.common.model.result.OrderTaskSubmitResult;
import com.ezreal.order.mq.service.OrderTaskPostService;
import com.ezreal.common.model.mq.task.PlaceOrderTask;
import com.ezreal.order.service.GoodStockCacheService;
import com.ezreal.order.service.PlaceOrderTaskService;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import static com.ezreal.common.ErrorCode.*;

@Service
@ConditionalOnProperty(name = "place_order_type", havingValue = "queued")
public class QueueOrderTaskServiceImpl implements PlaceOrderTaskService {

    private static final Logger logger = LoggerFactory.getLogger(QueueOrderTaskServiceImpl.class);
    private static final String PLACE_ORDER_TASK_ID_KEY = "PLACE_ORDER_TASK_ID_KEY_";
    private static final String PLACE_ORDER_TASK_AVAILABLE_TOKENS_KEY = "PLACE_ORDER_TASK_AVAILABLE_TOKENS_KEY_";
    private static final String LOCK_REFRESH_LATEST_AVAILABLE_TOKENS_KEY = "LOCK_REFRESH_LATEST_AVAILABLE_TOKENS_KEY_";
    private static final Cache<Long, Integer> localAvailableTokens = CacheBuilder.newBuilder()
            .concurrencyLevel(5)
            .initialCapacity(2)
            .expireAfterWrite(20, TimeUnit.MILLISECONDS)
            .build();
    private static final DefaultRedisScript<Long> TAKE_ORDER_TOKEN_LUA;

    static {
        TAKE_ORDER_TOKEN_LUA = new DefaultRedisScript<>();
        TAKE_ORDER_TOKEN_LUA.setLocation(new ClassPathResource("/lua/take_order_token.lua"));
        TAKE_ORDER_TOKEN_LUA.setResultType(Long.class);
    }

    @Resource
    private DistributedCacheService redisCacheService;

    @Autowired
    private GoodStockCacheService goodStockCacheService;

    @Resource
    private DistributedLockFactoryService distributedLockFactoryService;

    @Autowired
    private OrderTaskPostService orderTaskPostService;

    /**
     * ??????????????????
     * @param placeOrderTask
     * @return
     */
    @Override
    public OrderTaskSubmitResult submit(PlaceOrderTask placeOrderTask) {
        logger.info("submitOrderTask|??????????????????|{}", JSON.toJSONString(placeOrderTask));
        if (placeOrderTask == null) {
            return OrderTaskSubmitResult.failed(INVALID_PARAMS);
        }
        String taskKey = getOrderTaskKey(placeOrderTask.getPlaceOrderTaskId());

        //todo ????????????????????????????????
        Integer taskIdSubmittedResult = redisCacheService.getObject(taskKey, Integer.class);
        if (taskIdSubmittedResult != null) {
            return OrderTaskSubmitResult.failed(ErrorCode.REDUNDANT_SUBMIT);
        }

        // ?????????????????? PlaceOrderToken
        Integer availableOrderTokens = getAvailableOrderTokens(placeOrderTask.getItemId());
        // ?????????????????????0
        if (availableOrderTokens == null || availableOrderTokens == 0) {
            return OrderTaskSubmitResult.failed(ORDER_TOKENS_NOT_AVAILABLE);
        }

        // ????????????
        if (!takeOrRecoverToken(placeOrderTask)) {
            logger.info("submitOrderTask|??????????????????|{},{}", placeOrderTask.getUserId(), placeOrderTask.getPlaceOrderTaskId());
            return OrderTaskSubmitResult.failed(ORDER_TOKENS_NOT_AVAILABLE);
        }

        // ???????????????????????????
        boolean isSuccessPost = orderTaskPostService.post(placeOrderTask);
        if (!isSuccessPost) {
            logger.info("submitOrderTask|????????????????????????|{},{}", placeOrderTask.getUserId(), placeOrderTask.getPlaceOrderTaskId());
            return OrderTaskSubmitResult.failed(ORDER_TASK_SUBMIT_FAILED);
        }

        // ??????????????????
        redisCacheService.put(taskKey, OrderTaskStatus.SUBMITTED.getStatus(), 24, TimeUnit.HOURS);
        logger.info("submitOrderTask|????????????????????????|{},{}", placeOrderTask.getUserId(), placeOrderTask.getPlaceOrderTaskId());
        return OrderTaskSubmitResult.ok();
    }

    // ????????????
    private boolean takeOrRecoverToken(PlaceOrderTask placeOrderTask) {
        ArrayList<String> keys = new ArrayList<>();
        keys.add(getGoodAvailableTokensKey(placeOrderTask.getItemId()));

        for (int i = 0; i < 3; i++) {
            Long result = redisCacheService.getRedisTemplate().execute(TAKE_ORDER_TOKEN_LUA, keys);
            if (result == null) {
                return false;
            }
            // ????????????
            if (result == -1L) {
                logger.info("?????????0|{}", JSON.toJSONString(placeOrderTask));
                return false;
            }
            // ??????????????????????????????????????????
            if (result == -100L) {
                refreshLatestAvailableTokens(placeOrderTask.getItemId());
                continue;
            }
            return result == 1L;
        }
        return false;
    }

    /**
     * ?????????????????????
     * @param placeOrderTaskId
     * @param result
     */
    @Override
    public void updateTaskHandleResult(String placeOrderTaskId, boolean result) {
        if (StringUtils.isEmpty(placeOrderTaskId)) {
            return;
        }
        String orderTaskKey = getOrderTaskKey(placeOrderTaskId);
        Integer taskStatus = redisCacheService.getInteger(orderTaskKey);
        if (taskStatus == null || taskStatus != 0) {
            return;
        }

        redisCacheService.put(orderTaskKey, result ? OrderTaskStatus.SUCCESS.getStatus() : OrderTaskStatus.FAILED.getStatus());
    }

    /**
     * ?????????????????????
     * @param placeOrderTaskId
     * @return
     */
    @Override
    public OrderTaskStatus getTaskStatus(String placeOrderTaskId) {
        String orderTaskKey = getOrderTaskKey(placeOrderTaskId);
        Integer taskStatus = redisCacheService.getInteger(orderTaskKey);
        return OrderTaskStatus.findBy(taskStatus);
    }

    // ???????????????orderToken
    private Integer getAvailableOrderTokens(Long itemId) {
        Integer localAvailableToken = localAvailableTokens.getIfPresent(itemId);
        if (localAvailableToken != null) {
            logger.info("??????????????????|{}", itemId);
            return localAvailableToken;
        }

        return refreshLocalAvailableTokens(itemId);
    }

    // ???????????????????????????????????????
    private synchronized Integer refreshLocalAvailableTokens(Long itemId) {
        // ???????????????????????????
        Integer localAvailableToken = localAvailableTokens.getIfPresent(itemId);
        if (localAvailableToken != null) {
            logger.info("??????????????????|{}", itemId);
            return localAvailableToken;
        }

        String goodAvailableTokensKey = getGoodAvailableTokensKey(itemId);
        Integer goodAvailableTokensInRedis = redisCacheService.getInteger(goodAvailableTokensKey);
        if (goodAvailableTokensInRedis != null) {
            logger.info("??????????????????|{}", itemId);
            localAvailableTokens.put(itemId, goodAvailableTokensInRedis);
            return goodAvailableTokensInRedis;
        }

        return refreshLatestAvailableTokens(itemId);
    }

    // ???????????????????????????
    private Integer refreshLatestAvailableTokens(Long itemId) {
        DistributedLock distributedLock = distributedLockFactoryService.getDistributedLock(getRefreshTokensLockKey(itemId));
        try {

            boolean isSuccessLock = distributedLock.tryLock(500, 1000, TimeUnit.MILLISECONDS);
            if (!isSuccessLock) {
                return null;
            }

            GoodStockCache availableItemStock = goodStockCacheService.getAvailableItemStock(-1L, itemId);
            if (availableItemStock == null || availableItemStock.getAvailableStock() == null || !availableItemStock.isSuccess()) {
                logger.info("???????????????|{}", itemId);
                return null;
            }

            Integer availableOrderTokens = (int) Math.ceil(availableItemStock.getAvailableStock() * 1.5);
            // ???????????????????????????
            redisCacheService.put(getGoodAvailableTokensKey(itemId), availableOrderTokens, 24, TimeUnit.HOURS);
            // ??????????????????
            localAvailableTokens.put(itemId, availableOrderTokens);
            return availableOrderTokens;
        } catch (InterruptedException e) {
            logger.info("??????tokens??????|{}", itemId, e);
            return null;
        }
    }

    // ????????????id
    private String getOrderTaskKey(String placeOrderTaskId) {
        return PLACE_ORDER_TASK_ID_KEY + placeOrderTaskId;
    }

    // ???redis???????????????token???key
    private String getGoodAvailableTokensKey(Long itemId) {
        return PLACE_ORDER_TASK_AVAILABLE_TOKENS_KEY + itemId;
    }

    // ????????????
    private String getRefreshTokensLockKey(Long itemId) {
        return LOCK_REFRESH_LATEST_AVAILABLE_TOKENS_KEY + itemId;
    }
}
