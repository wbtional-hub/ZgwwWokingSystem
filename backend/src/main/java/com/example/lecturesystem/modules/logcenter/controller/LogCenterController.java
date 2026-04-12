package com.example.lecturesystem.modules.logcenter.controller;

import com.example.lecturesystem.common.ApiResponse;
import com.example.lecturesystem.modules.logcenter.dto.LogCenterQueryRequest;
import com.example.lecturesystem.modules.logcenter.dto.LogCenterReportRequest;
import com.example.lecturesystem.modules.logcenter.service.LogCenterService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/log-center")
public class LogCenterController {
    private final LogCenterService logCenterService;

    public LogCenterController(LogCenterService logCenterService) {
        this.logCenterService = logCenterService;
    }

    @PostMapping("/report")
    public ApiResponse<?> report(@RequestBody(required = false) LogCenterReportRequest request,
                                 HttpServletRequest servletRequest) {
        logCenterService.report(request == null ? new LogCenterReportRequest() : request, servletRequest);
        return ApiResponse.success("ok");
    }

    @PostMapping("/query")
    public ApiResponse<?> query(@RequestBody(required = false) LogCenterQueryRequest request) {
        return ApiResponse.success(logCenterService.query(request == null ? new LogCenterQueryRequest() : request));
    }

    @GetMapping("/{id}")
    public ApiResponse<?> detail(@PathVariable Long id) {
        return ApiResponse.success(logCenterService.detail(id));
    }
}
