package com.example.lecturesystem.modules.skill.mapper;

import com.example.lecturesystem.modules.skill.entity.SkillValidationResultEntity;
import com.example.lecturesystem.modules.skill.vo.SkillValidationResultVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SkillValidationResultMapper {
    int batchInsert(@Param("list") List<SkillValidationResultEntity> list);
    List<SkillValidationResultVO> queryByRunId(@Param("runId") Long runId);
}