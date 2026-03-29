package com.example.lecturesystem.modules.aipermission.entity;

import java.time.LocalDateTime;

public class UserSkillPermissionEntity {
    private Long id;
    private Long userId;
    private Long skillId;
    private Boolean canView;
    private Boolean canUse;
    private Boolean canTrain;
    private Boolean canPublish;
    private Integer status;
    private LocalDateTime grantTime;
    private String grantUser;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getSkillId() { return skillId; }
    public void setSkillId(Long skillId) { this.skillId = skillId; }
    public Boolean getCanView() { return canView; }
    public void setCanView(Boolean canView) { this.canView = canView; }
    public Boolean getCanUse() { return canUse; }
    public void setCanUse(Boolean canUse) { this.canUse = canUse; }
    public Boolean getCanTrain() { return canTrain; }
    public void setCanTrain(Boolean canTrain) { this.canTrain = canTrain; }
    public Boolean getCanPublish() { return canPublish; }
    public void setCanPublish(Boolean canPublish) { this.canPublish = canPublish; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public LocalDateTime getGrantTime() { return grantTime; }
    public void setGrantTime(LocalDateTime grantTime) { this.grantTime = grantTime; }
    public String getGrantUser() { return grantUser; }
    public void setGrantUser(String grantUser) { this.grantUser = grantUser; }
}