package com.example.lecturesystem.modules.expert.controller;

import com.example.lecturesystem.common.ApiResponse;
import com.example.lecturesystem.modules.expert.dto.ExpertQueryRequest;
import com.example.lecturesystem.modules.expert.dto.SaveSkillOwnerRequest;
import com.example.lecturesystem.modules.expert.service.ExpertService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/expert")
public class ExpertController {
    private final ExpertService expertService;

    public ExpertController(ExpertService expertService) {
        this.expertService = expertService;
    }

    @PostMapping("/list")
    public ApiResponse<?> list(@RequestBody(required = false) ExpertQueryRequest request) {
        return ApiResponse.success(expertService.list(request == null ? new ExpertQueryRequest() : request));
    }

    @PostMapping("/save")
    public ApiResponse<?> save(@Validated @RequestBody SaveSkillOwnerRequest request) {
        expertService.saveSkillOwner(request);
        return ApiResponse.success("ok");
    }
}