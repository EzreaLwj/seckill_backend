package com.ezreal.activity.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ezreal.common.model.domain.SeckillActivity;
import com.ezreal.common.model.query.SeckillActivityQuery;
import com.ezreal.common.model.request.PublishSeckillActivityRequest;
import com.ezreal.common.model.response.avtivity.MultiSeckillActivitiesResponse;
import com.ezreal.common.model.response.avtivity.SeckillActivitiesResponse;
import com.ezreal.common.BaseResponse;
import com.ezreal.common.SuccessCode;


/**
* @author Ezreal
* @description 针对表【seckill_activity(秒杀活动表)】的数据库操作Service
* @createDate 2023-01-02 10:35:44
*/
public interface SeckillActivityService extends IService<SeckillActivity> {
    BaseResponse<SuccessCode> publishSeckillActivity(Long userId, PublishSeckillActivityRequest seckillActivity);

    BaseResponse<SuccessCode> updateSeckillActivity(Long userId, Long activityId, PublishSeckillActivityRequest publishSeckillActivityRequest);

    BaseResponse<MultiSeckillActivitiesResponse> getSeckillActivities(Long userId, SeckillActivityQuery seckillActivityQuery);

    BaseResponse<SeckillActivitiesResponse> getSeckillActivity(Long userId, Long activityId);

    BaseResponse<SuccessCode> onlineSeckillActivity(Long userId, Long activityId);

    BaseResponse<SuccessCode> offlineSeckillActivity(Long userId, Long activityId);

    BaseResponse<MultiSeckillActivitiesResponse> getOnlineSeckillActivities(Long userId, SeckillActivityQuery seckillActivityQuery);
}
