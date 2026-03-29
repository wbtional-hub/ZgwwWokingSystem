package com.example.lecturesystem.modules.skill.controller;

import com.example.lecturesystem.common.ApiResponse;
import com.example.lecturesystem.modules.skill.dto.*;
import com.example.lecturesystem.modules.skill.service.SkillService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/skill")
public class SkillController {
    private final SkillService skillService;

    public SkillController(SkillService skillService) {
        this.skillService = skillService;
    }

    @PostMapping("/list")
    public ApiResponse<?> list(@RequestBody(required = false) SkillQueryRequest request) {
        return ApiResponse.success(skillService.list(request == null ? new SkillQueryRequest() : request));
    }

    @PostMapping("/save")
    public ApiResponse<?> saveSkill(@Validated @RequestBody SaveSkillRequest request) {
        return ApiResponse.success(skillService.saveSkill(request));
    }

    @PostMapping("/version/save")
    public ApiResponse<?> saveVersion(@Validated @RequestBody SaveSkillVersionRequest request) {
        return ApiResponse.success(skillService.saveSkillVersion(request));
    }

    @PostMapping("/version/publish")
    public ApiResponse<?> publishVersion(@Validated @RequestBody PublishSkillVersionRequest request) {
        skillService.publishSkillVersion(request);
        return ApiResponse.success("ok");
    }

    @GetMapping("/{skillId}/published-version")
    public ApiResponse<?> getPublishedVersion(@PathVariable Long skillId) {
        return ApiResponse.success(skillService.getPublishedVersion(skillId));
    }

    @PostMapping("/binding/save")
    public ApiResponse<?> saveBinding(@Validated @RequestBody SaveSkillBindingRequest request) {
        skillService.saveSkillBinding(request);
        return ApiResponse.success("ok");
    }

    @PostMapping("/test-case/list")
    public ApiResponse<?> listTestCases(@Validated @RequestBody SkillTestCaseQueryRequest request) {
        return ApiResponse.success(skillService.listTestCases(request));
    }

    @PostMapping("/test-case/save")
    public ApiResponse<?> saveTestCase(@Validated @RequestBody SaveSkillTestCaseRequest request) {
        return ApiResponse.success(skillService.saveTestCase(request));
    }

    @PostMapping("/validation/run")
    public ApiResponse<?> runValidation(@Validated @RequestBody RunSkillValidationRequest request) {
        return ApiResponse.success(skillService.runValidation(request));
    }

    @GetMapping("/validation/{runId}")
    public ApiResponse<?> validationDetail(@PathVariable Long runId) {
        return ApiResponse.success(skillService.getValidationDetail(runId));
    }
}