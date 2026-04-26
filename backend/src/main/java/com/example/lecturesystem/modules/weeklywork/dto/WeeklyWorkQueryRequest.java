package com.example.lecturesystem.modules.weeklywork.dto;

import com.example.lecturesystem.modules.permission.support.TreePathScopedRequest;

import java.util.List;

public class WeeklyWorkQueryRequest implements TreePathScopedRequest {
    private String treePathPrefix;
    private List<Long> crossDeptVisibleUserIds;
    private String weekNo;
    private String status;
    private Long userId;

    public String getTreePathPrefix() {
        return treePathPrefix;
    }

    public void setTreePathPrefix(String treePathPrefix) {
        this.treePathPrefix = treePathPrefix;
    }

    public List<Long> getCrossDeptVisibleUserIds() {
        return crossDeptVisibleUserIds;
    }

    public void setCrossDeptVisibleUserIds(List<Long> crossDeptVisibleUserIds) {
        this.crossDeptVisibleUserIds = crossDeptVisibleUserIds;
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
