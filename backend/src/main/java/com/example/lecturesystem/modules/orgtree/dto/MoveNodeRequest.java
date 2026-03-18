package com.example.lecturesystem.modules.orgtree.dto;

import jakarta.validation.constraints.NotNull;

public class MoveNodeRequest {
    @NotNull(message = "当前节点不能为空")
    private Long userId;
    @NotNull(message = "目标上级不能为空")
    private Long targetParentUserId;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getTargetParentUserId() { return targetParentUserId; }
    public void setTargetParentUserId(Long targetParentUserId) { this.targetParentUserId = targetParentUserId; }
}
