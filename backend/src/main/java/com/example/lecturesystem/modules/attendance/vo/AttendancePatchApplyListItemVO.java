package com.example.lecturesystem.modules.attendance.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class AttendancePatchApplyListItemVO {
    private Long id;
    private Long userId;
    private Long unitId;
    private LocalDate attendanceDate;
    private String patchType;
    private String applyType;
    private LocalDateTime patchTime;
    private String reason;
    private String attachmentsJson;
    private String status;
    private Long approveUserId;
    private LocalDateTime approveTime;
    private String approveComment;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer validFlag;
    private String username;
    private String realName;
    private String unitName;
    private String approveUsername;
    private String approveRealName;
    private String applyTypeText;
    private Boolean canApprove;
    private Boolean readonlyMode;
    private String currentNodeCode;
    private String currentNodeName;
    private Long currentApproverUserId;
    private String currentApproverName;
    private String latestActionType;
    private String latestActionComment;
    private LocalDateTime latestActionTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getUnitId() { return unitId; }
    public void setUnitId(Long unitId) { this.unitId = unitId; }
    public LocalDate getAttendanceDate() { return attendanceDate; }
    public void setAttendanceDate(LocalDate attendanceDate) { this.attendanceDate = attendanceDate; }
    public String getPatchType() { return patchType; }
    public void setPatchType(String patchType) { this.patchType = patchType; }
    public String getApplyType() { return applyType; }
    public void setApplyType(String applyType) { this.applyType = applyType; }
    public LocalDateTime getPatchTime() { return patchTime; }
    public void setPatchTime(LocalDateTime patchTime) { this.patchTime = patchTime; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getAttachmentsJson() { return attachmentsJson; }
    public void setAttachmentsJson(String attachmentsJson) { this.attachmentsJson = attachmentsJson; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getApproveUserId() { return approveUserId; }
    public void setApproveUserId(Long approveUserId) { this.approveUserId = approveUserId; }
    public LocalDateTime getApproveTime() { return approveTime; }
    public void setApproveTime(LocalDateTime approveTime) { this.approveTime = approveTime; }
    public String getApproveComment() { return approveComment; }
    public void setApproveComment(String approveComment) { this.approveComment = approveComment; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    public Integer getValidFlag() { return validFlag; }
    public void setValidFlag(Integer validFlag) { this.validFlag = validFlag; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getRealName() { return realName; }
    public void setRealName(String realName) { this.realName = realName; }
    public String getUnitName() { return unitName; }
    public void setUnitName(String unitName) { this.unitName = unitName; }
    public String getApproveUsername() { return approveUsername; }
    public void setApproveUsername(String approveUsername) { this.approveUsername = approveUsername; }
    public String getApproveRealName() { return approveRealName; }
    public void setApproveRealName(String approveRealName) { this.approveRealName = approveRealName; }
    public String getApplyTypeText() { return applyTypeText; }
    public void setApplyTypeText(String applyTypeText) { this.applyTypeText = applyTypeText; }
    public Boolean getCanApprove() { return canApprove; }
    public void setCanApprove(Boolean canApprove) { this.canApprove = canApprove; }
    public Boolean getReadonlyMode() { return readonlyMode; }
    public void setReadonlyMode(Boolean readonlyMode) { this.readonlyMode = readonlyMode; }
    public String getCurrentNodeCode() { return currentNodeCode; }
    public void setCurrentNodeCode(String currentNodeCode) { this.currentNodeCode = currentNodeCode; }
    public String getCurrentNodeName() { return currentNodeName; }
    public void setCurrentNodeName(String currentNodeName) { this.currentNodeName = currentNodeName; }
    public Long getCurrentApproverUserId() { return currentApproverUserId; }
    public void setCurrentApproverUserId(Long currentApproverUserId) { this.currentApproverUserId = currentApproverUserId; }
    public String getCurrentApproverName() { return currentApproverName; }
    public void setCurrentApproverName(String currentApproverName) { this.currentApproverName = currentApproverName; }
    public String getLatestActionType() { return latestActionType; }
    public void setLatestActionType(String latestActionType) { this.latestActionType = latestActionType; }
    public String getLatestActionComment() { return latestActionComment; }
    public void setLatestActionComment(String latestActionComment) { this.latestActionComment = latestActionComment; }
    public LocalDateTime getLatestActionTime() { return latestActionTime; }
    public void setLatestActionTime(LocalDateTime latestActionTime) { this.latestActionTime = latestActionTime; }
}
