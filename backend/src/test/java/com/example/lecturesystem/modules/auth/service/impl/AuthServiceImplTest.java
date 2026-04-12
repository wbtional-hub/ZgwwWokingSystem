package com.example.lecturesystem.modules.auth.service.impl;

import com.example.lecturesystem.common.ApiResponse;
import com.example.lecturesystem.modules.auth.controller.AuthController;
import com.example.lecturesystem.modules.auth.config.AuthProperties;
import com.example.lecturesystem.modules.auth.dto.ChangePasswordRequest;
import com.example.lecturesystem.modules.auth.dto.LoginRequest;
import com.example.lecturesystem.modules.auth.dto.WechatMiniLoginRequest;
import com.example.lecturesystem.modules.auth.security.JwtTokenService;
import com.example.lecturesystem.modules.auth.security.LoginUser;
import com.example.lecturesystem.modules.auth.support.Sm3PasswordCodec;
import com.example.lecturesystem.modules.auth.support.PasswordPolicyValidator;
import com.example.lecturesystem.modules.auth.service.AuthService;
import com.example.lecturesystem.modules.auth.service.WechatMpAuthService;
import com.example.lecturesystem.modules.auth.vo.LoginVO;
import com.example.lecturesystem.modules.auth.vo.MobileLoginOptionsVO;
import com.example.lecturesystem.modules.permission.mapper.PermissionMapper;
import com.example.lecturesystem.modules.user.dto.UserQueryRequest;
import com.example.lecturesystem.modules.user.entity.UserEntity;
import com.example.lecturesystem.modules.user.mapper.UserMapper;
import com.example.lecturesystem.modules.user.vo.UserDetailVO;
import com.example.lecturesystem.modules.user.vo.UserListItemVO;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.List;

public class AuthServiceImplTest {
    @After
    public void tearDown() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    public void loginShouldReturnMinimalLoginResult() {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        StubUserMapper userMapper = new StubUserMapper();
        UserEntity user = buildUser(passwordEncoder, 1L, "admin", "admin123", "ADMIN", 1);
        userMapper.user = user;

        AuthServiceImpl authService = new AuthServiceImpl(
                userMapper,
                new StubPermissionMapper(true),
                new JwtTokenService("change-this-secret-in-production", 7200)
        );

        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("admin123");

        LoginVO result = authService.login(request);

        Assert.assertNotNull(result.getToken());
        Assert.assertEquals("admin", result.getUsername());
        Assert.assertEquals("ADMIN", result.getRole());
        Assert.assertEquals(Boolean.TRUE, result.getSuperAdmin());
    }

    @Test(expected = BadCredentialsException.class)
    public void loginShouldRejectWrongPassword() {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        StubUserMapper userMapper = new StubUserMapper();
        userMapper.user = buildUser(passwordEncoder, 1L, "admin", "admin123", "ADMIN", 1);

        AuthServiceImpl authService = new AuthServiceImpl(
                userMapper,
                new StubPermissionMapper(true),
                new JwtTokenService("change-this-secret-in-production", 7200)
        );

        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("wrong-password");

        authService.login(request);
    }

    @Test
    public void loginShouldIncreaseFailCountAndLockAfterFiveFailures() {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        StubUserMapper userMapper = new StubUserMapper();
        userMapper.user = buildUser(passwordEncoder, 1L, "admin", "admin123", "ADMIN", 1);

        AuthServiceImpl authService = new AuthServiceImpl(
                userMapper,
                new StubPermissionMapper(true),
                new JwtTokenService("change-this-secret-in-production", 7200)
        );

        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("wrong-password");

        for (int i = 0; i < 4; i++) {
            try {
                authService.login(request);
                Assert.fail("Expected wrong password to fail");
            } catch (BadCredentialsException ex) {
                Assert.assertEquals(i + 1, userMapper.user.getLoginFailCount().intValue());
                Assert.assertNull(userMapper.user.getLockUntil());
            }
        }

        try {
            authService.login(request);
            Assert.fail("Expected user to be locked on the fifth failure");
        } catch (BadCredentialsException ex) {
            Assert.assertEquals(5, userMapper.user.getLoginFailCount().intValue());
            Assert.assertNotNull(userMapper.user.getLockUntil());
            Assert.assertTrue(ex.getMessage().contains("锁定"));
        }
    }

    @Test
    public void loginShouldResetFailCountAfterSuccess() {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        StubUserMapper userMapper = new StubUserMapper();
        userMapper.user = buildUser(passwordEncoder, 1L, "admin", "admin123", "ADMIN", 1);
        userMapper.user.setLoginFailCount(3);
        userMapper.user.setLockUntil(LocalDateTime.now().minusMinutes(1));

        AuthServiceImpl authService = new AuthServiceImpl(
                userMapper,
                new StubPermissionMapper(true),
                new JwtTokenService("change-this-secret-in-production", 7200)
        );

        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("admin123");

        LoginVO result = authService.login(request);

        Assert.assertNotNull(result.getToken());
        Assert.assertEquals(Integer.valueOf(0), userMapper.user.getLoginFailCount());
        Assert.assertNull(userMapper.user.getLockUntil());
    }

    @Test
    public void loginShouldMarkForcePasswordChangeWhenFlagIsTrue() {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        StubUserMapper userMapper = new StubUserMapper();
        userMapper.user = buildUser(passwordEncoder, 1L, "admin", "admin123", "ADMIN", 1);
        userMapper.user.setForcePasswordChange(Boolean.TRUE);

        AuthServiceImpl authService = new AuthServiceImpl(
                userMapper,
                new StubPermissionMapper(true),
                new JwtTokenService("change-this-secret-in-production", 7200)
        );

        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("admin123");

        LoginVO result = authService.login(request);

        Assert.assertNotNull(result.getToken());
        Assert.assertEquals(Boolean.TRUE, result.getForcePasswordChange());
    }

    @Test
    public void loginShouldRejectMobilePasswordLoginWhenSwitchIsOff() {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        StubUserMapper userMapper = new StubUserMapper();
        userMapper.user = buildUser(passwordEncoder, 1L, "admin", "admin123", "ADMIN", 1);

        AuthServiceImpl authService = createService(userMapper, false);
        bindMobileRequest();

        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("admin123");

        try {
            authService.login(request);
            Assert.fail("Expected mobile password login to be blocked");
        } catch (IllegalArgumentException ex) {
            Assert.assertEquals("当前环境未开启手机端账号密码登录，请使用微信登录", ex.getMessage());
        }
    }

    @Test
    public void loginShouldAllowMobilePasswordLoginWhenSwitchIsOn() {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        StubUserMapper userMapper = new StubUserMapper();
        userMapper.user = buildUser(passwordEncoder, 1L, "admin", "admin123", "ADMIN", 1);

        AuthServiceImpl authService = createService(userMapper, true);
        bindMobileRequest();

        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("admin123");

        LoginVO result = authService.login(request);

        Assert.assertNotNull(result.getToken());
        Assert.assertEquals("admin", result.getUsername());
    }

    @Test
    public void loginShouldAllowMobilePasswordLoginFromWhitelistedTestOriginWhenSwitchIsOff() {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        StubUserMapper userMapper = new StubUserMapper();
        userMapper.user = buildUser(passwordEncoder, 1L, "admin", "admin123", "ADMIN", 1);

        AuthServiceImpl authService = createService(userMapper, false);
        bindMobileRequest("http://124.220.158.213:9090", "124.220.158.213:9090", "http");

        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("admin123");

        LoginVO result = authService.login(request);

        Assert.assertNotNull(result.getToken());
        Assert.assertEquals("admin", result.getUsername());
    }

    @Test
    public void mobileLoginOptionsShouldExposePasswordSwitch() {
        AuthServiceImpl authService = createService(new StubUserMapper(), true);

        MobileLoginOptionsVO result = authService.getMobileLoginOptions();

        Assert.assertTrue(result.isPasswordLoginEnabled());
    }

    @Test
    public void mobileLoginOptionsShouldAllowWhitelistedTestOriginWhenSwitchIsOff() {
        AuthServiceImpl authService = createService(new StubUserMapper(), false);
        bindMobileRequest("http://124.220.158.213:9090", "124.220.158.213:9090", "http");

        MobileLoginOptionsVO result = authService.getMobileLoginOptions();

        Assert.assertTrue(result.isPasswordLoginEnabled());
    }

    @Test
    public void loginShouldSupportSm3PasswordWhenPasswordAlgoIsSm3() {
        StubUserMapper userMapper = new StubUserMapper();
        UserEntity user = new UserEntity();
        user.setId(3L);
        user.setUsername("sm3-user");
        user.setRealName("sm3-user");
        user.setRole("USER");
        user.setStatus(1);
        user.setPasswordAlgo("SM3");
        user.setPasswordSalt("salt-001");
        user.setPasswordHash(new Sm3PasswordCodec().encode("admin123", "salt-001"));
        userMapper.user = user;

        AuthServiceImpl authService = new AuthServiceImpl(
                userMapper,
                new StubPermissionMapper(false),
                new JwtTokenService("change-this-secret-in-production", 7200)
        );

        LoginRequest request = new LoginRequest();
        request.setUsername("sm3-user");
        request.setPassword("admin123");

        LoginVO result = authService.login(request);

        Assert.assertNotNull(result.getToken());
        Assert.assertEquals("sm3-user", result.getUsername());
        Assert.assertEquals("USER", result.getRole());
    }

    @Test(expected = DisabledException.class)
    public void loginShouldRejectDisabledUser() {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        StubUserMapper userMapper = new StubUserMapper();
        userMapper.user = buildUser(passwordEncoder, 2L, "disabled", "admin123", "USER", 0);

        AuthServiceImpl authService = new AuthServiceImpl(
                userMapper,
                new StubPermissionMapper(false),
                new JwtTokenService("change-this-secret-in-production", 7200)
        );

        LoginRequest request = new LoginRequest();
        request.setUsername("disabled");
        request.setPassword("admin123");

        authService.login(request);
    }

    @Test
    public void changePasswordShouldUpdateCurrentUserPassword() {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        Sm3PasswordCodec sm3PasswordCodec = new Sm3PasswordCodec();
        StubUserMapper userMapper = new StubUserMapper();
        userMapper.user = buildUser(passwordEncoder, 1L, "admin", "admin123", "ADMIN", 1);

        AuthServiceImpl authService = createService(userMapper);
        mockLoginUser(1L, "admin", true);

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword("admin123");
        request.setNewPassword("newpass123");
        request.setConfirmPassword("newpass123");

        authService.changePassword(request);

        Assert.assertEquals("SM3", userMapper.user.getPasswordAlgo());
        Assert.assertNotNull(userMapper.user.getPasswordSalt());
        Assert.assertTrue(sm3PasswordCodec.matches("newpass123", userMapper.user.getPasswordSalt(), userMapper.user.getPasswordHash()));
        Assert.assertFalse(sm3PasswordCodec.matches("admin123", userMapper.user.getPasswordSalt(), userMapper.user.getPasswordHash()));
        Assert.assertEquals(Boolean.FALSE, userMapper.user.getForcePasswordChange());

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("admin");
        loginRequest.setPassword("newpass123");
        LoginVO loginResult = authService.login(loginRequest);
        Assert.assertNotNull(loginResult.getToken());
    }

    @Test
    public void changePasswordShouldRejectWrongOldPassword() {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        StubUserMapper userMapper = new StubUserMapper();
        userMapper.user = buildUser(passwordEncoder, 1L, "admin", "admin123", "ADMIN", 1);

        AuthServiceImpl authService = createService(userMapper);
        mockLoginUser(1L, "admin", true);

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword("wrong-password");
        request.setNewPassword("newpass123");
        request.setConfirmPassword("newpass123");

        try {
            authService.changePassword(request);
            Assert.fail("Expected wrong old password to fail");
        } catch (IllegalArgumentException ex) {
            Assert.assertNotNull(ex.getMessage());
            Assert.assertTrue(passwordEncoder.matches("admin123", userMapper.user.getPasswordHash()));
        }
    }

    @Test
    public void changePasswordShouldRejectSamePassword() {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        StubUserMapper userMapper = new StubUserMapper();
        userMapper.user = buildUser(passwordEncoder, 1L, "admin", "admin123", "ADMIN", 1);

        AuthServiceImpl authService = createService(userMapper);
        mockLoginUser(1L, "admin", true);

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword("admin123");
        request.setNewPassword("admin123");
        request.setConfirmPassword("admin123");

        try {
            authService.changePassword(request);
            Assert.fail("Expected same password to fail");
        } catch (IllegalArgumentException ex) {
            Assert.assertNotNull(ex.getMessage());
            Assert.assertTrue(passwordEncoder.matches("admin123", userMapper.user.getPasswordHash()));
        }
    }

    @Test
    public void changePasswordShouldRejectWeakNewPassword() {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        StubUserMapper userMapper = new StubUserMapper();
        userMapper.user = buildUser(passwordEncoder, 1L, "admin", "admin123", "ADMIN", 1);

        AuthServiceImpl authService = createService(userMapper);
        mockLoginUser(1L, "admin", true);

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword("admin123");
        request.setNewPassword("admin123");
        request.setConfirmPassword("admin123");

        try {
            authService.changePassword(request);
            Assert.fail("Expected weak password to fail");
        } catch (IllegalArgumentException ex) {
            Assert.assertEquals("新密码不能与原密码相同", ex.getMessage());
        }

        request.setNewPassword("password");
        request.setConfirmPassword("password");

        try {
            authService.changePassword(request);
            Assert.fail("Expected weak password to fail");
        } catch (IllegalArgumentException ex) {
            Assert.assertEquals(PasswordPolicyValidator.MESSAGE, ex.getMessage());
            Assert.assertTrue(passwordEncoder.matches("admin123", userMapper.user.getPasswordHash()));
        }
    }

    @Test
    public void callbackShouldReturnUnboundWechatHintInsteadOfThrowing() {
        StubUserMapper userMapper = new StubUserMapper();
        AuthProperties authProperties = new AuthProperties();
        AuthServiceImpl authService = new AuthServiceImpl(
                authProperties,
                userMapper,
                new JwtTokenService("change-this-secret-in-production", 7200),
                new NoopLoginLogService(),
                new NoopOperationLogService(),
                code -> {
                    throw new UnsupportedOperationException("not used");
                },
                new WechatMpAuthService() {
                    @Override
                    public com.example.lecturesystem.modules.auth.vo.WechatMpAuthorizeUrlVO buildAuthorizeUrl(String returnUrl, String state) {
                        throw new UnsupportedOperationException("not used");
                    }

                    @Override
                    public WechatMpIdentity exchangeCode(String code) {
                        return new WechatMpIdentity("openid-1", "unionid-1");
                    }

                    @Override
                    public WechatMpCallbackState parseCallbackState(String state) {
                        return new WechatMpCallbackState("/mobile-workspace", "login");
                    }

                    @Override
                    public String buildSuccessCallbackRedirect(LoginVO loginVO, WechatMpCallbackState callbackState) {
                        throw new UnsupportedOperationException("not used");
                    }

                    @Override
                    public String buildFailureCallbackRedirect(WechatMpCallbackState callbackState, String message) {
                        return buildFailureCallbackRedirect(callbackState, CALLBACK_ERROR_CODE_UNKNOWN, message);
                    }

                    @Override
                    public String buildFailureCallbackRedirect(WechatMpCallbackState callbackState, String code, String message) {
                        return "https://www.xmzgww.com/login?redirect=/mobile-workspace&wechatAuthFailed=1"
                                + "&wechatAuthCode=" + code
                                + "&wechatAuthMessage=" + message;
                    }
                }
        );

        String redirectUrl = authService.handleWechatMpCallback("demo-code", "state", null, null);

        Assert.assertTrue(redirectUrl.contains("wechatAuthFailed=1"));
        Assert.assertTrue(redirectUrl.contains("wechatAuthCode=WECHAT_MP_UNBOUND"));
    }

    @Test
    public void meShouldReturnSameShapeAsLoginResult() {
        AuthController controller = new AuthController(new AuthService() {
            @Override
            public LoginVO login(LoginRequest request) {
                return null;
            }

            @Override
            public LoginVO wechatMiniLogin(WechatMiniLoginRequest request) {
                return null;
            }

            @Override
            public WechatMiniIdentity exchangeWechatMiniCode(String code) {
                return null;
            }
        });
        LoginUser loginUser = new LoginUser(1L, "admin", "Admin", true);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(loginUser, null);

        ApiResponse<?> response = controller.me(authentication);

        Assert.assertEquals(0, response.getCode());
        Assert.assertTrue(response.getData() instanceof LoginVO);

        LoginVO result = (LoginVO) response.getData();
        Assert.assertEquals(Long.valueOf(1L), result.getUserId());
        Assert.assertEquals("admin", result.getUsername());
        Assert.assertEquals("ADMIN", result.getRole());
        Assert.assertEquals(Boolean.TRUE, result.getSuperAdmin());
        Assert.assertNull(result.getToken());
    }

    @Test
    public void loginControllerShouldReturnForcePasswordChangeMessage() {
        AuthController controller = new AuthController(new AuthService() {
            @Override
            public LoginVO login(LoginRequest request) {
                LoginVO loginVO = new LoginVO();
                loginVO.setToken("demo-token");
                loginVO.setUserId(1L);
                loginVO.setUsername("admin");
                loginVO.setForcePasswordChange(Boolean.TRUE);
                return loginVO;
            }

            @Override
            public LoginVO wechatMiniLogin(WechatMiniLoginRequest request) {
                return null;
            }

            @Override
            public WechatMiniIdentity exchangeWechatMiniCode(String code) {
                return null;
            }
        });

        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("admin123");

        ApiResponse<?> response = controller.login(request);

        Assert.assertEquals(0, response.getCode());
        Assert.assertEquals("FORCE_PASSWORD_CHANGE", response.getMessage());
        Assert.assertTrue(response.getData() instanceof LoginVO);
    }

    @Test
    public void mobileLoginOptionsControllerShouldReturnSwitchState() {
        AuthController controller = new AuthController(new AuthService() {
            @Override
            public LoginVO login(LoginRequest request) {
                return null;
            }

            @Override
            public MobileLoginOptionsVO getMobileLoginOptions() {
                MobileLoginOptionsVO result = new MobileLoginOptionsVO();
                result.setPasswordLoginEnabled(false);
                return result;
            }

            @Override
            public LoginVO wechatMiniLogin(WechatMiniLoginRequest request) {
                return null;
            }

            @Override
            public WechatMiniIdentity exchangeWechatMiniCode(String code) {
                return null;
            }
        });

        ApiResponse<MobileLoginOptionsVO> response = controller.mobileLoginOptions();

        Assert.assertEquals(0, response.getCode());
        Assert.assertNotNull(response.getData());
        Assert.assertFalse(response.getData().isPasswordLoginEnabled());
    }

    private static AuthServiceImpl createService(StubUserMapper userMapper) {
        return createService(userMapper, false);
    }

    private static AuthServiceImpl createService(StubUserMapper userMapper, boolean mobilePasswordLoginEnabled) {
        AuthProperties authProperties = new AuthProperties();
        authProperties.getMobile().getPasswordLogin().setEnabled(mobilePasswordLoginEnabled);
        return new AuthServiceImpl(
                authProperties,
                userMapper,
                new JwtTokenService("change-this-secret-in-production", 7200),
                new NoopLoginLogService(),
                new NoopOperationLogService(),
                code -> {
                    throw new UnsupportedOperationException("not used");
                },
                new WechatMpAuthService() {
                    @Override
                    public com.example.lecturesystem.modules.auth.vo.WechatMpAuthorizeUrlVO buildAuthorizeUrl(String returnUrl, String state) {
                        throw new UnsupportedOperationException("not used");
                    }

                    @Override
                    public WechatMpIdentity exchangeCode(String code) {
                        throw new UnsupportedOperationException("not used");
                    }

                    @Override
                    public WechatMpCallbackState parseCallbackState(String state) {
                        throw new UnsupportedOperationException("not used");
                    }

                    @Override
                    public String buildSuccessCallbackRedirect(LoginVO loginVO, WechatMpCallbackState callbackState) {
                        throw new UnsupportedOperationException("not used");
                    }

                    @Override
                    public String buildFailureCallbackRedirect(WechatMpCallbackState callbackState, String message) {
                        throw new UnsupportedOperationException("not used");
                    }
                }
        );
    }

    private static UserEntity buildUser(BCryptPasswordEncoder passwordEncoder,
                                        Long id,
                                        String username,
                                        String rawPassword,
                                        String role,
                                        Integer status) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setRealName(username);
        user.setRole(role);
        user.setStatus(status);
        return user;
    }

    private static void mockLoginUser(Long userId, String username, boolean admin) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(new LoginUser(userId, username, username, admin), null, List.of())
        );
    }

    private static void bindMobileRequest() {
        bindMobileRequest(null, null, null);
    }

    private static void bindMobileRequest(String origin, String host, String scheme) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.addHeader("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X)");
        if (origin != null) {
            request.addHeader("Origin", origin);
            request.addHeader("Referer", origin + "/login");
        }
        if (host != null) {
            request.addHeader("Host", host);
            request.addHeader("X-Forwarded-Host", host);
        }
        if (scheme != null) {
            request.setScheme(scheme);
            request.addHeader("X-Forwarded-Proto", scheme);
        }
        request.setServerName("124.220.158.213");
        request.setServerPort(9090);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
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
        public UserEntity findByWechatOpenId(String wechatOpenId) {
            return user != null && wechatOpenId.equals(user.getWechatOpenId()) ? user : null;
        }

        @Override
        public UserEntity findByWechatUnionId(String wechatUnionId) {
            return user != null && wechatUnionId.equals(user.getWechatUnionId()) ? user : null;
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
        public int insertUser(UserEntity entity) {
            this.user = entity;
            return 1;
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
        public int updateWechatBinding(Long id, String wechatOpenId, String wechatUnionId, String updateUser, LocalDateTime updateTime) {
            if (user != null && id.equals(user.getId())) {
                user.setWechatOpenId(wechatOpenId);
                user.setWechatUnionId(wechatUnionId);
                user.setUpdateUser(updateUser);
                user.setUpdateTime(updateTime);
            }
            return 1;
        }

        @Override
        public int logicalDelete(Long id, String updateUser, LocalDateTime updateTime) {
            return 1;
        }

        @Override
        public int updatePassword(Long id, String passwordHash, String passwordAlgo, String passwordSalt, Boolean forcePasswordChange, String updateUser, LocalDateTime updateTime) {
            if (user != null && id.equals(user.getId())) {
                user.setPasswordHash(passwordHash);
                user.setPasswordAlgo(passwordAlgo);
                user.setPasswordSalt(passwordSalt);
                user.setForcePasswordChange(forcePasswordChange);
                user.setUpdateUser(updateUser);
                user.setUpdateTime(updateTime);
            }
            return 1;
        }

        @Override
        public int updateLoginSecurityState(Long id, Integer loginFailCount, LocalDateTime lockUntil, String updateUser, LocalDateTime updateTime) {
            if (user != null && id.equals(user.getId())) {
                user.setLoginFailCount(loginFailCount);
                user.setLockUntil(lockUntil);
                user.setUpdateUser(updateUser);
                user.setUpdateTime(updateTime);
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

    private static class NoopOperationLogService implements com.example.lecturesystem.modules.operationlog.service.OperationLogService {
        @Override
        public void log(String moduleName, String actionName, Long bizId, String content) {
        }

        @Override
        public Object query(com.example.lecturesystem.modules.operationlog.dto.OperationLogQueryRequest request) {
            return List.of();
        }
    }

    private static class NoopLoginLogService implements com.example.lecturesystem.modules.auth.service.LoginLogService {
        @Override
        public void record(Long userId, String username, String loginIp, String loginResult, String failReason, String userAgent) {
        }
    }
}
