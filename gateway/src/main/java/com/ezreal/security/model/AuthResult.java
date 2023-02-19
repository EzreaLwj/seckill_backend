package com.ezreal.security.model;

import com.ezreal.common.ErrorCode;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class AuthResult {
    private Long userId;

    private boolean success;

    private long expireTime;

    private ErrorCode errorCode;

    public AuthResult pass(Long userId, long expireTime) {
        this.success = true;
        this.userId = userId;
        this.expireTime = expireTime;
        return this;
    }

    public AuthResult error(ErrorCode errorCode) {
        this.success = false;
        this.errorCode = errorCode;
        return this;
    }
}
