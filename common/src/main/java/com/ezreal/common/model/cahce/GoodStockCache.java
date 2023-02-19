package com.ezreal.common.model.cahce;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class GoodStockCache {

    /**
     * 是否存在
     */
    protected boolean exist;

    /**
     * 可用库存
     */
    private Integer availableStock;

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 稍后尝试
     */
    private boolean later;

    public GoodStockCache with(Integer availableStock) {
        this.exist = true;
        this.availableStock = availableStock;
        this.success = true;
        return this;
    }

    public GoodStockCache tryLater() {
        this.later = true;
        this.success = false;
        return this;
    }

    public GoodStockCache notExist() {
        this.exist = false;
        this.success = true;
        return this;
    }
}
