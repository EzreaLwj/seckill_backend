package com.ezreal.common.model.response.good;

import com.ezreal.common.model.enums.SeckillActivityStatus;
import com.ezreal.common.model.enums.SeckillGoodStatus;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;

@Data
@Slf4j
public class SeckillGoodResponse {


    /**
     * 主键
     */
    private Long id;

    /**
     * 秒杀品名称标题
     */
    private String itemTitle;

    /**
     * 秒杀品副标题
     */
    private String itemSubTitle;

    /**
     * 秒杀品介绍富文本文案
     */
    private String itemDesc;

    /**
     * 秒杀品初始库存
     */
    private Integer initialStock;

    /**
     * 秒杀品可用库存
     */
    private Integer availableStock;

    /**
     * 秒杀品库存是否已经预热
     */
    private Integer stockWarmUp;

    /**
     * 秒杀品原价
     */
    private Long originalPrice;

    /**
     * 秒杀价
     */
    private Long flashPrice;

    /**
     * 秒杀开始时间
     */
    private Date startTime;

    /**
     * 秒杀结束时间
     */
    private Date endTime;

    /**
     * 秒杀可配规则，JSON格式
     */
    private String rules;

    /**
     * 秒杀品状态
     */
    private Integer status;

    /**
     * 所属活动id
     */
    private Long activityId;

    /**
     * 数据版本号
     */
    private Long version;

    /**
     * 当前服务器时间
     */
    private long serverTimeMills = System.currentTimeMillis();

    /**
     * 当前秒杀品秒杀是否开始
     */
    public SeckillGoodResponse() {
    }


    public boolean isStarted() {
        if (!SeckillGoodStatus.isOnline(status)) {
            return false;
        }
        if (startTime == null || endTime == null) {
            return false;
        }
        Date now = new Date();
        return (startTime.equals(now) || startTime.before(now)) && endTime.after(now);
    }

    public boolean isAllowPlaceOrderOrNot() {
        if (System.currentTimeMillis() > endTime.getTime()) {
            log.info("秒杀商品时间已经结束|{}, {}", activityId, id);
            return false;
        }
        
        if (!SeckillActivityStatus.isOnline(status)) {
            log.info("当前商品未上线|{}, {}", activityId, id);
            return false;
        }

        return true;
    }
}
