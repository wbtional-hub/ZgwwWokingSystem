package com.example.lecturesystem.modules.attendance.mapper;

import com.example.lecturesystem.modules.attendance.entity.AttendancePatchApplyNodeEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AttendancePatchApplyNodeMapper {
    int batchInsert(@Param("list") List<AttendancePatchApplyNodeEntity> list);

    List<AttendancePatchApplyNodeEntity> queryByApplyId(@Param("applyId") Long applyId);

    List<AttendancePatchApplyNodeEntity> queryByApplyIds(@Param("applyIds") List<Long> applyIds);

    AttendancePatchApplyNodeEntity findCurrentPendingNode(@Param("applyId") Long applyId);

    AttendancePatchApplyNodeEntity findCurrentPendingNodeForApprover(@Param("applyId") Long applyId,
                                                                     @Param("approverUserId") Long approverUserId);

    int updateReview(AttendancePatchApplyNodeEntity entity);

    int activateNode(@Param("id") Long id, @Param("updateTime") LocalDateTime updateTime);

    int updateRemainingNodeStatus(@Param("applyId") Long applyId,
                                  @Param("minNodeOrder") Integer minNodeOrder,
                                  @Param("targetStatus") String targetStatus,
                                  @Param("updateTime") LocalDateTime updateTime);
}
