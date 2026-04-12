package com.example.lecturesystem.modules.auth.service.impl;

import com.example.lecturesystem.modules.auth.config.WechatProperties;
import com.example.lecturesystem.modules.auth.service.WechatMpAuthService;
import com.example.lecturesystem.modules.auth.vo.LoginVO;
import com.example.lecturesystem.modules.auth.vo.WechatMpAuthorizeUrlVO;
import com.example.lecturesystem.modules.param.service.ParamService;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class WechatMpAuthServiceImplTest {

    @Test
    void buildAuthorizeUrlShouldUseConfiguredScopeAndState() {
        WechatMpAuthServiceImpl service = createService(enabledWechatProperties());

        WechatMpAuthorizeUrlVO result = service.buildAuthorizeUrl("/weekly-work?tab=1", "login");

        assertTrue(result.isEnabled());
        assertEquals("snsapi_base", result.getAuthorizeScope());
        assertTrue(result.getAuthorizeUrl().startsWith("https://open.weixin.qq.com/connect/oauth2/authorize"));

        URI uri = URI.create(result.getAuthorizeUrl().replace("#wechat_redirect", ""));
        String state = extractQueryParam(uri, "state");
        WechatMpAuthService.WechatMpCallbackState callbackState = service.parseCallbackState(state);
        assertEquals("/weekly-work?tab=1", callbackState.returnUrl());
        assertEquals("login", callbackState.requestState());
    }

    @Test
    void buildAuthorizeUrlShouldRejectDisabledMpLogin() {
        WechatProperties properties = new WechatProperties();
        WechatMpAuthServiceImpl service = createService(properties);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.buildAuthorizeUrl("/mobile-workspace", "login"));

        assertTrue(ex.getMessage().contains("wechat-mp-authorize-url"));
        assertTrue(ex.getMessage().contains("enabled=false"));
        assertTrue(ex.getMessage().contains("missing=[enabled, appId, appSecret]"));
    }

    @Test
    void callbackRedirectBuildersShouldReturnFrontendLoginUrl() {
        WechatMpAuthServiceImpl service = createService(enabledWechatProperties());
        WechatMpAuthService.WechatMpCallbackState callbackState = service.parseCallbackState(
                URI.create(service.buildAuthorizeUrl("/attendance", "login").getAuthorizeUrl().replace("#wechat_redirect", ""))
                        .getQuery()
                        .replaceAll(".*state=([^&]+).*", "$1")
        );

        LoginVO loginVO = new LoginVO();
        loginVO.setToken("token-123");
        loginVO.setUserId(1L);
        loginVO.setUsername("admin");

        String successRedirect = service.buildSuccessCallbackRedirect(loginVO, callbackState);
        String failureRedirect = service.buildFailureCallbackRedirect(
                callbackState,
                WechatMpAuthService.CALLBACK_ERROR_CODE_UNBOUND,
                "当前微信未绑定系统账号"
        );

        assertTrue(successRedirect.startsWith("https://www.xmzgww.com/login"));
        assertTrue(successRedirect.contains("redirect=/attendance") || successRedirect.contains("redirect=%2Fattendance"));
        assertTrue(successRedirect.contains("#wechatLogin="));

        assertTrue(failureRedirect.startsWith("https://www.xmzgww.com/login"));
        assertTrue(failureRedirect.contains("wechatAuthFailed=1"));
        assertTrue(failureRedirect.contains("wechatAuthCode=WECHAT_MP_UNBOUND"));
        assertTrue(failureRedirect.contains("redirect=/attendance") || failureRedirect.contains("redirect=%2Fattendance"));
    }

    private static WechatMpAuthServiceImpl createService(WechatProperties properties) {
        ParamService paramService = mock(ParamService.class);
        return new WechatMpAuthServiceImpl(properties, paramService);
    }

    private static WechatProperties enabledWechatProperties() {
        WechatProperties properties = new WechatProperties();
        properties.getMp().setEnabled(true);
        properties.getMp().setAppId("wx-demo-app");
        properties.getMp().setAppSecret("demo-secret");
        properties.getMp().setOauthRedirectUri("https://www.xmzgww.com/api/auth/wechat-mp-callback");
        properties.getMp().setAuthorizeScope("snsapi_base");
        return properties;
    }

    private static String extractQueryParam(URI uri, String key) {
        String[] pairs = uri.getQuery().split("&");
        for (String pair : pairs) {
            int index = pair.indexOf('=');
            if (index > 0 && key.equals(pair.substring(0, index))) {
                return URLDecoder.decode(pair.substring(index + 1), StandardCharsets.UTF_8);
            }
        }
        return "";
    }
}
