package com.ezreal.order.service;

import com.ezreal.common.model.enums.OrderTaskStatus;
import com.ezreal.common.model.result.OrderTaskSubmitResult;
import com.ezreal.common.model.mq.task.PlaceOrderTask;

public interface PlaceOrderTaskService {
    /**
     * 提交订单任务
     * @param placeOrderTask
     * @return
     */
    OrderTaskSubmitResult submit(PlaceOrderTask placeOrderTask);

    void updateTaskHandleResult(String placeOrderTaskId, boolean result);

    OrderTaskStatus getTaskStatus(String placeOrderTaskId);
}
