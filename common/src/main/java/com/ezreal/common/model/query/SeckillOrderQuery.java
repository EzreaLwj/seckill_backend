package com.ezreal.common.model.query;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class SeckillOrderQuery {

    /**
     * 关键词
     */
    private String keyword;

    /**
     * 当前页的记录数
     */
    private Integer pageSize;

    /**
     * 当前页码
     */
    private Integer pageNumber;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 查询的用户
     */
    private Long checkId;

    public SeckillOrderQuery buildParams() {
        if (this.pageSize == null) {
            this.pageSize = 7;
        }
        if (this.pageSize > 100) {
            this.pageSize = 100;
        }
        if (this.pageNumber == null || this.pageNumber == 0) {
            this.pageNumber = 1;
        }
        return this;
    }
}
