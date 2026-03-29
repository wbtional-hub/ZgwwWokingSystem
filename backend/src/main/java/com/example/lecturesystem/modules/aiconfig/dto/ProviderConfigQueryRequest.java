package com.example.lecturesystem.modules.aiconfig.dto;

public class ProviderConfigQueryRequest {
    private String keywords;
    private Integer status;
    private String connectStatus;

    public String getKeywords() { return keywords; }
    public void setKeywords(String keywords) { this.keywords = keywords; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getConnectStatus() { return connectStatus; }
    public void setConnectStatus(String connectStatus) { this.connectStatus = connectStatus; }
}