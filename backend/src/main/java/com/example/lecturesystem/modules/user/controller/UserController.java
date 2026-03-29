package com.example.lecturesystem.modules.user.controller;

import com.example.lecturesystem.common.ApiResponse;
import com.example.lecturesystem.modules.user.dto.CreateUserRequest;
import com.example.lecturesystem.modules.user.dto.UserQueryRequest;
import com.example.lecturesystem.modules.user.dto.UpdateUserRequest;
import com.example.lecturesystem.modules.user.service.UserService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

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

    // 兼容当前骨架里的旧接口，避免已有调用被破坏。
    @PostMapping("/user/update")
    public ApiResponse<?> update(@Validated @RequestBody UpdateUserRequest request) {
        userService.updateUser(request);
        return ApiResponse.success("ok");
    }

    // 兼容当前骨架里的旧接口，避免已有调用被破坏。
    @PostMapping("/user/reset-password/{userId}")
    public ApiResponse<?> resetPassword(@PathVariable("userId") Long userId) {
        userService.resetPassword(userId);
        return ApiResponse.success("ok");
    }
}
