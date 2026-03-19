package com.example.lecturesystem.modules.attendance.vo;

import java.math.BigDecimal;
import java.time.LocalDate;

public class AttendanceAbnormalUserSummaryVO {
    private Long userId;
    private String username;
    private String realName;
    private String unitName;
    private Long abnormalCount;
    private LocalDate recentAbnormalDate;
    private String recentAbnormalType;
    private Integer riskScore;
    private String riskLevel;
    private Long recent7DayAbnormalCount;
    private Long previous7DayAbnormalCount;
    private String trendDirection;
    private String mainReasonKey;
    private String mainReasonLabel;
    private String mainReasonTag;
    private String topLocation;
    private Long topLocationCount;
    private BigDecimal locationConcentrationRate;
    private Long morningAbnormalCount;
    private Long afternoonAbnormalCount;
    private Long eveningAbnormalCount;
    private String peakTimeSlot;
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

    public String getTopLocation() {
        return topLocation;
    }

    public void setTopLocation(String topLocation) {
        this.topLocation = topLocation;
    }

    public Long getTopLocationCount() {
        return topLocationCount;
    }

    public void setTopLocationCount(Long topLocationCount) {
        this.topLocationCount = topLocationCount;
    }

    public BigDecimal getLocationConcentrationRate() {
        return locationConcentrationRate;
    }

    public void setLocationConcentrationRate(BigDecimal locationConcentrationRate) {
        this.locationConcentrationRate = locationConcentrationRate;
    }

    public Long getMorningAbnormalCount() {
        return morningAbnormalCount;
    }

    public void setMorningAbnormalCount(Long morningAbnormalCount) {
        this.morningAbnormalCount = morningAbnormalCount;
    }

    public Long getAfternoonAbnormalCount() {
        return afternoonAbnormalCount;
    }

    public void setAfternoonAbnormalCount(Long afternoonAbnormalCount) {
        this.afternoonAbnormalCount = afternoonAbnormalCount;
    }

    public Long getEveningAbnormalCount() {
        return eveningAbnormalCount;
    }

    public void setEveningAbnormalCount(Long eveningAbnormalCount) {
        this.eveningAbnormalCount = eveningAbnormalCount;
    }

    public String getPeakTimeSlot() {
        return peakTimeSlot;
    }

    public void setPeakTimeSlot(String peakTimeSlot) {
        this.peakTimeSlot = peakTimeSlot;
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
