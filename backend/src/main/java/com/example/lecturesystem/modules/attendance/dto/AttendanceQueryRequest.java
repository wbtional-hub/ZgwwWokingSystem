package com.example.lecturesystem.modules.attendance.dto;

import com.example.lecturesystem.modules.permission.support.UnitScopedRequest;

public class AttendanceQueryRequest implements UnitScopedRequest {
    private Long unitId;
    private String keywords;
    private String unitName;
    private String dateFrom;
    private String dateTo;

    @Override
    public Long getUnitId() {
        return unitId;
    }

    @Override
    public void setUnitId(Long unitId) {
        this.unitId = unitId;
    }

    public String getKeywords() {
        return keywords;
    }

    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }

    public String getUnitName() {
        return unitName;
    }

    public void setUnitName(String unitName) {
        this.unitName = unitName;
    }

    public String getDateFrom() {
        return dateFrom;
    }

    public void setDateFrom(String dateFrom) {
        this.dateFrom = dateFrom;
    }

    public String getDateTo() {
        return dateTo;
    }

    public void setDateTo(String dateTo) {
        this.dateTo = dateTo;
    }
}
