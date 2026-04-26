package com.example.lecturesystem.modules.agent.mapper;

import com.example.lecturesystem.modules.agent.entity.AiPolicyFaqEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AiPolicyFaqMapper {
    int insert(AiPolicyFaqEntity entity);
    int batchInsert(@Param("list") List<AiPolicyFaqEntity> list);
    int deleteByBaseId(@Param("baseId") Long baseId);
    Integer countByBaseId(@Param("baseId") Long baseId);
    List<AiPolicyFaqEntity> queryEnabledByBaseId(@Param("baseId") Long baseId);
}
