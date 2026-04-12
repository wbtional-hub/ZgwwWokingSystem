package com.example.lecturesystem.modules.attendance.vo;

import java.util.ArrayList;
import java.util.List;

public class AttendancePatchApplyPageVO {
    private Integer pageNo;
    private Integer pageSize;
    private Long total;
    private List<AttendancePatchApplyListItemVO> list = new ArrayList<>();

    public Integer getPageNo() { return pageNo; }
    public void setPageNo(Integer pageNo) { this.pageNo = pageNo; }
    public Integer getPageSize() { return pageSize; }
    public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }
    public Long getTotal() { return total; }
    public void setTotal(Long total) { this.total = total; }
    public List<AttendancePatchApplyListItemVO> getList() { return list; }
    public void setList(List<AttendancePatchApplyListItemVO> list) { this.list = list; }
}
