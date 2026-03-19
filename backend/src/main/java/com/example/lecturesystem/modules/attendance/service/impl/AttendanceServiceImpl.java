package com.example.lecturesystem.modules.attendance.service.impl;

import com.example.lecturesystem.modules.attendance.dto.AttendanceQueryRequest;
import com.example.lecturesystem.modules.attendance.dto.CheckInRequest;
import com.example.lecturesystem.modules.attendance.dto.SaveAttendanceRequest;
import com.example.lecturesystem.modules.attendance.entity.AttendanceRecordEntity;
import com.example.lecturesystem.modules.attendance.mapper.AttendanceMapper;
import com.example.lecturesystem.modules.attendance.support.AttendanceCheckInStatus;
import com.example.lecturesystem.modules.attendance.service.AttendanceService;
import com.example.lecturesystem.modules.attendance.vo.AttendanceAbnormalMonitorVO;
import com.example.lecturesystem.modules.attendance.vo.AttendanceAbnormalReasonDistributionVO;
import com.example.lecturesystem.modules.attendance.vo.AttendanceAbnormalTrendComparisonVO;
import com.example.lecturesystem.modules.attendance.vo.AttendanceAbnormalTrendPointVO;
import com.example.lecturesystem.modules.attendance.vo.AttendanceAbnormalUserRankVO;
import com.example.lecturesystem.modules.attendance.vo.AttendanceAbnormalUserBehaviorPointVO;
import com.example.lecturesystem.modules.attendance.vo.AttendanceAbnormalUserSummaryVO;
import com.example.lecturesystem.modules.attendance.vo.AttendancePageVO;
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
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final double EARTH_RADIUS_METERS = 6371000D;
    private static final String RISK_LEVEL_HIGH = "HIGH";
    private static final String RISK_LEVEL_MEDIUM = "MEDIUM";
    private static final String RISK_LEVEL_LOW = "LOW";
    private static final String TREND_RISING = "RISING";
    private static final String TREND_FALLING = "FALLING";
    private static final String TREND_STABLE = "STABLE";

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
        if (reasonKey.contains("定位") || AttendanceCheckInStatus.LOCATION_REQUIRED.equals(reasonKey)) {
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
