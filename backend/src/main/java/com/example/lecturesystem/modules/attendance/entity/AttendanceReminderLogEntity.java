package com.example.lecturesystem.modules.attendance.entity;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public class AttendanceReminderLogEntity {
    private Long id;
    private Long userId;
    private String openid;
    private LocalDate attendanceDate;
    private String reminderType;
    private String templateId;
    private String templatePayload;
    private String targetUrl;
    private String sendStatus;
    private String wxErrCode;
    private String wxErrMsg;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getOpenid() {
        return openid;
    }

    public void setOpenid(String openid) {
        this.openid = openid;
    }

    public LocalDate getAttendanceDate() {
        return attendanceDate;
    }

    public void setAttendanceDate(LocalDate attendanceDate) {
        this.attendanceDate = attendanceDate;
    }

    public String getReminderType() {
        return reminderType;
    }

    public void setReminderType(String reminderType) {
        this.reminderType = reminderType;
    }

    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    public String getTemplatePayload() {
        return templatePayload;
    }

    public void setTemplatePayload(String templatePayload) {
        this.templatePayload = templatePayload;
    }

    public String getTargetUrl() {
        return targetUrl;
    }

    public void setTargetUrl(String targetUrl) {
        this.targetUrl = targetUrl;
    }

    public String getSendStatus() {
        return sendStatus;
    }

    public void setSendStatus(String sendStatus) {
        this.sendStatus = sendStatus;
    }

    public String getWxErrCode() {
        return wxErrCode;
    }

    public void setWxErrCode(String wxErrCode) {
        this.wxErrCode = wxErrCode;
    }

    public String getWxErrMsg() {
        return wxErrMsg;
    }

    public void setWxErrMsg(String wxErrMsg) {
        this.wxErrMsg = wxErrMsg;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
