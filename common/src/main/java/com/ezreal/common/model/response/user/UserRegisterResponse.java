package com.ezreal.common.model.response.user;


import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class UserRegisterResponse {

    /**
     * 用户id
     */
    private Long id;

    /**
     * 用户
     */
    private String nickName;

    /**
     * token
     */
    private String token;

    /**
     * 用户头像
     */
    private String headImg;

    /**
     * 用户身份
     */
    private Integer status;
}
