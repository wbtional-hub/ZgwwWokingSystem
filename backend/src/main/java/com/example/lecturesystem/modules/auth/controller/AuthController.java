package com.example.lecturesystem.modules.auth.controller;

import com.example.lecturesystem.common.ApiResponse;
import com.example.lecturesystem.modules.auth.dto.ChangePasswordRequest;
import com.example.lecturesystem.modules.auth.dto.LoginRequest;
import com.example.lecturesystem.modules.auth.dto.WechatMpLoginRequest;
import com.example.lecturesystem.modules.auth.dto.WechatMiniLoginRequest;
import com.example.lecturesystem.modules.auth.security.LoginUser;
import com.example.lecturesystem.modules.auth.service.AuthService;
import com.example.lecturesystem.modules.auth.vo.LoginVO;
import com.example.lecturesystem.modules.auth.vo.MobileLoginOptionsVO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final String FORCE_PASSWORD_CHANGE_MESSAGE = "FORCE_PASSWORD_CHANGE";
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<?> login(@Validated @RequestBody LoginRequest request) {
        return buildLoginResponse(authService.login(request));
    }

    @GetMapping("/mobile-login-options")
    public ApiResponse<MobileLoginOptionsVO> mobileLoginOptions() {
        return ApiResponse.success(authService.getMobileLoginOptions());
    }

    @PostMapping("/change-password")
    public ApiResponse<?> changePassword(@Validated @RequestBody ChangePasswordRequest request) {
        authService.changePassword(request);
        return ApiResponse.success("密码修改成功");
    }

    @PostMapping("/wechat-mini-login")
    public ApiResponse<?> wechatMiniLogin(@Validated @RequestBody WechatMiniLoginRequest request) {
        return buildLoginResponse(authService.wechatMiniLogin(request));
    }

    @PostMapping("/wechat-mp-login")
    public ApiResponse<?> wechatMpLogin(@Validated @RequestBody WechatMpLoginRequest request) {
        return buildLoginResponse(authService.wechatMpLogin(request.getCode()));
    }

    @GetMapping("/wechat-mp-authorize-url")
    public ApiResponse<?> wechatMpAuthorizeUrl(@RequestParam(value = "redirect", required = false) String redirect,
                                               @RequestParam(value = "returnUrl", required = false) String returnUrl,
                                               @RequestParam(value = "state", required = false) String state) {
        String target = firstNonBlank(returnUrl, redirect);
        return ApiResponse.success(authService.buildWechatMpAuthorizeUrl(target, state));
    }

    @GetMapping("/wechat-mp-callback")
    public ResponseEntity<Void> wechatMpCallback(@RequestParam(value = "code", required = false) String code,
                                                 @RequestParam(value = "state", required = false) String state,
                                                 @RequestParam(value = "error", required = false) String error,
                                                 @RequestParam(value = "error_description", required = false) String errorDescription) {
        String redirectUrl = authService.handleWechatMpCallback(code, state, error, errorDescription);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(redirectUrl))
                .build();
    }

    @GetMapping("/me")
    public ApiResponse<?> me(Authentication authentication) {
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        return ApiResponse.success(com.example.lecturesystem.modules.auth.vo.LoginVO.fromLoginUser(loginUser));
    }

    private String firstNonBlank(String primary, String secondary) {
        if (primary != null && !primary.trim().isEmpty()) {
            return primary;
        }
        if (secondary != null && !secondary.trim().isEmpty()) {
            return secondary;
        }
        return null;
    }

    private ApiResponse<LoginVO> buildLoginResponse(LoginVO loginVO) {
        String message = Boolean.TRUE.equals(loginVO.getForcePasswordChange())
                ? FORCE_PASSWORD_CHANGE_MESSAGE
                : "success";
        return new ApiResponse<>(0, message, loginVO);
    }
}
