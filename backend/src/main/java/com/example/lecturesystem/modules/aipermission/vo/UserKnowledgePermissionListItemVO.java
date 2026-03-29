package com.example.lecturesystem.modules.aipermission.vo;

import java.time.LocalDateTime;

public class UserKnowledgePermissionListItemVO {
    private Long id;
    private Long userId;
    private String username;
    private String realName;
    private Long baseId;
    private String baseName;
    private Boolean canView;
    private Boolean canUpload;
    private Boolean canTrainSkill;
    private Boolean canAnalyze;
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
    public Long getBaseId() { return baseId; }
    public void setBaseId(Long baseId) { this.baseId = baseId; }
    public String getBaseName() { return baseName; }
    public void setBaseName(String baseName) { this.baseName = baseName; }
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
}