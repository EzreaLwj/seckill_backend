package com.ezreal.common.model.response.bucket;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;
@Data
@Accessors(chain = true)
public class MultiSeckillBucketResponse {
    private Integer totalStocksAmount;
    private Integer availableStocksAmount;
    private List<SeckillBucketResponse> buckets;
}
