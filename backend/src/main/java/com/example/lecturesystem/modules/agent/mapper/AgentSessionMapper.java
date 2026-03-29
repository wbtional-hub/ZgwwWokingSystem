package com.example.lecturesystem.modules.agent.mapper;

import com.example.lecturesystem.modules.agent.dto.AgentSessionQueryRequest;
import com.example.lecturesystem.modules.agent.entity.AgentSessionEntity;
import com.example.lecturesystem.modules.agent.vo.AgentExpertMetricVO;
import com.example.lecturesystem.modules.agent.vo.AgentMonthlySummaryVO;
import com.example.lecturesystem.modules.agent.vo.AgentRankItemVO;
import com.example.lecturesystem.modules.agent.vo.AgentSessionVO;
import com.example.lecturesystem.modules.agent.vo.AgentTrendPointVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AgentSessionMapper {
    int insert(AgentSessionEntity entity);
    int updateTitle(@Param("id") Long id, @Param("sessionTitle") String sessionTitle);
    int updateStatus(@Param("id") Long id, @Param("status") String status);
    List<AgentSessionVO> queryList(@Param("request") AgentSessionQueryRequest request);
    List<AgentTrendPointVO> queryDailyTrend(@Param("request") AgentSessionQueryRequest request);
    List<AgentRankItemVO> querySkillRanking(@Param("request") AgentSessionQueryRequest request);
    List<AgentRankItemVO> queryUserRanking(@Param("request") AgentSessionQueryRequest request);
    AgentMonthlySummaryVO queryMonthlySummary(@Param("request") AgentSessionQueryRequest request);
    List<AgentTrendPointVO> queryMonthlyTrend(@Param("request") AgentSessionQueryRequest request);
    List<AgentRankItemVO> queryMonthlySkillRanking(@Param("request") AgentSessionQueryRequest request);
    List<AgentRankItemVO> queryMonthlyBaseRanking(@Param("request") AgentSessionQueryRequest request);
    List<AgentRankItemVO> queryMonthlyUserRanking(@Param("request") AgentSessionQueryRequest request);
    List<AgentExpertMetricVO> queryMonthlyExpertRanking(@Param("request") AgentSessionQueryRequest request);
    AgentSessionEntity findById(@Param("id") Long id);
    AgentSessionVO queryDetail(@Param("id") Long id);
}