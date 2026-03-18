package com.example.lecturesystem.modules.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class UpdateUserRequest {
    @NotNull(message = "用户ID不能为空")
    private Long id;
    @NotBlank(message = "姓名不能为空")
    private String realName;
    private String jobTitle;
    private String mobile;
    @NotNull(message = "状态不能为空")
    private Integer status;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRealName() { return realName; }
    public void setRealName(String realName) { this.realName = realName; }
    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }
    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}
