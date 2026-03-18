package com.example.lecturesystem.modules.statistics.service.impl;

import com.example.lecturesystem.modules.attendance.mapper.AttendanceMapper;
import com.example.lecturesystem.modules.permission.support.CurrentUserFacade;
import com.example.lecturesystem.modules.permission.support.DataScopeContext;
import com.example.lecturesystem.modules.permission.service.PermissionService;
import com.example.lecturesystem.modules.statistics.mapper.StatisticsMapper;
import com.example.lecturesystem.modules.statistics.service.StatisticsService;
import com.example.lecturesystem.modules.statistics.vo.StatisticsOverviewVO;
import com.example.lecturesystem.modules.weeklywork.mapper.WeeklyWorkMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.HashMap;
import java.util.Map;

@Service
public class StatisticsServiceImpl implements StatisticsService {
    private static final String STATUS_SUBMITTED = "SUBMITTED";

    private final StatisticsMapper statisticsMapper;
    private final AttendanceMapper attendanceMapper;
    private final WeeklyWorkMapper weeklyWorkMapper;
    private final PermissionService permissionService;
    private final CurrentUserFacade currentUserFacade;

    @Autowired
    public StatisticsServiceImpl(StatisticsMapper statisticsMapper,
                                 AttendanceMapper attendanceMapper,
                                 WeeklyWorkMapper weeklyWorkMapper,
                                 PermissionService permissionService,
                                 CurrentUserFacade currentUserFacade) {
        this.statisticsMapper = statisticsMapper;
        this.attendanceMapper = attendanceMapper;
        this.weeklyWorkMapper = weeklyWorkMapper;
        this.permissionService = permissionService;
        this.currentUserFacade = currentUserFacade;
    }

    public StatisticsServiceImpl(StatisticsMapper statisticsMapper,
                                 AttendanceMapper attendanceMapper,
                                 WeeklyWorkMapper weeklyWorkMapper,
                                 PermissionService permissionService) {
        this(statisticsMapper, attendanceMapper, weeklyWorkMapper, permissionService, null);
    }

    @Override
    public Object overview() {
        return legacyOverview();
    }

    @Override
    public Object overview(String weekNo, String unitName) {
        DataScopeContext scope = currentScope();
        String resolvedWeekNo = normalizeWeekNo(weekNo);
        String normalizedUnitName = normalizeText(unitName);
        Long scopedUnitId = scope.isSuperAdmin() ? null : scope.getUnitId();

        StatisticsOverviewVO overview = statisticsMapper.queryOverview(resolvedWeekNo, normalizedUnitName, scopedUnitId);
        long totalUserCount = statisticsMapper.countUsersByUnitName(normalizedUnitName, scopedUnitId);

        Map<String, Object> map = new HashMap<>();
        map.put("weekNo", resolvedWeekNo);
        map.put("totalUserCount", totalUserCount);
        map.put("scoreCount", overview == null || overview.getScoreCount() == null ? 0L : overview.getScoreCount());
        map.put("averageScore", overview == null || overview.getAverageScore() == null ? BigDecimal.ZERO : overview.getAverageScore());
        map.put("redCount", overview == null || overview.getRedCount() == null ? 0L : overview.getRedCount());
        map.put("normalCount", overview == null || overview.getNormalCount() == null ? 0L : overview.getNormalCount());
        map.put("yellowCount", overview == null || overview.getYellowCount() == null ? 0L : overview.getYellowCount());
        map.put("coverageRate", calculateRate(
                overview == null || overview.getScoreCount() == null ? 0L : overview.getScoreCount(),
                totalUserCount
        ));
        return map;
    }

    @Override
    public Object orgRank(String weekNo, String unitName) {
        DataScopeContext scope = currentScope();
        return statisticsMapper.queryOrgRank(normalizeWeekNo(weekNo), normalizeText(unitName), scope.isSuperAdmin() ? null : scope.getUnitId());
    }

    @Override
    public Object redList(String weekNo, String unitName) {
        DataScopeContext scope = currentScope();
        return statisticsMapper.queryStatusList(normalizeWeekNo(weekNo), normalizeText(unitName), "RED", scope.isSuperAdmin() ? null : scope.getUnitId());
    }

    @Override
    public Object yellowList(String weekNo, String unitName) {
        DataScopeContext scope = currentScope();
        return statisticsMapper.queryStatusList(normalizeWeekNo(weekNo), normalizeText(unitName), "YELLOW", scope.isSuperAdmin() ? null : scope.getUnitId());
    }

    @Override
    public Object trend(String unitName) {
        DataScopeContext scope = currentScope();
        return statisticsMapper.queryTrend(normalizeText(unitName), scope.isSuperAdmin() ? null : scope.getUnitId());
    }

    private Object legacyOverview() {
        DataScopeContext scope = currentScope();
        Long scopedUnitId = scope.isSuperAdmin() ? null : scope.getUnitId();
        long userCount = scopedUnitId == null ? statisticsMapper.countAllUsers() : statisticsMapper.countUsersByUnitName(null, scopedUnitId);
        LocalDate today = LocalDate.now();
        long attendanceUserCount = attendanceMapper.countDistinctUsersByRange(
                scopedUnitId,
                today,
                today,
                1
        );

        String currentWeekNo = buildCurrentWeekNo(today);
        long weeklySubmittedUserCount = weeklyWorkMapper.countDistinctSubmittedUsers(
                scopedUnitId,
                currentWeekNo,
                STATUS_SUBMITTED
        );

        Map<String, Object> map = new HashMap<>();
        map.put("userCount", userCount);
        map.put("attendanceUserCount", attendanceUserCount);
        map.put("weeklySubmittedUserCount", weeklySubmittedUserCount);
        map.put("attendanceRate", calculateRate(attendanceUserCount, userCount));
        map.put("weeklySubmitRate", calculateRate(weeklySubmittedUserCount, userCount));
        map.put("currentWeekNo", currentWeekNo);
        return map;
    }

    private String normalizeWeekNo(String weekNo) {
        String normalized = normalizeText(weekNo);
        return normalized != null ? normalized : buildCurrentWeekNo(LocalDate.now());
    }

    private String normalizeText(String text) {
        if (text == null) {
            return null;
        }
        String trimmed = text.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String buildCurrentWeekNo(LocalDate date) {
        WeekFields weekFields = WeekFields.ISO;
        int weekBasedYear = date.get(weekFields.weekBasedYear());
        int week = date.get(weekFields.weekOfWeekBasedYear());
        return String.format("%d-W%02d", weekBasedYear, week);
    }

    private double calculateRate(long numerator, long denominator) {
        if (denominator <= 0) {
            return 0D;
        }
        return BigDecimal.valueOf(numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private DataScopeContext currentScope() {
        if (currentUserFacade != null) {
            return currentUserFacade.currentDataScope();
        }
        return new DataScopeContext(0L, null, true);
    }

}
