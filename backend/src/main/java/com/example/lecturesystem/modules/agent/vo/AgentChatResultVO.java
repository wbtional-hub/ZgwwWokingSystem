package com.example.lecturesystem.modules.agent.vo;

import java.util.List;

public class AgentChatResultVO {
    private Long sessionId;
    private String answer;
    private String citedChunkIds;
    private List<String> citedChunkIdList;
    private List<String> citedTitles;

    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }
    public String getCitedChunkIds() { return citedChunkIds; }
    public void setCitedChunkIds(String citedChunkIds) { this.citedChunkIds = citedChunkIds; }
    public List<String> getCitedChunkIdList() { return citedChunkIdList; }
    public void setCitedChunkIdList(List<String> citedChunkIdList) { this.citedChunkIdList = citedChunkIdList; }
    public List<String> getCitedTitles() { return citedTitles; }
    public void setCitedTitles(List<String> citedTitles) { this.citedTitles = citedTitles; }
}