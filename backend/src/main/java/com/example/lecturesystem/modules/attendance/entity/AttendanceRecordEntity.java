package com.example.lecturesystem.modules.attendance.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class AttendanceRecordEntity {
    private Long id;
    private Long unitId;
    private Long userId;
    private LocalDate attendanceDate;
    private String checkType;
    private LocalDateTime checkTime;

    /**
     * 上午上班
     */
    private LocalDateTime checkInTime;

    /**
     * 上午下班
     */
    private LocalDateTime amOffTime;

    /**
     * 下午上班
     */
    private LocalDateTime pmOnTime;

    /**
     * 下午下班
     */
    private LocalDateTime checkOutTime;

    private String checkInAddress;
    private String amOffAddress;
    private String pmOnAddress;
    private String checkOutAddress;

    private BigDecimal checkInLatitude;
    private BigDecimal checkInLongitude;
    private Integer checkInDistanceMeters;

    private BigDecimal amOffLatitude;
    private BigDecimal amOffLongitude;
    private Integer amOffDistanceMeters;

    private BigDecimal pmOnLatitude;
    private BigDecimal pmOnLongitude;
    private Integer pmOnDistanceMeters;

    private BigDecimal checkOutLatitude;
    private BigDecimal checkOutLongitude;
    private Integer checkOutDistanceMeters;

    private String checkInResult;
    private String checkInFailReason;
    private String locationSource;
    private String locationProvider;
    private Integer validFlag;
    private LocalDateTime createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUnitId() {
        return unitId;
    }

    public void setUnitId(Long unitId) {
        this.unitId = unitId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public LocalDate getAttendanceDate() {
        return attendanceDate;
    }

    public void setAttendanceDate(LocalDate attendanceDate) {
        this.attendanceDate = attendanceDate;
    }

    public String getCheckType() {
        return checkType;
    }

    public void setCheckType(String checkType) {
        this.checkType = checkType;
    }

    public LocalDateTime getCheckTime() {
        return checkTime;
    }

    public void setCheckTime(LocalDateTime checkTime) {
        this.checkTime = checkTime;
    }

    public LocalDateTime getCheckInTime() {
        return checkInTime;
    }

    public void setCheckInTime(LocalDateTime checkInTime) {
        this.checkInTime = checkInTime;
    }

    public LocalDateTime getAmOffTime() {
        return amOffTime;
    }

    public void setAmOffTime(LocalDateTime amOffTime) {
        this.amOffTime = amOffTime;
    }

    public LocalDateTime getPmOnTime() {
        return pmOnTime;
    }

    public void setPmOnTime(LocalDateTime pmOnTime) {
        this.pmOnTime = pmOnTime;
    }

    public LocalDateTime getCheckOutTime() {
        return checkOutTime;
    }

    public void setCheckOutTime(LocalDateTime checkOutTime) {
        this.checkOutTime = checkOutTime;
    }

    public String getCheckInAddress() {
        return checkInAddress;
    }

    public void setCheckInAddress(String checkInAddress) {
        this.checkInAddress = checkInAddress;
    }

    public String getAmOffAddress() {
        return amOffAddress;
    }

    public void setAmOffAddress(String amOffAddress) {
        this.amOffAddress = amOffAddress;
    }

    public String getPmOnAddress() {
        return pmOnAddress;
    }

    public void setPmOnAddress(String pmOnAddress) {
        this.pmOnAddress = pmOnAddress;
    }

    public String getCheckOutAddress() {
        return checkOutAddress;
    }

    public void setCheckOutAddress(String checkOutAddress) {
        this.checkOutAddress = checkOutAddress;
    }

    public BigDecimal getCheckInLatitude() {
        return checkInLatitude;
    }

    public void setCheckInLatitude(BigDecimal checkInLatitude) {
        this.checkInLatitude = checkInLatitude;
    }

    public BigDecimal getCheckInLongitude() {
        return checkInLongitude;
    }

    public void setCheckInLongitude(BigDecimal checkInLongitude) {
        this.checkInLongitude = checkInLongitude;
    }

    public Integer getCheckInDistanceMeters() {
        return checkInDistanceMeters;
    }

    public void setCheckInDistanceMeters(Integer checkInDistanceMeters) {
        this.checkInDistanceMeters = checkInDistanceMeters;
    }

    public BigDecimal getAmOffLatitude() {
        return amOffLatitude;
    }

    public void setAmOffLatitude(BigDecimal amOffLatitude) {
        this.amOffLatitude = amOffLatitude;
    }

    public BigDecimal getAmOffLongitude() {
        return amOffLongitude;
    }

    public void setAmOffLongitude(BigDecimal amOffLongitude) {
        this.amOffLongitude = amOffLongitude;
    }

    public Integer getAmOffDistanceMeters() {
        return amOffDistanceMeters;
    }

    public void setAmOffDistanceMeters(Integer amOffDistanceMeters) {
        this.amOffDistanceMeters = amOffDistanceMeters;
    }

    public BigDecimal getPmOnLatitude() {
        return pmOnLatitude;
    }

    public void setPmOnLatitude(BigDecimal pmOnLatitude) {
        this.pmOnLatitude = pmOnLatitude;
    }

    public BigDecimal getPmOnLongitude() {
        return pmOnLongitude;
    }

    public void setPmOnLongitude(BigDecimal pmOnLongitude) {
        this.pmOnLongitude = pmOnLongitude;
    }

    public Integer getPmOnDistanceMeters() {
        return pmOnDistanceMeters;
    }

    public void setPmOnDistanceMeters(Integer pmOnDistanceMeters) {
        this.pmOnDistanceMeters = pmOnDistanceMeters;
    }

    public BigDecimal getCheckOutLatitude() {
        return checkOutLatitude;
    }

    public void setCheckOutLatitude(BigDecimal checkOutLatitude) {
        this.checkOutLatitude = checkOutLatitude;
    }

    public BigDecimal getCheckOutLongitude() {
        return checkOutLongitude;
    }

    public void setCheckOutLongitude(BigDecimal checkOutLongitude) {
        this.checkOutLongitude = checkOutLongitude;
    }

    public Integer getCheckOutDistanceMeters() {
        return checkOutDistanceMeters;
    }

    public void setCheckOutDistanceMeters(Integer checkOutDistanceMeters) {
        this.checkOutDistanceMeters = checkOutDistanceMeters;
    }

    public String getCheckInResult() {
        return checkInResult;
    }

    public void setCheckInResult(String checkInResult) {
        this.checkInResult = checkInResult;
    }

    public String getCheckInFailReason() {
        return checkInFailReason;
    }

    public void setCheckInFailReason(String checkInFailReason) {
        this.checkInFailReason = checkInFailReason;
    }

    public String getLocationSource() {
        return locationSource;
    }

    public void setLocationSource(String locationSource) {
        this.locationSource = locationSource;
    }

    public String getLocationProvider() {
        return locationProvider;
    }

    public void setLocationProvider(String locationProvider) {
        this.locationProvider = locationProvider;
    }

    public Integer getValidFlag() {
        return validFlag;
    }

    public void setValidFlag(Integer validFlag) {
        this.validFlag = validFlag;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}