package com.example.lecturesystem.modules.agent.mapper;

import com.example.lecturesystem.modules.agent.dto.AiPolicyCorrectionTaskQueryRequest;
import com.example.lecturesystem.modules.agent.entity.AiPolicyFeedbackTaskEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AiPolicyFeedbackTaskMapper {
    int insert(AiPolicyFeedbackTaskEntity entity);
    AiPolicyFeedbackTaskEntity findById(@Param("id") Long id);
    List<AiPolicyFeedbackTaskEntity> queryPage(@Param("request") AiPolicyCorrectionTaskQueryRequest request,
                                               @Param("limit") int limit,
                                               @Param("offset") int offset);
    int countPage(@Param("request") AiPolicyCorrectionTaskQueryRequest request);
    int updateStatus(@Param("id") Long id,
                     @Param("status") String status,
                     @Param("handlerId") Long handlerId,
                     @Param("handleResult") String handleResult,
                     @Param("markHandled") boolean markHandled);
}
