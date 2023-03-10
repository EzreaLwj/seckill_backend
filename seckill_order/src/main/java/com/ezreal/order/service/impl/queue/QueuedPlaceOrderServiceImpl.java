package com.ezreal.order.service.impl.queue;

import com.alibaba.fastjson.JSON;
import com.ezreal.clients.SeckillActivityClient;
import com.ezreal.clients.SeckillGoodClient;
import com.ezreal.common.BaseResponse;
import com.ezreal.common.ErrorCode;
import com.ezreal.common.ResultUtils;
import com.ezreal.common.cache.redis.DistributedCacheService;
import com.ezreal.common.model.builder.PlaceOrderTaskBuilder;
import com.ezreal.common.model.builder.SeckillOrderBuilder;
import com.ezreal.common.model.domain.SeckillOrder;
import com.ezreal.common.model.domain.StockDeduction;
import com.ezreal.common.model.enums.OrderTaskStatus;
import com.ezreal.common.model.enums.SeckillOrderStatus;
import com.ezreal.common.model.result.OrderTaskSubmitResult;
import com.ezreal.common.model.mq.task.PlaceOrderTask;
import com.ezreal.common.model.request.SeckillPlaceOrderRequest;
import com.ezreal.common.model.response.avtivity.SeckillActivitiesResponse;
import com.ezreal.common.model.response.good.SeckillGoodResponse;
import com.ezreal.common.model.response.order.SeckillOrderMessageResponse;
import com.ezreal.exception.BusinessException;
import com.ezreal.order.mapper.SeckillOrderMapper;
import com.ezreal.order.service.GoodStockDeductionService;
import com.ezreal.order.service.PlaceOrderService;
import com.ezreal.order.service.PlaceOrderTaskService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;

import java.util.concurrent.TimeUnit;

import static com.ezreal.common.ErrorCode.PLACE_ORDER_FAILED;

@Service
@Slf4j
@ConditionalOnProperty(name = "place_order_type", havingValue = "queued")
public class QueuedPlaceOrderServiceImpl implements PlaceOrderService {

    private Logger logger = LoggerFactory.getLogger(QueuedPlaceOrderServiceImpl.class);
    private static final String PLACE_ORDER_TASK_ORDER_ID_KEY = "PLACE_ORDER_TASK_ORDER_ID_KEY_";
    @Resource
    private SeckillActivityClient seckillActivityClient;

    @Resource
    private SeckillGoodClient seckillGoodClient;

    @Autowired
    private PlaceOrderTaskService placeOrderTaskService;

    @Autowired
    private GoodStockDeductionService goodStockDeductionService;

    @Autowired
    private SeckillOrderMapper seckillOrderMapper;

    @Resource
    private DistributedCacheService redisCacheService;


    @Override
    public BaseResponse<SeckillOrderMessageResponse> doPlaceOrder(Long userId, SeckillPlaceOrderRequest seckillPlaceOrderRequest) {
        logger.info("submitOrderTask|??????????????????|{}", JSON.toJSONString(seckillPlaceOrderRequest));
        if (seckillPlaceOrderRequest == null) {
            return ResultUtils.error(ErrorCode.INVALID_PARAMS);
        }

        // ????????????
        Long activityId = seckillPlaceOrderRequest.getActivityId();
        BaseResponse<SeckillActivitiesResponse> seckillActivitiesResponse = seckillActivityClient.getSeckillActivity(userId, activityId);
        if (!isSeckillActivityValid(seckillActivitiesResponse, activityId)) {
            logger.info("??????????????????|{}, {}", userId, activityId);
            return ResultUtils.error(PLACE_ORDER_FAILED.getCode(), PLACE_ORDER_FAILED.getMessage());
        }

        // ????????????
        Long itemId = seckillPlaceOrderRequest.getItemId();
        BaseResponse<SeckillGoodResponse> seckillGoodResponse = seckillGoodClient.getSeckillGood(userId, activityId, itemId);
        if (!isSeckillGoodValid(seckillGoodResponse, activityId, itemId)) {
            logger.info("??????????????????|{}, {}", userId, activityId);
            return ResultUtils.error(PLACE_ORDER_FAILED.getCode(), PLACE_ORDER_FAILED.getMessage());
        }

        // ????????????id
        String placeOrderTaskId = generatePlaceOrderTaskId(userId, seckillPlaceOrderRequest.getItemId());
        PlaceOrderTask placeOrderTask = PlaceOrderTaskBuilder.with(userId, seckillPlaceOrderRequest);
        placeOrderTask.setPlaceOrderTaskId(placeOrderTaskId);

        logger.info("doPlaceOrder|????????????|{},{},{}", userId, placeOrderTaskId, JSON.toJSONString(placeOrderTask));
        OrderTaskSubmitResult orderTaskSubmitResult = placeOrderTaskService.submit(placeOrderTask);
        logger.info("placeOrder|??????????????????|{},{},{}", userId, placeOrderTaskId, JSON.toJSONString(placeOrderTask));
        if (!orderTaskSubmitResult.isSuccess()) {
            logger.info("placeOrder|????????????????????????|{},{}", userId, seckillPlaceOrderRequest.getActivityId());
            return ResultUtils.error(orderTaskSubmitResult.getCode(), orderTaskSubmitResult.getMessage());
        }
        logger.info("placeOrder|????????????????????????|{},{}", userId, placeOrderTaskId);
        return ResultUtils.success(SeckillOrderMessageResponse.ok(placeOrderTaskId));
    }

    /**
     * ????????????id
     *
     * @param userId
     * @param itemId
     * @return
     */
    private String generatePlaceOrderTaskId(Long userId, Long itemId) {
        String toEncrypt = userId + "_" + itemId;
        return DigestUtils.md5DigestAsHex(toEncrypt.getBytes());
    }

    /**
     * ??????????????????
     *
     * @param placeOrderTask
     */
    @Transactional
    public void handlePlaceOrderTask(PlaceOrderTask placeOrderTask) {
        Long userId = placeOrderTask.getUserId();

        SeckillGoodResponse seckillGoodResponse = seckillGoodClient
                .getSeckillGood(userId, placeOrderTask.getActivityId(), placeOrderTask.getItemId())
                .getData();
        // ?????? ?????????
        SeckillOrder seckillOrder = SeckillOrderBuilder.toDomain(placeOrderTask);
        seckillOrder.setItemTitle(seckillGoodResponse.getItemTitle());
        seckillOrder.setFlashPrice(seckillGoodResponse.getFlashPrice());
        seckillOrder.setUserId(userId);

        StockDeduction stockDeduction = new StockDeduction()
                .setItemId(placeOrderTask.getItemId())
                .setUserId(userId)
                .setQuantity(seckillOrder.getQuantity());
        Long orderId = null;
        try {

            // ??????????????????
            boolean isDecreaseStock = goodStockDeductionService.decreaseItemStock(stockDeduction);
            if (!isDecreaseStock) {
                logger.info("??????????????????|{}, {}", userId, JSON.toJSONString(placeOrderTask));
                return;
            }

            // ????????????
            logger.info("placeOrder|??????|{},{}", userId, JSON.toJSONString(placeOrderTask));
            seckillOrder.setStatus(SeckillOrderStatus.CREATED.getCode());

            boolean isSuccessSave = seckillOrderMapper.insert(seckillOrder) > 0;
            if (!isSuccessSave) {
                logger.info("??????????????????|{},{}", userId, JSON.toJSONString(seckillOrder));
                throw new BusinessException(PLACE_ORDER_FAILED);
            }
            orderId = seckillOrder.getId();
            redisCacheService.put(PLACE_ORDER_TASK_ORDER_ID_KEY + placeOrderTask.getPlaceOrderTaskId(), orderId, 24, TimeUnit.HOURS);
            placeOrderTaskService.updateTaskHandleResult(placeOrderTask.getPlaceOrderTaskId(), true);
            logger.info("?????????????????????|{},{}", userId, JSON.toJSONString(seckillOrder));
        } catch (Exception e) {
            // ????????????????????????
            logger.error("????????????|{},{}", userId, JSON.toJSONString(placeOrderTask), e);
            placeOrderTaskService.updateTaskHandleResult(placeOrderTask.getPlaceOrderTaskId(), false);
            throw new BusinessException(ErrorCode.PLACE_ORDER_FAILED);
        }
    }

    /**
     * ??????????????????
     * @param userId
     * @param itemId
     * @param placeOrderTaskId
     * @return
     */
    public BaseResponse<SeckillOrderMessageResponse> getPlaceOrderResult(Long userId, Long itemId, String placeOrderTaskId) {
        String orderTaskId = generatePlaceOrderTaskId(userId, itemId);
        if (!orderTaskId.equals(placeOrderTaskId)) {
            logger.info("??????ID??????|{}, {}, {}", userId, itemId, placeOrderTaskId);
            return ResultUtils.error(ErrorCode.PLACE_ORDER_TASK_ID_INVALID);
        }

        // ?????????????????????
        OrderTaskStatus taskStatus = placeOrderTaskService.getTaskStatus(placeOrderTaskId);
        if (taskStatus == null) {
            logger.info("??????????????????|{}", placeOrderTaskId);
            return ResultUtils.error(ErrorCode.PLACE_ORDER_TASK_ID_INVALID);
        }

        if (!taskStatus.getStatus().equals(OrderTaskStatus.SUCCESS.getStatus())) {
            logger.info("????????????????????????|{}", placeOrderTaskId);
            return ResultUtils.success(SeckillOrderMessageResponse.ok().setCode(taskStatus.getStatus()));
        }
        // ???????????????
        Long orderId = redisCacheService.getLong(PLACE_ORDER_TASK_ORDER_ID_KEY + placeOrderTaskId);
        if (orderId == null) {
            logger.info("??????id????????????|{}, {}, {}", userId, itemId, placeOrderTaskId);
            return null;
        }
        // ????????????
        SeckillOrderMessageResponse seckillOrderMessageResponse = SeckillOrderMessageResponse.ok().setOrderId(orderId)
                .setPlaceOrderTaskId(orderTaskId)
                .setCode(OrderTaskStatus.SUCCESS.getStatus());
        return ResultUtils.success(seckillOrderMessageResponse);
    }


    private boolean isSeckillActivityValid(BaseResponse<SeckillActivitiesResponse> baseResponse, Long activityId) {
        int code = baseResponse.getCode();

        // ??????????????????
        if (code >= 100 && code <= 1000) {
            log.info(baseResponse.getMessage() + "{}", activityId);
            return false;
        }

        SeckillActivitiesResponse seckillActivitiesResponse = baseResponse.getData();
        if (seckillActivitiesResponse == null) {
            log.info("???????????????|{}", activityId);
            return false;
        }

        return seckillActivitiesResponse.isAllowPlaceOrderOrNot();
    }

    private boolean isSeckillGoodValid(BaseResponse<SeckillGoodResponse> baseResponse, Long activityId, Long itemId) {
        int code = baseResponse.getCode();

        // ??????????????????
        if (code >= 100 && code <= 1000) {
            log.info(baseResponse.getMessage() + "|{}, {}", activityId, itemId);
            return false;
        }

        SeckillGoodResponse seckillGoodResponse = baseResponse.getData();
        if (seckillGoodResponse == null) {
            log.info("???????????????|{}, {}", activityId, itemId);
            return false;
        }

        return seckillGoodResponse.isAllowPlaceOrderOrNot();
    }

}
