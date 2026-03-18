package com.example.lecturesystem.modules.workscore.controller;

import com.example.lecturesystem.common.ApiResponse;
import com.example.lecturesystem.modules.workscore.dto.CalculateWorkScoreRequest;
import com.example.lecturesystem.modules.workscore.dto.WorkScoreQueryRequest;
import com.example.lecturesystem.modules.workscore.service.WorkScoreService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping({"/api/work-score", "/api/score"})
public class WorkScoreController {
    private final WorkScoreService workScoreService;

    public WorkScoreController(WorkScoreService workScoreService) {
        this.workScoreService = workScoreService;
    }

    @PostMapping("/query")
    public ApiResponse<?> query(@RequestBody WorkScoreQueryRequest request) {
        return ApiResponse.success(workScoreService.query(request));
    }

    @PostMapping("/calculate")
    public ApiResponse<?> calculate(@Validated @RequestBody CalculateWorkScoreRequest request) {
        return ApiResponse.success(workScoreService.calculate(request));
    }

    @GetMapping("/detail/{id}")
    public ApiResponse<?> detail(@PathVariable Long id) {
        return ApiResponse.success(workScoreService.detail(id));
    }

    @GetMapping("/report/export")
    public ResponseEntity<byte[]> exportReport(@RequestParam String weekNo) {
        byte[] bytes = workScoreService.exportReport(weekNo);
        String fileName = URLEncoder.encode("weekly-score-report-" + weekNo + ".docx", StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + fileName)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .body(bytes);
    }
}
