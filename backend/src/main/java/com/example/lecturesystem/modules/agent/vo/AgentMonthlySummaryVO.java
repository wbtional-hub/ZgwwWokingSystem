package com.example.lecturesystem.modules.agent.vo;

public class AgentMonthlySummaryVO {
    private Integer totalSessionCount;
    private Integer totalMessageCount;
    private Integer assistantMessageCount;
    private Integer citedMessageCount;

    public Integer getTotalSessionCount() { return totalSessionCount; }
    public void setTotalSessionCount(Integer totalSessionCount) { this.totalSessionCount = totalSessionCount; }
    public Integer getTotalMessageCount() { return totalMessageCount; }
    public void setTotalMessageCount(Integer totalMessageCount) { this.totalMessageCount = totalMessageCount; }
    public Integer getAssistantMessageCount() { return assistantMessageCount; }
    public void setAssistantMessageCount(Integer assistantMessageCount) { this.assistantMessageCount = assistantMessageCount; }
    public Integer getCitedMessageCount() { return citedMessageCount; }
    public void setCitedMessageCount(Integer citedMessageCount) { this.citedMessageCount = citedMessageCount; }
}