package com.example.lecturesystem.modules.skill.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class SaveSkillVersionRequest {
    private Long id;

    @NotNull(message = "技能ID不能为空")
    private Long skillId;

    @NotBlank(message = "版本号不能为空")
    private String versionNo;

    private Long providerConfigId;
    private String modelCode;

    @NotBlank(message = "系统提示词不能为空")
    private String systemPrompt;

    private String taskPrompt;
    private String outputTemplate;
    private String forbiddenRules;
    private String citationRules;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSkillId() { return skillId; }
    public void setSkillId(Long skillId) { this.skillId = skillId; }
    public String getVersionNo() { return versionNo; }
    public void setVersionNo(String versionNo) { this.versionNo = versionNo; }
    public Long getProviderConfigId() { return providerConfigId; }
    public void setProviderConfigId(Long providerConfigId) { this.providerConfigId = providerConfigId; }
    public String getModelCode() { return modelCode; }
    public void setModelCode(String modelCode) { this.modelCode = modelCode; }
    public String getSystemPrompt() { return systemPrompt; }
    public void setSystemPrompt(String systemPrompt) { this.systemPrompt = systemPrompt; }
    public String getTaskPrompt() { return taskPrompt; }
    public void setTaskPrompt(String taskPrompt) { this.taskPrompt = taskPrompt; }
    public String getOutputTemplate() { return outputTemplate; }
    public void setOutputTemplate(String outputTemplate) { this.outputTemplate = outputTemplate; }
    public String getForbiddenRules() { return forbiddenRules; }
    public void setForbiddenRules(String forbiddenRules) { this.forbiddenRules = forbiddenRules; }
    public String getCitationRules() { return citationRules; }
    public void setCitationRules(String citationRules) { this.citationRules = citationRules; }
}