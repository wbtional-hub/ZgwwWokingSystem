package com.example.lecturesystem.modules.attendance.entity;

import java.time.LocalTime;

public class AttendanceReminderCandidateEntity {
    private Long userId;
    private Long unitId;
    private String username;
    private String realName;
    private String openid;
    private LocalTime workStartTime;
    private LocalTime amOffTime;
    private LocalTime pmOnTime;
    private LocalTime workEndTime;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getUnitId() {
        return unitId;
    }

    public void setUnitId(Long unitId) {
        this.unitId = unitId;
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

    public String getOpenid() {
        return openid;
    }

    public void setOpenid(String openid) {
        this.openid = openid;
    }

    public LocalTime getWorkStartTime() {
        return workStartTime;
    }

    public void setWorkStartTime(LocalTime workStartTime) {
        this.workStartTime = workStartTime;
    }

    public LocalTime getAmOffTime() {
        return amOffTime;
    }

    public void setAmOffTime(LocalTime amOffTime) {
        this.amOffTime = amOffTime;
    }

    public LocalTime getPmOnTime() {
        return pmOnTime;
    }

    public void setPmOnTime(LocalTime pmOnTime) {
        this.pmOnTime = pmOnTime;
    }

    public LocalTime getWorkEndTime() {
        return workEndTime;
    }

    public void setWorkEndTime(LocalTime workEndTime) {
        this.workEndTime = workEndTime;
    }
}
