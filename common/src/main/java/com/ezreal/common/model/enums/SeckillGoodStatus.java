package com.ezreal.common.model.enums;

public enum SeckillGoodStatus {
    PUBLISHED(0),
    ONLINE(1),
    OFFLINE(2);

    private final Integer code;

    SeckillGoodStatus(Integer code) {
        this.code = code;
    }

    public static boolean isOffline(Integer status) {
        return OFFLINE.getCode().equals(status);
    }

    public static boolean isOnline(Integer status) {
        return ONLINE.getCode().equals(status);
    }

    public Integer getCode() {
        return code;
    }
}
