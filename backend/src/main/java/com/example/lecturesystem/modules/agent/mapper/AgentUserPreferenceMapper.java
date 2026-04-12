package com.example.lecturesystem.modules.agent.mapper;

import com.example.lecturesystem.modules.agent.entity.AgentUserPreferenceEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AgentUserPreferenceMapper {
    AgentUserPreferenceEntity findByUserId(@Param("userId") Long userId);

    int upsert(AgentUserPreferenceEntity entity);
}
