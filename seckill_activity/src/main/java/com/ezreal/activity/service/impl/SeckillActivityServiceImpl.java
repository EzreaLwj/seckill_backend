package com.ezreal.activity.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ezreal.common.model.builder.SeckillActivityBuilder;
import com.ezreal.common.model.domain.SeckillActivity;
import com.ezreal.common.model.enums.SeckillActivityStatus;
import com.ezreal.common.model.query.SeckillActivityQuery;
import com.ezreal.common.model.request.PublishSeckillActivityRequest;
import com.ezreal.common.model.response.avtivity.MultiSeckillActivitiesResponse;
import com.ezreal.common.model.response.avtivity.SeckillActivitiesResponse;
import com.ezreal.activity.service.SeckillActivityService;
import com.ezreal.activity.mapper.SeckillActivityMapper;
import com.ezreal.common.BaseResponse;
import com.ezreal.common.ErrorCode;
import com.ezreal.common.ResultUtils;
import com.ezreal.common.SuccessCode;
import com.ezreal.exception.BusinessException;
import com.ezreal.exception.SQLException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Ezreal
 * @description 针对表【seckill_activity(秒杀活动表)】的数据库操作Service实现
 * @createDate 2023-01-02 10:35:44
 */
@Service
@Slf4j
public class SeckillActivityServiceImpl extends ServiceImpl<SeckillActivityMapper, SeckillActivity>
        implements SeckillActivityService {

    @Autowired
    private SeckillActivityMapper seckillActivityMapper;

    @Override
    public BaseResponse<SuccessCode> publishSeckillActivity(Long userId,
                                                            PublishSeckillActivityRequest publishSeckillActivityRequest) {
        if (publishSeckillActivityRequest == null || !publishSeckillActivityRequest.validate()) {
            throw new BusinessException(ErrorCode.INVALID_PARAMS);
        }

        SeckillActivity seckillActivity = SeckillActivityBuilder.toDomain(publishSeckillActivityRequest);
        int insert = seckillActivityMapper.insert(seckillActivity);

        if (insert == 0) {
            log.error("发布活动失败");
            throw new SQLException(ErrorCode.SQL_INSERT);
        }

        log.info("发布秒杀活动|{}", userId);

        return ResultUtils.success(SuccessCode.ACTIVITY_PUBLISH_SUCCESS);
    }

    @Override
    public BaseResponse<SuccessCode> updateSeckillActivity(Long userId,
                                                           Long activityId,
                                                           PublishSeckillActivityRequest publishSeckillActivityRequest) {
        if (activityId == null || publishSeckillActivityRequest == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAMS);
        }
        SeckillActivity seckillActivity = SeckillActivityBuilder.toDomain(publishSeckillActivityRequest);
        seckillActivity.setActivityId(activityId);

        if (seckillActivity.getActivityId() == null) {
            seckillActivityMapper.insert(seckillActivity);
        }

        seckillActivityMapper.updateById(seckillActivity);
        log.info("修改秒杀活动信息|{}", userId);

        return ResultUtils.success(SuccessCode.ACTIVITY_UPDATE_SUCCESS);
    }

    @Override
    public BaseResponse<MultiSeckillActivitiesResponse> getSeckillActivities(Long userId, SeckillActivityQuery seckillActivityQuery) {
        if (seckillActivityQuery.getPageNum() == null || seckillActivityQuery.getPageSize() == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAMS);
        }
        LambdaQueryWrapper<SeckillActivity> seckillActivityLambdaQueryWrapper = new LambdaQueryWrapper<>();

        Page<SeckillActivity> seckillActivityPage = new Page<>(seckillActivityQuery.getPageNum(),
                seckillActivityQuery.getPageSize());

        seckillActivityLambdaQueryWrapper.like(!StringUtils.isEmpty(seckillActivityQuery.getKeyword()),
                SeckillActivity::getActivityName,
                seckillActivityQuery.getKeyword());

        seckillActivityMapper.selectPage(seckillActivityPage, seckillActivityLambdaQueryWrapper);

        List<SeckillActivity> records = seckillActivityPage.getRecords();
        List<SeckillActivitiesResponse> seckillActivitiesResponseList = records.stream()
                .map(SeckillActivityBuilder::toSeckillActivitiesResponse)
                .collect(Collectors.toList());

        MultiSeckillActivitiesResponse multiSeckillActivitiesResponse = new MultiSeckillActivitiesResponse()
                .setSeckillActivitiesResponseList(seckillActivitiesResponseList)
                .setTotal(seckillActivityPage.getTotal());

        return ResultUtils.success(multiSeckillActivitiesResponse);
    }

    @Override
    public BaseResponse<SeckillActivitiesResponse> getSeckillActivity(Long userId, Long activityId) {
        if (activityId == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAMS);
        }

        SeckillActivity seckillActivity = seckillActivityMapper.selectById(activityId);
        SeckillActivitiesResponse seckillActivitiesResponse = SeckillActivityBuilder.toSeckillActivitiesResponse(seckillActivity);
        return ResultUtils.success(seckillActivitiesResponse);
    }

    @Override
    public BaseResponse<SuccessCode> onlineSeckillActivity(Long userId, Long activityId) {
        if (activityId == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAMS);
        }
        log.info("上线秒杀活动|{},{}", userId, activityId);
        SeckillActivity seckillActivity = seckillActivityMapper.selectById(activityId);
        if (SeckillActivityStatus.isOnline(seckillActivity.getActivityStatus())) {
            return ResultUtils.success(SuccessCode.ACTIVITY_ONLINE_SUCCESS);
        }

        seckillActivity.setActivityStatus(SeckillActivityStatus.ONLINE.getCode());
        seckillActivityMapper.updateById(seckillActivity);
        return ResultUtils.success(SuccessCode.ACTIVITY_ONLINE_SUCCESS);
    }

    @Override
    public BaseResponse<SuccessCode> offlineSeckillActivity(Long userId, Long activityId) {
        if (activityId == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAMS);
        }
        log.info("下线秒杀活动|{},{}", userId, activityId);
        SeckillActivity seckillActivity = seckillActivityMapper.selectById(activityId);
        if (SeckillActivityStatus.isOffline(seckillActivity.getActivityStatus())) {
            return ResultUtils.success(SuccessCode.ACTIVITY_OFFLINE_SUCCESS);
        }

        seckillActivity.setActivityStatus(SeckillActivityStatus.OFFLINE.getCode());
        seckillActivityMapper.updateById(seckillActivity);
        return ResultUtils.success(SuccessCode.ACTIVITY_OFFLINE_SUCCESS);
    }

    @Override
    public BaseResponse<MultiSeckillActivitiesResponse> getOnlineSeckillActivities(Long userId,
                                                                                   SeckillActivityQuery seckillActivityQuery) {
        if (seckillActivityQuery.getPageNum() == null || seckillActivityQuery.getPageSize() == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAMS);
        }
        LambdaQueryWrapper<SeckillActivity> seckillActivityLambdaQueryWrapper = new LambdaQueryWrapper<>();

        Page<SeckillActivity> seckillActivityPage = new Page<>(seckillActivityQuery.getPageNum(),
                seckillActivityQuery.getPageSize());

        seckillActivityLambdaQueryWrapper
                .like(!StringUtils.isEmpty(seckillActivityQuery.getKeyword()),
                        SeckillActivity::getActivityName,
                        seckillActivityQuery.getKeyword())
                .eq(seckillActivityQuery.getStatus() != null,
                        SeckillActivity::getActivityStatus,
                        seckillActivityQuery.getStatus());

        seckillActivityMapper.selectPage(seckillActivityPage, seckillActivityLambdaQueryWrapper);

        List<SeckillActivity> records = seckillActivityPage.getRecords();
        List<SeckillActivitiesResponse> seckillActivitiesResponseList = records.stream()
                .map(SeckillActivityBuilder::toSeckillActivitiesResponse)
                .collect(Collectors.toList());

        MultiSeckillActivitiesResponse multiSeckillActivitiesResponse = new MultiSeckillActivitiesResponse()
                .setSeckillActivitiesResponseList(seckillActivitiesResponseList)
                .setTotal(seckillActivityPage.getTotal());

        return ResultUtils.success(multiSeckillActivitiesResponse);
    }
}
