package com.example.lecturesystem.modules.agent.mapper;

import com.example.lecturesystem.modules.agent.entity.AiPolicyRouteRuleEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AiPolicyRouteRuleMapper {
    int insert(AiPolicyRouteRuleEntity entity);
    int batchInsert(@Param("list") List<AiPolicyRouteRuleEntity> list);
    int deleteByBaseId(@Param("baseId") Long baseId);
    Integer countByBaseId(@Param("baseId") Long baseId);
    List<AiPolicyRouteRuleEntity> queryEnabledByBaseId(@Param("baseId") Long baseId);
}
