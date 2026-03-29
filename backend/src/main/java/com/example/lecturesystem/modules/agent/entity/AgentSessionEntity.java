package com.example.lecturesystem.modules.agent.entity;

import java.time.LocalDateTime;

public class AgentSessionEntity {
    private Long id;
    private Long userId;
    private Long skillId;
    private Long skillVersionId;
    private Long providerConfigId;
    private String modelCode;
    private Long baseId;
    private String sessionTitle;
    private String status;
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getSkillId() { return skillId; }
    public void setSkillId(Long skillId) { this.skillId = skillId; }
    public Long getSkillVersionId() { return skillVersionId; }
    public void setSkillVersionId(Long skillVersionId) { this.skillVersionId = skillVersionId; }
    public Long getProviderConfigId() { return providerConfigId; }
    public void setProviderConfigId(Long providerConfigId) { this.providerConfigId = providerConfigId; }
    public String getModelCode() { return modelCode; }
    public void setModelCode(String modelCode) { this.modelCode = modelCode; }
    public Long getBaseId() { return baseId; }
    public void setBaseId(Long baseId) { this.baseId = baseId; }
    public String getSessionTitle() { return sessionTitle; }
    public void setSessionTitle(String sessionTitle) { this.sessionTitle = sessionTitle; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}