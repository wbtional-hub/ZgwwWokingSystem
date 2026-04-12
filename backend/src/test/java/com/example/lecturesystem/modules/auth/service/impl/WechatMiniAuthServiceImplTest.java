package com.example.lecturesystem.modules.auth.service.impl;

import com.example.lecturesystem.modules.auth.config.WechatProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WechatMiniAuthServiceImplTest {
    @Test
    void exchangeCodeShouldRejectDisabledMiniLogin() {
        WechatMiniAuthServiceImpl service = new WechatMiniAuthServiceImpl(new WechatProperties());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.exchangeCode("demo-code"));

        assertEquals("未启用小程序登录", ex.getMessage());
    }
}
