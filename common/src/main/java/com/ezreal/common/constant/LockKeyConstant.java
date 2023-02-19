package com.ezreal.common.constant;

public enum LockKeyConstant {
    GOOD_CREATE_LOCK_KEY("GOOD_CREATE_LOCK_KEY_"),
    GOODS_CREATE_LOCK_KEY("GOODS_CREATE_LOCK_KEY_"),
    GOOD_MODIFICATION_LOCK_KEY("GOOD_MODIFICATION_LOCK_KEY_"),

    SCHEDULED_WARM_UP_LOCK("SCHEDULED_WARM_UP_LOCK");

    private final String keyName;

    LockKeyConstant(String key) {
        this.keyName = key;
    }

    public String getKeyName() {
        return keyName;
    }
}
