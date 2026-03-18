package com.example.lecturesystem.modules.attendance.vo;

import java.time.LocalDate;

public class AttendanceAbnormalUserSummaryVO {
    private Long userId;
    private String username;
    private String realName;
    private String unitName;
    private Long abnormalCount;
    private LocalDate recentAbnormalDate;
    private String recentAbnormalType;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public String getUnitName() {
        return unitName;
    }

    public void setUnitName(String unitName) {
        this.unitName = unitName;
    }

    public Long getAbnormalCount() {
        return abnormalCount;
    }

    public void setAbnormalCount(Long abnormalCount) {
        this.abnormalCount = abnormalCount;
    }

    public LocalDate getRecentAbnormalDate() {
        return recentAbnormalDate;
    }

    public void setRecentAbnormalDate(LocalDate recentAbnormalDate) {
        this.recentAbnormalDate = recentAbnormalDate;
    }

    public String getRecentAbnormalType() {
        return recentAbnormalType;
    }

    public void setRecentAbnormalType(String recentAbnormalType) {
        this.recentAbnormalType = recentAbnormalType;
    }
}
