package com.ezreal.common.model.domain;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class StockDeduction {
    /**
     * 商品id
     */
    private Long itemId;
    /**
     * 商品数量
     */
    private Integer quantity;
    /**
     * 用户id
     */
    private Long userId;

    private Integer serialNo;

    public boolean validate() {
        return itemId != null && quantity != null && quantity > 0 && userId != null;
    }
}
