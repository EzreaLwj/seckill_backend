package com.ezreal.common.model.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.util.StringUtils;

import java.util.Date;

@Data
public class PublishSeckillGoodRequest {

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
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;

    /**
     * 秒杀结束时间
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;

    /**
     * 秒杀品状态
     */
    private Integer status;

    /**
     * 所属活动id
     */
    private Long activityId;

    public boolean validate() {

        return !StringUtils.isEmpty(itemTitle) &&
                initialStock != null &&
                availableStock != null &&
                originalPrice != null &&
                flashPrice != null &&
                startTime != null &&
                endTime != null &&
                startTime.before(endTime);
    }
}
