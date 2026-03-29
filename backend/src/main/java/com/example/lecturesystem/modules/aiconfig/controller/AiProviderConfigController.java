package com.example.lecturesystem.modules.aiconfig.controller;

import com.example.lecturesystem.common.ApiResponse;
import com.example.lecturesystem.modules.aiconfig.dto.ProviderConfigQueryRequest;
import com.example.lecturesystem.modules.aiconfig.dto.SaveProviderConfigRequest;
import com.example.lecturesystem.modules.aiconfig.dto.TestProviderConfigRequest;
import com.example.lecturesystem.modules.aiconfig.dto.ToggleProviderConfigStatusRequest;
import com.example.lecturesystem.modules.aiconfig.service.AiProviderConfigService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/provider")
public class AiProviderConfigController {
    private final AiProviderConfigService aiProviderConfigService;

    public AiProviderConfigController(AiProviderConfigService aiProviderConfigService) {
        this.aiProviderConfigService = aiProviderConfigService;
    }

    @PostMapping("/list")
    public ApiResponse<?> list(@RequestBody(required = false) ProviderConfigQueryRequest request) {
        return ApiResponse.success(aiProviderConfigService.list(request == null ? new ProviderConfigQueryRequest() : request));
    }

    @PostMapping("/save")
    public ApiResponse<?> save(@Validated @RequestBody SaveProviderConfigRequest request) {
        return ApiResponse.success(aiProviderConfigService.save(request));
    }

    @PostMapping("/toggle-status")
    public ApiResponse<?> toggleStatus(@Validated @RequestBody ToggleProviderConfigStatusRequest request) {
        aiProviderConfigService.toggleStatus(request);
        return ApiResponse.success("ok");
    }

    @PostMapping("/test")
    public ApiResponse<?> test(@Validated @RequestBody TestProviderConfigRequest request) {
        return ApiResponse.success(aiProviderConfigService.test(request));
    }
}