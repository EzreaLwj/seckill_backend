package com.ezreal.goods.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ezreal.common.cache.redis.DistributedCacheService;
import com.ezreal.common.lock.redisson.DistributedLock;
import com.ezreal.common.lock.redisson.DistributedLockFactoryService;
import com.ezreal.common.model.cahce.SeckillGoodsCache;
import com.ezreal.common.model.domain.SeckillGood;
import com.ezreal.common.model.enums.SeckillGoodStatus;
import com.ezreal.goods.mapper.SeckillGoodMapper;
import com.ezreal.goods.service.SeckillGoodsCacheService;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.ezreal.common.constant.CacheConstant.*;

@Service
public class SeckillGoodsCacheServiceImpl implements SeckillGoodsCacheService {

    private final static Logger logger = LoggerFactory.getLogger(SeckillGoodsCacheService.class);

    private final static Cache<Long, SeckillGoodsCache> localCache = CacheBuilder.newBuilder()
            .initialCapacity(10)
            .concurrencyLevel(5)
            .expireAfterWrite(10L, TimeUnit.SECONDS)
            .build();

    @Resource
    private DistributedLockFactoryService distributedLockService;

    @Resource
    private DistributedCacheService distributedCacheService;

    @Autowired
    private SeckillGoodMapper seckillGoodMapper;

    private static final Lock localCacheLock = new ReentrantLock();
    private static final String UPDATE_GOODS_CACHE_LOCK_KEY = "UPDATE_GOODS_CACHE_LOCK_KEY_";

    public SeckillGoodsCache getSeckillGoodsCache(Long userId, Long activityId, Long version) {

        logger.info("??????????????????|{}", activityId);
        SeckillGoodsCache localSeckillGoodsCache = localCache.getIfPresent(activityId);

        if (localSeckillGoodsCache != null) {
            if (version == null) {
                logger.info("??????????????????|{}", activityId);
                return localSeckillGoodsCache;
            }

            Long cacheVersion = localSeckillGoodsCache.getVersion();
            if (cacheVersion.equals(version) || version < cacheVersion) {
                logger.info("??????????????????|{}", activityId);
                return localSeckillGoodsCache;
            }
            if (version > cacheVersion) {
                logger.info("?????????????????????|{}", activityId);
                return getDistributedSeckillGoodsCache(userId, activityId);
            }
        }
        logger.info("?????????????????????|{}", activityId);
        return getDistributedSeckillGoodsCache(userId, activityId);
    }

    public SeckillGoodsCache getDistributedSeckillGoodsCache(Long userId, Long activityId) {
        logger.info("??????????????????|{}", activityId);

        // ??????????????????
        SeckillGoodsCache distributedSeckillGoodsCache = distributedCacheService.getObject(buildItemCacheKey(activityId), SeckillGoodsCache.class);

        if (distributedSeckillGoodsCache == null) {
            distributedSeckillGoodsCache = updateDistributedSeckillGoodCache(userId, activityId);
        }

        // ??????????????????
        if (distributedSeckillGoodsCache != null && !distributedSeckillGoodsCache.isLater()) {
            try {
                boolean isLockSuccess = localCacheLock.tryLock();
                if (isLockSuccess) {
                    logger.info("??????????????????|{}", activityId);
                    localCache.put(activityId, distributedSeckillGoodsCache);
                }
            } finally {
                localCacheLock.unlock();
            }
        }

        return distributedSeckillGoodsCache;
    }

    public SeckillGoodsCache updateDistributedSeckillGoodCache(Long userId, Long activityId) {
        logger.info("itemsCache|??????????????????|{}", activityId);
        DistributedLock distributedLock = distributedLockService.getDistributedLock(UPDATE_GOODS_CACHE_LOCK_KEY + activityId);
        try {
            boolean isSuccessLock = distributedLock.tryLock(500, 1000, TimeUnit.MILLISECONDS);
            if (!isSuccessLock) {
                logger.info("????????????????????????|{}", activityId);
                return new SeckillGoodsCache().tryLater();
            }

            // ??????????????????
            SeckillGoodsCache distributedSeckillGoodsCache = distributedCacheService.getObject(buildItemCacheKey(activityId), SeckillGoodsCache.class);
            if (distributedSeckillGoodsCache != null) {
                return distributedSeckillGoodsCache;
            }

            distributedSeckillGoodsCache = new SeckillGoodsCache();

            // ???????????????????????????
            Page<SeckillGood> seckillGoodPage = new Page<>(1, 7);
            LambdaQueryWrapper<SeckillGood> seckillGoodLambdaQueryWrapper = new LambdaQueryWrapper<>();


            seckillGoodLambdaQueryWrapper.eq(SeckillGood::getActivityId, activityId)
                    .eq(SeckillGood::getStatus, SeckillGoodStatus.ONLINE.getCode());

            seckillGoodMapper.selectPage(seckillGoodPage, seckillGoodLambdaQueryWrapper);

            if (seckillGoodPage.getRecords() == null) {
                distributedSeckillGoodsCache.empty();
            } else {
                distributedSeckillGoodsCache.setSeckillGoods(seckillGoodPage.getRecords())
                        .setTotal((int) seckillGoodPage.getTotal())
                        .setVersion(System.currentTimeMillis());
            }

            distributedCacheService.put(buildItemCacheKey(activityId), distributedSeckillGoodsCache, FIVE_SECONDS, TimeUnit.SECONDS);
            logger.info("?????????????????????|{}", activityId);
            return distributedSeckillGoodsCache;
        } catch (InterruptedException e) {
            logger.info("????????????????????????|{}", activityId, e);
            return new SeckillGoodsCache().tryLater();
        } finally {
            distributedLock.unlock();
        }
    }

    private String buildItemCacheKey(Long itemId) {
        return GOODS_CACHE_KEY + itemId;
    }
}
