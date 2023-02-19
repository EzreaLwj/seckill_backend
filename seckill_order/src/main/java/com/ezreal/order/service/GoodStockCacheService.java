package com.ezreal.order.service;

import com.ezreal.common.model.cahce.GoodStockCache;
import com.ezreal.common.model.domain.StockDeduction;

public interface GoodStockCacheService {
    /**
     * 预热调度
     * @param activity
     * @param itemId
     * @return
     */
    boolean alignItemStocks(Long activity ,Long itemId);

    /**
     * 减少库存
     * @param stockDeduction
     * @return
     */
    boolean decreaseGoodStock(StockDeduction stockDeduction);

    /**
     * 增加库存
     * @param stockDeduction
     * @return
     */
    boolean increaseGoodStock(StockDeduction stockDeduction);

    /**
     * 获取可用的秒杀库存
     * @param userId
     * @param itemId
     * @return
     */
    GoodStockCache getAvailableItemStock(Long userId, Long itemId);
}
