package com.example.lecturesystem.modules.weeklywork.dto;

import com.example.lecturesystem.modules.permission.support.TreePathScopedRequest;

public class WeeklyWorkQueryRequest implements TreePathScopedRequest {
    private String treePathPrefix;
    private String weekNo;
    private String status;
    private Long userId;

    public String getTreePathPrefix() {
        return treePathPrefix;
    }

    public void setTreePathPrefix(String treePathPrefix) {
        this.treePathPrefix = treePathPrefix;
    }

    public String getWeekNo() {
        return weekNo;
    }

    public void setWeekNo(String weekNo) {
        this.weekNo = weekNo;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
