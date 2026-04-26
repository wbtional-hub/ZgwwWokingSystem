package com.example.lecturesystem.modules.agent.mapper;

import com.example.lecturesystem.modules.agent.entity.AiPolicyFeedbackEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AiPolicyFeedbackMapper {
    int insert(AiPolicyFeedbackEntity entity);
    int deleteByBaseId(@Param("baseId") Long baseId);
    Integer countByIntentAndType(@Param("baseId") Long baseId, @Param("intentId") Long intentId, @Param("feedbackType") String feedbackType);
    Integer countByReviewSignal(@Param("baseId") Long baseId, @Param("feedbackType") String feedbackType);
    List<AiPolicyFeedbackEntity> queryRecentByBaseId(@Param("baseId") Long baseId);
}
