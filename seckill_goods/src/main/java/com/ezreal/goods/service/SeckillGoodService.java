package com.ezreal.goods.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.ezreal.common.BaseResponse;
import com.ezreal.common.SuccessCode;
import com.ezreal.common.model.domain.SeckillGood;
import com.ezreal.common.model.query.SeckillGoodQuery;
import com.ezreal.common.model.request.PublishSeckillGoodRequest;
import com.ezreal.common.model.request.UpdateSeckillGoodRequest;
import com.ezreal.common.model.response.good.MultiSeckillGoodsResponse;
import com.ezreal.common.model.response.good.SeckillGoodResponse;

/**
* @author Ezreal
* @description 针对表【seckill_good(秒杀品)】的数据库操作Service
* @createDate 2023-01-03 00:17:00
*/
public interface SeckillGoodService extends IService<SeckillGood> {

    BaseResponse<SuccessCode> publishSeckillGood(Long userId, Long activityId, PublishSeckillGoodRequest publishSeckillGoodRequest);

    BaseResponse<SuccessCode> onlineSeckillGood(Long userId, Long activityId, Long itemId);

    BaseResponse<SuccessCode> offlineSeckillGood(Long userId, Long activityId, Long itemId);

    BaseResponse<SeckillGoodResponse> getSeckillGood(Long userId, Long activityId, Long itemId, Long version);

    BaseResponse<MultiSeckillGoodsResponse> getSeckillGoods(Long userId, Long activityId, SeckillGoodQuery seckillGoodQuery);

    BaseResponse<MultiSeckillGoodsResponse> getOnlineSeckillGoods(Long userId, Long activityId, SeckillGoodQuery seckillGoodQuery);

    BaseResponse<SuccessCode> updateSeckillGoodStock(boolean isDecrease, Long itemId, Integer quantity);

    BaseResponse<MultiSeckillGoodsResponse> getAllSeckillGoods(Long userId, SeckillGoodQuery seckillGoodQuery);

    BaseResponse<SuccessCode> updateSeckillGood(Long userId, Long itemId, UpdateSeckillGoodRequest updateSeckillGoodRequest);
}
