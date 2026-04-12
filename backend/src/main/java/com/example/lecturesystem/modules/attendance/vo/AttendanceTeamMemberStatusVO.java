package com.example.lecturesystem.modules.attendance.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class AttendanceTeamMemberStatusVO {
    private Long userId;
    private Long unitId;
    private String username;
    private String realName;
    private String unitName;
    private LocalDate attendanceDate;
    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;
    private Boolean hasRecord;
    private Boolean late;
    private Boolean earlyLeave;
    private String statusLabel;
    private String abnormalReason;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getUnitId() { return unitId; }
    public void setUnitId(Long unitId) { this.unitId = unitId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getRealName() { return realName; }
    public void setRealName(String realName) { this.realName = realName; }
    public String getUnitName() { return unitName; }
    public void setUnitName(String unitName) { this.unitName = unitName; }
    public LocalDate getAttendanceDate() { return attendanceDate; }
    public void setAttendanceDate(LocalDate attendanceDate) { this.attendanceDate = attendanceDate; }
    public LocalDateTime getCheckInTime() { return checkInTime; }
    public void setCheckInTime(LocalDateTime checkInTime) { this.checkInTime = checkInTime; }
    public LocalDateTime getCheckOutTime() { return checkOutTime; }
    public void setCheckOutTime(LocalDateTime checkOutTime) { this.checkOutTime = checkOutTime; }
    public Boolean getHasRecord() { return hasRecord; }
    public void setHasRecord(Boolean hasRecord) { this.hasRecord = hasRecord; }
    public Boolean getLate() { return late; }
    public void setLate(Boolean late) { this.late = late; }
    public Boolean getEarlyLeave() { return earlyLeave; }
    public void setEarlyLeave(Boolean earlyLeave) { this.earlyLeave = earlyLeave; }
    public String getStatusLabel() { return statusLabel; }
    public void setStatusLabel(String statusLabel) { this.statusLabel = statusLabel; }
    public String getAbnormalReason() { return abnormalReason; }
    public void setAbnormalReason(String abnormalReason) { this.abnormalReason = abnormalReason; }
}
