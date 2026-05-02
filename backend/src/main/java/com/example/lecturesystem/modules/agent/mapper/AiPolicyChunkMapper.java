package com.example.lecturesystem.modules.agent.mapper;

import com.example.lecturesystem.modules.agent.entity.AiPolicyChunkEntity;
import com.example.lecturesystem.modules.agent.support.policy.AiPolicyChunkSearchQuery;
import com.example.lecturesystem.modules.knowledge.vo.KnowledgeSearchResultVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AiPolicyChunkMapper {
    int insert(AiPolicyChunkEntity entity);
    int batchInsert(@Param("list") List<AiPolicyChunkEntity> list);
    int deleteByBaseId(@Param("baseId") Long baseId);
    Integer countByBaseId(@Param("baseId") Long baseId);
    List<AiPolicyChunkEntity> findByIds(@Param("ids") List<Long> ids);
    List<KnowledgeSearchResultVO> search(@Param("query") AiPolicyChunkSearchQuery query);
}
