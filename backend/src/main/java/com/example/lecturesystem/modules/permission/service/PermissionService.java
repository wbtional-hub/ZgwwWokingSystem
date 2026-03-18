package com.example.lecturesystem.modules.permission.service;

import java.util.Set;

public interface PermissionService {
    boolean isSuperAdmin(Long userId);
    boolean isUnitAdmin(Long userId);
    Set<Long> queryDataScopeUserIds(Long currentUserId);
}
