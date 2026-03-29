package com.example.lecturesystem.modules.agent.vo;

import java.time.LocalDateTime;
import java.util.List;

public class AgentMessageVO {
    private Long id;
    private String messageRole;
    private String messageText;
    private String citedChunkIds;
    private List<String> citedChunkIdList;
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getMessageRole() { return messageRole; }
    public void setMessageRole(String messageRole) { this.messageRole = messageRole; }
    public String getMessageText() { return messageText; }
    public void setMessageText(String messageText) { this.messageText = messageText; }
    public String getCitedChunkIds() { return citedChunkIds; }
    public void setCitedChunkIds(String citedChunkIds) { this.citedChunkIds = citedChunkIds; }
    public List<String> getCitedChunkIdList() { return citedChunkIdList; }
    public void setCitedChunkIdList(List<String> citedChunkIdList) { this.citedChunkIdList = citedChunkIdList; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}