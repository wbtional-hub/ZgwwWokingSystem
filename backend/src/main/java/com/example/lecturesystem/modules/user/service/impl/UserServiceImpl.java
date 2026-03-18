package com.example.lecturesystem.modules.user.service.impl;

import com.example.lecturesystem.modules.auth.security.LoginUser;
import com.example.lecturesystem.modules.operationlog.service.OperationLogService;
import com.example.lecturesystem.modules.permission.support.DataScopeService;
import com.example.lecturesystem.modules.user.dto.CreateUserRequest;
import com.example.lecturesystem.modules.user.dto.UserQueryRequest;
import com.example.lecturesystem.modules.user.dto.UpdateUserRequest;
import com.example.lecturesystem.modules.user.entity.UserEntity;
import com.example.lecturesystem.modules.user.mapper.UserMapper;
import com.example.lecturesystem.modules.user.service.UserService;
import com.example.lecturesystem.modules.user.vo.UserDetailVO;
import com.example.lecturesystem.modules.user.vo.UserPageVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class UserServiceImpl implements UserService {
    private static final String DEFAULT_PASSWORD = "123456";

    private final UserMapper userMapper;
    private final OperationLogService operationLogService;
    private final DataScopeService dataScopeService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserServiceImpl(UserMapper userMapper) {
        this(userMapper, new OperationLogService() {
            @Override
            public void log(String moduleName, String actionName, Long bizId, String content) {
            }

            @Override
            public Object query(com.example.lecturesystem.modules.operationlog.dto.OperationLogQueryRequest request) {
                return java.util.List.of();
            }
        }, new DataScopeService());
    }

    @Autowired
    public UserServiceImpl(UserMapper userMapper, OperationLogService operationLogService, DataScopeService dataScopeService) {
        this.userMapper = userMapper;
        this.operationLogService = operationLogService;
        this.dataScopeService = dataScopeService;
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
        entity.setPasswordHash(passwordEncoder.encode(request.getPassword().trim()));
        entity.setRealName(request.getRealName().trim());
        entity.setJobTitle(trimToNull(request.getJobTitle()));
        entity.setMobile(trimToNull(request.getMobile()));
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
        userMapper.updatePassword(
                userId,
                passwordEncoder.encode(DEFAULT_PASSWORD),
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
