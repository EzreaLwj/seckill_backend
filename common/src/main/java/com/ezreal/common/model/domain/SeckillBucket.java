package com.ezreal.common.model.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.data.redis.core.convert.Bucket;

import static com.ezreal.common.model.enums.BucketType.PRIMARY;

/**
 * 秒杀品库存分桶表
 * @TableName seckill_bucket
 */
@TableName(value ="seckill_bucket")
@Data
@Accessors(chain = true)
public class SeckillBucket implements Serializable {
    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 秒杀品ID
     */
    private Long itemId;

    /**
     * 库存总量
     */
    private Integer totalStocksAmount;

    /**
     * 可用库存总量
     */
    private Integer availableStocksAmount;

    /**
     * 库存状态
     */
    private Integer status;

    /**
     * 库存分桶编号
     */
    private Integer serialNo;

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


    public boolean isSubSeckillBucket() {
        return !PRIMARY.getCode().equals(serialNo);
    }

    public void addAvailableStocks(int availableStocksAmount) {
        if (this.availableStocksAmount == null) {
            return;
        }
        this.availableStocksAmount += availableStocksAmount;
    }

    public Integer getSerialNo() {
        return serialNo;
    }

    public boolean isPrimarySeckillBucket() {
        return PRIMARY.getCode().equals(serialNo);
    }

    public SeckillBucket initPrimary() {
        this.serialNo = PRIMARY.getCode();
        return this;
    }

    public void increaseTotalStocksAmount(Integer incrementalStocksAmount) {
        if (incrementalStocksAmount == null) {
            return;
        }
        this.totalStocksAmount += incrementalStocksAmount;
    }
}