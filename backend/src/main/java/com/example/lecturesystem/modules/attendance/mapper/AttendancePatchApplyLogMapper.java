package com.example.lecturesystem.modules.attendance.mapper;

import com.example.lecturesystem.modules.attendance.entity.AttendancePatchApplyLogEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AttendancePatchApplyLogMapper {
    int insert(AttendancePatchApplyLogEntity entity);

    List<AttendancePatchApplyLogEntity> queryByApplyId(@Param("applyId") Long applyId);

    List<AttendancePatchApplyLogEntity> queryLatestByApplyIds(@Param("applyIds") List<Long> applyIds);
}
