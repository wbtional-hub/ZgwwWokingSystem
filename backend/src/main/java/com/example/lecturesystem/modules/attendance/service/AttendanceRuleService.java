package com.example.lecturesystem.modules.attendance.service;

import com.example.lecturesystem.modules.attendance.dto.SaveAttendanceRuleRequest;

public interface AttendanceRuleService {
    Object queryCurrentRule();
    Long saveCurrentRule(SaveAttendanceRuleRequest request);
}
