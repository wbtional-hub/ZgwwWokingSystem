package com.example.lecturesystem.modules.auth.service;

public interface WechatMiniAuthService {
    AuthService.WechatMiniIdentity exchangeCode(String code);
}
