package com.ezreal.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ezreal.common.BaseResponse;
import com.ezreal.common.SuccessCode;
import com.ezreal.common.model.domain.SeckillBucket;
import com.ezreal.common.model.request.SeckillBucketsArrangementRequest;
import com.ezreal.common.model.response.bucket.MultiSeckillBucketResponse;

/**
* @author Ezreal
* @description 针对表【seckill_bucket(秒杀品库存分桶表)】的数据库操作Service
* @createDate 2023-01-17 21:52:11
*/
public interface SeckillBucketService extends IService<SeckillBucket> {

    /**
     * 获取某件商品的分桶库存
     * @param userId
     * @param itemId
     * @return
     */
    BaseResponse<MultiSeckillBucketResponse> getStockBucketsSummary(Long userId, Long itemId);

    /**
     * 为某件商品实现分桶
     * @param userId
     * @param itemId
     * @param seckillBucketsArrangementRequest
     * @return
     */
    BaseResponse<SuccessCode> arrangeStockBuckets(Long userId, Long activityId,  Long itemId, SeckillBucketsArrangementRequest seckillBucketsArrangementRequest);
}
