package com.ezreal.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 通用返回类
 *
 * @param <T>
 * @author yupi
 */
@Data
public class BaseResponse<T> implements Serializable {

    private int code;

    private T data;

    private String message;

    public BaseResponse() {
    }

    public BaseResponse(int code, T data, String message) {
        this.code = code;
        this.data = data;
        this.message = message;
    }

    public BaseResponse(int code, T data) {
        this(code, data, "");
    }

    public BaseResponse(ErrorCode errorCode) {
        this(errorCode.getCode(), null, errorCode.getMessage());
    }

    public BaseResponse(SuccessCode successCode) {
        this(successCode.getCode(), null, successCode.getMessage());
    }

    /**
     * 数据更新中，稍后再试
     * 客户端对此类错误不做页面刷新等其他操作
     */
    public static <T> BaseResponse<T> tryLater() {
        BaseResponse<T> baseResponse = new BaseResponse<>();
        baseResponse.setCode(ErrorCode.TRY_LATER.getCode());
        baseResponse.setMessage(ErrorCode.TRY_LATER.getMessage());
        return baseResponse;
    }
    public static <T> BaseResponse<T> empty() {
        BaseResponse<T> baseResponse = new BaseResponse<>();
        baseResponse.setCode(ErrorCode.GOOD_NOT_FOUND.getCode());
        baseResponse.setMessage(ErrorCode.GOOD_NOT_FOUND.getMessage());
        return baseResponse;
    }

}
