package com.ezreal.goods.service;

import com.ezreal.common.model.cahce.SeckillGoodCache;

public interface SeckillGoodCacheService {
    SeckillGoodCache getSeckillGoodCache(Long activityId, Long itemId, Long version);
}
