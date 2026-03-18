package com.example.lecturesystem.modules.weeklywork.service.impl;

import com.example.lecturesystem.modules.auth.security.LoginUser;
import com.example.lecturesystem.modules.operationlog.service.OperationLogService;
import com.example.lecturesystem.modules.permission.support.CurrentUserFacade;
import com.example.lecturesystem.modules.permission.support.DataScopeService;
import com.example.lecturesystem.modules.weeklywork.dto.ReviewWeeklyWorkRequest;
import com.example.lecturesystem.modules.weeklywork.dto.SaveWeeklyWorkRequest;
import com.example.lecturesystem.modules.weeklywork.dto.SubmitWeeklyWorkRequest;
import com.example.lecturesystem.modules.weeklywork.dto.WeeklyWorkQueryRequest;
import com.example.lecturesystem.modules.weeklywork.entity.WeeklyWorkEntity;
import com.example.lecturesystem.modules.weeklywork.mapper.WeeklyWorkMapper;
import com.example.lecturesystem.modules.weeklywork.service.WeeklyWorkService;
import com.example.lecturesystem.modules.permission.service.PermissionService;
import com.example.lecturesystem.modules.user.entity.UserEntity;
import com.example.lecturesystem.modules.user.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

@Service
public class WeeklyWorkServiceImpl implements WeeklyWorkService {
    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_SUBMITTED = "SUBMITTED";
    private static final String STATUS_RETURNED = "RETURNED";
    private static final String STATUS_APPROVED = "APPROVED";

    private final WeeklyWorkMapper weeklyWorkMapper;
    private final PermissionService permissionService;
    private final UserMapper userMapper;
    private final OperationLogService operationLogService;
    private final CurrentUserFacade currentUserFacade;
    private final DataScopeService dataScopeService;

    public WeeklyWorkServiceImpl(WeeklyWorkMapper weeklyWorkMapper,
                                 PermissionService permissionService,
                                 UserMapper userMapper) {
        this(weeklyWorkMapper, permissionService, userMapper, new OperationLogService() {
            @Override
            public void log(String moduleName, String actionName, Long bizId, String content) {
            }

            @Override
            public Object query(com.example.lecturesystem.modules.operationlog.dto.OperationLogQueryRequest request) {
                return java.util.List.of();
            }
        }, null, new DataScopeService());
    }

    @Autowired
    public WeeklyWorkServiceImpl(WeeklyWorkMapper weeklyWorkMapper,
                                 PermissionService permissionService,
                                 UserMapper userMapper,
                                 OperationLogService operationLogService,
                                 CurrentUserFacade currentUserFacade,
                                 DataScopeService dataScopeService) {
        this.weeklyWorkMapper = weeklyWorkMapper;
        this.permissionService = permissionService;
        this.userMapper = userMapper;
        this.operationLogService = operationLogService;
        this.currentUserFacade = currentUserFacade;
        this.dataScopeService = dataScopeService;
    }

    @Override
    @Transactional
    public Long saveDraft(SaveWeeklyWorkRequest request) {
        LoginUser loginUser = currentLoginUser();
        UserEntity currentUser = requireCurrentUser(loginUser.getUserId());
        WeeklyWorkEntity existed = weeklyWorkMapper.findByUserIdAndWeekNo(loginUser.getUserId(), request.getWeekNo());

        if (existed == null) {
            WeeklyWorkEntity entity = new WeeklyWorkEntity();
            entity.setUnitId(currentUser.getUnitId());
            entity.setUserId(loginUser.getUserId());
            entity.setWeekNo(request.getWeekNo());
            entity.setStatus(STATUS_DRAFT);
            entity.setWorkPlan(request.getWorkPlan());
            entity.setWorkContent(request.getWorkContent());
            entity.setRemark(request.getRemark());
            weeklyWorkMapper.insert(entity);
            return entity.getId();
        }

        if (!STATUS_DRAFT.equals(existed.getStatus()) && !STATUS_RETURNED.equals(existed.getStatus())) {
            throw new IllegalArgumentException("当前周工作已提交，不能直接覆盖草稿");
        }

        existed.setWorkPlan(request.getWorkPlan());
        existed.setWorkContent(request.getWorkContent());
        existed.setRemark(request.getRemark());
        existed.setStatus(STATUS_DRAFT);
        weeklyWorkMapper.updateDraft(existed);
        return existed.getId();
    }

    @Override
    @Transactional
    public Object submit(SubmitWeeklyWorkRequest request) {
        LoginUser loginUser = currentLoginUser();
        WeeklyWorkEntity entity = requireWeeklyWork(request.getId());

        if (!loginUser.getUserId().equals(entity.getUserId())) {
            throw new IllegalArgumentException("只能提交本人的周工作");
        }
        if (!STATUS_DRAFT.equals(entity.getStatus()) && !STATUS_RETURNED.equals(entity.getStatus())) {
            throw new IllegalArgumentException("当前状态不允许提交");
        }

        weeklyWorkMapper.markSubmitted(entity.getId(), LocalDateTime.now());
        return "ok";
    }

    @Override
    public Object query(WeeklyWorkQueryRequest request) {
        WeeklyWorkQueryRequest normalizedRequest = request == null ? new WeeklyWorkQueryRequest() : request;
        LoginUser loginUser = currentLoginUser();
        if (!permissionService.isSuperAdmin(loginUser.getUserId())) {
            dataScopeService.injectTreePathScope(normalizedRequest, requireCurrentUser(loginUser.getUserId()));
        }

        if (!permissionService.isSuperAdmin(loginUser.getUserId()) && normalizedRequest.getUserId() != null) {
            UserEntity targetUser = requireCurrentUser(normalizedRequest.getUserId());
            dataScopeService.validateReadableUser(requireCurrentUser(loginUser.getUserId()), targetUser, "无权查看指定用户的周工作");
        }

        return weeklyWorkMapper.queryList(normalizedRequest);
    }

    @Override
    public Object detail(Long id) {
        LoginUser loginUser = currentLoginUser();
        WeeklyWorkEntity entity = requireWeeklyWork(id);
        validateReadable(loginUser, entity);
        return entity;
    }

    @Override
    @Transactional
    public Object review(ReviewWeeklyWorkRequest request) {
        LoginUser loginUser = currentLoginUser();
        WeeklyWorkEntity entity = requireWeeklyWork(request.getId());
        validateReadable(loginUser, entity);

        if (loginUser.getUserId().equals(entity.getUserId())) {
            throw new IllegalArgumentException("不能审核自己的周报");
        }
        if (!STATUS_SUBMITTED.equals(entity.getStatus())) {
            throw new IllegalArgumentException("当前状态不允许审核");
        }

        String action = request.getAction() == null ? "" : request.getAction().trim().toUpperCase();
        String nextStatus;
        if ("APPROVE".equals(action)) {
            nextStatus = STATUS_APPROVED;
        } else if ("RETURN".equals(action)) {
            nextStatus = STATUS_RETURNED;
        } else {
            throw new IllegalArgumentException("不支持的审核动作");
        }

        weeklyWorkMapper.updateStatus(entity.getId(), nextStatus);
        operationLogService.log(
                "WEEKLY_WORK",
                "REVIEW",
                entity.getId(),
                "审核周报：" + entity.getWeekNo() + "，用户ID=" + entity.getUserId() + "，结果=" + nextStatus
        );
        return "ok";
    }

    private WeeklyWorkEntity requireWeeklyWork(Long id) {
        WeeklyWorkEntity entity = weeklyWorkMapper.findById(id);
        if (entity == null) {
            throw new IllegalArgumentException("周工作不存在");
        }
        return entity;
    }

    private UserEntity requireCurrentUser(Long userId) {
        UserEntity user = userMapper.findById(userId);
        if (user == null) {
            throw new IllegalArgumentException("当前用户不存在");
        }
        return user;
    }

    private void validateReadable(LoginUser loginUser, WeeklyWorkEntity entity) {
        if (permissionService.isSuperAdmin(loginUser.getUserId())) {
            return;
        }
        UserEntity currentUser = requireCurrentUser(loginUser.getUserId());
        UserEntity targetUser = requireCurrentUser(entity.getUserId());
        dataScopeService.validateReadableUser(currentUser, targetUser, "无权查看该周报");
    }

    private LoginUser currentLoginUser() {
        if (currentUserFacade != null) {
            return currentUserFacade.currentLoginUser();
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof LoginUser loginUser)) {
            throw new IllegalArgumentException("当前未登录");
        }
        return loginUser;
    }

}
