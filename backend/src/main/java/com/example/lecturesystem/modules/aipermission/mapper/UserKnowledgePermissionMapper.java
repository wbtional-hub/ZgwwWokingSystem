package com.example.lecturesystem.modules.aipermission.mapper;

import com.example.lecturesystem.modules.aipermission.dto.UserKnowledgePermissionQueryRequest;
import com.example.lecturesystem.modules.aipermission.entity.UserKnowledgePermissionEntity;
import com.example.lecturesystem.modules.aipermission.vo.UserKnowledgePermissionListItemVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserKnowledgePermissionMapper {
    List<UserKnowledgePermissionListItemVO> queryList(@Param("request") UserKnowledgePermissionQueryRequest request);
    List<UserKnowledgePermissionListItemVO> queryActiveByUserId(@Param("userId") Long userId);
    UserKnowledgePermissionEntity findByUserIdAndBaseId(@Param("userId") Long userId, @Param("baseId") Long baseId);
    int insert(UserKnowledgePermissionEntity entity);
    int update(UserKnowledgePermissionEntity entity);
    int countCanView(@Param("userId") Long userId, @Param("baseId") Long baseId);
    int countCanUpload(@Param("userId") Long userId, @Param("baseId") Long baseId);
    int countCanAnalyze(@Param("userId") Long userId, @Param("baseId") Long baseId);
}