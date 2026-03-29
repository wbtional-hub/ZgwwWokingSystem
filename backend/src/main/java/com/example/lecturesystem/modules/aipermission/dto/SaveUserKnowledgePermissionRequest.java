package com.example.lecturesystem.modules.aipermission.dto;

import jakarta.validation.constraints.NotNull;

public class SaveUserKnowledgePermissionRequest {
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotNull(message = "知识库ID不能为空")
    private Long baseId;

    @NotNull(message = "状态不能为空")
    private Integer status;

    private Boolean canView;
    private Boolean canUpload;
    private Boolean canTrainSkill;
    private Boolean canAnalyze;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getBaseId() { return baseId; }
    public void setBaseId(Long baseId) { this.baseId = baseId; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Boolean getCanView() { return canView; }
    public void setCanView(Boolean canView) { this.canView = canView; }
    public Boolean getCanUpload() { return canUpload; }
    public void setCanUpload(Boolean canUpload) { this.canUpload = canUpload; }
    public Boolean getCanTrainSkill() { return canTrainSkill; }
    public void setCanTrainSkill(Boolean canTrainSkill) { this.canTrainSkill = canTrainSkill; }
    public Boolean getCanAnalyze() { return canAnalyze; }
    public void setCanAnalyze(Boolean canAnalyze) { this.canAnalyze = canAnalyze; }
}