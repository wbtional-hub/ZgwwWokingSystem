package com.example.lecturesystem.modules.aipermission.dto;

import jakarta.validation.constraints.NotNull;

public class SaveUserAiPermissionRequest {
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotNull(message = "AI接入ID不能为空")
    private Long providerConfigId;

    @NotNull(message = "状态不能为空")
    private Integer status;

    private Boolean canManageProvider;
    private Boolean canUseAi;
    private Boolean canTrainSkill;
    private Boolean canPublishSkill;
    private Boolean canUseAgent;
    private Boolean canRunAnalysis;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getProviderConfigId() { return providerConfigId; }
    public void setProviderConfigId(Long providerConfigId) { this.providerConfigId = providerConfigId; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
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
}