package com.example.lecturesystem.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;

public class WechatMpLoginRequest {
    @NotBlank(message = "公众号授权 code 不能为空")
    private String code;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
