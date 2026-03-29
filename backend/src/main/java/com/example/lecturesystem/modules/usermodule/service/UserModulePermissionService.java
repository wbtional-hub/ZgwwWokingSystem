package com.example.lecturesystem.modules.usermodule.service;

import com.example.lecturesystem.modules.usermodule.dto.SaveUserModulePermissionRequest;

public interface UserModulePermissionService {
    Object listModuleDefinitions();

    Object getUserModulePermissions(Long userId);

    void saveUserModulePermissions(SaveUserModulePermissionRequest request);

    Object currentUserModulePermissions();
}
