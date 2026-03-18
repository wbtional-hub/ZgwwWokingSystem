package com.example.lecturesystem.modules.attendance.dto;

import com.example.lecturesystem.modules.permission.support.TreePathScopedRequest;

public class AttendanceQueryRequest implements TreePathScopedRequest {
    private String treePathPrefix;
    private String keywords;
    private String unitName;
    private String dateFrom;
    private String dateTo;
    private String checkInStatus;
    private Long userId;
    private Boolean abnormalOnly;

    public String getTreePathPrefix() {
        return treePathPrefix;
    }

    public void setTreePathPrefix(String treePathPrefix) {
        this.treePathPrefix = treePathPrefix;
    }

    public String getKeywords() {
        return keywords;
    }

    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }

    public String getUnitName() {
        return unitName;
    }

    public void setUnitName(String unitName) {
        this.unitName = unitName;
    }

    public String getDateFrom() {
        return dateFrom;
    }

    public void setDateFrom(String dateFrom) {
        this.dateFrom = dateFrom;
    }

    public String getDateTo() {
        return dateTo;
    }

    public void setDateTo(String dateTo) {
        this.dateTo = dateTo;
    }

    public String getCheckInStatus() {
        return checkInStatus;
    }

    public void setCheckInStatus(String checkInStatus) {
        this.checkInStatus = checkInStatus;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Boolean getAbnormalOnly() {
        return abnormalOnly;
    }

    public void setAbnormalOnly(Boolean abnormalOnly) {
        this.abnormalOnly = abnormalOnly;
    }
}
