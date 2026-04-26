package com.example.lecturesystem.modules.agent.entity;

import java.time.OffsetDateTime;

public class AiPolicyEvalCaseEntity {
    private Long id;
    private Long baseId;
    private String regionScope;
    private String question;
    private String expectedPolicyKey;
    private String expectedQuestionType;
    private String expectedAnswerContains;
    private String expectedForbiddenKeywords;
    private Boolean enabled;
    private OffsetDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getBaseId() { return baseId; }
    public void setBaseId(Long baseId) { this.baseId = baseId; }
    public String getRegionScope() { return regionScope; }
    public void setRegionScope(String regionScope) { this.regionScope = regionScope; }
    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
    public String getExpectedPolicyKey() { return expectedPolicyKey; }
    public void setExpectedPolicyKey(String expectedPolicyKey) { this.expectedPolicyKey = expectedPolicyKey; }
    public String getExpectedQuestionType() { return expectedQuestionType; }
    public void setExpectedQuestionType(String expectedQuestionType) { this.expectedQuestionType = expectedQuestionType; }
    public String getExpectedAnswerContains() { return expectedAnswerContains; }
    public void setExpectedAnswerContains(String expectedAnswerContains) { this.expectedAnswerContains = expectedAnswerContains; }
    public String getExpectedForbiddenKeywords() { return expectedForbiddenKeywords; }
    public void setExpectedForbiddenKeywords(String expectedForbiddenKeywords) { this.expectedForbiddenKeywords = expectedForbiddenKeywords; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
