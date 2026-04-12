package com.example.lecturesystem.modules.auth.service;

import com.example.lecturesystem.modules.auth.dto.ChangePasswordRequest;
import com.example.lecturesystem.modules.auth.dto.LoginRequest;
import com.example.lecturesystem.modules.auth.dto.WechatMiniLoginRequest;
import com.example.lecturesystem.modules.auth.vo.LoginVO;
import com.example.lecturesystem.modules.auth.vo.MobileLoginOptionsVO;
import com.example.lecturesystem.modules.auth.vo.WechatMpAuthorizeUrlVO;

public interface AuthService {
    LoginVO login(LoginRequest request);

    default MobileLoginOptionsVO getMobileLoginOptions() {
        throw new UnsupportedOperationException("Not implemented");
    }

    LoginVO wechatMiniLogin(WechatMiniLoginRequest request);

    default LoginVO wechatMpLogin(String code) {
        throw new UnsupportedOperationException("Not implemented");
    }

    default WechatMpAuthorizeUrlVO buildWechatMpAuthorizeUrl(String returnUrl, String state) {
        throw new UnsupportedOperationException("Not implemented");
    }

    default String handleWechatMpCallback(String code, String state, String error, String errorDescription) {
        throw new UnsupportedOperationException("Not implemented");
    }

    default void changePassword(ChangePasswordRequest request) {
        throw new UnsupportedOperationException("Not implemented");
    }

    WechatMiniIdentity exchangeWechatMiniCode(String code);

    record WechatMiniIdentity(String openId, String unionId) {
    }
}
