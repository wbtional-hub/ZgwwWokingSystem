package com.example.lecturesystem.modules.auth.controller;

import com.example.lecturesystem.common.ApiResponse;
import com.example.lecturesystem.modules.auth.dto.QrLoginConfirmRequest;
import com.example.lecturesystem.modules.auth.security.LoginUser;
import com.example.lecturesystem.modules.auth.service.QrLoginService;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/qr-login")
public class QrLoginController {
    private final QrLoginService qrLoginService;

    public QrLoginController(QrLoginService qrLoginService) {
        this.qrLoginService = qrLoginService;
    }

    @PostMapping("/session")
    public ApiResponse<?> createSession() {
        return ApiResponse.success(qrLoginService.createSession());
    }

    @GetMapping("/status")
    public ApiResponse<?> getStatus(@RequestParam("qrToken") String qrToken) {
        return ApiResponse.success(qrLoginService.getSessionStatus(qrToken));
    }

    @GetMapping("/mobile-session")
    public ApiResponse<?> getMobileSession(@RequestParam("qrToken") String qrToken,
                                           Authentication authentication) {
        currentLoginUser(authentication);
        return ApiResponse.success(qrLoginService.getMobileSession(qrToken));
    }

    @PostMapping("/confirm")
    public ApiResponse<?> confirm(@Validated @RequestBody QrLoginConfirmRequest request,
                                  Authentication authentication) {
        LoginUser loginUser = currentLoginUser(authentication);
        return ApiResponse.success(qrLoginService.confirmSession(request.getQrToken(), loginUser.getUserId(), Boolean.TRUE.equals(request.getApprove())));
    }

    private LoginUser currentLoginUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof LoginUser loginUser)) {
            throw new IllegalArgumentException("当前未登录");
        }
        return loginUser;
    }
}
