package com.ezreal.goods.service;

import com.ezreal.common.model.cahce.SeckillGoodsCache;

public interface SeckillGoodsCacheService {
    SeckillGoodsCache getSeckillGoodsCache(Long userId, Long activityId, Long version);
}
