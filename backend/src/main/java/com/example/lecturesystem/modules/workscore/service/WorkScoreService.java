package com.example.lecturesystem.modules.workscore.service;

import com.example.lecturesystem.modules.workscore.dto.CalculateWorkScoreRequest;
import com.example.lecturesystem.modules.workscore.dto.WorkScoreQueryRequest;

public interface WorkScoreService {
    Object query(WorkScoreQueryRequest request);
    Object detail(Long id);
    Object calculate(CalculateWorkScoreRequest request);
    byte[] exportReport(String weekNo);
}
