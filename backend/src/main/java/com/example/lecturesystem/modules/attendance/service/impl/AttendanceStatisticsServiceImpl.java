package com.example.lecturesystem.modules.attendance.service.impl;

import com.example.lecturesystem.modules.attendance.dto.AttendanceStatsQueryRequest;
import com.example.lecturesystem.modules.attendance.entity.AttendanceRuleEntity;
import com.example.lecturesystem.modules.attendance.mapper.AttendanceRuleMapper;
import com.example.lecturesystem.modules.attendance.mapper.AttendanceStatisticsMapper;
import com.example.lecturesystem.modules.attendance.service.AttendanceStatisticsService;
import com.example.lecturesystem.modules.attendance.vo.AttendanceScopedUserVO;
import com.example.lecturesystem.modules.attendance.vo.AttendanceStatsRecordVO;
import com.example.lecturesystem.modules.attendance.vo.AttendanceTeamMemberStatusVO;
import com.example.lecturesystem.modules.attendance.vo.AttendanceTeamStatisticsVO;
import com.example.lecturesystem.modules.attendance.vo.AttendanceWeeklyOverviewPointVO;
import com.example.lecturesystem.modules.permission.support.CurrentUserFacade;
import com.example.lecturesystem.modules.permission.support.DataScopeService;
import com.example.lecturesystem.modules.user.entity.UserEntity;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class AttendanceStatisticsServiceImpl implements AttendanceStatisticsService {
    private final AttendanceStatisticsMapper attendanceStatisticsMapper;
    private final AttendanceRuleMapper attendanceRuleMapper;
    private final CurrentUserFacade currentUserFacade;
    private final DataScopeService dataScopeService;

    public AttendanceStatisticsServiceImpl(AttendanceStatisticsMapper attendanceStatisticsMapper,
                                           AttendanceRuleMapper attendanceRuleMapper,
                                           CurrentUserFacade currentUserFacade,
                                           DataScopeService dataScopeService) {
        this.attendanceStatisticsMapper = attendanceStatisticsMapper;
        this.attendanceRuleMapper = attendanceRuleMapper;
        this.currentUserFacade = currentUserFacade;
        this.dataScopeService = dataScopeService;
    }

    @Override
    public Object queryTeamStatistics(AttendanceStatsQueryRequest request) {
        UserEntity currentUser = currentUserFacade.currentUserEntity();
        LocalDate targetDate = resolveTargetDate(request);
        List<AttendanceScopedUserVO> scopedUsers = queryScopedUsers(currentUser);
        Map<Long, AttendanceRuleEntity> ruleMap = queryRuleMap(scopedUsers);
        LocalDate weekStart = targetDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = weekStart.plusDays(6);
        Map<Long, Map<LocalDate, AttendanceStatsRecordVO>> recordMap = queryRecordMap(scopedUsers, weekStart, weekEnd);

        AttendanceTeamStatisticsVO result = new AttendanceTeamStatisticsVO();
        result.setDate(targetDate);
        result.setWorkday(isWorkday(targetDate));
        result.setScopeUserCount((long) scopedUsers.size());
        result.setScopeDescription(dataScopeService.describeScope(currentUser, scopedUsers.size()));
        result.setWeeklyOverview(buildWeeklyOverview(scopedUsers, ruleMap, recordMap, weekStart));

        List<AttendanceTeamMemberStatusVO> missingUsers = new ArrayList<>();
        List<AttendanceTeamMemberStatusVO> abnormalUsers = new ArrayList<>();
        List<AttendanceTeamMemberStatusVO> overtimeUsers = new ArrayList<>();
        List<AttendanceTeamMemberStatusVO> recentMembers = new ArrayList<>();

        int shouldAttendCount = result.getWorkday() ? scopedUsers.size() : 0;
        int checkedInCount = 0;
        int lateCount = 0;
        int earlyLeaveCount = 0;
        int overtimeCount = 0;

        for (AttendanceScopedUserVO user : scopedUsers) {
            AttendanceStatsRecordVO todayRecord = recordMap.getOrDefault(user.getUserId(), Map.of()).get(targetDate);
            AttendanceStatsRecordVO latestRecord = findLatestRecord(recordMap.get(user.getUserId()));
            AttendanceRuleEntity rule = ruleMap.get(user.getUnitId());
            boolean hasTodayRecord = todayRecord != null && (todayRecord.getCheckInTime() != null || todayRecord.getCheckOutTime() != null);
            boolean late = result.getWorkday() && isLate(todayRecord, rule);
            boolean earlyLeave = result.getWorkday() && isEarlyLeave(todayRecord, rule);

            if (result.getWorkday()) {
                if (hasTodayRecord) {
                    checkedInCount += 1;
                } else {
                    missingUsers.add(buildMemberStatus(user, null, false, false, "今日未打卡", "工作日应到未打卡"));
                }
                if (late) {
                    lateCount += 1;
                }
                if (earlyLeave) {
                    earlyLeaveCount += 1;
                }
                if (late || earlyLeave) {
                    abnormalUsers.add(buildMemberStatus(user, todayRecord, late, earlyLeave, resolveAbnormalLabel(late, earlyLeave), resolveAbnormalReason(late, earlyLeave)));
                }
            } else if (hasTodayRecord) {
                overtimeCount += 1;
                overtimeUsers.add(buildMemberStatus(user, todayRecord, false, false, "非工作日已打卡", "按加班/值班记录展示"));
            }

            recentMembers.add(buildRecentMemberStatus(user, targetDate, result.getWorkday(), todayRecord, latestRecord, late, earlyLeave));
        }

        sortMembers(missingUsers);
        sortMembers(abnormalUsers);
        sortMembers(overtimeUsers);
        sortMembers(recentMembers);

        result.setShouldAttendCount(shouldAttendCount);
        result.setCheckedInCount(result.getWorkday() ? checkedInCount : 0);
        result.setMissingCount(result.getWorkday() ? Math.max(shouldAttendCount - checkedInCount, 0) : 0);
        result.setLateCount(result.getWorkday() ? lateCount : 0);
        result.setEarlyLeaveCount(result.getWorkday() ? earlyLeaveCount : 0);
        result.setOvertimeCount(result.getWorkday() ? 0 : overtimeCount);
        result.setMissingUsers(missingUsers);
        result.setAbnormalUsers(abnormalUsers);
        result.setOvertimeUsers(overtimeUsers);
        result.setRecentMembers(recentMembers);
        if (!result.getWorkday()) {
            result.setNonWorkdayNotice("今日为非工作日，未打卡不会计入异常，已打卡人员按加班/值班记录展示。");
        }
        return result;
    }

    private List<AttendanceScopedUserVO> queryScopedUsers(UserEntity currentUser) {
        String treePathPrefix = dataScopeService.buildTreePathPrefix(currentUser);
        List<AttendanceScopedUserVO> users = treePathPrefix == null
                ? attendanceStatisticsMapper.queryAllScopedUsers()
                : attendanceStatisticsMapper.queryScopedUsers(treePathPrefix);
        users.sort(Comparator.comparing(AttendanceScopedUserVO::getRealName, Comparator.nullsLast(String::compareTo))
                .thenComparing(AttendanceScopedUserVO::getUsername, Comparator.nullsLast(String::compareTo))
                .thenComparing(AttendanceScopedUserVO::getUserId));
        return users;
    }

    private Map<Long, AttendanceRuleEntity> queryRuleMap(List<AttendanceScopedUserVO> scopedUsers) {
        Set<Long> unitIds = new LinkedHashSet<>();
        for (AttendanceScopedUserVO user : scopedUsers) {
            if (user.getUnitId() != null) {
                unitIds.add(user.getUnitId());
            }
        }
        Map<Long, AttendanceRuleEntity> ruleMap = new LinkedHashMap<>();
        if (unitIds.isEmpty()) {
            return ruleMap;
        }
        for (AttendanceRuleEntity item : attendanceRuleMapper.queryByUnitIds(new ArrayList<>(unitIds))) {
            ruleMap.put(item.getUnitId(), item);
        }
        return ruleMap;
    }

    private Map<Long, Map<LocalDate, AttendanceStatsRecordVO>> queryRecordMap(List<AttendanceScopedUserVO> scopedUsers,
                                                                               LocalDate startDate,
                                                                               LocalDate endDate) {
        Map<Long, Map<LocalDate, AttendanceStatsRecordVO>> result = new LinkedHashMap<>();
        if (scopedUsers.isEmpty()) {
            return result;
        }
        List<Long> userIds = new ArrayList<>(scopedUsers.size());
        for (AttendanceScopedUserVO user : scopedUsers) {
            userIds.add(user.getUserId());
        }
        for (AttendanceStatsRecordVO record : attendanceStatisticsMapper.queryRecordsByUserIdsAndDateRange(userIds, startDate, endDate)) {
            result.computeIfAbsent(record.getUserId(), key -> new LinkedHashMap<>()).put(record.getAttendanceDate(), record);
        }
        return result;
    }

    private List<AttendanceWeeklyOverviewPointVO> buildWeeklyOverview(List<AttendanceScopedUserVO> scopedUsers,
                                                                      Map<Long, AttendanceRuleEntity> ruleMap,
                                                                      Map<Long, Map<LocalDate, AttendanceStatsRecordVO>> recordMap,
                                                                      LocalDate weekStart) {
        List<AttendanceWeeklyOverviewPointVO> result = new ArrayList<>();
        for (int index = 0; index < 7; index++) {
            LocalDate date = weekStart.plusDays(index);
            boolean workday = isWorkday(date);
            int checkedInCount = 0;
            int lateCount = 0;
            int earlyLeaveCount = 0;
            int overtimeCount = 0;
            for (AttendanceScopedUserVO user : scopedUsers) {
                AttendanceStatsRecordVO record = recordMap.getOrDefault(user.getUserId(), Map.of()).get(date);
                if (record == null || (record.getCheckInTime() == null && record.getCheckOutTime() == null)) {
                    continue;
                }
                if (workday) {
                    checkedInCount += 1;
                    AttendanceRuleEntity rule = ruleMap.get(user.getUnitId());
                    if (isLate(record, rule)) {
                        lateCount += 1;
                    }
                    if (isEarlyLeave(record, rule)) {
                        earlyLeaveCount += 1;
                    }
                } else {
                    overtimeCount += 1;
                }
            }
            AttendanceWeeklyOverviewPointVO point = new AttendanceWeeklyOverviewPointVO();
            point.setDate(date);
            point.setWorkday(workday);
            point.setShouldAttendCount(workday ? scopedUsers.size() : 0);
            point.setCheckedInCount(workday ? checkedInCount : 0);
            point.setMissingCount(workday ? Math.max(scopedUsers.size() - checkedInCount, 0) : 0);
            point.setLateCount(workday ? lateCount : 0);
            point.setEarlyLeaveCount(workday ? earlyLeaveCount : 0);
            point.setOvertimeCount(workday ? 0 : overtimeCount);
            result.add(point);
        }
        return result;
    }

    private AttendanceStatsRecordVO findLatestRecord(Map<LocalDate, AttendanceStatsRecordVO> recordByDate) {
        if (recordByDate == null || recordByDate.isEmpty()) {
            return null;
        }
        AttendanceStatsRecordVO latest = null;
        for (AttendanceStatsRecordVO item : recordByDate.values()) {
            if (latest == null || item.getAttendanceDate().isAfter(latest.getAttendanceDate())) {
                latest = item;
            }
        }
        return latest;
    }

    private AttendanceTeamMemberStatusVO buildRecentMemberStatus(AttendanceScopedUserVO user,
                                                                 LocalDate targetDate,
                                                                 boolean workday,
                                                                 AttendanceStatsRecordVO todayRecord,
                                                                 AttendanceStatsRecordVO latestRecord,
                                                                 boolean late,
                                                                 boolean earlyLeave) {
        if (!workday && todayRecord != null) {
            return buildMemberStatus(user, todayRecord, false, false, "非工作日已打卡", "按加班/值班记录展示");
        }
        if (todayRecord != null) {
            return buildMemberStatus(user, todayRecord, late, earlyLeave, resolveAbnormalLabel(late, earlyLeave, "今日已打卡"), resolveAbnormalReason(late, earlyLeave));
        }
        if (latestRecord != null) {
            return buildMemberStatus(user, latestRecord, false, false, "最近记录 " + latestRecord.getAttendanceDate(), "当前日期暂无打卡");
        }
        return buildMemberStatus(user, null, false, false, workday ? "今日未打卡" : "暂无打卡记录", workday ? "工作日应到未打卡" : "暂无加班/值班记录");
    }

    private AttendanceTeamMemberStatusVO buildMemberStatus(AttendanceScopedUserVO user,
                                                           AttendanceStatsRecordVO record,
                                                           boolean late,
                                                           boolean earlyLeave,
                                                           String statusLabel,
                                                           String abnormalReason) {
        AttendanceTeamMemberStatusVO item = new AttendanceTeamMemberStatusVO();
        item.setUserId(user.getUserId());
        item.setUnitId(user.getUnitId());
        item.setUsername(user.getUsername());
        item.setRealName(user.getRealName());
        item.setUnitName(user.getUnitName());
        item.setHasRecord(record != null);
        item.setLate(late);
        item.setEarlyLeave(earlyLeave);
        item.setStatusLabel(statusLabel);
        item.setAbnormalReason(abnormalReason);
        if (record != null) {
            item.setAttendanceDate(record.getAttendanceDate());
            item.setCheckInTime(record.getCheckInTime());
            item.setCheckOutTime(record.getCheckOutTime());
        }
        return item;
    }

    private boolean isLate(AttendanceStatsRecordVO record, AttendanceRuleEntity rule) {
        if (record == null || record.getCheckInTime() == null || rule == null || rule.getWorkStartTime() == null) {
            return false;
        }
        LocalTime threshold = rule.getWorkStartTime().plusMinutes(rule.getLateGraceMinutes() == null ? 0 : rule.getLateGraceMinutes());
        return record.getCheckInTime().toLocalTime().isAfter(threshold);
    }

    private boolean isEarlyLeave(AttendanceStatsRecordVO record, AttendanceRuleEntity rule) {
        if (record == null || record.getCheckOutTime() == null || rule == null || rule.getWorkEndTime() == null) {
            return false;
        }
        LocalTime threshold = rule.getWorkEndTime().minusMinutes(rule.getEarlyLeaveGraceMinutes() == null ? 0 : rule.getEarlyLeaveGraceMinutes());
        return record.getCheckOutTime().toLocalTime().isBefore(threshold);
    }

    private String resolveAbnormalLabel(boolean late, boolean earlyLeave) {
        return resolveAbnormalLabel(late, earlyLeave, "今日已打卡");
    }

    private String resolveAbnormalLabel(boolean late, boolean earlyLeave, String fallback) {
        if (late && earlyLeave) {
            return "迟到且早退";
        }
        if (late) {
            return "迟到";
        }
        if (earlyLeave) {
            return "早退";
        }
        return fallback;
    }

    private String resolveAbnormalReason(boolean late, boolean earlyLeave) {
        if (late && earlyLeave) {
            return "当天存在迟到和早退";
        }
        if (late) {
            return "上班打卡晚于规则时间";
        }
        if (earlyLeave) {
            return "下班打卡早于规则时间";
        }
        return "";
    }

    private void sortMembers(List<AttendanceTeamMemberStatusVO> items) {
        items.sort(Comparator.comparing(AttendanceTeamMemberStatusVO::getUnitName, Comparator.nullsLast(String::compareTo))
                .thenComparing(AttendanceTeamMemberStatusVO::getRealName, Comparator.nullsLast(String::compareTo))
                .thenComparing(AttendanceTeamMemberStatusVO::getUsername, Comparator.nullsLast(String::compareTo))
                .thenComparing(AttendanceTeamMemberStatusVO::getUserId));
    }

    private boolean isWorkday(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY;
    }

    private LocalDate resolveTargetDate(AttendanceStatsQueryRequest request) {
        if (request == null || request.getDate() == null || request.getDate().isBlank()) {
            return LocalDate.now();
        }
        return LocalDate.parse(request.getDate().trim());
    }
}
