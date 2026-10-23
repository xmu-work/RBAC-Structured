package com.iam.platform.common.result;

import lombok.Data;

import java.util.List;

/**
 * 分页响应结果。
 *
 * <p>对应 API 设计说明书 1.3 分页响应 data 结构：
 * <pre>
 * { "total": 10000, "page": 1, "page_size": 20, "list": [] }
 * </pre>
 */
@Data
public class PageResult<T> {

    /** 总记录数 */
    private long total;

    /** 当前页码（从 1 开始） */
    private int page;

    /** 每页大小 */
    private int pageSize;

    /** 数据列表 */
    private List<T> list;

    public PageResult() {
    }

    public PageResult(long total, int page, int pageSize, List<T> list) {
        this.total = total;
        this.page = page;
        this.pageSize = pageSize;
        this.list = list;
    }

    public static <T> PageResult<T> of(long total, int page, int pageSize, List<T> list) {
        return new PageResult<>(total, page, pageSize, list);
    }
}
