package com.example.lecturesystem.modules.user.service.impl;

import com.example.lecturesystem.modules.auth.security.LoginUser;
import com.example.lecturesystem.modules.auth.service.AuthService;
import com.example.lecturesystem.modules.auth.support.PasswordPolicyValidator;
import com.example.lecturesystem.modules.auth.support.Sm3PasswordCodec;
import com.example.lecturesystem.modules.operationlog.service.OperationLogService;
import com.example.lecturesystem.modules.permission.support.DataScopeService;
import com.example.lecturesystem.modules.user.dto.BindWechatMiniRequest;
import com.example.lecturesystem.modules.user.dto.CreateUserRequest;
import com.example.lecturesystem.modules.user.dto.UpdateUserRequest;
import com.example.lecturesystem.modules.user.dto.UserQueryRequest;
import com.example.lecturesystem.modules.user.entity.UserEntity;
import com.example.lecturesystem.modules.user.mapper.UserMapper;
import com.example.lecturesystem.modules.user.service.UserService;
import com.example.lecturesystem.modules.user.vo.UserDetailVO;
import com.example.lecturesystem.modules.user.vo.UserPageVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class UserServiceImpl implements UserService {
    private static final String DEFAULT_PASSWORD = "Admin2026";
    private static final String PASSWORD_ALGO_SM3 = "SM3";

    private final UserMapper userMapper;
    private final OperationLogService operationLogService;
    private final DataScopeService dataScopeService;
    private final AuthService authService;
    private final String defaultPassword;
    private final Sm3PasswordCodec sm3PasswordCodec = new Sm3PasswordCodec();

    public UserServiceImpl(UserMapper userMapper) {
        this(userMapper, new OperationLogService() {
            @Override
            public void log(String moduleName, String actionName, Long bizId, String content) {
            }

            @Override
            public Object query(com.example.lecturesystem.modules.operationlog.dto.OperationLogQueryRequest request) {
                return java.util.List.of();
            }
        }, new DataScopeService(), new AuthService() {
            @Override
            public com.example.lecturesystem.modules.auth.vo.LoginVO login(com.example.lecturesystem.modules.auth.dto.LoginRequest request) {
                throw new UnsupportedOperationException("Not implemented in tests");
            }

            @Override
            public com.example.lecturesystem.modules.auth.vo.LoginVO wechatMiniLogin(com.example.lecturesystem.modules.auth.dto.WechatMiniLoginRequest request) {
                throw new UnsupportedOperationException("Not implemented in tests");
            }

            @Override
            public WechatMiniIdentity exchangeWechatMiniCode(String code) {
                throw new UnsupportedOperationException("Not implemented in tests");
            }
        }, DEFAULT_PASSWORD);
    }

    @Autowired
    public UserServiceImpl(UserMapper userMapper,
                           OperationLogService operationLogService,
                           DataScopeService dataScopeService,
                           AuthService authService) {
        this(userMapper, operationLogService, dataScopeService, authService, DEFAULT_PASSWORD);
    }

    UserServiceImpl(UserMapper userMapper,
                    OperationLogService operationLogService,
                    DataScopeService dataScopeService,
                    AuthService authService,
                    String defaultPassword) {
        this.userMapper = userMapper;
        this.operationLogService = operationLogService;
        this.dataScopeService = dataScopeService;
        this.authService = authService;
        this.defaultPassword = defaultPassword;
    }

    @Override
    public Object queryPage(UserQueryRequest request) {
        normalizePage(request);
        LoginUser loginUser = currentLoginUser();
        boolean admin = loginUser.isAdmin();
        String treePathPrefix = null;
        UserEntity currentUser = null;
        if (!admin) {
            currentUser = requireUser(loginUser.getUserId());
            treePathPrefix = dataScopeService.buildTreePathPrefix(currentUser);
        }
        long total = admin ? userMapper.countPage(request) : userMapper.countPageByTreePath(treePathPrefix, request);
        UserPageVO result = new UserPageVO();
        result.setPageNo(request.getPageNo());
        result.setPageSize(request.getPageSize());
        result.setTotal(total);
        result.setList(admin ? userMapper.queryPage(request) : userMapper.queryPageByTreePath(treePathPrefix, request));
        long scopeUserCount = admin ? userMapper.countPage(new UserQueryRequest()) : userMapper.countPageByTreePath(treePathPrefix, new UserQueryRequest());
        result.setScopeUserCount(scopeUserCount);
        result.setScopeType((admin ? com.example.lecturesystem.modules.permission.support.DataScopeType.CUSTOM : dataScopeService.resolveScopeType(currentUser)).name());
        result.setScopeDescription(admin ? "当前可查看全部用户，共 " + scopeUserCount + " 人" : dataScopeService.describeScope(currentUser, scopeUserCount));
        return result;
    }

    @Override
    @Transactional
    public Long createUser(CreateUserRequest request) {
        requireAdmin();
        if (userMapper.findByUsername(request.getUsername()) != null) {
            throw new IllegalArgumentException("登录账号已存在");
        }

        String operator = currentOperator();
        LocalDateTime now = LocalDateTime.now();
        UserEntity entity = new UserEntity();
        entity.setUnitId(request.getUnitId());
        entity.setUsername(request.getUsername().trim());
        String rawPassword = request.getPassword() == null ? null : request.getPassword().trim();
        PasswordPolicyValidator.validateOrThrow(rawPassword);
        String salt = sm3PasswordCodec.generateSalt();
        entity.setPasswordHash(sm3PasswordCodec.encode(rawPassword, salt));
        entity.setPasswordAlgo(PASSWORD_ALGO_SM3);
        entity.setPasswordSalt(salt);
        entity.setRealName(request.getRealName().trim());
        entity.setJobTitle(trimToNull(request.getJobTitle()));
        entity.setMobile(trimToNull(request.getMobile()));
        entity.setWechatNo(trimToNull(request.getWechatNo()));
        entity.setWechatOpenId(trimToNull(request.getWechatOpenId()));
        entity.setWechatUnionId(trimToNull(request.getWechatUnionId()));
        entity.setRole("USER");
        entity.setStatus(request.getStatus());
        entity.setCreateTime(now);
        entity.setUpdateTime(now);
        entity.setCreateUser(operator);
        entity.setUpdateUser(operator);
        entity.setIsDeleted(Boolean.FALSE);
        userMapper.insertUser(entity);
        operationLogService.log(
                "USER",
                "CREATE",
                entity.getId(),
                "新增用户：" + entity.getRealName() + "（" + entity.getUsername() + "），单位ID=" + entity.getUnitId()
        );
        return entity.getId();
    }

    @Override
    public Object detail(Long userId) {
        LoginUser loginUser = currentLoginUser();
        UserDetailVO detail;
        if (loginUser.isAdmin()) {
            detail = userMapper.detailById(userId);
        } else {
            UserEntity currentUser = requireUser(loginUser.getUserId());
            detail = userMapper.detailByIdAndTreePath(userId, dataScopeService.buildTreePathPrefix(currentUser));
            if (detail == null) {
                throw new IllegalArgumentException("只能查看自己及下级的用户信息");
            }
        }
        if (detail == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        operationLogService.log(
                "DATA_ACCESS",
                "VIEW_USER_DETAIL",
                userId,
                "用户 " + currentOperator() + " 查看了用户 " + (detail.getRealName() == null ? detail.getUsername() : detail.getRealName()) + " 的资料"
        );
        return detail;
    }

    @Override
    @Transactional
    public void updateUser(UpdateUserRequest request) {
        requireAdmin();
        UserEntity existed = requireUser(request.getId());

        UserEntity entity = new UserEntity();
        entity.setId(request.getId());
        entity.setUnitId(existed.getUnitId());
        entity.setRealName(request.getRealName().trim());
        entity.setJobTitle(trimToNull(request.getJobTitle()));
        entity.setMobile(trimToNull(request.getMobile()));
        entity.setWechatNo(trimToNull(request.getWechatNo()));
        entity.setWechatOpenId(trimToNull(request.getWechatOpenId()));
        entity.setWechatUnionId(trimToNull(request.getWechatUnionId()));
        entity.setStatus(request.getStatus());
        entity.setUpdateTime(LocalDateTime.now());
        entity.setUpdateUser(currentOperator());
        userMapper.updateUser(entity);
        operationLogService.log(
                "USER",
                "UPDATE",
                entity.getId(),
                "编辑用户：" + existed.getRealName() + " -> " + entity.getRealName() + "，状态=" + entity.getStatus()
        );
    }

    @Override
    @Transactional
    public Object bindWechatMini(BindWechatMiniRequest request) {
        requireAdmin();
        UserEntity targetUser = requireUser(request.getUserId());

        AuthService.WechatMiniIdentity identity = authService.exchangeWechatMiniCode(request.getCode());
        String openId = trimToNull(identity.openId());
        String unionId = trimToNull(identity.unionId());
        if (openId == null) {
            throw new IllegalArgumentException("微信授权结果缺少 openid，无法完成绑定");
        }

        UserEntity openIdOwner = userMapper.findByWechatOpenId(openId);
        if (openIdOwner != null && !openIdOwner.getId().equals(targetUser.getId())) {
            throw new IllegalArgumentException("该微信小程序 openid 已被其他用户绑定，禁止覆盖");
        }

        if (unionId != null) {
            UserEntity unionIdOwner = userMapper.findByWechatUnionId(unionId);
            if (unionIdOwner != null && !unionIdOwner.getId().equals(targetUser.getId())) {
                throw new IllegalArgumentException("该微信小程序 unionid 已被其他用户绑定，禁止覆盖");
            }
        }

        int updated = userMapper.updateWechatBinding(
                targetUser.getId(),
                openId,
                unionId,
                currentOperator(),
                LocalDateTime.now()
        );
        if (updated <= 0) {
            throw new IllegalStateException("微信小程序绑定失败，请稍后重试");
        }

        operationLogService.log(
                "USER",
                "BIND_WECHAT_MINI",
                targetUser.getId(),
                "管理员为用户 " + targetUser.getUsername() + " 绑定微信小程序账号"
        );

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("userId", targetUser.getId());
        result.put("openIdBound", Boolean.TRUE);
        result.put("unionIdPresent", unionId != null);
        return result;
    }

    @Override
    @Transactional
    public void deleteUser(Long userId) {
        requireAdmin();
        UserEntity targetUser = requireUser(userId);
        userMapper.logicalDelete(userId, currentOperator(), LocalDateTime.now());
        operationLogService.log("USER", "DELETE", userId, "删除用户：" + targetUser.getRealName() + "（" + targetUser.getUsername() + "）");
    }

    @Override
    @Transactional
    public void resetPassword(Long userId) {
        requireAdmin();
        requireUser(userId);
        PasswordPolicyValidator.validateOrThrow(defaultPassword);
        String salt = sm3PasswordCodec.generateSalt();
        userMapper.updatePassword(
                userId,
                sm3PasswordCodec.encode(defaultPassword, salt),
                PASSWORD_ALGO_SM3,
                salt,
                Boolean.TRUE,
                currentOperator(),
                LocalDateTime.now()
        );
    }

    private void normalizePage(UserQueryRequest request) {
        if (request.getPageNo() == null || request.getPageNo() < 1) {
            request.setPageNo(1);
        }
        if (request.getPageSize() == null || request.getPageSize() < 1) {
            request.setPageSize(10);
        }
        if (request.getPageSize() > 100) {
            request.setPageSize(100);
        }
        if (request.getKeywords() != null) {
            String trimmed = request.getKeywords().trim();
            request.setKeywords(trimmed.isEmpty() ? null : trimmed);
        }
    }

    private UserEntity requireUser(Long userId) {
        UserEntity user = userMapper.findById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
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

    private String currentOperator() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof LoginUser loginUser) {
            return loginUser.getUsername();
        }
        return "system";
    }

    private void requireAdmin() {
        if (!currentLoginUser().isAdmin()) {
            throw new IllegalArgumentException("仅管理员可执行该操作");
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
