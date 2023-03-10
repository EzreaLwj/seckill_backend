package com.ezreal.order.service.impl.bucket;

import com.alibaba.fastjson.JSON;
import com.ezreal.common.ErrorCode;
import com.ezreal.common.cache.redis.DistributedCacheService;
import com.ezreal.common.lock.redisson.DistributedLock;
import com.ezreal.common.lock.redisson.DistributedLockFactoryService;
import com.ezreal.common.model.builder.SeckillBucketBuilder;
import com.ezreal.common.model.domain.SeckillBucket;
import com.ezreal.common.model.enums.ArrangementMode;
import com.ezreal.common.model.enums.SeckillBucketStatus;
import com.ezreal.common.model.response.bucket.MultiSeckillBucketResponse;
import com.ezreal.common.model.response.bucket.SeckillBucketResponse;
import com.ezreal.exception.BusinessException;
import com.ezreal.exception.StockBucketException;

import com.ezreal.order.mapper.SeckillBucketMapper;
import com.ezreal.order.service.BucketsArrangementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.ezreal.common.constant.CacheConstant.*;

@Service
@ConditionalOnProperty(name = "place_order_type", havingValue = "buckets", matchIfMissing = true)
public class BucketsArrangementServiceImpl implements BucketsArrangementService {

    private Logger logger = LoggerFactory.getLogger(BucketsArrangementServiceImpl.class);

    @Autowired
    private SeckillBucketMapper seckillBucketMapper;

    @Resource
    private DistributedLockFactoryService distributedLockFactoryService;

    @Resource
    private DataSourceTransactionManager dataSourceTransactionManager;

    @Resource
    private TransactionDefinition transactionDefinition;

    @Resource
    private DistributedCacheService distributedCacheService;

    /**
     * ?????????????????????????????????
     *
     * @param itemId
     * @return
     */
    @Override
    public MultiSeckillBucketResponse queryStockBucketsSummary(Long itemId) {
        if (itemId == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAMS);
        }
        List<SeckillBucket> seckillBuckets = seckillBucketMapper.selectByItemId(itemId);
        int remainAvailableStocks = seckillBuckets.stream()
                .mapToInt(SeckillBucket::getAvailableStocksAmount)
                .sum();

        // todo ???????????????
        Optional<SeckillBucket> primaryBucketOptional = seckillBuckets.stream()
                .filter(SeckillBucket::isPrimarySeckillBucket)
                .findFirst();

        if (!primaryBucketOptional.isPresent()) {
            return new MultiSeckillBucketResponse();
        }

        List<SeckillBucketResponse> seckillBucketResponseList = seckillBuckets.stream()
                .map(SeckillBucketBuilder::toResponse)
                .collect(Collectors.toList());

        return new MultiSeckillBucketResponse()
                .setBuckets(seckillBucketResponseList)
                .setAvailableStocksAmount(remainAvailableStocks)
                .setTotalStocksAmount(primaryBucketOptional.get().getTotalStocksAmount());
    }

    /**
     * ?????????????????????????????????
     *
     * @param itemId
     * @param totalStocksAmount
     * @param bucketsQuantity
     * @param arrangementMode
     */
    @Override
    public void arrangeStockBuckets(Long itemId, Integer totalStocksAmount, Integer bucketsQuantity, Integer arrangementMode) {
        logger.info("arrangeBuckets|??????????????????|{},{},{}", itemId, totalStocksAmount, bucketsQuantity);
        if (itemId == null || totalStocksAmount == null || totalStocksAmount < 0 || bucketsQuantity == null || bucketsQuantity <= 0) {
            throw new StockBucketException(ErrorCode.INVALID_PARAMS);
        }
        // ???????????????????????????itemId????????????
        DistributedLock distributedLock = distributedLockFactoryService.getDistributedLock(ITEM_STOCK_BUCKETS_SUSPEND_KEY + itemId);

        try {
            boolean tryLock = distributedLock.tryLock(5, 5, TimeUnit.SECONDS);
            if (!tryLock) {
                logger.info("arrangeStockBuckets|???????????????|{}", itemId);
                return;
            }

            // ??????????????????
            TransactionStatus transactionStatus = dataSourceTransactionManager.getTransaction(transactionDefinition);
            try {
                // ?????????????????????
                logger.info("suspendBuckets|??????????????????|{}", itemId);
                int updateStatusByItemId = seckillBucketMapper.updateStatusByItemId(itemId, SeckillBucketStatus.DISABLED.getCode());
                if (updateStatusByItemId < 0) {
                    logger.info("arrangeBuckets|????????????????????????|{}", itemId);
                    throw new StockBucketException(ErrorCode.ARRANGE_STOCK_BUCKETS_FAILED);
                }
                logger.info("suspendBuckets|?????????????????????|{}", itemId);
                dataSourceTransactionManager.commit(transactionStatus);
            } catch (Exception e) {
                logger.info("arrangeBuckets|???????????????????????????|{}", itemId, e);
                dataSourceTransactionManager.rollback(transactionStatus);
            }


            List<SeckillBucket> seckillBuckets = seckillBucketMapper.selectByItemId(itemId);
            if (seckillBuckets == null || seckillBuckets.size() == 0) {
                initStockBuckets(itemId, totalStocksAmount, bucketsQuantity);
                return;
            }

            // ??????????????????
            if (ArrangementMode.isTotalAmountMode(arrangementMode)) {
                arrangeStockBucketsBasedTotalMode(itemId, totalStocksAmount, bucketsQuantity, seckillBuckets);
            }

            // ??????????????????
            if (ArrangementMode.isIncrementalAmountMode(arrangementMode)) {
                rearrangeStockBucketsBasedIncrementalMode(itemId, totalStocksAmount, bucketsQuantity, seckillBuckets);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * ?????????????????????
     *
     * @param itemId
     * @param totalStocksAmount
     * @param bucketsQuantity
     */
    private void initStockBuckets(Long itemId, Integer totalStocksAmount, Integer bucketsQuantity) {
        SeckillBucket primaryBucket = new SeckillBucket()
                .initPrimary()
                .setItemId(itemId)
                .setTotalStocksAmount(totalStocksAmount);
        List<SeckillBucket> presentBuckets = buildBuckets(itemId, totalStocksAmount, bucketsQuantity, primaryBucket);
        submitBucketsToArrange(itemId, presentBuckets);
    }

    /**
     * ??????????????????
     *
     * @param itemId
     * @param totalStocksAmount
     * @param bucketsQuantity
     * @param primaryBucket
     * @return
     */
    private List<SeckillBucket> buildBuckets(Long itemId, Integer totalStocksAmount, Integer bucketsQuantity, SeckillBucket primaryBucket) {
        if (itemId == null || totalStocksAmount == null || bucketsQuantity == null || bucketsQuantity <= 0) {
            throw new StockBucketException(ErrorCode.INVALID_PARAMS);
        }

        List<SeckillBucket> seckillBucketList = new ArrayList<>();
        Integer averageStockAmount = totalStocksAmount / bucketsQuantity;
        Integer remainStockAmount = totalStocksAmount % bucketsQuantity;
        for (int i = 0; i < bucketsQuantity; i++) {
            if (i == 0) {
                if (primaryBucket == null) {
                    primaryBucket = new SeckillBucket();
                }
                primaryBucket
                        .setAvailableStocksAmount(averageStockAmount)
                        .setSerialNo(i)
                        .setStatus(SeckillBucketStatus.ENABLED.getCode());
                seckillBucketList.add(primaryBucket);
                continue;
            }
            SeckillBucket seckillBucket = new SeckillBucket()
                    .setSerialNo(i)
                    .setStatus(SeckillBucketStatus.ENABLED.getCode())
                    .setItemId(itemId);

            if (i < bucketsQuantity - 1) {
                seckillBucket.setAvailableStocksAmount(averageStockAmount)
                        .setTotalStocksAmount(averageStockAmount);
            }

            if (i == bucketsQuantity - 1) {
                Integer restAvailableStocksAmount = averageStockAmount + remainStockAmount;
                seckillBucket.setAvailableStocksAmount(restAvailableStocksAmount)
                        .setTotalStocksAmount(restAvailableStocksAmount);

            }
            seckillBucketList.add(seckillBucket);
        }
        return seckillBucketList;
    }

    /**
     * ??????????????????
     *
     * @param itemId
     * @param presentBuckets
     */
    private void submitBucketsToArrange(Long itemId, List<SeckillBucket> presentBuckets) {
        logger.info("arrangeBuckets|??????????????????|{},{}", itemId, JSON.toJSONString(presentBuckets));
        if (itemId == null || itemId <= 0 || CollectionUtils.isEmpty(presentBuckets)) {
            logger.info("arrangeBuckets|????????????????????????|{}", itemId);
            throw new BusinessException(ErrorCode.INVALID_PARAMS);
        }

        Optional<SeckillBucket> primaryBucketOptional = presentBuckets.stream().filter(SeckillBucket::isPrimarySeckillBucket).findFirst();
        // ????????????????????????
        if (!primaryBucketOptional.isPresent()) {
            throw new StockBucketException(ErrorCode.PRIMARY_BUCKET_IS_MISSING);
        }

        // ????????????????????????
        if (presentBuckets.stream().filter(SeckillBucket::isPrimarySeckillBucket).count() > 1) {
            throw new StockBucketException(ErrorCode.MULTI_PRIMARY_BUCKETS_FOUND_BUT_EXPECT_ONE);
        }

        // ????????????
        presentBuckets.forEach((seckillBucket) -> {
            if (seckillBucket.getAvailableStocksAmount() == null || seckillBucket.getAvailableStocksAmount() < 0) {
                throw new StockBucketException(ErrorCode.AVAILABLE_STOCKS_AMOUNT_INVALID);
            }

            if (seckillBucket.getTotalStocksAmount() == null || seckillBucket.getTotalStocksAmount() < 0) {
                throw new StockBucketException(ErrorCode.TOTAL_STOCKS_AMOUNT_INVALID);
            }

            if (!seckillBucket.getTotalStocksAmount().equals(seckillBucket.getAvailableStocksAmount()) && seckillBucket.isSubSeckillBucket()) {
                throw new StockBucketException(ErrorCode.AVAILABLE_STOCKS_AMOUNT_NOT_EQUALS_TO_TOTAL_STOCKS_AMOUNT);
            }

            if (!seckillBucket.getItemId().equals(itemId)) {
                throw new StockBucketException(ErrorCode.STOCK_BUCKET_ITEM_INVALID);
            }
        });

        // ??????????????????
        seckillBucketMapper.deleteById(itemId);
        int insertBatch = seckillBucketMapper.insertBatch(presentBuckets);
        if (insertBatch > 1) {
            // ????????????
            presentBuckets.forEach((seckillBucket -> {
                distributedCacheService.put(getBucketAvailableStocksCacheKey(itemId, seckillBucket.getSerialNo()), seckillBucket.getAvailableStocksAmount());
                distributedCacheService.put(getItemStockBucketsQuantityCacheKey(itemId), presentBuckets.size());
            }));
        } else {
            logger.info("submitBucketsToArrange|??????????????????|{}, {}", itemId, JSON.toJSONString(presentBuckets));
            throw new StockBucketException(ErrorCode.ARRANGE_STOCK_BUCKETS_FAILED);
        }
    }

    /**
     * ????????????????????????
     *
     * @param itemId
     * @param totalStocksAmount
     * @param bucketsQuantity
     * @param seckillBuckets
     */
    private void arrangeStockBucketsBasedTotalMode(Long itemId, Integer totalStocksAmount, Integer bucketsQuantity, List<SeckillBucket> existingBuckets) {
        // ?????????????????????????????????
        int remainAvailableStocks = existingBuckets.stream()
                .filter(SeckillBucket::isSubSeckillBucket)
                .mapToInt(SeckillBucket::getAvailableStocksAmount).sum();
        Optional<SeckillBucket> optionalSeckillBucket = existingBuckets.stream().filter(SeckillBucket::isPrimarySeckillBucket).findFirst();
        if (!optionalSeckillBucket.isPresent()) {
            throw new StockBucketException(ErrorCode.PRIMARY_BUCKET_IS_MISSING);
        }

        // ???????????????????????????
        SeckillBucket primarySeckillBucket = optionalSeckillBucket.get();
        primarySeckillBucket.addAvailableStocks(remainAvailableStocks);
        // ??????????????????
        int soldStocksAmount = primarySeckillBucket.getTotalStocksAmount() - primarySeckillBucket.getAvailableStocksAmount();

        if (soldStocksAmount > totalStocksAmount) {
            throw new StockBucketException(799, "?????????????????????????????????????????????");
        }

        // ?????????????????????????????????
        primarySeckillBucket.setTotalStocksAmount(totalStocksAmount);
        List<SeckillBucket> seckillBucketList = buildBuckets(itemId, totalStocksAmount, bucketsQuantity, primarySeckillBucket);
        submitBucketsToArrange(itemId, seckillBucketList);
    }


    /**
     * ????????????????????????
     *
     * @param itemId
     * @param totalStocksAmount
     * @param bucketsQuantity
     * @param seckillBuckets
     */
    private void rearrangeStockBucketsBasedIncrementalMode(Long itemId, Integer incrementalStocksAmount, Integer bucketsQuantity, List<SeckillBucket> existingBuckets) {
        Optional<SeckillBucket> optionalSeckillBucket = existingBuckets.stream().filter(SeckillBucket::isPrimarySeckillBucket).findFirst();
        if (!optionalSeckillBucket.isPresent()) {
            throw new StockBucketException(ErrorCode.PRIMARY_BUCKET_IS_MISSING);
        }

        // ?????????????????? (?????????????????????????????????????????????)
        int remainAvailableStocks = existingBuckets.stream().mapToInt(SeckillBucket::getAvailableStocksAmount).sum();

        // ???????????????????????????
        Integer totalAvailableStocks = remainAvailableStocks + incrementalStocksAmount;
        int presentAvailableStocks = remainAvailableStocks + incrementalStocksAmount;

        if (presentAvailableStocks < 0) {
            throw new StockBucketException(ErrorCode.STOCK_NOT_ENOUGH);
        }

        SeckillBucket primarySeckillBucket = optionalSeckillBucket.get();
        primarySeckillBucket.increaseTotalStocksAmount(incrementalStocksAmount);

        List<SeckillBucket> seckillBucketList = buildBuckets(itemId, totalAvailableStocks, bucketsQuantity, primarySeckillBucket);
        submitBucketsToArrange(itemId, seckillBucketList);
    }

    /**
     * ??????????????????????????????
     *
     * @param itemId
     * @param serialNumber
     * @return
     */
    public static String getBucketAvailableStocksCacheKey(Long itemId, Integer serialNumber) {
        return ITEM_BUCKET_AVAILABLE_STOCKS_KEY + "_" + itemId + "_" + serialNumber;
    }

    public static String getItemStockBucketsSuspendKey(Long itemId) {
        return ITEM_STOCK_BUCKETS_SUSPEND_KEY + "_" + itemId;
    }

    /**
     * ?????????????????????????????????
     *
     * @param itemId
     * @return
     */
    public static String getItemStockBucketsQuantityCacheKey(Long itemId) {
        return ITEM_BUCKETS_QUANTITY_KEY + "_" + itemId;
    }


}
