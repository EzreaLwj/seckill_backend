package com.ezreal.clients;

import com.ezreal.common.BaseResponse;
import com.ezreal.common.SuccessCode;
import com.ezreal.common.model.query.SeckillGoodQuery;
import com.ezreal.common.model.request.UpdateSeckillGoodRequest;
import com.ezreal.common.model.response.good.MultiSeckillGoodsResponse;
import com.ezreal.common.model.response.good.SeckillGoodResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.*;

@FeignClient("seckill-good")
public interface SeckillGoodClient {

    @GetMapping("/api/good/{activityId}/list/{itemId}")
    BaseResponse<SeckillGoodResponse> getSeckillGood(@RequestHeader("TokenInfo") Long userId,
                                                     @PathVariable Long activityId,
                                                     @PathVariable Long itemId);

    @PutMapping("/api/good/stock/{itemId}")
    BaseResponse<SuccessCode> updateSeckillGoodStock(@RequestHeader("TokenInfo") Long userId,
                                                     @PathVariable Long itemId,
                                                     @RequestParam("quantity") Integer quantity,
                                                     @RequestParam("isDecrease") boolean isDecrease);

    @GetMapping("/api/good/list")
    BaseResponse<MultiSeckillGoodsResponse> getAllSeckillGoods(@RequestHeader("TokenInfo") Long userId,
                                                               @SpringQueryMap SeckillGoodQuery seckillGoodQuery);

    @PutMapping("/api/good/update/{itemId}")
    BaseResponse<SuccessCode> updateSeckillGood(@RequestHeader("TokenInfo") Long userId,
                                                @PathVariable Long itemId,
                                                @RequestBody UpdateSeckillGoodRequest updateSeckillGoodRequest);
}
