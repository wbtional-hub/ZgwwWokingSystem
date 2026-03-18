package com.example.lecturesystem.modules.operationlog.service.impl;

import com.example.lecturesystem.modules.auth.security.LoginUser;
import com.example.lecturesystem.modules.operationlog.dto.OperationLogQueryRequest;
import com.example.lecturesystem.modules.operationlog.entity.OperationLogEntity;
import com.example.lecturesystem.modules.operationlog.mapper.OperationLogMapper;
import com.example.lecturesystem.modules.operationlog.service.OperationLogService;
import com.example.lecturesystem.modules.permission.support.CurrentUserFacade;
import com.example.lecturesystem.modules.permission.support.DataScopeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class OperationLogServiceImpl implements OperationLogService {
    private final OperationLogMapper operationLogMapper;
    private final CurrentUserFacade currentUserFacade;
    private final DataScopeService dataScopeService;

    public OperationLogServiceImpl(OperationLogMapper operationLogMapper) {
        this(operationLogMapper, null, null);
    }

    public OperationLogServiceImpl(OperationLogMapper operationLogMapper,
                                   DataScopeService dataScopeService) {
        this(operationLogMapper, dataScopeService, null);
    }

    @Autowired
    public OperationLogServiceImpl(OperationLogMapper operationLogMapper,
                                   DataScopeService dataScopeService,
                                   CurrentUserFacade currentUserFacade) {
        this.operationLogMapper = operationLogMapper;
        this.currentUserFacade = currentUserFacade;
        this.dataScopeService = dataScopeService;
    }

    @Override
    public void log(String moduleName, String actionName, Long bizId, String content) {
        OperationLogEntity entity = new OperationLogEntity();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof LoginUser loginUser) {
            entity.setOperatorId(loginUser.getUserId());
            entity.setOperatorName(resolveOperatorName(loginUser));
            entity.setUnitId(resolveOperatorUnitId());
        } else {
            entity.setOperatorName("system");
        }
        entity.setModuleName(moduleName);
        entity.setActionName(actionName);
        entity.setBizId(bizId);
        entity.setContent(content == null || content.isBlank() ? "执行操作" : content.trim());
        entity.setCreateTime(LocalDateTime.now());
        operationLogMapper.insert(entity);
    }

    @Override
    public Object query(OperationLogQueryRequest request) {
        OperationLogQueryRequest normalizedRequest = request == null ? new OperationLogQueryRequest() : request;
        if (currentUserFacade != null && dataScopeService != null) {
            dataScopeService.injectTreePathScope(normalizedRequest, currentUserFacade.currentUserEntity());
        }
        normalizedRequest.setModuleName(normalizeText(normalizedRequest.getModuleName()));
        normalizedRequest.setOperatorName(normalizeText(normalizedRequest.getOperatorName()));
        LocalDateTime startTime = parseTime(normalizedRequest.getStartTime(), false);
        LocalDateTime endTime = parseTime(normalizedRequest.getEndTime(), true);
        return operationLogMapper.queryList(normalizedRequest, startTime, endTime);
    }

    private String resolveOperatorName(LoginUser loginUser) {
        String realName = normalizeText(loginUser.getRealName());
        return realName != null ? realName : loginUser.getUsername();
    }

    private String normalizeText(String text) {
        if (text == null) {
            return null;
        }
        String trimmed = text.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private LocalDateTime parseTime(String text, boolean endOfDay) {
        String normalized = normalizeText(text);
        if (normalized == null) {
            return null;
        }
        if (normalized.length() == 10) {
            return LocalDateTime.parse(normalized + (endOfDay ? "T23:59:59" : "T00:00:00"));
        }
        return LocalDateTime.parse(normalized.replace(" ", "T"));
    }

    private Long resolveOperatorUnitId() {
        if (currentUserFacade == null) {
            return null;
        }
        try {
            return currentUserFacade.currentDataScope().getUnitId();
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
