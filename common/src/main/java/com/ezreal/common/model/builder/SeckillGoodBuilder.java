package com.ezreal.common.model.builder;

import com.ezreal.common.model.cahce.SeckillGoodCache;
import com.ezreal.common.model.domain.SeckillGood;
import com.ezreal.common.model.request.PublishSeckillGoodRequest;
import com.ezreal.common.model.request.UpdateSeckillGoodRequest;
import com.ezreal.common.model.response.good.SeckillGoodResponse;
import org.springframework.beans.BeanUtils;

public class SeckillGoodBuilder {
    public static SeckillGood toDomain(PublishSeckillGoodRequest publishSeckillGoodRequest) {
        if (publishSeckillGoodRequest == null) {
            return null;
        }

        SeckillGood seckillGood = new SeckillGood();
        BeanUtils.copyProperties(publishSeckillGoodRequest, seckillGood);

        return seckillGood;
    }

    public static SeckillGood toDomain(UpdateSeckillGoodRequest updateSeckillGoodRequest) {
        if (updateSeckillGoodRequest == null) {
            return null;
        }

        SeckillGood seckillGood = new SeckillGood();
        BeanUtils.copyProperties(updateSeckillGoodRequest, seckillGood);

        return seckillGood;
    }

    public static SeckillGoodResponse toResponse(SeckillGood seckillGood) {
        if (seckillGood == null) {
            return null;
        }

        SeckillGoodResponse seckillGoodResponse = new SeckillGoodResponse();
        BeanUtils.copyProperties(seckillGood, seckillGoodResponse);

        return seckillGoodResponse;
    }

    public static SeckillGoodCache toCache(SeckillGood seckillGood) {
        if (seckillGood == null) {
            return null;
        }

        SeckillGoodCache seckillGoodCache = new SeckillGoodCache();
        BeanUtils.copyProperties(seckillGood, seckillGoodCache.getSeckillGood());

        return seckillGoodCache;
    }
}
