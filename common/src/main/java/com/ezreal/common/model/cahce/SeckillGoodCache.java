package com.ezreal.common.model.cahce;

import com.ezreal.common.model.domain.SeckillGood;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class SeckillGoodCache {

    /**
     * 数据是否存在
     */
    protected boolean exist;

    /**
     * 商品实体
     */
    private SeckillGood seckillGood;

    /**
     * 版本号
     */
    private Long version;

    /**
     *没有获取到锁 是否重试
     */
    private boolean later;

    public SeckillGoodCache with(SeckillGood seckillGood) {
        this.exist = true;
        this.seckillGood = seckillGood;
        return this;
    }


    public SeckillGoodCache withVersion(Long version) {
        this.version = version;
        return this;
    }

    public SeckillGoodCache tryLater() {
        this.later = true;
        return this;
    }

    public SeckillGoodCache notExist() {
        this.exist = false;
        return this;
    }
}
