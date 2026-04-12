package com.example.lecturesystem.modules.attendance.vo;

import java.time.LocalDateTime;
import java.time.LocalTime;

public class AttendanceRuleVO {
    private Long id;
    private Long unitId;
    private String unitName;
    private LocalTime workStartTime;
    private LocalTime workEndTime;
    private Integer lateGraceMinutes;
    private Integer earlyLeaveGraceMinutes;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUnitId() { return unitId; }
    public void setUnitId(Long unitId) { this.unitId = unitId; }
    public String getUnitName() { return unitName; }
    public void setUnitName(String unitName) { this.unitName = unitName; }
    public LocalTime getWorkStartTime() { return workStartTime; }
    public void setWorkStartTime(LocalTime workStartTime) { this.workStartTime = workStartTime; }
    public LocalTime getWorkEndTime() { return workEndTime; }
    public void setWorkEndTime(LocalTime workEndTime) { this.workEndTime = workEndTime; }
    public Integer getLateGraceMinutes() { return lateGraceMinutes; }
    public void setLateGraceMinutes(Integer lateGraceMinutes) { this.lateGraceMinutes = lateGraceMinutes; }
    public Integer getEarlyLeaveGraceMinutes() { return earlyLeaveGraceMinutes; }
    public void setEarlyLeaveGraceMinutes(Integer earlyLeaveGraceMinutes) { this.earlyLeaveGraceMinutes = earlyLeaveGraceMinutes; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
