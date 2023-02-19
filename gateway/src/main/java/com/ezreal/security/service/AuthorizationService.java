package com.ezreal.security.service;

import com.ezreal.security.model.AuthResult;

public interface AuthorizationService {
    /**
     * 根据加密token鉴权
     * @param encryptionToken
     * @return
     */
    AuthResult auth(String encryptionToken);

    /**
     * 获取token
     * @param userId
     * @return
     */
    String getToken(Long userId);
}
