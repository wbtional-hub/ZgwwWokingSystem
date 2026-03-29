package com.example.lecturesystem.modules.agent.controller;

import com.example.lecturesystem.common.ApiResponse;
import com.example.lecturesystem.modules.agent.dto.AgentChatRequest;
import com.example.lecturesystem.modules.agent.dto.AgentSessionQueryRequest;
import com.example.lecturesystem.modules.agent.dto.CreateAgentSessionRequest;
import com.example.lecturesystem.modules.agent.dto.UpdateAgentSessionStatusRequest;
import com.example.lecturesystem.modules.agent.service.AgentService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/agent")
public class AgentController {
    private final AgentService agentService;

    public AgentController(AgentService agentService) {
        this.agentService = agentService;
    }

    @PostMapping("/session/create")
    public ApiResponse<?> createSession(@Validated @RequestBody CreateAgentSessionRequest request) {
        return ApiResponse.success(agentService.createSession(request));
    }

    @PostMapping("/session/list")
    public ApiResponse<?> listSessions(@RequestBody(required = false) AgentSessionQueryRequest request) {
        return ApiResponse.success(agentService.querySessions(request == null ? new AgentSessionQueryRequest() : request));
    }

    @PostMapping("/session/stats")
    public ApiResponse<?> sessionStats(@RequestBody(required = false) AgentSessionQueryRequest request) {
        return ApiResponse.success(agentService.querySessionStats(request == null ? new AgentSessionQueryRequest() : request));
    }

    @PostMapping("/session/trend")
    public ApiResponse<?> sessionTrend(@RequestBody(required = false) AgentSessionQueryRequest request) {
        return ApiResponse.success(agentService.querySessionTrend(request == null ? new AgentSessionQueryRequest() : request));
    }

    @PostMapping("/session/monthly-report")
    public ApiResponse<?> monthlyReport(@RequestBody(required = false) AgentSessionQueryRequest request) {
        return ApiResponse.success(agentService.queryMonthlyReport(request == null ? new AgentSessionQueryRequest() : request));
    }

    @GetMapping("/session/monthly-report/export/excel")
    public ResponseEntity<byte[]> exportMonthlyReportExcel(AgentSessionQueryRequest request) {
        AgentSessionQueryRequest safeRequest = request == null ? new AgentSessionQueryRequest() : request;
        byte[] bytes = agentService.exportMonthlyReportExcel(safeRequest);
        int year = safeRequest.getYear() == null ? LocalDate.now().getYear() : safeRequest.getYear();
        String fileName = URLEncoder.encode("ai-consultation-monthly-report-" + year + ".xlsx", StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + fileName)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    @GetMapping("/session/export")
    public ResponseEntity<byte[]> exportSessions(AgentSessionQueryRequest request) {
        byte[] bytes = agentService.exportSessions(request == null ? new AgentSessionQueryRequest() : request);
        String fileName = URLEncoder.encode("ai-consultation-ledger-" + LocalDate.now() + ".csv", StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + fileName)
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(bytes);
    }

    @GetMapping("/session/export/excel")
    public ResponseEntity<byte[]> exportSessionsExcel(AgentSessionQueryRequest request) {
        byte[] bytes = agentService.exportSessionsExcel(request == null ? new AgentSessionQueryRequest() : request);
        String fileName = URLEncoder.encode("ai-consultation-ledger-" + LocalDate.now() + ".xlsx", StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + fileName)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    @PostMapping("/session/status")
    public ApiResponse<?> updateSessionStatus(@Validated @RequestBody UpdateAgentSessionStatusRequest request) {
        agentService.updateSessionStatus(request);
        return ApiResponse.success("ok");
    }

    @PostMapping("/chat")
    public ApiResponse<?> chat(@Validated @RequestBody AgentChatRequest request) {
        return ApiResponse.success(agentService.chat(request));
    }

    @GetMapping("/session/{sessionId}/messages")
    public ApiResponse<?> messages(@PathVariable Long sessionId) {
        return ApiResponse.success(agentService.queryMessages(sessionId));
    }
}