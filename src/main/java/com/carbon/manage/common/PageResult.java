package com.carbon.manage.common;

import java.util.List;
import lombok.Data;

/**
 * 分页结果包装。
 */
@Data
public class PageResult<T> {

    private long total;
    private int pageNum;
    private int pageSize;
    private List<T> list;

    public static <T> PageResult<T> of(long total, int pageNum, int pageSize, List<T> list) {
        PageResult<T> result = new PageResult<>();
        result.setTotal(total);
        result.setPageNum(pageNum);
        result.setPageSize(pageSize);
        result.setList(list);
        return result;
    }
}
