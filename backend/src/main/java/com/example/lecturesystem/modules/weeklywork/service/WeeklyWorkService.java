package com.example.lecturesystem.modules.weeklywork.service;

import com.example.lecturesystem.modules.weeklywork.dto.ReviewWeeklyWorkRequest;
import com.example.lecturesystem.modules.weeklywork.dto.SaveWeeklyWorkRequest;
import com.example.lecturesystem.modules.weeklywork.dto.SubmitWeeklyWorkRequest;
import com.example.lecturesystem.modules.weeklywork.dto.WeeklyWorkQueryRequest;

public interface WeeklyWorkService {
    Long saveDraft(SaveWeeklyWorkRequest request);
    Object submit(SubmitWeeklyWorkRequest request);
    Object query(WeeklyWorkQueryRequest request);
    Object detail(Long id);
    Object review(ReviewWeeklyWorkRequest request);
}
