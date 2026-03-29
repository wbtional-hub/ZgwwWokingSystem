package com.example.lecturesystem.modules.aiconfig.service;

import com.example.lecturesystem.modules.aiconfig.dto.ProviderConfigQueryRequest;
import com.example.lecturesystem.modules.aiconfig.dto.SaveProviderConfigRequest;
import com.example.lecturesystem.modules.aiconfig.dto.TestProviderConfigRequest;
import com.example.lecturesystem.modules.aiconfig.dto.ToggleProviderConfigStatusRequest;

public interface AiProviderConfigService {
    Object list(ProviderConfigQueryRequest request);
    Long save(SaveProviderConfigRequest request);
    void toggleStatus(ToggleProviderConfigStatusRequest request);
    Object test(TestProviderConfigRequest request);
}