package com.example.lecturesystem.modules.agent.vo;

public class AgentExpertMetricVO {
    private String label;
    private String expertLevel;
    private Integer sessionCount;
    private Integer messageCount;
    private Integer assistantMessageCount;
    private Integer citedMessageCount;
    private Double citationHitRate;

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public String getExpertLevel() { return expertLevel; }
    public void setExpertLevel(String expertLevel) { this.expertLevel = expertLevel; }
    public Integer getSessionCount() { return sessionCount; }
    public void setSessionCount(Integer sessionCount) { this.sessionCount = sessionCount; }
    public Integer getMessageCount() { return messageCount; }
    public void setMessageCount(Integer messageCount) { this.messageCount = messageCount; }
    public Integer getAssistantMessageCount() { return assistantMessageCount; }
    public void setAssistantMessageCount(Integer assistantMessageCount) { this.assistantMessageCount = assistantMessageCount; }
    public Integer getCitedMessageCount() { return citedMessageCount; }
    public void setCitedMessageCount(Integer citedMessageCount) { this.citedMessageCount = citedMessageCount; }
    public Double getCitationHitRate() { return citationHitRate; }
    public void setCitationHitRate(Double citationHitRate) { this.citationHitRate = citationHitRate; }
}