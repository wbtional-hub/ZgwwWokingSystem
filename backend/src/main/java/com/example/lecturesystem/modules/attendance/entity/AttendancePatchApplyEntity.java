package com.example.lecturesystem.modules.attendance.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class AttendancePatchApplyEntity {
    private Long id;
    private Long userId;
    private Long unitId;
    private LocalDate attendanceDate;
    private String patchType;
    private LocalDateTime patchTime;
    private String reason;
    private String status;
    private Long approveUserId;
    private LocalDateTime approveTime;
    private String approveComment;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer validFlag;

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
    public LocalDateTime getPatchTime() { return patchTime; }
    public void setPatchTime(LocalDateTime patchTime) { this.patchTime = patchTime; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
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
}
