package com.example.lecturesystem.modules.aiconfig.entity;

import java.time.LocalDateTime;

public class ProviderConfigEntity {
    private Long id;
    private String providerCode;
    private String providerName;
    private String apiBaseUrl;
    private String apiTokenCipher;
    private String tokenMask;
    private String defaultModel;
    private String connectStatus;
    private Integer status;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String createUser;
    private String updateUser;
    private Boolean isDeleted;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getProviderCode() { return providerCode; }
    public void setProviderCode(String providerCode) { this.providerCode = providerCode; }
    public String getProviderName() { return providerName; }
    public void setProviderName(String providerName) { this.providerName = providerName; }
    public String getApiBaseUrl() { return apiBaseUrl; }
    public void setApiBaseUrl(String apiBaseUrl) { this.apiBaseUrl = apiBaseUrl; }
    public String getApiTokenCipher() { return apiTokenCipher; }
    public void setApiTokenCipher(String apiTokenCipher) { this.apiTokenCipher = apiTokenCipher; }
    public String getTokenMask() { return tokenMask; }
    public void setTokenMask(String tokenMask) { this.tokenMask = tokenMask; }
    public String getDefaultModel() { return defaultModel; }
    public void setDefaultModel(String defaultModel) { this.defaultModel = defaultModel; }
    public String getConnectStatus() { return connectStatus; }
    public void setConnectStatus(String connectStatus) { this.connectStatus = connectStatus; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    public String getCreateUser() { return createUser; }
    public void setCreateUser(String createUser) { this.createUser = createUser; }
    public String getUpdateUser() { return updateUser; }
    public void setUpdateUser(String updateUser) { this.updateUser = updateUser; }
    public Boolean getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Boolean isDeleted) { this.isDeleted = isDeleted; }
}