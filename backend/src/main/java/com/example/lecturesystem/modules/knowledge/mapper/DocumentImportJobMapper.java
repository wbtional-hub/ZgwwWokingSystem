package com.example.lecturesystem.modules.knowledge.mapper;

import com.example.lecturesystem.modules.knowledge.entity.DocumentImportJobEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DocumentImportJobMapper {
    int insert(DocumentImportJobEntity entity);
    int updateResult(DocumentImportJobEntity entity);
}
