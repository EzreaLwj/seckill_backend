package com.ezreal.common.model.domain;

import java.io.Serializable;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

/**
 * 秒杀活动表
 * @TableName seckill_activity
 */
@Data
public class SeckillActivity implements Serializable {
    /**
     * 自增主键
     */
    @TableId(type = IdType.AUTO)
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

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}