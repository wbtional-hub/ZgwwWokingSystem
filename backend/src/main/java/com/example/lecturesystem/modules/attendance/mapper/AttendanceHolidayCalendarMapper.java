package com.example.lecturesystem.modules.attendance.mapper;

import com.example.lecturesystem.modules.attendance.entity.AttendanceHolidayCalendarEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AttendanceHolidayCalendarMapper {
    AttendanceHolidayCalendarEntity selectEnabledByDate(@Param("calendarDate") LocalDate calendarDate);
    AttendanceHolidayCalendarEntity selectById(@Param("id") Long id);
    AttendanceHolidayCalendarEntity selectByDate(@Param("calendarDate") LocalDate calendarDate);
    List<AttendanceHolidayCalendarEntity> selectByYear(@Param("calendarYear") Integer calendarYear);
    int countConfirmedByYear(@Param("calendarYear") Integer calendarYear);
    int countEnabledDuplicateByYear(@Param("calendarYear") Integer calendarYear);
    int insert(AttendanceHolidayCalendarEntity entity);
    int update(AttendanceHolidayCalendarEntity entity);
    int deleteById(@Param("id") Long id);
    int confirmYear(@Param("calendarYear") Integer calendarYear,
                    @Param("updateTime") LocalDateTime updateTime);
}
