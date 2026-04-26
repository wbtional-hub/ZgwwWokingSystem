package com.example.lecturesystem.modules.attendance.vo;

import java.time.LocalDateTime;

public class AttendanceNodeStatusVO {
    private String nodeCode;
    private String nodeTitleShort;
    private String timeRangeText;
    private LocalDateTime actualPunchTime;
    private String statusCode;
    private Boolean canPunch;
    private Boolean canApplyMakeup;
    private Boolean canEvidence;
    private Boolean needEarlyConfirm;
    private Boolean finalResolved;
    private String simpleRemark;

    public String getNodeCode() {
        return nodeCode;
    }

    public void setNodeCode(String nodeCode) {
        this.nodeCode = nodeCode;
    }

    public String getNodeTitleShort() {
        return nodeTitleShort;
    }

    public void setNodeTitleShort(String nodeTitleShort) {
        this.nodeTitleShort = nodeTitleShort;
    }

    public String getTimeRangeText() {
        return timeRangeText;
    }

    public void setTimeRangeText(String timeRangeText) {
        this.timeRangeText = timeRangeText;
    }

    public LocalDateTime getActualPunchTime() {
        return actualPunchTime;
    }

    public void setActualPunchTime(LocalDateTime actualPunchTime) {
        this.actualPunchTime = actualPunchTime;
    }

    public String getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(String statusCode) {
        this.statusCode = statusCode;
    }

    public Boolean getCanPunch() {
        return canPunch;
    }

    public void setCanPunch(Boolean canPunch) {
        this.canPunch = canPunch;
    }

    public Boolean getCanApplyMakeup() {
        return canApplyMakeup;
    }

    public void setCanApplyMakeup(Boolean canApplyMakeup) {
        this.canApplyMakeup = canApplyMakeup;
    }

    public Boolean getCanEvidence() {
        return canEvidence;
    }

    public void setCanEvidence(Boolean canEvidence) {
        this.canEvidence = canEvidence;
    }

    public Boolean getNeedEarlyConfirm() {
        return needEarlyConfirm;
    }

    public void setNeedEarlyConfirm(Boolean needEarlyConfirm) {
        this.needEarlyConfirm = needEarlyConfirm;
    }

    public Boolean getFinalResolved() {
        return finalResolved;
    }

    public void setFinalResolved(Boolean finalResolved) {
        this.finalResolved = finalResolved;
    }

    public String getSimpleRemark() {
        return simpleRemark;
    }

    public void setSimpleRemark(String simpleRemark) {
        this.simpleRemark = simpleRemark;
    }
}
