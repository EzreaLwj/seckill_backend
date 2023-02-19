package com.ezreal.activity.controller;

import com.ezreal.common.model.enums.SeckillActivityStatus;
import com.ezreal.common.model.query.SeckillActivityQuery;
import com.ezreal.common.model.request.PublishSeckillActivityRequest;
import com.ezreal.common.model.response.avtivity.MultiSeckillActivitiesResponse;
import com.ezreal.common.model.response.avtivity.SeckillActivitiesResponse;
import com.ezreal.activity.service.SeckillActivityService;
import com.ezreal.common.BaseResponse;
import com.ezreal.common.SuccessCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/activity")
public class SeckillActivityController {

    @Autowired
    private SeckillActivityService seckillActivityService;

    /**
     * 发布秒杀活动
     * @param userId
     * @param publishSeckillActivityRequest
     * @return
     */
    @PostMapping("/publish")
    public BaseResponse<SuccessCode> publishSeckillActivity(@RequestHeader(value = "TokenInfo") Long userId,
                                                            @RequestBody PublishSeckillActivityRequest publishSeckillActivityRequest) {

        return seckillActivityService.publishSeckillActivity(userId, publishSeckillActivityRequest);
    }

    /**
     * 修改秒杀活动
     * @param userId
     * @param activityId
     * @param publishSeckillActivityRequest
     * @return
     */
    @PutMapping("/update/{activityId}")
    public BaseResponse<SuccessCode> updateSeckillActivity(@RequestHeader(value = "TokenInfo") Long userId,
                                                           @PathVariable Long activityId,
                                                           @RequestBody PublishSeckillActivityRequest publishSeckillActivityRequest
                                                           ) {
        return seckillActivityService.updateSeckillActivity(userId, activityId, publishSeckillActivityRequest);
    }

    /**
     * 上线活动
     * @param userId
     * @param activityId
     * @return
     */
    @PutMapping("/update/online/{activityId}")
    public BaseResponse<SuccessCode> onlineSeckillActivity(@RequestHeader(value = "TokenInfo") Long userId,
                                                           @PathVariable Long activityId) {
        return seckillActivityService.onlineSeckillActivity(userId, activityId);
    }

    /**
     * 下线活动
     * @param userId
     * @param activityId
     * @return
     */
    @PutMapping("/update/offline/{activityId}")
    public BaseResponse<SuccessCode> offlineSeckillActivity(@RequestHeader(value = "TokenInfo") Long userId,
                                                            @PathVariable Long activityId) {
        return seckillActivityService.offlineSeckillActivity(userId, activityId);
    }

    /**
     * 获取秒杀活动列表
     * @param userId
     * @param pageNum
     * @param pageSize
     * @param keyword
     * @return
     */
    @GetMapping("/list")
    public BaseResponse<MultiSeckillActivitiesResponse> getSeckillActivities(@RequestHeader(value = "TokenInfo") Long userId,
                                                                             @RequestParam("pageNum")Integer pageNum,
                                                                             @RequestParam("pageSize") Integer pageSize,
                                                                             @RequestParam(value = "keyword", required = false) String keyword) {
        SeckillActivityQuery seckillActivityQuery = new SeckillActivityQuery()
                .setKeyword(keyword)
                .setPageNum(pageNum)
                .setPageSize(pageSize);
        return seckillActivityService.getSeckillActivities(userId, seckillActivityQuery);
    }

    @GetMapping("/list/online")
    public BaseResponse<MultiSeckillActivitiesResponse> getOnlineSeckillActivities(@RequestHeader(value = "TokenInfo") Long userId,
                                                                             @RequestParam("pageNum")Integer pageNum,
                                                                             @RequestParam("pageSize") Integer pageSize,
                                                                             @RequestParam(value = "keyword", required = false) String keyword) {
        SeckillActivityQuery seckillActivityQuery = new SeckillActivityQuery()
                .setKeyword(keyword)
                .setPageNum(pageNum)
                .setPageSize(pageSize)
                .setStatus(SeckillActivityStatus.ONLINE.getCode());
        return seckillActivityService.getOnlineSeckillActivities(userId, seckillActivityQuery);
    }



    /**
     * 获取单条秒杀活动
     * @param userId
     * @param activityId
     * @return
     */
    @GetMapping("/list/{activityId}")
    public BaseResponse<SeckillActivitiesResponse> getSeckillActivity(@RequestHeader(value = "TokenInfo") Long userId,
                                                                      @PathVariable Long activityId) {

        return seckillActivityService.getSeckillActivity(userId, activityId);
    }
}
