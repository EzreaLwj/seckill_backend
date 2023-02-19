package com.ezreal.filter;

import com.alibaba.fastjson.JSON;
import com.ezreal.exception.ExceptionCode;
import com.ezreal.exception.ExceptionResponse;
import com.ezreal.security.service.SecurityRuleChainService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class SecurityRulesFilter implements GlobalFilter, Ordered {

    @Resource
    private List<SecurityRuleChainService> securityRuleChainServices;

    private List<SecurityRuleChainService> getSecurityRuleChainServices() {
        if (CollectionUtils.isEmpty(securityRuleChainServices)) {
            return new ArrayList<>();
        }
        return securityRuleChainServices
                .stream()
                .sorted(Comparator.comparing(SecurityRuleChainService::getOrder))
                .collect(Collectors.toList());
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpResponse response = exchange.getResponse();
        for (SecurityRuleChainService securityRuleChainService : getSecurityRuleChainServices()) {
            if (!securityRuleChainService.run(exchange.getRequest(), exchange.getResponse())) {
                ExceptionResponse exceptionResponse = new ExceptionResponse()
                        .setErrorCode(ExceptionCode.LIMIT_BLOCK.getCode())
                        .setErrorMessage(ExceptionCode.LIMIT_BLOCK.getDesc());

                DataBuffer dataBuffer = response.bufferFactory()
                        .wrap(JSON.toJSONString(exceptionResponse)
                                .getBytes(StandardCharsets.UTF_8));

                return response.writeWith(Mono.just(dataBuffer)).doOnError(error -> DataBufferUtils.release(dataBuffer));
            }
        }
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return 2;
    }
}
