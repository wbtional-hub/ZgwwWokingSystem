package com.example.lecturesystem.modules.agent.mapper;

import com.example.lecturesystem.modules.agent.entity.AiPolicyFeedbackActionEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AiPolicyFeedbackActionMapper {
    int insert(AiPolicyFeedbackActionEntity entity);
    List<AiPolicyFeedbackActionEntity> queryByTaskId(@Param("taskId") Long taskId);
}
