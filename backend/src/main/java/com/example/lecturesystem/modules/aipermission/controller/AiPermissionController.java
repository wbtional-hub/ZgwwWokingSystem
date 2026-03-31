package com.example.lecturesystem.modules.aipermission.controller;

import com.example.lecturesystem.common.ApiResponse;
import com.example.lecturesystem.modules.aipermission.dto.*;
import com.example.lecturesystem.modules.aipermission.service.AiPermissionService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/permission")
public class AiPermissionController {
    private final AiPermissionService aiPermissionService;

    public AiPermissionController(AiPermissionService aiPermissionService) {
        this.aiPermissionService = aiPermissionService;
    }

    @PostMapping("/user-ai/list")
    public ApiResponse<?> listUserAiPermissions(@RequestBody(required = false) UserAiPermissionQueryRequest request) {
        return ApiResponse.success(aiPermissionService.listUserAiPermissions(request == null ? new UserAiPermissionQueryRequest() : request));
    }

    @PostMapping("/user-ai/save")
    public ApiResponse<?> saveUserAiPermission(@Validated @RequestBody SaveUserAiPermissionRequest request) {
        aiPermissionService.saveUserAiPermission(request);
        return ApiResponse.success("ok");
    }

    @PostMapping("/user-knowledge/list")
    public ApiResponse<?> listUserKnowledgePermissions(@RequestBody(required = false) UserKnowledgePermissionQueryRequest request) {
        return ApiResponse.success(aiPermissionService.listUserKnowledgePermissions(request == null ? new UserKnowledgePermissionQueryRequest() : request));
    }

    @GetMapping("/user-knowledge/grantable-users/{baseId}")
    public ApiResponse<?> listGrantableKnowledgeUsers(@PathVariable("baseId") Long baseId) {
        return ApiResponse.success(aiPermissionService.listGrantableKnowledgeUsers(baseId));
    }

    @PostMapping("/user-knowledge/save")
    public ApiResponse<?> saveUserKnowledgePermission(@Validated @RequestBody SaveUserKnowledgePermissionRequest request) {
        aiPermissionService.saveUserKnowledgePermission(request);
        return ApiResponse.success("ok");
    }

    @PostMapping("/user-skill/list")
    public ApiResponse<?> listUserSkillPermissions(@RequestBody(required = false) UserSkillPermissionQueryRequest request) {
        return ApiResponse.success(aiPermissionService.listUserSkillPermissions(request == null ? new UserSkillPermissionQueryRequest() : request));
    }

    @PostMapping("/user-skill/save")
    public ApiResponse<?> saveUserSkillPermission(@Validated @RequestBody SaveUserSkillPermissionRequest request) {
        aiPermissionService.saveUserSkillPermission(request);
        return ApiResponse.success("ok");
    }

    @PostMapping("/current")
    public ApiResponse<?> current() {
        return ApiResponse.success(aiPermissionService.currentPermissions());
    }
}
