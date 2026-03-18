package com.example.lecturesystem.modules.auth.service.impl;

import com.example.lecturesystem.modules.auth.dto.LoginRequest;
import com.example.lecturesystem.modules.auth.security.JwtTokenService;
import com.example.lecturesystem.modules.auth.security.LoginUser;
import com.example.lecturesystem.modules.auth.service.AuthService;
import com.example.lecturesystem.modules.auth.vo.LoginVO;
import com.example.lecturesystem.modules.operationlog.service.OperationLogService;
import com.example.lecturesystem.modules.permission.mapper.PermissionMapper;
import com.example.lecturesystem.modules.user.entity.UserEntity;
import com.example.lecturesystem.modules.user.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {
    private final UserMapper userMapper;
    private final JwtTokenService jwtTokenService;
    private final OperationLogService operationLogService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Autowired
    public AuthServiceImpl(UserMapper userMapper,
                           JwtTokenService jwtTokenService,
                           OperationLogService operationLogService) {
        this.userMapper = userMapper;
        this.jwtTokenService = jwtTokenService;
        this.operationLogService = operationLogService;
    }

    public AuthServiceImpl(UserMapper userMapper,
                           PermissionMapper permissionMapper,
                           JwtTokenService jwtTokenService) {
        this(userMapper, jwtTokenService, new OperationLogService() {
            @Override
            public void log(String moduleName, String actionName, Long bizId, String content) {
            }

            @Override
            public Object query(com.example.lecturesystem.modules.operationlog.dto.OperationLogQueryRequest request) {
                return java.util.List.of();
            }
        });
    }

    @Override
    public LoginVO login(LoginRequest request) {
        UserEntity user = userMapper.findByUsername(request.getUsername());
        if (user == null) {
            throw new BadCredentialsException("用户名或密码错误");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("用户名或密码错误");
        }
        if (!Integer.valueOf(1).equals(user.getStatus())) {
            throw new DisabledException("用户已停用");
        }

        LoginUser loginUser = new LoginUser(
                user.getId(),
                user.getUsername(),
                user.getRealName(),
                user.getRole()
        );
        operationLogService.log("AUTH", "LOGIN", user.getId(), "用户登录成功：" + user.getUsername() + "（" + loginUser.getRole() + "）");
        return buildLoginVO(loginUser);
    }

    private LoginVO buildLoginVO(LoginUser loginUser) {
        LoginVO vo = LoginVO.fromLoginUser(loginUser);
        vo.setToken(jwtTokenService.generateToken(loginUser));
        return vo;
    }
}
