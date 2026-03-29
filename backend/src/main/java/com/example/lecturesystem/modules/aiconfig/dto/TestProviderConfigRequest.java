package com.example.lecturesystem.modules.aiconfig.dto;

import jakarta.validation.constraints.NotNull;

public class TestProviderConfigRequest {
    @NotNull(message = "接入配置ID不能为空")
    private Long id;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
}