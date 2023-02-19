package com.ezreal.common.model.builder;


import com.ezreal.common.model.mq.task.PlaceOrderTask;
import com.ezreal.common.model.request.SeckillPlaceOrderRequest;
import org.springframework.beans.BeanUtils;

public class PlaceOrderTaskBuilder {
    public static PlaceOrderTask with(Long userId, SeckillPlaceOrderRequest seckillPlaceOrderRequest) {
        if (seckillPlaceOrderRequest == null) {
            return null;
        }
        PlaceOrderTask placeOrderTask = new PlaceOrderTask();
        BeanUtils.copyProperties(seckillPlaceOrderRequest, placeOrderTask);
        placeOrderTask.setUserId(userId);
        return placeOrderTask;
    }
}
