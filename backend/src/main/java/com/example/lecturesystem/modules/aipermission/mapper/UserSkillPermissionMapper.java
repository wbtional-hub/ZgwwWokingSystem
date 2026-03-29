package com.example.lecturesystem.modules.aipermission.mapper;

import com.example.lecturesystem.modules.aipermission.dto.UserSkillPermissionQueryRequest;
import com.example.lecturesystem.modules.aipermission.entity.UserSkillPermissionEntity;
import com.example.lecturesystem.modules.aipermission.vo.UserSkillPermissionListItemVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserSkillPermissionMapper {
    List<UserSkillPermissionListItemVO> queryList(@Param("request") UserSkillPermissionQueryRequest request);
    List<UserSkillPermissionListItemVO> queryActiveByUserId(@Param("userId") Long userId);
    UserSkillPermissionEntity findByUserIdAndSkillId(@Param("userId") Long userId, @Param("skillId") Long skillId);
    int insert(UserSkillPermissionEntity entity);
    int update(UserSkillPermissionEntity entity);
    int countCanView(@Param("userId") Long userId, @Param("skillId") Long skillId);
    int countCanUse(@Param("userId") Long userId, @Param("skillId") Long skillId);
    int countCanTrain(@Param("userId") Long userId, @Param("skillId") Long skillId);
    int countCanPublish(@Param("userId") Long userId, @Param("skillId") Long skillId);
}