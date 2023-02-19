package com.ezreal.common.model.builder;

import com.ezreal.common.model.domain.SeckillBucket;
import com.ezreal.common.model.response.bucket.SeckillBucketResponse;
import org.springframework.beans.BeanUtils;

public class SeckillBucketBuilder {
    public static SeckillBucketResponse toResponse(SeckillBucket seckillBucket) {
        if (seckillBucket == null) {
            return null;
        }
        SeckillBucketResponse stockBucketResponse = new SeckillBucketResponse();
        BeanUtils.copyProperties(seckillBucket, stockBucketResponse);
        return stockBucketResponse;
    }
}
