package com.example.lecturesystem.modules.auth.service;

import com.example.lecturesystem.modules.auth.dto.LoginRequest;
import com.example.lecturesystem.modules.auth.vo.LoginVO;

public interface AuthService {
    LoginVO login(LoginRequest request);
}
