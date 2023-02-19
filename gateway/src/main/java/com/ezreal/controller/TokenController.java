package com.ezreal.controller;

import com.ezreal.security.service.AuthorizationService;
import com.ezreal.common.BaseResponse;
import com.ezreal.common.ResultUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("token")
public class TokenController {
    @Autowired
    private AuthorizationService authorizationService;

    @GetMapping("/{userId}")
    public BaseResponse<String> getToken(@PathVariable Long userId) {
        String token = authorizationService.getToken(userId);

        return ResultUtils.success(token);
    }
}
