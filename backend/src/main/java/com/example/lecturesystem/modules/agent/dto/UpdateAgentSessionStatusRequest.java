package com.example.lecturesystem.modules.agent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class UpdateAgentSessionStatusRequest {
    @NotNull(message = "sessionId is required")
    private Long sessionId;

    @NotBlank(message = "status is required")
    private String status;

    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}