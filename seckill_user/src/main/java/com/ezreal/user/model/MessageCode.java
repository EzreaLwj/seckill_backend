package com.ezreal.user.model;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

@Data
@Accessors(chain = true)
public class MessageCode {
    private String code;

    private Date endTime;
}
