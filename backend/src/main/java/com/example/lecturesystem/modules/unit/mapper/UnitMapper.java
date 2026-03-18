package com.example.lecturesystem.modules.unit.mapper;

import com.example.lecturesystem.modules.unit.entity.AttendanceLocationEntity;
import com.example.lecturesystem.modules.unit.entity.UnitEntity;
import com.example.lecturesystem.modules.unit.vo.AttendanceLocationVO;
import com.example.lecturesystem.modules.unit.vo.UnitListItemVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Set;

@Mapper
public interface UnitMapper {
    UnitEntity findById(@Param("id") Long id);

    UnitEntity findByUnitCode(@Param("unitCode") String unitCode);

    int insertUnit(UnitEntity entity);

    int insertLegacyUnit(UnitEntity entity);

    List<UnitListItemVO> queryUnitList();

    List<UnitListItemVO> queryUnitListByIds(@Param("unitIds") Set<Long> unitIds);

    int updateUnit(UnitEntity entity);

    int updateLegacyUnit(UnitEntity entity);

    int deleteUnit(@Param("id") Long id);

    int deleteLegacyUnit(@Param("id") Long id);

    int countOrgNodesByUnitId(@Param("id") Long id);

    int countUsersByUnitId(@Param("id") Long id);

    AttendanceLocationVO findAttendanceLocationByUnitId(@Param("unitId") Long unitId);

    int insertAttendanceLocation(AttendanceLocationEntity entity);

    int updateAttendanceLocation(AttendanceLocationEntity entity);
}
