package com.example.lecturesystem.modules.agent.vo;

public class AgentRankItemVO {
    private String label;
    private Integer sessionCount;
    private Integer messageCount;

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public Integer getSessionCount() { return sessionCount; }
    public void setSessionCount(Integer sessionCount) { this.sessionCount = sessionCount; }
    public Integer getMessageCount() { return messageCount; }
    public void setMessageCount(Integer messageCount) { this.messageCount = messageCount; }
}