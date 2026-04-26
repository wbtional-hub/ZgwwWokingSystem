package com.example.lecturesystem.modules.agent.mapper;

import com.example.lecturesystem.modules.agent.entity.AiPolicyCandidateAnswerEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AiPolicyCandidateAnswerMapper {
    int insert(AiPolicyCandidateAnswerEntity entity);
    int deleteByBaseId(@Param("baseId") Long baseId);
    List<AiPolicyCandidateAnswerEntity> queryByReviewStatus(@Param("baseId") Long baseId, @Param("reviewStatus") String reviewStatus);
    AiPolicyCandidateAnswerEntity findById(@Param("id") Long id);
    int updateReviewStatus(@Param("id") Long id, @Param("reviewStatus") String reviewStatus);
    Integer countByReviewStatus(@Param("baseId") Long baseId, @Param("reviewStatus") String reviewStatus);
}
