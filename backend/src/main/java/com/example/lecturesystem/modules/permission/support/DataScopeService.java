package com.example.lecturesystem.modules.permission.support;

import com.example.lecturesystem.modules.permission.mapper.PermissionMapper;
import com.example.lecturesystem.modules.user.entity.UserEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class DataScopeService {
    private static final String ROLE_ADMIN = "ADMIN";
    private final PermissionMapper permissionMapper;
    private final ConcurrentMap<String, Set<Long>> treePathUserIdsCache = new ConcurrentHashMap<>();

    public DataScopeService() {
        this.permissionMapper = null;
    }

    @Autowired
    public DataScopeService(PermissionMapper permissionMapper) {
        this.permissionMapper = permissionMapper;
    }

    public DataScopeType resolveScopeType(UserEntity currentUser) {
        if (currentUser == null) {
            throw new IllegalArgumentException("当前用户不存在");
        }
        return ROLE_ADMIN.equalsIgnoreCase(currentUser.getRole()) ? DataScopeType.CUSTOM : DataScopeType.SUB_TREE;
    }

    public String buildTreePathPrefix(UserEntity currentUser) {
        return buildTreePathPrefix(currentUser, resolveScopeType(currentUser));
    }

    public String buildTreePathPrefix(UserEntity currentUser, DataScopeType scopeType) {
        if (currentUser == null) {
            throw new IllegalArgumentException("当前用户不存在");
        }
        if (scopeType == null) {
            throw new IllegalArgumentException("数据权限类型不存在");
        }
        if (scopeType == DataScopeType.CUSTOM && ROLE_ADMIN.equalsIgnoreCase(currentUser.getRole())) {
            return null;
        }
        if (scopeType == DataScopeType.SELF) {
            if (currentUser.getId() == null) {
                throw new IllegalArgumentException("当前用户缺少用户ID");
            }
            return "/" + currentUser.getId() + "/";
        }
        if (currentUser.getTreePath() == null || currentUser.getTreePath().isBlank()) {
            throw new IllegalArgumentException("当前用户缺少 treePath 权限信息");
        }
        return currentUser.getTreePath().endsWith("/") ? currentUser.getTreePath() : currentUser.getTreePath() + "/";
    }

    public <T extends TreePathScopedRequest> T injectTreePathScope(T request, UserEntity currentUser) {
        if (request == null) {
            return null;
        }
        request.setTreePathPrefix(buildTreePathPrefix(currentUser));
        return request;
    }

    public boolean isReadableTreePath(String treePathPrefix, String targetTreePath) {
        if (treePathPrefix == null) {
            return true;
        }
        if (targetTreePath == null || targetTreePath.isBlank()) {
            return false;
        }
        return targetTreePath.startsWith(treePathPrefix);
    }

    public void validateReadableTreePath(String treePathPrefix, String targetTreePath, String message) {
        if (!isReadableTreePath(treePathPrefix, targetTreePath)) {
            throw new IllegalArgumentException(message);
        }
    }

    public void validateReadableUser(UserEntity currentUser, UserEntity targetUser, String message) {
        if (targetUser == null) {
            throw new IllegalArgumentException(message);
        }
        validateReadableTreePath(
                buildTreePathPrefix(currentUser),
                targetUser.getTreePath(),
                message
        );
    }

    public Set<Long> queryScopedUserIds(UserEntity currentUser) {
        String treePathPrefix = buildTreePathPrefix(currentUser);
        if (treePathPrefix == null) {
            return Set.of();
        }
        if (permissionMapper == null) {
            throw new IllegalStateException("DataScopeService 未配置 PermissionMapper");
        }
        return treePathUserIdsCache.computeIfAbsent(
                treePathPrefix,
                key -> new LinkedHashSet<>(permissionMapper.queryUserIdsByTreePathPrefix(key + "%"))
        );
    }

    public String describeScope(UserEntity currentUser, long scopeUserCount) {
        DataScopeType scopeType = resolveScopeType(currentUser);
        if (scopeType == DataScopeType.SELF) {
            return "当前仅可查看本人，共 " + scopeUserCount + " 人";
        }
        if (scopeType == DataScopeType.SUB_TREE) {
            return "当前可查看自己及下级，共 " + scopeUserCount + " 人";
        }
        return "当前可查看全部用户，共 " + scopeUserCount + " 人";
    }
}
