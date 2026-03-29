package com.example.lecturesystem.modules.aipermission.entity;

import java.time.LocalDateTime;

public class UserAiPermissionEntity {
    private Long id;
    private Long userId;
    private Long providerConfigId;
    private Boolean canManageProvider;
    private Boolean canUseAi;
    private Boolean canTrainSkill;
    private Boolean canPublishSkill;
    private Boolean canUseAgent;
    private Boolean canRunAnalysis;
    private Integer status;
    private LocalDateTime grantTime;
    private String grantUser;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getProviderConfigId() { return providerConfigId; }
    public void setProviderConfigId(Long providerConfigId) { this.providerConfigId = providerConfigId; }
    public Boolean getCanManageProvider() { return canManageProvider; }
    public void setCanManageProvider(Boolean canManageProvider) { this.canManageProvider = canManageProvider; }
    public Boolean getCanUseAi() { return canUseAi; }
    public void setCanUseAi(Boolean canUseAi) { this.canUseAi = canUseAi; }
    public Boolean getCanTrainSkill() { return canTrainSkill; }
    public void setCanTrainSkill(Boolean canTrainSkill) { this.canTrainSkill = canTrainSkill; }
    public Boolean getCanPublishSkill() { return canPublishSkill; }
    public void setCanPublishSkill(Boolean canPublishSkill) { this.canPublishSkill = canPublishSkill; }
    public Boolean getCanUseAgent() { return canUseAgent; }
    public void setCanUseAgent(Boolean canUseAgent) { this.canUseAgent = canUseAgent; }
    public Boolean getCanRunAnalysis() { return canRunAnalysis; }
    public void setCanRunAnalysis(Boolean canRunAnalysis) { this.canRunAnalysis = canRunAnalysis; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public LocalDateTime getGrantTime() { return grantTime; }
    public void setGrantTime(LocalDateTime grantTime) { this.grantTime = grantTime; }
    public String getGrantUser() { return grantUser; }
    public void setGrantUser(String grantUser) { this.grantUser = grantUser; }
}