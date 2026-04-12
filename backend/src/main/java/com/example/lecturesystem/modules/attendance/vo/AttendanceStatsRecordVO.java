package com.example.lecturesystem.modules.attendance.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class AttendanceStatsRecordVO {
    private Long id;
    private Long unitId;
    private Long userId;
    private LocalDate attendanceDate;
    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;
    private String checkInResult;
    private String checkInFailReason;
    private String checkInAddress;
    private String checkOutAddress;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUnitId() { return unitId; }
    public void setUnitId(Long unitId) { this.unitId = unitId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public LocalDate getAttendanceDate() { return attendanceDate; }
    public void setAttendanceDate(LocalDate attendanceDate) { this.attendanceDate = attendanceDate; }
    public LocalDateTime getCheckInTime() { return checkInTime; }
    public void setCheckInTime(LocalDateTime checkInTime) { this.checkInTime = checkInTime; }
    public LocalDateTime getCheckOutTime() { return checkOutTime; }
    public void setCheckOutTime(LocalDateTime checkOutTime) { this.checkOutTime = checkOutTime; }
    public String getCheckInResult() { return checkInResult; }
    public void setCheckInResult(String checkInResult) { this.checkInResult = checkInResult; }
    public String getCheckInFailReason() { return checkInFailReason; }
    public void setCheckInFailReason(String checkInFailReason) { this.checkInFailReason = checkInFailReason; }
    public String getCheckInAddress() { return checkInAddress; }
    public void setCheckInAddress(String checkInAddress) { this.checkInAddress = checkInAddress; }
    public String getCheckOutAddress() { return checkOutAddress; }
    public void setCheckOutAddress(String checkOutAddress) { this.checkOutAddress = checkOutAddress; }
}
