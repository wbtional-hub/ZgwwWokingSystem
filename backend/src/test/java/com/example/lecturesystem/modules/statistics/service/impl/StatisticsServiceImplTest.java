package com.example.lecturesystem.modules.statistics.service.impl;

import com.example.lecturesystem.modules.attendance.mapper.AttendanceMapper;
import com.example.lecturesystem.modules.attendance.entity.AttendanceRecordEntity;
import com.example.lecturesystem.modules.attendance.vo.AttendanceRecordListItemVO;
import com.example.lecturesystem.modules.attendance.vo.AttendanceAbnormalUserRankVO;
import com.example.lecturesystem.modules.attendance.vo.AttendanceAbnormalReasonDistributionVO;
import com.example.lecturesystem.modules.attendance.vo.AttendanceAbnormalTrendComparisonVO;
import com.example.lecturesystem.modules.attendance.vo.AttendanceAbnormalTrendPointVO;
import com.example.lecturesystem.modules.attendance.vo.AttendanceAbnormalUserBehaviorPointVO;
import com.example.lecturesystem.modules.attendance.vo.AttendanceAbnormalUserSummaryVO;
import com.example.lecturesystem.modules.attendance.vo.AttendanceStatusCountVO;
import com.example.lecturesystem.modules.attendance.dto.AttendanceQueryRequest;
import com.example.lecturesystem.modules.permission.support.CurrentUserFacade;
import com.example.lecturesystem.modules.permission.support.DataScopeContext;
import com.example.lecturesystem.modules.permission.support.DataScopeService;
import com.example.lecturesystem.modules.statistics.mapper.StatisticsMapper;
import com.example.lecturesystem.modules.statistics.vo.StatisticsOrgRankVO;
import com.example.lecturesystem.modules.statistics.vo.StatisticsOverviewVO;
import com.example.lecturesystem.modules.statistics.vo.StatisticsTrendVO;
import com.example.lecturesystem.modules.user.entity.UserEntity;
import com.example.lecturesystem.modules.workscore.vo.WorkScoreListItemVO;
import com.example.lecturesystem.modules.weeklywork.dto.WeeklyWorkQueryRequest;
import com.example.lecturesystem.modules.weeklywork.entity.WeeklyWorkEntity;
import com.example.lecturesystem.modules.weeklywork.mapper.WeeklyWorkMapper;
import com.example.lecturesystem.modules.weeklywork.vo.WeeklyWorkListItemVO;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.WeekFields;
import java.util.List;
import java.util.Map;

public class StatisticsServiceImplTest {
    @After
    public void tearDown() {
    }

    @Test
    public void overviewShouldUseScopedCountsForNonSuperAdmin() {
        StubStatisticsMapper statisticsMapper = new StubStatisticsMapper(10L);
        StubAttendanceMapper attendanceMapper = new StubAttendanceMapper();
        StubWeeklyWorkMapper weeklyWorkMapper = new StubWeeklyWorkMapper();
        StatisticsServiceImpl service = new StatisticsServiceImpl(
                statisticsMapper,
                attendanceMapper,
                weeklyWorkMapper,
                new StubCurrentUserFacade(3L, "USER", "/3/"),
                new DataScopeService()
        );
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
        StatisticsServiceImpl service = new StatisticsServiceImpl(
                statisticsMapper,
                attendanceMapper,
                weeklyWorkMapper,
                new StubCurrentUserFacade(1L, "ADMIN", "/1/"),
                new DataScopeService()
        );
        Map<?, ?> result = (Map<?, ?>) service.overview();

        Assert.assertEquals(8L, result.get("userCount"));
        Assert.assertEquals(4L, result.get("attendanceUserCount"));
        Assert.assertEquals(5L, result.get("weeklySubmittedUserCount"));
        Assert.assertEquals(50D, (Double) result.get("attendanceRate"), 0.001D);
        Assert.assertEquals(62.5D, (Double) result.get("weeklySubmitRate"), 0.001D);
    }

    private static class StubCurrentUserFacade extends CurrentUserFacade {
        private final UserEntity currentUser;

        private StubCurrentUserFacade(Long userId, String role, String treePath) {
            super(null);
            this.currentUser = new UserEntity();
            this.currentUser.setId(userId);
            this.currentUser.setRole(role);
            this.currentUser.setTreePath(treePath);
        }

        @Override
        public UserEntity currentUserEntity() {
            return currentUser;
        }

        @Override
        public DataScopeContext currentDataScope() {
            return new DataScopeContext(
                    currentUser.getId(),
                    currentUser.getUnitId(),
                    "ADMIN".equalsIgnoreCase(currentUser.getRole())
            );
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
        public long countUsersByUnitName(String unitName, String treePathPrefix) {
            return treePathPrefix == null ? allUsers : 3L;
        }

        @Override
        public StatisticsOverviewVO queryOverview(String weekNo, String unitName, String treePathPrefix) {
            return null;
        }

        @Override
        public List<StatisticsOrgRankVO> queryOrgRank(String weekNo, String unitName, String treePathPrefix) {
            return List.of();
        }

        @Override
        public List<WorkScoreListItemVO> queryStatusList(String weekNo, String unitName, String status, String treePathPrefix) {
            return List.of();
        }

        @Override
        public List<StatisticsTrendVO> queryTrend(String unitName, String treePathPrefix) {
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
        public com.example.lecturesystem.modules.unit.vo.AttendanceLocationVO findAttendanceLocationByUnitId(Long unitId) {
            return null;
        }

        @Override
        public String findUnitNameById(Long unitId) {
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
        public long countDistinctUsersByRange(String treePathPrefix, LocalDate startDate, LocalDate endDate, Integer validFlag) {
            return treePathPrefix == null ? 4L : 2L;
        }

        @Override
        public long countByQuery(AttendanceQueryRequest request) {
            return 0;
        }

        @Override
        public List<AttendanceStatusCountVO> queryStatusCounts(AttendanceQueryRequest request) {
            return List.of();
        }

        @Override
        public List<AttendanceAbnormalUserRankVO> queryAbnormalUserRanks(AttendanceQueryRequest request) {
            return List.of();
        }

        @Override
        public List<AttendanceAbnormalTrendPointVO> queryAbnormalTrend(AttendanceQueryRequest request) {
            return List.of();
        }

        @Override
        public List<AttendanceAbnormalTrendComparisonVO> queryAbnormalTrendComparisons(AttendanceQueryRequest request,
                                                                                        LocalDate recentDateFrom,
                                                                                        LocalDate recentDateTo,
                                                                                        LocalDate previousDateFrom,
                                                                                        LocalDate previousDateTo) {
            return List.of();
        }

        @Override
        public List<AttendanceAbnormalReasonDistributionVO> queryAbnormalReasonDistributions(AttendanceQueryRequest request) {
            return List.of();
        }

        @Override
        public List<AttendanceAbnormalReasonDistributionVO> queryAbnormalUserTopReasons(AttendanceQueryRequest request) {
            return List.of();
        }

        @Override
        public AttendanceAbnormalUserSummaryVO queryAbnormalUserSummary(AttendanceQueryRequest request) {
            return null;
        }

        @Override
        public List<AttendanceAbnormalUserBehaviorPointVO> queryAbnormalUserBehaviorPoints(AttendanceQueryRequest request) {
            return List.of();
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
        public long countDistinctSubmittedUsers(String treePathPrefix, String weekNo, String status) {
            String currentWeekNo = buildCurrentWeekNo(LocalDate.now());
            if (!currentWeekNo.equals(weekNo)) {
                return 0L;
            }
            return treePathPrefix == null ? 5L : 2L;
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
