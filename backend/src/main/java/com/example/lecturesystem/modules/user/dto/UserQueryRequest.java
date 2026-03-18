package com.example.lecturesystem.modules.user.dto;

import com.example.lecturesystem.modules.permission.support.UnitScopedRequest;

public class UserQueryRequest implements UnitScopedRequest {
    private Long unitId;
    private String keywords;
    private Integer status;
    private Integer pageNo = 1;
    private Integer pageSize = 10;

    public String getKeywords() {
        return keywords;
    }

    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getPageNo() {
        return pageNo;
    }

    public void setPageNo(Integer pageNo) {
        this.pageNo = pageNo;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    @Override
    public Long getUnitId() {
        return unitId;
    }

    @Override
    public void setUnitId(Long unitId) {
        this.unitId = unitId;
    }

    public int getOffset() {
        return (pageNo - 1) * pageSize;
    }
}
