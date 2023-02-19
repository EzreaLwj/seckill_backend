package com.ezreal.filter;

import com.alibaba.fastjson.JSON;
import com.ezreal.security.service.AuthorizationService;
import com.ezreal.security.model.AuthResult;
import com.ezreal.common.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;

import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class AuthFilter implements GlobalFilter , Ordered {
    private static final String USER_ID = "userId";

    @Autowired
    private AuthorizationService authorizationService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    private static final String UPDATE_USERID_LOCK_KEY = "UPDATE_USERID_LOCK_KEY_";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        log.info("访问网关");
        ServerHttpRequest request = exchange.getRequest();

        String token = request.getHeaders().getFirst("Authorization");
        ServerHttpResponse response = exchange.getResponse();

        if (token == null) {
            DataBuffer dataBuffer = response.bufferFactory()
                    .wrap(JSON.toJSONString(ErrorCode.AUTH_NO_TOKEN).getBytes(StandardCharsets.UTF_8));
            return response.writeWith(Mono.just(dataBuffer)).doOnError(error -> DataBufferUtils.release(dataBuffer));
        }

        Object userId = redisTemplate.opsForValue().get(userTokenKey(token));

        if (userId == null) {
            RLock lock = redissonClient.getLock(UPDATE_USERID_LOCK_KEY + token);
            try {
                boolean isSuccessLock = lock.tryLock(500, 2000, TimeUnit.MILLISECONDS);

                if (!isSuccessLock) {
                    DataBuffer dataBuffer = response.bufferFactory()
                            .wrap(JSON.toJSONString(ErrorCode.FREQUENTLY_ERROR).getBytes(StandardCharsets.UTF_8));
                    return response.writeWith(Mono.just(dataBuffer)).doOnError(error -> DataBufferUtils.release(dataBuffer));
                }

                AuthResult auth = authorizationService.auth(token);
                if (!auth.isSuccess()) {
                    DataBuffer dataBuffer = response.bufferFactory()
                            .wrap(JSON.toJSONString(auth.getErrorCode()).getBytes(StandardCharsets.UTF_8));
                    return response.writeWith(Mono.just(dataBuffer)).doOnError(error -> DataBufferUtils.release(dataBuffer));
                }

                userId = auth.getUserId();
                redisTemplate.opsForValue().set(userTokenKey(token), userId,auth.getExpireTime() - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                log.info("获取锁失败", e);
                DataBuffer dataBuffer = response.bufferFactory()
                        .wrap(JSON.toJSONString(ErrorCode.FREQUENTLY_ERROR).getBytes(StandardCharsets.UTF_8));
                return response.writeWith(Mono.just(dataBuffer)).doOnError(error -> DataBufferUtils.release(dataBuffer));
            } finally {
                lock.unlock();
            }
        }

        request = request.mutate()
                .header("TokenInfo", String.valueOf(userId))
                .build();

        ServerWebExchange newExchange = exchange.mutate().request(request).build();
        return chain.filter(newExchange);
    }

    private String userTokenKey(String token) {
        return "user:" + token;
    }

    @Override
    public int getOrder() {
        return 1;
    }
}
