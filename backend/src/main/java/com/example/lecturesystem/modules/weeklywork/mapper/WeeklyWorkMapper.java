package com.example.lecturesystem.modules.weeklywork.mapper;

import com.example.lecturesystem.modules.weeklywork.dto.WeeklyWorkQueryRequest;
import com.example.lecturesystem.modules.weeklywork.entity.WeeklyWorkApprovalLogEntity;
import com.example.lecturesystem.modules.weeklywork.entity.WeeklyWorkEntity;
import com.example.lecturesystem.modules.weeklywork.vo.WeeklyWorkApprovalLogVO;
import com.example.lecturesystem.modules.weeklywork.vo.WeeklyWorkListItemVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface WeeklyWorkMapper {
    WeeklyWorkEntity findByUserIdAndWeekNo(@Param("userId") Long userId, @Param("weekNo") String weekNo);

    WeeklyWorkEntity findById(@Param("id") Long id);

    int insert(WeeklyWorkEntity entity);

    int updateDraft(WeeklyWorkEntity entity);

    int markSubmitted(@Param("id") Long id,
                      @Param("status") String status,
                      @Param("currentApprovalNode") String currentApprovalNode,
                      @Param("currentHandlerUserId") Long currentHandlerUserId,
                      @Param("currentHandlerUserName") String currentHandlerUserName,
                      @Param("currentFlowOrder") Integer currentFlowOrder,
                      @Param("finalApproverUserId") Long finalApproverUserId,
                      @Param("approvedTime") LocalDateTime approvedTime,
                      @Param("submitTime") LocalDateTime submitTime);

    int updateApproval(WeeklyWorkEntity entity);

    int insertApprovalLog(WeeklyWorkApprovalLogEntity entity);

    List<WeeklyWorkApprovalLogVO> queryApprovalLogs(@Param("weeklyWorkId") Long weeklyWorkId);

    long countDistinctSubmittedUsers(@Param("treePathPrefix") String treePathPrefix,
                                     @Param("weekNo") String weekNo,
                                     @Param("status") String status);

    List<WeeklyWorkListItemVO> queryList(@Param("request") WeeklyWorkQueryRequest request);
}
