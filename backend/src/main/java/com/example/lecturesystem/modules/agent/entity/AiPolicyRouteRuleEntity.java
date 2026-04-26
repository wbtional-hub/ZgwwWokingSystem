package com.example.lecturesystem.modules.agent.entity;

import java.time.OffsetDateTime;

public class AiPolicyRouteRuleEntity {
    private Long id;
    private Long baseId;
    private String regionScope;
    private String questionType;
    private String keywordPattern;
    private String targetDocType;
    private String targetTopicType;
    private Integer priority;
    private Boolean enabled;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getBaseId() { return baseId; }
    public void setBaseId(Long baseId) { this.baseId = baseId; }
    public String getRegionScope() { return regionScope; }
    public void setRegionScope(String regionScope) { this.regionScope = regionScope; }
    public String getQuestionType() { return questionType; }
    public void setQuestionType(String questionType) { this.questionType = questionType; }
    public String getKeywordPattern() { return keywordPattern; }
    public void setKeywordPattern(String keywordPattern) { this.keywordPattern = keywordPattern; }
    public String getTargetDocType() { return targetDocType; }
    public void setTargetDocType(String targetDocType) { this.targetDocType = targetDocType; }
    public String getTargetTopicType() { return targetTopicType; }
    public void setTargetTopicType(String targetTopicType) { this.targetTopicType = targetTopicType; }
    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
