package com.example.lecturesystem.modules.weeklywork.controller;

import com.example.lecturesystem.common.ApiResponse;
import com.example.lecturesystem.modules.weeklywork.dto.ReviewWeeklyWorkRequest;
import com.example.lecturesystem.modules.weeklywork.dto.SaveWeeklyWorkRequest;
import com.example.lecturesystem.modules.weeklywork.dto.SubmitWeeklyWorkRequest;
import com.example.lecturesystem.modules.weeklywork.dto.WeeklyWorkQueryRequest;
import com.example.lecturesystem.modules.weeklywork.service.WeeklyWorkService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/weekly-work")
public class WeeklyWorkController {
    private final WeeklyWorkService weeklyWorkService;

    public WeeklyWorkController(WeeklyWorkService weeklyWorkService) {
        this.weeklyWorkService = weeklyWorkService;
    }

    @PostMapping("/save-draft")
    public ApiResponse<?> saveDraft(@Validated @RequestBody SaveWeeklyWorkRequest request) {
        return ApiResponse.success(weeklyWorkService.saveDraft(request));
    }

    @PostMapping("/submit")
    public ApiResponse<?> submit(@Validated @RequestBody SubmitWeeklyWorkRequest request) {
        return ApiResponse.success(weeklyWorkService.submit(request));
    }

    @PostMapping("/query")
    public ApiResponse<?> query(@RequestBody WeeklyWorkQueryRequest request) {
        return ApiResponse.success(weeklyWorkService.query(request));
    }

    @GetMapping("/detail/{id}")
    public ApiResponse<?> detail(@PathVariable Long id) {
        return ApiResponse.success(weeklyWorkService.detail(id));
    }

    @PostMapping("/review")
    public ApiResponse<?> review(@Validated @RequestBody ReviewWeeklyWorkRequest request) {
        return ApiResponse.success(weeklyWorkService.review(request));
    }
}
