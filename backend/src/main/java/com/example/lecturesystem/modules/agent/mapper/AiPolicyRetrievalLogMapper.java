package com.example.lecturesystem.modules.agent.mapper;

import com.example.lecturesystem.modules.agent.entity.AiPolicyRetrievalLogEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AiPolicyRetrievalLogMapper {
    int insert(AiPolicyRetrievalLogEntity entity);
    int deleteByBaseId(@Param("baseId") Long baseId);
}
