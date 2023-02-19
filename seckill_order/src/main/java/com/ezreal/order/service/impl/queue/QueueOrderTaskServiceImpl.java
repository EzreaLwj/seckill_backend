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
     * 提交下单任务
     * @param placeOrderTask
     * @return
     */
    @Override
    public OrderTaskSubmitResult submit(PlaceOrderTask placeOrderTask) {
        logger.info("submitOrderTask|提交下单任务|{}", JSON.toJSONString(placeOrderTask));
        if (placeOrderTask == null) {
            return OrderTaskSubmitResult.failed(INVALID_PARAMS);
        }
        String taskKey = getOrderTaskKey(placeOrderTask.getPlaceOrderTaskId());

        //todo 验证是否重复提交订单??
        Integer taskIdSubmittedResult = redisCacheService.getObject(taskKey, Integer.class);
        if (taskIdSubmittedResult != null) {
            return OrderTaskSubmitResult.failed(ErrorCode.REDUNDANT_SUBMIT);
        }

        // 获取下单许可 PlaceOrderToken
        Integer availableOrderTokens = getAvailableOrderTokens(placeOrderTask.getItemId());
        // 判断数量是否为0
        if (availableOrderTokens == null || availableOrderTokens == 0) {
            return OrderTaskSubmitResult.failed(ORDER_TOKENS_NOT_AVAILABLE);
        }

        // 扣减库存
        if (!takeOrRecoverToken(placeOrderTask)) {
            logger.info("submitOrderTask|库存扣减失败|{},{}", placeOrderTask.getUserId(), placeOrderTask.getPlaceOrderTaskId());
            return OrderTaskSubmitResult.failed(ORDER_TOKENS_NOT_AVAILABLE);
        }

        // 发送扣减库存的信息
        boolean isSuccessPost = orderTaskPostService.post(placeOrderTask);
        if (!isSuccessPost) {
            logger.info("submitOrderTask|下单任务提交失败|{},{}", placeOrderTask.getUserId(), placeOrderTask.getPlaceOrderTaskId());
            return OrderTaskSubmitResult.failed(ORDER_TASK_SUBMIT_FAILED);
        }

        // 存储任务标识
        redisCacheService.put(taskKey, OrderTaskStatus.SUBMITTED.getStatus(), 24, TimeUnit.HOURS);
        logger.info("submitOrderTask|下单任务提交成功|{},{}", placeOrderTask.getUserId(), placeOrderTask.getPlaceOrderTaskId());
        return OrderTaskSubmitResult.ok();
    }

    // 扣减许可
    private boolean takeOrRecoverToken(PlaceOrderTask placeOrderTask) {
        ArrayList<String> keys = new ArrayList<>();
        keys.add(getGoodAvailableTokensKey(placeOrderTask.getItemId()));

        for (int i = 0; i < 3; i++) {
            Long result = redisCacheService.getRedisTemplate().execute(TAKE_ORDER_TOKEN_LUA, keys);
            if (result == null) {
                return false;
            }
            // 没有库存
            if (result == -1L) {
                logger.info("库存为0|{}", JSON.toJSONString(placeOrderTask));
                return false;
            }
            // 数据在缓存中不存在，先去更新
            if (result == -100L) {
                refreshLatestAvailableTokens(placeOrderTask.getItemId());
                continue;
            }
            return result == 1L;
        }
        return false;
    }

    /**
     * 更新任务的状态
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
     * 获取任务的状态
     * @param placeOrderTaskId
     * @return
     */
    @Override
    public OrderTaskStatus getTaskStatus(String placeOrderTaskId) {
        String orderTaskKey = getOrderTaskKey(placeOrderTaskId);
        Integer taskStatus = redisCacheService.getInteger(orderTaskKey);
        return OrderTaskStatus.findBy(taskStatus);
    }

    // 从本地获取orderToken
    private Integer getAvailableOrderTokens(Long itemId) {
        Integer localAvailableToken = localAvailableTokens.getIfPresent(itemId);
        if (localAvailableToken != null) {
            logger.info("本地缓存命中|{}", itemId);
            return localAvailableToken;
        }

        return refreshLocalAvailableTokens(itemId);
    }

    // 从本地获取远程的缓存并更新
    private synchronized Integer refreshLocalAvailableTokens(Long itemId) {
        // 再次从本地缓存获取
        Integer localAvailableToken = localAvailableTokens.getIfPresent(itemId);
        if (localAvailableToken != null) {
            logger.info("本地缓存命中|{}", itemId);
            return localAvailableToken;
        }

        String goodAvailableTokensKey = getGoodAvailableTokensKey(itemId);
        Integer goodAvailableTokensInRedis = redisCacheService.getInteger(goodAvailableTokensKey);
        if (goodAvailableTokensInRedis != null) {
            logger.info("远程缓存命中|{}", itemId);
            localAvailableTokens.put(itemId, goodAvailableTokensInRedis);
            return goodAvailableTokensInRedis;
        }

        return refreshLatestAvailableTokens(itemId);
    }

    // 获取最新的库存信息
    private Integer refreshLatestAvailableTokens(Long itemId) {
        DistributedLock distributedLock = distributedLockFactoryService.getDistributedLock(getRefreshTokensLockKey(itemId));
        try {

            boolean isSuccessLock = distributedLock.tryLock(500, 1000, TimeUnit.MILLISECONDS);
            if (!isSuccessLock) {
                return null;
            }

            GoodStockCache availableItemStock = goodStockCacheService.getAvailableItemStock(-1L, itemId);
            if (availableItemStock == null || availableItemStock.getAvailableStock() == null || !availableItemStock.isSuccess()) {
                logger.info("库存不存在|{}", itemId);
                return null;
            }

            Integer availableOrderTokens = (int) Math.ceil(availableItemStock.getAvailableStock() * 1.5);
            // 更新远程的下单许可
            redisCacheService.put(getGoodAvailableTokensKey(itemId), availableOrderTokens, 24, TimeUnit.HOURS);
            // 存入本地缓存
            localAvailableTokens.put(itemId, availableOrderTokens);
            return availableOrderTokens;
        } catch (InterruptedException e) {
            logger.info("刷新tokens失败|{}", itemId, e);
            return null;
        }
    }

    // 获取任务id
    private String getOrderTaskKey(String placeOrderTaskId) {
        return PLACE_ORDER_TASK_ID_KEY + placeOrderTaskId;
    }

    // 在redis中获取可用token的key
    private String getGoodAvailableTokensKey(Long itemId) {
        return PLACE_ORDER_TASK_AVAILABLE_TOKENS_KEY + itemId;
    }

    // 分布式锁
    private String getRefreshTokensLockKey(Long itemId) {
        return LOCK_REFRESH_LATEST_AVAILABLE_TOKENS_KEY + itemId;
    }
}
