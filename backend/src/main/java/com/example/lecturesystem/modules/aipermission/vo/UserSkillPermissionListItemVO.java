package com.example.lecturesystem.modules.aipermission.vo;

import java.time.LocalDateTime;

public class UserSkillPermissionListItemVO {
    private Long id;
    private Long userId;
    private String username;
    private String realName;
    private Long skillId;
    private String skillName;
    private Boolean canView;
    private Boolean canUse;
    private Boolean canTrain;
    private Boolean canPublish;
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
    public Long getSkillId() { return skillId; }
    public void setSkillId(Long skillId) { this.skillId = skillId; }
    public String getSkillName() { return skillName; }
    public void setSkillName(String skillName) { this.skillName = skillName; }
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
}