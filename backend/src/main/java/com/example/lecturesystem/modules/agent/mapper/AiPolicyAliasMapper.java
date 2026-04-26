package com.example.lecturesystem.modules.agent.mapper;

import com.example.lecturesystem.modules.agent.entity.AiPolicyAliasEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AiPolicyAliasMapper {
    int insert(AiPolicyAliasEntity entity);
    int batchInsert(@Param("list") List<AiPolicyAliasEntity> list);
    int deleteByBaseId(@Param("baseId") Long baseId);
    Integer countByBaseId(@Param("baseId") Long baseId);
    List<AiPolicyAliasEntity> queryEnabledByBaseId(@Param("baseId") Long baseId);
}
