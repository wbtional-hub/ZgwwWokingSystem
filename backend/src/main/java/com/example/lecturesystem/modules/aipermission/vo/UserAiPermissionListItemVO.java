package com.example.lecturesystem.modules.aipermission.vo;

import java.time.LocalDateTime;

public class UserAiPermissionListItemVO {
    private Long id;
    private Long userId;
    private String username;
    private String realName;
    private Long providerConfigId;
    private String providerName;
    private Boolean canManageProvider;
    private Boolean canUseAi;
    private Boolean canTrainSkill;
    private Boolean canPublishSkill;
    private Boolean canUseAgent;
    private Boolean canRunAnalysis;
    private Integer status;
    private LocalDateTime grantTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getRealName() { return realName; }
    public void setRealName(String realName) { this.realName = realName; }
    public Long getProviderConfigId() { return providerConfigId; }
    public void setProviderConfigId(Long providerConfigId) { this.providerConfigId = providerConfigId; }
    public String getProviderName() { return providerName; }
    public void setProviderName(String providerName) { this.providerName = providerName; }
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
}