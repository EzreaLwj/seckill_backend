package com.ezreal.common.model.enums;

public enum SeckillBucketStatus {
    ENABLED(1),
    DISABLED(0);

    private final Integer code;

    SeckillBucketStatus(Integer code) {
        this.code = code;
    }

    public Integer getCode() {
        return code;
    }
}
