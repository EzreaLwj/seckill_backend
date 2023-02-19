package com.ezreal.order.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ezreal.clients.SeckillGoodClient;
import com.ezreal.common.BaseResponse;
import com.ezreal.common.ErrorCode;
import com.ezreal.common.ResultUtils;
import com.ezreal.common.SuccessCode;
import com.ezreal.common.lock.redisson.DistributedLock;
import com.ezreal.common.lock.redisson.DistributedLockFactoryService;
import com.ezreal.common.model.domain.SeckillBucket;
import com.ezreal.common.model.request.SeckillBucketsArrangementRequest;
import com.ezreal.common.model.response.bucket.MultiSeckillBucketResponse;
import com.ezreal.common.model.response.good.SeckillGoodResponse;
import com.ezreal.exception.BusinessException;

import com.ezreal.order.mapper.SeckillBucketMapper;
import com.ezreal.order.service.BucketsArrangementService;
import com.ezreal.order.service.SeckillBucketService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * @author Ezreal
 * @description 针对表【seckill_bucket(秒杀品库存分桶表)】的数据库操作Service实现
 * @createDate 2023-01-17 21:52:11
 */
@Slf4j
@Service
public class SeckillBucketServiceImpl extends ServiceImpl<SeckillBucketMapper, SeckillBucket>
        implements SeckillBucketService {
    private static final String STOCK_BUCKET_ARRANGEMENT_KEY = "STOCK_BUCKET_ARRANGEMENT_KEY";
    private static final Logger logger = LoggerFactory.getLogger(SeckillBucketServiceImpl.class);

    @Autowired
    private BucketsArrangementService bucketsArrangementService;

    @Resource
    private DistributedLockFactoryService distributedLockFactoryService;

    @Resource
    private SeckillGoodClient seckillGoodClient;

    /**
     * 获取某件商品的分桶库存
     *
     * @param userId
     * @param itemId
     * @return
     */
    @Override
    public BaseResponse<MultiSeckillBucketResponse> getStockBucketsSummary(Long userId, Long itemId) {

        if (userId == null || itemId == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAMS);
        }
        try {
            MultiSeckillBucketResponse multiStockBucketResponse =
                    bucketsArrangementService.queryStockBucketsSummary(itemId);
            return ResultUtils.success(multiStockBucketResponse);

        } catch (BusinessException e) {
            logger.error("stockBucketsSummary|获取库存分桶数据失败|{}", itemId, e);
            return ResultUtils.error(ErrorCode.QUERY_STOCK_BUCKETS_FAILED);
        } catch (Exception e) {
            logger.error("stockBucketsSummary|获取库存分桶数据错误|{}", itemId, e);
            return ResultUtils.error(ErrorCode.QUERY_STOCK_BUCKETS_FAILED);
        }

    }

    /**
     * 为某件商品库存实现分桶
     *
     * @param userId
     * @param itemId
     * @param seckillBucketsArrangementRequest
     * @return
     */
    @Override
    public BaseResponse<SuccessCode> arrangeStockBuckets(Long userId, Long activityId, Long itemId, SeckillBucketsArrangementRequest seckillBucketsArrangementRequest) {
        logger.info("arrangeBuckets|编排库存分桶|{},{},{}", userId, itemId, JSON.toJSON(seckillBucketsArrangementRequest));
        String keyName = getArrangementKey(userId, itemId);
        DistributedLock distributedLock = distributedLockFactoryService.getDistributedLock(keyName);
        try {
            boolean tryLock = distributedLock.tryLock(5, 5, TimeUnit.SECONDS);
            if (!tryLock) {
                return ResultUtils.error(ErrorCode.FREQUENTLY_ERROR);
            }

            BaseResponse<SeckillGoodResponse> seckillGood = seckillGoodClient.getSeckillGood(userId, activityId, itemId);
            if (seckillGood == null) {
                return ResultUtils.error(ErrorCode.GOOD_NOT_FOUND);
            }

            bucketsArrangementService.arrangeStockBuckets(itemId, seckillBucketsArrangementRequest.getTotalStocksAmount(),
                    seckillBucketsArrangementRequest.getBucketsQuantity(), seckillBucketsArrangementRequest.getArrangementMode());

            logger.info("arrangeBuckets|库存编排完成|{}", itemId);
            return ResultUtils.success(SuccessCode.STOCK_ARRANGE_SUCCESS);
        } catch (BusinessException e) {
            logger.error("arrangeBuckets|库存编排错误|{}", itemId, e);
            return ResultUtils.error(ErrorCode.BUSINESS_ERROR);
        } catch (Exception e) {
            logger.error("arrangeBuckets|库存编排错误|{}", itemId, e);
            return ResultUtils.error(ErrorCode.ARRANGE_STOCK_BUCKETS_FAILED);
        } finally {
            distributedLock.unlock();
        }
    }


    private String getArrangementKey(Long userId, Long itemId) {
        return STOCK_BUCKET_ARRANGEMENT_KEY + "_" + userId + "_" + itemId;
    }
}




