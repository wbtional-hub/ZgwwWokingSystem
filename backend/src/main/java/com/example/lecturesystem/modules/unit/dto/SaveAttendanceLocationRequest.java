package com.example.lecturesystem.modules.unit.dto;

import java.math.BigDecimal;

public class SaveAttendanceLocationRequest {
    private Long unitId;
    private String locationName;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private Integer radiusMeters;
    private String address;
    private Integer status;

    public Long getUnitId() { return unitId; }
    public void setUnitId(Long unitId) { this.unitId = unitId; }
    public String getLocationName() { return locationName; }
    public void setLocationName(String locationName) { this.locationName = locationName; }
    public BigDecimal getLatitude() { return latitude; }
    public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }
    public BigDecimal getLongitude() { return longitude; }
    public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }
    public Integer getRadiusMeters() { return radiusMeters; }
    public void setRadiusMeters(Integer radiusMeters) { this.radiusMeters = radiusMeters; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}
