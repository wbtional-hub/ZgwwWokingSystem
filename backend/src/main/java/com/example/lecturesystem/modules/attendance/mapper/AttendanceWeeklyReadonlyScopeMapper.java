package com.example.lecturesystem.modules.attendance.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AttendanceWeeklyReadonlyScopeMapper {
    String findUnitNameById(@Param("unitId") Long unitId);

    List<Long> queryCrossDeptReadonlyUserIds(@Param("currentUserId") Long currentUserId,
                                             @Param("currentUnitId") Long currentUnitId,
                                             @Param("treePathPrefix") String treePathPrefix,
                                             @Param("currentLevelNo") Integer currentLevelNo);
}
