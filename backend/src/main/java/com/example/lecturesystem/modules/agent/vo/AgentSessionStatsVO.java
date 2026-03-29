package com.example.lecturesystem.modules.agent.vo;

public class AgentSessionStatsVO {
    private Integer totalSessionCount;
    private Integer activeSessionCount;
    private Integer todaySessionCount;
    private Integer totalMessageCount;
    private Integer distinctSkillCount;

    public Integer getTotalSessionCount() { return totalSessionCount; }
    public void setTotalSessionCount(Integer totalSessionCount) { this.totalSessionCount = totalSessionCount; }
    public Integer getActiveSessionCount() { return activeSessionCount; }
    public void setActiveSessionCount(Integer activeSessionCount) { this.activeSessionCount = activeSessionCount; }
    public Integer getTodaySessionCount() { return todaySessionCount; }
    public void setTodaySessionCount(Integer todaySessionCount) { this.todaySessionCount = todaySessionCount; }
    public Integer getTotalMessageCount() { return totalMessageCount; }
    public void setTotalMessageCount(Integer totalMessageCount) { this.totalMessageCount = totalMessageCount; }
    public Integer getDistinctSkillCount() { return distinctSkillCount; }
    public void setDistinctSkillCount(Integer distinctSkillCount) { this.distinctSkillCount = distinctSkillCount; }
}