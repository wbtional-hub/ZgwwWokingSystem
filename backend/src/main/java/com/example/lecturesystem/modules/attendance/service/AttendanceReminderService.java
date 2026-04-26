package com.example.lecturesystem.modules.attendance.service;

import com.example.lecturesystem.modules.attendance.support.AttendanceReminderType;

import java.util.Map;

public interface AttendanceReminderService {
    Map<String, Object> sendScheduledReminders(AttendanceReminderType reminderType);

    Map<String, Object> runNow(AttendanceReminderType reminderType);

    Map<String, Object> testSend(Long userId, AttendanceReminderType reminderType);

    Map<String, Object> healthCheck();
}
