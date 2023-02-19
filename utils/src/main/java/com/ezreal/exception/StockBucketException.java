package com.ezreal.exception;

import com.ezreal.common.ErrorCode;

public class StockBucketException extends RuntimeException{

    private final int code;

    public StockBucketException(int code, String message) {
        super(message);
        this.code = code;
    }

    public StockBucketException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public StockBucketException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }

    public int getCode() {
        return code;
    }
}
