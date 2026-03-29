package com.example.lecturesystem.modules.expert.dto;

public class ExpertQueryRequest {
    private Long userId;
    private Long skillId;
    private Integer status;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getSkillId() { return skillId; }
    public void setSkillId(Long skillId) { this.skillId = skillId; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}