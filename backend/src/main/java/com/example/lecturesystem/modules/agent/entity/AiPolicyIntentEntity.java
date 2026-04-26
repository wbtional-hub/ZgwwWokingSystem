package com.example.lecturesystem.modules.agent.entity;

import java.time.OffsetDateTime;

public class AiPolicyIntentEntity {
    private Long id;
    private Long baseId;
    private String regionScope;
    private String policyKey;
    private String intentCode;
    private String intentName;
    private String standardQuestion;
    private String questionType;
    private String topicType;
    private String answerLevelDefault;
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
    public String getPolicyKey() { return policyKey; }
    public void setPolicyKey(String policyKey) { this.policyKey = policyKey; }
    public String getIntentCode() { return intentCode; }
    public void setIntentCode(String intentCode) { this.intentCode = intentCode; }
    public String getIntentName() { return intentName; }
    public void setIntentName(String intentName) { this.intentName = intentName; }
    public String getStandardQuestion() { return standardQuestion; }
    public void setStandardQuestion(String standardQuestion) { this.standardQuestion = standardQuestion; }
    public String getQuestionType() { return questionType; }
    public void setQuestionType(String questionType) { this.questionType = questionType; }
    public String getTopicType() { return topicType; }
    public void setTopicType(String topicType) { this.topicType = topicType; }
    public String getAnswerLevelDefault() { return answerLevelDefault; }
    public void setAnswerLevelDefault(String answerLevelDefault) { this.answerLevelDefault = answerLevelDefault; }
    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
