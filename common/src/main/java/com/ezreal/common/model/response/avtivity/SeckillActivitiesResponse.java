package com.ezreal.common.model.response.avtivity;

import com.ezreal.common.model.enums.SeckillActivityStatus;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;

@Data
@Slf4j
public class SeckillActivitiesResponse {
    /**
     * 自增主键
     */
    private Long activityId;

    /**
     * 活动名称
     */
    private String activityName;

    /**
     * 开始时间
     */
    private Date startTime;

    /**
     * 结束时间
     */
    private Date endTime;

    /**
     * 活动状态 0-发布 1-上线 2-下线
     */
    private Integer activityStatus;

    /**
     * 活动描述
     */
    private String activityDesc;

    /**
     * 数据版本
     */
    private Long version;

    public boolean isAllowPlaceOrderOrNot() {
        if (System.currentTimeMillis() > endTime.getTime()) {
            log.info("秒杀活动已经结束|{}", activityId);
            return false;
        }
        if (!SeckillActivityStatus.isOnline(activityStatus)) {
            log.info("当前活动未上线|{}", activityId);
            return false;
        }

        return true;
    }
}
