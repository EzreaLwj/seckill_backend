package com.ezreal.common.model.builder;


import com.ezreal.common.model.domain.SeckillActivity;
import com.ezreal.common.model.request.PublishSeckillActivityRequest;
import com.ezreal.common.model.response.avtivity.SeckillActivitiesResponse;
import org.springframework.beans.BeanUtils;


public class SeckillActivityBuilder {
    /**
     * 请求转实体类
     * @param publishSeckillActivityRequest
     * @return
     */
    public static SeckillActivity toDomain(PublishSeckillActivityRequest publishSeckillActivityRequest) {
        if (publishSeckillActivityRequest == null) {
            return null;
        }

        SeckillActivity seckillActivity = new SeckillActivity();
        BeanUtils.copyProperties(publishSeckillActivityRequest, seckillActivity);

        return seckillActivity;
    }

    public static SeckillActivitiesResponse toSeckillActivitiesResponse(SeckillActivity seckillActivity) {
        if (seckillActivity == null) {
            return null;
        }
        SeckillActivitiesResponse seckillActivitiesResponse = new SeckillActivitiesResponse();
        BeanUtils.copyProperties(seckillActivity, seckillActivitiesResponse);
        return seckillActivitiesResponse;
    }
}
