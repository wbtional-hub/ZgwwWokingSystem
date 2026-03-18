package com.example.lecturesystem.modules.auth.service.impl;

import com.example.lecturesystem.common.ApiResponse;
import com.example.lecturesystem.modules.auth.controller.AuthController;
import com.example.lecturesystem.modules.auth.dto.LoginRequest;
import com.example.lecturesystem.modules.auth.security.JwtTokenService;
import com.example.lecturesystem.modules.auth.security.LoginUser;
import com.example.lecturesystem.modules.auth.vo.LoginVO;
import com.example.lecturesystem.modules.permission.mapper.PermissionMapper;
import com.example.lecturesystem.modules.user.dto.UserQueryRequest;
import com.example.lecturesystem.modules.user.entity.UserEntity;
import com.example.lecturesystem.modules.user.mapper.UserMapper;
import com.example.lecturesystem.modules.user.vo.UserDetailVO;
import com.example.lecturesystem.modules.user.vo.UserListItemVO;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;

public class AuthServiceImplTest {
    @Test
    public void loginShouldReturnMinimalLoginResult() {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        StubUserMapper userMapper = new StubUserMapper();
        UserEntity user = new UserEntity();
        user.setId(1L);
        user.setUsername("admin");
        user.setPasswordHash(passwordEncoder.encode("admin123"));
        user.setRealName("超级管理员");
        user.setRole("ADMIN");
        user.setStatus(1);
        userMapper.user = user;

        StubPermissionMapper permissionMapper = new StubPermissionMapper(true);
        JwtTokenService jwtTokenService = new JwtTokenService("change-this-secret-in-production", 7200);
        AuthServiceImpl authService = new AuthServiceImpl(userMapper, permissionMapper, jwtTokenService);

        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("admin123");

        LoginVO result = authService.login(request);

        Assert.assertNotNull(result.getToken());
        Assert.assertEquals("admin", result.getUsername());
        Assert.assertEquals("超级管理员", result.getRealName());
        Assert.assertEquals(Boolean.TRUE, result.getSuperAdmin());
    }

    @Test
    public void loginShouldRejectWrongPassword() {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        StubUserMapper userMapper = new StubUserMapper();
        UserEntity user = new UserEntity();
        user.setId(1L);
        user.setUsername("admin");
        user.setPasswordHash(passwordEncoder.encode("admin123"));
        user.setRealName("超级管理员");
        user.setRole("ADMIN");
        user.setStatus(1);
        userMapper.user = user;

        StubPermissionMapper permissionMapper = new StubPermissionMapper(true);
        JwtTokenService jwtTokenService = new JwtTokenService("change-this-secret-in-production", 7200);
        AuthServiceImpl authService = new AuthServiceImpl(userMapper, permissionMapper, jwtTokenService);

        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("wrong-password");

        try {
            authService.login(request);
            Assert.fail("预期错误密码时抛出异常");
        } catch (BadCredentialsException ex) {
            Assert.assertEquals("用户名或密码错误", ex.getMessage());
        }
    }

    @Test
    public void loginShouldRejectDisabledUser() {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        StubUserMapper userMapper = new StubUserMapper();
        UserEntity user = new UserEntity();
        user.setId(2L);
        user.setUsername("disabled");
        user.setPasswordHash(passwordEncoder.encode("admin123"));
        user.setRealName("停用用户");
        user.setRole("USER");
        user.setStatus(0);
        userMapper.user = user;

        StubPermissionMapper permissionMapper = new StubPermissionMapper(false);
        JwtTokenService jwtTokenService = new JwtTokenService("change-this-secret-in-production", 7200);
        AuthServiceImpl authService = new AuthServiceImpl(userMapper, permissionMapper, jwtTokenService);

        LoginRequest request = new LoginRequest();
        request.setUsername("disabled");
        request.setPassword("admin123");

        try {
            authService.login(request);
            Assert.fail("预期停用用户无法登录");
        } catch (DisabledException ex) {
            Assert.assertEquals("用户已停用", ex.getMessage());
        }
    }

    @Test
    public void meShouldReturnSameShapeAsLoginResult() {
        AuthController controller = new AuthController(request -> null);
        LoginUser loginUser = new LoginUser(1L, "admin", "超级管理员", true);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(loginUser, null);

        ApiResponse<?> response = controller.me(authentication);

        Assert.assertEquals(0, response.getCode());
        Assert.assertTrue(response.getData() instanceof LoginVO);

        LoginVO result = (LoginVO) response.getData();
        Assert.assertEquals(Long.valueOf(1L), result.getUserId());
        Assert.assertEquals("admin", result.getUsername());
        Assert.assertEquals("超级管理员", result.getRealName());
        Assert.assertEquals("ADMIN", result.getRole());
        Assert.assertEquals(Boolean.TRUE, result.getSuperAdmin());
        Assert.assertNull(result.getToken());
    }

    private static class StubUserMapper implements UserMapper {
        private UserEntity user;

        @Override
        public UserEntity findById(Long id) {
            return user != null && id.equals(user.getId()) ? user : null;
        }

        @Override
        public UserEntity findByUsername(String username) {
            return user != null && username.equals(user.getUsername()) ? user : null;
        }

        @Override
        public int insertUser(UserEntity entity) {
            this.user = entity;
            return 1;
        }

        @Override
        public long countPageByUserId(Long userId, UserQueryRequest request) {
            return 0;
        }

        @Override
        public List<UserListItemVO> queryPageByUserId(Long userId, UserQueryRequest request) {
            return List.of();
        }

        @Override
        public long countPageByTreePath(String treePathPrefix, UserQueryRequest request) {
            return 0;
        }

        @Override
        public List<UserListItemVO> queryPageByTreePath(String treePathPrefix, UserQueryRequest request) {
            return List.of();
        }

        @Override
        public long countPage(UserQueryRequest request) {
            return 0;
        }

        @Override
        public List<UserListItemVO> queryPage(UserQueryRequest request) {
            return List.of();
        }

        @Override
        public UserDetailVO detailById(Long id) {
            return null;
        }

        @Override
        public UserDetailVO detailByIdAndTreePath(Long id, String treePathPrefix) {
            return null;
        }

        @Override
        public int updateUser(UserEntity entity) {
            this.user = entity;
            return 1;
        }

        @Override
        public int logicalDelete(Long id, String updateUser, LocalDateTime updateTime) {
            return 1;
        }

        @Override
        public int updatePassword(Long id, String passwordHash, String updateUser, LocalDateTime updateTime) {
            if (user != null && id.equals(user.getId())) {
                user.setPasswordHash(passwordHash);
            }
            return 1;
        }
    }

    private static class StubPermissionMapper implements PermissionMapper {
        private final boolean superAdmin;

        private StubPermissionMapper(boolean superAdmin) {
            this.superAdmin = superAdmin;
        }

        @Override
        public boolean existsUserRole(Long userId, String roleCode) {
            return superAdmin;
        }

        @Override
        public List<Long> queryUserIdsByTreePathPrefix(String treePathPrefix) {
            return List.of();
        }

        @Override
        public int insertUserRole(Long userId, String roleCode) {
            return 1;
        }
    }
}
