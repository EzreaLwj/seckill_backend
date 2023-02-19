package com.ezreal.common.model.response.bucket;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class SeckillBucketResponse {
    private Integer serialNo;
    private Integer totalStocksAmount;
    private Integer availableStocksAmount;
    private Integer status;
}
