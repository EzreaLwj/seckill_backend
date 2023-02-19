package com.ezreal.common;

import com.alibaba.fastjson.annotation.JSONType;

/**
 * 错误码
 *
 * @author yupi
 */
@JSONType(serializeEnumAsJavaBean = true)
public enum ErrorCode {


    /**
     * 一般性错误
     */
    INVALID_PARAMS(100, "参数错误"),
    TRY_LATER(120, "稍后再试"),
    FREQUENTLY_ERROR(130, "操作频繁，稍后再试"),
    LOCK_FAILED_ERROR(140, "变更中，稍后再试"),
    BUSINESS_ERROR(150, "未知错误"),
    SYSTEM_ERROR(160, "系统内部异常"),


    /**
     * 活动相关错误
     */
    ACTIVITY_NOT_FOUND(310, "活动不存在"),
    ACTIVITY_NOT_ONLINE(320, "活动未上线"),
    ACTIVITY_NOT_IN_PROGRESS(330, "当前不是活动时段"),


    /**
     * 商品相关错误
     */
    GOOD_NOT_FOUND(410, "商品不存在"),
    GOOD_NOT_ONLINE(430, "秒杀品未上线"),
    GOOD_NOT_ON_SALE(440, "当前不是秒杀时段"),
    GOOD_ADD_ERROR(450, "秒杀品发布失败"),
    GOOD_ONLINE_ERROR(460, "秒杀品上线失败"),
    GOOD_OFFLINE_ERROR(470, "秒杀品下线失败"),

    /**
     * 数据库相关错误
     */
    SQL_INSERT(810, "插入错误"),
    SQL_DELETE(820, "删除错误"),
    SQL_UPDATE(830, "更新错误"),

    /**
     * 鉴权相关错误
     */
    AUTH_TIME_OUT(610, "登录超时"),
    AUTH_NOT_FOUND(620, "用户未找到"),
    AUTH_NO_TOKEN(630, "TOKEN丢失"),
    /**
     * 下单错误
     */
    GET_ITEM_FAILED(710, "获取秒杀品失败"),
    ITEM_SOLD_OUT(711, "秒杀品已售罄"),
    REDUNDANT_SUBMIT(712, "请勿重复下单"),
    ORDER_TOKENS_NOT_AVAILABLE(713, "暂无可用库存"),
    ORDER_TASK_SUBMIT_FAILED(714, "订单提交失败，请稍后再试"),
    ORDER_NOT_FOUND(715, "订单不存在"),
    ORDER_TYPE_NOT_SUPPORT(716, "下单类型不支持"),
    ORDER_CANCEL_FAILED(717, "订单取消失败"),
    PLACE_ORDER_FAILED(718, "下单失败"),
    PLACE_ORDER_TASK_ID_INVALID(719, "下单任务编号错误"),


    /**
     * 分桶相关错误码
     */
    ARRANGE_STOCK_BUCKETS_FAILED(720, "库存编排错误"),
    QUERY_STOCK_BUCKETS_FAILED(721, "获取库存分桶错误"),
    PRIMARY_BUCKET_IS_MISSING(722, "主桶缺失"),
    MULTI_PRIMARY_BUCKETS_FOUND_BUT_EXPECT_ONE(723, "发现多个主桶但只需要一个"),
    TOTAL_STOCKS_AMOUNT_INVALID(724, "库存总数错误"),
    AVAILABLE_STOCKS_AMOUNT_NOT_EQUALS_TO_TOTAL_STOCKS_AMOUNT(725, "子桶可用库存与库存总数不匹配"),
    AVAILABLE_STOCKS_AMOUNT_INVALID(726, "子桶可用库存数量错误"),
    STOCK_BUCKET_ITEM_INVALID(727, "秒杀品ID设置错误"),
    STOCK_NOT_ENOUGH(728, "可用库存不足"),


    /**
     * 用户相关
     */
    USER_NOT_FOUNT(810, "用户未找到"),

    USER_EXIST(814, "用户已经存在"),

    /**
     * 验证码相关
     */
    MESSAGE_CODE_LOGIN_ERROR(811, "用户登录验证码错误"),
    MESSAGE_CODE_REGISTER_ERROR(812, "用户注册验证码错误"),
    MESSAGE_CODE_STATUS_ERROR(817, "获取验证码身份错误"),
    MESSAGE_CODE_EXIST(813,"短信验证码已经存在");
    /**
     * 状态码
     */
    private final int code;

    /**
     * 信息
     */
    private final String message;

    ErrorCode(int code, String message) {
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
