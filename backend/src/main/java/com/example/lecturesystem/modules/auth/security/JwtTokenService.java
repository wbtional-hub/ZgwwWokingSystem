package com.example.lecturesystem.modules.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtTokenService {
    private final SecretKey secretKey;
    private final long expireSeconds;

    public JwtTokenService(@Value("${app.jwt.secret}") String secret,
                           @Value("${app.jwt.expire-seconds}") long expireSeconds) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expireSeconds = expireSeconds;
    }

    public String generateToken(LoginUser loginUser) {
        Instant now = Instant.now();
        Instant expireAt = now.plusSeconds(expireSeconds);

        return Jwts.builder()
                .subject(String.valueOf(loginUser.getUserId()))
                .claim("username", loginUser.getUsername())
                .claim("realName", loginUser.getRealName())
                .claim("role", loginUser.getRole())
                .claim("superAdmin", loginUser.isAdmin())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expireAt))
                .signWith(secretKey)
                .compact();
    }

    public LoginUser parseToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        Long userId = Long.valueOf(claims.getSubject());
        String username = claims.get("username", String.class);
        String realName = claims.get("realName", String.class);
        String role = claims.get("role", String.class);
        if (role == null || role.isBlank()) {
            Boolean superAdmin = claims.get("superAdmin", Boolean.class);
            role = Boolean.TRUE.equals(superAdmin) ? "ADMIN" : "USER";
        }
        return new LoginUser(userId, username, realName, role);
    }
}
