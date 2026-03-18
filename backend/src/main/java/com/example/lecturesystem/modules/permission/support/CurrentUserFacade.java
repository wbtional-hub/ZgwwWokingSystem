package com.example.lecturesystem.modules.permission.support;

import com.example.lecturesystem.modules.auth.security.LoginUser;
import com.example.lecturesystem.modules.user.entity.UserEntity;
import com.example.lecturesystem.modules.user.mapper.UserMapper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserFacade {
    private static final String ROLE_ADMIN = "ADMIN";

    private final UserMapper userMapper;

    public CurrentUserFacade(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public LoginUser currentLoginUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof LoginUser loginUser)) {
            throw new IllegalArgumentException("当前未登录");
        }
        return loginUser;
    }

    public UserEntity currentUserEntity() {
        UserEntity user = userMapper.findById(currentLoginUser().getUserId());
        if (user == null) {
            throw new IllegalArgumentException("当前用户不存在");
        }
        return user;
    }

    public DataScopeContext currentDataScope() {
        UserEntity user = currentUserEntity();
        boolean superAdmin = ROLE_ADMIN.equalsIgnoreCase(user.getRole());
        if (!superAdmin && user.getUnitId() == null) {
            throw new IllegalArgumentException("当前用户未绑定单位");
        }
        return new DataScopeContext(user.getId(), user.getUnitId(), superAdmin);
    }
}
