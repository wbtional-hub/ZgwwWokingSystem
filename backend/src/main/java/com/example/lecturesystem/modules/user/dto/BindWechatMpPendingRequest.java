package com.example.lecturesystem.modules.user.dto;

import jakarta.validation.constraints.NotNull;

public class BindWechatMpPendingRequest {

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotNull(message = "待绑定记录ID不能为空")
    private Long pendingBindId;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getPendingBindId() {
        return pendingBindId;
    }

    public void setPendingBindId(Long pendingBindId) {
        this.pendingBindId = pendingBindId;
    }
}