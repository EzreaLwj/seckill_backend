package com.ezreal.common;

public enum SuccessCode {

    /**
     * 活动相关
     */
    ACTIVITY_PUBLISH_SUCCESS(20, "发布活动成功"),
    ACTIVITY_UPDATE_SUCCESS(21, "修改活动成功"),
    ACTIVITY_ONLINE_SUCCESS(22, "上线活动成功"),
    ACTIVITY_OFFLINE_SUCCESS(23, "下线活动成功"),

    /**
     * 商品相关
     */
    GOOD_PUBLISH_SUCCESS(30, "发布商品成功"),
    GOOD_ONLINE_SUCCESS(31, "上线商品成功"),
    GOOD_OFFLINE_SUCCESS(32, "下线商品成功"),
    GOOD_STOCK_DECREASE_SUCCESS(33, "商品库存扣减成功"),
    GOOD_STOCK_INCREASE_SUCCESS(34, "商品库存增加成功"),
    GOOD_UPDATE_SUCCESS(35, "修改商品成功"),

    /**
     * 订单相关
     */
    ORDER_CANCEL_SUCCESS(40, "订单取消成功"),

    /**
     * 库存分桶/编排成功
     */
    STOCK_ARRANGE_SUCCESS(60, "库存分桶成功");

    /**
     * 状态码
     */
    private final int code;

    /**
     * 信息
     */
    private final String message;

    SuccessCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
