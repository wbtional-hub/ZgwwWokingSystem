package com.example.lecturesystem.modules.agent.mapper;

import com.example.lecturesystem.modules.agent.entity.AiPolicyAnswerLogEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AiPolicyAnswerLogMapper {
    int insert(AiPolicyAnswerLogEntity entity);
    int deleteByBaseId(@Param("baseId") Long baseId);
}
