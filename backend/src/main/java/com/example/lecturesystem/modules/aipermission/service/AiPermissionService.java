package com.example.lecturesystem.modules.aipermission.service;

import com.example.lecturesystem.modules.aipermission.dto.*;

public interface AiPermissionService {
    Object listUserAiPermissions(UserAiPermissionQueryRequest request);
    void saveUserAiPermission(SaveUserAiPermissionRequest request);
    Object listUserKnowledgePermissions(UserKnowledgePermissionQueryRequest request);
    void saveUserKnowledgePermission(SaveUserKnowledgePermissionRequest request);
    Object listUserSkillPermissions(UserSkillPermissionQueryRequest request);
    void saveUserSkillPermission(SaveUserSkillPermissionRequest request);
    Object currentPermissions();
    boolean canManageProvider(Long userId);
    boolean canTrainSkill(Long userId);
    boolean canTrainSkill(Long userId, Long skillId);
    boolean canPublishSkill(Long userId);
    boolean canPublishSkill(Long userId, Long skillId);
    boolean canUseAgent(Long userId);
    boolean canUseAi(Long userId);
    boolean canViewKnowledgeBase(Long userId, Long baseId);
    boolean canUploadKnowledgeBase(Long userId, Long baseId);
    boolean canAnalyzeKnowledgeBase(Long userId, Long baseId);
    boolean canViewSkill(Long userId, Long skillId);
    boolean canUseSkill(Long userId, Long skillId);
}