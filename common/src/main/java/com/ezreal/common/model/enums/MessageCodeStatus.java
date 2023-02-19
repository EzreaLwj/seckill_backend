package com.ezreal.common.model.enums;

public enum MessageCodeStatus {
    Login(0),
    Register(1);

    private final Integer status;

    MessageCodeStatus(Integer status) {
        this.status = status;
    }

    public Integer getStatus() {
        return status;
    }
}
