package com.ezreal.order.service;

import com.ezreal.common.BaseResponse;
import com.ezreal.common.model.request.SeckillPlaceOrderRequest;
import com.ezreal.common.model.response.order.SeckillOrderMessageResponse;

public interface PlaceOrderService {
     /**
      * 下单方式
      * @param userId
      * @param seckillPlaceOrderRequest
      * @return
      */
     BaseResponse<SeckillOrderMessageResponse> doPlaceOrder(Long userId, SeckillPlaceOrderRequest seckillPlaceOrderRequest);
}
