package com.example.lecturesystem.modules.agent.mapper;

import com.example.lecturesystem.modules.agent.entity.AiPolicyUserFavoriteEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AiPolicyUserFavoriteMapper {
    int insert(AiPolicyUserFavoriteEntity entity);
    int deleteByBaseId(@Param("baseId") Long baseId);
    Integer countByBaseId(@Param("baseId") Long baseId);
    Integer countByBaseIdWithIntent(@Param("baseId") Long baseId);
    Integer countByIntentId(@Param("baseId") Long baseId, @Param("intentId") Long intentId);
    Integer countByUserAndIntentId(@Param("baseId") Long baseId, @Param("userId") Long userId, @Param("intentId") Long intentId);
    List<AiPolicyUserFavoriteEntity> queryRecentByBaseId(@Param("baseId") Long baseId);
}
