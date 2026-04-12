package com.example.lecturesystem.modules.auth.service.impl;

import com.example.lecturesystem.modules.auth.entity.LoginLogEntity;
import com.example.lecturesystem.modules.auth.mapper.LoginLogMapper;
import com.example.lecturesystem.modules.auth.service.LoginLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class LoginLogServiceImpl implements LoginLogService {
    private static final Logger log = LoggerFactory.getLogger(LoginLogServiceImpl.class);

    private final LoginLogMapper loginLogMapper;

    public LoginLogServiceImpl(LoginLogMapper loginLogMapper) {
        this.loginLogMapper = loginLogMapper;
    }

    @Override
    public void record(Long userId,
                       String username,
                       String loginIp,
                       String loginResult,
                       String failReason,
                       String userAgent) {
        LoginLogEntity entity = new LoginLogEntity();
        entity.setUserId(userId);
        entity.setUsername(username);
        entity.setLoginTime(LocalDateTime.now());
        entity.setLoginIp(loginIp);
        entity.setLoginResult(loginResult);
        entity.setFailReason(failReason);
        entity.setUserAgent(userAgent);

        try {
            loginLogMapper.insert(entity);
        } catch (Exception ex) {
            log.warn("Failed to persist login log, username={}, result={}", username, loginResult, ex);
        }
    }
}
