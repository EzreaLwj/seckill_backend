package com.ezreal.common.model.builder;

import com.ezreal.common.model.domain.SeckillOrder;
import com.ezreal.common.model.mq.task.PlaceOrderTask;
import com.ezreal.common.model.request.SeckillPlaceOrderRequest;
import com.ezreal.common.model.response.order.SeckillOrderResponse;
import org.springframework.beans.BeanUtils;

public class SeckillOrderBuilder {
    public static SeckillOrder toDomain(SeckillPlaceOrderRequest seckillPlaceOrderRequest) {
        if (seckillPlaceOrderRequest == null) {
            return null;
        }

        SeckillOrder seckillOrder = new SeckillOrder();
        BeanUtils.copyProperties(seckillPlaceOrderRequest, seckillOrder);
        return seckillOrder;
    }

    public static SeckillOrder toDomain(PlaceOrderTask placeOrderTask) {
        if (placeOrderTask == null) {
            return null;
        }

        SeckillOrder seckillOrder = new SeckillOrder();
        BeanUtils.copyProperties(placeOrderTask, seckillOrder);
        return seckillOrder;
    }


    public static SeckillOrderResponse toResponse(SeckillOrder seckillOrder) {
        if (seckillOrder == null) {
            return null;
        }

        SeckillOrderResponse seckillOrderResponse = new SeckillOrderResponse();
        BeanUtils.copyProperties(seckillOrder, seckillOrderResponse);
        return seckillOrderResponse;
    }
}
