package com.ezreal.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ezreal.common.BaseResponse;
import com.ezreal.common.SuccessCode;
import com.ezreal.common.model.domain.SeckillOrder;
import com.ezreal.common.model.query.SeckillOrderQuery;
import com.ezreal.common.model.request.SeckillPlaceOrderRequest;
import com.ezreal.common.model.response.order.MultiSeckillOrdersResponse;
import com.ezreal.common.model.response.order.SeckillOrderMessageResponse;
import com.ezreal.common.model.response.order.SeckillOrderResponse;

/**
 * @author Ezreal
 * @description 针对表【seckill_order(秒杀订单表)】的数据库操作Service
 * @createDate 2023-01-06 13:13:56
 */
public interface SeckillOrderService extends IService<SeckillOrder> {

    /**
     * 下单
     * @param userId
     * @param seckillPlaceOrderRequest
     * @return
     */
    BaseResponse<SeckillOrderMessageResponse> placeOrder(Long userId, SeckillPlaceOrderRequest seckillPlaceOrderRequest);

    /**
     * 取消订单
     * @param userId
     * @param orderId
     * @return
     */
    BaseResponse<SuccessCode> cancelOrder(Long userId, Long orderId);

    /**
     * 获取我所有订单
     * @param userId
     * @param seckillOrderQuery
     * @return
     */
    BaseResponse<MultiSeckillOrdersResponse> getMyOrders(Long userId, SeckillOrderQuery seckillOrderQuery);

    /**
     * 获取单个订单
     * @param userId
     * @param orderId
     * @return
     */
    BaseResponse<SeckillOrderResponse> getOrder(Long userId, Long orderId);

    /**
     * 获取所有订单
     * @param userId
     * @param seckillOrderQuery
     * @return
     */
    BaseResponse<MultiSeckillOrdersResponse> getOrders(Long userId, SeckillOrderQuery seckillOrderQuery);

    /**
     * 获取订单结果
     * @param userId
     * @param itemId
     * @param placeOrderTaskId
     * @return
     */
    BaseResponse<SeckillOrderMessageResponse> getPlaceOrderResult(Long userId, Long itemId, String placeOrderTaskId);
}
