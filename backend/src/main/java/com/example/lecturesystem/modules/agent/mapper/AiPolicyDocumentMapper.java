package com.example.lecturesystem.modules.agent.mapper;

import com.example.lecturesystem.modules.agent.entity.AiPolicyDocumentEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AiPolicyDocumentMapper {
    int insert(AiPolicyDocumentEntity entity);
    int deleteByBaseId(@Param("baseId") Long baseId);
    Integer countByBaseId(@Param("baseId") Long baseId);
    List<AiPolicyDocumentEntity> queryByBaseId(@Param("baseId") Long baseId);
}
