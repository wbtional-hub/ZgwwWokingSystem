package com.example.lecturesystem.modules.skill.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class SkillVersionDetailVO {
    private Long id;
    private Long skillId;
    private String versionNo;
    private Long providerConfigId;
    private String modelCode;
    private String systemPrompt;
    private String taskPrompt;
    private String outputTemplate;
    private String forbiddenRules;
    private String citationRules;
    private String validationStatus;
    private String publishStatus;
    private BigDecimal score;
    private LocalDateTime createTime;
    private Long baseId;
    private String baseName;

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
    public String getValidationStatus() { return validationStatus; }
    public void setValidationStatus(String validationStatus) { this.validationStatus = validationStatus; }
    public String getPublishStatus() { return publishStatus; }
    public void setPublishStatus(String publishStatus) { this.publishStatus = publishStatus; }
    public BigDecimal getScore() { return score; }
    public void setScore(BigDecimal score) { this.score = score; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public Long getBaseId() { return baseId; }
    public void setBaseId(Long baseId) { this.baseId = baseId; }
    public String getBaseName() { return baseName; }
    public void setBaseName(String baseName) { this.baseName = baseName; }
}