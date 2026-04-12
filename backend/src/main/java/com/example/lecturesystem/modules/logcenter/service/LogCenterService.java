package com.example.lecturesystem.modules.logcenter.service;

import com.example.lecturesystem.modules.logcenter.dto.LogCenterQueryRequest;
import com.example.lecturesystem.modules.logcenter.dto.LogCenterReportRequest;
import jakarta.servlet.http.HttpServletRequest;

public interface LogCenterService {
    void report(LogCenterReportRequest request, HttpServletRequest servletRequest);

    void recordBackendException(HttpServletRequest request,
                                Exception exception,
                                String friendlyMessage,
                                Integer responseStatus,
                                boolean unhandled);

    Object query(LogCenterQueryRequest request);

    Object detail(Long id);
}
