package com.example.lecturesystem.modules.operationlog.controller;

import com.example.lecturesystem.common.ApiResponse;
import com.example.lecturesystem.modules.operationlog.dto.OperationLogQueryRequest;
import com.example.lecturesystem.modules.operationlog.service.OperationLogService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/operation-log")
public class OperationLogController {
    private final OperationLogService operationLogService;

    public OperationLogController(OperationLogService operationLogService) {
        this.operationLogService = operationLogService;
    }

    @PostMapping("/query")
    public ApiResponse<?> query(@RequestBody(required = false) OperationLogQueryRequest request) {
        return ApiResponse.success(operationLogService.query(request == null ? new OperationLogQueryRequest() : request));
    }
}
