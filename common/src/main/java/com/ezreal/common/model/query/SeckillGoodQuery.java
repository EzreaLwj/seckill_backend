package com.ezreal.common.model.query;

import com.ezreal.common.model.enums.SeckillGoodStatus;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.util.StringUtils;

@Data
@Accessors(chain = true)
public class SeckillGoodQuery {
    private String keyword;
    private Integer pageSize;
    private Integer pageNum;
    private Integer status;
    private Long version;
    private Integer stockWarmUp;

    public boolean isFirstPureQuery() {
        return StringUtils.isEmpty(keyword) && pageNum != null && pageNum == 1 && SeckillGoodStatus.isOnline(status);
    }

    public SeckillGoodQuery buildParams() {
        if (this.pageSize == null) {
            this.pageSize = 7;
        }
        if (this.pageSize > 100) {
            this.pageSize = 100;
        }
        if (this.pageNum == null || this.pageNum == 0) {
            this.pageNum = 1;
        }
        return this;
    }

}
