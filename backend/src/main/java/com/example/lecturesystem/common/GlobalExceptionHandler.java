package com.example.lecturesystem.common;

import com.example.lecturesystem.modules.operationlog.service.OperationLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final OperationLogService operationLogService;

    public GlobalExceptionHandler(OperationLogService operationLogService) {
        this.operationLogService = operationLogService;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<String> handleValidation(MethodArgumentNotValidException ex) {
        return ApiResponse.fail(ex.getBindingResult().getFieldError() != null
                ? ex.getBindingResult().getFieldError().getDefaultMessage()
                : "参数校验失败");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResponse<String> handleIllegalArgument(IllegalArgumentException ex) {
        return ApiResponse.fail(resolveFriendlyMessage(ex, "请求参数有误"));
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<String> handleException(Exception ex) {
        logPermissionDenied(ex);
        log.error("Unhandled exception", ex);
        return ApiResponse.fail(resolveFriendlyMessage(ex, "操作失败，请稍后重试"));
    }

    private String resolveFriendlyMessage(Exception ex, String fallback) {
        String message = ex.getMessage();
        if (message == null) {
            return fallback;
        }
        String trimmed = message.trim();
        return trimmed.isEmpty() ? fallback : trimmed;
    }

    private void logPermissionDenied(Exception ex) {
        String message = ex.getMessage();
        if (message == null) {
            return;
        }
        if (!message.contains("仅管理员可执行该操作") && !message.contains("无权")) {
            return;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            operationLogService.log("AUTH", "PERMISSION_DENIED", null, message);
            return;
        }
        operationLogService.log("AUTH", "PERMISSION_DENIED", null, message);
    }
}
