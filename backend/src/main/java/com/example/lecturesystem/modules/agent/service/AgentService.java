package com.example.lecturesystem.modules.agent.service;

import com.example.lecturesystem.modules.agent.dto.AgentChatRequest;
import com.example.lecturesystem.modules.agent.dto.AgentSessionQueryRequest;
import com.example.lecturesystem.modules.agent.dto.CreateAgentSessionRequest;
import com.example.lecturesystem.modules.agent.dto.UpdateAgentSessionStatusRequest;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface AgentService {
    Object createSession(CreateAgentSessionRequest request);
    Object chat(AgentChatRequest request);
    SseEmitter chatStream(AgentChatRequest request);
    Object querySessions(AgentSessionQueryRequest request);
    Object querySessionStats(AgentSessionQueryRequest request);
    Object querySessionTrend(AgentSessionQueryRequest request);
    Object queryMonthlyReport(AgentSessionQueryRequest request);
    byte[] exportMonthlyReportExcel(AgentSessionQueryRequest request);
    byte[] exportSessions(AgentSessionQueryRequest request);
    byte[] exportSessionsExcel(AgentSessionQueryRequest request);
    void updateSessionStatus(UpdateAgentSessionStatusRequest request);
    Object queryMessages(Long sessionId);
}
