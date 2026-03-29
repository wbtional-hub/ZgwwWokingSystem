package com.example.lecturesystem.modules.expert.service.impl;

import com.example.lecturesystem.modules.auth.security.LoginUser;
import com.example.lecturesystem.modules.expert.dto.ExpertQueryRequest;
import com.example.lecturesystem.modules.expert.dto.SaveSkillOwnerRequest;
import com.example.lecturesystem.modules.expert.entity.SkillOwnerEntity;
import com.example.lecturesystem.modules.expert.mapper.SkillOwnerMapper;
import com.example.lecturesystem.modules.expert.service.ExpertService;
import com.example.lecturesystem.modules.operationlog.service.OperationLogService;
import com.example.lecturesystem.modules.permission.service.PermissionService;
import com.example.lecturesystem.modules.permission.support.CurrentUserFacade;
import com.example.lecturesystem.modules.skill.entity.SkillEntity;
import com.example.lecturesystem.modules.skill.entity.SkillVersionEntity;
import com.example.lecturesystem.modules.skill.mapper.SkillMapper;
import com.example.lecturesystem.modules.skill.mapper.SkillVersionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class ExpertServiceImpl implements ExpertService {
    private final SkillOwnerMapper skillOwnerMapper;
    private final SkillMapper skillMapper;
    private final SkillVersionMapper skillVersionMapper;
    private final CurrentUserFacade currentUserFacade;
    private final PermissionService permissionService;
    private final OperationLogService operationLogService;

    public ExpertServiceImpl(SkillOwnerMapper skillOwnerMapper,
                             SkillMapper skillMapper,
                             SkillVersionMapper skillVersionMapper,
                             CurrentUserFacade currentUserFacade,
                             PermissionService permissionService,
                             OperationLogService operationLogService) {
        this.skillOwnerMapper = skillOwnerMapper;
        this.skillMapper = skillMapper;
        this.skillVersionMapper = skillVersionMapper;
        this.currentUserFacade = currentUserFacade;
        this.permissionService = permissionService;
        this.operationLogService = operationLogService;
    }

    @Override
    public Object list(ExpertQueryRequest request) {
        LoginUser user = currentUserFacade.currentLoginUser();
        ExpertQueryRequest safeRequest = request == null ? new ExpertQueryRequest() : request;
        if (!permissionService.isSuperAdmin(user.getUserId())) {
            safeRequest.setUserId(user.getUserId());
        }
        return skillOwnerMapper.queryList(safeRequest);
    }

    @Override
    @Transactional
    public void saveSkillOwner(SaveSkillOwnerRequest request) {
        LoginUser user = currentUserFacade.currentLoginUser();
        if (!permissionService.isSuperAdmin(user.getUserId())) {
            throw new IllegalArgumentException("Only admin can maintain experts");
        }
        SkillEntity skill = skillMapper.findById(request.getSkillId());
        if (skill == null) {
            throw new IllegalArgumentException("skill not found");
        }
        SkillVersionEntity version = skillVersionMapper.findById(request.getSkillVersionId());
        if (version == null || !request.getSkillId().equals(version.getSkillId())) {
            throw new IllegalArgumentException("skill version does not match skill");
        }
        SkillOwnerEntity existed = skillOwnerMapper.findByUserIdAndSkillId(request.getUserId(), request.getSkillId());
        String expertLevel = normalize(request.getExpertLevel());
        if (existed == null) {
            SkillOwnerEntity entity = new SkillOwnerEntity();
            entity.setUserId(request.getUserId());
            entity.setSkillId(request.getSkillId());
            entity.setSkillVersionId(request.getSkillVersionId());
            entity.setExpertLevel(expertLevel == null ? "NORMAL" : expertLevel);
            entity.setStatus(request.getStatus());
            entity.setGrantTime(LocalDateTime.now());
            skillOwnerMapper.insert(entity);
        } else {
            existed.setSkillVersionId(request.getSkillVersionId());
            existed.setExpertLevel(expertLevel == null ? "NORMAL" : expertLevel);
            existed.setStatus(request.getStatus());
            existed.setGrantTime(LocalDateTime.now());
            skillOwnerMapper.update(existed);
        }
        operationLogService.log("EXPERT", "SAVE_OWNER", request.getUserId(), "bind expert skill=" + request.getSkillId());
    }

    private String normalize(String text) {
        if (text == null) {
            return null;
        }
        String trimmed = text.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}