package com.example.lecturesystem.modules.agent.mapper;

import com.example.lecturesystem.modules.agent.entity.AiPolicyEvalCaseEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AiPolicyEvalCaseMapper {
    int insert(AiPolicyEvalCaseEntity entity);
    int batchInsert(@Param("list") List<AiPolicyEvalCaseEntity> list);
    int deleteByBaseId(@Param("baseId") Long baseId);
    Integer countByBaseId(@Param("baseId") Long baseId);
    List<AiPolicyEvalCaseEntity> queryEnabledByBaseId(@Param("baseId") Long baseId);
}
