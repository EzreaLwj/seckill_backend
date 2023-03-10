package com.ezreal.order.service.impl.normal;

import com.alibaba.fastjson.JSON;
import com.ezreal.clients.SeckillActivityClient;
import com.ezreal.clients.SeckillGoodClient;
import com.ezreal.common.BaseResponse;
import com.ezreal.common.ErrorCode;
import com.ezreal.common.ResultUtils;
import com.ezreal.common.model.builder.SeckillOrderBuilder;
import com.ezreal.common.model.domain.SeckillOrder;
import com.ezreal.common.model.domain.StockDeduction;
import com.ezreal.common.model.enums.SeckillOrderStatus;
import com.ezreal.common.model.request.SeckillPlaceOrderRequest;
import com.ezreal.common.model.response.avtivity.SeckillActivitiesResponse;
import com.ezreal.common.model.response.good.SeckillGoodResponse;
import com.ezreal.common.model.response.order.SeckillOrderMessageResponse;
import com.ezreal.exception.BusinessException;
import com.ezreal.order.mapper.SeckillOrderMapper;
import com.ezreal.order.service.GoodStockCacheService;
import com.ezreal.order.service.GoodStockDeductionService;
import com.ezreal.order.service.PlaceOrderService;
import com.ezreal.order.utils.MultiPlaceOrderTypesCondition;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;


import javax.annotation.Resource;

import static com.ezreal.common.ErrorCode.PLACE_ORDER_FAILED;

@Service
@Slf4j
@Conditional(MultiPlaceOrderTypesCondition.class)
public class NormalPlaceOrderService implements PlaceOrderService {

    private Logger logger = LoggerFactory.getLogger(NormalPlaceOrderService.class);

    @Autowired
    private GoodStockCacheService goodStockCacheService;

    @Resource
    private SeckillActivityClient seckillActivityClient;

    @Resource
    private SeckillGoodClient seckillGoodClient;

    @Autowired
    private GoodStockDeductionService goodStockDeductionService;

    @Autowired
    private SeckillOrderMapper seckillOrderMapper;

    @Override
    public BaseResponse<SeckillOrderMessageResponse> doPlaceOrder(Long userId, SeckillPlaceOrderRequest seckillPlaceOrderRequest) {
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

        // ?????? ?????????
        SeckillGoodResponse seckillGoodResponseData = seckillGoodResponse.getData();
        SeckillOrder seckillOrder = SeckillOrderBuilder.toDomain(seckillPlaceOrderRequest);
        seckillOrder.setItemTitle(seckillGoodResponseData.getItemTitle());
        seckillOrder.setFlashPrice(seckillGoodResponseData.getFlashPrice());
        seckillOrder.setUserId(userId);

        StockDeduction stockDeduction = new StockDeduction()
                .setItemId(itemId)
                .setUserId(userId)
                .setQuantity(seckillOrder.getQuantity());
        boolean isPreDecreaseStock = false;
        boolean isDecreaseStock = false;
        Long orderId = null;
        try {
            // ????????????
            isPreDecreaseStock = goodStockCacheService.decreaseGoodStock(stockDeduction);
            if (!isPreDecreaseStock) {
                logger.info("??????????????????|{}, {}, {}", activityId, itemId, JSON.toJSONString(seckillPlaceOrderRequest));
                return ResultUtils.error(PLACE_ORDER_FAILED.getCode(), PLACE_ORDER_FAILED.getMessage());
            }

            // ??????????????????
            isDecreaseStock = goodStockDeductionService.decreaseItemStock(stockDeduction);
            if (!isDecreaseStock) {
                logger.info("??????????????????|{}, {}, {}", activityId, itemId, JSON.toJSONString(seckillPlaceOrderRequest));
                return ResultUtils.error(PLACE_ORDER_FAILED.getCode(), PLACE_ORDER_FAILED.getMessage());
            }

            // ????????????
            logger.info("placeOrder|??????|{},{}", userId, JSON.toJSONString(seckillOrder));
            seckillOrder.setStatus(SeckillOrderStatus.CREATED.getCode());

            boolean isSuccessSave = seckillOrderMapper.insert(seckillOrder) > 0;
            if (!isSuccessSave) {
                logger.info("??????????????????|{},{}", userId, JSON.toJSONString(seckillOrder));
                throw new BusinessException(PLACE_ORDER_FAILED);
            }
            orderId = seckillOrder.getId();
            logger.info("?????????????????????|{},{}", userId, JSON.toJSONString(seckillOrder));
        } catch (Exception e) {
            // ????????????????????????
            if (isPreDecreaseStock) {
                boolean recoverStockSuccess = goodStockCacheService.increaseGoodStock(stockDeduction);
                if (!recoverStockSuccess) {
                    logger.info("?????????????????????|{}, {}", userId, JSON.toJSONString(seckillOrder));
                    logger.error("????????????|{},{}", userId, JSON.toJSONString(seckillPlaceOrderRequest), e);
                    throw new BusinessException(ErrorCode.PLACE_ORDER_FAILED);
                }
            }
        }
        return ResultUtils.success(SeckillOrderMessageResponse.ok(orderId));
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
