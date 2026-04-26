package com.example.lecturesystem.modules.attendance.mapper;

import com.example.lecturesystem.modules.attendance.entity.AttendanceReminderLogEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;

@Mapper
public interface AttendanceReminderLogMapper {
    int insert(AttendanceReminderLogEntity entity);

    int updateSendResult(AttendanceReminderLogEntity entity);

    int countByUserIdAndDateAndType(@Param("userId") Long userId,
                                    @Param("attendanceDate") LocalDate attendanceDate,
                                    @Param("reminderType") String reminderType);
}
