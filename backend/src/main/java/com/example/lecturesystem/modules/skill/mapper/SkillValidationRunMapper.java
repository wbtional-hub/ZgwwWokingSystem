package com.example.lecturesystem.modules.skill.mapper;

import com.example.lecturesystem.modules.skill.entity.SkillValidationRunEntity;
import com.example.lecturesystem.modules.skill.vo.SkillValidationDetailVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SkillValidationRunMapper {
    int insert(SkillValidationRunEntity entity);
    int update(SkillValidationRunEntity entity);
    SkillValidationDetailVO queryDetail(@Param("runId") Long runId);
}