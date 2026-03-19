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
    private Integer riskScore;
    private String riskLevel;
    private Long recent7DayAbnormalCount;
    private Long previous7DayAbnormalCount;
    private String trendDirection;
    private String mainReasonKey;
    private String mainReasonLabel;
    private String mainReasonTag;
    private Boolean alertTriggered;
    private String alertRuleText;

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

    public Integer getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(Integer riskScore) {
        this.riskScore = riskScore;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public Long getRecent7DayAbnormalCount() {
        return recent7DayAbnormalCount;
    }

    public void setRecent7DayAbnormalCount(Long recent7DayAbnormalCount) {
        this.recent7DayAbnormalCount = recent7DayAbnormalCount;
    }

    public Long getPrevious7DayAbnormalCount() {
        return previous7DayAbnormalCount;
    }

    public void setPrevious7DayAbnormalCount(Long previous7DayAbnormalCount) {
        this.previous7DayAbnormalCount = previous7DayAbnormalCount;
    }

    public String getTrendDirection() {
        return trendDirection;
    }

    public void setTrendDirection(String trendDirection) {
        this.trendDirection = trendDirection;
    }

    public String getMainReasonKey() {
        return mainReasonKey;
    }

    public void setMainReasonKey(String mainReasonKey) {
        this.mainReasonKey = mainReasonKey;
    }

    public String getMainReasonLabel() {
        return mainReasonLabel;
    }

    public void setMainReasonLabel(String mainReasonLabel) {
        this.mainReasonLabel = mainReasonLabel;
    }

    public String getMainReasonTag() {
        return mainReasonTag;
    }

    public void setMainReasonTag(String mainReasonTag) {
        this.mainReasonTag = mainReasonTag;
    }

    public Boolean getAlertTriggered() {
        return alertTriggered;
    }

    public void setAlertTriggered(Boolean alertTriggered) {
        this.alertTriggered = alertTriggered;
    }

    public String getAlertRuleText() {
        return alertRuleText;
    }

    public void setAlertRuleText(String alertRuleText) {
        this.alertRuleText = alertRuleText;
    }
}
