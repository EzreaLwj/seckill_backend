package com.ezreal.common.model.builder;

import com.ezreal.common.model.domain.SeckillUser;
import com.ezreal.common.model.request.user.UserLoginRequest;
import com.ezreal.common.model.request.user.UserRegisterRequest;
import com.ezreal.common.model.response.user.UserLoginResponse;
import com.ezreal.common.model.response.user.UserRegisterResponse;
import org.springframework.beans.BeanUtils;

public class SeckillUserBuilder {
    public static SeckillUser toDomain(UserLoginRequest userLoginRequest) {
        if (userLoginRequest == null) {
            return null;
        }

        SeckillUser seckillUser = new SeckillUser();
        BeanUtils.copyProperties(userLoginRequest, seckillUser);
        return seckillUser;
    }

    public static SeckillUser toDomain(UserRegisterRequest userRegisterRequest) {
        if (userRegisterRequest == null) {
            return null;
        }

        SeckillUser seckillUser = new SeckillUser();
        BeanUtils.copyProperties(userRegisterRequest, seckillUser);
        return seckillUser;
    }

    public static UserLoginResponse toLoginResponse(SeckillUser seckillUser) {
        if (seckillUser == null) {
            return null;
        }

        UserLoginResponse userLoginResponse = new UserLoginResponse();
        BeanUtils.copyProperties(seckillUser, userLoginResponse);
        return userLoginResponse;
    }

    public static UserRegisterResponse toRegisterResponse(SeckillUser seckillUser) {
        if (seckillUser == null) {
            return null;
        }

        UserRegisterResponse userRegisterResponse = new UserRegisterResponse();
        BeanUtils.copyProperties(seckillUser, userRegisterResponse);
        return userRegisterResponse;
    }

}
