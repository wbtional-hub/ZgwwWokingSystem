package com.example.lecturesystem.modules.attendance.service.impl;

import com.example.lecturesystem.modules.attendance.entity.AttendanceHolidayCalendarEntity;
import com.example.lecturesystem.modules.attendance.mapper.AttendanceHolidayCalendarMapper;
import com.example.lecturesystem.modules.attendance.service.AttendanceWorkdayService;
import com.example.lecturesystem.modules.attendance.vo.WorkdayInfoVO;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;

@Service
public class AttendanceWorkdayServiceImpl implements AttendanceWorkdayService {
    public static final String DAY_TYPE_HOLIDAY = "HOLIDAY";
    public static final String DAY_TYPE_WEEKDAY_REST = "WEEKDAY_REST";
    public static final String DAY_TYPE_MAKEUP_WORKDAY = "MAKEUP_WORKDAY";
    public static final String DAY_TYPE_WEEKDAY = "WEEKDAY";
    public static final String DAY_TYPE_WEEKEND = "WEEKEND";

    private final AttendanceHolidayCalendarMapper holidayCalendarMapper;

    public AttendanceWorkdayServiceImpl(AttendanceHolidayCalendarMapper holidayCalendarMapper) {
        this.holidayCalendarMapper = holidayCalendarMapper;
    }

    @Override
    public WorkdayInfoVO resolveWorkdayInfo(LocalDate date) {
        LocalDate targetDate = date == null ? LocalDate.now() : date;
        AttendanceHolidayCalendarEntity configured = holidayCalendarMapper == null
                ? null
                : holidayCalendarMapper.selectEnabledByDate(targetDate);
        if (configured != null) {
            String dayType = normalizeDayType(configured.getDayType());
            if (DAY_TYPE_HOLIDAY.equals(dayType)) {
                return WorkdayInfoVO.holiday(dayType, configured.getName());
            }
            if (DAY_TYPE_WEEKDAY_REST.equals(dayType)) {
                return WorkdayInfoVO.weekdayRest(configured.getName());
            }
            if (DAY_TYPE_MAKEUP_WORKDAY.equals(dayType)) {
                return WorkdayInfoVO.adjustedWorkday(dayType, configured.getName());
            }
        }
        return defaultByWeek(targetDate);
    }

    @Override
    public boolean isWorkday(LocalDate date) {
        return Boolean.TRUE.equals(resolveWorkdayInfo(date).getWorkday());
    }

    private WorkdayInfoVO defaultByWeek(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        if (DayOfWeek.SATURDAY.equals(dayOfWeek) || DayOfWeek.SUNDAY.equals(dayOfWeek)) {
            return WorkdayInfoVO.weekend();
        }
        return WorkdayInfoVO.weekday();
    }

    private String normalizeDayType(String dayType) {
        return dayType == null ? "" : dayType.trim().toUpperCase();
    }
}
