package com.example.lecturesystem.modules.attendance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class SaveAttendanceRuleRequest {
    @NotBlank(message = "上班时间不能为空")
    private String workStartTime;

    @NotBlank(message = "下班时间不能为空")
    private String workEndTime;

    @NotNull(message = "迟到宽限分钟不能为空")
    private Integer lateGraceMinutes;

    @NotNull(message = "早退宽限分钟不能为空")
    private Integer earlyLeaveGraceMinutes;

    private Integer status = 1;

    public String getWorkStartTime() { return workStartTime; }
    public void setWorkStartTime(String workStartTime) { this.workStartTime = workStartTime; }
    public String getWorkEndTime() { return workEndTime; }
    public void setWorkEndTime(String workEndTime) { this.workEndTime = workEndTime; }
    public Integer getLateGraceMinutes() { return lateGraceMinutes; }
    public void setLateGraceMinutes(Integer lateGraceMinutes) { this.lateGraceMinutes = lateGraceMinutes; }
    public Integer getEarlyLeaveGraceMinutes() { return earlyLeaveGraceMinutes; }
    public void setEarlyLeaveGraceMinutes(Integer earlyLeaveGraceMinutes) { this.earlyLeaveGraceMinutes = earlyLeaveGraceMinutes; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}
