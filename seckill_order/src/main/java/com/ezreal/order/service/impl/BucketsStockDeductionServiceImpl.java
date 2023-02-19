package com.ezreal.order.service.impl;

import com.alibaba.fastjson.JSON;
import com.ezreal.common.ErrorCode;
import com.ezreal.common.model.domain.SeckillBucket;
import com.ezreal.common.model.domain.StockDeduction;
import com.ezreal.exception.BusinessException;
import com.ezreal.order.mapper.SeckillBucketMapper;
import com.ezreal.order.service.GoodStockDeductionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "place_order_type", havingValue = "buckets", matchIfMissing = true)
public class BucketsStockDeductionServiceImpl implements GoodStockDeductionService {

    private Logger logger = LoggerFactory.getLogger(BucketsStockDeductionServiceImpl.class);

    @Autowired
    private SeckillBucketMapper seckillBucketMapper;

    @Override
    public boolean decreaseItemStock(StockDeduction stockDeduction) {
        logger.info("decreaseItemStock|扣减库存|{}", JSON.toJSONString(stockDeduction));
        if (stockDeduction == null || stockDeduction.getItemId() == null || stockDeduction.getQuantity() == null || stockDeduction.getSerialNo() == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAMS);
        }

        SeckillBucket seckillBucket = seckillBucketMapper.selectByItemIdAndSerialNo(stockDeduction.getItemId(), stockDeduction.getSerialNo());
        Integer oldAvailableStocksAmount = seckillBucket.getAvailableStocksAmount();

        int update = seckillBucketMapper.decreaseBucketStock(seckillBucket.getItemId(),
                seckillBucket.getSerialNo(),
                stockDeduction.getQuantity(),
                oldAvailableStocksAmount);
        if (update < 1) {
            logger.info("decreaseItemStock|乐观锁扣减库存失败|{}", JSON.toJSONString(stockDeduction));
            return false;
        }
        logger.info("decreaseItemStock|扣减库存成功|{}", JSON.toJSONString(stockDeduction));
        return true;
    }

    @Override
    public boolean increaseItemStock(StockDeduction stockDeduction) {
        logger.info("decreaseItemStock|恢复库存|{}", JSON.toJSONString(stockDeduction));
        if (stockDeduction == null || stockDeduction.getItemId() == null || stockDeduction.getQuantity() == null || stockDeduction.getSerialNo() == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAMS);
        }

        SeckillBucket seckillBucket = seckillBucketMapper.selectByItemIdAndSerialNo(stockDeduction.getItemId(), stockDeduction.getSerialNo());
        Integer oldAvailableStocksAmount = seckillBucket.getAvailableStocksAmount();

        int update = seckillBucketMapper.increaseBucketStock(seckillBucket.getItemId(),
                seckillBucket.getSerialNo(),
                stockDeduction.getQuantity(),
                oldAvailableStocksAmount);
        if (update < 1) {
            logger.info("decreaseItemStock|乐观锁恢复库存失败|{}", JSON.toJSONString(stockDeduction));
            return false;
        }
        logger.info("decreaseItemStock|恢复库存成功|{}", JSON.toJSONString(stockDeduction));
        return true;
    }
}
