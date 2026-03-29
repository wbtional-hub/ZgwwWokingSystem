package com.example.lecturesystem.modules.aipermission.dto;

public class UserKnowledgePermissionQueryRequest {
    private Long userId;
    private Long baseId;
    private Integer status;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getBaseId() { return baseId; }
    public void setBaseId(Long baseId) { this.baseId = baseId; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}