package com.example.lecturesystem.modules.user.controller;

import com.example.lecturesystem.common.ApiResponse;
import com.example.lecturesystem.modules.user.dto.BindWechatMiniRequest;
import com.example.lecturesystem.modules.user.dto.CreateUserRequest;
import com.example.lecturesystem.modules.user.dto.UpdateUserRequest;
import com.example.lecturesystem.modules.user.dto.UserQueryRequest;
import com.example.lecturesystem.modules.user.service.UserService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/users")
    public ApiResponse<?> page(UserQueryRequest request) {
        return ApiResponse.success(userService.queryPage(request));
    }

    @GetMapping("/users/{userId}")
    public ApiResponse<?> detail(@PathVariable("userId") Long userId) {
        return ApiResponse.success(userService.detail(userId));
    }

    @PostMapping("/users")
    public ApiResponse<?> create(@Validated @RequestBody CreateUserRequest request) {
        return ApiResponse.success(userService.createUser(request));
    }

    @PutMapping("/users/{userId}")
    public ApiResponse<?> updateById(@PathVariable("userId") Long userId, @Validated @RequestBody UpdateUserRequest request) {
        request.setId(userId);
        userService.updateUser(request);
        return ApiResponse.success("ok");
    }

    @DeleteMapping("/users/{userId}")
    public ApiResponse<?> delete(@PathVariable("userId") Long userId) {
        userService.deleteUser(userId);
        return ApiResponse.success("ok");
    }

    @PostMapping("/users/bind-wechat-mini")
    public ApiResponse<?> bindWechatMini(@Validated @RequestBody BindWechatMiniRequest request) {
        return ApiResponse.success(userService.bindWechatMini(request));
    }

    @PostMapping("/user/update")
    public ApiResponse<?> update(@Validated @RequestBody UpdateUserRequest request) {
        userService.updateUser(request);
        return ApiResponse.success("ok");
    }

    @PostMapping("/user/reset-password/{userId}")
    public ApiResponse<?> resetPassword(@PathVariable("userId") Long userId) {
        userService.resetPassword(userId);
        return ApiResponse.success("ok");
    }
}
