package com.example.lecturesystem.modules.config.controller;

import com.example.lecturesystem.common.ApiResponse;
import com.example.lecturesystem.modules.param.service.ParamService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/config")
public class ConfigController {
    private final ParamService paramService;

    public ConfigController(ParamService paramService) {
        this.paramService = paramService;
    }

    @GetMapping("/amap")
    public ApiResponse<?> queryAmapConfig() {
        return ApiResponse.success(paramService.queryAmapConfig());
    }
}
