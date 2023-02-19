package com.ezreal.goods.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ezreal.clients.SeckillActivityClient;
import com.ezreal.common.BaseResponse;
import com.ezreal.common.ErrorCode;
import com.ezreal.common.ResultUtils;
import com.ezreal.common.SuccessCode;
import com.ezreal.common.constant.LockKeyConstant;
import com.ezreal.common.lock.redisson.DistributedLock;
import com.ezreal.common.lock.redisson.DistributedLockFactoryService;
import com.ezreal.common.model.builder.SeckillGoodBuilder;
import com.ezreal.common.model.cahce.SeckillGoodCache;
import com.ezreal.common.model.cahce.SeckillGoodsCache;
import com.ezreal.common.model.domain.SeckillGood;
import com.ezreal.common.model.enums.SeckillGoodStatus;
import com.ezreal.common.model.query.SeckillGoodQuery;
import com.ezreal.common.model.request.PublishSeckillGoodRequest;
import com.ezreal.common.model.request.UpdateSeckillGoodRequest;
import com.ezreal.common.model.response.good.MultiSeckillGoodsResponse;
import com.ezreal.common.model.response.avtivity.SeckillActivitiesResponse;
import com.ezreal.common.model.response.good.SeckillGoodResponse;
import com.ezreal.exception.BusinessException;
import com.ezreal.goods.mapper.SeckillGoodMapper;
import com.ezreal.goods.service.SeckillGoodCacheService;
import com.ezreal.goods.service.SeckillGoodService;
import com.ezreal.goods.service.SeckillGoodsCacheService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author Ezreal
 * @description 针对表【seckill_good(秒杀品)】的数据库操作Service实现
 * @createDate 2023-01-03 00:17:00
 */
@Service
@Slf4j
public class SeckillGoodServiceImpl extends ServiceImpl<SeckillGoodMapper, SeckillGood>
        implements SeckillGoodService {

    private final static Logger logger = LoggerFactory.getLogger(SeckillGoodServiceImpl.class);

    @Autowired
    private SeckillActivityClient seckillActivityClient;

    @Autowired
    private SeckillGoodMapper seckillGoodMapper;

    @Resource
    private DistributedLockFactoryService distributedLockFactoryService;

    @Autowired
    private SeckillGoodCacheService seckillGoodCacheService;

    @Resource
    private SeckillGoodsCacheService seckillGoodsCacheService;

    @Override
    public BaseResponse<SuccessCode> publishSeckillGood(Long userId,
                                                        Long activityId,
                                                        PublishSeckillGoodRequest publishSeckillGoodRequest) {
        if (userId == null || activityId == null || publishSeckillGoodRequest == null || !publishSeckillGoodRequest.validate()) {
            throw new BusinessException(ErrorCode.INVALID_PARAMS);
        }

        // 加锁防抖
        DistributedLock distributedLock = distributedLockFactoryService.getDistributedLock(getItemCreateLockKey(userId));

        try {
            boolean isSuccessLock = distributedLock.tryLock(500, 1000, TimeUnit.MILLISECONDS);

            if (!isSuccessLock) {
                log.info("发布秒杀品失败|{}", userId);
                // 操作频繁 稍后再试
                throw new BusinessException(ErrorCode.FREQUENTLY_ERROR);
            }

            // 远程调用
            BaseResponse<SeckillActivitiesResponse> seckillActivity = seckillActivityClient.getSeckillActivity(userId, activityId);
            SeckillActivitiesResponse seckillActivitiesResponse = seckillActivity.getData();

            log.info("秒杀活动为|{}", seckillActivitiesResponse);
            if (seckillActivitiesResponse == null) {
                log.info("秒杀活动不存在|{}", activityId);
                throw new BusinessException(ErrorCode.ACTIVITY_NOT_FOUND);
            }

            SeckillGood seckillGood = SeckillGoodBuilder.toDomain(publishSeckillGoodRequest);
            seckillGood.setActivityId(activityId);
            seckillGood.setStockWarmUp(0);
            seckillGoodMapper.insert(seckillGood);
            log.info("秒杀品已经发布");
            return ResultUtils.success(SuccessCode.GOOD_PUBLISH_SUCCESS);

        } catch (InterruptedException e) {
            log.info("发布秒杀品失败|{}", userId, e);
            throw new BusinessException(ErrorCode.GOOD_ADD_ERROR);
        } finally {
            distributedLock.unlock();
        }
    }

    @Override
    public BaseResponse<SuccessCode> onlineSeckillGood(Long userId, Long activityId, Long itemId) {
        if (userId == null || activityId == null || itemId == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAMS);
        }
        DistributedLock modifyLock = distributedLockFactoryService.getDistributedLock(getItemModificationLockKey(userId));
        try {
            boolean isSuccessLock = modifyLock.tryLock(100, 500, TimeUnit.MILLISECONDS);
            if (!isSuccessLock) {
                log.info("上线秒杀品失败|{}", userId);
                // 操作频繁 稍后再试
                throw new BusinessException(ErrorCode.FREQUENTLY_ERROR);
            }

            SeckillGood seckillGood = seckillGoodMapper.selectById(itemId);

            if (seckillGood == null) {
                throw new BusinessException(ErrorCode.GOOD_NOT_FOUND);
            }

            if (SeckillGoodStatus.isOnline(seckillGood.getStatus())) {
                log.info("秒杀品上线|{}, {}", activityId, itemId);
                return ResultUtils.success(SuccessCode.GOOD_ONLINE_SUCCESS);
            }


            seckillGood.setStatus(SeckillGoodStatus.ONLINE.getCode());
            seckillGoodMapper.updateById(seckillGood);
            log.info("秒杀品上线|{}, {}", activityId, itemId);

            return ResultUtils.success(SuccessCode.GOOD_ONLINE_SUCCESS);

        } catch (InterruptedException e) {
            log.info("上线秒杀品失败|{}", userId);
            // 操作频繁 稍后再试
            throw new BusinessException(ErrorCode.GOOD_ONLINE_ERROR);
        } finally {
            modifyLock.unlock();
        }
    }

    @Override
    public BaseResponse<SuccessCode> offlineSeckillGood(Long userId, Long activityId, Long itemId) {
        if (userId == null || activityId == null || itemId == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAMS);
        }
        DistributedLock modifyLock = distributedLockFactoryService.getDistributedLock(getItemModificationLockKey(userId));
        try {
            boolean isSuccessLock = modifyLock.tryLock(500, 1000, TimeUnit.MILLISECONDS);
            if (!isSuccessLock) {
                log.info("下线秒杀品失败|{}", userId);
                // 操作频繁 稍后再试
                throw new BusinessException(ErrorCode.FREQUENTLY_ERROR);
            }

            SeckillGood seckillGood = seckillGoodMapper.selectById(itemId);

            if (seckillGood == null) {
                throw new BusinessException(ErrorCode.GOOD_NOT_FOUND);
            }

            if (SeckillGoodStatus.isOffline(seckillGood.getStatus())) {
                log.info("秒杀品下线|{}, {}", activityId, itemId);
                return ResultUtils.success(SuccessCode.GOOD_OFFLINE_SUCCESS);
            }


            seckillGood.setStatus(SeckillGoodStatus.OFFLINE.getCode());
            seckillGoodMapper.updateById(seckillGood);
            log.info("秒杀品下线|{}, {}", activityId, itemId);

            return ResultUtils.success(SuccessCode.GOOD_OFFLINE_SUCCESS);
        } catch (InterruptedException e) {
            log.info("下线秒杀品失败|{}", userId);
            // 操作频繁 稍后再试
            throw new BusinessException(ErrorCode.GOOD_OFFLINE_ERROR);
        } finally {
            modifyLock.unlock();
        }
    }

    /**
     * 获取单件秒杀品
     *
     * @param userId
     * @param activityId
     * @param itemId
     * @return
     */
    @Override
    public BaseResponse<SeckillGoodResponse> getSeckillGood(Long userId, Long activityId, Long itemId, Long version) {
        if (userId == null || activityId == null || itemId == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAMS);
        }
        // 从缓存中获取
        SeckillGoodCache seckillGoodCache = seckillGoodCacheService.getSeckillGoodCache(activityId, itemId, version);

        // 没找到
        if (!seckillGoodCache.isExist() || seckillGoodCache.getSeckillGood() == null) {
            throw new BusinessException(ErrorCode.GOOD_NOT_FOUND);
        }

        // 正在更新稍后重试
        if (seckillGoodCache.isLater()) {
            return BaseResponse.tryLater();
        }

        log.info("获取指定秒杀品|{}, {}", activityId, itemId);
        SeckillGoodResponse seckillGoodResponse = SeckillGoodBuilder.toResponse(seckillGoodCache.getSeckillGood());
        seckillGoodResponse.setVersion(seckillGoodCache.getVersion());
        return ResultUtils.success(seckillGoodResponse);
    }

    @Override
    public BaseResponse<MultiSeckillGoodsResponse> getSeckillGoods(Long userId, Long activityId, SeckillGoodQuery seckillGoodQuery) {
        if (userId == null || activityId == null || seckillGoodQuery.getPageNum() == null || seckillGoodQuery.getPageSize() == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAMS);
        }

        Page<SeckillGood> seckillGoodPage = new Page<>(seckillGoodQuery.getPageNum(), seckillGoodQuery.getPageSize());

        LambdaQueryWrapper<SeckillGood> seckillGoodLambdaQueryWrapper = new LambdaQueryWrapper<>();
        seckillGoodLambdaQueryWrapper.like(!StringUtils.isEmpty(seckillGoodQuery.getKeyword()),
                SeckillGood::getItemTitle,
                seckillGoodQuery.getKeyword());
        seckillGoodLambdaQueryWrapper.eq(SeckillGood::getActivityId, activityId);

        seckillGoodMapper.selectPage(seckillGoodPage, seckillGoodLambdaQueryWrapper);
        log.info("获取指定活动秒杀品| {}", activityId);
        List<SeckillGood> records = seckillGoodPage.getRecords();
        List<SeckillGoodResponse> seckillGoodResponses = records.stream().map(SeckillGoodBuilder::toResponse).collect(Collectors.toList());

        MultiSeckillGoodsResponse multiSeckillGoodsResponse = new MultiSeckillGoodsResponse()
                .setSeckillGoodResponses(seckillGoodResponses)
                .setTotal(seckillGoodPage.getTotal());

        return ResultUtils.success(multiSeckillGoodsResponse);
    }

    @Override
    public BaseResponse<MultiSeckillGoodsResponse> getOnlineSeckillGoods(Long userId, Long activityId, SeckillGoodQuery seckillGoodQuery) {
        if (activityId == null || seckillGoodQuery.getPageNum() == null || seckillGoodQuery.getPageSize() == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAMS);
        }
        // 如果是首次访问
        List<SeckillGood> seckillGoods = null;
        Long total = null;
        if (seckillGoodQuery.isFirstPureQuery()) {
            SeckillGoodsCache seckillGoodsCache = seckillGoodsCacheService.getSeckillGoodsCache(userId, activityId, seckillGoodQuery.getVersion());

            if (seckillGoodsCache.isLater()) {
                return BaseResponse.tryLater();
            }

            if (seckillGoodsCache.isEmpty()) {
                return BaseResponse.empty();
            }
            seckillGoods = seckillGoodsCache.getSeckillGoods();
            total = Long.valueOf(seckillGoodsCache.getTotal());

        } else {

            Page<SeckillGood> seckillGoodPage = new Page<>(seckillGoodQuery.getPageNum(), seckillGoodQuery.getPageSize());
            LambdaQueryWrapper<SeckillGood> seckillGoodLambdaQueryWrapper = new LambdaQueryWrapper<>();
            seckillGoodLambdaQueryWrapper.like(!StringUtils.isEmpty(seckillGoodQuery.getKeyword()),
                    SeckillGood::getItemTitle,
                    seckillGoodQuery.getKeyword());
            seckillGoodLambdaQueryWrapper.eq(SeckillGood::getActivityId, activityId);
            seckillGoodLambdaQueryWrapper.eq(SeckillGood::getStatus, SeckillGoodStatus.ONLINE.getCode());

            seckillGoodMapper.selectPage(seckillGoodPage, seckillGoodLambdaQueryWrapper);
            log.info("获取指定活动秒杀品| {}", activityId);
            seckillGoods = seckillGoodPage.getRecords();
            total = seckillGoodPage.getTotal();
        }

        if (seckillGoods == null || seckillGoods.isEmpty()) {
            return BaseResponse.empty();
        }

        List<SeckillGoodResponse> seckillGoodResponses = seckillGoods.stream().map(SeckillGoodBuilder::toResponse).collect(Collectors.toList());
        MultiSeckillGoodsResponse multiSeckillGoodsResponse = new MultiSeckillGoodsResponse()
                .setSeckillGoodResponses(seckillGoodResponses)
                .setTotal(total);
        return ResultUtils.success(multiSeckillGoodsResponse);
    }

    @Override
    public BaseResponse<MultiSeckillGoodsResponse> getAllSeckillGoods(Long userId, SeckillGoodQuery seckillGoodQuery) {
        if (seckillGoodQuery == null) {
            seckillGoodQuery = new SeckillGoodQuery().buildParams();
        }

        if (seckillGoodQuery.getPageNum() == null || seckillGoodQuery.getPageSize() == null) {
            seckillGoodQuery.buildParams();
        }
        Page<SeckillGood> seckillGoodPage = new Page<>(seckillGoodQuery.getPageNum(), seckillGoodQuery.getPageSize());
        LambdaQueryWrapper<SeckillGood> seckillGoodLambdaQueryWrapper = new LambdaQueryWrapper<>();


        seckillGoodLambdaQueryWrapper.like(seckillGoodQuery.getKeyword() != null, SeckillGood::getItemTitle, seckillGoodQuery.getKeyword())
                .eq(seckillGoodQuery.getStatus() != null, SeckillGood::getStatus, seckillGoodQuery.getStatus())
                .eq(seckillGoodQuery.getStockWarmUp() != null, SeckillGood::getStockWarmUp, seckillGoodQuery.getStockWarmUp());


        seckillGoodMapper.selectPage(seckillGoodPage, seckillGoodLambdaQueryWrapper);

        List<SeckillGood> records = seckillGoodPage.getRecords();
        List<SeckillGoodResponse> seckillGoodResponseList = records.stream()
                .map(SeckillGoodBuilder::toResponse)
                .collect(Collectors.toList());
        MultiSeckillGoodsResponse multiSeckillGoodsResponse = new MultiSeckillGoodsResponse()
                .setSeckillGoodResponses(seckillGoodResponseList)
                .setTotal(seckillGoodPage.getTotal());

        return ResultUtils.success(multiSeckillGoodsResponse);
    }

    /**
     * 更新商品
     * @param userId
     * @param itemId
     * @param updateSeckillGoodRequest
     * @return
     */
    @Override
    public BaseResponse<SuccessCode> updateSeckillGood(Long userId, Long itemId, UpdateSeckillGoodRequest updateSeckillGoodRequest) {

        if (itemId == null || updateSeckillGoodRequest == null) {
            return ResultUtils.error(ErrorCode.INVALID_PARAMS);
        }
        SeckillGood seckillGood = seckillGoodMapper.selectById(itemId);
        if (seckillGood == null) {
            return ResultUtils.error(ErrorCode.GOOD_NOT_FOUND);
        }

        seckillGood = SeckillGoodBuilder.toDomain(updateSeckillGoodRequest);
        seckillGood.setId(itemId);
        seckillGoodMapper.updateById(seckillGood);
        logger.info("更改秒杀商品信息|{}, {}", userId, itemId);
        return ResultUtils.success(SuccessCode.GOOD_UPDATE_SUCCESS);
    }

    /**
     * 更新库存
     *
     * @param isDecrease
     * @param itemId
     * @param quantity
     * @return
     */
    @Override
    public BaseResponse<SuccessCode> updateSeckillGoodStock(boolean isDecrease, Long itemId, Integer quantity) {

        int oldAvailableStock = seckillGoodMapper.selectAvailableStockById(itemId);
        if (isDecrease) {
            int update = seckillGoodMapper.decreaseAvailableStockById(itemId, quantity, oldAvailableStock);
            if (update > 0) {
                return ResultUtils.success(SuccessCode.GOOD_STOCK_DECREASE_SUCCESS);
            }
        } else {
            int update = seckillGoodMapper.increaseAvailableStockById(itemId, quantity, oldAvailableStock);

            if (update > 0) {
                return ResultUtils.success(SuccessCode.GOOD_STOCK_INCREASE_SUCCESS);
            }
        }
        return ResultUtils.error(ErrorCode.FREQUENTLY_ERROR);
    }

    private String getItemCreateLockKey(Long userId) {
        return LockKeyConstant.GOOD_CREATE_LOCK_KEY.getKeyName() + userId;
    }

    private String getItemModificationLockKey(Long itemId) {
        return LockKeyConstant.GOOD_MODIFICATION_LOCK_KEY.getKeyName() + itemId;
    }
}




