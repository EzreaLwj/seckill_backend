package com.ezreal.common.model.response.avtivity;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class MultiSeckillActivitiesResponse {
    private List<SeckillActivitiesResponse> seckillActivitiesResponseList;

    private Long total;

    private Long version;

}
