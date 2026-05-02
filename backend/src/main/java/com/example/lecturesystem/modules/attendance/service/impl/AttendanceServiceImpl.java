package com.example.lecturesystem.modules.attendance.service.impl;

import com.example.lecturesystem.modules.attendance.dto.AttendanceQueryRequest;
import com.example.lecturesystem.modules.attendance.dto.CheckInRequest;
import com.example.lecturesystem.modules.attendance.dto.SaveAttendanceRequest;
import com.example.lecturesystem.modules.attendance.entity.AttendancePatchApplyEntity;
import com.example.lecturesystem.modules.attendance.entity.AttendanceRecordEntity;
import com.example.lecturesystem.modules.attendance.mapper.AttendanceMapper;
import com.example.lecturesystem.modules.attendance.mapper.AttendancePatchApplyMapper;
import com.example.lecturesystem.modules.attendance.support.AttendanceCheckInStatus;
import com.example.lecturesystem.modules.attendance.support.AttendanceNodeStateResolver;
import com.example.lecturesystem.modules.attendance.service.AttendanceService;
import com.example.lecturesystem.modules.attendance.vo.AttendanceAbnormalMonitorVO;
import com.example.lecturesystem.modules.attendance.vo.AttendanceAbnormalReasonDistributionVO;
import com.example.lecturesystem.modules.attendance.vo.AttendanceAbnormalTrendComparisonVO;
import com.example.lecturesystem.modules.attendance.vo.AttendanceAbnormalTrendPointVO;
import com.example.lecturesystem.modules.attendance.vo.AttendanceAbnormalUserRankVO;
import com.example.lecturesystem.modules.attendance.vo.AttendanceAbnormalUserBehaviorPointVO;
import com.example.lecturesystem.modules.attendance.vo.AttendanceAbnormalUserSummaryVO;
import com.example.lecturesystem.modules.attendance.vo.AttendanceNodeStatusVO;
import com.example.lecturesystem.modules.attendance.vo.AttendancePageVO;
import com.example.lecturesystem.modules.attendance.vo.AttendanceStatusCountVO;
import com.example.lecturesystem.modules.attendance.vo.AttendanceSummaryVO;
import com.example.lecturesystem.modules.auth.security.LoginUser;
import com.example.lecturesystem.modules.operationlog.service.OperationLogService;
import com.example.lecturesystem.modules.param.service.ParamService;
import com.example.lecturesystem.modules.permission.support.CurrentUserFacade;
import com.example.lecturesystem.modules.permission.support.DataScopeService;
import com.example.lecturesystem.modules.permission.service.PermissionService;
import com.example.lecturesystem.modules.unit.vo.AttendanceLocationVO;
import com.example.lecturesystem.modules.user.entity.UserEntity;
import com.example.lecturesystem.modules.user.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.lecturesystem.modules.attendance.entity.AttendanceRuleEntity;
import com.example.lecturesystem.modules.attendance.mapper.AttendanceRuleMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AttendanceServiceImpl implements AttendanceService {
    private static final Logger log = LoggerFactory.getLogger(AttendanceServiceImpl.class);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final double EARTH_RADIUS_METERS = 6371000D;
    private static final int DEFAULT_GOOD_ACCURACY_METERS = 100;
    private static final int DEFAULT_MAX_ACCURACY_METERS = 1000;
    private static final String PARAM_ATTENDANCE_LOCATION_ACCURACY_GOOD_THRESHOLD = "ATTENDANCE_LOCATION_ACCURACY_GOOD_THRESHOLD";
    private static final String PARAM_ATTENDANCE_LOCATION_ACCURACY_MAX_THRESHOLD = "ATTENDANCE_LOCATION_ACCURACY_MAX_THRESHOLD";
    private static final int MAX_WEAK_TOLERANCE_METERS = 50;
    private static final String RISK_LEVEL_HIGH = "HIGH";
    private static final String RISK_LEVEL_MEDIUM = "MEDIUM";
    private static final String RISK_LEVEL_LOW = "LOW";
    
    private static final String CHECK_ACTION_AM_ON = "AM_ON";
    private static final String CHECK_ACTION_AM_OFF = "AM_OFF";
    private static final String CHECK_ACTION_PM_ON = "PM_ON";
    private static final String CHECK_ACTION_PM_OFF = "PM_OFF";

    private static final String REASON_AM_ON_COMPLETED = "今日已完成上午上班打卡";
    private static final String REASON_AM_OFF_COMPLETED = "今日已完成上午下班打卡";
    private static final String REASON_PM_ON_COMPLETED = "今日已完成下午上班打卡";
    private static final String REASON_PM_OFF_COMPLETED = "今日已完成下午下班打卡";
    private static final String REASON_CHECK_IN_REQUIRED = "请先完成上一节点打卡";
    private static final String REASON_TODAY_FINISHED = "今日打卡已全部完成";

    private static final String TREND_RISING = "RISING";
    private static final String TREND_FALLING = "FALLING";
    private static final String TREND_STABLE = "STABLE";

 private final AttendanceMapper attendanceMapper;
private final AttendancePatchApplyMapper attendancePatchApplyMapper;
private final AttendanceRuleMapper attendanceRuleMapper;
private final PermissionService permissionService;
private final UserMapper userMapper;
private final OperationLogService operationLogService;
private final CurrentUserFacade currentUserFacade;
private final DataScopeService dataScopeService;
private final ParamService paramService;

    public AttendanceServiceImpl(AttendanceMapper attendanceMapper,
                             PermissionService permissionService,
                             UserMapper userMapper) {
    this(
            attendanceMapper,
            null,
            null,
            permissionService,
            userMapper,
            new OperationLogService() {
                @Override
                public void log(String moduleName, String actionName, Long bizId, String content) {
                }

                @Override
                public Object query(com.example.lecturesystem.modules.operationlog.dto.OperationLogQueryRequest request) {
                    return java.util.List.of();
                }
            },
            null,
            new DataScopeService(),
            new ParamService() {
                @Override
                public Object listParams(com.example.lecturesystem.modules.param.dto.ParamQueryRequest request) {
                    return java.util.List.of();
                }

                @Override
                public String getByCode(String code) {
                    return null;
                }

                @Override
                public com.example.lecturesystem.modules.config.vo.AmapConfigVO queryAmapConfig() {
                    return null;
                }

                @Override
                public Long saveParam(com.example.lecturesystem.modules.param.dto.SaveParamRequest request) {
                    return null;
                }

                @Override
                public void deleteParam(Long id) {
                }

                @Override
                public void toggleStatus(com.example.lecturesystem.modules.param.dto.ToggleParamStatusRequest request) {
                }
            }
    );
}

@Autowired
public AttendanceServiceImpl(AttendanceMapper attendanceMapper,
                             AttendancePatchApplyMapper attendancePatchApplyMapper,
                             AttendanceRuleMapper attendanceRuleMapper,
                             PermissionService permissionService,
                             UserMapper userMapper,
                             OperationLogService operationLogService,
                             CurrentUserFacade currentUserFacade,
                             DataScopeService dataScopeService,
                             ParamService paramService) {
    this.attendanceMapper = attendanceMapper;
    this.attendancePatchApplyMapper = attendancePatchApplyMapper;
    this.attendanceRuleMapper = attendanceRuleMapper;
    this.permissionService = permissionService;
    this.userMapper = userMapper;
    this.operationLogService = operationLogService;
    this.currentUserFacade = currentUserFacade;
    this.dataScopeService = dataScopeService;
    this.paramService = paramService;
}

    @Override
public Object queryCurrentAttendanceLocation() {
    LoginUser loginUser = currentLoginUser();
    UserEntity currentUser = requireCurrentUser(loginUser.getUserId());
    CheckInScope scope = resolveCheckInScope(currentUser);
    LocalDate today = LocalDate.now();
    AttendanceRecordEntity todayRecord = attendanceMapper.findByUserIdAndDate(currentUser.getId(), today);
    AttendanceNodeStateResolver.ResolvedAttendanceState resolvedState = resolveAttendanceState(
            currentUser.getUnitId(),
            currentUser.getId(),
            today,
            todayRecord,
            LocalTime.now(),
            null
    );

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("userId", currentUser.getId());
    result.put("unitId", currentUser.getUnitId());
    result.put("unitName", scope.unitName);
    result.put("configured", scope.location != null);
    result.put("status", scope.status);
    result.put("accuracyGoodThreshold", resolveGoodAccuracyThreshold());
    result.put("accuracyMaxThreshold", resolveMaxAccuracyThreshold());
    result.put("ruleTimes", buildRuleTimes(findActiveAttendanceRule(currentUser.getUnitId())));

    if (scope.location != null) {
        result.put("locationName", scope.location.getLocationName());
        result.put("latitude", scope.location.getLatitude());
        result.put("longitude", scope.location.getLongitude());
        result.put("radiusMeters", scope.location.getRadiusMeters());
        result.put("address", scope.location.getAddress());
        result.put("locationStatus", scope.location.getStatus());
    }

    applyResolvedAttendanceState(result, resolvedState, null);

    if (todayRecord != null) {
        result.put("checkInTime", todayRecord.getCheckInTime());
        result.put("amOffTime", todayRecord.getAmOffTime());
        result.put("pmOnTime", todayRecord.getPmOnTime());
        result.put("checkOutTime", todayRecord.getCheckOutTime());
    }

    if (scope.reason != null) {
        result.put("allowCheckIn", false);
        result.put("reason", scope.reason);
    } else {
        result.put("allowCheckIn", resolvedState.isCurrentActionAvailable());
        result.put("reason", resolvedState.isCurrentActionAvailable() ? null : resolvedState.getHint());
    }
    return result;
}

private Map<String, String> buildRuleTimes(AttendanceRuleEntity rule) {
    Map<String, String> ruleTimes = new LinkedHashMap<>();
    if (rule == null) {
        return ruleTimes;
    }
    ruleTimes.put("amOn", formatRuleTime(rule.getWorkStartTime()));
    ruleTimes.put("amOff", formatRuleTime(rule.getAmOffTime()));
    ruleTimes.put("pmOn", formatRuleTime(rule.getPmOnTime()));
    ruleTimes.put("pmOff", formatRuleTime(rule.getWorkEndTime()));
    return ruleTimes;
}

private String formatRuleTime(LocalTime value) {
    return value == null ? "" : TIME_FORMATTER.format(value);
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
            logCheckInFailureDiagnostic(
                    loginUser,
                    request,
                    "CHECK_IN_VALIDATE",
                    AttendanceCheckInStatus.LOCATION_REQUIRED,
                    null,
                    scope.location == null ? null : scope.location.getRadiusMeters(),
                    "未获取到当前位置"
            );
            return buildCheckInResult(false, null, scope, null, "未获取到当前位置", AttendanceCheckInStatus.LOCATION_REQUIRED);
        }
        Integer accuracyMeters = normalizeAccuracyMeters(request.getAccuracyMeters());
        int goodAccuracyThreshold = resolveGoodAccuracyThreshold();
        int maxAccuracyThreshold = resolveMaxAccuracyThreshold();
        if (isInvalidLocation(request, accuracyMeters, maxAccuracyThreshold)) {
            String invalidReason = buildInvalidLocationReason(request, accuracyMeters, maxAccuracyThreshold);
            logCheckInFailureDiagnostic(
                    loginUser,
                    request,
                    "CHECK_IN_VALIDATE",
                    AttendanceCheckInStatus.LOCATION_INVALID,
                    null,
                    scope.location == null ? null : scope.location.getRadiusMeters(),
                    invalidReason
            );
            log.warn(
                    "attendance check-in rejected userId={} branch={} status={} submittedLat={} submittedLng={} accuracyMeters={} reason={}",
                    loginUser.getUserId(),
                    "INVALID",
                    AttendanceCheckInStatus.LOCATION_INVALID,
                    request.getLatitude(),
                    request.getLongitude(),
                    accuracyMeters,
                    invalidReason
            );
            return buildCheckInResult(false, null, scope, null, invalidReason, AttendanceCheckInStatus.LOCATION_INVALID, null, accuracyMeters, "INVALID", false, 0);
        }
        if (isWeakLocation(accuracyMeters, goodAccuracyThreshold, maxAccuracyThreshold)) {
            String weakReason = buildWeakLocationReason(accuracyMeters, goodAccuracyThreshold);
            logCheckInFailureDiagnostic(
                    loginUser,
                    request,
                    "CHECK_IN_VALIDATE",
                    AttendanceCheckInStatus.LOCATION_WEAK,
                    null,
                    scope.location == null ? null : scope.location.getRadiusMeters(),
                    weakReason
            );
            return buildCheckInResult(false, null, scope, null, weakReason, AttendanceCheckInStatus.LOCATION_WEAK, null, accuracyMeters, "ACCURACY_WEAK", false, 0);
        }
        int distanceMeters = calculateDistanceMeters(
                request.getLatitude().doubleValue(),
                request.getLongitude().doubleValue(),
                scope.location.getLatitude().doubleValue(),
                scope.location.getLongitude().doubleValue()
        );
        int toleranceMeters = resolveWeakToleranceMeters(accuracyMeters);
        log.info(
                "attendance check-in validate userId={} submittedLat={} submittedLng={} targetLat={} targetLng={} radiusMeters={} accuracyMeters={} distanceMeters={} toleranceMeters={}",
                loginUser.getUserId(),
                request.getLatitude(),
                request.getLongitude(),
                scope.location.getLatitude(),
                scope.location.getLongitude(),
                scope.location.getRadiusMeters(),
                accuracyMeters,
                distanceMeters,
                toleranceMeters
        );
        if (distanceMeters <= scope.location.getRadiusMeters()) {
            return persistCheckInSuccessStateDriven(currentUser, loginUser, today, now, request, scope, distanceMeters, accuracyMeters, false, "SUCCESS");
        }
        String outOfRangeReason = buildOutOfRangeReason(distanceMeters, scope.location.getRadiusMeters(), accuracyMeters);
        AttendanceRecordEntity todayRecord = attendanceMapper.findByUserIdAndDate(loginUser.getUserId(), today);
        String evidenceNodeCode = resolveEvidenceNodeCode(
                currentUser.getUnitId(),
                loginUser.getUserId(),
                today,
                todayRecord,
                now.toLocalTime(),
                resolveRequestedCheckAction(request)
        );
        logCheckInFailureDiagnostic(
                loginUser,
                request,
                "CHECK_IN_VALIDATE",
                AttendanceCheckInStatus.EVIDENCE_REQUIRED,
                distanceMeters,
                scope.location.getRadiusMeters(),
                outOfRangeReason
        );
        log.warn(
                "attendance check-in rejected userId={} branch={} status={} distanceMeters={} radiusMeters={} accuracyMeters={} reason={}",
                loginUser.getUserId(),
                "OUT",
                AttendanceCheckInStatus.EVIDENCE_REQUIRED,
                distanceMeters,
                scope.location.getRadiusMeters(),
                accuracyMeters,
                outOfRangeReason
        );
        return buildCheckInResult(
                false,
                evidenceNodeCode,
                scope,
                distanceMeters,
                outOfRangeReason,
                AttendanceCheckInStatus.EVIDENCE_REQUIRED,
                todayRecord,
                accuracyMeters,
                "OUT_OF_RANGE_EVIDENCE",
                false,
                toleranceMeters,
                evidenceNodeCode
        );
    }

    @Override
    public Object query(AttendanceQueryRequest request) {
        AttendanceQueryRequest normalizedRequest = normalizeQueryRequest(request);
        AttendancePageVO page = new AttendancePageVO();
        page.setPageNo(normalizedRequest.getPageNo());
        page.setPageSize(normalizedRequest.getPageSize());
        page.setTotal(attendanceMapper.countByQuery(normalizedRequest));
        page.setList(attendanceMapper.queryList(normalizedRequest));
        return page;
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
        abnormalRequest.setCheckInStatus(null);
        LocalDate trendEndDate = resolveTrendEndDate(normalizedRequest);
        List<AttendanceAbnormalTrendComparisonVO> trendComparisons = attendanceMapper.queryAbnormalTrendComparisons(
                abnormalRequest,
                trendEndDate.minusDays(6),
                trendEndDate,
                trendEndDate.minusDays(13),
                trendEndDate.minusDays(7)
        );
        List<AttendanceAbnormalReasonDistributionVO> reasonDistributions = attendanceMapper.queryAbnormalReasonDistributions(abnormalRequest);
        List<AttendanceAbnormalReasonDistributionVO> topReasons = attendanceMapper.queryAbnormalUserTopReasons(abnormalRequest);
        Map<Long, AttendanceAbnormalTrendComparisonVO> trendComparisonMap = toTrendComparisonMap(trendComparisons);
        Map<Long, AttendanceAbnormalReasonDistributionVO> topReasonMap = toTopReasonMap(topReasons);

        long highRiskCount = 0L;
        long alertCount = 0L;
        for (AttendanceAbnormalUserRankVO item : topUsers) {
            enrichRiskFields(item, trendComparisonMap.get(item.getUserId()), topReasonMap.get(item.getUserId()));
            if (RISK_LEVEL_HIGH.equals(item.getRiskLevel())) {
                highRiskCount += 1L;
            }
            if (Boolean.TRUE.equals(item.getAlertTriggered())) {
                alertCount += 1L;
            }
        }
        for (AttendanceAbnormalReasonDistributionVO item : reasonDistributions) {
            item.setReasonLabel(resolveReasonLabel(item.getReasonKey()));
            item.setReasonTag(resolveReasonTag(item.getReasonKey()));
        }
        applyReasonRates(reasonDistributions);

        AttendanceAbnormalMonitorVO monitor = new AttendanceAbnormalMonitorVO();
        monitor.setTopUsers(topUsers);
        monitor.setStatusCounts(attendanceMapper.queryStatusCounts(abnormalRequest));
        monitor.setReasonDistributions(reasonDistributions);
        monitor.setHighRiskCount(highRiskCount);
        monitor.setAlertCount(alertCount);
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
        AttendanceAbnormalUserSummaryVO summary = attendanceMapper.queryAbnormalUserSummary(summaryRequest);
        if (summary == null) {
            return null;
        }
        LocalDate trendEndDate = resolveTrendEndDate(summaryRequest);
        AttendanceAbnormalTrendComparisonVO trendComparison = firstOrNull(attendanceMapper.queryAbnormalTrendComparisons(
                summaryRequest,
                trendEndDate.minusDays(6),
                trendEndDate,
                trendEndDate.minusDays(13),
                trendEndDate.minusDays(7)
        ));
        AttendanceQueryRequest abnormalOnlyRequest = cloneRequest(summaryRequest);
        abnormalOnlyRequest.setAbnormalOnly(Boolean.TRUE);
        AttendanceAbnormalReasonDistributionVO topReason = firstOrNull(attendanceMapper.queryAbnormalUserTopReasons(abnormalOnlyRequest));
        List<AttendanceAbnormalUserBehaviorPointVO> behaviorPoints = attendanceMapper.queryAbnormalUserBehaviorPoints(abnormalOnlyRequest);

        AttendanceAbnormalUserRankVO rankProjection = firstOrNull(attendanceMapper.queryAbnormalUserRanks(summaryRequest));
        if (rankProjection == null) {
            rankProjection = new AttendanceAbnormalUserRankVO();
            rankProjection.setUserId(summary.getUserId());
            rankProjection.setAbnormalCount(summary.getAbnormalCount());
            rankProjection.setTotalCount(summary.getAbnormalCount());
            rankProjection.setAbnormalRate(BigDecimal.ZERO);
        }
        enrichRiskFields(rankProjection, trendComparison, topReason);

        summary.setRiskScore(rankProjection.getRiskScore());
        summary.setRiskLevel(rankProjection.getRiskLevel());
        summary.setRecent7DayAbnormalCount(rankProjection.getRecent7DayAbnormalCount());
        summary.setPrevious7DayAbnormalCount(rankProjection.getPrevious7DayAbnormalCount());
        summary.setTrendDirection(rankProjection.getTrendDirection());
        summary.setMainReasonKey(rankProjection.getMainReasonKey());
        summary.setMainReasonLabel(rankProjection.getMainReasonLabel());
        summary.setMainReasonTag(rankProjection.getMainReasonTag());
        summary.setAlertTriggered(rankProjection.getAlertTriggered());
        summary.setAlertRuleText(rankProjection.getAlertRuleText());
        enrichBehaviorPortrait(summary, behaviorPoints);
        return summary;
    }

    private void applyReasonRates(List<AttendanceAbnormalReasonDistributionVO> items) {
        long total = 0L;
        for (AttendanceAbnormalReasonDistributionVO item : items) {
            total += item.getCount() == null ? 0L : item.getCount();
        }
        for (AttendanceAbnormalReasonDistributionVO item : items) {
            long count = item.getCount() == null ? 0L : item.getCount();
            item.setRate(total == 0L ? BigDecimal.ZERO : BigDecimal.valueOf((count * 100.0) / total).setScale(1, RoundingMode.HALF_UP));
        }
    }

    private Map<Long, AttendanceAbnormalTrendComparisonVO> toTrendComparisonMap(List<AttendanceAbnormalTrendComparisonVO> items) {
        Map<Long, AttendanceAbnormalTrendComparisonVO> result = new HashMap<>();
        for (AttendanceAbnormalTrendComparisonVO item : items) {
            result.put(item.getUserId(), item);
        }
        return result;
    }

    private Map<Long, AttendanceAbnormalReasonDistributionVO> toTopReasonMap(List<AttendanceAbnormalReasonDistributionVO> items) {
        Map<Long, AttendanceAbnormalReasonDistributionVO> result = new HashMap<>();
        for (AttendanceAbnormalReasonDistributionVO item : items) {
            result.put(item.getUserId(), item);
        }
        return result;
    }

    private void enrichRiskFields(AttendanceAbnormalUserRankVO target,
                                  AttendanceAbnormalTrendComparisonVO trendComparison,
                                  AttendanceAbnormalReasonDistributionVO topReason) {
        long abnormalCount = target.getAbnormalCount() == null ? 0L : target.getAbnormalCount();
        BigDecimal abnormalRate = target.getAbnormalRate() == null ? BigDecimal.ZERO : target.getAbnormalRate();
        long recent = trendComparison == null || trendComparison.getRecent7DayAbnormalCount() == null ? 0L : trendComparison.getRecent7DayAbnormalCount();
        long previous = trendComparison == null || trendComparison.getPrevious7DayAbnormalCount() == null ? 0L : trendComparison.getPrevious7DayAbnormalCount();
        String trendDirection = resolveTrendDirection(recent, previous);
        int riskScore = calculateRiskScore(abnormalCount, abnormalRate);
        String riskLevel = resolveRiskLevel(riskScore);
        String mainReasonKey = topReason == null ? null : topReason.getReasonKey();
        String mainReasonLabel = resolveReasonLabel(mainReasonKey);
        String mainReasonTag = resolveReasonTag(mainReasonKey);

        target.setRecent7DayAbnormalCount(recent);
        target.setPrevious7DayAbnormalCount(previous);
        target.setTrendDirection(trendDirection);
        target.setRiskScore(riskScore);
        target.setRiskLevel(riskLevel);
        target.setMainReasonKey(mainReasonKey);
        target.setMainReasonLabel(mainReasonLabel);
        target.setMainReasonTag(mainReasonTag);
        target.setAlertTriggered(isAlertTriggered(riskLevel, recent, abnormalRate, trendDirection));
        target.setAlertRuleText(buildAlertRuleText(riskLevel, recent, trendDirection, abnormalRate, mainReasonTag));
    }

    private void enrichBehaviorPortrait(AttendanceAbnormalUserSummaryVO summary,
                                        List<AttendanceAbnormalUserBehaviorPointVO> behaviorPoints) {
        if (behaviorPoints == null || behaviorPoints.isEmpty()) {
            summary.setTopLocation("-");
            summary.setTopLocationCount(0L);
            summary.setLocationConcentrationRate(BigDecimal.ZERO);
            summary.setMorningAbnormalCount(0L);
            summary.setAfternoonAbnormalCount(0L);
            summary.setEveningAbnormalCount(0L);
            summary.setPeakTimeSlot("暂无数据");
            return;
        }
        Map<String, Long> locationCounts = new HashMap<>();
        long morning = 0L;
        long afternoon = 0L;
        long evening = 0L;
        for (AttendanceAbnormalUserBehaviorPointVO item : behaviorPoints) {
            String location = normalizePortraitLocation(item.getCheckInAddress());
            locationCounts.put(location, locationCounts.getOrDefault(location, 0L) + 1L);
            LocalTime checkInTime = item.getCheckInTime() == null ? null : item.getCheckInTime().toLocalTime();
            if (checkInTime == null || checkInTime.isBefore(LocalTime.NOON)) {
                morning += 1L;
            } else if (checkInTime.isBefore(LocalTime.of(18, 0))) {
                afternoon += 1L;
            } else {
                evening += 1L;
            }
        }
        Map.Entry<String, Long> topLocation = locationCounts.entrySet().stream()
                .max(Map.Entry.<String, Long>comparingByValue().thenComparing(Map.Entry.comparingByKey()))
                .orElse(null);
        long total = behaviorPoints.size();
        summary.setTopLocation(topLocation == null ? "-" : topLocation.getKey());
        summary.setTopLocationCount(topLocation == null ? 0L : topLocation.getValue());
        summary.setLocationConcentrationRate(total == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(((topLocation == null ? 0L : topLocation.getValue()) * 100.0) / total).setScale(1, RoundingMode.HALF_UP));
        summary.setMorningAbnormalCount(morning);
        summary.setAfternoonAbnormalCount(afternoon);
        summary.setEveningAbnormalCount(evening);
        summary.setPeakTimeSlot(resolvePeakTimeSlot(morning, afternoon, evening));
    }

    private String normalizePortraitLocation(String location) {
        String normalized = normalizeText(location);
        return normalized == null ? "未知地点" : normalized;
    }

    private String resolvePeakTimeSlot(long morning, long afternoon, long evening) {
        if (morning >= afternoon && morning >= evening) {
            return "上午";
        }
        if (afternoon >= morning && afternoon >= evening) {
            return "下午";
        }
        return "晚间";
    }

    private LocalDate resolveTrendEndDate(AttendanceQueryRequest request) {
        LocalDate dateTo = parseDate(request.getDateTo());
        return dateTo == null ? LocalDate.now() : dateTo;
    }

    private <T> T firstOrNull(List<T> items) {
        return items == null || items.isEmpty() ? null : items.get(0);
    }

    private int calculateRiskScore(long abnormalCount, BigDecimal abnormalRate) {
        double rateScore = abnormalRate == null ? 0D : abnormalRate.doubleValue() * 0.5D;
        double countScore = abnormalCount * 10D;
        return (int) Math.max(0D, Math.min(100D, Math.round(rateScore + countScore)));
    }

    private String resolveRiskLevel(int riskScore) {
        if (riskScore >= 75) {
            return RISK_LEVEL_HIGH;
        }
        if (riskScore >= 45) {
            return RISK_LEVEL_MEDIUM;
        }
        return RISK_LEVEL_LOW;
    }

    private String resolveTrendDirection(long recent, long previous) {
        if (recent > previous) {
            return TREND_RISING;
        }
        if (recent < previous) {
            return TREND_FALLING;
        }
        return TREND_STABLE;
    }

    private String resolveReasonLabel(String reasonKey) {
        if (reasonKey == null || reasonKey.isBlank()) {
            return "未知原因";
        }
        if (reasonKey.contains("超出")) {
            return "超出打卡范围";
        }
        if (reasonKey.contains("配置")) {
            return "打卡点未配置";
        }
        if (reasonKey.contains("启用")) {
            return "打卡点未启用";
        }
        if (reasonKey.contains("绑定")) {
            return "用户未绑定单位";
        }
        if (reasonKey.contains("定位无效") || AttendanceCheckInStatus.LOCATION_INVALID.equals(reasonKey)) {
            return "定位无效";
        }
        if (reasonKey.contains("定位质量较差") || AttendanceCheckInStatus.LOCATION_WEAK.equals(reasonKey)) {
            return "定位质量差";
        }
        if (reasonKey.contains("定位")) {
            return "定位信息缺失";
        }
        if (reasonKey.contains("已完成")) {
            return "今日考勤已完成";
        }
        return switch (reasonKey) {
            case AttendanceCheckInStatus.LOCATION_NOT_CONFIGURED -> "打卡点未配置";
            case AttendanceCheckInStatus.LOCATION_DISABLED -> "打卡点未启用";
            case AttendanceCheckInStatus.LOCATION_NOT_BOUND -> "用户未绑定单位";
            case AttendanceCheckInStatus.LOCATION_REQUIRED -> "定位信息缺失";
            case AttendanceCheckInStatus.LOCATION_INVALID -> "定位无效";
            case AttendanceCheckInStatus.LOCATION_WEAK -> "定位质量差";
            case AttendanceCheckInStatus.OUT_OF_RANGE -> "超出打卡范围";
            case AttendanceCheckInStatus.ALREADY_FINISHED -> "今日考勤已完成";
            default -> reasonKey;
        };
    }

    private String resolveReasonTag(String reasonKey) {
        if (reasonKey == null || reasonKey.isBlank()) {
            return "其他";
        }
        if (reasonKey.contains("配置") || AttendanceCheckInStatus.LOCATION_NOT_CONFIGURED.equals(reasonKey) || AttendanceCheckInStatus.LOCATION_DISABLED.equals(reasonKey)) {
            return "配置问题";
        }
        if (reasonKey.contains("范围") || AttendanceCheckInStatus.OUT_OF_RANGE.equals(reasonKey)) {
            return "范围问题";
        }
        if (reasonKey.contains("定位")
                || AttendanceCheckInStatus.LOCATION_REQUIRED.equals(reasonKey)
                || AttendanceCheckInStatus.LOCATION_INVALID.equals(reasonKey)
                || AttendanceCheckInStatus.LOCATION_WEAK.equals(reasonKey)) {
            return "定位问题";
        }
        if (reasonKey.contains("绑定") || AttendanceCheckInStatus.LOCATION_NOT_BOUND.equals(reasonKey)) {
            return "组织问题";
        }
        if (reasonKey.contains("已完成") || AttendanceCheckInStatus.ALREADY_FINISHED.equals(reasonKey)) {
            return "重复打卡";
        }
        return "其他";
    }

    private boolean isAlertTriggered(String riskLevel, long recent7Count, BigDecimal abnormalRate, String trendDirection) {
        double rate = abnormalRate == null ? 0D : abnormalRate.doubleValue();
        return RISK_LEVEL_HIGH.equals(riskLevel)
                || recent7Count >= 3
                || (TREND_RISING.equals(trendDirection) && recent7Count >= 2 && rate >= 70D);
    }

    private String buildAlertRuleText(String riskLevel,
                                      long recent7Count,
                                      String trendDirection,
                                      BigDecimal abnormalRate,
                                      String mainReasonTag) {
        List<String> hits = new ArrayList<>();
        double rate = abnormalRate == null ? 0D : abnormalRate.doubleValue();
        if (RISK_LEVEL_HIGH.equals(riskLevel)) {
            hits.add("综合风险高");
        }
        if (recent7Count >= 3) {
            hits.add("近7天异常>=3次");
        }
        if (TREND_RISING.equals(trendDirection)) {
            hits.add("近7天异常上升");
        }
        if (TREND_RISING.equals(trendDirection) && recent7Count >= 2 && rate >= 70D) {
            hits.add("异常率高");
        }
        return hits.isEmpty() ? "" : String.join(" / ", hits);
    }

    private AttendanceQueryRequest normalizeQueryRequest(AttendanceQueryRequest request) {
        AttendanceQueryRequest normalizedRequest = request == null ? new AttendanceQueryRequest() : request;
        LoginUser loginUser = currentLoginUser();
        if (!permissionService.isSuperAdmin(loginUser.getUserId())) {
            dataScopeService.injectTreePathScope(normalizedRequest, requireCurrentUser(loginUser.getUserId()));
        }
        normalizedRequest.setKeywords(normalizeText(normalizedRequest.getKeywords()));
        normalizedRequest.setUnitName(normalizeText(normalizedRequest.getUnitName()));
        normalizedRequest.setPageNo(normalizePageNo(normalizedRequest.getPageNo()));
        normalizedRequest.setPageSize(normalizePageSize(normalizedRequest.getPageSize()));
        validateQueryDateRange(normalizedRequest);
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
        target.setPageNo(source.getPageNo());
        target.setPageSize(source.getPageSize());
        return target;
    }

    private Integer normalizePageNo(Integer pageNo) {
        return pageNo == null || pageNo < 1 ? 1 : pageNo;
    }

    private Integer normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return 10;
        }
        return Math.min(pageSize, 100);
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
    LocalDateTime amOffTime = parseDateTime(request.getAmOffTime());
    LocalDateTime pmOnTime = parseDateTime(request.getPmOnTime());
    LocalDateTime checkOutTime = parseDateTime(request.getCheckOutTime());

    validateAttendanceTimesForFourSteps(attendanceDate, checkInTime, amOffTime, pmOnTime, checkOutTime);

    String checkType = resolveCheckTypeForFourSteps(checkInTime, amOffTime, pmOnTime, checkOutTime);
    LocalDateTime checkTime = resolveCheckTimeForFourSteps(checkType, checkInTime, amOffTime, pmOnTime, checkOutTime);

    if (checkTime == null) {
        throw new IllegalArgumentException("补录/编辑考勤时必须至少填写一个打卡时间");
    }

    if (request.getId() == null) {
        AttendanceRecordEntity duplicated = attendanceMapper.findByUserIdAndDate(targetUserId, attendanceDate);
        if (duplicated != null) {
            throw new IllegalArgumentException("该用户当天考勤已存在");
        }

        AttendanceRecordEntity entity = new AttendanceRecordEntity();
        entity.setUnitId(targetUser.getUnitId());
        entity.setUserId(targetUserId);
        entity.setAttendanceDate(attendanceDate);
        entity.setCheckType(checkType);
        entity.setCheckTime(checkTime);

        entity.setCheckInTime(checkInTime);
        entity.setAmOffTime(amOffTime);
        entity.setPmOnTime(pmOnTime);
        entity.setCheckOutTime(checkOutTime);

        entity.setCheckInAddress(normalizeText(request.getCheckInAddress()));
        entity.setAmOffAddress(normalizeText(request.getAmOffAddress()));
        entity.setPmOnAddress(normalizeText(request.getPmOnAddress()));
        entity.setCheckOutAddress(normalizeText(request.getCheckOutAddress()));

        entity.setLocationSource("MANUAL");
        entity.setLocationProvider("BACKOFFICE");
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
    existed.setCheckType(checkType);
    existed.setCheckTime(checkTime);

    existed.setCheckInTime(checkInTime);
    existed.setAmOffTime(amOffTime);
    existed.setPmOnTime(pmOnTime);
    existed.setCheckOutTime(checkOutTime);

    existed.setCheckInAddress(normalizeText(request.getCheckInAddress()));
    existed.setAmOffAddress(normalizeText(request.getAmOffAddress()));
    existed.setPmOnAddress(normalizeText(request.getPmOnAddress()));
    existed.setCheckOutAddress(normalizeText(request.getCheckOutAddress()));

    existed.setLocationSource("MANUAL");
    existed.setLocationProvider("BACKOFFICE");
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

    private String resolveLocationSource(CheckInRequest request) {
        String source = normalizeText(request == null ? null : request.getLocationSource());
        return source == null ? "BROWSER_GEO" : source.toUpperCase();
    }

    private String resolveLocationProvider(CheckInRequest request) {
        String provider = normalizeText(request == null ? null : request.getLocationProvider());
        if (provider != null) {
            return provider.toUpperCase();
        }
        String source = resolveLocationSource(request);
        if (source.startsWith("WECHAT")) {
            return "WECHAT";
        }
        return "BROWSER";
    }

    private void logCheckInFailureDiagnostic(LoginUser loginUser,
                                             CheckInRequest request,
                                             String stage,
                                             String errorCode,
                                             Integer distanceMeters,
                                             Integer radiusMeters,
                                             String rawMessage) {
        String locationSource = request == null ? "" : resolveLocationSource(request);
        log.warn(
                "attendance check-in diagnostic userId={} errorCode={} locationSource={} stage={} latitude={} longitude={} accuracy={} distanceMeters={} radiusMeters={} fallbackEnabled={} rawMessage={} locationProvider={}",
                loginUser == null ? null : loginUser.getUserId(),
                errorCode,
                locationSource,
                stage,
                request == null ? null : request.getLatitude(),
                request == null ? null : request.getLongitude(),
                request == null ? null : request.getAccuracyMeters(),
                distanceMeters,
                radiusMeters,
                locationSource.contains("FALLBACK"),
                normalizeText(rawMessage),
                request == null ? "" : resolveLocationProvider(request)
        );
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

    private void validateQueryDateRange(AttendanceQueryRequest request) {
        LocalDate dateFrom = parseDate(request.getDateFrom());
        LocalDate dateTo = parseDate(request.getDateTo());
        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            throw new IllegalArgumentException("开始日期不能晚于结束日期");
        }
    }

    private LocalDate parseDate(String text) {
        return text == null || text.isBlank() ? null : LocalDate.parse(text);
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
        if (location.getLatitude() == null
                || location.getLongitude() == null
                || location.getRadiusMeters() == null
                || location.getRadiusMeters() <= 0) {
            return new CheckInScope(location, unitName, "当前单位打卡点配置不完整，请联系管理员重新保存", AttendanceCheckInStatus.LOCATION_NOT_CONFIGURED);
        }
        return new CheckInScope(location, unitName, null, AttendanceCheckInStatus.LOCATION_READY);
    }

    private Map<String, Object> buildCheckInResult(boolean success,
                                                   String action,
                                                   CheckInScope scope,
                                                   Integer distanceMeters,
                                                   String reason) {
        return buildCheckInResult(success, action, scope, distanceMeters, reason, scope.status, null, null, scope.status, false, 0);
    }

    private Map<String, Object> buildCheckInResult(boolean success,
                                                   String action,
                                                   CheckInScope scope,
                                                   Integer distanceMeters,
                                                   String reason,
                                                   String status) {
        return buildCheckInResult(success, action, scope, distanceMeters, reason, status, null, null, status, false, 0);
    }

    private Map<String, Object> buildCheckInResult(boolean success,
                                               String action,
                                               CheckInScope scope,
                                               Integer distanceMeters,
                                               String reason,
                                               String status,
                                               AttendanceRecordEntity entity,
                                               Integer accuracyMeters,
                                               String decisionBranch,
                                               boolean weakToleranceApplied,
                                               Integer toleranceMeters) {
        return buildCheckInResult(
                success,
                action,
                scope,
                distanceMeters,
                reason,
                status,
                entity,
                accuracyMeters,
                decisionBranch,
                weakToleranceApplied,
                toleranceMeters,
                null
        );
    }

    private Map<String, Object> buildCheckInResult(boolean success,
                                               String action,
                                               CheckInScope scope,
                                               Integer distanceMeters,
                                               String reason,
                                               String status,
                                               AttendanceRecordEntity entity,
                                               Integer accuracyMeters,
                                               String decisionBranch,
                                               boolean weakToleranceApplied,
                                               Integer toleranceMeters,
                                               String evidenceRequiredNodeCode) {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("success", success);
    result.put("allowCheckIn", success);
    result.put("action", action);
    result.put("actionLabel", resolveActionLabel(action));
    result.put("status", status);
    result.put("reason", reason);
    result.put("failReason", reason);
    result.put("distanceMeters", distanceMeters);
    result.put("configured", scope.location != null);
    result.put("unitName", scope.unitName);
    result.put("radiusMeters", scope.location == null ? null : scope.location.getRadiusMeters());
    result.put("accuracyMeters", accuracyMeters);
    result.put("decisionBranch", decisionBranch);
    result.put("weakToleranceApplied", weakToleranceApplied);
    result.put("toleranceMeters", toleranceMeters);
    result.put("locationName", scope.location == null ? null : scope.location.getLocationName());
    result.put("locationAddress", scope.location == null ? null : scope.location.getAddress());

    AttendanceNodeStateResolver.ResolvedAttendanceState resolvedState = resolveAttendanceState(
            resolveDecisionUnitId(entity),
            resolveDecisionUserId(entity),
            resolveDecisionAttendanceDate(entity),
            entity,
            LocalTime.now(),
            evidenceRequiredNodeCode
    );

    if (entity != null) {
        result.put("id", entity.getId());
        result.put("attendanceDate", entity.getAttendanceDate());
        result.put("checkType", entity.getCheckType());

        result.put("checkInTime", entity.getCheckInTime());
        result.put("amOffTime", entity.getAmOffTime());
        result.put("pmOnTime", entity.getPmOnTime());
        result.put("checkOutTime", entity.getCheckOutTime());

        result.put("validFlag", entity.getValidFlag());
    }

    applyResolvedAttendanceState(result, resolvedState, evidenceRequiredNodeCode);

    return result;
}

    private Map<String, Object> persistCheckInSuccess(UserEntity currentUser,
                                                  LoginUser loginUser,
                                                  LocalDate today,
                                                  LocalDateTime now,
                                                  CheckInRequest request,
                                                  CheckInScope scope,
                                                  Integer distanceMeters,
                                                  Integer accuracyMeters,
                                                  boolean weakToleranceApplied,
                                                  String decisionBranch) {
    AttendanceRecordEntity entity = attendanceMapper.findByUserIdAndDate(loginUser.getUserId(), today);
    String requestedAction = resolveRequestedCheckAction(request);
    TimeDrivenActionDecision currentDecision = resolveTimeDrivenActionDecision(
            currentUser.getUnitId(),
            entity,
            now.toLocalTime()
    );

    if (currentDecision.finished) {
        return buildCheckInResult(
                false,
                null,
                scope,
                distanceMeters,
                currentDecision.hint,
                AttendanceCheckInStatus.ALREADY_FINISHED,
                entity,
                accuracyMeters,
                "FINISHED",
                false,
                0
        );
    }

    if (!currentDecision.actionAvailable || currentDecision.action == null) {
        return buildCheckInResult(
                false,
                null,
                scope,
                distanceMeters,
                currentDecision.hint,
                AttendanceCheckInStatus.ALREADY_FINISHED,
                entity,
                accuracyMeters,
                "TIME_WINDOW_BLOCKED",
                false,
                0
        );
    }

    String action = resolveEffectiveCheckAction(requestedAction, currentDecision.action);
    if (!currentDecision.action.equals(action)) {
        return buildCheckInResult(
                false,
                null,
                scope,
                distanceMeters,
                "当前时段应执行：" + currentDecision.actionLabel,
                AttendanceCheckInStatus.ALREADY_FINISHED,
                entity,
                accuracyMeters,
                "ACTION_MISMATCH",
                false,
                0
        );
    }

    if (CHECK_ACTION_AM_ON.equals(action)) {
        if (entity != null && entity.getCheckInTime() != null) {
            return buildDuplicateCheckInResult(scope, distanceMeters, accuracyMeters, entity, "AM_ON_DUPLICATE");
        }
        AttendanceRecordEntity insertEntity = buildCheckInEntity(currentUser, loginUser, today, now, request, distanceMeters);
        try {
            attendanceMapper.insert(insertEntity);
            entity = insertEntity;
        } catch (DuplicateKeyException ex) {
            log.warn("attendance check-in duplicate insert intercepted userId={} attendanceDate={}", loginUser.getUserId(), today, ex);
            AttendanceRecordEntity latest = attendanceMapper.findByUserIdAndDate(loginUser.getUserId(), today);
            if (latest == null) {
                throw ex;
            }
            return buildDuplicateCheckInResult(scope, distanceMeters, accuracyMeters, latest, "DUPLICATE_INSERT");
        }
    } else {
        if (entity == null) {
            return buildCheckInResult(
                    false,
                    null,
                    scope,
                    distanceMeters,
                    REASON_CHECK_IN_REQUIRED,
                    AttendanceCheckInStatus.LOCATION_REQUIRED,
                    null,
                    accuracyMeters,
                    "NODE_REQUIRED",
                    false,
                    0
            );
        }

        if (CHECK_ACTION_AM_OFF.equals(action)) {
            if (entity.getCheckInTime() == null) {
                return buildCheckInResult(false, null, scope, distanceMeters, "请先完成上午上班打卡",
                        AttendanceCheckInStatus.LOCATION_REQUIRED, entity, accuracyMeters, "AM_OFF_BEFORE_AM_ON", false, 0);
            }
            if (entity.getAmOffTime() != null) {
                return buildDuplicateCheckInResult(scope, distanceMeters, accuracyMeters, entity, "AM_OFF_DUPLICATE");
            }
            entity.setAmOffTime(now);
            entity.setAmOffAddress(normalizeText(request.getAddress()));
            entity.setAmOffLatitude(request.getLatitude());
            entity.setAmOffLongitude(request.getLongitude());
            entity.setAmOffDistanceMeters(distanceMeters);
        } else if (CHECK_ACTION_PM_ON.equals(action)) {
            if (entity.getPmOnTime() != null) {
                return buildDuplicateCheckInResult(scope, distanceMeters, accuracyMeters, entity, "PM_ON_DUPLICATE");
            }
            entity.setPmOnTime(now);
            entity.setPmOnAddress(normalizeText(request.getAddress()));
            entity.setPmOnLatitude(request.getLatitude());
            entity.setPmOnLongitude(request.getLongitude());
            entity.setPmOnDistanceMeters(distanceMeters);
        } else if (CHECK_ACTION_PM_OFF.equals(action)) {
            if (entity.getCheckOutTime() != null) {
                return buildDuplicateCheckInResult(scope, distanceMeters, accuracyMeters, entity, "PM_OFF_DUPLICATE");
            }
            entity.setCheckOutTime(now);
            entity.setCheckOutAddress(normalizeText(request.getAddress()));
            entity.setCheckOutLatitude(request.getLatitude());
            entity.setCheckOutLongitude(request.getLongitude());
            entity.setCheckOutDistanceMeters(distanceMeters);
        } else {
            return buildCheckInResult(
                    false,
                    null,
                    scope,
                    distanceMeters,
                    REASON_TODAY_FINISHED,
                    AttendanceCheckInStatus.ALREADY_FINISHED,
                    entity,
                    accuracyMeters,
                    "UNKNOWN_ACTION",
                    false,
                    0
            );
        }

        entity.setCheckType(action);
        entity.setCheckTime(resolveCheckTime(action, entity));
        entity.setCheckInResult(CHECK_ACTION_PM_OFF.equals(action)
                ? AttendanceCheckInStatus.CHECK_OUT_SUCCESS
                : AttendanceCheckInStatus.CHECK_IN_SUCCESS);
        entity.setCheckInFailReason(null);
        entity.setLocationSource(resolveLocationSource(request));
        entity.setLocationProvider(resolveLocationProvider(request));
        attendanceMapper.update(entity);
    }

    log.info(
            "attendance check-in accepted userId={} branch={} action={} distanceMeters={} radiusMeters={} accuracyMeters={} weakToleranceApplied={}",
            loginUser.getUserId(),
            decisionBranch,
            action,
            distanceMeters,
            scope.location.getRadiusMeters(),
            accuracyMeters,
            weakToleranceApplied
    );

    return buildCheckInResult(
            true,
            action,
            scope,
            distanceMeters,
            null,
            CHECK_ACTION_PM_OFF.equals(action)
                    ? AttendanceCheckInStatus.CHECK_OUT_SUCCESS
                    : AttendanceCheckInStatus.CHECK_IN_SUCCESS,
            entity,
            accuracyMeters,
            decisionBranch,
            weakToleranceApplied,
            resolveWeakToleranceMeters(accuracyMeters)
    );
}

    private Map<String, Object> persistCheckInSuccessStateDriven(UserEntity currentUser,
                                                                 LoginUser loginUser,
                                                                 LocalDate today,
                                                                 LocalDateTime now,
                                                                 CheckInRequest request,
                                                                 CheckInScope scope,
                                                                 Integer distanceMeters,
                                                                 Integer accuracyMeters,
                                                                 boolean weakToleranceApplied,
                                                                 String decisionBranch) {
        AttendanceRecordEntity entity = attendanceMapper.findByUserIdAndDate(loginUser.getUserId(), today);
        String requestedAction = resolveRequestedCheckAction(request);
        AttendanceNodeStateResolver.ResolvedAttendanceState resolvedState = resolveAttendanceState(
                currentUser.getUnitId(),
                loginUser.getUserId(),
                today,
                entity,
                now.toLocalTime(),
                null
        );
        AttendanceNodeStatusVO targetNode = resolveTargetNodeState(resolvedState, requestedAction);

        if (targetNode == null) {
            return buildCheckInResult(
                    false,
                    null,
                    scope,
                    distanceMeters,
                    resolvedState.getHint(),
                    resolvedState.isFinished() ? AttendanceCheckInStatus.ALREADY_FINISHED : resolvedState.getFinalStatusCode(),
                    entity,
                    accuracyMeters,
                    resolvedState.isFinished() ? "FINISHED" : "NO_AVAILABLE_NODE",
                    false,
                    0
            );
        }

        if (!Boolean.TRUE.equals(targetNode.getCanPunch())) {
            return buildCheckInResult(
                    false,
                    targetNode.getNodeCode(),
                    scope,
                    distanceMeters,
                    targetNode.getSimpleRemark(),
                    targetNode.getStatusCode(),
                    entity,
                    accuracyMeters,
                    "NODE_BLOCKED",
                    false,
                    0
            );
        }

        String action = targetNode.getNodeCode();
        if (entity == null) {
            entity = buildEmptyAttendanceEntity(currentUser, loginUser, today);
        }
        if (isActionAlreadyCompleted(entity, action)) {
            return buildDuplicateCheckInResult(scope, distanceMeters, accuracyMeters, entity, action + "_DUPLICATE");
        }

        applyCheckAction(entity, action, now, request, distanceMeters);
        entity.setCheckType(action);
        entity.setCheckTime(resolveCheckTime(action, entity));
        entity.setCheckInResult(CHECK_ACTION_PM_OFF.equals(action)
                ? AttendanceCheckInStatus.CHECK_OUT_SUCCESS
                : AttendanceCheckInStatus.CHECK_IN_SUCCESS);
        entity.setCheckInFailReason(null);
        entity.setLocationSource(resolveLocationSource(request));
        entity.setLocationProvider(resolveLocationProvider(request));

        if (entity.getId() == null) {
            try {
                attendanceMapper.insert(entity);
            } catch (DuplicateKeyException ex) {
                log.warn("attendance check-in duplicate insert intercepted userId={} attendanceDate={}", loginUser.getUserId(), today, ex);
                AttendanceRecordEntity latest = attendanceMapper.findByUserIdAndDate(loginUser.getUserId(), today);
                if (latest == null) {
                    throw ex;
                }
                return buildDuplicateCheckInResult(scope, distanceMeters, accuracyMeters, latest, "DUPLICATE_INSERT");
            }
        } else {
            attendanceMapper.update(entity);
        }

        log.info(
                "attendance check-in accepted userId={} branch={} action={} distanceMeters={} radiusMeters={} accuracyMeters={} weakToleranceApplied={}",
                loginUser.getUserId(),
                decisionBranch,
                action,
                distanceMeters,
                scope.location.getRadiusMeters(),
                accuracyMeters,
                weakToleranceApplied
        );

        return buildCheckInResult(
                true,
                action,
                scope,
                distanceMeters,
                null,
                CHECK_ACTION_PM_OFF.equals(action)
                        ? AttendanceCheckInStatus.CHECK_OUT_SUCCESS
                        : AttendanceCheckInStatus.CHECK_IN_SUCCESS,
                entity,
                accuracyMeters,
                decisionBranch,
                weakToleranceApplied,
                resolveWeakToleranceMeters(accuracyMeters)
        );
    }

    private AttendanceRecordEntity buildCheckInEntity(UserEntity currentUser,
                                                  LoginUser loginUser,
                                                  LocalDate today,
                                                  LocalDateTime now,
                                                  CheckInRequest request,
                                                  Integer distanceMeters) {
    AttendanceRecordEntity entity = new AttendanceRecordEntity();
    entity.setUnitId(currentUser.getUnitId());
    entity.setUserId(loginUser.getUserId());
    entity.setAttendanceDate(today);
    entity.setCheckType(CHECK_ACTION_AM_ON);
    entity.setCheckInTime(now);
    entity.setCheckTime(now);
    entity.setCheckInAddress(normalizeText(request.getAddress()));
    entity.setCheckInLatitude(request.getLatitude());
    entity.setCheckInLongitude(request.getLongitude());
    entity.setCheckInDistanceMeters(distanceMeters);
    entity.setCheckInResult(AttendanceCheckInStatus.CHECK_IN_SUCCESS);
    entity.setCheckInFailReason(null);
    entity.setLocationSource(resolveLocationSource(request));
    entity.setLocationProvider(resolveLocationProvider(request));
    entity.setValidFlag(1);
    return entity;
}

    private AttendanceRecordEntity buildEmptyAttendanceEntity(UserEntity currentUser,
                                                              LoginUser loginUser,
                                                              LocalDate today) {
        AttendanceRecordEntity entity = new AttendanceRecordEntity();
        entity.setUnitId(currentUser.getUnitId());
        entity.setUserId(loginUser.getUserId());
        entity.setAttendanceDate(today);
        entity.setValidFlag(1);
        return entity;
    }

    private boolean isActionAlreadyCompleted(AttendanceRecordEntity entity, String action) {
        if (entity == null || action == null) {
            return false;
        }
        if (CHECK_ACTION_AM_ON.equals(action)) {
            return entity.getCheckInTime() != null;
        }
        if (CHECK_ACTION_AM_OFF.equals(action)) {
            return entity.getAmOffTime() != null;
        }
        if (CHECK_ACTION_PM_ON.equals(action)) {
            return entity.getPmOnTime() != null;
        }
        if (CHECK_ACTION_PM_OFF.equals(action)) {
            return entity.getCheckOutTime() != null;
        }
        return false;
    }

    private void applyCheckAction(AttendanceRecordEntity entity,
                                  String action,
                                  LocalDateTime now,
                                  CheckInRequest request,
                                  Integer distanceMeters) {
        if (CHECK_ACTION_AM_ON.equals(action)) {
            entity.setCheckInTime(now);
            entity.setCheckInAddress(normalizeText(request.getAddress()));
            entity.setCheckInLatitude(request.getLatitude());
            entity.setCheckInLongitude(request.getLongitude());
            entity.setCheckInDistanceMeters(distanceMeters);
            return;
        }
        if (CHECK_ACTION_AM_OFF.equals(action)) {
            entity.setAmOffTime(now);
            entity.setAmOffAddress(normalizeText(request.getAddress()));
            entity.setAmOffLatitude(request.getLatitude());
            entity.setAmOffLongitude(request.getLongitude());
            entity.setAmOffDistanceMeters(distanceMeters);
            return;
        }
        if (CHECK_ACTION_PM_ON.equals(action)) {
            entity.setPmOnTime(now);
            entity.setPmOnAddress(normalizeText(request.getAddress()));
            entity.setPmOnLatitude(request.getLatitude());
            entity.setPmOnLongitude(request.getLongitude());
            entity.setPmOnDistanceMeters(distanceMeters);
            return;
        }
        if (CHECK_ACTION_PM_OFF.equals(action)) {
            entity.setCheckOutTime(now);
            entity.setCheckOutAddress(normalizeText(request.getAddress()));
            entity.setCheckOutLatitude(request.getLatitude());
            entity.setCheckOutLongitude(request.getLongitude());
            entity.setCheckOutDistanceMeters(distanceMeters);
        }
    }

    private String resolveRequestedCheckAction(CheckInRequest request) {
    String action = normalizeText(request == null ? null : request.getAction());
    if (action == null) {
        return null;
    }
    String normalized = action.toUpperCase();
    if (CHECK_ACTION_AM_ON.equals(normalized)
            || CHECK_ACTION_AM_OFF.equals(normalized)
            || CHECK_ACTION_PM_ON.equals(normalized)
            || CHECK_ACTION_PM_OFF.equals(normalized)) {
        return normalized;
    }
    return null;
}

    private String resolveEffectiveCheckAction(String requestedAction, String currentAction) {
    if (currentAction == null) {
        return null;
    }
    if (requestedAction == null) {
        return currentAction;
    }
    return requestedAction;
}

    private Map<String, Object> buildDuplicateCheckInResult(CheckInScope scope,
                                                        Integer distanceMeters,
                                                        Integer accuracyMeters,
                                                        AttendanceRecordEntity entity,
                                                        String decisionBranch) {
    return buildCheckInResult(
            false,
            null,
            scope,
            distanceMeters,
            resolveDuplicateReason(entity),
            AttendanceCheckInStatus.ALREADY_FINISHED,
            entity,
            accuracyMeters,
            decisionBranch,
            false,
            0
    );
}

    private String resolveCheckType(LocalDateTime checkInTime, LocalDateTime checkOutTime) {
        if (checkOutTime != null) {
            return "CHECK_OUT";
        }
        if (checkInTime != null) {
            return "CHECK_IN";
        }
        return "CHECK_IN";
    }

    private void validateAttendanceTimesForFourSteps(LocalDate attendanceDate,
                                                 LocalDateTime checkInTime,
                                                 LocalDateTime amOffTime,
                                                 LocalDateTime pmOnTime,
                                                 LocalDateTime checkOutTime) {
    if (checkInTime != null && !attendanceDate.equals(checkInTime.toLocalDate())) {
        throw new IllegalArgumentException("上午上班时间必须落在考勤日期当天");
    }
    if (amOffTime != null && !attendanceDate.equals(amOffTime.toLocalDate())) {
        throw new IllegalArgumentException("上午下班时间必须落在考勤日期当天");
    }
    if (pmOnTime != null && !attendanceDate.equals(pmOnTime.toLocalDate())) {
        throw new IllegalArgumentException("下午上班时间必须落在考勤日期当天");
    }
    if (checkOutTime != null && !attendanceDate.equals(checkOutTime.toLocalDate())) {
        throw new IllegalArgumentException("下午下班时间必须落在考勤日期当天");
    }

    if (amOffTime != null && checkInTime == null) {
        throw new IllegalArgumentException("填写上午下班时间前，必须先填写上午上班时间");
    }
    if (pmOnTime != null && amOffTime == null) {
        throw new IllegalArgumentException("填写下午上班时间前，必须先填写上午下班时间");
    }
    if (checkOutTime != null && pmOnTime == null) {
        throw new IllegalArgumentException("填写下午下班时间前，必须先填写下午上班时间");
    }

    if (checkInTime != null && amOffTime != null && !amOffTime.isAfter(checkInTime)) {
        throw new IllegalArgumentException("上午下班时间必须晚于上午上班时间");
    }
    if (amOffTime != null && pmOnTime != null && !pmOnTime.isAfter(amOffTime)) {
        throw new IllegalArgumentException("下午上班时间必须晚于上午下班时间");
    }
    if (pmOnTime != null && checkOutTime != null && !checkOutTime.isAfter(pmOnTime)) {
        throw new IllegalArgumentException("下午下班时间必须晚于下午上班时间");
    }
}

private String resolveCheckTypeForFourSteps(LocalDateTime checkInTime,
                                            LocalDateTime amOffTime,
                                            LocalDateTime pmOnTime,
                                            LocalDateTime checkOutTime) {
    if (checkOutTime != null) {
        return CHECK_ACTION_PM_OFF;
    }
    if (pmOnTime != null) {
        return CHECK_ACTION_PM_ON;
    }
    if (amOffTime != null) {
        return CHECK_ACTION_AM_OFF;
    }
    if (checkInTime != null) {
        return CHECK_ACTION_AM_ON;
    }
    return CHECK_ACTION_AM_ON;
}

private LocalDateTime resolveCheckTimeForFourSteps(String checkType,
                                                   LocalDateTime checkInTime,
                                                   LocalDateTime amOffTime,
                                                   LocalDateTime pmOnTime,
                                                   LocalDateTime checkOutTime) {
    if (CHECK_ACTION_PM_OFF.equals(checkType)) {
        return checkOutTime;
    }
    if (CHECK_ACTION_PM_ON.equals(checkType)) {
        return pmOnTime;
    }
    if (CHECK_ACTION_AM_OFF.equals(checkType)) {
        return amOffTime;
    }
    return checkInTime;
}

    private LocalDateTime resolveCheckTime(String checkType, LocalDateTime checkInTime, LocalDateTime checkOutTime) {
        if ("CHECK_OUT".equals(checkType) && checkOutTime != null) {
            return checkOutTime;
        }
        if (checkInTime != null) {
            return checkInTime;
        }
        return checkOutTime;
    }

    private String resolveNextCheckAction(AttendanceRecordEntity entity) {
    AttendanceNodeStateResolver.ResolvedAttendanceState resolvedState = resolveAttendanceState(
            resolveDecisionUnitId(entity),
            resolveDecisionUserId(entity),
            resolveDecisionAttendanceDate(entity),
            entity,
            LocalTime.now(),
            null
    );
    return resolvedState.getCurrentAction();
}

private String resolveActionLabel(String action) {
    if (CHECK_ACTION_AM_ON.equals(action)) {
        return "上午上班打卡";
    }
    if (CHECK_ACTION_AM_OFF.equals(action)) {
        return "上午下班打卡";
    }
    if (CHECK_ACTION_PM_ON.equals(action)) {
        return "下午上班打卡";
    }
    if (CHECK_ACTION_PM_OFF.equals(action)) {
        return "下午下班打卡";
    }
    return "今日已完成";
}

private String resolveDuplicateReason(AttendanceRecordEntity entity) {
    String nextAction = resolveNextCheckAction(entity);
    if (CHECK_ACTION_AM_OFF.equals(nextAction)) {
        return REASON_AM_ON_COMPLETED;
    }
    if (CHECK_ACTION_PM_ON.equals(nextAction)) {
        return REASON_AM_OFF_COMPLETED;
    }
    if (CHECK_ACTION_PM_OFF.equals(nextAction)) {
        return REASON_PM_ON_COMPLETED;
    }
    if (nextAction == null) {
        return REASON_TODAY_FINISHED;
    }
    return REASON_PM_OFF_COMPLETED;
}

private LocalDateTime resolveCheckTime(String checkType, AttendanceRecordEntity entity) {
    if (CHECK_ACTION_PM_OFF.equals(checkType)) {
        return entity.getCheckOutTime();
    }
    if (CHECK_ACTION_PM_ON.equals(checkType)) {
        return entity.getPmOnTime();
    }
    if (CHECK_ACTION_AM_OFF.equals(checkType)) {
        return entity.getAmOffTime();
    }
    return entity.getCheckInTime();
}
private static final class TimeDrivenActionDecision {
    private final String action;
    private final String actionLabel;
    private final boolean actionAvailable;
    private final boolean finished;
    private final String hint;

    private TimeDrivenActionDecision(String action,
                                     String actionLabel,
                                     boolean actionAvailable,
                                     boolean finished,
                                     String hint) {
        this.action = action;
        this.actionLabel = actionLabel;
        this.actionAvailable = actionAvailable;
        this.finished = finished;
        this.hint = hint;
    }
}

private Long resolveDecisionUnitId(AttendanceRecordEntity entity) {
    if (entity != null && entity.getUnitId() != null) {
        return entity.getUnitId();
    }
    try {
        LoginUser loginUser = currentLoginUser();
        UserEntity currentUser = requireCurrentUser(loginUser.getUserId());
        return currentUser.getUnitId();
    } catch (Exception ignore) {
        return null;
    }
}

private Long resolveDecisionUserId(AttendanceRecordEntity entity) {
    if (entity != null && entity.getUserId() != null) {
        return entity.getUserId();
    }
    try {
        return currentLoginUser().getUserId();
    } catch (Exception ignore) {
        return null;
    }
}

private LocalDate resolveDecisionAttendanceDate(AttendanceRecordEntity entity) {
    if (entity != null && entity.getAttendanceDate() != null) {
        return entity.getAttendanceDate();
    }
    return LocalDate.now();
}

private AttendanceNodeStateResolver.ResolvedAttendanceState resolveAttendanceState(Long unitId,
                                                                                   Long userId,
                                                                                   LocalDate attendanceDate,
                                                                                   AttendanceRecordEntity entity,
                                                                                   LocalTime nowTime,
                                                                                   String evidenceRequiredNodeCode) {
    AttendanceRuleEntity rule = findActiveAttendanceRule(unitId);
    List<AttendancePatchApplyEntity> patchApplies = resolvePatchApplies(userId, attendanceDate);
    return AttendanceNodeStateResolver.resolve(rule, entity, patchApplies, nowTime, evidenceRequiredNodeCode);
}

private List<AttendancePatchApplyEntity> resolvePatchApplies(Long userId, LocalDate attendanceDate) {
    if (attendancePatchApplyMapper == null || userId == null || attendanceDate == null) {
        return List.of();
    }
    try {
        List<AttendancePatchApplyEntity> items = attendancePatchApplyMapper.queryByUserAndDate(userId, attendanceDate);
        return items == null ? List.of() : items;
    } catch (Exception ex) {
        log.warn("attendance patch apply query failed userId={} attendanceDate={}", userId, attendanceDate, ex);
        return List.of();
    }
}

private AttendanceNodeStatusVO resolveTargetNodeState(AttendanceNodeStateResolver.ResolvedAttendanceState resolvedState,
                                                      String requestedAction) {
    if (resolvedState == null) {
        return null;
    }
    AttendanceNodeStatusVO requestedNode = findNodeState(resolvedState.getNodeStates(), requestedAction);
    if (requestedNode != null) {
        return requestedNode;
    }
    AttendanceNodeStatusVO currentNode = findNodeState(resolvedState.getNodeStates(), resolvedState.getCurrentAction());
    if (currentNode != null) {
        return currentNode;
    }
    for (AttendanceNodeStatusVO nodeState : resolvedState.getNodeStates()) {
        if (Boolean.TRUE.equals(nodeState.getCanPunch())) {
            return nodeState;
        }
    }
    return null;
}

private AttendanceNodeStatusVO findNodeState(List<AttendanceNodeStatusVO> nodeStates, String nodeCode) {
    if (nodeStates == null || nodeCode == null) {
        return null;
    }
    for (AttendanceNodeStatusVO nodeState : nodeStates) {
        if (nodeCode.equals(nodeState.getNodeCode())) {
            return nodeState;
        }
    }
    return null;
}

private String resolveEvidenceNodeCode(Long unitId,
                                       Long userId,
                                       LocalDate attendanceDate,
                                       AttendanceRecordEntity entity,
                                       LocalTime nowTime,
                                       String requestedAction) {
    AttendanceNodeStateResolver.ResolvedAttendanceState resolvedState = resolveAttendanceState(
            unitId,
            userId,
            attendanceDate,
            entity,
            nowTime,
            null
    );
    AttendanceNodeStatusVO targetNode = resolveTargetNodeState(resolvedState, requestedAction);
    if (targetNode != null) {
        return targetNode.getNodeCode();
    }
    return requestedAction;
}

private void applyResolvedAttendanceState(Map<String, Object> result,
                                          AttendanceNodeStateResolver.ResolvedAttendanceState resolvedState,
                                          String evidenceRequiredNodeCode) {
    result.put("nextAction", resolvedState.getCurrentAction());
    result.put("nextActionLabel", resolvedState.getCurrentActionLabel());
    result.put("currentAction", resolvedState.getCurrentAction());
    result.put("currentActionLabel", resolvedState.getCurrentActionLabel());
    result.put("currentActionAvailable", resolvedState.isCurrentActionAvailable());
    result.put("currentActionHint", resolvedState.getHint());
    result.put("finished", resolvedState.isFinished());
    result.put("nodeStates", resolvedState.getNodeStates());
    result.put("attendanceStatusCode", resolvedState.getFinalStatusCode());
    result.put("attendanceFinalResolved", resolvedState.isFinalResolved());
    result.put("attendanceLate", resolvedState.isLate());
    result.put("attendanceEarly", resolvedState.isEarly());
    if (evidenceRequiredNodeCode != null) {
        result.put("evidenceNodeCode", evidenceRequiredNodeCode);
    }
}

private AttendanceRuleEntity findActiveAttendanceRule(Long unitId) {
    if (unitId == null || attendanceRuleMapper == null) {
        return null;
    }
    AttendanceRuleEntity rule = attendanceRuleMapper.findByUnitId(unitId);
    if (rule == null || rule.getStatus() == null || rule.getStatus() != 1) {
        return null;
    }
    if (rule.getWorkStartTime() == null
            || rule.getAmOffTime() == null
            || rule.getPmOnTime() == null
            || rule.getWorkEndTime() == null) {
        return null;
    }
    return rule;
}

private TimeDrivenActionDecision resolveTimeDrivenActionDecision(Long unitId,
                                                                 AttendanceRecordEntity entity,
                                                                 LocalTime nowTime) {
    boolean amOnDone = entity != null && entity.getCheckInTime() != null;
    boolean amOffDone = entity != null && entity.getAmOffTime() != null;
    boolean pmOnDone = entity != null && entity.getPmOnTime() != null;
    boolean pmOffDone = entity != null && entity.getCheckOutTime() != null;

    if (pmOffDone) {
        return finishedDecision("今日四次打卡已全部完成，无需重复提交。");
    }

    AttendanceRuleEntity rule = findActiveAttendanceRule(unitId);
    if (rule == null) {
        String fallbackAction = resolveNextCheckAction(entity);
        if (fallbackAction == null) {
            return finishedDecision("今日四次打卡已全部完成，无需重复提交。");
        }
        return availableDecision(fallbackAction, "当前未读取到完整四段规则，暂按顺序打卡。");
    }

    if (nowTime.isBefore(rule.getAmOffTime())) {
        if (!amOnDone) {
            return availableDecision(CHECK_ACTION_AM_ON, "当前为上午时段，请完成上午上班打卡。");
        }
        if (!amOffDone) {
            return availableDecision(CHECK_ACTION_AM_OFF, "当前为上午时段，请完成上午下班打卡。");
        }
        return unavailableDecision("上午打卡已完成，请在下午上班时间后继续打卡。");
    }

    if (nowTime.isBefore(rule.getPmOnTime())) {
        if (!amOnDone || !amOffDone) {
            return unavailableDecision("上午时段已结束，上午漏卡请走补卡申请。");
        }
        return unavailableDecision("当前为午休时段，请在下午上班时间后继续打卡。");
    }

    if (nowTime.isBefore(rule.getWorkEndTime())) {
        if (!pmOnDone) {
            return availableDecision(CHECK_ACTION_PM_ON, "当前为下午时段，请完成下午上班打卡；上午漏卡请走补卡申请。");
        }
        if (!pmOffDone) {
            return availableDecision(CHECK_ACTION_PM_OFF, "当前为下午时段，请完成下午下班打卡。");
        }
        return finishedDecision("今日四次打卡已全部完成，无需重复提交。");
    }

    if (!pmOnDone) {
        return unavailableDecision("下午上班时段已结束，请通过补卡申请补下午上班卡。");
    }
    if (!pmOffDone) {
        return availableDecision(CHECK_ACTION_PM_OFF, "已到下班时段，请完成下午下班打卡。");
    }
    return finishedDecision("今日四次打卡已全部完成，无需重复提交。");
}

private TimeDrivenActionDecision availableDecision(String action, String hint) {
    return new TimeDrivenActionDecision(
            action,
            resolveActionLabel(action),
            true,
            false,
            hint
    );
}

private TimeDrivenActionDecision unavailableDecision(String hint) {
    return new TimeDrivenActionDecision(
            null,
            "",
            false,
            false,
            hint
    );
}

private TimeDrivenActionDecision finishedDecision(String hint) {
    return new TimeDrivenActionDecision(
            null,
            "",
            false,
            true,
            hint
    );
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

    private Integer normalizeAccuracyMeters(Integer accuracyMeters) {
        if (accuracyMeters == null || accuracyMeters < 0) {
            return null;
        }
        return accuracyMeters;
    }

    private boolean isInvalidLocation(CheckInRequest request, Integer accuracyMeters, Integer maxAccuracyThreshold) {
        if (request == null || request.getLatitude() == null || request.getLongitude() == null) {
            return true;
        }
        double latitude = request.getLatitude().doubleValue();
        double longitude = request.getLongitude().doubleValue();
        if (Double.isNaN(latitude) || Double.isNaN(longitude)) {
            return true;
        }
        if (latitude < -90D || latitude > 90D || longitude < -180D || longitude > 180D) {
            return true;
        }
        if (Math.abs(latitude) < 0.000001D && Math.abs(longitude) < 0.000001D) {
            return true;
        }
        return accuracyMeters != null && accuracyMeters > (maxAccuracyThreshold == null ? DEFAULT_MAX_ACCURACY_METERS : maxAccuracyThreshold);
    }

    private boolean isWeakSuccess(Integer radiusMeters, Integer accuracyMeters, Integer distanceMeters) {
        if (radiusMeters == null || accuracyMeters == null || distanceMeters == null) {
            return false;
        }
        return accuracyMeters <= resolveGoodAccuracyThreshold()
                && distanceMeters <= radiusMeters + resolveWeakToleranceMeters(accuracyMeters);
    }

    private boolean isWeakLocation(Integer accuracyMeters, Integer goodAccuracyThreshold, Integer maxAccuracyThreshold) {
        if (accuracyMeters == null) {
            return false;
        }
        int goodThreshold = goodAccuracyThreshold == null ? DEFAULT_GOOD_ACCURACY_METERS : goodAccuracyThreshold;
        int maxThreshold = maxAccuracyThreshold == null ? DEFAULT_MAX_ACCURACY_METERS : maxAccuracyThreshold;
        return accuracyMeters > goodThreshold && accuracyMeters <= maxThreshold;
    }

    private int resolveWeakToleranceMeters(Integer accuracyMeters) {
        if (accuracyMeters == null || accuracyMeters <= 0) {
            return 0;
        }
        return Math.min(accuracyMeters, MAX_WEAK_TOLERANCE_METERS);
    }

    private String buildInvalidLocationReason(CheckInRequest request, Integer accuracyMeters, Integer maxAccuracyThreshold) {
        if (request == null || request.getLatitude() == null || request.getLongitude() == null) {
            return "定位失败，未获取到有效经纬度，请检查定位权限后重试";
        }
        int maxThreshold = maxAccuracyThreshold == null ? DEFAULT_MAX_ACCURACY_METERS : maxAccuracyThreshold;
        if (accuracyMeters != null && accuracyMeters > maxThreshold) {
            return "定位精度过低（约 " + accuracyMeters + " 米），不适合做 100 米围栏判断，请使用手机端或微信中重试";
        }
        if (accuracyMeters != null) {
            return "定位失败，当前坐标无效，定位精度约 " + accuracyMeters + " 米，请检查定位权限或定位服务后重试";
        }
        return "定位失败，请检查定位权限或定位服务后重试";
    }

    private String buildWeakLocationReason(Integer accuracyMeters, Integer goodAccuracyThreshold) {
        int goodThreshold = goodAccuracyThreshold == null ? DEFAULT_GOOD_ACCURACY_METERS : goodAccuracyThreshold;
        if (accuracyMeters != null) {
            return "定位精度不足（约 " + accuracyMeters + " 米），当前仅支持 " + goodThreshold + " 米内精度参与围栏判断，请确认后重试";
        }
        return "定位精度不足，请确认后重试";
    }

    private String buildOutOfRangeReason(Integer distanceMeters, Integer radiusMeters, Integer accuracyMeters) {
        StringBuilder builder = new StringBuilder();
        builder.append("当前位置距离打卡点 ")
                .append(distanceMeters == null ? "-" : distanceMeters)
                .append(" 米，允许半径 ")
                .append(radiusMeters == null ? "-" : radiusMeters)
                .append(" 米");
        if (accuracyMeters != null && radiusMeters != null && accuracyMeters > radiusMeters) {
            builder.append("；当前定位质量较差，定位精度约 ")
                    .append(accuracyMeters)
                    .append(" 米，请尽量在室外空旷区域重试");
        }
        return builder.toString();
    }

    private int resolveGoodAccuracyThreshold() {
        return resolveIntParam(PARAM_ATTENDANCE_LOCATION_ACCURACY_GOOD_THRESHOLD, DEFAULT_GOOD_ACCURACY_METERS, 1, DEFAULT_MAX_ACCURACY_METERS);
    }

    private int resolveMaxAccuracyThreshold() {
        return resolveIntParam(PARAM_ATTENDANCE_LOCATION_ACCURACY_MAX_THRESHOLD, DEFAULT_MAX_ACCURACY_METERS, DEFAULT_GOOD_ACCURACY_METERS, 5000);
    }

    private int resolveIntParam(String code, int fallback, int min, int max) {
        if (paramService == null) {
            return fallback;
        }
        try {
            String value = paramService.getByCode(code);
            if (value == null || value.isBlank()) {
                return fallback;
            }
            int parsed = Integer.parseInt(value.trim());
            if (parsed < min || parsed > max) {
                return fallback;
            }
            return parsed;
        } catch (Exception ex) {
            return fallback;
        }
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
