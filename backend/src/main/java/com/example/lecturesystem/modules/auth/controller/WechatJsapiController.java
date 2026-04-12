package com.example.lecturesystem.modules.auth.controller;

import com.example.lecturesystem.common.ApiResponse;
import com.example.lecturesystem.modules.auth.service.WechatJsapiService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wechat")
public class WechatJsapiController {
    private final WechatJsapiService wechatJsapiService;

    public WechatJsapiController(WechatJsapiService wechatJsapiService) {
        this.wechatJsapiService = wechatJsapiService;
    }

    @GetMapping("/jsapi-config")
    public ApiResponse<?> queryJsapiConfig(@RequestParam("url") String url) {
        return ApiResponse.success(wechatJsapiService.queryJsapiConfig(url));
    }
}
