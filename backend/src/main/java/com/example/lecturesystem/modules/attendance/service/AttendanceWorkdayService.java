package com.example.lecturesystem.modules.attendance.service;

import com.example.lecturesystem.modules.attendance.vo.WorkdayInfoVO;

import java.time.LocalDate;

public interface AttendanceWorkdayService {
    WorkdayInfoVO resolveWorkdayInfo(LocalDate date);

    boolean isWorkday(LocalDate date);
}
