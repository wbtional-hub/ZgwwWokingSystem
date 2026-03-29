package com.example.lecturesystem.modules.agent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class AgentChatRequest {
    @NotNull(message = "会话ID不能为空")
    private Long sessionId;

    @NotBlank(message = "问题不能为空")
    private String question;

    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
}