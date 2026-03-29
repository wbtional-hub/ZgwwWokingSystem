package com.example.lecturesystem.modules.aipermission.service.impl;

import com.example.lecturesystem.modules.aipermission.dto.SaveUserAiPermissionRequest;
import com.example.lecturesystem.modules.aipermission.dto.SaveUserKnowledgePermissionRequest;
import com.example.lecturesystem.modules.aipermission.dto.SaveUserSkillPermissionRequest;
import com.example.lecturesystem.modules.aipermission.dto.UserAiPermissionQueryRequest;
import com.example.lecturesystem.modules.aipermission.dto.UserKnowledgePermissionQueryRequest;
import com.example.lecturesystem.modules.aipermission.dto.UserSkillPermissionQueryRequest;
import com.example.lecturesystem.modules.aipermission.entity.UserAiPermissionEntity;
import com.example.lecturesystem.modules.aipermission.entity.UserKnowledgePermissionEntity;
import com.example.lecturesystem.modules.aipermission.entity.UserSkillPermissionEntity;
import com.example.lecturesystem.modules.aipermission.mapper.UserAiPermissionMapper;
import com.example.lecturesystem.modules.aipermission.mapper.UserKnowledgePermissionMapper;
import com.example.lecturesystem.modules.aipermission.mapper.UserSkillPermissionMapper;
import com.example.lecturesystem.modules.aipermission.service.AiPermissionService;
import com.example.lecturesystem.modules.aipermission.vo.CurrentAiPermissionVO;
import com.example.lecturesystem.modules.auth.security.LoginUser;
import com.example.lecturesystem.modules.operationlog.service.OperationLogService;
import com.example.lecturesystem.modules.permission.service.PermissionService;
import com.example.lecturesystem.modules.permission.support.CurrentUserFacade;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AiPermissionServiceImpl implements AiPermissionService {
    private final UserAiPermissionMapper userAiPermissionMapper;
    private final UserKnowledgePermissionMapper userKnowledgePermissionMapper;
    private final UserSkillPermissionMapper userSkillPermissionMapper;
    private final CurrentUserFacade currentUserFacade;
    private final PermissionService permissionService;
    private final OperationLogService operationLogService;

    public AiPermissionServiceImpl(UserAiPermissionMapper userAiPermissionMapper,
                                   UserKnowledgePermissionMapper userKnowledgePermissionMapper,
                                   UserSkillPermissionMapper userSkillPermissionMapper,
                                   CurrentUserFacade currentUserFacade,
                                   PermissionService permissionService,
                                   OperationLogService operationLogService) {
        this.userAiPermissionMapper = userAiPermissionMapper;
        this.userKnowledgePermissionMapper = userKnowledgePermissionMapper;
        this.userSkillPermissionMapper = userSkillPermissionMapper;
        this.currentUserFacade = currentUserFacade;
        this.permissionService = permissionService;
        this.operationLogService = operationLogService;
    }

    @Override
    public Object listUserAiPermissions(UserAiPermissionQueryRequest request) {
        requireAdmin();
        return userAiPermissionMapper.queryList(request == null ? new UserAiPermissionQueryRequest() : request);
    }

    @Override
    @Transactional
    public void saveUserAiPermission(SaveUserAiPermissionRequest request) {
        LoginUser loginUser = currentUserFacade.currentLoginUser();
        requireAdmin();
        UserAiPermissionEntity existed = userAiPermissionMapper.findByUserIdAndProviderId(request.getUserId(), request.getProviderConfigId());
        if (existed == null) {
            UserAiPermissionEntity entity = new UserAiPermissionEntity();
            entity.setUserId(request.getUserId());
            entity.setProviderConfigId(request.getProviderConfigId());
            entity.setCanManageProvider(Boolean.TRUE.equals(request.getCanManageProvider()));
            entity.setCanUseAi(Boolean.TRUE.equals(request.getCanUseAi()));
            entity.setCanTrainSkill(Boolean.TRUE.equals(request.getCanTrainSkill()));
            entity.setCanPublishSkill(Boolean.TRUE.equals(request.getCanPublishSkill()));
            entity.setCanUseAgent(Boolean.TRUE.equals(request.getCanUseAgent()));
            entity.setCanRunAnalysis(Boolean.TRUE.equals(request.getCanRunAnalysis()));
            entity.setStatus(request.getStatus());
            entity.setGrantTime(LocalDateTime.now());
            entity.setGrantUser(loginUser.getUsername());
            userAiPermissionMapper.insert(entity);
        } else {
            existed.setCanManageProvider(Boolean.TRUE.equals(request.getCanManageProvider()));
            existed.setCanUseAi(Boolean.TRUE.equals(request.getCanUseAi()));
            existed.setCanTrainSkill(Boolean.TRUE.equals(request.getCanTrainSkill()));
            existed.setCanPublishSkill(Boolean.TRUE.equals(request.getCanPublishSkill()));
            existed.setCanUseAgent(Boolean.TRUE.equals(request.getCanUseAgent()));
            existed.setCanRunAnalysis(Boolean.TRUE.equals(request.getCanRunAnalysis()));
            existed.setStatus(request.getStatus());
            existed.setGrantTime(LocalDateTime.now());
            existed.setGrantUser(loginUser.getUsername());
            userAiPermissionMapper.update(existed);
        }
        operationLogService.log("AI_PERMISSION", "SAVE_USER_AI_PERMISSION", request.getUserId(), "save user AI permission, provider=" + request.getProviderConfigId());
    }

    @Override
    public Object listUserKnowledgePermissions(UserKnowledgePermissionQueryRequest request) {
        requireAdmin();
        return userKnowledgePermissionMapper.queryList(request == null ? new UserKnowledgePermissionQueryRequest() : request);
    }

    @Override
    @Transactional
    public void saveUserKnowledgePermission(SaveUserKnowledgePermissionRequest request) {
        LoginUser loginUser = currentUserFacade.currentLoginUser();
        requireAdmin();
        UserKnowledgePermissionEntity existed = userKnowledgePermissionMapper.findByUserIdAndBaseId(request.getUserId(), request.getBaseId());
        if (existed == null) {
            UserKnowledgePermissionEntity entity = new UserKnowledgePermissionEntity();
            entity.setUserId(request.getUserId());
            entity.setBaseId(request.getBaseId());
            entity.setCanView(Boolean.TRUE.equals(request.getCanView()));
            entity.setCanUpload(Boolean.TRUE.equals(request.getCanUpload()));
            entity.setCanTrainSkill(Boolean.TRUE.equals(request.getCanTrainSkill()));
            entity.setCanAnalyze(Boolean.TRUE.equals(request.getCanAnalyze()));
            entity.setStatus(request.getStatus());
            entity.setGrantTime(LocalDateTime.now());
            entity.setGrantUser(loginUser.getUsername());
            userKnowledgePermissionMapper.insert(entity);
        } else {
            existed.setCanView(Boolean.TRUE.equals(request.getCanView()));
            existed.setCanUpload(Boolean.TRUE.equals(request.getCanUpload()));
            existed.setCanTrainSkill(Boolean.TRUE.equals(request.getCanTrainSkill()));
            existed.setCanAnalyze(Boolean.TRUE.equals(request.getCanAnalyze()));
            existed.setStatus(request.getStatus());
            existed.setGrantTime(LocalDateTime.now());
            existed.setGrantUser(loginUser.getUsername());
            userKnowledgePermissionMapper.update(existed);
        }
        operationLogService.log("AI_PERMISSION", "SAVE_USER_KNOWLEDGE_PERMISSION", request.getUserId(), "save user knowledge permission, base=" + request.getBaseId());
    }

    @Override
    public Object listUserSkillPermissions(UserSkillPermissionQueryRequest request) {
        requireAdmin();
        return userSkillPermissionMapper.queryList(request == null ? new UserSkillPermissionQueryRequest() : request);
    }

    @Override
    @Transactional
    public void saveUserSkillPermission(SaveUserSkillPermissionRequest request) {
        LoginUser loginUser = currentUserFacade.currentLoginUser();
        requireAdmin();
        UserSkillPermissionEntity existed = userSkillPermissionMapper.findByUserIdAndSkillId(request.getUserId(), request.getSkillId());
        if (existed == null) {
            UserSkillPermissionEntity entity = new UserSkillPermissionEntity();
            entity.setUserId(request.getUserId());
            entity.setSkillId(request.getSkillId());
            entity.setCanView(Boolean.TRUE.equals(request.getCanView()));
            entity.setCanUse(Boolean.TRUE.equals(request.getCanUse()));
            entity.setCanTrain(Boolean.TRUE.equals(request.getCanTrain()));
            entity.setCanPublish(Boolean.TRUE.equals(request.getCanPublish()));
            entity.setStatus(request.getStatus());
            entity.setGrantTime(LocalDateTime.now());
            entity.setGrantUser(loginUser.getUsername());
            userSkillPermissionMapper.insert(entity);
        } else {
            existed.setCanView(Boolean.TRUE.equals(request.getCanView()));
            existed.setCanUse(Boolean.TRUE.equals(request.getCanUse()));
            existed.setCanTrain(Boolean.TRUE.equals(request.getCanTrain()));
            existed.setCanPublish(Boolean.TRUE.equals(request.getCanPublish()));
            existed.setStatus(request.getStatus());
            existed.setGrantTime(LocalDateTime.now());
            existed.setGrantUser(loginUser.getUsername());
            userSkillPermissionMapper.update(existed);
        }
        operationLogService.log("AI_PERMISSION", "SAVE_USER_SKILL_PERMISSION", request.getUserId(), "save user skill permission, skill=" + request.getSkillId());
    }

    @Override
    public Object currentPermissions() {
        LoginUser loginUser = currentUserFacade.currentLoginUser();
        boolean admin = permissionService.isSuperAdmin(loginUser.getUserId());
        CurrentAiPermissionVO vo = new CurrentAiPermissionVO();
        vo.setAdmin(admin);
        vo.setCanManageProvider(admin || canManageProvider(loginUser.getUserId()));
        vo.setAiPermissions(userAiPermissionMapper.queryActiveByUserId(loginUser.getUserId()));
        vo.setKnowledgePermissions(userKnowledgePermissionMapper.queryActiveByUserId(loginUser.getUserId()));
        vo.setSkillPermissions(userSkillPermissionMapper.queryActiveByUserId(loginUser.getUserId()));
        return vo;
    }

    @Override
    public boolean canManageProvider(Long userId) {
        return permissionService.isSuperAdmin(userId) || userAiPermissionMapper.countManageProvider(userId) > 0;
    }

    @Override
    public boolean canTrainSkill(Long userId) {
        return permissionService.isSuperAdmin(userId) || userAiPermissionMapper.countCanTrainSkill(userId) > 0;
    }

    @Override
    public boolean canTrainSkill(Long userId, Long skillId) {
        if (permissionService.isSuperAdmin(userId)) {
            return true;
        }
        return userSkillPermissionMapper.countCanTrain(userId, skillId) > 0 || userSkillPermissionMapper.countCanPublish(userId, skillId) > 0;
    }

    @Override
    public boolean canPublishSkill(Long userId) {
        return permissionService.isSuperAdmin(userId) || userAiPermissionMapper.countCanPublishSkill(userId) > 0;
    }

    @Override
    public boolean canPublishSkill(Long userId, Long skillId) {
        if (permissionService.isSuperAdmin(userId)) {
            return true;
        }
        return userSkillPermissionMapper.countCanPublish(userId, skillId) > 0;
    }

    @Override
    public boolean canUseAgent(Long userId) {
        return permissionService.isSuperAdmin(userId) || userAiPermissionMapper.countCanUseAgent(userId) > 0;
    }

    @Override
    public boolean canUseAi(Long userId) {
        return permissionService.isSuperAdmin(userId) || userAiPermissionMapper.countCanUseAi(userId) > 0;
    }

    @Override
    public boolean canViewKnowledgeBase(Long userId, Long baseId) {
        return permissionService.isSuperAdmin(userId) || userKnowledgePermissionMapper.countCanView(userId, baseId) > 0;
    }

    @Override
    public boolean canUploadKnowledgeBase(Long userId, Long baseId) {
        return permissionService.isSuperAdmin(userId) || userKnowledgePermissionMapper.countCanUpload(userId, baseId) > 0;
    }

    @Override
    public boolean canAnalyzeKnowledgeBase(Long userId, Long baseId) {
        return permissionService.isSuperAdmin(userId) || userKnowledgePermissionMapper.countCanAnalyze(userId, baseId) > 0;
    }

    @Override
    public boolean canViewSkill(Long userId, Long skillId) {
        if (permissionService.isSuperAdmin(userId)) {
            return true;
        }
        return userSkillPermissionMapper.countCanView(userId, skillId) > 0
                || userSkillPermissionMapper.countCanUse(userId, skillId) > 0
                || userSkillPermissionMapper.countCanTrain(userId, skillId) > 0
                || userSkillPermissionMapper.countCanPublish(userId, skillId) > 0;
    }

    @Override
    public boolean canUseSkill(Long userId, Long skillId) {
        if (permissionService.isSuperAdmin(userId)) {
            return true;
        }
        return userSkillPermissionMapper.countCanUse(userId, skillId) > 0
                || userSkillPermissionMapper.countCanTrain(userId, skillId) > 0
                || userSkillPermissionMapper.countCanPublish(userId, skillId) > 0;
    }

    private void requireAdmin() {
        LoginUser loginUser = currentUserFacade.currentLoginUser();
        if (!permissionService.isSuperAdmin(loginUser.getUserId())) {
            throw new IllegalArgumentException("Only admin can manage AI permissions");
        }
    }
}