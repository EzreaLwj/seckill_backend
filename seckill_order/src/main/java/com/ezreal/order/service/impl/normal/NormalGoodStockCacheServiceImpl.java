package com.ezreal.order.service.impl.normal;

import com.ezreal.clients.SeckillGoodClient;
import com.ezreal.common.cache.redis.DistributedCacheService;
import com.ezreal.common.cache.redis.RedisCacheService;
import com.ezreal.common.lock.redisson.DistributedLockFactoryService;
import com.ezreal.common.model.cahce.GoodStockCache;
import com.ezreal.common.model.domain.StockDeduction;
import com.ezreal.common.model.response.good.SeckillGoodResponse;
import com.ezreal.order.service.GoodStockCacheService;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import io.swagger.models.auth.In;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

@Service
@ConditionalOnProperty(name = "place_order_type", havingValue = "normal", matchIfMissing = true)
public class NormalGoodStockCacheServiceImpl implements GoodStockCacheService {

    private final static Logger logger = LoggerFactory.getLogger(NormalGoodStockCacheServiceImpl.class);

    @Resource
    private RedisCacheService redisCacheService;

    @Resource
    private SeckillGoodClient seckillGoodClient;

    @Resource
    private DistributedCacheService distributedCacheService;

    @Resource
    private DistributedLockFactoryService distributedLockService;

    // 预扣减库存的代码
    private static final DefaultRedisScript<Long> DECREASE_GOOD_STOCK_SCRIPT;

    // 恢复库存的代码
    private static final DefaultRedisScript<Long> INCREASE_GOOD_STOCK_SCRIPT;

    // 校准库存的代码
    private static final DefaultRedisScript<Long> INIT_OR_ALIGN_ITEM_STOCK_SCRIPT;


    private static final String ITEM_STOCK_ALIGN_LOCK_KEY = "ITEM_STOCK_ALIGN_LOCK_KEY_";
    private static final String ITEM_STOCKS_CACHE_KEY = "ITEM_STOCKS_CACHE_KEY_";
    private static final int IN_STOCK_ALIGNING = -9;

    private static final Cache<Long, GoodStockCache> goodStockLocalCache = CacheBuilder.newBuilder()
            .concurrencyLevel(5)
            .initialCapacity(10)
            .expireAfterWrite(10, TimeUnit.SECONDS)
            .build();

    static {
        DECREASE_GOOD_STOCK_SCRIPT = new DefaultRedisScript<>();
        DECREASE_GOOD_STOCK_SCRIPT.setLocation(new ClassPathResource("/lua/decrease_good_stock.lua"));
        DECREASE_GOOD_STOCK_SCRIPT.setResultType(Long.class);

        INCREASE_GOOD_STOCK_SCRIPT = new DefaultRedisScript<>();
        INCREASE_GOOD_STOCK_SCRIPT.setLocation(new ClassPathResource("/lua/increase_good_stock.lua"));
        INCREASE_GOOD_STOCK_SCRIPT.setResultType(Long.class);

        INIT_OR_ALIGN_ITEM_STOCK_SCRIPT = new DefaultRedisScript<>();
        INIT_OR_ALIGN_ITEM_STOCK_SCRIPT.setLocation(new ClassPathResource("/lua/init_or_align_good_stock.lua"));
        INIT_OR_ALIGN_ITEM_STOCK_SCRIPT.setResultType(Long.class);
    }

    /**
     * 校准库存
     *
     * @param itemId
     * @return
     */
    @Override
    public boolean alignItemStocks(Long activity, Long itemId) {
        if (itemId == null) {
            logger.info("秒杀品不存在|{}", itemId);
            return false;
        }
        try {
            SeckillGoodResponse seckillGoodResponse = seckillGoodClient.getSeckillGood(-1L, activity, itemId).getData();
            if (seckillGoodResponse == null) {
                logger.info("秒杀品不存在|{}", itemId);
                return false;
            }

            Integer initialStock = seckillGoodResponse.getInitialStock();
            if (initialStock == null) {
                logger.info("秒杀品未设置库存|{}", itemId);
                return false;
            }

            // 获取锁
            String goodsStockCacheKey = getGoodsStockCacheKey(itemId);
            String goodsStockCacheAlignKey = getGoodsStockCacheAlignKey(itemId);

            ArrayList<String> keys = Lists.newArrayList(goodsStockCacheKey, goodsStockCacheAlignKey);
            Long result = redisCacheService.getRedisTemplate().execute(INIT_OR_ALIGN_ITEM_STOCK_SCRIPT, keys, initialStock);
            if (result == null) {
                logger.info("alignItemStocks|秒杀品库存校准失败|{},{},{}", itemId, goodsStockCacheKey, initialStock);
                return false;
            }
            if (result == -997) {
                logger.info("alignItemStocks|已在校准中，本次校准取消|{},{},{},{}", result, itemId, goodsStockCacheKey, initialStock);
                return true;
            }
            if (result == 1) {
                logger.info("alignItemStocks|秒杀品库存校准完成|{},{},{},{}", result, itemId, goodsStockCacheKey, initialStock);
                return true;
            }
        } catch (Exception e) {
            logger.error("alignItemStocks|秒杀品库存校准错误|{}", itemId, e);
            return false;
        }
        return false;
    }

    /**
     * 预减库存
     *
     * @param stockDeduction
     * @return
     */
    @Override
    public boolean decreaseGoodStock(StockDeduction stockDeduction) {
        try {
            String goodsStockCacheKey = getGoodsStockCacheKey(stockDeduction.getItemId());
            String goodsStockCacheAlignKey = getGoodsStockCacheAlignKey(stockDeduction.getItemId());
            ArrayList<String> keys = Lists.newArrayList(goodsStockCacheKey, goodsStockCacheAlignKey);

            Long result = null;
            long startTime = System.currentTimeMillis();
            while ((result == null || result == IN_STOCK_ALIGNING) && (System.currentTimeMillis() - startTime < 1500)) {
                result = redisCacheService.getRedisTemplate().execute(DECREASE_GOOD_STOCK_SCRIPT, keys, stockDeduction.getQuantity());
                if (result == null) {
                    logger.info("库存扣减失败|{}", goodsStockCacheKey);
                    return false;
                }

                if (result == IN_STOCK_ALIGNING) {
                    logger.info("库存校准中|{}", goodsStockCacheKey);
                    Thread.sleep(20);
                }

                if (result == -3) {
                    logger.info("库存小于当前秒杀数量|{}", goodsStockCacheAlignKey);
                    return false;
                }

                if (result == -1 || result == -2) {
                    logger.info("库存扣减失败|{}", goodsStockCacheKey);
                    return false;
                }

                if (result == 1) {
                    logger.info("库存扣减成功|{}", goodsStockCacheAlignKey);
                    return true;
                }

            }
        } catch (Exception e) {
            logger.info("库存扣减失败", e);
            return false;
        }
        return false;
    }

    /**
     * 恢复库存
     *
     * @param stockDeduction
     * @return
     */
    @Override
    public boolean increaseGoodStock(StockDeduction stockDeduction) {
        try {
            String goodsStockCacheKey = getGoodsStockCacheKey(stockDeduction.getItemId());
            String goodsStockCacheAlignKey = getGoodsStockCacheAlignKey(stockDeduction.getItemId());

            ArrayList<String> keys = Lists.newArrayList(goodsStockCacheKey, goodsStockCacheAlignKey);
            Long result = null;
            long startTime = System.currentTimeMillis();

            while ((result == null || result == IN_STOCK_ALIGNING) && System.currentTimeMillis() - startTime < 1500) {
                result = redisCacheService.getRedisTemplate().execute(INCREASE_GOOD_STOCK_SCRIPT, keys, stockDeduction.getQuantity());

                if (result == null) {
                    logger.info("恢复库存失败|{}", goodsStockCacheKey);
                    return false;
                }

                if (result == -1) {
                    logger.info("恢复库存失败|{}", goodsStockCacheKey);
                    return false;
                }

                if (result == IN_STOCK_ALIGNING) {
                    logger.info("校准库存中|{}", goodsStockCacheKey);
                    Thread.sleep(20);
                }

                if (result == 1) {
                    logger.info("increaseItemStock|库存增加成功|{}", goodsStockCacheKey);
                    return true;
                }
            }
        } catch (Exception e) {
            logger.info("恢复库存失败", e);
            return false;
        }
        return false;
    }

    @Override
    public GoodStockCache getAvailableItemStock(Long userId, Long itemId) {

        // 从本地缓存获取
        GoodStockCache goodStockCache = goodStockLocalCache.getIfPresent(itemId);
        if (goodStockCache != null) {
            return goodStockCache;
        }

        // 从远程缓存获取
        Integer availableStock = redisCacheService.getInteger(getGoodsStockCacheKey(itemId));
        //Integer availableStock = (Integer) redisCacheService.getRedisTemplate().opsForValue().get(getGoodsStockCacheKey(itemId));
        if (availableStock == null) {
            return null;
        }


        // 返回对象
        goodStockCache = new GoodStockCache().with(availableStock);
        goodStockLocalCache.put(itemId, goodStockCache);
        return goodStockCache;
    }


    public static String getGoodsStockCacheAlignKey(Long itemId) {
        return ITEM_STOCK_ALIGN_LOCK_KEY + itemId;
    }

    public static String getGoodsStockCacheKey(Long itemId) {
        return ITEM_STOCKS_CACHE_KEY + itemId;
    }
}
