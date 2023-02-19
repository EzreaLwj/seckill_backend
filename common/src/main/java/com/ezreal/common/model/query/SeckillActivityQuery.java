package com.ezreal.common.model.query;

import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.util.StringUtils;

@Data
@Accessors(chain = true)
public class SeckillActivityQuery {
    private String keyword;
    private Integer pageSize;
    private Integer pageNum;
    private Integer status;
    private Long version;

    public boolean isFirstPureQuery() {
        return StringUtils.isEmpty(keyword) && pageNum != null && pageNum == 1;
    }

}
