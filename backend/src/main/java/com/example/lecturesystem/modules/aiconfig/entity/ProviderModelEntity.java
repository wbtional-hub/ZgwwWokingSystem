package com.example.lecturesystem.modules.aiconfig.entity;

import java.time.LocalDateTime;

public class ProviderModelEntity {
    private Long id;
    private Long providerConfigId;
    private String modelCode;
    private String modelName;
    private String modelType;
    private Boolean supportKnowledge;
    private Boolean supportSkillTrain;
    private Boolean supportAgentChat;
    private Boolean supportAnalysis;
    private Integer status;
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getProviderConfigId() { return providerConfigId; }
    public void setProviderConfigId(Long providerConfigId) { this.providerConfigId = providerConfigId; }
    public String getModelCode() { return modelCode; }
    public void setModelCode(String modelCode) { this.modelCode = modelCode; }
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    public String getModelType() { return modelType; }
    public void setModelType(String modelType) { this.modelType = modelType; }
    public Boolean getSupportKnowledge() { return supportKnowledge; }
    public void setSupportKnowledge(Boolean supportKnowledge) { this.supportKnowledge = supportKnowledge; }
    public Boolean getSupportSkillTrain() { return supportSkillTrain; }
    public void setSupportSkillTrain(Boolean supportSkillTrain) { this.supportSkillTrain = supportSkillTrain; }
    public Boolean getSupportAgentChat() { return supportAgentChat; }
    public void setSupportAgentChat(Boolean supportAgentChat) { this.supportAgentChat = supportAgentChat; }
    public Boolean getSupportAnalysis() { return supportAnalysis; }
    public void setSupportAnalysis(Boolean supportAnalysis) { this.supportAnalysis = supportAnalysis; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}