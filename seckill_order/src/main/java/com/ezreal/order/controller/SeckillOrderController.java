package com.ezreal.order.controller;

import com.ezreal.common.BaseResponse;
import com.ezreal.common.SuccessCode;
import com.ezreal.common.model.domain.SeckillOrder;
import com.ezreal.common.model.query.SeckillOrderQuery;
import com.ezreal.common.model.request.SeckillPlaceOrderRequest;
import com.ezreal.common.model.response.order.MultiSeckillOrdersResponse;
import com.ezreal.common.model.response.order.SeckillOrderMessageResponse;
import com.ezreal.common.model.response.order.SeckillOrderResponse;
import com.ezreal.order.service.SeckillOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/order")
public class SeckillOrderController {

    @Autowired
    private SeckillOrderService seckillOrderService;

    /**
     * 生成订单
     * @param userId
     * @param seckillPlaceOrderRequest
     * @return
     */
    @PostMapping
    public BaseResponse<SeckillOrderMessageResponse> placeOrder(@RequestHeader("TokenInfo") Long userId,
                                                                @RequestBody SeckillPlaceOrderRequest seckillPlaceOrderRequest) {

        return seckillOrderService.placeOrder(userId, seckillPlaceOrderRequest);
    }

    /**
     * 删除某个订单
     * @param userId
     * @param orderId
     * @return
     */
    @DeleteMapping
    public BaseResponse<SuccessCode> cancelOrder(@RequestHeader("TokenInfo") Long userId,
                                            @RequestParam("orderId") Long orderId) {
        return seckillOrderService.cancelOrder(userId, orderId);
    }

    /**
     * 获取自己的订单
     * @param userId
     * @param pageSize
     * @param pageNumber
     * @param keyword
     * @return
     */
    @GetMapping("/my")
    public BaseResponse<MultiSeckillOrdersResponse> getMyOrders(@RequestHeader("TokenInfo") Long userId,
                                                                @RequestParam Integer pageSize,
                                                                @RequestParam Integer pageNumber,
                                                                @RequestParam(required = false) String keyword) {
        SeckillOrderQuery seckillOrderQuery = new SeckillOrderQuery().setKeyword(keyword)
                .setPageSize(pageSize)
                .setPageNumber(pageNumber);

        return seckillOrderService.getMyOrders(userId, seckillOrderQuery);
    }

    /**
     * 获取单个订单
     * @param userId
     * @param orderId
     * @return
     */
    @GetMapping("/{orderId}")
    public BaseResponse<SeckillOrderResponse> getOrder(@RequestHeader("TokenInfo") Long userId,
                                                       @PathVariable Long orderId) {
        return seckillOrderService.getOrder(userId, orderId);
    }

    /**
     * 获取订单列表
     * @param userId
     * @param seckillOrderQuery
     * @return
     */
    @GetMapping("/list")
    public BaseResponse<MultiSeckillOrdersResponse> getOrders(@RequestHeader("TokenInfo") Long userId,
                                                              SeckillOrderQuery seckillOrderQuery) {
        return seckillOrderService.getOrders(userId, seckillOrderQuery);
    }

    /**
     * 获取订单结果
     * @param userId
     * @param itemId
     * @param placeOrderTaskId
     * @return
     */
    @GetMapping("/result/{itemId}/{placeOrderTaskId}")
    public BaseResponse<SeckillOrderMessageResponse> getPlaceOrderResult(@RequestHeader("TokenInfo") Long userId,
                                                                         @PathVariable Long itemId,
                                                                         @PathVariable String placeOrderTaskId) {
        return seckillOrderService.getPlaceOrderResult(userId, itemId, placeOrderTaskId);
    }
}
