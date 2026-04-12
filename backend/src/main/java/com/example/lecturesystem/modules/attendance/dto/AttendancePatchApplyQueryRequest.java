package com.example.lecturesystem.modules.attendance.dto;

import com.example.lecturesystem.modules.permission.support.TreePathScopedRequest;

public class AttendancePatchApplyQueryRequest implements TreePathScopedRequest {
    private String treePathPrefix;
    private String keywords;
    private String dateFrom;
    private String dateTo;
    private String patchType;
    private String status;
    private Integer pageNo = 1;
    private Integer pageSize = 10;

    @Override
    public String getTreePathPrefix() { return treePathPrefix; }
    @Override
    public void setTreePathPrefix(String treePathPrefix) { this.treePathPrefix = treePathPrefix; }
    public String getKeywords() { return keywords; }
    public void setKeywords(String keywords) { this.keywords = keywords; }
    public String getDateFrom() { return dateFrom; }
    public void setDateFrom(String dateFrom) { this.dateFrom = dateFrom; }
    public String getDateTo() { return dateTo; }
    public void setDateTo(String dateTo) { this.dateTo = dateTo; }
    public String getPatchType() { return patchType; }
    public void setPatchType(String patchType) { this.patchType = patchType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getPageNo() { return pageNo; }
    public void setPageNo(Integer pageNo) { this.pageNo = pageNo; }
    public Integer getPageSize() { return pageSize; }
    public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }

    public int getOffset() {
        int normalizedPageNo = pageNo == null || pageNo < 1 ? 1 : pageNo;
        int normalizedPageSize = pageSize == null || pageSize < 1 ? 10 : pageSize;
        return (normalizedPageNo - 1) * normalizedPageSize;
    }
}
