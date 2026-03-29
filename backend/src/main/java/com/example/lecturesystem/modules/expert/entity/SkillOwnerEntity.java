package com.example.lecturesystem.modules.expert.entity;

import java.time.LocalDateTime;

public class SkillOwnerEntity {
    private Long id;
    private Long skillId;
    private Long skillVersionId;
    private Long userId;
    private String expertLevel;
    private Integer status;
    private LocalDateTime grantTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSkillId() { return skillId; }
    public void setSkillId(Long skillId) { this.skillId = skillId; }
    public Long getSkillVersionId() { return skillVersionId; }
    public void setSkillVersionId(Long skillVersionId) { this.skillVersionId = skillVersionId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getExpertLevel() { return expertLevel; }
    public void setExpertLevel(String expertLevel) { this.expertLevel = expertLevel; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public LocalDateTime getGrantTime() { return grantTime; }
    public void setGrantTime(LocalDateTime grantTime) { this.grantTime = grantTime; }
}