package com.example.lecturesystem.modules.auth.service.impl;

import com.example.lecturesystem.modules.auth.config.AuthProperties;
import com.example.lecturesystem.modules.auth.dto.ChangePasswordRequest;
import com.example.lecturesystem.modules.auth.dto.LoginRequest;
import com.example.lecturesystem.modules.auth.dto.WechatMiniLoginRequest;
import com.example.lecturesystem.modules.auth.security.JwtTokenService;
import com.example.lecturesystem.modules.auth.security.LoginUser;
import com.example.lecturesystem.modules.auth.service.AuthService;
import com.example.lecturesystem.modules.auth.service.LoginLogService;
import com.example.lecturesystem.modules.auth.support.PasswordPolicyValidator;
import com.example.lecturesystem.modules.auth.support.Sm3PasswordCodec;
import com.example.lecturesystem.modules.auth.service.WechatMiniAuthService;
import com.example.lecturesystem.modules.auth.service.WechatMpAuthService;
import com.example.lecturesystem.modules.auth.vo.LoginVO;
import com.example.lecturesystem.modules.auth.vo.MobileLoginOptionsVO;
import com.example.lecturesystem.modules.auth.vo.WechatMpAuthorizeUrlVO;
import com.example.lecturesystem.modules.operationlog.service.OperationLogService;
import com.example.lecturesystem.modules.permission.mapper.PermissionMapper;
import com.example.lecturesystem.modules.user.entity.UserEntity;
import com.example.lecturesystem.modules.user.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

@Service
public class AuthServiceImpl implements AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);
    private static final String WECHAT_MP_BINDING_OPERATOR = "wechat-mp-login";
    private static final String LOGIN_RESULT_SUCCESS = "SUCCESS";
    private static final String LOGIN_RESULT_FAIL = "FAIL";
    private static final String LOGIN_TYPE_PASSWORD = "PASSWORD";
    private static final String LOGIN_TYPE_WECHAT = "WECHAT";
    private static final String PASSWORD_ALGO_BCRYPT = "BCRYPT";
    private static final String PASSWORD_ALGO_SM3 = "SM3";
    private static final int MAX_LOGIN_FAIL_COUNT = 5;
    private static final int LOGIN_LOCK_MINUTES = 30;
    private static final String TEST_LOGIN_ORIGIN = "http://124.220.158.213:9090";
    private static final String TEST_LOGIN_HOST = "124.220.158.213:9090";
    private static final Pattern MOBILE_USER_AGENT_PATTERN = Pattern.compile("Android|iPhone|iPad|iPod|Mobile|HarmonyOS", Pattern.CASE_INSENSITIVE);

    private final AuthProperties authProperties;
    private final UserMapper userMapper;
    private final JwtTokenService jwtTokenService;
    private final LoginLogService loginLogService;
    private final OperationLogService operationLogService;
    private final WechatMiniAuthService wechatMiniAuthService;
    private final WechatMpAuthService wechatMpAuthService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final Sm3PasswordCodec sm3PasswordCodec = new Sm3PasswordCodec();

    @Autowired
    public AuthServiceImpl(AuthProperties authProperties,
                           UserMapper userMapper,
                           JwtTokenService jwtTokenService,
                           LoginLogService loginLogService,
                           OperationLogService operationLogService,
                           WechatMiniAuthService wechatMiniAuthService,
                           WechatMpAuthService wechatMpAuthService) {
        this.authProperties = authProperties;
        this.userMapper = userMapper;
        this.jwtTokenService = jwtTokenService;
        this.loginLogService = loginLogService;
        this.operationLogService = operationLogService;
        this.wechatMiniAuthService = wechatMiniAuthService;
        this.wechatMpAuthService = wechatMpAuthService;
    }

    public AuthServiceImpl(UserMapper userMapper,
                           PermissionMapper permissionMapper,
                           JwtTokenService jwtTokenService) {
        this(
                new AuthProperties(),
                userMapper,
                jwtTokenService,
                new LoginLogService() {
                    @Override
                    public void record(Long userId,
                                       String username,
                                       String loginIp,
                                       String loginResult,
                                       String failReason,
                                       String userAgent) {
                    }
                },
                new OperationLogService() {
                    @Override
                    public void log(String moduleName, String actionName, Long bizId, String content) {
                    }

                    @Override
                    public Object query(com.example.lecturesystem.modules.operationlog.dto.OperationLogQueryRequest request) {
                        return java.util.List.of();
                    }
                },
                new WechatMiniAuthService() {
                    @Override
                    public WechatMiniIdentity exchangeCode(String code) {
                        throw new UnsupportedOperationException("Not implemented in tests");
                    }
                },
                new WechatMpAuthService() {
                    @Override
                    public WechatMpAuthorizeUrlVO buildAuthorizeUrl(String returnUrl, String state) {
                        throw new UnsupportedOperationException("Not implemented in tests");
                    }

                    @Override
                    public WechatMpIdentity exchangeCode(String code) {
                        throw new UnsupportedOperationException("Not implemented in tests");
                    }

                    @Override
                    public WechatMpCallbackState parseCallbackState(String state) {
                        return new WechatMpCallbackState("/mobile-workspace", null);
                    }

                    @Override
                    public String buildSuccessCallbackRedirect(LoginVO loginVO, WechatMpCallbackState callbackState) {
                        throw new UnsupportedOperationException("Not implemented in tests");
                    }

                    @Override
                    public String buildFailureCallbackRedirect(WechatMpCallbackState callbackState, String message) {
                        throw new UnsupportedOperationException("Not implemented in tests");
                    }
                }
        );
    }

    @Override
    public LoginVO login(LoginRequest request) {
        validateMobilePasswordLoginPolicy();
        String username = normalizeText(request.getUsername());
        UserEntity user = userMapper.findByUsername(username);
        try {
            validatePasswordUser(user, request.getPassword());
            resetLoginSecurityState(user);

            LoginUser loginUser = toLoginUser(user);
            operationLogService.log("AUTH", "LOGIN", user.getId(), "用户登录成功：" + user.getUsername() + "，角色：" + loginUser.getRole());
            LoginVO loginVO = buildLoginVO(loginUser);
            applyForcePasswordChangeFlag(loginVO, user);
            recordLoginAttempt(user, username, LOGIN_RESULT_SUCCESS, LOGIN_TYPE_PASSWORD, null);
            return loginVO;
        } catch (RuntimeException ex) {
            RuntimeException actual = handlePasswordLoginFailure(user, ex);
            recordLoginAttempt(user, username, LOGIN_RESULT_FAIL, resolveFriendlyMessage(actual, "登录失败"));
            throw actual;
        }
    }

    @Override
    public MobileLoginOptionsVO getMobileLoginOptions() {
        MobileLoginOptionsVO result = new MobileLoginOptionsVO();
        result.setPasswordLoginEnabled(isMobilePasswordLoginEnabledForCurrentRequest());
        return result;
    }

    @Override
    public LoginVO wechatMiniLogin(WechatMiniLoginRequest request) {
        UserEntity user = null;
        try {
            WechatMiniIdentity identity = exchangeWechatMiniCode(request.getCode());
            user = resolveWechatLoginUser(identity);
            validateUserStatus(user);

            LoginUser loginUser = toLoginUser(user);
            operationLogService.log("AUTH", "WECHAT_MINI_LOGIN", user.getId(), "微信小程序登录成功：" + user.getUsername());
            LoginVO loginVO = buildLoginVO(loginUser);
            applyForcePasswordChangeFlag(loginVO, user);
            recordLoginAttempt(user, user.getUsername(), LOGIN_RESULT_SUCCESS, null);
            return loginVO;
        } catch (RuntimeException ex) {
            recordLoginAttempt(user, user == null ? null : user.getUsername(), LOGIN_RESULT_FAIL, resolveFriendlyMessage(ex, "微信小程序登录失败"));
            throw ex;
        }
    }

    @Override
    public LoginVO wechatMpLogin(String code) {
        UserEntity user = null;
        try {
            WechatMpAuthService.WechatMpIdentity identity = wechatMpAuthService.exchangeCode(code);
            String openId = normalizeText(identity.openId());
            String unionId = normalizeText(identity.unionId());
            user = resolveWechatMpLoginUser(openId, unionId);
            if (user == null) {
                throw new BadCredentialsException("当前微信未绑定系统账号，请联系管理员先完成绑定");
            }

            syncWechatIdentityIfNecessary(user, openId, unionId);
            validateUserStatus(user);

            LoginUser loginUser = toLoginUser(user);
            operationLogService.log("AUTH", "WECHAT_MP_LOGIN", user.getId(), "微信公众号 H5 登录成功：" + user.getUsername());
            LoginVO loginVO = buildLoginVO(loginUser);
            applyForcePasswordChangeFlag(loginVO, user);
            recordLoginAttempt(user, user.getUsername(), LOGIN_RESULT_SUCCESS, null);
            return loginVO;
        } catch (RuntimeException ex) {
            recordLoginAttempt(user, user == null ? null : user.getUsername(), LOGIN_RESULT_FAIL, resolveFriendlyMessage(ex, "微信公众号登录失败"));
            throw ex;
        }
    }

    @Override
    public WechatMpAuthorizeUrlVO buildWechatMpAuthorizeUrl(String returnUrl, String state) {
        return wechatMpAuthService.buildAuthorizeUrl(returnUrl, state);
    }

    @Override
    public void changePassword(ChangePasswordRequest request) {
        LoginUser loginUser = currentLoginUser();
        UserEntity user = requireUser(loginUser.getUserId());
        validateUserStatus(user);

        String oldPassword = requireText(request.getOldPassword(), "原密码不能为空");
        String newPassword = requireText(request.getNewPassword(), "新密码不能为空");
        String confirmPassword = requireText(request.getConfirmPassword(), "确认新密码不能为空");

        if (!matchesPassword(user, oldPassword)) {
            throw new IllegalArgumentException("原密码错误");
        }
        if (!newPassword.equals(confirmPassword)) {
            throw new IllegalArgumentException("新密码与确认新密码不一致");
        }
        if (matchesPassword(user, newPassword)) {
            throw new IllegalArgumentException("新密码不能与原密码相同");
        }

        PasswordPolicyValidator.validateOrThrow(newPassword);
        String salt = sm3PasswordCodec.generateSalt();
        int updated = userMapper.updatePassword(
                user.getId(),
                sm3PasswordCodec.encode(newPassword, salt),
                PASSWORD_ALGO_SM3,
                salt,
                Boolean.FALSE,
                loginUser.getUsername(),
                LocalDateTime.now()
        );
        if (updated <= 0) {
            throw new IllegalStateException("密码修改失败，请稍后重试");
        }
        operationLogService.log("AUTH", "CHANGE_PASSWORD", user.getId(), "用户修改了自己的登录密码");
    }

    @Override
    public String handleWechatMpCallback(String code, String state, String error, String errorDescription) {
        WechatMpAuthService.WechatMpCallbackState callbackState = wechatMpAuthService.parseCallbackState(state);
        String normalizedError = normalizeText(error);
        if (normalizedError != null) {
            String message = normalizeText(errorDescription);
            return wechatMpAuthService.buildFailureCallbackRedirect(
                    callbackState,
                    WechatMpAuthService.CALLBACK_ERROR_CODE_OAUTH_DENIED,
                    message == null ? "公众号授权失败：" + normalizedError : message
            );
        }
        if (normalizeText(code) == null) {
            return wechatMpAuthService.buildFailureCallbackRedirect(
                    callbackState,
                    WechatMpAuthService.CALLBACK_ERROR_CODE_CODE_MISSING,
                    "公众号授权回调缺少 code"
            );
        }
        try {
            LoginVO loginVO = wechatMpLogin(code);
            return wechatMpAuthService.buildSuccessCallbackRedirect(loginVO, callbackState);
        } catch (Exception ex) {
            String message = resolveFriendlyMessage(ex, "公众号登录失败，请稍后重试");
            String errorCode = resolveWechatCallbackErrorCode(ex);
            log.warn("公众号 H5 callback 登录失败，errorCode={}, message={}", errorCode, message, ex);
            return wechatMpAuthService.buildFailureCallbackRedirect(callbackState, errorCode, message);
        }
    }

    @Override
    public WechatMiniIdentity exchangeWechatMiniCode(String code) {
        return wechatMiniAuthService.exchangeCode(code);
    }

    private void validatePasswordUser(UserEntity user, String rawPassword) {
        if (user == null) {
            throw new BadCredentialsException("用户名或密码错误");
        }
        if (isLoginLocked(user, LocalDateTime.now())) {
            throw new BadCredentialsException("账号已锁定30分钟，请稍后再试");
        }
        if (!matchesPassword(user, rawPassword)) {
            throw new BadCredentialsException("用户名或密码错误");
        }
        validateUserStatus(user);
    }

    private void validateMobilePasswordLoginPolicy() {
        HttpServletRequest request = currentRequest();
        if (!isMobileRequest(request) || isMobilePasswordLoginEnabledForCurrentRequest()) {
            return;
        }
        throw new IllegalArgumentException("当前环境未开启手机端账号密码登录，请使用微信登录");
    }

    private boolean isMobilePasswordLoginEnabledForCurrentRequest() {
        return isMobilePasswordLoginEnabled() || isWhitelistedTestLoginRequest(currentRequest());
    }

    private boolean isMobilePasswordLoginEnabled() {
        return authProperties != null
                && authProperties.getMobile() != null
                && authProperties.getMobile().getPasswordLogin() != null
                && authProperties.getMobile().getPasswordLogin().isEnabled();
    }

    private boolean isMobileRequest(HttpServletRequest request) {
        String userAgent = normalizeText(resolveUserAgent(request));
        return userAgent != null && MOBILE_USER_AGENT_PATTERN.matcher(userAgent).find();
    }

    private boolean isWhitelistedTestLoginRequest(HttpServletRequest request) {
        if (request == null) {
            return false;
        }
        String origin = normalizeRequestOrigin(request);
        if (TEST_LOGIN_ORIGIN.equals(origin)) {
            return true;
        }
        String referer = firstHeaderValue(request.getHeader("Referer"));
        if (referer != null && referer.startsWith(TEST_LOGIN_ORIGIN)) {
            return true;
        }
        String host = normalizeRequestHost(request);
        return TEST_LOGIN_HOST.equals(host);
    }

    private String normalizeRequestOrigin(HttpServletRequest request) {
        String origin = firstHeaderValue(request.getHeader("Origin"));
        if (origin != null) {
            return origin;
        }
        String scheme = firstHeaderValue(request.getHeader("X-Forwarded-Proto"));
        if (scheme == null) {
            scheme = normalizeText(request.getScheme());
        }
        String host = normalizeRequestHost(request);
        if (scheme == null || host == null) {
            return null;
        }
        return scheme + "://" + host;
    }

    private String normalizeRequestHost(HttpServletRequest request) {
        String forwardedHost = firstHeaderValue(request.getHeader("X-Forwarded-Host"));
        if (forwardedHost != null) {
            return forwardedHost;
        }
        String hostHeader = firstHeaderValue(request.getHeader("Host"));
        if (hostHeader != null) {
            return hostHeader;
        }
        String serverName = normalizeText(request.getServerName());
        if (serverName == null) {
            return null;
        }
        int serverPort = request.getServerPort();
        boolean defaultHttpPort = "http".equalsIgnoreCase(request.getScheme()) && serverPort == 80;
        boolean defaultHttpsPort = "https".equalsIgnoreCase(request.getScheme()) && serverPort == 443;
        if (serverPort <= 0 || defaultHttpPort || defaultHttpsPort) {
            return serverName;
        }
        return serverName + ":" + serverPort;
    }

    private String firstHeaderValue(String headerValue) {
        String normalized = normalizeText(headerValue);
        if (normalized == null) {
            return null;
        }
        int commaIndex = normalized.indexOf(',');
        if (commaIndex >= 0) {
            return normalizeText(normalized.substring(0, commaIndex));
        }
        return normalized;
    }

    private boolean matchesPassword(UserEntity user, String rawPassword) {
        String passwordAlgo = normalizeText(user.getPasswordAlgo());
        if (PASSWORD_ALGO_SM3.equalsIgnoreCase(passwordAlgo)) {
            return sm3PasswordCodec.matches(rawPassword, user.getPasswordSalt(), user.getPasswordHash());
        }
        return passwordAlgo == null
                || PASSWORD_ALGO_BCRYPT.equalsIgnoreCase(passwordAlgo)
                ? passwordEncoder.matches(rawPassword, user.getPasswordHash())
                : false;
    }

    private RuntimeException handlePasswordLoginFailure(UserEntity user, RuntimeException ex) {
        if (!(ex instanceof BadCredentialsException) || user == null) {
            return ex;
        }

        LocalDateTime now = LocalDateTime.now();
        if (isLoginLocked(user, now)) {
            return new BadCredentialsException("账号已锁定30分钟，请稍后再试");
        }

        int currentFailCount = isLockExpired(user, now) ? 0 : safeFailCount(user.getLoginFailCount());
        int nextFailCount = currentFailCount + 1;
        LocalDateTime lockUntil = nextFailCount >= MAX_LOGIN_FAIL_COUNT
                ? now.plusMinutes(LOGIN_LOCK_MINUTES)
                : null;

        try {
            userMapper.updateLoginSecurityState(
                    user.getId(),
                    nextFailCount,
                    lockUntil,
                    user.getUsername(),
                    now
            );
            user.setLoginFailCount(nextFailCount);
            user.setLockUntil(lockUntil);
        } catch (Exception updateEx) {
            log.warn("Failed to update login security state for user {}", user.getUsername(), updateEx);
        }

        if (lockUntil != null) {
            return new BadCredentialsException("连续输入错误5次，账号已锁定30分钟");
        }
        return ex;
    }

    private void resetLoginSecurityState(UserEntity user) {
        if (user == null) {
            return;
        }
        if (safeFailCount(user.getLoginFailCount()) <= 0 && user.getLockUntil() == null) {
            return;
        }

        try {
            userMapper.updateLoginSecurityState(
                    user.getId(),
                    0,
                    null,
                    user.getUsername(),
                    LocalDateTime.now()
            );
            user.setLoginFailCount(0);
            user.setLockUntil(null);
        } catch (Exception ex) {
            log.warn("Failed to reset login security state for user {}", user.getUsername(), ex);
        }
    }

    private UserEntity requireUser(Long userId) {
        UserEntity user = userMapper.findById(userId);
        if (user == null) {
            throw new IllegalArgumentException("当前登录用户不存在");
        }
        return user;
    }

    private LoginUser currentLoginUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof LoginUser loginUser)) {
            throw new IllegalArgumentException("当前未登录");
        }
        return loginUser;
    }

    private void validateUserStatus(UserEntity user) {
        if (user == null) {
            throw new BadCredentialsException("当前微信未绑定系统账号，请先在组织架构管理中维护微信绑定");
        }
        if (!Integer.valueOf(1).equals(user.getStatus())) {
            throw new DisabledException("用户已停用");
        }
    }

    private UserEntity resolveWechatLoginUser(WechatMiniIdentity identity) {
        UserEntity user = null;
        String openId = identity.openId();
        if (openId != null) {
            user = userMapper.findByWechatOpenId(openId);
        }
        if (user == null && identity.unionId() != null) {
            user = userMapper.findByWechatUnionId(identity.unionId());
        }
        return user;
    }

    private UserEntity resolveWechatMpLoginUser(String openId, String unionId) {
        UserEntity user = null;
        if (unionId != null) {
            user = userMapper.findByWechatUnionId(unionId);
        }
        if (user == null && openId != null) {
            user = userMapper.findByWechatOpenId(openId);
        }
        return user;
    }

    private void syncWechatIdentityIfNecessary(UserEntity user, String openId, String unionId) {
        String currentOpenId = normalizeText(user.getWechatOpenId());
        String currentUnionId = normalizeText(user.getWechatUnionId());

        if (currentUnionId != null && unionId != null && !currentUnionId.equals(unionId)) {
            throw new IllegalStateException("当前系统账号绑定的微信 unionid 与本次授权不一致，请联系管理员核实绑定关系");
        }

        String nextOpenId = currentOpenId;
        String nextUnionId = currentUnionId;
        boolean changed = false;

        if (nextUnionId == null && unionId != null) {
            assertWechatIdentityAvailableForUser(user.getId(), null, unionId);
            nextUnionId = unionId;
            changed = true;
        }

        if (nextOpenId == null && openId != null) {
            assertWechatIdentityAvailableForUser(user.getId(), openId, null);
            nextOpenId = openId;
            changed = true;
        }

        if (!changed) {
            return;
        }

        int updated = userMapper.updateWechatBinding(
                user.getId(),
                nextOpenId,
                nextUnionId,
                WECHAT_MP_BINDING_OPERATOR,
                LocalDateTime.now()
        );
        if (updated <= 0) {
            throw new IllegalStateException("同步微信公众号绑定信息失败，请稍后重试");
        }
        user.setWechatOpenId(nextOpenId);
        user.setWechatUnionId(nextUnionId);
    }

    private void assertWechatIdentityAvailableForUser(Long userId, String openId, String unionId) {
        if (openId != null) {
            UserEntity openIdOwner = userMapper.findByWechatOpenId(openId);
            if (openIdOwner != null && !openIdOwner.getId().equals(userId)) {
                throw new IllegalStateException("当前微信公众号 openid 已绑定其他系统账号，请联系管理员处理");
            }
        }
        if (unionId != null) {
            UserEntity unionIdOwner = userMapper.findByWechatUnionId(unionId);
            if (unionIdOwner != null && !unionIdOwner.getId().equals(userId)) {
                throw new IllegalStateException("当前微信公众号 unionid 已绑定其他系统账号，请联系管理员处理");
            }
        }
    }

    private LoginUser toLoginUser(UserEntity user) {
        return new LoginUser(user.getId(), user.getUsername(), user.getRealName(), user.getRole());
    }

    private LoginVO buildLoginVO(LoginUser loginUser) {
        LoginVO vo = LoginVO.fromLoginUser(loginUser);
        vo.setToken(jwtTokenService.generateToken(loginUser));
        return vo;
    }

    private void applyForcePasswordChangeFlag(LoginVO loginVO, UserEntity user) {
        loginVO.setForcePasswordChange(Boolean.TRUE.equals(user.getForcePasswordChange()));
    }

    private void recordLoginAttempt(UserEntity user, String username, String loginResult, String failReason) {
        recordLoginAttempt(user, username, loginResult, resolveLoginType(), failReason);
    }

    private void recordLoginAttempt(UserEntity user, String username, String loginResult, String loginType, String failReason) {
        HttpServletRequest request = currentRequest();
        loginLogService.record(
                user == null ? null : user.getId(),
                truncate(normalizeText(username), 64),
                truncate(resolveClientIp(request), 64),
                encodeLoginResult(loginResult, loginType),
                truncate(normalizeText(failReason), 255),
                truncate(normalizeText(resolveUserAgent(request)), 500)
        );
    }

    private String resolveLoginType() {
        for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
            String methodName = element.getMethodName();
            if ("wechatMiniLogin".equals(methodName) || "wechatMpLogin".equals(methodName)) {
                return LOGIN_TYPE_WECHAT;
            }
            if ("login".equals(methodName)) {
                return LOGIN_TYPE_PASSWORD;
            }
        }
        return LOGIN_TYPE_PASSWORD;
    }

    private String encodeLoginResult(String loginResult, String loginType) {
        String normalizedResult = normalizeText(loginResult);
        String normalizedType = normalizeText(loginType);
        if (normalizedResult == null) {
            return normalizedType;
        }
        if (normalizedType == null) {
            return normalizedResult;
        }
        return normalizedResult + "#" + normalizedType;
    }

    private HttpServletRequest currentRequest() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return null;
        }
        return attributes.getRequest();
    }

    private String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String forwardedFor = normalizeText(request.getHeader("X-Forwarded-For"));
        if (forwardedFor != null) {
            int index = forwardedFor.indexOf(',');
            return index >= 0 ? forwardedFor.substring(0, index).trim() : forwardedFor;
        }
        String realIp = normalizeText(request.getHeader("X-Real-IP"));
        if (realIp != null) {
            return realIp;
        }
        return normalizeText(request.getRemoteAddr());
    }

    private String resolveUserAgent(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        return request.getHeader("User-Agent");
    }

    private boolean isLoginLocked(UserEntity user, LocalDateTime now) {
        return user.getLockUntil() != null && user.getLockUntil().isAfter(now);
    }

    private boolean isLockExpired(UserEntity user, LocalDateTime now) {
        return user.getLockUntil() != null && !user.getLockUntil().isAfter(now);
    }

    private int safeFailCount(Integer loginFailCount) {
        return loginFailCount == null ? 0 : Math.max(loginFailCount, 0);
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String requireText(String value, String message) {
        String normalized = normalizeText(value);
        if (normalized == null) {
            throw new IllegalArgumentException(message);
        }
        return normalized;
    }

    private String resolveFriendlyMessage(Exception ex, String fallback) {
        String message = ex.getMessage();
        if (message == null) {
            return fallback;
        }
        String trimmed = message.trim();
        return trimmed.isEmpty() ? fallback : trimmed;
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private String resolveWechatCallbackErrorCode(Exception ex) {
        String message = resolveFriendlyMessage(ex, "");
        if (message.contains("未绑定系统账号")) {
            return WechatMpAuthService.CALLBACK_ERROR_CODE_UNBOUND;
        }
        if (message.contains("暂未启用")) {
            return WechatMpAuthService.CALLBACK_ERROR_CODE_NOT_ENABLED;
        }
        if (message.contains("配置缺少")) {
            return WechatMpAuthService.CALLBACK_ERROR_CODE_CONFIG_MISSING;
        }
        if (message.contains("code 无效") || message.contains("code") && message.contains("过期")) {
            return WechatMpAuthService.CALLBACK_ERROR_CODE_CODE_INVALID;
        }
        if (message.contains("openid 已绑定其他系统账号")
                || message.contains("unionid 已绑定其他系统账号")
                || message.contains("绑定的微信 unionid 与本次授权不一致")) {
            return WechatMpAuthService.CALLBACK_ERROR_CODE_BINDING_CONFLICT;
        }
        if (ex instanceof DisabledException || message.contains("用户已停用")) {
            return WechatMpAuthService.CALLBACK_ERROR_CODE_USER_DISABLED;
        }
        return WechatMpAuthService.CALLBACK_ERROR_CODE_UNKNOWN;
    }
}
