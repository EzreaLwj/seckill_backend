package com.ezreal.order.service.impl.bucket;

import com.alibaba.fastjson.JSON;
import com.ezreal.common.cache.redis.DistributedCacheService;
import com.ezreal.common.lock.redisson.DistributedLock;
import com.ezreal.common.lock.redisson.DistributedLockFactoryService;
import com.ezreal.common.model.cahce.GoodStockCache;
import com.ezreal.common.model.domain.SeckillBucket;
import com.ezreal.common.model.domain.StockDeduction;
import com.ezreal.order.mapper.SeckillBucketMapper;
import com.ezreal.order.service.GoodStockCacheService;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.ezreal.common.constant.CacheConstant.*;
import static com.ezreal.order.service.impl.bucket.BucketsArrangementServiceImpl.getItemStockBucketsQuantityCacheKey;
import static com.ezreal.order.service.impl.normal.NormalGoodStockCacheServiceImpl.getGoodsStockCacheAlignKey;

@Service
@ConditionalOnProperty(name = "place_order_type", havingValue = "buckets", matchIfMissing = true)
public class BucketsCacheServiceImpl implements GoodStockCacheService {

    private Logger logger = LoggerFactory.getLogger(BucketsCacheServiceImpl.class);
    private static final Cache<Long, Integer> goodBucketsQuantityLocalCache = CacheBuilder.newBuilder()
            .concurrencyLevel(5)
            .initialCapacity(10)
            .expireAfterWrite(10, TimeUnit.SECONDS)
            .build();

    private static final Cache<String, Integer> bucketAvailableStocksLocalCache = CacheBuilder.newBuilder()
            .concurrencyLevel(5)
            .initialCapacity(1000)
            .expireAfterWrite(100, TimeUnit.MILLISECONDS)
            .build();

    @Autowired
    private SeckillBucketMapper seckillBucketMapper;

    @Resource
    private DistributedCacheService redisCacheService;

    @Resource
    private DistributedLockFactoryService distributedLockFactoryService;

    private static final DefaultRedisScript<Long> INIT_OR_ALIGN_ITEM_STOCK_LUA;
    private static final DefaultRedisScript<Long> INCREASE_ITEM_STOCK_LUA;
    private static final DefaultRedisScript<Long> DECREASE_ITEM_STOCK_LUA;

    static {
        INIT_OR_ALIGN_ITEM_STOCK_LUA = new DefaultRedisScript<>();
        INIT_OR_ALIGN_ITEM_STOCK_LUA.setLocation(new ClassPathResource("/lua/bucket/init_or_align_item_stock.lua"));
        INIT_OR_ALIGN_ITEM_STOCK_LUA.setResultType(Long.class);

        INCREASE_ITEM_STOCK_LUA = new DefaultRedisScript<>();
        INCREASE_ITEM_STOCK_LUA.setLocation(new ClassPathResource("/lua/bucket/increase_item_stock.lua"));
        INCREASE_ITEM_STOCK_LUA.setResultType(Long.class);

        DECREASE_ITEM_STOCK_LUA = new DefaultRedisScript<>();
        DECREASE_ITEM_STOCK_LUA.setLocation(new ClassPathResource("/lua/bucket/decrease_item_stock.lua"));
        DECREASE_ITEM_STOCK_LUA.setResultType(Long.class);
    }

    @Override
    public boolean alignItemStocks(Long activity, Long itemId) {
        if (activity == null || itemId == null) {
            logger.info("alignItemStocks|????????????");
            return false;
        }
        String stockBucketCacheInitLockKey = getStockBucketCacheInitLockKey(itemId);
        DistributedLock distributedLock = distributedLockFactoryService.getDistributedLock(stockBucketCacheInitLockKey);
        try {
            boolean isSuccess = distributedLock.tryLock(5, 5, TimeUnit.SECONDS);
            if (!isSuccess) {
                logger.info("alignItemStocks|??????????????????????????????|{}", itemId);
                return false;
            }
            List<SeckillBucket> seckillBucketList = seckillBucketMapper.selectByItemId(itemId);
            if (CollectionUtils.isEmpty(seckillBucketList)) {
                logger.info("alignItemStocks|????????????????????????|{}", itemId);
                return false;
            }

            seckillBucketList.forEach((seckillBucket) -> {
                String key1StockBucketCacheKey = getBucketAvailableStocksCacheKey(seckillBucket.getItemId(), seckillBucket.getSerialNo());
                String key2StockBucketsSuspendKey = getItemStockBucketsSuspendKey(seckillBucket.getItemId());
                String key3ItemStocksCacheAlignKey = getGoodsStockCacheAlignKey(seckillBucket.getItemId());
                String key4ItemStockBucketsQuantityCacheKey = getGoodStockBucketsQuantityCacheKey(seckillBucket.getItemId());

                List<String> keys = Lists.newArrayList(key1StockBucketCacheKey,
                        key2StockBucketsSuspendKey,
                        key3ItemStocksCacheAlignKey,
                        key4ItemStockBucketsQuantityCacheKey);

                Long result = redisCacheService.getRedisTemplate().execute(INIT_OR_ALIGN_ITEM_STOCK_LUA
                        , keys
                        , seckillBucket.getAvailableStocksAmount()
                        , seckillBucketList.size());
                if (result == null) {
                    logger.info("alignItemStocks|????????????????????????|{},{}", itemId, stockBucketCacheInitLockKey);
                    return;
                }
                if (result == -998) {
                    logger.info("alignItemStocks|?????????????????????????????????|{},{},{}", result, itemId, stockBucketCacheInitLockKey);
                    return;
                }
                if (result == -997) {
                    logger.info("alignItemStocks|???????????????????????????|{},{},{}", result, itemId, stockBucketCacheInitLockKey);
                    return;
                }
                if (result == 1) {
                    logger.info("alignItemStocks|????????????????????????|{},{},{}", result, itemId, stockBucketCacheInitLockKey);
                }
            });
            logger.info("alignItemStocks|??????????????????????????????|{},{}", itemId, stockBucketCacheInitLockKey);
            return true;
        } catch (Exception e) {
            logger.error("alignItemStocks|??????????????????????????????|{},{}", itemId, stockBucketCacheInitLockKey, e);
            return false;
        } finally {
            distributedLock.unlock();
        }
    }

    @Override
    public boolean decreaseGoodStock(StockDeduction stockDeduction) {
        logger.info("decreaseItemStock|?????????????????????{}", JSON.toJSONString(stockDeduction));
        if (stockDeduction == null || !stockDeduction.validate()) {
            return false;
        }
        try {
            Integer subBucketsQuantity = getSubBucketsQuantity(stockDeduction.getItemId());
            if (subBucketsQuantity == null) {
                return false;
            }
            Integer targetBucketSerialNo = getTargetBucketSerialNo(stockDeduction.getUserId(), subBucketsQuantity);
            stockDeduction.setSerialNo(targetBucketSerialNo);

            String key1StockBucketCacheKey = getBucketAvailableStocksCacheKey(stockDeduction.getItemId(), targetBucketSerialNo);
            String key2StockBucketsSuspendKey = getItemStockBucketsSuspendKey(stockDeduction.getItemId());
            String key3ItemStocksAlignKey = getGoodsStockCacheAlignKey(stockDeduction.getItemId());

            List<String> keys = Lists.newArrayList(key1StockBucketCacheKey, key2StockBucketsSuspendKey, key3ItemStocksAlignKey);
            Long result = redisCacheService.getRedisTemplate().execute(DECREASE_ITEM_STOCK_LUA, keys, stockDeduction.getQuantity());
            if (result == null || result == -996) {
                logger.info("decreaseItemStock|?????????????????????|{},{}", targetBucketSerialNo, key1StockBucketCacheKey);
                return false;
            }
            if (result == -998) {
                logger.info("decreaseItemStock|?????????????????????????????????|{},{}", result, key1StockBucketCacheKey);
                return false;
            }
            if (result == -997) {
                logger.info("decreaseItemStock|???????????????????????????|{},{}", result, key1StockBucketCacheKey);
                return false;
            }
            if (result == -1) {
                logger.info("decreaseItemStock|????????????|{},{}", result, key1StockBucketCacheKey);
                return false;
            }
            if (result == 1) {
                logger.info("decreaseItemStock|??????????????????|{},{}", result, key1StockBucketCacheKey);
                return true;
            }
            logger.info("decreaseItemStock|??????????????????|{},{}", result, key1StockBucketCacheKey);
            return false;
        } catch (Exception e) {
            logger.error("decreaseItemStock|??????????????????", e);
            return false;
        }
    }

    @Override
    public boolean increaseGoodStock(StockDeduction stockDeduction) {
        logger.info("increaseItemStock|?????????????????????|{}", JSON.toJSONString(stockDeduction));
        if (stockDeduction == null || !stockDeduction.validate()) {
            return false;
        }
        try {
            Integer targetBucketSerialNo = getTargetBucketSerialNo(stockDeduction.getUserId(), getSubBucketsQuantity(stockDeduction.getItemId()));
            String key1StockBucketCacheKey = getBucketAvailableStocksCacheKey(stockDeduction.getItemId(), targetBucketSerialNo);
            String key2StockBucketsSuspendKey = getItemStockBucketsSuspendKey(stockDeduction.getItemId());
            String key3ItemStocksAlignKey = getGoodsStockCacheAlignKey(stockDeduction.getItemId());
            List<String> keys = Lists.newArrayList(key1StockBucketCacheKey, key2StockBucketsSuspendKey, key3ItemStocksAlignKey);
            Long result = redisCacheService.getRedisTemplate().execute(INCREASE_ITEM_STOCK_LUA, keys, stockDeduction.getQuantity());

            if (result == null || result == -996) {
                logger.info("increaseItemStock|?????????????????????|{},{},{}", stockDeduction.getItemId(), targetBucketSerialNo, key1StockBucketCacheKey);
                return false;
            }
            if (result == -998) {
                logger.info("increaseItemStock|?????????????????????????????????|{},{},{}", result, stockDeduction.getItemId(), key1StockBucketCacheKey);
                return false;
            }
            if (result == -997) {
                logger.info("increaseItemStock|???????????????????????????|{},{}", result, key1StockBucketCacheKey);
                return false;
            }
            if (result == 1) {
                logger.info("increaseItemStock|??????????????????|{},{}", result, key1StockBucketCacheKey);
                return true;
            }
            return false;
        } catch (Exception e) {
            logger.error("increaseItemStock|??????????????????|", e);
            return false;
        }
    }

    @Override
    public GoodStockCache getAvailableItemStock(Long userId, Long itemId) {
        Integer subBucketsQuantity = getSubBucketsQuantity(itemId);
        if (subBucketsQuantity == null) {
            return  null;
        }
        Integer targetBucketSerialNo = getTargetBucketSerialNo(userId, subBucketsQuantity);
        String bucketCacheKey = getBucketAvailableStocksCacheKey(itemId, targetBucketSerialNo);
        Integer availableBucketStocks = bucketAvailableStocksLocalCache.getIfPresent(bucketCacheKey);
        if (availableBucketStocks == null) {
            availableBucketStocks = redisCacheService.getObject(getBucketAvailableStocksCacheKey(itemId, targetBucketSerialNo), Integer.class);
        }
        return new GoodStockCache().with(availableBucketStocks);
    }


    private Integer getTargetBucketSerialNo(Long userId, Integer bucketsQuantity) {
        if (userId == null || bucketsQuantity == null || bucketsQuantity <= 0) {
            return null;
        }
        if (bucketsQuantity == 1) {
            return 0;
        }
        return userId.hashCode() % bucketsQuantity;
    }

    /**
     * ?????????????????????????????????
     * @param itemId
     * @return
     */
    private Integer getSubBucketsQuantity(Long itemId) {
        Integer subBucketsQuantity = goodBucketsQuantityLocalCache.getIfPresent(itemId);
        if (subBucketsQuantity != null) {
            return subBucketsQuantity;
        }
        subBucketsQuantity = redisCacheService.getInteger(getItemStockBucketsQuantityCacheKey(itemId));
        if (subBucketsQuantity != null) {
            goodBucketsQuantityLocalCache.put(itemId, subBucketsQuantity);
        }
        return subBucketsQuantity;
    }

    /**
     * ???????????????????????????
     *
     * @param itemId
     * @param serialNumber
     * @return
     */
    public static String getBucketAvailableStocksCacheKey(Long itemId, Integer serialNumber) {
        return (ITEM_BUCKET_AVAILABLE_STOCKS_KEY + "_" + itemId + "_" + serialNumber);
    }

    /**
     * ?????????????????????
     *
     * @param itemId
     * @return
     */
    public static String getItemStockBucketsSuspendKey(Long itemId) {
        return (ITEM_STOCK_BUCKETS_SUSPEND_KEY + "_" + itemId);
    }

    /**
     * ?????????????????????????????????
     *
     * @param itemId
     * @return
     */
    public static String getGoodStockBucketsQuantityCacheKey(Long itemId) {
        return ITEM_BUCKETS_QUANTITY_KEY + "_" + itemId;
    }


    /**
     * ????????????????????????
     *
     * @param itemId
     * @return
     */
    private String getStockBucketCacheInitLockKey(Long itemId) {
        return (ITEM_BUCKETS_CACHE_INIT_KEY + "_" + itemId);
    }

}
