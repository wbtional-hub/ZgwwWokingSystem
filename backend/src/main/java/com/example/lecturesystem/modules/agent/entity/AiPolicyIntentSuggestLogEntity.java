package com.example.lecturesystem.modules.agent.entity;

import java.time.OffsetDateTime;

public class AiPolicyIntentSuggestLogEntity {
    private Long id;
    private Long baseId;
    private Long userId;
    private String rawInput;
    private String normalizedInput;
    private String suggestedItems;
    private Long selectedIntentId;
    private OffsetDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getBaseId() { return baseId; }
    public void setBaseId(Long baseId) { this.baseId = baseId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getRawInput() { return rawInput; }
    public void setRawInput(String rawInput) { this.rawInput = rawInput; }
    public String getNormalizedInput() { return normalizedInput; }
    public void setNormalizedInput(String normalizedInput) { this.normalizedInput = normalizedInput; }
    public String getSuggestedItems() { return suggestedItems; }
    public void setSuggestedItems(String suggestedItems) { this.suggestedItems = suggestedItems; }
    public Long getSelectedIntentId() { return selectedIntentId; }
    public void setSelectedIntentId(Long selectedIntentId) { this.selectedIntentId = selectedIntentId; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
