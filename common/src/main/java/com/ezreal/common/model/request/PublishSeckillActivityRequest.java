package com.ezreal.common.model.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.util.StringUtils;

import java.util.Date;

@Data
public class PublishSeckillActivityRequest {
    /**
     * 活动名称
     */
    private String activityName;

    /**
     * 开始时间
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;

    /**
     * 结束时间
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;

    /**
     * 活动状态 0-发布 1-上线 2-下线
     */
    private Integer activityStatus;

    /**
     * 活动描述
     */
    private String activityDesc;

    /**
     * 判断参数
     * @return 是否合法
     */
    public boolean validate() {
        return !StringUtils.isEmpty(activityName)
                && startTime != null
                && endTime != null
                && startTime.before(endTime);
    }
}
