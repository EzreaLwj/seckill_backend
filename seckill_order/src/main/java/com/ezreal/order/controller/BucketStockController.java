package com.ezreal.order.controller;

import com.ezreal.common.BaseResponse;
import com.ezreal.common.SuccessCode;
import com.ezreal.common.model.request.SeckillBucketsArrangementRequest;
import com.ezreal.common.model.response.bucket.MultiSeckillBucketResponse;

import com.ezreal.order.service.SeckillBucketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/bucket")
@ConditionalOnProperty(name = "place_order_type", havingValue = "buckets", matchIfMissing = true)
public class BucketStockController {

    @Autowired
    private SeckillBucketService seckillBucketService;

    @PostMapping("/items/{activityId}/{itemId}")
    public BaseResponse<SuccessCode> arrangeStockBuckets(@RequestHeader("TokenInfo") Long userId,
                                                         @PathVariable Long itemId,
                                                         @PathVariable Long activityId,
                                                         @RequestBody SeckillBucketsArrangementRequest seckillBucketsArrangementRequest) {
        return seckillBucketService.arrangeStockBuckets(userId, activityId, itemId, seckillBucketsArrangementRequest);
    }

    /**
     * 查询某件商品的分桶数量
     *
     * @param userId
     * @param itemId
     * @return
     */
    @GetMapping("/items/{itemId}")
    public BaseResponse<MultiSeckillBucketResponse> getBuckets(@RequestHeader("TokenInfo") Long userId,
                                                               @PathVariable Long itemId) {

        return seckillBucketService.getStockBucketsSummary(userId, itemId);
    }


}
