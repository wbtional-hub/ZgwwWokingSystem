package com.example.lecturesystem.modules.attendance.service;

import com.example.lecturesystem.modules.attendance.dto.AttendanceQueryRequest;
import com.example.lecturesystem.modules.attendance.dto.CheckInRequest;
import com.example.lecturesystem.modules.attendance.dto.SaveAttendanceRequest;

public interface AttendanceService {
    Object queryCurrentAttendanceLocation();
    Object checkIn(CheckInRequest request);
    Object query(AttendanceQueryRequest request);
    Object querySummary(AttendanceQueryRequest request);
    Object queryAbnormalMonitor(AttendanceQueryRequest request);
    Object queryAbnormalTrend(AttendanceQueryRequest request);
    Object queryAbnormalUserSummary(AttendanceQueryRequest request);
    Long saveAttendance(SaveAttendanceRequest request);
    void deleteAttendance(Long id);
}
