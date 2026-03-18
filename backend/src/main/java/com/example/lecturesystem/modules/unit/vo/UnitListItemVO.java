package com.example.lecturesystem.modules.unit.vo;

import java.time.LocalDateTime;

public class UnitListItemVO {
    private Long id;
    private String unitName;
    private String unitCode;
    private Integer status;
    private Long adminUserId;
    private String adminUsername;
    private String adminRealName;
    private Long attendanceLocationId;
    private String attendanceLocationName;
    private String attendanceLocationAddress;
    private java.math.BigDecimal attendanceLocationLongitude;
    private java.math.BigDecimal attendanceLocationLatitude;
    private Integer attendanceLocationRadiusMeters;
    private Integer attendanceLocationStatus;
    private LocalDateTime createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUnitName() {
        return unitName;
    }

    public void setUnitName(String unitName) {
        this.unitName = unitName;
    }

    public String getUnitCode() {
        return unitCode;
    }

    public void setUnitCode(String unitCode) {
        this.unitCode = unitCode;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Long getAdminUserId() {
        return adminUserId;
    }

    public void setAdminUserId(Long adminUserId) {
        this.adminUserId = adminUserId;
    }

    public String getAdminUsername() {
        return adminUsername;
    }

    public void setAdminUsername(String adminUsername) {
        this.adminUsername = adminUsername;
    }

    public String getAdminRealName() {
        return adminRealName;
    }

    public void setAdminRealName(String adminRealName) {
        this.adminRealName = adminRealName;
    }

    public Long getAttendanceLocationId() {
        return attendanceLocationId;
    }

    public void setAttendanceLocationId(Long attendanceLocationId) {
        this.attendanceLocationId = attendanceLocationId;
    }

    public String getAttendanceLocationName() {
        return attendanceLocationName;
    }

    public void setAttendanceLocationName(String attendanceLocationName) {
        this.attendanceLocationName = attendanceLocationName;
    }

    public String getAttendanceLocationAddress() {
        return attendanceLocationAddress;
    }

    public void setAttendanceLocationAddress(String attendanceLocationAddress) {
        this.attendanceLocationAddress = attendanceLocationAddress;
    }

    public java.math.BigDecimal getAttendanceLocationLongitude() {
        return attendanceLocationLongitude;
    }

    public void setAttendanceLocationLongitude(java.math.BigDecimal attendanceLocationLongitude) {
        this.attendanceLocationLongitude = attendanceLocationLongitude;
    }

    public java.math.BigDecimal getAttendanceLocationLatitude() {
        return attendanceLocationLatitude;
    }

    public void setAttendanceLocationLatitude(java.math.BigDecimal attendanceLocationLatitude) {
        this.attendanceLocationLatitude = attendanceLocationLatitude;
    }

    public Integer getAttendanceLocationRadiusMeters() {
        return attendanceLocationRadiusMeters;
    }

    public void setAttendanceLocationRadiusMeters(Integer attendanceLocationRadiusMeters) {
        this.attendanceLocationRadiusMeters = attendanceLocationRadiusMeters;
    }

    public Integer getAttendanceLocationStatus() {
        return attendanceLocationStatus;
    }

    public void setAttendanceLocationStatus(Integer attendanceLocationStatus) {
        this.attendanceLocationStatus = attendanceLocationStatus;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
