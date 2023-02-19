package com.ezreal.common.model.response.order;

import com.ezreal.common.ErrorCode;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class SeckillOrderMessageResponse {

    private boolean success;

    private Integer code;

    private String message;

    private String placeOrderTaskId;

    private Long orderId;

    public static SeckillOrderMessageResponse ok(String placeOrderTaskId) {
        return new SeckillOrderMessageResponse()
                .setSuccess(true)
                .setCode(0)
                .setPlaceOrderTaskId(placeOrderTaskId);
    }

    public static SeckillOrderMessageResponse ok(Long orderId) {
        return new SeckillOrderMessageResponse()
                .setSuccess(true)
                .setCode(0)
                .setOrderId(orderId);
    }

    public static SeckillOrderMessageResponse failed(Integer code, String message) {
        return new SeckillOrderMessageResponse()
                .setSuccess(false)
                .setCode(code)
                .setMessage(message);
    }

    public static SeckillOrderMessageResponse failed(ErrorCode errorCode) {
        return new SeckillOrderMessageResponse()
                .setSuccess(false)
                .setCode(errorCode.getCode())
                .setMessage(errorCode.getMessage());
    }

    public static SeckillOrderMessageResponse ok() {
        return new SeckillOrderMessageResponse()
                .setSuccess(true);
    }
}
