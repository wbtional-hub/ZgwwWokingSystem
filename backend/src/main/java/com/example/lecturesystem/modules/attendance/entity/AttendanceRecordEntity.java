package com.example.lecturesystem.modules.attendance.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class AttendanceRecordEntity {
    private Long id;
    private Long unitId;
    private Long userId;
    private LocalDate attendanceDate;
    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;
    private String checkInAddress;
    private String checkOutAddress;
    private BigDecimal checkInLatitude;
    private BigDecimal checkInLongitude;
    private Integer checkInDistanceMeters;
    private String checkInResult;
    private String checkInFailReason;
    private Integer validFlag;
    private LocalDateTime createTime;

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
    public String getCheckInAddress() { return checkInAddress; }
    public void setCheckInAddress(String checkInAddress) { this.checkInAddress = checkInAddress; }
    public String getCheckOutAddress() { return checkOutAddress; }
    public void setCheckOutAddress(String checkOutAddress) { this.checkOutAddress = checkOutAddress; }
    public BigDecimal getCheckInLatitude() { return checkInLatitude; }
    public void setCheckInLatitude(BigDecimal checkInLatitude) { this.checkInLatitude = checkInLatitude; }
    public BigDecimal getCheckInLongitude() { return checkInLongitude; }
    public void setCheckInLongitude(BigDecimal checkInLongitude) { this.checkInLongitude = checkInLongitude; }
    public Integer getCheckInDistanceMeters() { return checkInDistanceMeters; }
    public void setCheckInDistanceMeters(Integer checkInDistanceMeters) { this.checkInDistanceMeters = checkInDistanceMeters; }
    public String getCheckInResult() { return checkInResult; }
    public void setCheckInResult(String checkInResult) { this.checkInResult = checkInResult; }
    public String getCheckInFailReason() { return checkInFailReason; }
    public void setCheckInFailReason(String checkInFailReason) { this.checkInFailReason = checkInFailReason; }
    public Integer getValidFlag() { return validFlag; }
    public void setValidFlag(Integer validFlag) { this.validFlag = validFlag; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
