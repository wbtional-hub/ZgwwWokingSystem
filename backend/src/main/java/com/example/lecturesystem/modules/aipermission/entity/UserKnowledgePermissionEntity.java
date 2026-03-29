package com.example.lecturesystem.modules.aipermission.entity;

import java.time.LocalDateTime;

public class UserKnowledgePermissionEntity {
    private Long id;
    private Long userId;
    private Long baseId;
    private Boolean canView;
    private Boolean canUpload;
    private Boolean canTrainSkill;
    private Boolean canAnalyze;
    private Integer status;
    private LocalDateTime grantTime;
    private String grantUser;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getBaseId() { return baseId; }
    public void setBaseId(Long baseId) { this.baseId = baseId; }
    public Boolean getCanView() { return canView; }
    public void setCanView(Boolean canView) { this.canView = canView; }
    public Boolean getCanUpload() { return canUpload; }
    public void setCanUpload(Boolean canUpload) { this.canUpload = canUpload; }
    public Boolean getCanTrainSkill() { return canTrainSkill; }
    public void setCanTrainSkill(Boolean canTrainSkill) { this.canTrainSkill = canTrainSkill; }
    public Boolean getCanAnalyze() { return canAnalyze; }
    public void setCanAnalyze(Boolean canAnalyze) { this.canAnalyze = canAnalyze; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public LocalDateTime getGrantTime() { return grantTime; }
    public void setGrantTime(LocalDateTime grantTime) { this.grantTime = grantTime; }
    public String getGrantUser() { return grantUser; }
    public void setGrantUser(String grantUser) { this.grantUser = grantUser; }
}