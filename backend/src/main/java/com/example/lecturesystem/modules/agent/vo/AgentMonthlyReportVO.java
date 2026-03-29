package com.example.lecturesystem.modules.agent.vo;

import java.util.List;

public class AgentMonthlyReportVO {
    private Integer year;
    private Integer totalSessionCount;
    private Integer totalMessageCount;
    private Integer activeMonthCount;
    private Integer averageMonthlySessions;
    private Integer currentMonthSessionCount;
    private Double monthOverMonthRate;
    private Double yearOverYearRate;
    private Integer assistantMessageCount;
    private Integer citedMessageCount;
    private Double citationHitRate;
    private String topSkillLabel;
    private String topBaseLabel;
    private String topExpertLabel;
    private List<AgentTrendPointVO> monthlySessions;
    private List<AgentTrendPointVO> previousYearMonthlySessions;
    private List<AgentRankItemVO> skillRanking;
    private List<AgentRankItemVO> baseRanking;
    private List<AgentRankItemVO> userRanking;
    private List<AgentExpertMetricVO> expertRanking;

    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }
    public Integer getTotalSessionCount() { return totalSessionCount; }
    public void setTotalSessionCount(Integer totalSessionCount) { this.totalSessionCount = totalSessionCount; }
    public Integer getTotalMessageCount() { return totalMessageCount; }
    public void setTotalMessageCount(Integer totalMessageCount) { this.totalMessageCount = totalMessageCount; }
    public Integer getActiveMonthCount() { return activeMonthCount; }
    public void setActiveMonthCount(Integer activeMonthCount) { this.activeMonthCount = activeMonthCount; }
    public Integer getAverageMonthlySessions() { return averageMonthlySessions; }
    public void setAverageMonthlySessions(Integer averageMonthlySessions) { this.averageMonthlySessions = averageMonthlySessions; }
    public Integer getCurrentMonthSessionCount() { return currentMonthSessionCount; }
    public void setCurrentMonthSessionCount(Integer currentMonthSessionCount) { this.currentMonthSessionCount = currentMonthSessionCount; }
    public Double getMonthOverMonthRate() { return monthOverMonthRate; }
    public void setMonthOverMonthRate(Double monthOverMonthRate) { this.monthOverMonthRate = monthOverMonthRate; }
    public Double getYearOverYearRate() { return yearOverYearRate; }
    public void setYearOverYearRate(Double yearOverYearRate) { this.yearOverYearRate = yearOverYearRate; }
    public Integer getAssistantMessageCount() { return assistantMessageCount; }
    public void setAssistantMessageCount(Integer assistantMessageCount) { this.assistantMessageCount = assistantMessageCount; }
    public Integer getCitedMessageCount() { return citedMessageCount; }
    public void setCitedMessageCount(Integer citedMessageCount) { this.citedMessageCount = citedMessageCount; }
    public Double getCitationHitRate() { return citationHitRate; }
    public void setCitationHitRate(Double citationHitRate) { this.citationHitRate = citationHitRate; }
    public String getTopSkillLabel() { return topSkillLabel; }
    public void setTopSkillLabel(String topSkillLabel) { this.topSkillLabel = topSkillLabel; }
    public String getTopBaseLabel() { return topBaseLabel; }
    public void setTopBaseLabel(String topBaseLabel) { this.topBaseLabel = topBaseLabel; }
    public String getTopExpertLabel() { return topExpertLabel; }
    public void setTopExpertLabel(String topExpertLabel) { this.topExpertLabel = topExpertLabel; }
    public List<AgentTrendPointVO> getMonthlySessions() { return monthlySessions; }
    public void setMonthlySessions(List<AgentTrendPointVO> monthlySessions) { this.monthlySessions = monthlySessions; }
    public List<AgentTrendPointVO> getPreviousYearMonthlySessions() { return previousYearMonthlySessions; }
    public void setPreviousYearMonthlySessions(List<AgentTrendPointVO> previousYearMonthlySessions) { this.previousYearMonthlySessions = previousYearMonthlySessions; }
    public List<AgentRankItemVO> getSkillRanking() { return skillRanking; }
    public void setSkillRanking(List<AgentRankItemVO> skillRanking) { this.skillRanking = skillRanking; }
    public List<AgentRankItemVO> getBaseRanking() { return baseRanking; }
    public void setBaseRanking(List<AgentRankItemVO> baseRanking) { this.baseRanking = baseRanking; }
    public List<AgentRankItemVO> getUserRanking() { return userRanking; }
    public void setUserRanking(List<AgentRankItemVO> userRanking) { this.userRanking = userRanking; }
    public List<AgentExpertMetricVO> getExpertRanking() { return expertRanking; }
    public void setExpertRanking(List<AgentExpertMetricVO> expertRanking) { this.expertRanking = expertRanking; }
}