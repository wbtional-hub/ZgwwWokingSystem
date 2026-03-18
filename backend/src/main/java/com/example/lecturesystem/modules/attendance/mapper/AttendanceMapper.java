package com.example.lecturesystem.modules.attendance.mapper;

import com.example.lecturesystem.modules.attendance.dto.AttendanceQueryRequest;
import com.example.lecturesystem.modules.attendance.entity.AttendanceRecordEntity;
import com.example.lecturesystem.modules.attendance.vo.AttendanceRecordListItemVO;
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

    int update(AttendanceRecordEntity entity);

    int updateValidFlag(@Param("id") Long id, @Param("validFlag") Integer validFlag);

    int deleteById(@Param("id") Long id);

    long countDistinctUsersByRange(@Param("unitId") Long unitId,
                                   @Param("startDate") LocalDate startDate,
                                   @Param("endDate") LocalDate endDate,
                                   @Param("validFlag") Integer validFlag);

    List<AttendanceRecordListItemVO> queryList(@Param("request") AttendanceQueryRequest request);
}
