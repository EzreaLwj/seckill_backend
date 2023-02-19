package com.ezreal.common.model.request;

import lombok.Data;

@Data
public class SeckillPlaceOrderRequest {
    /**
     * 商品id
     */
    private Long itemId;

    /**
     * 活动id
     */
    private Long activityId;

    /**
     * 商品数量
     */
    private Integer quantity;

    /**
     * 总金额
     */
    private Long totalAmount;

    public boolean validateParams() {
        return itemId != null && activityId != null && quantity != null && quantity > 0;
    }
}
