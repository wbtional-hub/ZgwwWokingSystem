package com.example.lecturesystem.modules.attendance.service;

import com.example.lecturesystem.modules.attendance.dto.SaveAttendanceHolidayCalendarRequest;
import com.example.lecturesystem.modules.attendance.vo.AttendanceHolidayCalendarVO;

import java.util.List;

public interface AttendanceHolidayCalendarService {
    List<AttendanceHolidayCalendarVO> queryByYear(Integer year);

    List<AttendanceHolidayCalendarVO> generateAiCandidates(Integer year);

    Long save(SaveAttendanceHolidayCalendarRequest request);

    void delete(Long id);

    List<AttendanceHolidayCalendarVO> confirmYear(Integer year);
}
