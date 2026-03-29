package com.example.lecturesystem.modules.skill.mapper;

import com.example.lecturesystem.modules.skill.entity.SkillTestCaseEntity;
import com.example.lecturesystem.modules.skill.vo.SkillTestCaseListItemVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SkillTestCaseMapper {
    List<SkillTestCaseListItemVO> queryBySkillVersionId(@Param("skillVersionId") Long skillVersionId);
    List<SkillTestCaseEntity> queryActiveEntitiesBySkillVersionId(@Param("skillVersionId") Long skillVersionId);
    SkillTestCaseEntity findById(@Param("id") Long id);
    int insert(SkillTestCaseEntity entity);
    int update(SkillTestCaseEntity entity);
}