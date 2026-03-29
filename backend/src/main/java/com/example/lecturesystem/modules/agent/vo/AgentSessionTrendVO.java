package com.example.lecturesystem.modules.agent.vo;

import java.util.List;

public class AgentSessionTrendVO {
    private List<AgentTrendPointVO> dailySessions;
    private List<AgentRankItemVO> skillRanking;
    private List<AgentRankItemVO> userRanking;

    public List<AgentTrendPointVO> getDailySessions() { return dailySessions; }
    public void setDailySessions(List<AgentTrendPointVO> dailySessions) { this.dailySessions = dailySessions; }
    public List<AgentRankItemVO> getSkillRanking() { return skillRanking; }
    public void setSkillRanking(List<AgentRankItemVO> skillRanking) { this.skillRanking = skillRanking; }
    public List<AgentRankItemVO> getUserRanking() { return userRanking; }
    public void setUserRanking(List<AgentRankItemVO> userRanking) { this.userRanking = userRanking; }
}