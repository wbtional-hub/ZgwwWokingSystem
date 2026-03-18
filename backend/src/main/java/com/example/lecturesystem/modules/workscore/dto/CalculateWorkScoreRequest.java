package com.example.lecturesystem.modules.workscore.dto;

import jakarta.validation.constraints.NotBlank;

public class CalculateWorkScoreRequest {
    @NotBlank(message = "周次不能为空")
    private String weekNo;

    public String getWeekNo() {
        return weekNo;
    }

    public void setWeekNo(String weekNo) {
        this.weekNo = weekNo;
    }
}
