package com.example.lecturesystem.modules.weeklywork.dto;

import com.example.lecturesystem.modules.permission.support.UnitScopedRequest;

public class WeeklyWorkQueryRequest implements UnitScopedRequest {
    private Long unitId;
    private String weekNo;
    private String status;
    private Long userId;

    @Override
    public Long getUnitId() {
        return unitId;
    }

    @Override
    public void setUnitId(Long unitId) {
        this.unitId = unitId;
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
