package com.example.lecturesystem.modules.attendance.dto;

import java.math.BigDecimal;

public class CheckInRequest {
    private String address;
    private String action;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private Integer accuracyMeters;
    private String locationSource;
    private String locationProvider;

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }

    public Integer getAccuracyMeters() {
        return accuracyMeters;
    }

    public void setAccuracyMeters(Integer accuracyMeters) {
        this.accuracyMeters = accuracyMeters;
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
}
