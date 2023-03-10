package com.ezreal.common.model.enums;

public enum BucketType {
    PRIMARY(0);

    private final Integer code;
    BucketType(Integer code) {
        this.code = code;
    }

    public Integer getCode() {
        return code;
    }
}
