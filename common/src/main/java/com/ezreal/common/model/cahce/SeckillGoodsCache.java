package com.ezreal.common.model.cahce;

import com.ezreal.common.model.domain.SeckillGood;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

@Data
@Accessors(chain = true)
public class SeckillGoodsCache {
    /**
     * 数据是否存在
     */
    protected boolean exist;

    /**
     * 数据的数量是否为0
     */
    protected boolean empty;

    /**
     * 数据
     */
    private List<SeckillGood> seckillGoods;

    /**
     * 版本号
     */
    private Long version;

    /**
     * 稍后访问
     */
    private boolean later;

    /**
     * 数据数量
     */
    private Integer total;

    public SeckillGoodsCache with(List<SeckillGood> seckillGoods) {
        this.exist = true;
        this.seckillGoods = seckillGoods;
        return this;
    }


    public SeckillGoodsCache withVersion(Long version) {
        this.version = version;
        return this;
    }

    public SeckillGoodsCache tryLater() {
        this.later = true;
        return this;
    }

    public SeckillGoodsCache notExist() {
        this.exist = false;
        return this;
    }
    public SeckillGoodsCache empty() {
        this.empty = true;
        this.seckillGoods = new ArrayList<>();
        this.total = 0;
        return this;
    }
}
