package com.example.lecturesystem.modules.attendance.vo;

import java.math.BigDecimal;

public class AttendanceAbnormalUserRankVO {
    private Long userId;
    private String username;
    private String realName;
    private String unitName;
    private Long totalCount;
    private Long abnormalCount;
    private BigDecimal abnormalRate;

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

    public Long getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Long totalCount) {
        this.totalCount = totalCount;
    }

    public Long getAbnormalCount() {
        return abnormalCount;
    }

    public void setAbnormalCount(Long abnormalCount) {
        this.abnormalCount = abnormalCount;
    }

    public BigDecimal getAbnormalRate() {
        return abnormalRate;
    }

    public void setAbnormalRate(BigDecimal abnormalRate) {
        this.abnormalRate = abnormalRate;
    }
}
