package com.example.lecturesystem.modules.agent.mapper;

import com.example.lecturesystem.modules.agent.entity.AiPolicyIntentSuggestLogEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AiPolicyIntentSuggestLogMapper {
    int insert(AiPolicyIntentSuggestLogEntity entity);
    int deleteByBaseId(@Param("baseId") Long baseId);
    int updateSelectedIntent(@Param("id") Long id, @Param("selectedIntentId") Long selectedIntentId);
    Integer countByBaseId(@Param("baseId") Long baseId);
    Integer countSelectedByBaseId(@Param("baseId") Long baseId);
}
