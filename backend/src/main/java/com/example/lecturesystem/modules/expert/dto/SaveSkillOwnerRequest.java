package com.example.lecturesystem.modules.expert.dto;

import jakarta.validation.constraints.NotNull;

public class SaveSkillOwnerRequest {
    @NotNull(message = "userId is required")
    private Long userId;
    @NotNull(message = "skillId is required")
    private Long skillId;
    @NotNull(message = "skillVersionId is required")
    private Long skillVersionId;
    @NotNull(message = "status is required")
    private Integer status;
    private String expertLevel;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getSkillId() { return skillId; }
    public void setSkillId(Long skillId) { this.skillId = skillId; }
    public Long getSkillVersionId() { return skillVersionId; }
    public void setSkillVersionId(Long skillVersionId) { this.skillVersionId = skillVersionId; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getExpertLevel() { return expertLevel; }
    public void setExpertLevel(String expertLevel) { this.expertLevel = expertLevel; }
}