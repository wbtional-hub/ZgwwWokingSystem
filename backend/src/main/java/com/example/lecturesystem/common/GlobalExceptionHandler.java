package com.example.lecturesystem.common;

import com.example.lecturesystem.modules.operationlog.service.OperationLogService;
import com.example.lecturesystem.modules.logcenter.service.LogCenterService;
import jakarta.servlet.http.HttpServletRequest;
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
    private final LogCenterService logCenterService;

    public GlobalExceptionHandler(OperationLogService operationLogService,
                                  LogCenterService logCenterService) {
        this.operationLogService = operationLogService;
        this.logCenterService = logCenterService;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<String> handleValidation(MethodArgumentNotValidException ex,
                                                HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldError() != null
                ? ex.getBindingResult().getFieldError().getDefaultMessage()
                : "参数校验失败";
        writeExceptionLog(request, ex, message, 400, false);
        return ApiResponse.fail(message);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResponse<String> handleIllegalArgument(IllegalArgumentException ex,
                                                     HttpServletRequest request) {
        String message = resolveFriendlyMessage(ex, "请求参数有误");
        writeExceptionLog(request, ex, message, 400, false);
        return ApiResponse.fail(message);
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<String> handleException(Exception ex,
                                               HttpServletRequest request) {
        logPermissionDenied(ex);
        log.error("Unhandled exception", ex);
        String message = resolveFriendlyMessage(ex, "操作失败，请稍后重试");
        writeExceptionLog(request, ex, message, 500, true);
        return ApiResponse.fail(message);
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

    private void writeExceptionLog(HttpServletRequest request,
                                   Exception ex,
                                   String message,
                                   Integer responseStatus,
                                   boolean unhandled) {
        try {
            logCenterService.recordBackendException(request, ex, message, responseStatus, unhandled);
        } catch (Exception logEx) {
            log.warn("Failed to write unified log record", logEx);
        }
    }
}
