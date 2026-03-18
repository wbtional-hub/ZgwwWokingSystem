package com.example.lecturesystem.modules.attendance.dto;

import jakarta.validation.constraints.NotNull;

public class UpdateAttendanceStatusRequest {
    @NotNull(message = "考勤ID不能为空")
    private Long id;

    @NotNull(message = "有效状态不能为空")
    private Integer validFlag;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getValidFlag() {
        return validFlag;
    }

    public void setValidFlag(Integer validFlag) {
        this.validFlag = validFlag;
    }
}
