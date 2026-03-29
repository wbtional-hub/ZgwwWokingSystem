package com.example.lecturesystem.modules.aipermission.dto;

public class UserAiPermissionQueryRequest {
    private Long userId;
    private Long providerConfigId;
    private Integer status;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getProviderConfigId() { return providerConfigId; }
    public void setProviderConfigId(Long providerConfigId) { this.providerConfigId = providerConfigId; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}