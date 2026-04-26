package com.example.lecturesystem.modules.agent.entity;

import java.time.OffsetDateTime;

public class AiPolicyIntentAnswerEntity {
    private Long id;
    private Long baseId;
    private Long intentId;
    private String answerMode;
    private String answerTitle;
    private String answerTemplate;
    private String evidenceRule;
    private String followupSuggestion;
    private Integer priority;
    private Boolean enabled;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getBaseId() { return baseId; }
    public void setBaseId(Long baseId) { this.baseId = baseId; }
    public Long getIntentId() { return intentId; }
    public void setIntentId(Long intentId) { this.intentId = intentId; }
    public String getAnswerMode() { return answerMode; }
    public void setAnswerMode(String answerMode) { this.answerMode = answerMode; }
    public String getAnswerTitle() { return answerTitle; }
    public void setAnswerTitle(String answerTitle) { this.answerTitle = answerTitle; }
    public String getAnswerTemplate() { return answerTemplate; }
    public void setAnswerTemplate(String answerTemplate) { this.answerTemplate = answerTemplate; }
    public String getEvidenceRule() { return evidenceRule; }
    public void setEvidenceRule(String evidenceRule) { this.evidenceRule = evidenceRule; }
    public String getFollowupSuggestion() { return followupSuggestion; }
    public void setFollowupSuggestion(String followupSuggestion) { this.followupSuggestion = followupSuggestion; }
    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
