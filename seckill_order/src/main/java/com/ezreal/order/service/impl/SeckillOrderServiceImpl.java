package com.ezreal.order.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ezreal.common.BaseResponse;
import com.ezreal.common.ErrorCode;
import com.ezreal.common.ResultUtils;
import com.ezreal.common.SuccessCode;
import com.ezreal.common.lock.redisson.DistributedLock;
import com.ezreal.common.lock.redisson.DistributedLockFactoryService;
import com.ezreal.common.model.builder.SeckillOrderBuilder;
import com.ezreal.common.model.domain.SeckillOrder;
import com.ezreal.common.model.domain.StockDeduction;
import com.ezreal.common.model.enums.SeckillOrderStatus;
import com.ezreal.common.model.query.SeckillOrderQuery;
import com.ezreal.common.model.request.SeckillPlaceOrderRequest;
import com.ezreal.common.model.response.order.MultiSeckillOrdersResponse;
import com.ezreal.common.model.response.order.SeckillOrderMessageResponse;
import com.ezreal.common.model.response.order.SeckillOrderResponse;
import com.ezreal.exception.BusinessException;
import com.ezreal.order.mapper.SeckillOrderMapper;
import com.ezreal.order.service.GoodStockCacheService;
import com.ezreal.order.service.GoodStockDeductionService;
import com.ezreal.order.service.PlaceOrderService;
import com.ezreal.order.service.SeckillOrderService;
import com.ezreal.order.service.impl.queue.QueuedPlaceOrderServiceImpl;
import com.sun.org.apache.bcel.internal.generic.I2F;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.ezreal.common.ErrorCode.PLACE_ORDER_FAILED;

/**
 * @author Ezreal
 * @description 针对表【seckill_order(秒杀订单表)】的数据库操作Service实现
 * @createDate 2023-01-06 13:13:56
 */
@Service
@Slf4j
public class SeckillOrderServiceImpl extends ServiceImpl<SeckillOrderMapper, SeckillOrder>
        implements SeckillOrderService {

    private static final Logger logger = LoggerFactory.getLogger(SeckillOrderServiceImpl.class);

    @Resource
    private DistributedLockFactoryService distributedLockFactoryService;


    @Autowired
    private GoodStockCacheService goodStockCacheService;

    @Autowired
    private GoodStockDeductionService goodStockDeductionService;

    @Autowired
    private SeckillOrderMapper seckillOrderMapper;

    @Autowired
    private PlaceOrderService placeOrderService;

    @Override
    @Transactional
    public BaseResponse<SeckillOrderMessageResponse> placeOrder(Long userId, SeckillPlaceOrderRequest seckillPlaceOrderRequest) {
        logger.info("下单|{}, {}", userId, JSON.toJSONString(seckillPlaceOrderRequest));
        if (userId == null || seckillPlaceOrderRequest == null || !seckillPlaceOrderRequest.validateParams()) {
            throw new BusinessException(ErrorCode.INVALID_PARAMS);
        }
        DistributedLock distributedLock = distributedLockFactoryService.getDistributedLock(String.valueOf(userId));

        try {
            boolean isLockSuccess = distributedLock.tryLock(500, 3000, TimeUnit.MILLISECONDS);
            if (!isLockSuccess) {
                logger.info("下单失败|{}", userId);
                return ResultUtils.error(ErrorCode.FREQUENTLY_ERROR);
            }
            BaseResponse<SeckillOrderMessageResponse> seckillOrderMessageResponseBaseResponse = placeOrderService.doPlaceOrder(userId, seckillPlaceOrderRequest);
            return seckillOrderMessageResponseBaseResponse;
        } catch (InterruptedException e) {
            logger.error("placeOrder|下单失败|{},{}", userId, JSON.toJSONString(seckillPlaceOrderRequest), e);
            return ResultUtils.error(PLACE_ORDER_FAILED);
        }
    }

    /**
     * 取消订单
     *
     * @param userId
     * @param orderId
     * @return
     */
    @Override
    @Transactional
    public BaseResponse<SuccessCode> cancelOrder(Long userId, Long orderId) {
        logger.info("取消订单|{}, {}", userId, orderId);
        if (userId == null || orderId == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAMS);
        }

        SeckillOrder seckillOrder = seckillOrderMapper.selectById(orderId);
        if (seckillOrder == null) {
            return ResultUtils.error(ErrorCode.ORDER_NOT_FOUND);
        }

        if (!seckillOrder.getUserId().equals(userId)) {
            return ResultUtils.error(ErrorCode.ORDER_CANCEL_FAILED);
        }

        if (Objects.equals(seckillOrder.getStatus(), SeckillOrderStatus.CANCELED.getCode())) {
            return ResultUtils.success(SuccessCode.ORDER_CANCEL_SUCCESS);
        }

        seckillOrder.setStatus(SeckillOrderStatus.CANCELED.getCode());
        boolean isUpdateSuccess = updateById(seckillOrder);
        if (!isUpdateSuccess) {
            logger.info("订单取消失败|{}, {}", userId, orderId);
            throw new BusinessException(ErrorCode.ORDER_CANCEL_FAILED);
        }

        StockDeduction stockDeduction = new StockDeduction()
                .setItemId(seckillOrder.getItemId())
                .setUserId(userId)
                .setQuantity(seckillOrder.getQuantity());

        // 先恢复数据库，再恢复缓存
        boolean isIncreaseSuccess = goodStockDeductionService.increaseItemStock(stockDeduction);
        if (!isIncreaseSuccess) {
            logger.info("预库存恢复失败|{}, {}", userId, orderId);
            throw new BusinessException(ErrorCode.ORDER_CANCEL_FAILED);
        }

        isIncreaseSuccess = goodStockCacheService.increaseGoodStock(stockDeduction);
        if (!isIncreaseSuccess) {
            logger.info("预库存恢复失败|{}, {}", userId, orderId);
            throw new BusinessException(ErrorCode.ORDER_CANCEL_FAILED);
        }

        logger.info("cancelOrder|订单取消成功|{}", orderId);
        return ResultUtils.success(SuccessCode.ORDER_CANCEL_SUCCESS);
    }

    /**
     * 获取自己的订单
     *
     * @param userId
     * @param seckillOrderQuery
     * @return
     */
    @Override
    public BaseResponse<MultiSeckillOrdersResponse> getMyOrders(Long userId, SeckillOrderQuery seckillOrderQuery) {
        if (userId == null || seckillOrderQuery == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAMS);
        }

        if (seckillOrderQuery.getPageNumber() == null || seckillOrderQuery.getPageSize() == null) {
            seckillOrderQuery.buildParams();
        }

        Page<SeckillOrder> seckillOrderPage = new Page<>(seckillOrderQuery.getPageNumber(), seckillOrderQuery.getPageSize());
        LambdaQueryWrapper<SeckillOrder> seckillOrderLambdaQueryWrapper = new LambdaQueryWrapper<>();
        seckillOrderLambdaQueryWrapper.eq(!StringUtils.isEmpty(seckillOrderQuery.getKeyword()),
                        SeckillOrder::getItemTitle,
                        seckillOrderQuery.getKeyword())
                .eq(seckillOrderQuery.getStatus() != null,
                        SeckillOrder::getStatus,
                        seckillOrderQuery.getStatus())
                .eq(SeckillOrder::getUserId, userId);

        seckillOrderMapper.selectPage(seckillOrderPage, seckillOrderLambdaQueryWrapper);
        List<SeckillOrderResponse> seckillOrderResponses = seckillOrderPage.getRecords()
                .stream()
                .map(SeckillOrderBuilder::toResponse)
                .collect(Collectors.toList());
        logger.info("查询自己的订单|{}, {}", userId, JSON.toJSONString(seckillOrderQuery));
        MultiSeckillOrdersResponse multiSeckillOrdersResponse = new MultiSeckillOrdersResponse()
                .setSeckillOrderResponses(seckillOrderResponses)
                .setTotal(seckillOrderPage.getTotal());
        return ResultUtils.success(multiSeckillOrdersResponse);
    }

    /**
     * 获取单个订单
     *
     * @param userId
     * @param orderId
     * @return
     */
    @Override
    public BaseResponse<SeckillOrderResponse> getOrder(Long userId, Long orderId) {
        if (userId == null || orderId == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAMS);
        }
        logger.info("查询订单|{}, {}", userId, orderId);
        SeckillOrder seckillOrder = seckillOrderMapper.selectById(orderId);
        SeckillOrderResponse seckillOrderResponse = SeckillOrderBuilder.toResponse(seckillOrder);
        return ResultUtils.success(seckillOrderResponse);
    }

    /**
     * 根据条件查询所有订单
     *
     * @param userId
     * @param seckillOrderQuery
     * @return
     */
    @Override
    public BaseResponse<MultiSeckillOrdersResponse> getOrders(Long userId, SeckillOrderQuery seckillOrderQuery) {
        if (userId == null || seckillOrderQuery == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAMS);
        }

        if (seckillOrderQuery.getPageNumber() == null || seckillOrderQuery.getPageSize() == null) {
            seckillOrderQuery.buildParams();
        }

        Page<SeckillOrder> seckillOrderPage = new Page<>(seckillOrderQuery.getPageNumber(), seckillOrderQuery.getPageSize());
        LambdaQueryWrapper<SeckillOrder> seckillOrderLambdaQueryWrapper = new LambdaQueryWrapper<>();
        seckillOrderLambdaQueryWrapper.eq(!StringUtils.isEmpty(seckillOrderQuery.getKeyword()),
                        SeckillOrder::getItemTitle,
                        seckillOrderQuery.getKeyword())
                .eq(seckillOrderQuery.getStatus() != null,
                        SeckillOrder::getStatus,
                        seckillOrderQuery.getStatus())
                .eq(seckillOrderQuery.getCheckId() != null,
                        SeckillOrder::getUserId,
                        seckillOrderQuery.getCheckId());

        seckillOrderMapper.selectPage(seckillOrderPage, seckillOrderLambdaQueryWrapper);
        List<SeckillOrderResponse> seckillOrderResponses = seckillOrderPage.getRecords()
                .stream()
                .map(SeckillOrderBuilder::toResponse)
                .collect(Collectors.toList());
        logger.info("查询订单列表|{}, {}", userId, JSON.toJSONString(seckillOrderQuery));
        MultiSeckillOrdersResponse multiSeckillOrdersResponse = new MultiSeckillOrdersResponse()
                .setSeckillOrderResponses(seckillOrderResponses)
                .setTotal(seckillOrderPage.getTotal());
        return ResultUtils.success(multiSeckillOrdersResponse);
    }

    @Override
    public BaseResponse<SeckillOrderMessageResponse> getPlaceOrderResult(Long userId, Long itemId, String placeOrderTaskId) {
        if (userId == null || itemId == null || StringUtils.isEmpty(placeOrderTaskId)) {
            return ResultUtils.error(ErrorCode.INVALID_PARAMS);
        }

        if (placeOrderService instanceof QueuedPlaceOrderServiceImpl) {
            BaseResponse<SeckillOrderMessageResponse> placeOrderResult = ((QueuedPlaceOrderServiceImpl) placeOrderService).getPlaceOrderResult(userId, itemId, placeOrderTaskId);
            return placeOrderResult;
        }
        return ResultUtils.error(ErrorCode.ORDER_TYPE_NOT_SUPPORT);
    }
}