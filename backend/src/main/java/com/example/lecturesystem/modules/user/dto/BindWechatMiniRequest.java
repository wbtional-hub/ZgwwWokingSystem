package com.example.lecturesystem.modules.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class BindWechatMiniRequest {
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotBlank(message = "微信授权 code 不能为空")
    private String code;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
