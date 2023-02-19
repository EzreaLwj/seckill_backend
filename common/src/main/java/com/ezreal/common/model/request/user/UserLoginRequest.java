package com.ezreal.common.model.request.user;

import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

@Data
@Accessors(chain = true)
@Slf4j
public class UserLoginRequest {
    private String phone;

    private String messageCode;


    public boolean validate() {
        if (StringUtils.isEmpty(phone) || StringUtils.isEmpty(messageCode)) {
            log.info("用户参数为空|{}, {}", phone, messageCode);
            return false;
        }

        if (messageCode.length() != 6) {
            log.info("验证码长度错误|{}", phone);
            return false;
        }

        if (phone.length() !=11) {
            log.info("手机号码长度错误|{}", phone);
            return false;
        }

        return true;
    }
}
