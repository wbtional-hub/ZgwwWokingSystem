package com.example.lecturesystem.modules.attendance.mapper;

import com.example.lecturesystem.modules.attendance.dto.AttendancePatchApplyQueryRequest;
import com.example.lecturesystem.modules.attendance.entity.AttendancePatchApplyEntity;
import com.example.lecturesystem.modules.attendance.vo.AttendancePatchApplyDetailVO;
import com.example.lecturesystem.modules.attendance.vo.AttendancePatchApplyListItemVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface AttendancePatchApplyMapper {
    int insert(AttendancePatchApplyEntity entity);

    AttendancePatchApplyEntity findById(@Param("id") Long id);

    AttendancePatchApplyEntity findPendingByUserDateType(@Param("userId") Long userId,
                                                         @Param("attendanceDate") LocalDate attendanceDate,
                                                         @Param("patchType") String patchType);

    long countMyPage(@Param("userId") Long userId,
                     @Param("request") AttendancePatchApplyQueryRequest request);

    List<AttendancePatchApplyListItemVO> queryMyPage(@Param("userId") Long userId,
                                                     @Param("request") AttendancePatchApplyQueryRequest request);

    long countPendingPage(@Param("treePathPrefix") String treePathPrefix,
                          @Param("currentUserId") Long currentUserId,
                          @Param("request") AttendancePatchApplyQueryRequest request);

    List<AttendancePatchApplyListItemVO> queryPendingPage(@Param("treePathPrefix") String treePathPrefix,
                                                          @Param("currentUserId") Long currentUserId,
                                                          @Param("request") AttendancePatchApplyQueryRequest request);

    AttendancePatchApplyDetailVO detailById(@Param("id") Long id);

    int updateReview(AttendancePatchApplyEntity entity);
}
