package com.example.lecturesystem.modules.attendance.service;

import com.example.lecturesystem.modules.attendance.dto.AttendanceQueryRequest;
import com.example.lecturesystem.modules.attendance.dto.CheckInRequest;
import com.example.lecturesystem.modules.attendance.dto.SaveAttendanceRequest;

public interface AttendanceService {
    Object checkIn(CheckInRequest request);
    Object query(AttendanceQueryRequest request);
    Long saveAttendance(SaveAttendanceRequest request);
    void deleteAttendance(Long id);
}
