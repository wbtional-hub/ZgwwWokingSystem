package com.example.lecturesystem.modules.usermodule.dto;

import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;

public class SaveUserModulePermissionRequest {
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    private List<String> moduleCodes = new ArrayList<>();

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public List<String> getModuleCodes() {
        return moduleCodes;
    }

    public void setModuleCodes(List<String> moduleCodes) {
        this.moduleCodes = moduleCodes;
    }
}
