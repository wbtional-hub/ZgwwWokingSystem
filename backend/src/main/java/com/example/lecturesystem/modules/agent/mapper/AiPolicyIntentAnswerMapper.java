package com.example.lecturesystem.modules.agent.mapper;

import com.example.lecturesystem.modules.agent.entity.AiPolicyIntentAnswerEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AiPolicyIntentAnswerMapper {
    int insert(AiPolicyIntentAnswerEntity entity);
    int deleteByBaseId(@Param("baseId") Long baseId);
    Integer countByBaseId(@Param("baseId") Long baseId);
    List<AiPolicyIntentAnswerEntity> queryEnabledByBaseId(@Param("baseId") Long baseId);
    List<AiPolicyIntentAnswerEntity> queryByIntentId(@Param("intentId") Long intentId);
}
