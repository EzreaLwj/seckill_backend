package com.ezreal.common.model.request.user;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.Date;

@Data
@Slf4j
public class UserRegisterRequest {
    /**
     * 用户昵称
     */
    private String nickName;

    /**
     * 用户手机号
     */
    private String phone;

    /**
     * 用户密码
     */
    private String password;

    /**
     * 生日
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date birthday;

    /**
     * 用户头像
     */
    private String headImg;

    /**
     * 性别
     */
    private Integer sex;

    /**
     * 短信验证码
     */
    private String messageCode;

    public boolean validate() {
        if (StringUtils.isEmpty(phone)
                || StringUtils.isEmpty(messageCode)
                || StringUtils.isEmpty(password)) {

            log.info("用户参数错误|{}, {}", phone, messageCode);
            return false;
        }

        if (messageCode.length() != 6) {
            log.info("验证码长度错误|{}", phone);
            return false;
        }

        if (phone.length() != 11) {
            log.info("手机号码长度错误|{}", phone);
            return false;
        }
        return true;
    }

}
