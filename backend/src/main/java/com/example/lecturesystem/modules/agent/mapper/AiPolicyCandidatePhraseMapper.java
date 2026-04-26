package com.example.lecturesystem.modules.agent.mapper;

import com.example.lecturesystem.modules.agent.entity.AiPolicyCandidatePhraseEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AiPolicyCandidatePhraseMapper {
    int insert(AiPolicyCandidatePhraseEntity entity);
    int deleteByBaseId(@Param("baseId") Long baseId);
    List<AiPolicyCandidatePhraseEntity> queryByReviewStatus(@Param("baseId") Long baseId, @Param("reviewStatus") String reviewStatus);
    AiPolicyCandidatePhraseEntity findById(@Param("id") Long id);
    int updateReviewStatus(@Param("id") Long id, @Param("reviewStatus") String reviewStatus);
    Integer countByReviewStatus(@Param("baseId") Long baseId, @Param("reviewStatus") String reviewStatus);
}
