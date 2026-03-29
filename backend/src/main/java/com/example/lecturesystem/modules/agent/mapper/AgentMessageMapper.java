package com.example.lecturesystem.modules.agent.mapper;

import com.example.lecturesystem.modules.agent.entity.AgentMessageEntity;
import com.example.lecturesystem.modules.agent.vo.AgentMessageVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AgentMessageMapper {
    int insert(AgentMessageEntity entity);
    List<AgentMessageVO> queryBySessionId(@Param("sessionId") Long sessionId);
}