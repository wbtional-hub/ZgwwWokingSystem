package com.example.lecturesystem.modules.aipermission.mapper;

import com.example.lecturesystem.modules.aipermission.dto.UserAiPermissionQueryRequest;
import com.example.lecturesystem.modules.aipermission.entity.UserAiPermissionEntity;
import com.example.lecturesystem.modules.aipermission.vo.UserAiPermissionListItemVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserAiPermissionMapper {
    List<UserAiPermissionListItemVO> queryList(@Param("request") UserAiPermissionQueryRequest request);
    List<UserAiPermissionListItemVO> queryActiveByUserId(@Param("userId") Long userId);
    UserAiPermissionEntity findByUserIdAndProviderId(@Param("userId") Long userId, @Param("providerConfigId") Long providerConfigId);
    int insert(UserAiPermissionEntity entity);
    int update(UserAiPermissionEntity entity);
    int countManageProvider(@Param("userId") Long userId);
    int countCanTrainSkill(@Param("userId") Long userId);
    int countCanPublishSkill(@Param("userId") Long userId);
    int countCanUseAgent(@Param("userId") Long userId);
    int countCanUseAi(@Param("userId") Long userId);
}