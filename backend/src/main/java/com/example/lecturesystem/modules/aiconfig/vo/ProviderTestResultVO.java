package com.example.lecturesystem.modules.aiconfig.vo;

import java.util.List;

public class ProviderTestResultVO {
    private Long providerConfigId;
    private String connectStatus;
    private String message;
    private Integer modelCount;
    private List<ProviderModelVO> models;

    public Long getProviderConfigId() { return providerConfigId; }
    public void setProviderConfigId(Long providerConfigId) { this.providerConfigId = providerConfigId; }
    public String getConnectStatus() { return connectStatus; }
    public void setConnectStatus(String connectStatus) { this.connectStatus = connectStatus; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Integer getModelCount() { return modelCount; }
    public void setModelCount(Integer modelCount) { this.modelCount = modelCount; }
    public List<ProviderModelVO> getModels() { return models; }
    public void setModels(List<ProviderModelVO> models) { this.models = models; }
}