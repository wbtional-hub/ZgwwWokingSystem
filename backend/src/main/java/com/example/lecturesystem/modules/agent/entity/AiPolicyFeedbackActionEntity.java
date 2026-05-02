package com.example.lecturesystem.modules.agent.entity;

import java.time.OffsetDateTime;

public class AiPolicyFeedbackActionEntity {
    private Long id;
    private Long taskId;
    private String actionType;
    private String actionContent;
    private String draftType;
    private String draftPayload;
    private String targetTable;
    private Long targetId;
    private Long operatorId;
    private OffsetDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }
    public String getActionContent() { return actionContent; }
    public void setActionContent(String actionContent) { this.actionContent = actionContent; }
    public String getDraftType() { return draftType; }
    public void setDraftType(String draftType) { this.draftType = draftType; }
    public String getDraftPayload() { return draftPayload; }
    public void setDraftPayload(String draftPayload) { this.draftPayload = draftPayload; }
    public String getTargetTable() { return targetTable; }
    public void setTargetTable(String targetTable) { this.targetTable = targetTable; }
    public Long getTargetId() { return targetId; }
    public void setTargetId(Long targetId) { this.targetId = targetId; }
    public Long getOperatorId() { return operatorId; }
    public void setOperatorId(Long operatorId) { this.operatorId = operatorId; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
