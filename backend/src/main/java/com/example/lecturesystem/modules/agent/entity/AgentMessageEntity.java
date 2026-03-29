package com.example.lecturesystem.modules.agent.entity;

import java.time.LocalDateTime;

public class AgentMessageEntity {
    private Long id;
    private Long sessionId;
    private String messageRole;
    private String messageText;
    private String citedChunkIds;
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
    public String getMessageRole() { return messageRole; }
    public void setMessageRole(String messageRole) { this.messageRole = messageRole; }
    public String getMessageText() { return messageText; }
    public void setMessageText(String messageText) { this.messageText = messageText; }
    public String getCitedChunkIds() { return citedChunkIds; }
    public void setCitedChunkIds(String citedChunkIds) { this.citedChunkIds = citedChunkIds; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}