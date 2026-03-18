package com.example.lecturesystem.modules.weeklywork.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ReviewWeeklyWorkRequest {
    @NotNull(message = "周报ID不能为空")
    private Long id;

    @NotBlank(message = "审核动作不能为空")
    private String action;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }
}
