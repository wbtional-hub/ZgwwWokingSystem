package com.example.lecturesystem.modules.knowledge.mapper;

import com.example.lecturesystem.modules.knowledge.dto.KnowledgeSearchRequest;
import com.example.lecturesystem.modules.knowledge.entity.KnowledgeChunkEntity;
import com.example.lecturesystem.modules.knowledge.vo.KnowledgeChunkListItemVO;
import com.example.lecturesystem.modules.knowledge.vo.KnowledgeSearchResultVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface KnowledgeChunkMapper {
    List<KnowledgeChunkListItemVO> queryByDocumentId(@Param("documentId") Long documentId);
    int batchInsert(@Param("list") List<KnowledgeChunkEntity> list);
    List<KnowledgeSearchResultVO> search(@Param("request") KnowledgeSearchRequest request);
}
