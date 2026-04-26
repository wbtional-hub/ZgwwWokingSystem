package com.example.lecturesystem.modules.attendance.mapper;

import com.example.lecturesystem.modules.attendance.entity.AttendanceRecordEntity;
import com.example.lecturesystem.modules.attendance.entity.AttendanceReminderCandidateEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface AttendanceReminderQueryMapper {
    List<AttendanceReminderCandidateEntity> queryReminderCandidates();

    List<AttendanceRecordEntity> queryTodayRecordsByUserIds(@Param("attendanceDate") LocalDate attendanceDate,
                                                            @Param("userIds") List<Long> userIds);
}
