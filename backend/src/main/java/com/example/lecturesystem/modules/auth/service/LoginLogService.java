package com.example.lecturesystem.modules.auth.service;

public interface LoginLogService {
    void record(Long userId,
                String username,
                String loginIp,
                String loginResult,
                String failReason,
                String userAgent);
}
