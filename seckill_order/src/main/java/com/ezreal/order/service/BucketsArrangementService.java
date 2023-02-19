package com.ezreal.order.service;

import com.ezreal.common.model.response.bucket.MultiSeckillBucketResponse;

public interface BucketsArrangementService {
    /**
     * 获取某件商品的分桶库存
     * @param itemId
     * @return
     */
    MultiSeckillBucketResponse queryStockBucketsSummary(Long itemId);

    /**
     * 安排库存分桶
     * @param itemId
     * @param totalStocksAmount
     * @param bucketsQuantity
     * @param arrangementMode
     */
    void arrangeStockBuckets(Long itemId, Integer totalStocksAmount, Integer bucketsQuantity, Integer arrangementMode);
}
