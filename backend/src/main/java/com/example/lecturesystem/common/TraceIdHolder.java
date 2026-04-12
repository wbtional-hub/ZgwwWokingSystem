package com.example.lecturesystem.common;

import org.slf4j.MDC;

import java.util.UUID;

public final class TraceIdHolder {
    public static final String TRACE_ID_KEY = "traceId";
    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    private TraceIdHolder() {
    }

    public static String getTraceId() {
        return MDC.get(TRACE_ID_KEY);
    }

    public static void setTraceId(String traceId) {
        if (traceId == null || traceId.isBlank()) {
            MDC.remove(TRACE_ID_KEY);
            return;
        }
        MDC.put(TRACE_ID_KEY, traceId.trim());
    }

    public static String getOrCreateTraceId() {
        String traceId = getTraceId();
        if (traceId != null && !traceId.isBlank()) {
            return traceId;
        }
        String created = createTraceId();
        setTraceId(created);
        return created;
    }

    public static String createTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static void clear() {
        MDC.remove(TRACE_ID_KEY);
    }
}
