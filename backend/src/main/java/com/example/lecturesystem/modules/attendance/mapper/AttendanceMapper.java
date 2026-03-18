package com.example.lecturesystem.modules.attendance.mapper;

import com.example.lecturesystem.modules.attendance.dto.AttendanceQueryRequest;
import com.example.lecturesystem.modules.attendance.entity.AttendanceRecordEntity;
import com.example.lecturesystem.modules.attendance.vo.AttendanceAbnormalUserRankVO;
import com.example.lecturesystem.modules.attendance.vo.AttendanceAbnormalUserSummaryVO;
import com.example.lecturesystem.modules.attendance.vo.AttendanceAbnormalTrendPointVO;
import com.example.lecturesystem.modules.attendance.vo.AttendanceRecordListItemVO;
import com.example.lecturesystem.modules.attendance.vo.AttendanceStatusCountVO;
import com.example.lecturesystem.modules.unit.vo.AttendanceLocationVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface AttendanceMapper {
    int insert(AttendanceRecordEntity entity);

    AttendanceRecordEntity findById(@Param("id") Long id);

    AttendanceRecordEntity findByUserIdAndDate(@Param("userId") Long userId,
                                               @Param("attendanceDate") LocalDate attendanceDate);

    AttendanceLocationVO findAttendanceLocationByUnitId(@Param("unitId") Long unitId);

    String findUnitNameById(@Param("unitId") Long unitId);

    int update(AttendanceRecordEntity entity);

    int updateValidFlag(@Param("id") Long id, @Param("validFlag") Integer validFlag);

    int deleteById(@Param("id") Long id);

    long countDistinctUsersByRange(@Param("treePathPrefix") String treePathPrefix,
                                   @Param("startDate") LocalDate startDate,
                                   @Param("endDate") LocalDate endDate,
                                   @Param("validFlag") Integer validFlag);

    long countByQuery(@Param("request") AttendanceQueryRequest request);

    List<AttendanceStatusCountVO> queryStatusCounts(@Param("request") AttendanceQueryRequest request);

    List<AttendanceAbnormalUserRankVO> queryAbnormalUserRanks(@Param("request") AttendanceQueryRequest request);

    List<AttendanceAbnormalTrendPointVO> queryAbnormalTrend(@Param("request") AttendanceQueryRequest request);

    AttendanceAbnormalUserSummaryVO queryAbnormalUserSummary(@Param("request") AttendanceQueryRequest request);

    List<AttendanceRecordListItemVO> queryList(@Param("request") AttendanceQueryRequest request);
}
