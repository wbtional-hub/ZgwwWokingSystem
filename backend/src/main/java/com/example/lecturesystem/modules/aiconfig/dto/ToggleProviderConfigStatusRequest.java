package com.example.lecturesystem.modules.aiconfig.dto;

import jakarta.validation.constraints.NotNull;

public class ToggleProviderConfigStatusRequest {
    @NotNull(message = "接入配置ID不能为空")
    private Long id;

    @NotNull(message = "状态不能为空")
    private Integer status;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}