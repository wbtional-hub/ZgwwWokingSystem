package com.example.lecturesystem.modules.auth.controller;

import com.example.lecturesystem.common.ApiResponse;
import com.example.lecturesystem.modules.auth.dto.LoginRequest;
import com.example.lecturesystem.modules.auth.security.LoginUser;
import com.example.lecturesystem.modules.auth.service.AuthService;
import com.example.lecturesystem.modules.auth.vo.LoginVO;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<?> login(@Validated @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @GetMapping("/me")
    public ApiResponse<?> me(Authentication authentication) {
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        return ApiResponse.success(LoginVO.fromLoginUser(loginUser));
    }
}
