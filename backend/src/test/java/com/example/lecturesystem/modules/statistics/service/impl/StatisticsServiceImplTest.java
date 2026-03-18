package com.example.lecturesystem.modules.statistics.service.impl;

import com.example.lecturesystem.modules.attendance.mapper.AttendanceMapper;
import com.example.lecturesystem.modules.attendance.entity.AttendanceRecordEntity;
import com.example.lecturesystem.modules.attendance.vo.AttendanceRecordListItemVO;
import com.example.lecturesystem.modules.attendance.dto.AttendanceQueryRequest;
import com.example.lecturesystem.modules.auth.security.LoginUser;
import com.example.lecturesystem.modules.permission.service.PermissionService;
import com.example.lecturesystem.modules.statistics.mapper.StatisticsMapper;
import com.example.lecturesystem.modules.statistics.vo.StatisticsOrgRankVO;
import com.example.lecturesystem.modules.statistics.vo.StatisticsOverviewVO;
import com.example.lecturesystem.modules.statistics.vo.StatisticsTrendVO;
import com.example.lecturesystem.modules.workscore.vo.WorkScoreListItemVO;
import com.example.lecturesystem.modules.weeklywork.dto.WeeklyWorkQueryRequest;
import com.example.lecturesystem.modules.weeklywork.entity.WeeklyWorkEntity;
import com.example.lecturesystem.modules.weeklywork.mapper.WeeklyWorkMapper;
import com.example.lecturesystem.modules.weeklywork.vo.WeeklyWorkListItemVO;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.WeekFields;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class StatisticsServiceImplTest {
    @After
    public void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void overviewShouldUseScopedCountsForNonSuperAdmin() {
        StubStatisticsMapper statisticsMapper = new StubStatisticsMapper(10L);
        StubAttendanceMapper attendanceMapper = new StubAttendanceMapper();
        StubWeeklyWorkMapper weeklyWorkMapper = new StubWeeklyWorkMapper();
        StubPermissionService permissionService = new StubPermissionService(false, Set.of(3L, 4L, 5L));
        StatisticsServiceImpl service = new StatisticsServiceImpl(statisticsMapper, attendanceMapper, weeklyWorkMapper, permissionService);

        mockLoginUser(3L, false);
        Map<?, ?> result = (Map<?, ?>) service.overview();

        Assert.assertEquals(3L, result.get("userCount"));
        Assert.assertEquals(2L, result.get("attendanceUserCount"));
        Assert.assertEquals(2L, result.get("weeklySubmittedUserCount"));
        Assert.assertEquals(66.67D, (Double) result.get("attendanceRate"), 0.001D);
        Assert.assertEquals(66.67D, (Double) result.get("weeklySubmitRate"), 0.001D);
    }

    @Test
    public void overviewShouldUseGlobalCountsForSuperAdmin() {
        StubStatisticsMapper statisticsMapper = new StubStatisticsMapper(8L);
        StubAttendanceMapper attendanceMapper = new StubAttendanceMapper();
        StubWeeklyWorkMapper weeklyWorkMapper = new StubWeeklyWorkMapper();
        StubPermissionService permissionService = new StubPermissionService(true, Set.of());
        StatisticsServiceImpl service = new StatisticsServiceImpl(statisticsMapper, attendanceMapper, weeklyWorkMapper, permissionService);

        mockLoginUser(1L, true);
        Map<?, ?> result = (Map<?, ?>) service.overview();

        Assert.assertEquals(8L, result.get("userCount"));
        Assert.assertEquals(4L, result.get("attendanceUserCount"));
        Assert.assertEquals(5L, result.get("weeklySubmittedUserCount"));
        Assert.assertEquals(50D, (Double) result.get("attendanceRate"), 0.001D);
        Assert.assertEquals(62.5D, (Double) result.get("weeklySubmitRate"), 0.001D);
    }

    private void mockLoginUser(Long userId, boolean superAdmin) {
        LoginUser loginUser = new LoginUser(userId, "tester", "测试用户", superAdmin);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, java.util.List.of())
        );
    }

    private static class StubPermissionService implements PermissionService {
        private final boolean superAdmin;
        private final Set<Long> scopeUserIds;

        private StubPermissionService(boolean superAdmin, Set<Long> scopeUserIds) {
            this.superAdmin = superAdmin;
            this.scopeUserIds = scopeUserIds;
        }

        @Override
        public boolean isSuperAdmin(Long userId) {
            return superAdmin;
        }

        @Override
        public boolean isUnitAdmin(Long userId) {
            return false;
        }

        @Override
        public Set<Long> queryDataScopeUserIds(Long currentUserId) {
            return scopeUserIds;
        }
    }

    private static class StubStatisticsMapper implements StatisticsMapper {
        private final long allUsers;

        private StubStatisticsMapper(long allUsers) {
            this.allUsers = allUsers;
        }

        @Override
        public long countAllUsers() {
            return allUsers;
        }

        @Override
        public long countUsersByUnitName(String unitName, Long unitId) {
            return allUsers;
        }

        @Override
        public StatisticsOverviewVO queryOverview(String weekNo, String unitName, Long unitId) {
            return null;
        }

        @Override
        public List<StatisticsOrgRankVO> queryOrgRank(String weekNo, String unitName, Long unitId) {
            return List.of();
        }

        @Override
        public List<WorkScoreListItemVO> queryStatusList(String weekNo, String unitName, String status, Long unitId) {
            return List.of();
        }

        @Override
        public List<StatisticsTrendVO> queryTrend(String unitName, Long unitId) {
            return List.of();
        }
    }

    private static class StubAttendanceMapper implements AttendanceMapper {
        @Override
        public int insert(AttendanceRecordEntity entity) {
            return 0;
        }

        @Override
        public AttendanceRecordEntity findById(Long id) {
            return null;
        }

        @Override
        public AttendanceRecordEntity findByUserIdAndDate(Long userId, LocalDate attendanceDate) {
            return null;
        }

        @Override
        public int update(AttendanceRecordEntity entity) {
            return 0;
        }

        @Override
        public int updateValidFlag(Long id, Integer validFlag) {
            return 0;
        }

        @Override
        public int deleteById(Long id) {
            return 0;
        }

        @Override
        public long countDistinctUsersByRange(Long unitId, LocalDate startDate, LocalDate endDate, Integer validFlag) {
            return unitId == null ? 4L : 2L;
        }

        @Override
        public List<AttendanceRecordListItemVO> queryList(AttendanceQueryRequest request) {
            return List.of();
        }
    }

    private static class StubWeeklyWorkMapper implements WeeklyWorkMapper {
        @Override
        public WeeklyWorkEntity findByUserIdAndWeekNo(Long userId, String weekNo) {
            return null;
        }

        @Override
        public WeeklyWorkEntity findById(Long id) {
            return null;
        }

        @Override
        public int insert(WeeklyWorkEntity entity) {
            return 0;
        }

        @Override
        public int updateDraft(WeeklyWorkEntity entity) {
            return 0;
        }

        @Override
        public int markSubmitted(Long id, LocalDateTime submitTime) {
            return 0;
        }

        @Override
        public int updateStatus(Long id, String status) {
            return 0;
        }

        @Override
        public long countDistinctSubmittedUsers(Long unitId, String weekNo, String status) {
            String currentWeekNo = buildCurrentWeekNo(LocalDate.now());
            if (!currentWeekNo.equals(weekNo)) {
                return 0L;
            }
            return unitId == null ? 5L : 2L;
        }

        @Override
        public List<WeeklyWorkListItemVO> queryList(WeeklyWorkQueryRequest request) {
            return List.of();
        }

        private String buildCurrentWeekNo(LocalDate date) {
            WeekFields weekFields = WeekFields.ISO;
            int weekBasedYear = date.get(weekFields.weekBasedYear());
            int week = date.get(weekFields.weekOfWeekBasedYear());
            return String.format("%d-W%02d", weekBasedYear, week);
        }
    }
}
