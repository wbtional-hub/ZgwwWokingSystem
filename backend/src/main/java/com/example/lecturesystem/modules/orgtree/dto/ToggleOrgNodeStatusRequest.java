package com.example.lecturesystem.modules.orgtree.dto;

import jakarta.validation.constraints.NotNull;

public class ToggleOrgNodeStatusRequest {
    @NotNull(message = "节点ID不能为空")
    private Long userId;

    @NotNull(message = "状态不能为空")
    private Integer status;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
