package com.ezreal.goods.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.ezreal.common.BaseResponse;
import com.ezreal.common.SuccessCode;
import com.ezreal.common.model.enums.SeckillGoodStatus;
import com.ezreal.common.model.query.SeckillGoodQuery;
import com.ezreal.common.model.request.PublishSeckillGoodRequest;
import com.ezreal.common.model.request.UpdateSeckillGoodRequest;
import com.ezreal.common.model.response.good.MultiSeckillGoodsResponse;
import com.ezreal.common.model.response.good.SeckillGoodResponse;
import com.ezreal.goods.service.SeckillGoodService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/good")
public class SeckillGoodController {

    @Autowired
    private SeckillGoodService seckillGoodService;

    /**
     * 发布秒杀品
     *
     * @param userId
     * @param activityId
     * @param publishSeckillGoodRequest
     * @return
     */
    @PostMapping("/{activityId}")
    public BaseResponse<SuccessCode> publishSeckillGood(@RequestHeader(value = "TokenInfo") Long userId,
                                                        @PathVariable Long activityId,
                                                        @RequestBody PublishSeckillGoodRequest publishSeckillGoodRequest) {
        return seckillGoodService.publishSeckillGood(userId, activityId, publishSeckillGoodRequest);
    }

    /**
     * 上线秒杀品
     *
     * @param userId
     * @param activityId
     * @param itemId
     * @return
     */

    @PutMapping("/{activityId}/online/{itemId}")
    public BaseResponse<SuccessCode> onlineSeckillGood(@RequestHeader(value = "TokenInfo") Long userId,
                                                       @PathVariable Long activityId,
                                                       @PathVariable Long itemId) {
        return seckillGoodService.onlineSeckillGood(userId, activityId, itemId);
    }

    /**
     * 下线秒杀品
     *
     * @param userId
     * @param activityId
     * @param itemId
     * @return
     */
    @PutMapping("/{activityId}/offline/{itemId}")
    public BaseResponse<SuccessCode> offlineSeckillGood(@RequestHeader(value = "TokenInfo") Long userId,
                                                        @PathVariable Long activityId,
                                                        @PathVariable Long itemId) {
        return seckillGoodService.offlineSeckillGood(userId, activityId, itemId);
    }

    /**
     * 更改信息
     * @param userId
     * @param itemId
     * @param updateSeckillGoodRequest
     * @return
     */
    @PutMapping("/update/{itemId}")
    public BaseResponse<SuccessCode> updateSeckillGood(@RequestHeader(value = "TokenInfo") Long userId,
                                                       @PathVariable Long itemId,
                                                       @RequestBody UpdateSeckillGoodRequest updateSeckillGoodRequest) {
        return seckillGoodService.updateSeckillGood(userId, itemId, updateSeckillGoodRequest);
    }


    /**
     * 获取指定id的商品
     *
     * @param userId
     * @param activityId
     * @param itemId
     * @return
     */
    @GetMapping("/{activityId}/list/{itemId}")
    @SentinelResource(value = "GetSeckillGood")
    public BaseResponse<SeckillGoodResponse> getSeckillGood(@RequestHeader(value = "TokenInfo") Long userId,
                                                            @PathVariable Long activityId,
                                                            @PathVariable Long itemId,
                                                            @RequestParam(required = false) Long version) {
        return seckillGoodService.getSeckillGood(userId, activityId, itemId, version);
    }

    /**
     * 获取指定活动的所有商品
     *
     * @param userId
     * @param activityId
     * @param pageSize
     * @param pageNum
     * @param keyword
     * @return
     */
    @GetMapping("/{activityId}/list")
    public BaseResponse<MultiSeckillGoodsResponse> getSeckillGoods(@RequestHeader(value = "TokenInfo") Long userId,
                                                                   @PathVariable Long activityId,
                                                                   @RequestParam Integer pageNum,
                                                                   @RequestParam Integer pageSize,
                                                                   @RequestParam(required = false) String keyword) {
        SeckillGoodQuery seckillGoodQuery = new SeckillGoodQuery()
                .setPageSize(pageSize)
                .setPageNum(pageNum)
                .setKeyword(keyword);
        return seckillGoodService.getSeckillGoods(userId, activityId, seckillGoodQuery);
    }


    /**
     * 获取指定活动的上线商品
     *
     * @param userId
     * @param activityId
     * @param pageNum
     * @param pageSize
     * @param keyword
     * @return
     */
    @GetMapping("/{activityId}/list/online")
    public BaseResponse<MultiSeckillGoodsResponse> getOnlineSeckillGoods(@RequestHeader(value = "TokenInfo") Long userId,
                                                                          @PathVariable Long activityId,
                                                                         @RequestParam Integer pageNum,
                                                                         @RequestParam Integer pageSize,
                                                                         @RequestParam(required = false) String keyword) {
        SeckillGoodQuery seckillGoodQuery = new SeckillGoodQuery()
                .setPageSize(pageSize)
                .setPageNum(pageNum)
                .setKeyword(keyword)
                .setStatus(SeckillGoodStatus.ONLINE.getCode());
        return seckillGoodService.getOnlineSeckillGoods(userId, activityId, seckillGoodQuery);
    }

    /**
     * 自定义查询条件获取商品
     *
     * @param userId
     * @param seckillGoodQuery
     * @return
     */
    @GetMapping("/list")
    public BaseResponse<MultiSeckillGoodsResponse> getAllSeckillGoods(@RequestHeader(value = "TokenInfo") Long userId,
                                                                      SeckillGoodQuery seckillGoodQuery) {


        return seckillGoodService.getAllSeckillGoods(userId, seckillGoodQuery);
    }


    /**
     * 扣减库存
     *
     * @param itemId
     * @param quantity
     * @return
     */
    @PutMapping("/stock/{itemId}")
    BaseResponse<SuccessCode> updateSeckillGoodStock(@PathVariable Long itemId,
                                                     @RequestParam("quantity") Integer quantity,
                                                     @RequestParam("isDecrease") boolean isDecrease) {
        return seckillGoodService.updateSeckillGoodStock(isDecrease, itemId, quantity);
    }
}
