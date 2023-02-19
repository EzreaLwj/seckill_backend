package com.ezreal.common.model.request;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class SeckillBucketsArrangementRequest {
    /**
     * 库存总数
     */
    private Integer totalStocksAmount;
    /**
     * 分桶数量
     */
    private Integer bucketsQuantity;
    /**
     * 分桶模式
     */
    private Integer arrangementMode;
}
