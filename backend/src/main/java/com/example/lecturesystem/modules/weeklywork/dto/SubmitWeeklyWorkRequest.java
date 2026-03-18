package com.example.lecturesystem.modules.weeklywork.dto;

import jakarta.validation.constraints.NotNull;

public class SubmitWeeklyWorkRequest {
    @NotNull(message = "周工作ID不能为空")
    private Long id;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
}
