package com.example.lecturesystem.modules.agent.mapper;

import com.example.lecturesystem.modules.agent.entity.AiPolicyIntentPhraseEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AiPolicyIntentPhraseMapper {
    int insert(AiPolicyIntentPhraseEntity entity);
    int deleteByBaseId(@Param("baseId") Long baseId);
    Integer countByBaseId(@Param("baseId") Long baseId);
    List<AiPolicyIntentPhraseEntity> queryEnabledByBaseId(@Param("baseId") Long baseId);
    List<AiPolicyIntentPhraseEntity> queryByIntentId(@Param("intentId") Long intentId);
    Integer countExisting(@Param("baseId") Long baseId, @Param("intentId") Long intentId, @Param("phrase") String phrase);
}
