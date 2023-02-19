package com.ezreal.user.controller;

import com.ezreal.common.BaseResponse;
import com.ezreal.common.model.request.MessageCodeRequest;
import com.ezreal.common.model.request.user.UserLoginRequest;
import com.ezreal.common.model.request.user.UserRegisterRequest;
import com.ezreal.common.model.response.user.UserLoginResponse;
import com.ezreal.common.model.response.user.UserRegisterResponse;
import com.ezreal.user.model.MessageCode;
import com.ezreal.user.service.SeckillUserService;
import com.fasterxml.jackson.databind.ser.Serializers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private SeckillUserService seckillUserService;

    @PostMapping("/login")
    public BaseResponse<UserLoginResponse> userLogin(@RequestBody UserLoginRequest userLoginRequest) {
        return seckillUserService.userLogin(userLoginRequest);
    }

    @GetMapping("/code")
    public BaseResponse<MessageCode> getMessageCode(MessageCodeRequest messageCodeRequest) {
        return seckillUserService.getMessageCode(messageCodeRequest);
    }

    @PostMapping("/register")
    public BaseResponse<UserRegisterResponse> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        return seckillUserService.userRegister(userRegisterRequest);
    }
}
