package com.example.lecturesystem.modules.agent.entity;

import java.time.OffsetDateTime;

public class AiPolicyRetrievalLogEntity {
    private Long id;
    private Long baseId;
    private Long sessionId;
    private Long userId;
    private String sourceScene;
    private String rawQuestion;
    private String normalizedQuestion;
    private String regionScope;
    private String policyKey;
    private String questionType;
    private String routePlan;
    private Long matchedIntentId;
    private String matchedIntentCode;
    private String suggestedIntents;
    private Long selectedIntentId;
    private Boolean learningSignalWritten;
    private Boolean candidateGenerated;
    private String searchQuery;
    private String hitChunkIds;
    private String retrievalSummary;
    private OffsetDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getBaseId() { return baseId; }
    public void setBaseId(Long baseId) { this.baseId = baseId; }
    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getSourceScene() { return sourceScene; }
    public void setSourceScene(String sourceScene) { this.sourceScene = sourceScene; }
    public String getRawQuestion() { return rawQuestion; }
    public void setRawQuestion(String rawQuestion) { this.rawQuestion = rawQuestion; }
    public String getNormalizedQuestion() { return normalizedQuestion; }
    public void setNormalizedQuestion(String normalizedQuestion) { this.normalizedQuestion = normalizedQuestion; }
    public String getRegionScope() { return regionScope; }
    public void setRegionScope(String regionScope) { this.regionScope = regionScope; }
    public String getPolicyKey() { return policyKey; }
    public void setPolicyKey(String policyKey) { this.policyKey = policyKey; }
    public String getQuestionType() { return questionType; }
    public void setQuestionType(String questionType) { this.questionType = questionType; }
    public String getRoutePlan() { return routePlan; }
    public void setRoutePlan(String routePlan) { this.routePlan = routePlan; }
    public Long getMatchedIntentId() { return matchedIntentId; }
    public void setMatchedIntentId(Long matchedIntentId) { this.matchedIntentId = matchedIntentId; }
    public String getMatchedIntentCode() { return matchedIntentCode; }
    public void setMatchedIntentCode(String matchedIntentCode) { this.matchedIntentCode = matchedIntentCode; }
    public String getSuggestedIntents() { return suggestedIntents; }
    public void setSuggestedIntents(String suggestedIntents) { this.suggestedIntents = suggestedIntents; }
    public Long getSelectedIntentId() { return selectedIntentId; }
    public void setSelectedIntentId(Long selectedIntentId) { this.selectedIntentId = selectedIntentId; }
    public Boolean getLearningSignalWritten() { return learningSignalWritten; }
    public void setLearningSignalWritten(Boolean learningSignalWritten) { this.learningSignalWritten = learningSignalWritten; }
    public Boolean getCandidateGenerated() { return candidateGenerated; }
    public void setCandidateGenerated(Boolean candidateGenerated) { this.candidateGenerated = candidateGenerated; }
    public String getSearchQuery() { return searchQuery; }
    public void setSearchQuery(String searchQuery) { this.searchQuery = searchQuery; }
    public String getHitChunkIds() { return hitChunkIds; }
    public void setHitChunkIds(String hitChunkIds) { this.hitChunkIds = hitChunkIds; }
    public String getRetrievalSummary() { return retrievalSummary; }
    public void setRetrievalSummary(String retrievalSummary) { this.retrievalSummary = retrievalSummary; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
