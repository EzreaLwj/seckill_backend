package com.ezreal.common.model.enums;

public enum SeckillUserStatus {
    COMMON_USER(0),
    MANAGER(1);

    private final Integer code;

    SeckillUserStatus(Integer code) {
        this.code = code;
    }

    public Integer getCode() {
        return code;
    }
}
