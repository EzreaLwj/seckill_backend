package com.ezreal.clients;

import com.ezreal.common.BaseResponse;
import com.ezreal.common.model.response.avtivity.SeckillActivitiesResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient("seckill-activity")
public interface SeckillActivityClient {
    @GetMapping("/api/activity/list/{activityId}")
    BaseResponse<SeckillActivitiesResponse> getSeckillActivity(@RequestHeader("TokenInfo") Long userId,
                                                               @PathVariable Long activityId);
}
