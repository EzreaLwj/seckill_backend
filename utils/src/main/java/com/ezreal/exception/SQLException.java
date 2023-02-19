package com.ezreal.exception;

import com.ezreal.common.ErrorCode;

public class SQLException extends RuntimeException{
    public SQLException(String message) {
        super(message);
    }

    public SQLException(ErrorCode errorCode) {
        this(errorCode.getMessage());
    }
}
