package com.ezreal.common.model.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 秒杀订单表
 * @TableName seckill_order
 */
@TableName(value ="seckill_order")
@Data
public class SeckillOrder implements Serializable {
    /**
     * 主键
     */
    @TableId
    private Long id;

    /**
     * 秒杀品ID
     */
    private Long itemId;

    /**
     * 秒杀活动ID
     */
    private Long activityId;

    /**
     * 秒杀品名称标题
     */
    private String itemTitle;

    /**
     * 秒杀价
     */
    private Long flashPrice;

    /**
     * 数量
     */
    private Integer quantity;

    /**
     * 总价格
     */
    private Long totalAmount;

    /**
     * 订单状态
     */
    private Integer status;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 更新时间
     */
    private Date modifiedTime;

    /**
     * 创建时间
     */
    private Date createTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}