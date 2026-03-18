package com.example.lecturesystem.modules.statistics.service.impl;

import com.example.lecturesystem.modules.attendance.mapper.AttendanceMapper;
import com.example.lecturesystem.modules.permission.support.CurrentUserFacade;
import com.example.lecturesystem.modules.permission.support.DataScopeService;
import com.example.lecturesystem.modules.statistics.mapper.StatisticsMapper;
import com.example.lecturesystem.modules.statistics.service.StatisticsService;
import com.example.lecturesystem.modules.statistics.vo.StatisticsOverviewVO;
import com.example.lecturesystem.modules.user.entity.UserEntity;
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
    private final CurrentUserFacade currentUserFacade;
    private final DataScopeService dataScopeService;

    @Autowired
    public StatisticsServiceImpl(StatisticsMapper statisticsMapper,
                                 AttendanceMapper attendanceMapper,
                                 WeeklyWorkMapper weeklyWorkMapper,
                                 CurrentUserFacade currentUserFacade,
                                 DataScopeService dataScopeService) {
        this.statisticsMapper = statisticsMapper;
        this.attendanceMapper = attendanceMapper;
        this.weeklyWorkMapper = weeklyWorkMapper;
        this.currentUserFacade = currentUserFacade;
        this.dataScopeService = dataScopeService;
    }

    @Override
    public Object overview() {
        return legacyOverview();
    }

    @Override
    public Object overview(String weekNo, String unitName) {
        String resolvedWeekNo = normalizeWeekNo(weekNo);
        String normalizedUnitName = normalizeText(unitName);
        String treePathPrefix = currentTreePathPrefix();

        StatisticsOverviewVO overview = statisticsMapper.queryOverview(resolvedWeekNo, normalizedUnitName, treePathPrefix);
        long totalUserCount = treePathPrefix == null
                ? statisticsMapper.countAllUsers()
                : statisticsMapper.countUsersByUnitName(normalizedUnitName, treePathPrefix);

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
        return statisticsMapper.queryOrgRank(normalizeWeekNo(weekNo), normalizeText(unitName), currentTreePathPrefix());
    }

    @Override
    public Object redList(String weekNo, String unitName) {
        return statisticsMapper.queryStatusList(normalizeWeekNo(weekNo), normalizeText(unitName), "RED", currentTreePathPrefix());
    }

    @Override
    public Object yellowList(String weekNo, String unitName) {
        return statisticsMapper.queryStatusList(normalizeWeekNo(weekNo), normalizeText(unitName), "YELLOW", currentTreePathPrefix());
    }

    @Override
    public Object trend(String unitName) {
        return statisticsMapper.queryTrend(normalizeText(unitName), currentTreePathPrefix());
    }

    private Object legacyOverview() {
        String treePathPrefix = currentTreePathPrefix();
        long userCount = treePathPrefix == null ? statisticsMapper.countAllUsers() : statisticsMapper.countUsersByUnitName(null, treePathPrefix);
        LocalDate today = LocalDate.now();
        long attendanceUserCount = attendanceMapper.countDistinctUsersByRange(
                treePathPrefix,
                today,
                today,
                1
        );

        String currentWeekNo = buildCurrentWeekNo(today);
        long weeklySubmittedUserCount = weeklyWorkMapper.countDistinctSubmittedUsers(
                treePathPrefix,
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

    private String currentTreePathPrefix() {
        if (currentUserFacade != null && dataScopeService != null) {
            UserEntity currentUser = currentUserFacade.currentUserEntity();
            return dataScopeService.buildTreePathPrefix(currentUser);
        }
        return null;
    }

}
