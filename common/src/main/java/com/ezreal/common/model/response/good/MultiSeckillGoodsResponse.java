package com.ezreal.common.model.response.good;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class MultiSeckillGoodsResponse {
    private List<SeckillGoodResponse> seckillGoodResponses;

    private Long total;

    private Long version;
}
