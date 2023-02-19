package com.ezreal.user.service;

import com.ezreal.common.BaseResponse;
import com.ezreal.common.model.domain.SeckillUser;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ezreal.common.model.request.MessageCodeRequest;
import com.ezreal.common.model.request.user.UserLoginRequest;
import com.ezreal.common.model.request.user.UserRegisterRequest;
import com.ezreal.common.model.response.user.UserLoginResponse;
import com.ezreal.common.model.response.user.UserRegisterResponse;
import com.ezreal.user.model.MessageCode;

/**
* @author Ezreal
* @description 针对表【seckill_user(用户表)】的数据库操作Service
* @createDate 2023-01-08 11:08:10
*/
public interface SeckillUserService extends IService<SeckillUser> {

    BaseResponse<UserLoginResponse> userLogin(UserLoginRequest userLoginRequest);

    BaseResponse<MessageCode> getMessageCode(MessageCodeRequest messageCodeRequest);

    BaseResponse<UserRegisterResponse> userRegister(UserRegisterRequest userRegisterRequest);
}
