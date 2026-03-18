package com.example.lecturesystem.modules.user.service;

import com.example.lecturesystem.modules.user.dto.CreateUserRequest;
import com.example.lecturesystem.modules.user.dto.UserQueryRequest;
import com.example.lecturesystem.modules.user.dto.UpdateUserRequest;

public interface UserService {
    Object queryPage(UserQueryRequest request);
    Long createUser(CreateUserRequest request);
    Object detail(Long userId);
    void updateUser(UpdateUserRequest request);
    void deleteUser(Long userId);
    void resetPassword(Long userId);
}
