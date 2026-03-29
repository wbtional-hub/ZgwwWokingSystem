package com.example.lecturesystem.modules.knowledge.mapper;

import com.example.lecturesystem.modules.knowledge.dto.KnowledgeDocumentQueryRequest;
import com.example.lecturesystem.modules.knowledge.entity.KnowledgeDocumentEntity;
import com.example.lecturesystem.modules.knowledge.vo.KnowledgeDocumentDetailVO;
import com.example.lecturesystem.modules.knowledge.vo.KnowledgeDocumentListItemVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface KnowledgeDocumentMapper {
    List<KnowledgeDocumentListItemVO> queryList(@Param("request") KnowledgeDocumentQueryRequest request);
    KnowledgeDocumentEntity findById(@Param("id") Long id);
    KnowledgeDocumentDetailVO queryDetail(@Param("id") Long id);
    int insert(KnowledgeDocumentEntity entity);
}
