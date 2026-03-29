package com.example.lecturesystem.modules.aiconfig.vo;

import java.time.LocalDateTime;
import java.util.List;

public class ProviderConfigListItemVO {
    private Long id;
    private String providerCode;
    private String providerName;
    private String apiBaseUrl;
    private String tokenMask;
    private String defaultModel;
    private String connectStatus;
    private Integer status;
    private String remark;
    private Integer modelCount;
    private LocalDateTime updateTime;
    private List<ProviderModelVO> models;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getProviderCode() { return providerCode; }
    public void setProviderCode(String providerCode) { this.providerCode = providerCode; }
    public String getProviderName() { return providerName; }
    public void setProviderName(String providerName) { this.providerName = providerName; }
    public String getApiBaseUrl() { return apiBaseUrl; }
    public void setApiBaseUrl(String apiBaseUrl) { this.apiBaseUrl = apiBaseUrl; }
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
    public Integer getModelCount() { return modelCount; }
    public void setModelCount(Integer modelCount) { this.modelCount = modelCount; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    public List<ProviderModelVO> getModels() { return models; }
    public void setModels(List<ProviderModelVO> models) { this.models = models; }
}