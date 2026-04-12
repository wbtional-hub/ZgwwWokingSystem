package com.example.lecturesystem.modules.auth.service.impl;

import com.example.lecturesystem.modules.auth.config.WechatProperties;
import com.example.lecturesystem.modules.auth.vo.WechatJsapiConfigVO;
import com.example.lecturesystem.modules.param.service.ParamService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WechatJsapiServiceImplTest {

    @Test
    void queryJsapiConfigShouldReturnSignedConfigAndReuseCache() {
        ParamService paramService = mock(ParamService.class);
        when(paramService.getByCode("WECHAT_MP_ENABLED")).thenReturn("true");
        when(paramService.getByCode("WECHAT_MP_APP_ID")).thenReturn("wx-demo-app");
        when(paramService.getByCode("WECHAT_MP_APP_SECRET")).thenReturn("demo-secret");
        when(paramService.getByCode("WECHAT_JSAPI_ENABLED")).thenReturn("true");
        when(paramService.getByCode("WECHAT_JSAPI_SIGNATURE_ENABLED")).thenReturn("true");
        when(paramService.getByCode("WECHAT_JSAPI_LOCATION_ENABLED")).thenReturn("true");
        when(paramService.getByCode("WECHAT_JSAPI_ALLOWED_DOMAINS")).thenReturn("xmzgww.com");
        when(paramService.getByCode("WECHAT_JSAPI_LOCATION_PRIORITY")).thenReturn("WECHAT_FIRST");
        when(paramService.getByCode("WECHAT_JSAPI_LOCATION_TYPE")).thenReturn("gcj02");
        when(paramService.getByCode("WECHAT_JSAPI_NON_WECHAT_FALLBACK")).thenReturn("BROWSER");
        when(paramService.getByCode("WECHAT_JSAPI_ACCURACY_THRESHOLD")).thenReturn("60");
        when(paramService.getByCode("WECHAT_JSAPI_TIMEOUT_MS")).thenReturn("5000");

        TestWechatJsapiServiceImpl service = new TestWechatJsapiServiceImpl(new WechatProperties(), paramService);

        WechatJsapiConfigVO first = service.queryJsapiConfig("https://www.xmzgww.com/mobile/attendance?tab=checkin#hash");
        WechatJsapiConfigVO second = service.queryJsapiConfig("https://www.xmzgww.com/mobile/attendance?tab=checkin");

        assertTrue(first.isEnabled());
        assertTrue(first.isLocationEnabled());
        assertEquals("wx-demo-app", first.getAppId());
        assertEquals("gcj02", first.getLocationType());
        assertEquals("WECHAT_FIRST", first.getPriority());
        assertEquals("BROWSER", first.getFallback());
        assertEquals(60, first.getAccuracyThreshold());
        assertEquals(5000, first.getTimeoutMs());
        assertEquals(2, first.getJsApiList().size());
        assertNotNull(first.getSignature());
        assertNotNull(first.getNonceStr());
        assertNotNull(first.getTimestamp());

        assertTrue(second.isEnabled());
        assertEquals(1, service.accessTokenRequestCount);
        assertEquals(1, service.ticketRequestCount);
    }

    @Test
    void queryJsapiConfigShouldFailWhenDomainNotAllowed() {
        ParamService paramService = mock(ParamService.class);
        when(paramService.getByCode("WECHAT_MP_ENABLED")).thenReturn("true");
        when(paramService.getByCode("WECHAT_MP_APP_ID")).thenReturn("wx-demo-app");
        when(paramService.getByCode("WECHAT_MP_APP_SECRET")).thenReturn("demo-secret");
        when(paramService.getByCode("WECHAT_JSAPI_ENABLED")).thenReturn("true");
        when(paramService.getByCode("WECHAT_JSAPI_SIGNATURE_ENABLED")).thenReturn("true");
        when(paramService.getByCode("WECHAT_JSAPI_LOCATION_ENABLED")).thenReturn("true");
        when(paramService.getByCode("WECHAT_JSAPI_ALLOWED_DOMAINS")).thenReturn("wechat.example.com");

        TestWechatJsapiServiceImpl service = new TestWechatJsapiServiceImpl(new WechatProperties(), paramService);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.queryJsapiConfig("https://www.xmzgww.com/mobile/attendance"));

        assertEquals("当前页面域名不在微信 JSAPI 白名单内: www.xmzgww.com", exception.getMessage());
        assertEquals(0, service.accessTokenRequestCount);
        assertEquals(0, service.ticketRequestCount);
    }

    @Test
    void queryJsapiConfigShouldRemainDisabledWhenJsapiSwitchOff() {
        ParamService paramService = mock(ParamService.class);
        when(paramService.getByCode("WECHAT_MP_ENABLED")).thenReturn("true");
        when(paramService.getByCode("WECHAT_MP_APP_ID")).thenReturn("wx-demo-app");
        when(paramService.getByCode("WECHAT_MP_APP_SECRET")).thenReturn("demo-secret");
        when(paramService.getByCode("WECHAT_JSAPI_ENABLED")).thenReturn("false");

        TestWechatJsapiServiceImpl service = new TestWechatJsapiServiceImpl(new WechatProperties(), paramService);
        WechatJsapiConfigVO result = service.queryJsapiConfig("https://www.xmzgww.com/mobile/attendance");

        assertFalse(result.isEnabled());
        assertFalse(result.isLocationEnabled());
        assertEquals("wx-demo-app", result.getAppId());
        assertEquals(0, service.accessTokenRequestCount);
        assertEquals(0, service.ticketRequestCount);
    }

    private static class TestWechatJsapiServiceImpl extends WechatJsapiServiceImpl {
        private int accessTokenRequestCount;
        private int ticketRequestCount;

        TestWechatJsapiServiceImpl(WechatProperties wechatProperties, ParamService paramService) {
            super(wechatProperties, paramService);
        }

        @Override
        protected WechatAccessTokenResponse requestAccessToken(String appId, String appSecret) {
            accessTokenRequestCount += 1;
            WechatAccessTokenResponse response = new WechatAccessTokenResponse();
            response.setAccessToken("access-token-demo");
            response.setExpiresIn(7200);
            return response;
        }

        @Override
        protected WechatJsapiTicketResponse requestJsapiTicket(String accessToken) {
            ticketRequestCount += 1;
            WechatJsapiTicketResponse response = new WechatJsapiTicketResponse();
            response.setTicket("ticket-demo");
            response.setExpiresIn(7200);
            return response;
        }
    }
}
