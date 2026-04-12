package com.example.lecturesystem.modules.logcenter.service.impl;

import com.example.lecturesystem.common.TraceIdHolder;
import com.example.lecturesystem.modules.auth.security.LoginUser;
import com.example.lecturesystem.modules.logcenter.dto.LogCenterQueryRequest;
import com.example.lecturesystem.modules.logcenter.dto.LogCenterReportRequest;
import com.example.lecturesystem.modules.logcenter.entity.LogCenterEntity;
import com.example.lecturesystem.modules.logcenter.mapper.LogCenterMapper;
import com.example.lecturesystem.modules.logcenter.service.LogCenterService;
import com.example.lecturesystem.modules.logcenter.vo.LogCenterDetailVO;
import com.example.lecturesystem.modules.logcenter.vo.LogCenterPageVO;
import com.example.lecturesystem.modules.permission.support.CurrentUserFacade;
import com.example.lecturesystem.modules.unit.entity.UnitEntity;
import com.example.lecturesystem.modules.unit.mapper.UnitMapper;
import com.example.lecturesystem.modules.user.entity.UserEntity;
import com.example.lecturesystem.modules.user.mapper.UserMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class LogCenterServiceImpl implements LogCenterService {
    private static final Logger log = LoggerFactory.getLogger(LogCenterServiceImpl.class);
    private static final int MAX_RATE_LIMIT_PER_MINUTE = 8;
    private static final long RATE_LIMIT_WINDOW_MILLIS = 60_000L;

    private final LogCenterMapper logCenterMapper;
    private final CurrentUserFacade currentUserFacade;
    private final UserMapper userMapper;
    private final UnitMapper unitMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ConcurrentMap<String, RateLimitState> rateLimitStates = new ConcurrentHashMap<>();

    public LogCenterServiceImpl(LogCenterMapper logCenterMapper,
                                CurrentUserFacade currentUserFacade,
                                UserMapper userMapper,
                                UnitMapper unitMapper) {
        this.logCenterMapper = logCenterMapper;
        this.currentUserFacade = currentUserFacade;
        this.userMapper = userMapper;
        this.unitMapper = unitMapper;
    }

    @Override
    public void report(LogCenterReportRequest request, HttpServletRequest servletRequest) {
        LogCenterEntity entity = new LogCenterEntity();
        entity.setTraceId(normalizeTraceId(request.getTraceId()));
        entity.setLogType(defaultIfBlank(normalizeText(request.getLogType()), "FRONTEND_LOG"));
        entity.setModule(defaultIfBlank(normalizeText(request.getModule()), resolveModuleFromUrl(request.getRequestUrl(), request.getPageUrl())));
        entity.setSubModule(defaultIfBlank(normalizeText(request.getSubModule()), resolveSubModuleFromUrl(request.getRequestUrl(), request.getPageUrl())));
        entity.setLevel(defaultIfBlank(normalizeText(request.getLevel()), "ERROR"));
        entity.setTitle(defaultIfBlank(normalizeText(request.getTitle()), "前端异常上报"));
        entity.setSummary(defaultIfBlank(normalizeText(request.getSummary()), defaultIfBlank(normalizeText(request.getMessage()), "前端异常上报")));
        entity.setDiagnosis(defaultIfBlank(normalizeText(request.getDiagnosis()), "前端已上报结构化日志，请结合 traceId、页面地址和原始上下文排查。"));
        entity.setErrorCode(normalizeText(request.getErrorCode()));
        entity.setMessage(normalizeText(request.getMessage()));
        entity.setRawData(serializeJson(request.getRawData()));
        entity.setRequestUrl(defaultIfBlank(normalizeText(request.getRequestUrl()), buildRequestUrl(servletRequest)));
        entity.setRequestMethod(defaultIfBlank(normalizeText(request.getRequestMethod()), resolveMethod(servletRequest)));
        entity.setRequestParams(serializeJson(request.getRequestParams()));
        entity.setResponseStatus(request.getResponseStatus());
        entity.setClientIp(defaultIfBlank(normalizeText(request.getClientIp()), resolveClientIp(servletRequest)));
        entity.setUserAgent(defaultIfBlank(normalizeText(request.getUserAgent()), resolveUserAgent(servletRequest)));
        entity.setDeviceType(defaultIfBlank(normalizeText(request.getDeviceType()), resolveDeviceType(entity.getUserAgent())));
        entity.setPlatform(defaultIfBlank(normalizeText(request.getPlatform()), resolvePlatform(entity.getUserAgent())));
        entity.setPageUrl(defaultIfBlank(normalizeText(request.getPageUrl()), normalizeText(servletRequest == null ? null : servletRequest.getHeader("X-Page-Url"))));
        entity.setReferer(defaultIfBlank(normalizeText(request.getReferer()), normalizeText(servletRequest == null ? null : servletRequest.getHeader("Referer"))));
        entity.setEnv(defaultIfBlank(normalizeText(request.getEnv()), "BROWSER"));
        fillUserContext(entity, request.getUserId(), request.getUserName(), request.getOrgId(), request.getOrgName());
        persist(entity);
    }

    @Override
    public void recordBackendException(HttpServletRequest request,
                                       Exception exception,
                                       String friendlyMessage,
                                       Integer responseStatus,
                                       boolean unhandled) {
        LogCenterEntity entity = new LogCenterEntity();
        entity.setTraceId(TraceIdHolder.getOrCreateTraceId());
        String requestUrl = buildRequestUrl(request);
        String module = resolveModuleFromUrl(requestUrl, null);
        String subModule = resolveSubModuleFromUrl(requestUrl, null);
        String logType = resolveBackendLogType(requestUrl, unhandled);
        entity.setLogType(logType);
        entity.setModule(module);
        entity.setSubModule(subModule);
        entity.setLevel(unhandled ? "ERROR" : "WARN");
        entity.setTitle(resolveBackendTitle(logType, unhandled));
        entity.setSummary(defaultIfBlank(normalizeText(friendlyMessage), defaultIfBlank(normalizeText(exception == null ? null : exception.getMessage()), entity.getTitle())));
        entity.setDiagnosis(buildBackendDiagnosis(logType, requestUrl, unhandled));
        entity.setErrorCode(resolveBackendErrorCode(logType, exception, unhandled));
        entity.setMessage(normalizeText(exception == null ? null : exception.getMessage()));
        entity.setRawData(buildBackendExceptionRawData(request, exception));
        entity.setRequestUrl(requestUrl);
        entity.setRequestMethod(resolveMethod(request));
        entity.setRequestParams(buildRequestParams(request));
        entity.setResponseStatus(responseStatus);
        entity.setClientIp(resolveClientIp(request));
        entity.setUserAgent(resolveUserAgent(request));
        entity.setDeviceType(resolveDeviceType(entity.getUserAgent()));
        entity.setPlatform(resolvePlatform(entity.getUserAgent()));
        entity.setPageUrl(normalizeText(request == null ? null : request.getHeader("X-Page-Url")));
        entity.setReferer(normalizeText(request == null ? null : request.getHeader("Referer")));
        entity.setEnv("SERVER");
        fillUserContext(entity, null, null, null, null);
        persist(entity);
    }

    @Override
    public Object query(LogCenterQueryRequest request) {
        requireAdmin();
        LogCenterQueryRequest normalized = request == null ? new LogCenterQueryRequest() : request;
        normalized.setTraceId(normalizeText(normalized.getTraceId()));
        normalized.setLogType(normalizeText(normalized.getLogType()));
        normalized.setModule(normalizeText(normalized.getModule()));
        normalized.setLevel(normalizeText(normalized.getLevel()));
        normalized.setUserKeyword(normalizeText(normalized.getUserKeyword()));
        normalized.setKeyword(normalizeText(normalized.getKeyword()));
        LocalDateTime startTime = parseTime(normalized.getStartTime(), false);
        LocalDateTime endTime = parseTime(normalized.getEndTime(), true);
        LogCenterPageVO page = new LogCenterPageVO();
        page.setPageNo(normalized.getPageNo());
        page.setPageSize(normalized.getPageSize());
        page.setTotal(logCenterMapper.countByQuery(normalized, startTime, endTime));
        page.setList(logCenterMapper.queryPage(normalized, startTime, endTime));
        return page;
    }

    @Override
    public Object detail(Long id) {
        requireAdmin();
        LogCenterDetailVO detail = logCenterMapper.findDetailById(id);
        if (detail == null) {
            throw new IllegalArgumentException("日志不存在");
        }
        detail.setRawData(prettyJson(detail.getRawData()));
        detail.setRequestParams(prettyJson(detail.getRequestParams()));
        detail.setAiAnalysisText(buildAiAnalysisText(detail));
        return detail;
    }

    private void persist(LogCenterEntity entity) {
        entity.setCreateTime(LocalDateTime.now());
        entity.setTraceId(normalizeTraceId(entity.getTraceId()));
        entity.setLogType(defaultIfBlank(normalizeText(entity.getLogType()), "SYSTEM_LOG"));
        entity.setModule(defaultIfBlank(normalizeText(entity.getModule()), "SYSTEM"));
        entity.setSubModule(normalizeText(entity.getSubModule()));
        entity.setLevel(defaultIfBlank(normalizeText(entity.getLevel()), "ERROR"));
        entity.setTitle(defaultIfBlank(normalizeText(entity.getTitle()), "系统日志"));
        entity.setSummary(defaultIfBlank(normalizeText(entity.getSummary()), entity.getTitle()));
        entity.setDiagnosis(defaultIfBlank(normalizeText(entity.getDiagnosis()), "请结合 traceId 和原始上下文排查。"));
        entity.setErrorCode(normalizeText(entity.getErrorCode()));
        entity.setMessage(normalizeText(entity.getMessage()));
        entity.setRequestUrl(normalizeText(entity.getRequestUrl()));
        entity.setRequestMethod(normalizeText(entity.getRequestMethod()));
        entity.setClientIp(normalizeText(entity.getClientIp()));
        entity.setUserAgent(normalizeText(entity.getUserAgent()));
        entity.setDeviceType(defaultIfBlank(normalizeText(entity.getDeviceType()), resolveDeviceType(entity.getUserAgent())));
        entity.setPlatform(defaultIfBlank(normalizeText(entity.getPlatform()), resolvePlatform(entity.getUserAgent())));
        entity.setPageUrl(normalizeText(entity.getPageUrl()));
        entity.setReferer(normalizeText(entity.getReferer()));
        entity.setEnv(defaultIfBlank(normalizeText(entity.getEnv()), "SERVER"));
        if (!shouldPersist(entity)) {
            return;
        }
        try {
            logCenterMapper.insert(entity);
        } catch (Exception ex) {
            log.warn("Failed to persist log center record, traceId={}, title={}", entity.getTraceId(), entity.getTitle(), ex);
        }
    }

    private void fillUserContext(LogCenterEntity entity,
                                 Long requestUserId,
                                 String requestUserName,
                                 Long requestOrgId,
                                 String requestOrgName) {
        entity.setUserId(requestUserId);
        entity.setUserName(normalizeText(requestUserName));
        entity.setOrgId(requestOrgId);
        entity.setOrgName(normalizeText(requestOrgName));
        if (entity.getUserId() != null && entity.getUserName() != null && entity.getOrgId() != null && entity.getOrgName() != null) {
            return;
        }
        try {
            LoginUser loginUser = currentUserFacade.currentLoginUser();
            entity.setUserId(entity.getUserId() == null ? loginUser.getUserId() : entity.getUserId());
            entity.setUserName(entity.getUserName() == null ? defaultIfBlank(normalizeText(loginUser.getRealName()), normalizeText(loginUser.getUsername())) : entity.getUserName());
            if (entity.getOrgId() == null || entity.getOrgName() == null) {
                UserEntity userEntity = userMapper.findById(loginUser.getUserId());
                if (userEntity != null && userEntity.getUnitId() != null) {
                    entity.setOrgId(entity.getOrgId() == null ? userEntity.getUnitId() : entity.getOrgId());
                    UnitEntity unitEntity = unitMapper.findById(userEntity.getUnitId());
                    if (unitEntity != null) {
                        entity.setOrgName(entity.getOrgName() == null ? normalizeText(unitEntity.getUnitName()) : entity.getOrgName());
                    }
                }
            }
        } catch (Exception ignored) {
            // anonymous report is allowed
        }
    }

    private boolean shouldPersist(LogCenterEntity entity) {
        cleanupRateLimitState();
        String fingerprint = String.join("|",
                defaultIfBlank(entity.getLogType(), ""),
                defaultIfBlank(entity.getModule(), ""),
                defaultIfBlank(entity.getSubModule(), ""),
                defaultIfBlank(entity.getLevel(), ""),
                defaultIfBlank(entity.getErrorCode(), ""),
                defaultIfBlank(entity.getTitle(), ""),
                defaultIfBlank(entity.getSummary(), ""),
                defaultIfBlank(entity.getRequestUrl(), ""),
                entity.getUserId() == null ? "" : String.valueOf(entity.getUserId()),
                defaultIfBlank(entity.getPageUrl(), "")
        );
        long now = System.currentTimeMillis();
        RateLimitState state = rateLimitStates.compute(fingerprint, (key, current) -> {
            if (current == null || now - current.windowStart >= RATE_LIMIT_WINDOW_MILLIS) {
                return new RateLimitState(now, 1);
            }
            current.count += 1;
            return current;
        });
        return state == null || state.count <= MAX_RATE_LIMIT_PER_MINUTE;
    }

    private void cleanupRateLimitState() {
        if (rateLimitStates.size() < 1024) {
            return;
        }
        long expireBefore = System.currentTimeMillis() - RATE_LIMIT_WINDOW_MILLIS * 2;
        rateLimitStates.entrySet().removeIf(entry -> entry.getValue().windowStart < expireBefore);
    }

    private String buildBackendExceptionRawData(HttpServletRequest request, Exception exception) {
        Map<String, Object> rawData = new LinkedHashMap<>();
        rawData.put("exceptionClass", exception == null ? null : exception.getClass().getName());
        rawData.put("friendlyMessage", exception == null ? null : normalizeText(exception.getMessage()));
        rawData.put("requestBody", parseJsonOrKeep(buildRequestParams(request)));
        rawData.put("queryString", normalizeText(request == null ? null : request.getQueryString()));
        rawData.put("traceId", TraceIdHolder.getOrCreateTraceId());
        rawData.put("stackTrace", buildStackTrace(exception));
        return serializeJson(rawData);
    }

    private String buildRequestParams(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        if (request instanceof ContentCachingRequestWrapper wrapper) {
            byte[] body = wrapper.getContentAsByteArray();
            if (body.length > 0) {
                return new String(body, StandardCharsets.UTF_8);
            }
        }
        Map<String, String[]> parameterMap = request.getParameterMap();
        if (parameterMap == null || parameterMap.isEmpty()) {
            return null;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        parameterMap.forEach((key, value) -> {
            if (value == null) {
                result.put(key, null);
            } else if (value.length == 1) {
                result.put(key, value[0]);
            } else {
                result.put(key, Arrays.asList(value));
            }
        });
        return serializeJson(result);
    }

    private Object parseJsonOrKeep(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(text, Object.class);
        } catch (Exception ex) {
            return text;
        }
    }

    private String buildAiAnalysisText(LogCenterDetailVO detail) {
        StringBuilder builder = new StringBuilder();
        appendAiLine(builder, "【问题标题】", detail.getTitle());
        appendAiLine(builder, "【发生时间】", detail.getCreateTime() == null ? "-" : detail.getCreateTime().toString().replace("T", " "));
        appendAiLine(builder, "【模块】", joinWithSlash(detail.getModule(), detail.getSubModule()));
        appendAiLine(builder, "【用户】", joinWithSlash(
                detail.getUserName(),
                detail.getUserId() == null ? null : String.valueOf(detail.getUserId()),
                detail.getOrgName()
        ));
        appendAiLine(builder, "【页面】", defaultIfBlank(detail.getPageUrl(), detail.getRequestUrl()));
        appendAiLine(builder, "【错误码】", defaultIfBlank(detail.getErrorCode(), detail.getLogType()));
        appendAiLine(builder, "【摘要】", detail.getSummary());
        appendAiLine(builder, "【诊断结论】", detail.getDiagnosis());
        appendAiLine(builder, "【请求信息】", buildRequestInfo(detail));
        appendAiLine(builder, "【原始日志JSON】", buildRawLogJson(detail));
        return builder.toString().trim();
    }

    private void requireAdmin() {
        UserEntity currentUser = currentUserFacade.currentUserEntity();
        if (!"ADMIN".equalsIgnoreCase(defaultIfBlank(currentUser.getRole(), ""))) {
            throw new IllegalArgumentException("仅管理员可查看日志中台");
        }
    }

    private String buildRequestInfo(LogCenterDetailVO detail) {
        Map<String, Object> requestInfo = new LinkedHashMap<>();
        requestInfo.put("traceId", detail.getTraceId());
        requestInfo.put("requestUrl", detail.getRequestUrl());
        requestInfo.put("requestMethod", detail.getRequestMethod());
        requestInfo.put("responseStatus", detail.getResponseStatus());
        requestInfo.put("requestParams", parseJsonOrKeep(detail.getRequestParams()));
        requestInfo.put("clientIp", detail.getClientIp());
        requestInfo.put("userAgent", detail.getUserAgent());
        requestInfo.put("env", detail.getEnv());
        return prettyJson(serializeJson(requestInfo));
    }

    private String buildRawLogJson(LogCenterDetailVO detail) {
        Map<String, Object> raw = new LinkedHashMap<>();
        raw.put("id", detail.getId());
        raw.put("traceId", detail.getTraceId());
        raw.put("logType", detail.getLogType());
        raw.put("module", detail.getModule());
        raw.put("subModule", detail.getSubModule());
        raw.put("level", detail.getLevel());
        raw.put("title", detail.getTitle());
        raw.put("summary", detail.getSummary());
        raw.put("diagnosis", detail.getDiagnosis());
        raw.put("errorCode", detail.getErrorCode());
        raw.put("message", detail.getMessage());
        raw.put("requestUrl", detail.getRequestUrl());
        raw.put("requestMethod", detail.getRequestMethod());
        raw.put("requestParams", parseJsonOrKeep(detail.getRequestParams()));
        raw.put("responseStatus", detail.getResponseStatus());
        raw.put("userId", detail.getUserId());
        raw.put("userName", detail.getUserName());
        raw.put("orgId", detail.getOrgId());
        raw.put("orgName", detail.getOrgName());
        raw.put("clientIp", detail.getClientIp());
        raw.put("userAgent", detail.getUserAgent());
        raw.put("deviceType", detail.getDeviceType());
        raw.put("platform", detail.getPlatform());
        raw.put("pageUrl", detail.getPageUrl());
        raw.put("referer", detail.getReferer());
        raw.put("env", detail.getEnv());
        raw.put("createTime", detail.getCreateTime());
        raw.put("rawData", parseJsonOrKeep(detail.getRawData()));
        return prettyJson(serializeJson(raw));
    }

    private String prettyJson(String json) {
        if (json == null || json.isBlank()) {
            return "{}";
        }
        try {
            Object value = objectMapper.readValue(json, Object.class);
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (Exception ex) {
            return json;
        }
    }

    private void appendAiLine(StringBuilder builder, String label, String value) {
        builder.append(label).append('\n');
        builder.append(defaultIfBlank(value, "-")).append('\n');
    }

    private String buildBackendDiagnosis(String logType, String requestUrl, boolean unhandled) {
        if ("LOGIN_FAILURE".equals(logType)) {
            return "登录接口返回失败，请结合账号状态、登录日志和 traceId 排查认证、锁定或配置问题。";
        }
        if ("CHECK_IN_FAILURE".equals(logType)) {
            return "打卡接口返回失败，请结合定位日志、围栏配置和 traceId 检查是定位问题还是业务校验失败。";
        }
        if ("APPROVAL_FAILURE".equals(logType)) {
            return "审批流程执行失败，请结合当前审批节点、权限范围和 traceId 排查流转条件。";
        }
        return unhandled
                ? "后端发生未捕获异常，请优先根据 traceId 串联上下游日志，并检查异常堆栈与请求参数。"
                : "后端返回业务失败，请结合 traceId、请求参数和模块规则排查业务校验。";
    }

    private String resolveBackendTitle(String logType, boolean unhandled) {
        return switch (logType) {
            case "LOGIN_FAILURE" -> "登录失败";
            case "CHECK_IN_FAILURE" -> "打卡失败";
            case "APPROVAL_FAILURE" -> "审批失败";
            default -> unhandled ? "后端未捕获异常" : "后端业务异常";
        };
    }

    private String resolveBackendErrorCode(String logType, Exception exception, boolean unhandled) {
        if ("LOGIN_FAILURE".equals(logType)) {
            return "BACKEND_LOGIN_FAILURE";
        }
        if ("CHECK_IN_FAILURE".equals(logType)) {
            return "BACKEND_CHECK_IN_FAILURE";
        }
        if ("APPROVAL_FAILURE".equals(logType)) {
            return "BACKEND_APPROVAL_FAILURE";
        }
        if (unhandled) {
            return "BACKEND_UNCAUGHT_EXCEPTION";
        }
        return exception == null ? "BACKEND_BUSINESS_ERROR" : exception.getClass().getSimpleName();
    }

    private String resolveBackendLogType(String requestUrl, boolean unhandled) {
        String normalized = defaultIfBlank(requestUrl, "");
        if (normalized.contains("/api/auth/login")) {
            return "LOGIN_FAILURE";
        }
        if (normalized.contains("/api/attendance/check-in")) {
            return "CHECK_IN_FAILURE";
        }
        if (normalized.contains("/approve") || normalized.contains("/reject") || normalized.contains("/review")) {
            return "APPROVAL_FAILURE";
        }
        return unhandled ? "BACKEND_UNCAUGHT_EXCEPTION" : "BACKEND_BUSINESS_ERROR";
    }

    private String resolveModuleFromUrl(String requestUrl, String pageUrl) {
        String source = defaultIfBlank(requestUrl, pageUrl);
        if (source == null) {
            return "SYSTEM";
        }
        String normalized = source.toLowerCase(Locale.ROOT);
        if (normalized.contains("attendance")) {
            return "ATTENDANCE";
        }
        if (normalized.contains("weekly-work")) {
            return "WEEKLY_WORK";
        }
        if (normalized.contains("/auth") || normalized.contains("login")) {
            return "AUTH";
        }
        if (normalized.contains("wechat")) {
            return "WECHAT";
        }
        if (normalized.contains("operation-log")) {
            return "OPERATION_LOG";
        }
        if (normalized.contains("log-center")) {
            return "LOG_CENTER";
        }
        if (normalized.contains("approval") || normalized.contains("approve")) {
            return "APPROVAL";
        }
        return "SYSTEM";
    }

    private String resolveSubModuleFromUrl(String requestUrl, String pageUrl) {
        String source = defaultIfBlank(requestUrl, pageUrl);
        if (source == null) {
            return null;
        }
        String normalized = source.toLowerCase(Locale.ROOT);
        if (normalized.contains("check-in")) {
            return "CHECK_IN";
        }
        if (normalized.contains("patch-apply")) {
            return "PATCH_APPLY";
        }
        if (normalized.contains("approve")) {
            return "APPROVE";
        }
        if (normalized.contains("reject")) {
            return "REJECT";
        }
        if (normalized.contains("wechat") && normalized.contains("jsapi-config")) {
            return "JSAPI_CONFIG";
        }
        if (normalized.contains("/auth/login")) {
            return "LOGIN";
        }
        return null;
    }

    private String buildStackTrace(Exception exception) {
        if (exception == null) {
            return null;
        }
        StringWriter writer = new StringWriter();
        exception.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }

    private String serializeJson(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String text) {
            String trimmed = text.trim();
            if (trimmed.isEmpty()) {
                return null;
            }
            try {
                Object parsed = objectMapper.readValue(trimmed, Object.class);
                return objectMapper.writeValueAsString(parsed);
            } catch (Exception ignored) {
                return trimmed;
            }
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            return String.valueOf(value);
        }
    }

    private String buildRequestUrl(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String uri = normalizeText(request.getRequestURI());
        String query = normalizeText(request.getQueryString());
        if (uri == null) {
            return null;
        }
        return query == null ? uri : uri + "?" + query;
    }

    private String resolveMethod(HttpServletRequest request) {
        return normalizeText(request == null ? null : request.getMethod());
    }

    private String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String forwarded = normalizeText(request.getHeader("X-Forwarded-For"));
        if (forwarded != null) {
            int separatorIndex = forwarded.indexOf(',');
            return separatorIndex >= 0 ? forwarded.substring(0, separatorIndex).trim() : forwarded;
        }
        return normalizeText(request.getRemoteAddr());
    }

    private String resolveUserAgent(HttpServletRequest request) {
        return normalizeText(request == null ? null : request.getHeader("User-Agent"));
    }

    private String resolveDeviceType(String userAgent) {
        String normalized = defaultIfBlank(userAgent, "").toLowerCase(Locale.ROOT);
        if (normalized.contains("micromessenger")) {
            return "WECHAT";
        }
        if (normalized.contains("iphone") || normalized.contains("android") || normalized.contains("mobile") || normalized.contains("harmony")) {
            return "MOBILE";
        }
        if (normalized.isBlank()) {
            return "UNKNOWN";
        }
        return "PC";
    }

    private String resolvePlatform(String userAgent) {
        String normalized = defaultIfBlank(userAgent, "").toLowerCase(Locale.ROOT);
        if (normalized.contains("windows")) {
            return "WINDOWS";
        }
        if (normalized.contains("android")) {
            return "ANDROID";
        }
        if (normalized.contains("iphone") || normalized.contains("ipad") || normalized.contains("ios")) {
            return "IOS";
        }
        if (normalized.contains("mac os")) {
            return "MAC";
        }
        if (normalized.contains("linux")) {
            return "LINUX";
        }
        if (normalized.contains("micromessenger")) {
            return "WECHAT";
        }
        return "UNKNOWN";
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

    private String normalizeTraceId(String traceId) {
        String normalized = normalizeText(traceId);
        if (normalized != null) {
            return normalized;
        }
        return TraceIdHolder.getOrCreateTraceId();
    }

    private String normalizeText(String text) {
        if (text == null) {
            return null;
        }
        String trimmed = text.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String joinWithSlash(String... parts) {
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            String normalized = normalizeText(part);
            if (normalized == null) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(" / ");
            }
            builder.append(normalized);
        }
        return builder.toString();
    }

    private static final class RateLimitState {
        private final long windowStart;
        private int count;

        private RateLimitState(long windowStart, int count) {
            this.windowStart = windowStart;
            this.count = count;
        }
    }
}
