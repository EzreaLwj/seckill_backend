package com.ezreal.common.model.enums;

public enum SeckillOrderStatus {
    CREATED(1),
    PAID(2),
    CANCELED(3),
    DELETED(4);

    private final Integer code;

    SeckillOrderStatus(Integer code) {
        this.code = code;
    }

    public static boolean isCancled(Integer status) {
        return CANCELED.getCode().equals(status);
    }

    public static boolean isDeleted(Integer status) {
        return DELETED.getCode().equals(status);
    }

    public Integer getCode() {
        return code;
    }
}
