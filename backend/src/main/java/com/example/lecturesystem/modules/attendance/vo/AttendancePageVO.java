package com.example.lecturesystem.modules.attendance.vo;

import java.util.ArrayList;
import java.util.List;

public class AttendancePageVO {
    private Integer pageNo;
    private Integer pageSize;
    private Long total;
    private List<AttendanceRecordListItemVO> list = new ArrayList<>();

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

    public List<AttendanceRecordListItemVO> getList() {
        return list;
    }

    public void setList(List<AttendanceRecordListItemVO> list) {
        this.list = list;
    }
}
