package com.example.lecturesystem.modules.attendance.vo;

import java.math.BigDecimal;

public class AttendanceAbnormalReasonDistributionVO {
    private Long userId;
    private String reasonKey;
    private String reasonLabel;
    private String reasonTag;
    private Long count;
    private BigDecimal rate;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getReasonKey() {
        return reasonKey;
    }

    public void setReasonKey(String reasonKey) {
        this.reasonKey = reasonKey;
    }

    public String getReasonLabel() {
        return reasonLabel;
    }

    public void setReasonLabel(String reasonLabel) {
        this.reasonLabel = reasonLabel;
    }

    public String getReasonTag() {
        return reasonTag;
    }

    public void setReasonTag(String reasonTag) {
        this.reasonTag = reasonTag;
    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }

    public BigDecimal getRate() {
        return rate;
    }

    public void setRate(BigDecimal rate) {
        this.rate = rate;
    }
}
