package com.example.lecturesystem.modules.knowledge.mapper;

import com.example.lecturesystem.modules.knowledge.dto.KnowledgeBaseQueryRequest;
import com.example.lecturesystem.modules.knowledge.entity.KnowledgeBaseEntity;
import com.example.lecturesystem.modules.knowledge.vo.KnowledgeBaseListItemVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface KnowledgeBaseMapper {
    List<KnowledgeBaseListItemVO> queryList(@Param("request") KnowledgeBaseQueryRequest request);
    List<KnowledgeBaseListItemVO> queryListByUserId(@Param("userId") Long userId, @Param("request") KnowledgeBaseQueryRequest request);
    KnowledgeBaseEntity findById(@Param("id") Long id);
    KnowledgeBaseEntity findByCode(@Param("baseCode") String baseCode);
    int insert(KnowledgeBaseEntity entity);
    int update(KnowledgeBaseEntity entity);
    int updateStatus(@Param("id") Long id,
                     @Param("status") Integer status,
                     @Param("updateUser") String updateUser,
                     @Param("updateTime") LocalDateTime updateTime);
}