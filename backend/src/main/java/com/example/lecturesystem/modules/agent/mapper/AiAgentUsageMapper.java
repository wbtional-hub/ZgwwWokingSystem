package com.example.lecturesystem.modules.agent.mapper;

import com.example.lecturesystem.modules.agent.entity.AiAgentUsageEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AiAgentUsageMapper {
    int insert(AiAgentUsageEntity entity);

    Long sumMonthTotalTokensByUserId(@Param("userId") Long userId);
}