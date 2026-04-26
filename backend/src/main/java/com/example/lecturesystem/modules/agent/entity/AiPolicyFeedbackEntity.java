package com.example.lecturesystem.modules.agent.entity;

import java.time.OffsetDateTime;

public class AiPolicyFeedbackEntity {
    private Long id;
    private Long baseId;
    private Long userId;
    private Long sessionId;
    private String rawQuestion;
    private String normalizedQuestion;
    private Long intentId;
    private String feedbackType;
    private String feedbackText;
    private String finalAnswer;
    private OffsetDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getBaseId() { return baseId; }
    public void setBaseId(Long baseId) { this.baseId = baseId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
    public String getRawQuestion() { return rawQuestion; }
    public void setRawQuestion(String rawQuestion) { this.rawQuestion = rawQuestion; }
    public String getNormalizedQuestion() { return normalizedQuestion; }
    public void setNormalizedQuestion(String normalizedQuestion) { this.normalizedQuestion = normalizedQuestion; }
    public Long getIntentId() { return intentId; }
    public void setIntentId(Long intentId) { this.intentId = intentId; }
    public String getFeedbackType() { return feedbackType; }
    public void setFeedbackType(String feedbackType) { this.feedbackType = feedbackType; }
    public String getFeedbackText() { return feedbackText; }
    public void setFeedbackText(String feedbackText) { this.feedbackText = feedbackText; }
    public String getFinalAnswer() { return finalAnswer; }
    public void setFinalAnswer(String finalAnswer) { this.finalAnswer = finalAnswer; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
