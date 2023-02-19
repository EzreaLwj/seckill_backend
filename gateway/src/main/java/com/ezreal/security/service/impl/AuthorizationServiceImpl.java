package com.ezreal.security.service.impl;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.ezreal.security.model.AuthResult;
import com.ezreal.security.service.AuthorizationService;
import com.ezreal.common.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
@Slf4j
public class AuthorizationServiceImpl implements AuthorizationService {

    private final static String USER_ID = "user_id";
    private final static String SALT = "ezreal";
    // 3天过期
    private static final long EXPIRE_TIME = 1000 * 60 * 60 * 24 * 24;
    @Override
    public AuthResult auth(String encryptionToken) {
        DecodedJWT verify = JWT.require(Algorithm.HMAC256(SALT))
                .build()
                .verify(encryptionToken);

        long expireTime = verify.getExpiresAt().getTime();
        if (System.currentTimeMillis() > expireTime) {
            new AuthResult().error(ErrorCode.AUTH_TIME_OUT);
        }

        Long userId = verify.getClaim(USER_ID).asLong();

        if (userId == null) {
            new AuthResult().error(ErrorCode.AUTH_NOT_FOUND);
        }

        return new AuthResult().pass(userId, expireTime);
    }

    @Override
    public String getToken(Long userId) {
        if (userId == null) {
            return null;
        }

        String sign = JWT.create()
                .withClaim(USER_ID, userId)
                .withExpiresAt(new Date(System.currentTimeMillis() + EXPIRE_TIME))
                .sign(Algorithm.HMAC256(SALT));
        log.info("生成token为：{}", sign);
        log.info("过期时间为：{}", new Date(System.currentTimeMillis() + EXPIRE_TIME));
        return sign;
    }
}
