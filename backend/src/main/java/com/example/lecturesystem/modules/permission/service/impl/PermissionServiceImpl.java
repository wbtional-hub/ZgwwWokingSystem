package com.example.lecturesystem.modules.permission.service.impl;

import com.example.lecturesystem.modules.permission.service.PermissionService;
import com.example.lecturesystem.modules.user.entity.UserEntity;
import com.example.lecturesystem.modules.permission.mapper.PermissionMapper;
import com.example.lecturesystem.modules.user.mapper.UserMapper;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
public class PermissionServiceImpl implements PermissionService {
    private static final String ROLE_ADMIN = "ADMIN";

    private final UserMapper userMapper;
    private final PermissionMapper permissionMapper;

    public PermissionServiceImpl(PermissionMapper permissionMapper,
                                 UserMapper userMapper,
                                 com.example.lecturesystem.modules.orgtree.mapper.OrgTreeMapper orgTreeMapper) {
        this.userMapper = userMapper;
        this.permissionMapper = permissionMapper;
    }

    @Override
    public boolean isSuperAdmin(Long userId) {
        UserEntity user = userMapper.findById(userId);
        return user != null && ROLE_ADMIN.equalsIgnoreCase(user.getRole());
    }

    @Override
    public boolean isUnitAdmin(Long userId) {
        return false;
    }

    @Override
    public Set<Long> queryDataScopeUserIds(Long currentUserId) {
        // 返回空集合表示“全量数据范围”，调用方据此识别超级管理员。
        if (isSuperAdmin(currentUserId)) {
            return Set.of();
        }

        UserEntity currentUser = userMapper.findById(currentUserId);
        if (currentUser == null) {
            throw new IllegalArgumentException("当前用户不存在");
        }
        if (currentUser.getUnitId() == null) {
            throw new IllegalArgumentException("当前用户未绑定单位");
        }

        return new HashSet<>(permissionMapper.queryUserIdsByUnitId(currentUser.getUnitId()));
    }
}
