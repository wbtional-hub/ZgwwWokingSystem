package com.example.lecturesystem.config;

import com.example.lecturesystem.modules.auth.security.JwtAuthenticationFilter;
import com.example.lecturesystem.modules.auth.security.JwtTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class SecurityConfigTest {
    @Test
    void corsShouldAllowProductionDomainAndRequestedLocalOrigins() {
        SecurityConfig securityConfig = new SecurityConfig(new JwtAuthenticationFilter(mock(JwtTokenService.class)));
        CorsConfiguration corsConfiguration = securityConfig.corsConfigurationSource()
                .getCorsConfiguration(request("OPTIONS", SecurityConfig.LOGIN_PATH));

        assertNotNull(corsConfiguration);
        assertAllowed(corsConfiguration, "https://www.xmzgww.com");
        assertAllowed(corsConfiguration, "http://124.220.158.213:9090");
        assertAllowed(corsConfiguration, "http://localhost:5173");
        assertAllowed(corsConfiguration, "http://127.0.0.1:5173");
        assertAllowed(corsConfiguration, "http://localhost:9000");
        assertAllowed(corsConfiguration, "http://127.0.0.1:9000");
        assertAllowed(corsConfiguration, "http://localhost:9090");
        assertAllowed(corsConfiguration, "http://127.0.0.1:9090");
        assertNull(corsConfiguration.checkOrigin("https://forbidden.example.com"));
    }

    @Test
    void jwtFilterShouldSkipOnlyPublicEndpointsAndOptions() {
        TestableJwtAuthenticationFilter filter = new TestableJwtAuthenticationFilter(mock(JwtTokenService.class));

        assertTrue(filter.shouldSkip(request("OPTIONS", "/api/users")));
        assertTrue(filter.shouldSkip(request("POST", SecurityConfig.LOGIN_PATH)));
        assertTrue(filter.shouldSkip(request("GET", SecurityConfig.MOBILE_LOGIN_OPTIONS_PATH)));
        assertTrue(filter.shouldSkip(request("GET", SecurityConfig.WECHAT_MP_AUTHORIZE_URL_PATH)));
        assertTrue(filter.shouldSkip(request("GET", SecurityConfig.WECHAT_MP_CALLBACK_PATH)));
        assertTrue(filter.shouldSkip(request("POST", SecurityConfig.WECHAT_MINI_LOGIN_PATH)));
        assertTrue(filter.shouldSkip(request("POST", SecurityConfig.WECHAT_MP_LOGIN_PATH)));
        assertTrue(filter.shouldSkip(request("GET", SecurityConfig.HEALTH_PATH)));
        assertFalse(filter.shouldSkip(request("GET", "/api/auth/me")));
        assertFalse(filter.shouldSkip(request("GET", "/api/users")));
    }

    private static MockHttpServletRequest request(String method, String path) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setRequestURI(path);
        return request;
    }

    private static void assertAllowed(CorsConfiguration corsConfiguration, String origin) {
        assertEquals(origin, corsConfiguration.checkOrigin(origin));
    }

    private static final class TestableJwtAuthenticationFilter extends JwtAuthenticationFilter {
        private TestableJwtAuthenticationFilter(JwtTokenService jwtTokenService) {
            super(jwtTokenService);
        }

        private boolean shouldSkip(MockHttpServletRequest request) {
            return shouldNotFilter(request);
        }
    }
}
