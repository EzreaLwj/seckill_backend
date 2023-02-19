package com.ezreal.common.model.result;


import com.ezreal.common.ErrorCode;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class OrderTaskSubmitResult {
    private boolean success;
    private int code;
    private String message;

    public static OrderTaskSubmitResult ok() {
        return new OrderTaskSubmitResult()
                .setSuccess(true);
    }

    public static OrderTaskSubmitResult failed(ErrorCode errorCode) {
        return new OrderTaskSubmitResult()
                .setSuccess(false)
                .setCode(errorCode.getCode())
                .setMessage(errorCode.getMessage());
    }
}
