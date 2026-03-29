package com.example.lecturesystem.modules.skill.mapper;

import com.example.lecturesystem.modules.skill.entity.SkillKbBindingEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SkillKbBindingMapper {
    SkillKbBindingEntity findBySkillVersionId(@Param("skillVersionId") Long skillVersionId);
    int deleteBySkillVersionId(@Param("skillVersionId") Long skillVersionId);
    int insert(SkillKbBindingEntity entity);
}