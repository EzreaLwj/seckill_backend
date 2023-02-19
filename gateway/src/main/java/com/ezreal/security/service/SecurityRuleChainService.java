package com.ezreal.security.service;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;

public interface SecurityRuleChainService {
    /**
     * @param request  请求
     * @param response 响应
     * @return 执行结果
     */
    boolean run(ServerHttpRequest request , ServerHttpResponse response);

    /**
     * 调用链执行顺序
     *
     * @return 执行顺序
     */
    int getOrder();
}
