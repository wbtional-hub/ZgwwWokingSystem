package com.example.lecturesystem.modules.operationlog.service;

import com.example.lecturesystem.modules.operationlog.dto.OperationLogQueryRequest;

public interface OperationLogService {
    void log(String moduleName, String actionName, Long bizId, String content);

    Object query(OperationLogQueryRequest request);
}
