package com.example.lecturesystem.config;

import com.example.lecturesystem.common.TraceIdHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestTraceFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        HttpServletRequest requestToUse = request instanceof ContentCachingRequestWrapper
                ? request
                : new ContentCachingRequestWrapper(request);
        String traceId = resolveTraceId(requestToUse);
        TraceIdHolder.setTraceId(traceId);
        response.setHeader(TraceIdHolder.TRACE_ID_HEADER, traceId);
        try {
            filterChain.doFilter(requestToUse, response);
        } finally {
            TraceIdHolder.clear();
        }
    }

    private String resolveTraceId(HttpServletRequest request) {
        String fromHeader = request.getHeader(TraceIdHolder.TRACE_ID_HEADER);
        if (fromHeader != null && !fromHeader.isBlank()) {
            return fromHeader.trim();
        }
        String fromParameter = request.getParameter("traceId");
        if (fromParameter != null && !fromParameter.isBlank()) {
            return fromParameter.trim();
        }
        return TraceIdHolder.createTraceId();
    }
}
