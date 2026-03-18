package com.example.lecturesystem.modules.weeklywork.dto;

import jakarta.validation.constraints.NotBlank;

public class SaveWeeklyWorkRequest {
    @NotBlank(message = "周次不能为空")
    private String weekNo;
    private String workPlan;
    private String workContent;
    private String remark;

    public String getWeekNo() { return weekNo; }
    public void setWeekNo(String weekNo) { this.weekNo = weekNo; }
    public String getWorkPlan() { return workPlan; }
    public void setWorkPlan(String workPlan) { this.workPlan = workPlan; }
    public String getWorkContent() { return workContent; }
    public void setWorkContent(String workContent) { this.workContent = workContent; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
