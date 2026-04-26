package com.example.lecturesystem.modules.agent.mapper;

import com.example.lecturesystem.modules.agent.entity.AiPolicyIntentEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AiPolicyIntentMapper {
    int insert(AiPolicyIntentEntity entity);
    int deleteByBaseId(@Param("baseId") Long baseId);
    Integer countByBaseId(@Param("baseId") Long baseId);
    AiPolicyIntentEntity findById(@Param("id") Long id);
    AiPolicyIntentEntity findByCode(@Param("baseId") Long baseId, @Param("intentCode") String intentCode);
    List<AiPolicyIntentEntity> queryEnabledByBaseId(@Param("baseId") Long baseId);
}
