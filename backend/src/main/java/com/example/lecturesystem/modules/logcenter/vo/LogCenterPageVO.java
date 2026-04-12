package com.example.lecturesystem.modules.logcenter.vo;

import java.util.List;

public class LogCenterPageVO {
    private Integer pageNo;
    private Integer pageSize;
    private Long total;
    private List<LogCenterListItemVO> list;

    public Integer getPageNo() {
        return pageNo;
    }

    public void setPageNo(Integer pageNo) {
        this.pageNo = pageNo;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }

    public List<LogCenterListItemVO> getList() {
        return list;
    }

    public void setList(List<LogCenterListItemVO> list) {
        this.list = list;
    }
}
