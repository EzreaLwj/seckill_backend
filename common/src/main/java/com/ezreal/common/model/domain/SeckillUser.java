package com.ezreal.common.model.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 用户表
 * @TableName seckill_user
 */
@TableName(value ="seckill_user")
@Data
public class SeckillUser implements Serializable {
    /**
     * 自增id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户昵称
     */
    private String nickName;

    /**
     * 用户手机号
     */
    private String phone;

    /**
     * 用户密码
     */
    private String password;

    /**
     * 生日
     */
    private Date birthday;

    /**
     * 用户身份
     */
    private Integer status;

    /**
     * 用户头像
     */
    private String headImg;

    /**
     * 性别
     */
    private Integer sex;

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