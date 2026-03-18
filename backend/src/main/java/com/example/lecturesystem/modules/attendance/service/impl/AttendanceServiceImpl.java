package com.example.lecturesystem.modules.attendance.service.impl;

import com.example.lecturesystem.modules.attendance.dto.AttendanceQueryRequest;
import com.example.lecturesystem.modules.attendance.dto.CheckInRequest;
import com.example.lecturesystem.modules.attendance.dto.SaveAttendanceRequest;
import com.example.lecturesystem.modules.attendance.entity.AttendanceRecordEntity;
import com.example.lecturesystem.modules.attendance.mapper.AttendanceMapper;
import com.example.lecturesystem.modules.attendance.support.AttendanceCheckInStatus;
import com.example.lecturesystem.modules.attendance.service.AttendanceService;
import com.example.lecturesystem.modules.attendance.vo.AttendanceAbnormalMonitorVO;
import com.example.lecturesystem.modules.attendance.vo.AttendanceAbnormalTrendPointVO;
import com.example.lecturesystem.modules.attendance.vo.AttendanceAbnormalUserRankVO;
import com.example.lecturesystem.modules.attendance.vo.AttendanceAbnormalUserSummaryVO;
import com.example.lecturesystem.modules.attendance.vo.AttendanceStatusCountVO;
import com.example.lecturesystem.modules.attendance.vo.AttendanceSummaryVO;
import com.example.lecturesystem.modules.auth.security.LoginUser;
import com.example.lecturesystem.modules.operationlog.service.OperationLogService;
import com.example.lecturesystem.modules.permission.support.CurrentUserFacade;
import com.example.lecturesystem.modules.permission.support.DataScopeService;
import com.example.lecturesystem.modules.permission.service.PermissionService;
import com.example.lecturesystem.modules.unit.vo.AttendanceLocationVO;
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
import java.util.List;
import java.util.Map;

@Service
public class AttendanceServiceImpl implements AttendanceService {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final double EARTH_RADIUS_METERS = 6371000D;

    private final AttendanceMapper attendanceMapper;
    private final PermissionService permissionService;
    private final UserMapper userMapper;
    private final OperationLogService operationLogService;
    private final CurrentUserFacade currentUserFacade;
    private final DataScopeService dataScopeService;

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
        }, null, new DataScopeService());
    }

    @Autowired
    public AttendanceServiceImpl(AttendanceMapper attendanceMapper,
                                 PermissionService permissionService,
                                 UserMapper userMapper,
                                 OperationLogService operationLogService,
                                 CurrentUserFacade currentUserFacade,
                                 DataScopeService dataScopeService) {
        this.attendanceMapper = attendanceMapper;
        this.permissionService = permissionService;
        this.userMapper = userMapper;
        this.operationLogService = operationLogService;
        this.currentUserFacade = currentUserFacade;
        this.dataScopeService = dataScopeService;
    }

    @Override
    public Object queryCurrentAttendanceLocation() {
        LoginUser loginUser = currentLoginUser();
        UserEntity currentUser = requireCurrentUser(loginUser.getUserId());
        CheckInScope scope = resolveCheckInScope(currentUser);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("userId", currentUser.getId());
        result.put("unitId", currentUser.getUnitId());
        result.put("unitName", scope.unitName);
        result.put("configured", scope.location != null);
        result.put("allowCheckIn", scope.reason == null);
        result.put("status", scope.status);
        result.put("reason", scope.reason);
        if (scope.location != null) {
            result.put("locationName", scope.location.getLocationName());
            result.put("latitude", scope.location.getLatitude());
            result.put("longitude", scope.location.getLongitude());
            result.put("radiusMeters", scope.location.getRadiusMeters());
            result.put("address", scope.location.getAddress());
            result.put("locationStatus", scope.location.getStatus());
        }
        return result;
    }

    @Override
    @Transactional
    public Object checkIn(CheckInRequest request) {
        LoginUser loginUser = currentLoginUser();
        UserEntity currentUser = requireCurrentUser(loginUser.getUserId());
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();
        CheckInScope scope = resolveCheckInScope(currentUser);
        if (scope.reason != null) {
            return buildCheckInResult(false, null, scope, null, scope.reason, scope.status);
        }
        if (request == null || request.getLatitude() == null || request.getLongitude() == null) {
            return buildCheckInResult(false, null, scope, null, "未获取到当前位置", AttendanceCheckInStatus.LOCATION_REQUIRED);
        }
        int distanceMeters = calculateDistanceMeters(
                request.getLatitude().doubleValue(),
                request.getLongitude().doubleValue(),
                scope.location.getLatitude().doubleValue(),
                scope.location.getLongitude().doubleValue()
        );
        if (distanceMeters > scope.location.getRadiusMeters()) {
            return buildCheckInResult(false, null, scope, distanceMeters, "超出单位打卡范围", AttendanceCheckInStatus.OUT_OF_RANGE);
        }

        AttendanceRecordEntity entity = attendanceMapper.findByUserIdAndDate(loginUser.getUserId(), today);
        String action;
        if (entity == null) {
            entity = new AttendanceRecordEntity();
            entity.setUnitId(currentUser.getUnitId());
            entity.setUserId(loginUser.getUserId());
            entity.setAttendanceDate(today);
            entity.setCheckInTime(now);
            entity.setCheckInAddress(normalizeText(request.getAddress()));
            entity.setCheckInLatitude(request.getLatitude());
            entity.setCheckInLongitude(request.getLongitude());
            entity.setCheckInDistanceMeters(distanceMeters);
            entity.setCheckInResult(AttendanceCheckInStatus.CHECK_IN_SUCCESS);
            entity.setCheckInFailReason(null);
            entity.setValidFlag(1);
            attendanceMapper.insert(entity);
            action = "CHECK_IN";
        } else if (entity.getCheckOutTime() == null) {
            entity.setCheckOutTime(now);
            entity.setCheckOutAddress(normalizeText(request.getAddress()));
            entity.setCheckInResult(AttendanceCheckInStatus.CHECK_OUT_SUCCESS);
            entity.setCheckInFailReason(null);
            attendanceMapper.update(entity);
            action = "CHECK_OUT";
        } else {
            return buildCheckInResult(false, null, scope, distanceMeters, "今日考勤已完成", AttendanceCheckInStatus.ALREADY_FINISHED);
        }

        return buildCheckInResult(
                true,
                action,
                scope,
                distanceMeters,
                null,
                "CHECK_IN".equals(action) ? AttendanceCheckInStatus.CHECK_IN_SUCCESS : AttendanceCheckInStatus.CHECK_OUT_SUCCESS,
                entity
        );
    }

    @Override
    public Object query(AttendanceQueryRequest request) {
        AttendanceQueryRequest normalizedRequest = normalizeQueryRequest(request);
        return attendanceMapper.queryList(normalizedRequest);
    }

    @Override
    public Object querySummary(AttendanceQueryRequest request) {
        AttendanceQueryRequest normalizedRequest = normalizeQueryRequest(request);
        long totalCount = attendanceMapper.countByQuery(normalizedRequest);
        List<AttendanceStatusCountVO> statusCounts = attendanceMapper.queryStatusCounts(normalizedRequest);
        long successCount = 0L;
        for (AttendanceStatusCountVO item : statusCounts) {
            if (AttendanceCheckInStatus.CHECK_IN_SUCCESS.equals(item.getCheckInStatus())
                    || AttendanceCheckInStatus.CHECK_OUT_SUCCESS.equals(item.getCheckInStatus())) {
                successCount += item.getCount() == null ? 0L : item.getCount();
            }
        }
        AttendanceSummaryVO summary = new AttendanceSummaryVO();
        summary.setTotalCount(totalCount);
        summary.setSuccessCount(successCount);
        summary.setAbnormalCount(Math.max(totalCount - successCount, 0L));
        summary.setStatusCounts(statusCounts);
        return summary;
    }

    @Override
    public Object queryAbnormalMonitor(AttendanceQueryRequest request) {
        AttendanceQueryRequest normalizedRequest = normalizeQueryRequest(request);
        List<AttendanceAbnormalUserRankVO> topUsers = attendanceMapper.queryAbnormalUserRanks(normalizedRequest);
        AttendanceQueryRequest abnormalRequest = cloneRequest(normalizedRequest);
        abnormalRequest.setAbnormalOnly(Boolean.TRUE);
        abnormalRequest.setKeywords(null);
        abnormalRequest.setUserId(null);
        abnormalRequest.setCheckInStatus(null);

        AttendanceAbnormalMonitorVO monitor = new AttendanceAbnormalMonitorVO();
        monitor.setTopUsers(topUsers);
        monitor.setStatusCounts(attendanceMapper.queryStatusCounts(abnormalRequest));
        return monitor;
    }

    @Override
    public Object queryAbnormalTrend(AttendanceQueryRequest request) {
        AttendanceQueryRequest normalizedRequest = normalizeQueryRequest(request);
        if (normalizedRequest.getUserId() == null) {
            return List.of();
        }
        AttendanceQueryRequest trendRequest = cloneRequest(normalizedRequest);
        trendRequest.setKeywords(null);
        trendRequest.setCheckInStatus(null);
        trendRequest.setAbnormalOnly(null);
        return attendanceMapper.queryAbnormalTrend(trendRequest);
    }

    @Override
    public Object queryAbnormalUserSummary(AttendanceQueryRequest request) {
        AttendanceQueryRequest normalizedRequest = normalizeQueryRequest(request);
        if (normalizedRequest.getUserId() == null) {
            return null;
        }
        AttendanceQueryRequest summaryRequest = cloneRequest(normalizedRequest);
        summaryRequest.setKeywords(null);
        summaryRequest.setCheckInStatus(null);
        summaryRequest.setAbnormalOnly(null);
        return attendanceMapper.queryAbnormalUserSummary(summaryRequest);
    }

    private AttendanceQueryRequest normalizeQueryRequest(AttendanceQueryRequest request) {
        AttendanceQueryRequest normalizedRequest = request == null ? new AttendanceQueryRequest() : request;
        LoginUser loginUser = currentLoginUser();
        if (!permissionService.isSuperAdmin(loginUser.getUserId())) {
            dataScopeService.injectTreePathScope(normalizedRequest, requireCurrentUser(loginUser.getUserId()));
        }
        normalizedRequest.setKeywords(normalizeText(normalizedRequest.getKeywords()));
        normalizedRequest.setUnitName(normalizeText(normalizedRequest.getUnitName()));
        return normalizedRequest;
    }

    private AttendanceQueryRequest cloneRequest(AttendanceQueryRequest source) {
        AttendanceQueryRequest target = new AttendanceQueryRequest();
        target.setTreePathPrefix(source.getTreePathPrefix());
        target.setKeywords(source.getKeywords());
        target.setUnitName(source.getUnitName());
        target.setDateFrom(source.getDateFrom());
        target.setDateTo(source.getDateTo());
        target.setCheckInStatus(source.getCheckInStatus());
        target.setUserId(source.getUserId());
        target.setAbnormalOnly(source.getAbnormalOnly());
        return target;
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
        UserEntity currentUser = requireCurrentUser(currentUserId);
        UserEntity targetUser = requireCurrentUser(targetUserId);
        dataScopeService.validateReadableUser(currentUser, targetUser, "无权操作该用户考勤");
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

    private CheckInScope resolveCheckInScope(UserEntity currentUser) {
        if (currentUser.getUnitId() == null) {
            return new CheckInScope(null, null, "当前用户未绑定单位", AttendanceCheckInStatus.LOCATION_NOT_BOUND);
        }
        AttendanceLocationVO location = attendanceMapper.findAttendanceLocationByUnitId(currentUser.getUnitId());
        String unitName = attendanceMapper.findUnitNameById(currentUser.getUnitId());
        if (location == null) {
            return new CheckInScope(location, unitName, "当前单位未配置打卡点", AttendanceCheckInStatus.LOCATION_NOT_CONFIGURED);
        }
        if (location.getStatus() == null || location.getStatus() != 1) {
            return new CheckInScope(location, unitName, "当前单位打卡点未启用", AttendanceCheckInStatus.LOCATION_DISABLED);
        }
        return new CheckInScope(location, unitName, null, AttendanceCheckInStatus.LOCATION_READY);
    }

    private Map<String, Object> buildCheckInResult(boolean success,
                                                   String action,
                                                   CheckInScope scope,
                                                   Integer distanceMeters,
                                                   String reason) {
        return buildCheckInResult(success, action, scope, distanceMeters, reason, scope.status, null);
    }

    private Map<String, Object> buildCheckInResult(boolean success,
                                                   String action,
                                                   CheckInScope scope,
                                                   Integer distanceMeters,
                                                   String reason,
                                                   String status) {
        return buildCheckInResult(success, action, scope, distanceMeters, reason, status, null);
    }

    private Map<String, Object> buildCheckInResult(boolean success,
                                                   String action,
                                                   CheckInScope scope,
                                                   Integer distanceMeters,
                                                   String reason,
                                                   String status,
                                                   AttendanceRecordEntity entity) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", success);
        result.put("allowCheckIn", success);
        result.put("action", action);
        result.put("status", status);
        result.put("reason", reason);
        result.put("failReason", reason);
        result.put("distanceMeters", distanceMeters);
        result.put("configured", scope.location != null);
        result.put("unitName", scope.unitName);
        result.put("radiusMeters", scope.location == null ? null : scope.location.getRadiusMeters());
        result.put("locationName", scope.location == null ? null : scope.location.getLocationName());
        result.put("locationAddress", scope.location == null ? null : scope.location.getAddress());
        if (entity != null) {
            result.put("id", entity.getId());
            result.put("attendanceDate", entity.getAttendanceDate());
            result.put("checkInTime", entity.getCheckInTime());
            result.put("checkOutTime", entity.getCheckOutTime());
            result.put("validFlag", entity.getValidFlag());
        }
        return result;
    }

    private int calculateDistanceMeters(double latitude1, double longitude1, double latitude2, double longitude2) {
        double latDistance = Math.toRadians(latitude2 - latitude1);
        double lngDistance = Math.toRadians(longitude2 - longitude1);
        double sinLat = Math.sin(latDistance / 2);
        double sinLng = Math.sin(lngDistance / 2);
        double a = sinLat * sinLat
                + Math.cos(Math.toRadians(latitude1)) * Math.cos(Math.toRadians(latitude2)) * sinLng * sinLng;
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return (int) Math.round(EARTH_RADIUS_METERS * c);
    }

    private static class CheckInScope {
        private final AttendanceLocationVO location;
        private final String unitName;
        private final String reason;
        private final String status;

        private CheckInScope(AttendanceLocationVO location, String unitName, String reason, String status) {
            this.location = location;
            this.unitName = unitName;
            this.reason = reason;
            this.status = status;
        }
    }

}
