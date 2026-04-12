package com.example.lecturesystem.modules.attendance.mapper;

import com.example.lecturesystem.modules.attendance.vo.AttendanceScopedUserVO;
import com.example.lecturesystem.modules.attendance.vo.AttendanceStatsRecordVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface AttendanceStatisticsMapper {
    List<AttendanceScopedUserVO> queryAllScopedUsers();

    List<AttendanceScopedUserVO> queryScopedUsers(@Param("treePathPrefix") String treePathPrefix);

    List<AttendanceStatsRecordVO> queryRecordsByUserIdsAndDateRange(@Param("userIds") List<Long> userIds,
                                                                    @Param("startDate") LocalDate startDate,
                                                                    @Param("endDate") LocalDate endDate);
}
