package com.example.lecturesystem.modules.knowledge.mapper;

import com.example.lecturesystem.modules.knowledge.entity.AiPolicyCatalogEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AiPolicyCatalogMapper {
    int deleteByBaseId(@Param("baseId") Long baseId);
    int batchInsert(@Param("list") List<AiPolicyCatalogEntity> list);
    List<AiPolicyCatalogEntity> queryByBaseId(@Param("baseId") Long baseId,
                                              @Param("regionScope") String regionScope,
                                              @Param("searchable") Boolean searchable);
}
