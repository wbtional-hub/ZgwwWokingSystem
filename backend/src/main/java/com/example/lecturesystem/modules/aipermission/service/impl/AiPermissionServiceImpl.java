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
import com.example.lecturesystem.modules.aipermission.vo.UserKnowledgePermissionListItemVO;
import com.example.lecturesystem.modules.auth.security.LoginUser;
import com.example.lecturesystem.modules.knowledge.entity.KnowledgeBaseEntity;
import com.example.lecturesystem.modules.knowledge.mapper.KnowledgeBaseMapper;
import com.example.lecturesystem.modules.knowledge.vo.KnowledgeBaseListItemVO;
import com.example.lecturesystem.modules.operationlog.service.OperationLogService;
import com.example.lecturesystem.modules.permission.service.PermissionService;
import com.example.lecturesystem.modules.permission.support.CurrentUserFacade;
import com.example.lecturesystem.modules.user.dto.UserQueryRequest;
import com.example.lecturesystem.modules.user.entity.UserEntity;
import com.example.lecturesystem.modules.user.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class AiPermissionServiceImpl implements AiPermissionService {
    private final UserAiPermissionMapper userAiPermissionMapper;
    private final UserKnowledgePermissionMapper userKnowledgePermissionMapper;
    private final UserSkillPermissionMapper userSkillPermissionMapper;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final UserMapper userMapper;
    private final CurrentUserFacade currentUserFacade;
    private final PermissionService permissionService;
    private final OperationLogService operationLogService;

    public AiPermissionServiceImpl(UserAiPermissionMapper userAiPermissionMapper,
                                   UserKnowledgePermissionMapper userKnowledgePermissionMapper,
                                   UserSkillPermissionMapper userSkillPermissionMapper,
                                   KnowledgeBaseMapper knowledgeBaseMapper,
                                   UserMapper userMapper,
                                   CurrentUserFacade currentUserFacade,
                                   PermissionService permissionService,
                                   OperationLogService operationLogService) {
        this.userAiPermissionMapper = userAiPermissionMapper;
        this.userKnowledgePermissionMapper = userKnowledgePermissionMapper;
        this.userSkillPermissionMapper = userSkillPermissionMapper;
        this.knowledgeBaseMapper = knowledgeBaseMapper;
        this.userMapper = userMapper;
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
        LoginUser loginUser = currentUserFacade.currentLoginUser();
        UserKnowledgePermissionQueryRequest safeRequest = request == null ? new UserKnowledgePermissionQueryRequest() : request;
        if (permissionService.isSuperAdmin(loginUser.getUserId())) {
            List<UserKnowledgePermissionListItemVO> permissionList = userKnowledgePermissionMapper.queryList(safeRequest);
            if (safeRequest.getBaseId() != null) {
                return mergeOwnerPermission(requireBase(safeRequest.getBaseId()), permissionList);
            }
            return permissionList;
        }
        if (safeRequest.getBaseId() == null) {
            throw new IllegalArgumentException("请选择知识库后再查看授权名单");
        }
        KnowledgeBaseEntity base = requireBase(safeRequest.getBaseId());
        requireKnowledgeOwnerOrAdmin(loginUser, base);
        return mergeOwnerPermission(base, userKnowledgePermissionMapper.queryList(safeRequest));
    }

    @Override
    public Object listGrantableKnowledgeUsers(Long baseId) {
        LoginUser loginUser = currentUserFacade.currentLoginUser();
        KnowledgeBaseEntity base = requireBase(baseId);
        UserQueryRequest request = new UserQueryRequest();
        request.setPageNo(1);
        request.setPageSize(200);
        request.setStatus(1);
        if (!permissionService.isSuperAdmin(loginUser.getUserId())) {
            requireKnowledgeOwnerOrAdmin(loginUser, base);
            UserEntity owner = requireUser(loginUser.getUserId());
            request.setUnitId(owner.getUnitId());
        }
        return userMapper.queryPage(request);
    }

    @Override
    @Transactional
    public void saveUserKnowledgePermission(SaveUserKnowledgePermissionRequest request) {
        LoginUser loginUser = currentUserFacade.currentLoginUser();
        KnowledgeBaseEntity base = requireBase(request.getBaseId());
        requireKnowledgeOwnerOrAdmin(loginUser, base);
        if (Objects.equals(base.getOwnerUserId(), request.getUserId())) {
            throw new IllegalArgumentException("知识库创建人默认拥有该知识库使用权限，无需单独设置");
        }
        UserEntity targetUser = requireUser(request.getUserId());
        if (!permissionService.isSuperAdmin(loginUser.getUserId())) {
            UserEntity owner = requireUser(loginUser.getUserId());
            if (!Objects.equals(owner.getUnitId(), targetUser.getUnitId())) {
                throw new IllegalArgumentException("只能授权给本单位用户使用该知识库");
            }
        }
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
        vo.setKnowledgePermissions(mergeOwnedKnowledgePermissions(
                loginUser.getUserId(),
                userKnowledgePermissionMapper.queryActiveByUserId(loginUser.getUserId())
        ));
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
        return permissionService.isSuperAdmin(userId)
                || isKnowledgeOwner(userId, baseId)
                || userKnowledgePermissionMapper.countCanView(userId, baseId) > 0;
    }

    @Override
    public boolean canUploadKnowledgeBase(Long userId, Long baseId) {
        return permissionService.isSuperAdmin(userId)
                || isKnowledgeOwner(userId, baseId)
                || userKnowledgePermissionMapper.countCanUpload(userId, baseId) > 0;
    }

    @Override
    public boolean canAnalyzeKnowledgeBase(Long userId, Long baseId) {
        return permissionService.isSuperAdmin(userId)
                || isKnowledgeOwner(userId, baseId)
                || userKnowledgePermissionMapper.countCanAnalyze(userId, baseId) > 0;
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

    private KnowledgeBaseEntity requireBase(Long baseId) {
        KnowledgeBaseEntity entity = knowledgeBaseMapper.findById(baseId);
        if (entity == null) {
            throw new IllegalArgumentException("知识库不存在");
        }
        return entity;
    }

    private UserEntity requireUser(Long userId) {
        UserEntity entity = userMapper.findById(userId);
        if (entity == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        return entity;
    }

    private void requireKnowledgeOwnerOrAdmin(LoginUser loginUser, KnowledgeBaseEntity base) {
        if (permissionService.isSuperAdmin(loginUser.getUserId())) {
            return;
        }
        if (!Objects.equals(base.getOwnerUserId(), loginUser.getUserId())) {
            throw new IllegalArgumentException("只有知识库创建人或超级管理员可以管理该知识库授权");
        }
    }

    private boolean isKnowledgeOwner(Long userId, Long baseId) {
        KnowledgeBaseEntity base = knowledgeBaseMapper.findById(baseId);
        return base != null && Objects.equals(base.getOwnerUserId(), userId);
    }

    private List<UserKnowledgePermissionListItemVO> mergeOwnerPermission(KnowledgeBaseEntity base,
                                                                         List<UserKnowledgePermissionListItemVO> permissionList) {
        List<UserKnowledgePermissionListItemVO> result = new ArrayList<>(permissionList == null ? List.of() : permissionList);
        UserKnowledgePermissionListItemVO ownerPermission = result.stream()
                .filter(item -> Objects.equals(item.getUserId(), base.getOwnerUserId()))
                .findFirst()
                .orElse(null);
        if (ownerPermission != null) {
            applyOwnerDefaultPermission(ownerPermission, base);
            return result;
        }
        if (base.getOwnerUserId() != null) {
            UserEntity owner = userMapper.findById(base.getOwnerUserId());
            if (owner != null) {
                UserKnowledgePermissionListItemVO defaultOwnerPermission = new UserKnowledgePermissionListItemVO();
                defaultOwnerPermission.setId(null);
                defaultOwnerPermission.setUserId(owner.getId());
                defaultOwnerPermission.setUsername(owner.getUsername());
                defaultOwnerPermission.setRealName(owner.getRealName());
                defaultOwnerPermission.setBaseId(base.getId());
                defaultOwnerPermission.setBaseName(base.getBaseName());
                applyOwnerDefaultPermission(defaultOwnerPermission, base);
                result.add(0, defaultOwnerPermission);
            }
        }
        return result;
    }

    private List<UserKnowledgePermissionListItemVO> mergeOwnedKnowledgePermissions(Long userId,
                                                                                   List<UserKnowledgePermissionListItemVO> permissionList) {
        List<UserKnowledgePermissionListItemVO> result = new ArrayList<>(permissionList == null ? List.of() : permissionList);
        List<KnowledgeBaseListItemVO> ownedBaseList = knowledgeBaseMapper.queryOwnedByUserId(userId);
        for (KnowledgeBaseListItemVO item : ownedBaseList) {
            UserKnowledgePermissionListItemVO existed = result.stream()
                    .filter(permission -> Objects.equals(permission.getBaseId(), item.getId()))
                    .findFirst()
                    .orElse(null);
            if (existed != null) {
                applyOwnerDefaultPermission(existed, item);
                continue;
            }
            UserKnowledgePermissionListItemVO ownerPermission = new UserKnowledgePermissionListItemVO();
            ownerPermission.setId(null);
            ownerPermission.setUserId(userId);
            ownerPermission.setUsername(loginUserUsername(userId));
            ownerPermission.setRealName(loginUserRealName(userId));
            ownerPermission.setBaseId(item.getId());
            ownerPermission.setBaseName(item.getBaseName());
            applyOwnerDefaultPermission(ownerPermission, item);
            result.add(ownerPermission);
        }
        return result;
    }

    private void applyOwnerDefaultPermission(UserKnowledgePermissionListItemVO permission, KnowledgeBaseEntity base) {
        permission.setBaseId(base.getId());
        permission.setBaseName(base.getBaseName());
        permission.setCanView(Boolean.TRUE);
        permission.setCanUpload(Boolean.TRUE);
        permission.setCanTrainSkill(Boolean.TRUE);
        permission.setCanAnalyze(Boolean.TRUE);
        permission.setStatus(1);
        permission.setGrantTime(base.getCreateTime());
    }

    private void applyOwnerDefaultPermission(UserKnowledgePermissionListItemVO permission, KnowledgeBaseListItemVO base) {
        permission.setBaseId(base.getId());
        permission.setBaseName(base.getBaseName());
        permission.setCanView(Boolean.TRUE);
        permission.setCanUpload(Boolean.TRUE);
        permission.setCanTrainSkill(Boolean.TRUE);
        permission.setCanAnalyze(Boolean.TRUE);
        permission.setStatus(1);
        permission.setGrantTime(base.getCreateTime());
    }

    private String loginUserUsername(Long userId) {
        UserEntity user = userMapper.findById(userId);
        return user == null ? null : user.getUsername();
    }

    private String loginUserRealName(Long userId) {
        UserEntity user = userMapper.findById(userId);
        return user == null ? null : user.getRealName();
    }
}
