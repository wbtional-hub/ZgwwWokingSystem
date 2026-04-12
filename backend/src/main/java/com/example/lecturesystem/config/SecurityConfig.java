package com.example.lecturesystem.config;

import com.example.lecturesystem.modules.auth.config.AuthProperties;
import com.example.lecturesystem.modules.auth.config.WechatProperties;
import com.example.lecturesystem.modules.auth.security.JwtAuthenticationFilter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableConfigurationProperties({AuthProperties.class, WechatProperties.class})
public class SecurityConfig {
    public static final String LOGIN_PATH = "/api/auth/login";
    public static final String MOBILE_LOGIN_OPTIONS_PATH = "/api/auth/mobile-login-options";
    public static final String WECHAT_MP_AUTHORIZE_URL_PATH = "/api/auth/wechat-mp-authorize-url";
    public static final String WECHAT_MP_CALLBACK_PATH = "/api/auth/wechat-mp-callback";
    public static final String WECHAT_MINI_LOGIN_PATH = "/api/auth/wechat-mini-login";
    public static final String WECHAT_MP_LOGIN_PATH = "/api/auth/wechat-mp-login";
    public static final String QR_LOGIN_SESSION_PATH = "/api/auth/qr-login/session";
    public static final String QR_LOGIN_STATUS_PATH = "/api/auth/qr-login/status";
    public static final String HEALTH_PATH = "/api/health";
    public static final String WECHAT_JSAPI_CONFIG_PATH = "/api/wechat/jsapi-config";
    public static final String LOG_CENTER_REPORT_PATH = "/api/log-center/report";

    public static final List<String> ALLOWED_ORIGINS = List.of(
            "https://www.xmzgww.com",
            "https://xmzgww.com",
            "http://124.220.158.213:9090",
            "http://localhost:5173",
            "http://127.0.0.1:5173",
            "http://localhost:9000",
            "http://127.0.0.1:9000",
            "http://localhost:9080",
            "http://127.0.0.1:9080",
            "http://localhost:9090",
            "http://127.0.0.1:9090"
    );

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.POST, LOGIN_PATH).permitAll()
                        .requestMatchers(HttpMethod.GET, MOBILE_LOGIN_OPTIONS_PATH).permitAll()
                        .requestMatchers(HttpMethod.POST, WECHAT_MINI_LOGIN_PATH).permitAll()
                        .requestMatchers(HttpMethod.POST, WECHAT_MP_LOGIN_PATH).permitAll()
                        .requestMatchers(HttpMethod.POST, QR_LOGIN_SESSION_PATH).permitAll()
                        .requestMatchers(HttpMethod.GET, WECHAT_MP_AUTHORIZE_URL_PATH).permitAll()
                        .requestMatchers(HttpMethod.GET, WECHAT_MP_CALLBACK_PATH).permitAll()
                        .requestMatchers(HttpMethod.GET, QR_LOGIN_STATUS_PATH).permitAll()
                        .requestMatchers(HttpMethod.GET, HEALTH_PATH).permitAll()
                        .requestMatchers(HttpMethod.GET, WECHAT_JSAPI_CONFIG_PATH).permitAll()
                        .requestMatchers(HttpMethod.POST, LOG_CENTER_REPORT_PATH).permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            throw new UsernameNotFoundException("默认认证方式已禁用，请使用 " + LOGIN_PATH);
        };
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(ALLOWED_ORIGINS);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(List.of("*"));
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
