package com.ezreal.order.service;

import com.ezreal.common.model.domain.StockDeduction;

public interface GoodStockDeductionService {
    /**
     * 库存扣减
     *
     * @param stockDeduction 库存扣减信息
     */
    boolean decreaseItemStock(StockDeduction stockDeduction);

    /**
     * 库存恢复
     *
     * @param stockDeduction 库存恢复信息
     */
    boolean increaseItemStock(StockDeduction stockDeduction);
}
