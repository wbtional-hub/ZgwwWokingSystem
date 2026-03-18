package com.example.lecturesystem.modules.param.controller;

import com.example.lecturesystem.common.ApiResponse;
import com.example.lecturesystem.modules.param.dto.ParamQueryRequest;
import com.example.lecturesystem.modules.param.dto.SaveParamRequest;
import com.example.lecturesystem.modules.param.dto.ToggleParamStatusRequest;
import com.example.lecturesystem.modules.param.service.ParamService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/param")
public class ParamController {
    private final ParamService paramService;

    public ParamController(ParamService paramService) {
        this.paramService = paramService;
    }

    @PostMapping("/list")
    public ApiResponse<?> list(@RequestBody(required = false) ParamQueryRequest request) {
        return ApiResponse.success(paramService.listParams(request == null ? new ParamQueryRequest() : request));
    }

    @PostMapping("/save")
    public ApiResponse<?> save(@Validated @RequestBody SaveParamRequest request) {
        return ApiResponse.success(paramService.saveParam(request));
    }

    @PostMapping("/toggle-status")
    public ApiResponse<?> toggleStatus(@Validated @RequestBody ToggleParamStatusRequest request) {
        paramService.toggleStatus(request);
        return ApiResponse.success("ok");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<?> delete(@PathVariable Long id) {
        paramService.deleteParam(id);
        return ApiResponse.success("ok");
    }
}
