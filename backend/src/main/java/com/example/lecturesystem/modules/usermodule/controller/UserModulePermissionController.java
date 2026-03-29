package com.example.lecturesystem.modules.usermodule.controller;

import com.example.lecturesystem.common.ApiResponse;
import com.example.lecturesystem.modules.usermodule.dto.SaveUserModulePermissionRequest;
import com.example.lecturesystem.modules.usermodule.service.UserModulePermissionService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user-module-permissions")
public class UserModulePermissionController {
    private final UserModulePermissionService userModulePermissionService;

    public UserModulePermissionController(UserModulePermissionService userModulePermissionService) {
        this.userModulePermissionService = userModulePermissionService;
    }

    @GetMapping("/modules")
    public ApiResponse<?> listModules() {
        return ApiResponse.success(userModulePermissionService.listModuleDefinitions());
    }

    @GetMapping("/users/{userId}")
    public ApiResponse<?> getUserModules(@PathVariable("userId") Long userId) {
        return ApiResponse.success(userModulePermissionService.getUserModulePermissions(userId));
    }

    @PutMapping("/users/{userId}")
    public ApiResponse<?> saveUserModules(@PathVariable("userId") Long userId,
                                          @Validated @RequestBody SaveUserModulePermissionRequest request) {
        request.setUserId(userId);
        userModulePermissionService.saveUserModulePermissions(request);
        return ApiResponse.success("ok");
    }

    @GetMapping("/current")
    public ApiResponse<?> currentUserModules() {
        return ApiResponse.success(userModulePermissionService.currentUserModulePermissions());
    }
}
