package com.ezreal.goods.service.impl;

import com.alibaba.fastjson.JSON;
import com.ezreal.common.cache.redis.DistributedCacheService;
import com.ezreal.common.lock.redisson.DistributedLock;
import com.ezreal.common.lock.redisson.DistributedLockFactoryService;
import com.ezreal.common.model.cahce.SeckillGoodCache;
import com.ezreal.common.model.domain.SeckillGood;
import com.ezreal.goods.mapper.SeckillGoodMapper;
import com.ezreal.goods.service.SeckillGoodCacheService;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.ezreal.common.constant.CacheConstant.FIVE_SECONDS;
import static com.ezreal.common.constant.CacheConstant.GOOD_CACHE_KEY;

@Service
public class SeckillGoodCacheServiceImpl implements SeckillGoodCacheService {

    private final static Logger logger = LoggerFactory.getLogger(SeckillGoodCacheServiceImpl.class);

    private final static Cache<Long, SeckillGoodCache> localCache = CacheBuilder.newBuilder()
            .initialCapacity(10)
            .concurrencyLevel(5)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build();
    private static final String UPDATE_ITEMS_CACHE_LOCK_KEY = "UPDATE_ITEMS_CACHE_LOCK_KEY_";
    private final Lock localCacheUpdateLock = new ReentrantLock();


    // 分布式锁
    @Resource
    private DistributedLockFactoryService distributedLockFactoryService;

    // 分布式缓存
    @Autowired
    private DistributedCacheService distributedCacheService;

    @Autowired
    private SeckillGoodMapper seckillGoodMapper;

    @Override
    public SeckillGoodCache getSeckillGoodCache(Long activityId, Long itemId, Long version) {
        // 获取本地缓存
        SeckillGoodCache seckillGoodCache = localCache.getIfPresent(itemId);

        // 检查本地缓存是否过期
        if (seckillGoodCache != null) {
            if (version == null) {
                logger.info("本地缓存命中|{}", itemId);
                return seckillGoodCache;
            }

            Long cacheVersion = seckillGoodCache.getVersion();
            if (cacheVersion.equals(version) || version < cacheVersion) {
                logger.info("本地缓存命中|{}, {}", itemId, version);
                return seckillGoodCache;
            }

            // 如果传入的版本大于本地缓存的版本，意味本地缓存滞后，需要更新
            if (version > cacheVersion) {
                return getLatestDistributedSeckillGood(activityId, itemId, version);
            }
        }

        return getLatestDistributedSeckillGood(activityId, itemId, version);
    }

    /**
     * 远程缓存的获取 + 本地缓存更新
     * @param activityId
     * @param itemId
     * @param version
     * @return
     */
    private SeckillGoodCache getLatestDistributedSeckillGood(Long activityId, Long itemId, Long version) {
        logger.info("itemCache|读取远程缓存|{}", itemId);

        // 获取远程缓存
        SeckillGoodCache distributedGoodCache = distributedCacheService.getObject(buildItemCacheKey(itemId), SeckillGoodCache.class);

        // 远程缓存为空 就更新远程缓存
        if (distributedGoodCache == null || distributedGoodCache.getSeckillGood() == null) {
            distributedGoodCache = updateDistributedSeckillGood(itemId);
        }

        // 不为空就更新本地缓存
        if (distributedGoodCache != null && !distributedGoodCache.isLater()) {

            // 只需要一个线程更新该锁即可 本地缓存
            boolean isLockSuccess = localCacheUpdateLock.tryLock();

            if (isLockSuccess) {
                try {
                    localCache.put(itemId, distributedGoodCache);
                    logger.info("本地缓存已更新|{}", itemId);
                } finally {
                    localCacheUpdateLock.unlock();
                }
            }
        }

        return distributedGoodCache;
    }

    private SeckillGoodCache updateDistributedSeckillGood(Long itemId) {
        logger.info("更新远程缓存|{}", itemId);
        DistributedLock distributedLock = distributedLockFactoryService.getDistributedLock(UPDATE_ITEMS_CACHE_LOCK_KEY + itemId);
        try {
            boolean tryLock = distributedLock.tryLock(1, 5, TimeUnit.SECONDS);

            // 如果没有获得到锁，就返回重试
            if (!tryLock) {
                return new SeckillGoodCache().tryLater();
            }

            // 再次检查
            SeckillGoodCache distributedSeckillCache = distributedCacheService.getObject(buildItemCacheKey(itemId), SeckillGoodCache.class);
            if (distributedSeckillCache != null) {
                return distributedSeckillCache;
            }

            // 查询数据库
            SeckillGood seckillGood = seckillGoodMapper.selectById(itemId);
            SeckillGoodCache seckillGoodCache = new SeckillGoodCache();
            if (seckillGood == null) {
                // 数据不存在 也要返回 也要存缓存 防止缓存穿透
                seckillGoodCache.notExist();
            } else {
                seckillGoodCache.with(seckillGood).setVersion(System.currentTimeMillis());
            }
            logger.info("itemCache|远程缓存已更新|{}", itemId);
            distributedCacheService.put(buildItemCacheKey(itemId), JSON.toJSONString(seckillGoodCache), FIVE_SECONDS, TimeUnit.SECONDS);
            return seckillGoodCache;
        } catch (InterruptedException e) {

            logger.error("itemCache|远程缓存更新失败|{}", itemId);
            return new SeckillGoodCache().tryLater();
        } finally {

            distributedLock.unlock();
        }
    }

    private String buildItemCacheKey(Long itemId) {
        return GOOD_CACHE_KEY + itemId;
    }
}
