package com.ezreal.common.model.request;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

@Data
@Slf4j
public class MessageCodeRequest {
    private String phone;

    private Integer status;

    public boolean validate() {
        if (StringUtils.isEmpty(phone) || status == null) {
            log.info("短信验证码获取参数为空");
            return false;
        }

        if (phone.length() != 11) {
            log.info("手机号码位数错误|{}", phone);
            return false;
        }

        return true;
    }
}
