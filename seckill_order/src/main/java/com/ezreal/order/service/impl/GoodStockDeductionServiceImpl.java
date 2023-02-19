package com.ezreal.order.service.impl;

import com.ezreal.clients.SeckillGoodClient;
import com.ezreal.common.BaseResponse;
import com.ezreal.common.ErrorCode;
import com.ezreal.common.SuccessCode;
import com.ezreal.common.model.domain.StockDeduction;
import com.ezreal.exception.BusinessException;
import com.ezreal.order.service.GoodStockDeductionService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@Slf4j
@ConditionalOnProperty(name = "place_order_type", havingValue = "normal", matchIfMissing = true)
public class GoodStockDeductionServiceImpl implements GoodStockDeductionService {

    private static final Logger logger = LoggerFactory.getLogger(GoodStockDeductionServiceImpl.class);

    @Resource
    private SeckillGoodClient seckillGoodClient;

    @Override
    public boolean decreaseItemStock(StockDeduction stockDeduction) {
        if (stockDeduction == null || stockDeduction.getItemId() == null || stockDeduction.getQuantity() == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAMS);
        }

        BaseResponse<SuccessCode> response =
                seckillGoodClient.updateSeckillGoodStock(stockDeduction.getUserId(), stockDeduction.getItemId(), stockDeduction.getQuantity(), true);

        if (response == null) {
            logger.info("返回库存扣减响应失败|{}", stockDeduction.getItemId());
            return false;
        }
        int code = response.getCode();
        if (code != 33) {
            logger.info("乐观锁更新失败|{}", stockDeduction.getItemId());
            return false;
        }
        return true;
    }

    @Override
    public boolean increaseItemStock(StockDeduction stockDeduction) {
        if (stockDeduction == null || stockDeduction.getItemId() == null || stockDeduction.getQuantity() == null) {
            throw new BusinessException(ErrorCode.INVALID_PARAMS);
        }

        BaseResponse<SuccessCode> response =
                seckillGoodClient.updateSeckillGoodStock(stockDeduction.getUserId(), stockDeduction.getItemId(), stockDeduction.getQuantity(), true);

        if (response == null) {
            logger.info("返回库存增加响应为空|{}", stockDeduction.getItemId());
            return false;
        }

        int code = response.getCode();
        if (code != 34) {
            logger.info("乐观锁更新失败|{}", stockDeduction.getItemId());
            return false;
        }
        return true;
    }
}
