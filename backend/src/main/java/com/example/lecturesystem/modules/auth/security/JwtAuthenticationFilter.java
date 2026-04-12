package com.example.lecturesystem.modules.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

import static com.example.lecturesystem.config.SecurityConfig.HEALTH_PATH;
import static com.example.lecturesystem.config.SecurityConfig.LOGIN_PATH;
import static com.example.lecturesystem.config.SecurityConfig.MOBILE_LOGIN_OPTIONS_PATH;
import static com.example.lecturesystem.config.SecurityConfig.WECHAT_MINI_LOGIN_PATH;
import static com.example.lecturesystem.config.SecurityConfig.WECHAT_JSAPI_CONFIG_PATH;
import static com.example.lecturesystem.config.SecurityConfig.WECHAT_MP_AUTHORIZE_URL_PATH;
import static com.example.lecturesystem.config.SecurityConfig.WECHAT_MP_CALLBACK_PATH;
import static com.example.lecturesystem.config.SecurityConfig.WECHAT_MP_LOGIN_PATH;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final Set<String> PUBLIC_PATHS = Set.of(
            LOGIN_PATH,
            MOBILE_LOGIN_OPTIONS_PATH,
            WECHAT_MINI_LOGIN_PATH,
            WECHAT_MP_AUTHORIZE_URL_PATH,
            WECHAT_MP_CALLBACK_PATH,
            WECHAT_MP_LOGIN_PATH,
            WECHAT_JSAPI_CONFIG_PATH,
            HEALTH_PATH
    );

    private final JwtTokenService jwtTokenService;

    public JwtAuthenticationFilter(JwtTokenService jwtTokenService) {
        this.jwtTokenService = jwtTokenService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }
        return PUBLIC_PATHS.contains(resolvePath(request));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                LoginUser loginUser = jwtTokenService.parseToken(token);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                loginUser,
                                null,
                                AuthorityUtils.createAuthorityList(loginUser.isAdmin() ? "ROLE_ADMIN" : "ROLE_USER")
                        );
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception ex) {
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }

    private String resolvePath(HttpServletRequest request) {
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (StringUtils.hasText(contextPath) && path.startsWith(contextPath)) {
            return path.substring(contextPath.length());
        }
        return path;
    }
}
