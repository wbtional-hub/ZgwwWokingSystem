package com.example.lecturesystem.modules.weeklywork.entity;

import java.time.LocalDateTime;

public class WeeklyWorkEntity {
    private Long id;
    private Long unitId;
    private Long userId;
    private String weekNo;
    private String status;
    private String workPlan;
    private String workContent;
    private String remark;
    private LocalDateTime submitTime;
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUnitId() { return unitId; }
    public void setUnitId(Long unitId) { this.unitId = unitId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getWeekNo() { return weekNo; }
    public void setWeekNo(String weekNo) { this.weekNo = weekNo; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getWorkPlan() { return workPlan; }
    public void setWorkPlan(String workPlan) { this.workPlan = workPlan; }
    public String getWorkContent() { return workContent; }
    public void setWorkContent(String workContent) { this.workContent = workContent; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public LocalDateTime getSubmitTime() { return submitTime; }
    public void setSubmitTime(LocalDateTime submitTime) { this.submitTime = submitTime; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
