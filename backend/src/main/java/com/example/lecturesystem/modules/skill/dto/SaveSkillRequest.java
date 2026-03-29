package com.example.lecturesystem.modules.skill.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class SaveSkillRequest {
    private Long id;

    @NotBlank(message = "技能编码不能为空")
    private String skillCode;

    @NotBlank(message = "技能名称不能为空")
    private String skillName;

    private String domainType;
    private String skillType;
    private String description;

    @NotNull(message = "状态不能为空")
    private Integer status;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSkillCode() { return skillCode; }
    public void setSkillCode(String skillCode) { this.skillCode = skillCode; }
    public String getSkillName() { return skillName; }
    public void setSkillName(String skillName) { this.skillName = skillName; }
    public String getDomainType() { return domainType; }
    public void setDomainType(String domainType) { this.domainType = domainType; }
    public String getSkillType() { return skillType; }
    public void setSkillType(String skillType) { this.skillType = skillType; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}