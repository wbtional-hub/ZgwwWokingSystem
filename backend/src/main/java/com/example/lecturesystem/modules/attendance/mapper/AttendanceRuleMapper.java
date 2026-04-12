package com.example.lecturesystem.modules.attendance.mapper;

import com.example.lecturesystem.modules.attendance.entity.AttendanceRuleEntity;
import com.example.lecturesystem.modules.attendance.vo.AttendanceRuleVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AttendanceRuleMapper {
    AttendanceRuleEntity findByUnitId(@Param("unitId") Long unitId);

    AttendanceRuleVO detailByUnitId(@Param("unitId") Long unitId);

    List<AttendanceRuleEntity> queryByUnitIds(@Param("unitIds") List<Long> unitIds);

    int insert(AttendanceRuleEntity entity);

    int update(AttendanceRuleEntity entity);
}
