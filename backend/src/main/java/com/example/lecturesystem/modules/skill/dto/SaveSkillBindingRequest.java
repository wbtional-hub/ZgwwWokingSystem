package com.example.lecturesystem.modules.skill.dto;

import jakarta.validation.constraints.NotNull;

public class SaveSkillBindingRequest {
    @NotNull(message = "技能ID不能为空")
    private Long skillId;

    @NotNull(message = "技能版本ID不能为空")
    private Long skillVersionId;

    @NotNull(message = "知识库ID不能为空")
    private Long baseId;

    private Long categoryId;

    public Long getSkillId() { return skillId; }
    public void setSkillId(Long skillId) { this.skillId = skillId; }
    public Long getSkillVersionId() { return skillVersionId; }
    public void setSkillVersionId(Long skillVersionId) { this.skillVersionId = skillVersionId; }
    public Long getBaseId() { return baseId; }
    public void setBaseId(Long baseId) { this.baseId = baseId; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
}