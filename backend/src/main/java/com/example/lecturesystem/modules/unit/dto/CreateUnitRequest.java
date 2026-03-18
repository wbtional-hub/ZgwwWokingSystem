package com.example.lecturesystem.modules.unit.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

public class CreateUnitRequest {
    @NotBlank(message = "单位名称不能为空")
    private String unitName;
    @NotBlank(message = "单位编码不能为空")
    private String unitCode;
    private String adminUsername;
    private String adminRealName;
    private String adminPassword;
    @NotNull(message = "状态不能为空")
    private Integer status;

    public String getUnitName() { return unitName; }
    public void setUnitName(String unitName) { this.unitName = unitName; }
    public String getUnitCode() { return unitCode; }
    public void setUnitCode(String unitCode) { this.unitCode = unitCode; }
    public String getAdminUsername() { return adminUsername; }
    public void setAdminUsername(String adminUsername) { this.adminUsername = adminUsername; }
    public String getAdminRealName() { return adminRealName; }
    public void setAdminRealName(String adminRealName) { this.adminRealName = adminRealName; }
    public String getAdminPassword() { return adminPassword; }
    public void setAdminPassword(String adminPassword) { this.adminPassword = adminPassword; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}
