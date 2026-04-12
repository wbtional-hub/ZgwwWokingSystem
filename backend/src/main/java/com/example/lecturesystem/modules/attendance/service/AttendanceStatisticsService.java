package com.example.lecturesystem.modules.attendance.service;

import com.example.lecturesystem.modules.attendance.dto.AttendanceStatsQueryRequest;

public interface AttendanceStatisticsService {
    Object queryTeamStatistics(AttendanceStatsQueryRequest request);
}
