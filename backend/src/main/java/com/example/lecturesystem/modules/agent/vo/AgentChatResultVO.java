package com.example.lecturesystem.modules.agent.vo;

import java.util.List;

public class AgentChatResultVO {
    private Long sessionId;
    private Long skillId;
    private String skillName;
    private String sourceScene;
    private String skillMatchMode;
    private String answer;
    private String citedChunkIds;
    private List<String> citedChunkIdList;
    private List<String> citedTitles;

    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
    public Long getSkillId() { return skillId; }
    public void setSkillId(Long skillId) { this.skillId = skillId; }
    public String getSkillName() { return skillName; }
    public void setSkillName(String skillName) { this.skillName = skillName; }
    public String getSourceScene() { return sourceScene; }
    public void setSourceScene(String sourceScene) { this.sourceScene = sourceScene; }
    public String getSkillMatchMode() { return skillMatchMode; }
    public void setSkillMatchMode(String skillMatchMode) { this.skillMatchMode = skillMatchMode; }
    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }
    public String getCitedChunkIds() { return citedChunkIds; }
    public void setCitedChunkIds(String citedChunkIds) { this.citedChunkIds = citedChunkIds; }
    public List<String> getCitedChunkIdList() { return citedChunkIdList; }
    public void setCitedChunkIdList(List<String> citedChunkIdList) { this.citedChunkIdList = citedChunkIdList; }
    public List<String> getCitedTitles() { return citedTitles; }
    public void setCitedTitles(List<String> citedTitles) { this.citedTitles = citedTitles; }
}
