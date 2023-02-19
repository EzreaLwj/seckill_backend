package com.ezreal.order.mq.service;

import com.ezreal.common.model.mq.task.PlaceOrderTask;

public interface OrderTaskPostService {
    boolean post(PlaceOrderTask placeOrderTask);
}
