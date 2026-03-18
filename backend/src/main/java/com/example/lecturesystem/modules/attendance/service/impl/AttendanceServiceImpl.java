package com.example.lecturesystem.modules.attendance.service.impl;

import com.example.lecturesystem.modules.attendance.dto.AttendanceQueryRequest;
import com.example.lecturesystem.modules.attendance.dto.CheckInRequest;
import com.example.lecturesystem.modules.attendance.dto.SaveAttendanceRequest;
import com.example.lecturesystem.modules.attendance.entity.AttendanceRecordEntity;
import com.example.lecturesystem.modules.attendance.mapper.AttendanceMapper;
import com.example.lecturesystem.modules.attendance.service.AttendanceService;
import com.example.lecturesystem.modules.auth.security.LoginUser;
import com.example.lecturesystem.modules.operationlog.service.OperationLogService;
import com.example.lecturesystem.modules.permission.support.CurrentUserFacade;
import com.example.lecturesystem.modules.permission.support.DataScopeHelper;
import com.example.lecturesystem.modules.permission.service.PermissionService;
import com.example.lecturesystem.modules.user.entity.UserEntity;
import com.example.lecturesystem.modules.user.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Service
public class AttendanceServiceImpl implements AttendanceService {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AttendanceMapper attendanceMapper;
    private final PermissionService permissionService;
    private final UserMapper userMapper;
    private final OperationLogService operationLogService;
    private final CurrentUserFacade currentUserFacade;
    private final DataScopeHelper dataScopeHelper;

    public AttendanceServiceImpl(AttendanceMapper attendanceMapper,
                                 PermissionService permissionService,
                                 UserMapper userMapper) {
        this(attendanceMapper, permissionService, userMapper, new OperationLogService() {
            @Override
            public void log(String moduleName, String actionName, Long bizId, String content) {
            }

            @Override
            public Object query(com.example.lecturesystem.modules.operationlog.dto.OperationLogQueryRequest request) {
                return java.util.List.of();
            }
        }, null, null);
    }

    @Autowired
    public AttendanceServiceImpl(AttendanceMapper attendanceMapper,
                                 PermissionService permissionService,
                                 UserMapper userMapper,
                                 OperationLogService operationLogService,
                                 CurrentUserFacade currentUserFacade,
                                 DataScopeHelper dataScopeHelper) {
        this.attendanceMapper = attendanceMapper;
        this.permissionService = permissionService;
        this.userMapper = userMapper;
        this.operationLogService = operationLogService;
        this.currentUserFacade = currentUserFacade;
        this.dataScopeHelper = dataScopeHelper;
    }

    @Override
    @Transactional
    public Object checkIn(CheckInRequest request) {
        LoginUser loginUser = currentLoginUser();
        UserEntity currentUser = requireCurrentUser(loginUser.getUserId());
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        AttendanceRecordEntity entity = attendanceMapper.findByUserIdAndDate(loginUser.getUserId(), today);
        String action;
        if (entity == null) {
            entity = new AttendanceRecordEntity();
            entity.setUnitId(currentUser.getUnitId());
            entity.setUserId(loginUser.getUserId());
            entity.setAttendanceDate(today);
            entity.setCheckInTime(now);
            entity.setCheckInAddress(normalizeText(request.getAddress()));
            entity.setValidFlag(1);
            attendanceMapper.insert(entity);
            action = "CHECK_IN";
        } else if (entity.getCheckOutTime() == null) {
            entity.setCheckOutTime(now);
            entity.setCheckOutAddress(normalizeText(request.getAddress()));
            attendanceMapper.update(entity);
            action = "CHECK_OUT";
        } else {
            throw new IllegalArgumentException("今日考勤已完成");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", entity.getId());
        result.put("attendanceDate", entity.getAttendanceDate());
        result.put("checkInTime", entity.getCheckInTime());
        result.put("checkOutTime", entity.getCheckOutTime());
        result.put("validFlag", entity.getValidFlag());
        result.put("action", action);
        return result;
    }

    @Override
    public Object query(AttendanceQueryRequest request) {
        AttendanceQueryRequest normalizedRequest = request == null ? new AttendanceQueryRequest() : request;
        if (dataScopeHelper != null) {
            dataScopeHelper.injectUnitScope(normalizedRequest);
        }
        normalizedRequest.setKeywords(normalizeText(normalizedRequest.getKeywords()));
        normalizedRequest.setUnitName(normalizeText(normalizedRequest.getUnitName()));
        return attendanceMapper.queryList(normalizedRequest);
    }

    @Override
    @Transactional
    public Long saveAttendance(SaveAttendanceRequest request) {
        LoginUser loginUser = currentLoginUser();
        Long targetUserId = request.getUserId() == null ? loginUser.getUserId() : request.getUserId();
        validateManageableUser(loginUser.getUserId(), targetUserId);
        UserEntity targetUser = requireCurrentUser(targetUserId);
        LocalDate attendanceDate = LocalDate.parse(request.getAttendanceDate());
        LocalDateTime checkInTime = parseDateTime(request.getCheckInTime());
        LocalDateTime checkOutTime = parseDateTime(request.getCheckOutTime());
        validateAttendanceTimes(attendanceDate, checkInTime, checkOutTime);

        if (request.getId() == null) {
            AttendanceRecordEntity duplicated = attendanceMapper.findByUserIdAndDate(targetUserId, attendanceDate);
            if (duplicated != null) {
                throw new IllegalArgumentException("该用户当天考勤已存在");
            }
            AttendanceRecordEntity entity = new AttendanceRecordEntity();
            entity.setUnitId(targetUser.getUnitId());
            entity.setUserId(targetUserId);
            entity.setAttendanceDate(attendanceDate);
            entity.setCheckInTime(checkInTime);
            entity.setCheckOutTime(checkOutTime);
            entity.setCheckInAddress(normalizeText(request.getCheckInAddress()));
            entity.setCheckOutAddress(normalizeText(request.getCheckOutAddress()));
            entity.setValidFlag(request.getValidFlag());
            attendanceMapper.insert(entity);
            operationLogService.log(
                    "ATTENDANCE",
                    "SAVE",
                    entity.getId(),
                    "补录考勤：用户ID=" + targetUserId + "，日期=" + attendanceDate + "，状态=" + request.getValidFlag()
            );
            return entity.getId();
        }

        AttendanceRecordEntity existed = requireAttendance(request.getId());
        validateManageableUser(loginUser.getUserId(), existed.getUserId());
        AttendanceRecordEntity duplicated = attendanceMapper.findByUserIdAndDate(targetUserId, attendanceDate);
        if (duplicated != null && !duplicated.getId().equals(existed.getId())) {
            throw new IllegalArgumentException("该用户当天考勤已存在");
        }
        existed.setUnitId(targetUser.getUnitId());
        existed.setUserId(targetUserId);
        existed.setAttendanceDate(attendanceDate);
        existed.setCheckInTime(checkInTime);
        existed.setCheckOutTime(checkOutTime);
        existed.setCheckInAddress(normalizeText(request.getCheckInAddress()));
        existed.setCheckOutAddress(normalizeText(request.getCheckOutAddress()));
        existed.setValidFlag(request.getValidFlag());
        attendanceMapper.update(existed);
        operationLogService.log(
                "ATTENDANCE",
                "SAVE",
                existed.getId(),
                "编辑考勤：用户ID=" + targetUserId + "，日期=" + attendanceDate + "，状态=" + request.getValidFlag()
        );
        return existed.getId();
    }

    @Override
    @Transactional
    public void deleteAttendance(Long id) {
        LoginUser loginUser = currentLoginUser();
        AttendanceRecordEntity existed = requireAttendance(id);
        validateManageableUser(loginUser.getUserId(), existed.getUserId());
        attendanceMapper.deleteById(id);
    }

    private LocalDateTime parseDateTime(String text) {
        return text == null || text.isBlank() ? null : LocalDateTime.parse(text, DATE_TIME_FORMATTER);
    }

    private String normalizeText(String text) {
        if (text == null) {
            return null;
        }
        String trimmed = text.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private UserEntity requireCurrentUser(Long userId) {
        UserEntity user = userMapper.findById(userId);
        if (user == null) {
            throw new IllegalArgumentException("当前用户不存在");
        }
        return user;
    }

    private AttendanceRecordEntity requireAttendance(Long id) {
        AttendanceRecordEntity entity = attendanceMapper.findById(id);
        if (entity == null) {
            throw new IllegalArgumentException("考勤记录不存在");
        }
        return entity;
    }

    private void validateManageableUser(Long currentUserId, Long targetUserId) {
        if (permissionService.isSuperAdmin(currentUserId)) {
            return;
        }
        Set<Long> scopeUserIds = permissionService.queryDataScopeUserIds(currentUserId);
        if (!scopeUserIds.contains(targetUserId)) {
            throw new IllegalArgumentException("无权操作该用户考勤");
        }
    }

    private void validateAttendanceTimes(LocalDate attendanceDate, LocalDateTime checkInTime, LocalDateTime checkOutTime) {
        if (checkInTime != null && !attendanceDate.equals(checkInTime.toLocalDate())) {
            throw new IllegalArgumentException("上班时间必须落在考勤日期当天");
        }
        if (checkOutTime != null && !attendanceDate.equals(checkOutTime.toLocalDate())) {
            throw new IllegalArgumentException("下班时间必须落在考勤日期当天");
        }
        if (checkInTime != null && checkOutTime != null && !checkOutTime.isAfter(checkInTime)) {
            throw new IllegalArgumentException("下班时间必须晚于上班时间");
        }
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
