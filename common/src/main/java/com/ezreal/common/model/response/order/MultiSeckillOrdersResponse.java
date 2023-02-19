package com.ezreal.common.model.response.order;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class MultiSeckillOrdersResponse {

    private List<SeckillOrderResponse> seckillOrderResponses;

    private Long total;
}
