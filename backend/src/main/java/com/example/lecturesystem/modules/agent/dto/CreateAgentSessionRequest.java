package com.example.lecturesystem.modules.agent.dto;

import jakarta.validation.constraints.NotNull;

public class CreateAgentSessionRequest {
    @NotNull(message = "技能ID不能为空")
    private Long skillId;

    private Long baseId;

    public Long getSkillId() { return skillId; }
    public void setSkillId(Long skillId) { this.skillId = skillId; }
    public Long getBaseId() { return baseId; }
    public void setBaseId(Long baseId) { this.baseId = baseId; }
}