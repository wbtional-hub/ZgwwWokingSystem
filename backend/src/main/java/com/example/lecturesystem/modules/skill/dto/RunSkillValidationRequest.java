package com.example.lecturesystem.modules.skill.dto;

import jakarta.validation.constraints.NotNull;

public class RunSkillValidationRequest {
    @NotNull(message = "技能版本ID不能为空")
    private Long skillVersionId;

    public Long getSkillVersionId() { return skillVersionId; }
    public void setSkillVersionId(Long skillVersionId) { this.skillVersionId = skillVersionId; }
}