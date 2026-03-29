package com.example.lecturesystem.modules.usermodule.service.impl;

import com.example.lecturesystem.modules.auth.security.LoginUser;
import com.example.lecturesystem.modules.operationlog.service.OperationLogService;
import com.example.lecturesystem.modules.permission.service.PermissionService;
import com.example.lecturesystem.modules.permission.support.CurrentUserFacade;
import com.example.lecturesystem.modules.user.entity.UserEntity;
import com.example.lecturesystem.modules.user.mapper.UserMapper;
import com.example.lecturesystem.modules.usermodule.dto.SaveUserModulePermissionRequest;
import com.example.lecturesystem.modules.usermodule.entity.UserModulePermissionEntity;
import com.example.lecturesystem.modules.usermodule.mapper.UserModulePermissionMapper;
import com.example.lecturesystem.modules.usermodule.service.UserModulePermissionService;
import com.example.lecturesystem.modules.usermodule.support.SystemModuleRegistry;
import com.example.lecturesystem.modules.usermodule.vo.UserModulePermissionVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class UserModulePermissionServiceImpl implements UserModulePermissionService {
    private final UserModulePermissionMapper userModulePermissionMapper;
    private final UserMapper userMapper;
    private final CurrentUserFacade currentUserFacade;
    private final PermissionService permissionService;
    private final OperationLogService operationLogService;
    private final SystemModuleRegistry systemModuleRegistry;

    public UserModulePermissionServiceImpl(UserModulePermissionMapper userModulePermissionMapper,
                                           UserMapper userMapper,
                                           CurrentUserFacade currentUserFacade,
                                           PermissionService permissionService,
                                           OperationLogService operationLogService,
                                           SystemModuleRegistry systemModuleRegistry) {
        this.userModulePermissionMapper = userModulePermissionMapper;
        this.userMapper = userMapper;
        this.currentUserFacade = currentUserFacade;
        this.permissionService = permissionService;
        this.operationLogService = operationLogService;
        this.systemModuleRegistry = systemModuleRegistry;
    }

    @Override
    public Object listModuleDefinitions() {
        requireAdmin();
        return systemModuleRegistry.listEnabledDefinitions();
    }

    @Override
    public Object getUserModulePermissions(Long userId) {
        requireAdmin();
        UserEntity user = requireUser(userId);
        UserModulePermissionVO vo = new UserModulePermissionVO();
        vo.setUserId(user.getId());
        vo.setAdmin(permissionService.isSuperAdmin(user.getId()));
        vo.setModuleCodes(resolveEffectiveModuleCodes(user.getId()));
        return vo;
    }

    @Override
    @Transactional
    public void saveUserModulePermissions(SaveUserModulePermissionRequest request) {
        LoginUser loginUser = currentUserFacade.currentLoginUser();
        requireAdmin();
        UserEntity user = requireUser(request.getUserId());
        List<String> normalizedCodes = normalizeAndValidateModuleCodes(request.getModuleCodes());

        userModulePermissionMapper.deleteByUserId(user.getId());
        if (!normalizedCodes.isEmpty()) {
            LocalDateTime now = LocalDateTime.now();
            List<UserModulePermissionEntity> entities = new ArrayList<>();
            for (String moduleCode : normalizedCodes) {
                UserModulePermissionEntity entity = new UserModulePermissionEntity();
                entity.setUserId(user.getId());
                entity.setModuleCode(moduleCode);
                entity.setCreateTime(now);
                entity.setCreateBy(loginUser.getUsername());
                entity.setUpdateTime(now);
                entity.setUpdateBy(loginUser.getUsername());
                entities.add(entity);
            }
            userModulePermissionMapper.batchInsert(entities);
        }

        operationLogService.log(
                "USER_MODULE_PERMISSION",
                "SAVE",
                user.getId(),
                "保存用户模块权限：" + user.getUsername() + " -> " + String.join(",", normalizedCodes)
        );
    }

    @Override
    public Object currentUserModulePermissions() {
        LoginUser loginUser = currentUserFacade.currentLoginUser();
        UserModulePermissionVO vo = new UserModulePermissionVO();
        vo.setUserId(loginUser.getUserId());
        vo.setAdmin(permissionService.isSuperAdmin(loginUser.getUserId()));
        vo.setModuleCodes(resolveEffectiveModuleCodes(loginUser.getUserId()));
        return vo;
    }

    private List<String> resolveEffectiveModuleCodes(Long userId) {
        if (permissionService.isSuperAdmin(userId)) {
            return systemModuleRegistry.listEnabledModuleCodes();
        }
        return userModulePermissionMapper.findModuleCodesByUserId(userId).stream()
                .map(systemModuleRegistry::normalizeModuleCode)
                .filter(systemModuleRegistry::containsEnabledModule)
                .distinct()
                .toList();
    }

    private List<String> normalizeAndValidateModuleCodes(List<String> moduleCodes) {
        Set<String> uniqueCodes = new LinkedHashSet<>();
        if (moduleCodes != null) {
            for (String moduleCode : moduleCodes) {
                String normalizedCode = systemModuleRegistry.normalizeModuleCode(moduleCode);
                if (normalizedCode == null || normalizedCode.isBlank()) {
                    continue;
                }
                if (!systemModuleRegistry.containsEnabledModule(normalizedCode)) {
                    throw new IllegalArgumentException("存在无效模块编码：" + moduleCode);
                }
                uniqueCodes.add(normalizedCode);
            }
        }
        return new ArrayList<>(uniqueCodes);
    }

    private void requireAdmin() {
        LoginUser loginUser = currentUserFacade.currentLoginUser();
        if (!permissionService.isSuperAdmin(loginUser.getUserId())) {
            throw new IllegalArgumentException("仅管理员可执行该操作");
        }
    }

    private UserEntity requireUser(Long userId) {
        UserEntity user = userMapper.findById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        return user;
    }
}
