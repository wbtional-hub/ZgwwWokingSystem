package com.example.lecturesystem.modules.attendance.service.impl;

import com.example.lecturesystem.modules.attendance.dto.AttendanceQueryRequest;
import com.example.lecturesystem.modules.attendance.dto.CheckInRequest;
import com.example.lecturesystem.modules.attendance.dto.SaveAttendanceRequest;
import com.example.lecturesystem.modules.attendance.vo.AttendanceAbnormalMonitorVO;
import com.example.lecturesystem.modules.attendance.vo.AttendanceAbnormalTrendPointVO;
import com.example.lecturesystem.modules.attendance.vo.AttendanceAbnormalUserRankVO;
import com.example.lecturesystem.modules.attendance.vo.AttendanceAbnormalUserSummaryVO;
import com.example.lecturesystem.modules.attendance.entity.AttendanceRecordEntity;
import com.example.lecturesystem.modules.attendance.mapper.AttendanceMapper;
import com.example.lecturesystem.modules.attendance.support.AttendanceCheckInStatus;
import com.example.lecturesystem.modules.attendance.vo.AttendanceRecordListItemVO;
import com.example.lecturesystem.modules.attendance.vo.AttendanceStatusCountVO;
import com.example.lecturesystem.modules.attendance.vo.AttendanceSummaryVO;
import com.example.lecturesystem.modules.auth.security.LoginUser;
import com.example.lecturesystem.modules.permission.service.PermissionService;
import com.example.lecturesystem.modules.unit.vo.AttendanceLocationVO;
import com.example.lecturesystem.modules.user.entity.UserEntity;
import com.example.lecturesystem.modules.user.mapper.UserMapper;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AttendanceServiceImplTest {
    @After
    public void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void checkInShouldCreateTodayRecord() {
        InMemoryAttendanceMapper attendanceMapper = new InMemoryAttendanceMapper();
        StubPermissionService permissionService = new StubPermissionService(false, Set.of(3L));
        StubUserMapper userMapper = new StubUserMapper();
        userMapper.users.put(3L, user(3L, 2L, "/3/"));
        attendanceMapper.unitNames.put(2L, "平台单位");
        attendanceMapper.attendanceLocations.put(2L, attendanceLocation(2L, "平台主打卡点", "116.397428", "39.909230", 300));
        AttendanceServiceImpl service = new AttendanceServiceImpl(attendanceMapper, permissionService, userMapper);

        mockLoginUser(3L, false);
        CheckInRequest request = new CheckInRequest();
        request.setAddress("办公楼");
        request.setLongitude(new BigDecimal("116.397428"));
        request.setLatitude(new BigDecimal("39.909230"));

        Map<?, ?> result = (Map<?, ?>) service.checkIn(request);

        Assert.assertEquals(Boolean.TRUE, result.get("success"));
        Assert.assertEquals("CHECK_IN", result.get("action"));
        Assert.assertEquals(AttendanceCheckInStatus.CHECK_IN_SUCCESS, result.get("status"));
        Assert.assertEquals(1, attendanceMapper.data.size());
        Assert.assertEquals(LocalDate.now(), attendanceMapper.data.get(1L).getAttendanceDate());
        Assert.assertNotNull(attendanceMapper.data.get(1L).getCheckInTime());
        Assert.assertEquals(new BigDecimal("39.909230"), attendanceMapper.data.get(1L).getCheckInLatitude());
        Assert.assertEquals(new BigDecimal("116.397428"), attendanceMapper.data.get(1L).getCheckInLongitude());
        Assert.assertEquals(Integer.valueOf(1), attendanceMapper.data.get(1L).getValidFlag());
    }

    @Test
    public void secondCheckInShouldFillCheckOutTime() {
        InMemoryAttendanceMapper attendanceMapper = new InMemoryAttendanceMapper();
        StubPermissionService permissionService = new StubPermissionService(false, Set.of(3L));
        StubUserMapper userMapper = new StubUserMapper();
        userMapper.users.put(3L, user(3L, 2L, "/3/"));
        attendanceMapper.unitNames.put(2L, "平台单位");
        attendanceMapper.attendanceLocations.put(2L, attendanceLocation(2L, "平台主打卡点", "116.397428", "39.909230", 300));
        AttendanceServiceImpl service = new AttendanceServiceImpl(attendanceMapper, permissionService, userMapper);

        AttendanceRecordEntity existed = new AttendanceRecordEntity();
        existed.setUnitId(2L);
        existed.setUserId(3L);
        existed.setAttendanceDate(LocalDate.now());
        existed.setCheckInTime(LocalDateTime.now().minusHours(8));
        existed.setCheckInAddress("办公楼");
        existed.setValidFlag(1);
        attendanceMapper.insert(existed);

        mockLoginUser(3L, false);
        CheckInRequest request = new CheckInRequest();
        request.setAddress("园区大门");
        request.setLongitude(new BigDecimal("116.397428"));
        request.setLatitude(new BigDecimal("39.909230"));

        Map<?, ?> result = (Map<?, ?>) service.checkIn(request);

        Assert.assertEquals(Boolean.TRUE, result.get("success"));
        Assert.assertEquals("CHECK_OUT", result.get("action"));
        Assert.assertEquals(AttendanceCheckInStatus.CHECK_OUT_SUCCESS, result.get("status"));
        Assert.assertNotNull(attendanceMapper.data.get(existed.getId()).getCheckOutTime());
        Assert.assertEquals("园区大门", attendanceMapper.data.get(existed.getId()).getCheckOutAddress());
    }

    @Test
    public void queryCurrentAttendanceLocationShouldReturnConfiguredLocation() {
        InMemoryAttendanceMapper attendanceMapper = new InMemoryAttendanceMapper();
        StubPermissionService permissionService = new StubPermissionService(false, Set.of(3L));
        StubUserMapper userMapper = new StubUserMapper();
        userMapper.users.put(3L, user(3L, 2L, "/3/"));
        attendanceMapper.unitNames.put(2L, "平台单位");
        attendanceMapper.attendanceLocations.put(2L, attendanceLocation(2L, "平台主打卡点", "116.397428", "39.909230", 300));
        AttendanceServiceImpl service = new AttendanceServiceImpl(attendanceMapper, permissionService, userMapper);

        mockLoginUser(3L, false);

        Map<?, ?> result = (Map<?, ?>) service.queryCurrentAttendanceLocation();

        Assert.assertEquals(Boolean.TRUE, result.get("configured"));
        Assert.assertEquals(Boolean.TRUE, result.get("allowCheckIn"));
        Assert.assertEquals(AttendanceCheckInStatus.LOCATION_READY, result.get("status"));
        Assert.assertEquals("平台单位", result.get("unitName"));
        Assert.assertEquals("平台主打卡点", result.get("locationName"));
    }

    @Test
    public void checkInShouldReturnFailureWhenOutsideRadius() {
        InMemoryAttendanceMapper attendanceMapper = new InMemoryAttendanceMapper();
        StubPermissionService permissionService = new StubPermissionService(false, Set.of(3L));
        StubUserMapper userMapper = new StubUserMapper();
        userMapper.users.put(3L, user(3L, 2L, "/3/"));
        attendanceMapper.unitNames.put(2L, "平台单位");
        attendanceMapper.attendanceLocations.put(2L, attendanceLocation(2L, "平台主打卡点", "116.397428", "39.909230", 200));
        AttendanceServiceImpl service = new AttendanceServiceImpl(attendanceMapper, permissionService, userMapper);

        mockLoginUser(3L, false);
        CheckInRequest request = new CheckInRequest();
        request.setAddress("远处位置");
        request.setLongitude(new BigDecimal("116.407428"));
        request.setLatitude(new BigDecimal("39.919230"));

        Map<?, ?> result = (Map<?, ?>) service.checkIn(request);

        Assert.assertEquals(Boolean.FALSE, result.get("success"));
        Assert.assertEquals(Boolean.FALSE, result.get("allowCheckIn"));
        Assert.assertEquals(AttendanceCheckInStatus.OUT_OF_RANGE, result.get("status"));
        Assert.assertEquals("超出单位打卡范围", result.get("reason"));
        Assert.assertEquals("超出单位打卡范围", result.get("failReason"));
        Assert.assertEquals(0, attendanceMapper.data.size());
    }

    @Test
    public void queryShouldUseDataScopeForNonSuperAdmin() {
        InMemoryAttendanceMapper attendanceMapper = new InMemoryAttendanceMapper();
        StubPermissionService permissionService = new StubPermissionService(false, Set.of(3L, 4L));
        StubUserMapper userMapper = new StubUserMapper();
        userMapper.users.put(3L, user(3L, 2L, "/3/"));
        userMapper.users.put(4L, user(4L, 2L, "/3/4/"));
        userMapper.users.put(5L, user(5L, 2L, "/5/"));
        attendanceMapper.userTreePaths.put(3L, "/3/");
        attendanceMapper.userTreePaths.put(4L, "/3/4/");
        attendanceMapper.userTreePaths.put(5L, "/5/");
        AttendanceServiceImpl service = new AttendanceServiceImpl(attendanceMapper, permissionService, userMapper);

        attendanceMapper.insert(seedAttendance(2L, 3L, LocalDate.now().minusDays(1)));
        attendanceMapper.insert(seedAttendance(2L, 4L, LocalDate.now()));
        attendanceMapper.insert(seedAttendance(2L, 5L, LocalDate.now()));

        mockLoginUser(3L, false);
        AttendanceQueryRequest request = new AttendanceQueryRequest();
        List<?> result = (List<?>) service.query(request);

        Assert.assertEquals(2, result.size());
    }

    @Test
    public void queryShouldSupportCheckInStatusFilter() {
        InMemoryAttendanceMapper attendanceMapper = new InMemoryAttendanceMapper();
        StubPermissionService permissionService = new StubPermissionService(true, Set.of());
        StubUserMapper userMapper = new StubUserMapper();
        AttendanceServiceImpl service = new AttendanceServiceImpl(attendanceMapper, permissionService, userMapper);

        AttendanceRecordEntity checkIn = seedAttendance(2L, 3L, LocalDate.now().minusDays(1));
        checkIn.setCheckInResult(AttendanceCheckInStatus.CHECK_IN_SUCCESS);
        attendanceMapper.insert(checkIn);
        AttendanceRecordEntity checkOut = seedAttendance(2L, 4L, LocalDate.now());
        checkOut.setCheckInResult(AttendanceCheckInStatus.CHECK_OUT_SUCCESS);
        attendanceMapper.insert(checkOut);

        mockLoginUser(1L, true);
        AttendanceQueryRequest request = new AttendanceQueryRequest();
        request.setCheckInStatus(AttendanceCheckInStatus.CHECK_OUT_SUCCESS);

        List<?> result = (List<?>) service.query(request);

        Assert.assertEquals(1, result.size());
        AttendanceRecordListItemVO item = (AttendanceRecordListItemVO) result.get(0);
        Assert.assertEquals(AttendanceCheckInStatus.CHECK_OUT_SUCCESS, item.getCheckInResult());
    }

    @Test
    public void saveAttendanceShouldRejectUserOutsideTreePathScope() {
        InMemoryAttendanceMapper attendanceMapper = new InMemoryAttendanceMapper();
        StubPermissionService permissionService = new StubPermissionService(false, Set.of(3L, 4L, 9L));
        StubUserMapper userMapper = new StubUserMapper();
        userMapper.users.put(3L, user(3L, 2L, "/3/"));
        userMapper.users.put(4L, user(4L, 2L, "/3/4/"));
        userMapper.users.put(9L, user(9L, 2L, "/9/"));
        AttendanceServiceImpl service = new AttendanceServiceImpl(attendanceMapper, permissionService, userMapper);

        mockLoginUser(3L, false);
        SaveAttendanceRequest request = new SaveAttendanceRequest();
        request.setUserId(9L);
        request.setAttendanceDate(LocalDate.now().toString());
        request.setValidFlag(1);

        IllegalArgumentException error = Assert.assertThrows(IllegalArgumentException.class, () -> service.saveAttendance(request));

        Assert.assertEquals("无权操作该用户考勤", error.getMessage());
    }

    @Test
    public void querySummaryShouldReturnOverviewAndStatusCounts() {
        InMemoryAttendanceMapper attendanceMapper = new InMemoryAttendanceMapper();
        StubPermissionService permissionService = new StubPermissionService(true, Set.of());
        StubUserMapper userMapper = new StubUserMapper();
        AttendanceServiceImpl service = new AttendanceServiceImpl(attendanceMapper, permissionService, userMapper);

        AttendanceRecordEntity success = seedAttendance(2L, 3L, LocalDate.now().minusDays(1));
        success.setCheckInResult(AttendanceCheckInStatus.CHECK_IN_SUCCESS);
        attendanceMapper.insert(success);
        AttendanceRecordEntity failed = seedAttendance(2L, 4L, LocalDate.now());
        failed.setCheckInResult(AttendanceCheckInStatus.OUT_OF_RANGE);
        attendanceMapper.insert(failed);

        mockLoginUser(1L, true);

        AttendanceSummaryVO result = (AttendanceSummaryVO) service.querySummary(new AttendanceQueryRequest());

        Assert.assertEquals(Long.valueOf(2L), result.getTotalCount());
        Assert.assertEquals(Long.valueOf(1L), result.getSuccessCount());
        Assert.assertEquals(Long.valueOf(1L), result.getAbnormalCount());
        Assert.assertEquals(2, result.getStatusCounts().size());
    }

    @Test
    public void querySummaryShouldUseDateAndTreePathScope() {
        InMemoryAttendanceMapper attendanceMapper = new InMemoryAttendanceMapper();
        StubPermissionService permissionService = new StubPermissionService(false, Set.of(3L, 4L));
        StubUserMapper userMapper = new StubUserMapper();
        userMapper.users.put(3L, user(3L, 2L, "/3/"));
        userMapper.users.put(4L, user(4L, 2L, "/3/4/"));
        userMapper.users.put(5L, user(5L, 2L, "/5/"));
        attendanceMapper.userTreePaths.put(3L, "/3/");
        attendanceMapper.userTreePaths.put(4L, "/3/4/");
        attendanceMapper.userTreePaths.put(5L, "/5/");
        AttendanceServiceImpl service = new AttendanceServiceImpl(attendanceMapper, permissionService, userMapper);

        AttendanceRecordEntity scoped = seedAttendance(2L, 4L, LocalDate.now());
        scoped.setCheckInResult(AttendanceCheckInStatus.OUT_OF_RANGE);
        attendanceMapper.insert(scoped);
        AttendanceRecordEntity outOfDate = seedAttendance(2L, 3L, LocalDate.now().minusDays(2));
        outOfDate.setCheckInResult(AttendanceCheckInStatus.CHECK_IN_SUCCESS);
        attendanceMapper.insert(outOfDate);
        AttendanceRecordEntity outOfScope = seedAttendance(2L, 5L, LocalDate.now());
        outOfScope.setCheckInResult(AttendanceCheckInStatus.LOCATION_NOT_CONFIGURED);
        attendanceMapper.insert(outOfScope);

        mockLoginUser(3L, false);
        AttendanceQueryRequest request = new AttendanceQueryRequest();
        request.setDateFrom(LocalDate.now().toString());
        request.setDateTo(LocalDate.now().toString());

        AttendanceSummaryVO result = (AttendanceSummaryVO) service.querySummary(request);

        Assert.assertEquals(Long.valueOf(1L), result.getTotalCount());
        Assert.assertEquals(Long.valueOf(0L), result.getSuccessCount());
        Assert.assertEquals(Long.valueOf(1L), result.getAbnormalCount());
        Assert.assertEquals(AttendanceCheckInStatus.OUT_OF_RANGE, result.getStatusCounts().get(0).getCheckInStatus());
    }

    @Test
    public void queryAbnormalMonitorShouldReturnTopUsersAndAbnormalStatusCounts() {
        InMemoryAttendanceMapper attendanceMapper = new InMemoryAttendanceMapper();
        StubPermissionService permissionService = new StubPermissionService(true, Set.of());
        StubUserMapper userMapper = new StubUserMapper();
        AttendanceServiceImpl service = new AttendanceServiceImpl(attendanceMapper, permissionService, userMapper);

        AttendanceRecordEntity userThreeFail1 = seedAttendance(2L, 3L, LocalDate.now().minusDays(1));
        userThreeFail1.setCheckInResult(AttendanceCheckInStatus.OUT_OF_RANGE);
        attendanceMapper.insert(userThreeFail1);
        AttendanceRecordEntity userThreeFail2 = seedAttendance(2L, 3L, LocalDate.now());
        userThreeFail2.setCheckInResult(AttendanceCheckInStatus.LOCATION_NOT_CONFIGURED);
        attendanceMapper.insert(userThreeFail2);
        AttendanceRecordEntity userThreeSuccess = seedAttendance(2L, 3L, LocalDate.now().minusDays(2));
        userThreeSuccess.setCheckInResult(AttendanceCheckInStatus.CHECK_IN_SUCCESS);
        attendanceMapper.insert(userThreeSuccess);

        AttendanceRecordEntity userFourFail = seedAttendance(2L, 4L, LocalDate.now());
        userFourFail.setCheckInResult(AttendanceCheckInStatus.OUT_OF_RANGE);
        attendanceMapper.insert(userFourFail);

        mockLoginUser(1L, true);

        AttendanceAbnormalMonitorVO result = (AttendanceAbnormalMonitorVO) service.queryAbnormalMonitor(new AttendanceQueryRequest());

        Assert.assertEquals(2, result.getTopUsers().size());
        AttendanceAbnormalUserRankVO topUser = result.getTopUsers().get(0);
        Assert.assertEquals(Long.valueOf(3L), topUser.getUserId());
        Assert.assertEquals(Long.valueOf(2L), topUser.getAbnormalCount());
        Assert.assertEquals(Long.valueOf(3L), topUser.getTotalCount());
        Assert.assertEquals(new BigDecimal("66.7"), topUser.getAbnormalRate());
        Assert.assertEquals(2, result.getStatusCounts().size());
    }

    @Test
    public void queryAbnormalTrendShouldReturnDailyTrendForSelectedUser() {
        InMemoryAttendanceMapper attendanceMapper = new InMemoryAttendanceMapper();
        StubPermissionService permissionService = new StubPermissionService(true, Set.of());
        StubUserMapper userMapper = new StubUserMapper();
        AttendanceServiceImpl service = new AttendanceServiceImpl(attendanceMapper, permissionService, userMapper);

        AttendanceRecordEntity dayOneFail = seedAttendance(2L, 3L, LocalDate.now().minusDays(1));
        dayOneFail.setCheckInResult(AttendanceCheckInStatus.OUT_OF_RANGE);
        attendanceMapper.insert(dayOneFail);
        AttendanceRecordEntity dayOneSuccess = seedAttendance(2L, 3L, LocalDate.now().minusDays(1));
        dayOneSuccess.setCheckInResult(AttendanceCheckInStatus.CHECK_IN_SUCCESS);
        attendanceMapper.insert(dayOneSuccess);
        AttendanceRecordEntity dayTwoSuccess = seedAttendance(2L, 3L, LocalDate.now());
        dayTwoSuccess.setCheckInResult(AttendanceCheckInStatus.CHECK_OUT_SUCCESS);
        attendanceMapper.insert(dayTwoSuccess);
        AttendanceRecordEntity otherUserFail = seedAttendance(2L, 4L, LocalDate.now());
        otherUserFail.setCheckInResult(AttendanceCheckInStatus.LOCATION_NOT_CONFIGURED);
        attendanceMapper.insert(otherUserFail);

        mockLoginUser(1L, true);
        AttendanceQueryRequest request = new AttendanceQueryRequest();
        request.setUserId(3L);

        List<?> result = (List<?>) service.queryAbnormalTrend(request);

        Assert.assertEquals(2, result.size());
        AttendanceAbnormalTrendPointVO first = (AttendanceAbnormalTrendPointVO) result.get(0);
        Assert.assertEquals(LocalDate.now().minusDays(1), first.getAttendanceDate());
        Assert.assertEquals(Long.valueOf(2L), first.getTotalCount());
        Assert.assertEquals(Long.valueOf(1L), first.getAbnormalCount());
        Assert.assertEquals(new BigDecimal("50.0"), first.getAbnormalRate());
    }

    @Test
    public void queryAbnormalUserSummaryShouldReturnMinimalSummary() {
        InMemoryAttendanceMapper attendanceMapper = new InMemoryAttendanceMapper();
        StubPermissionService permissionService = new StubPermissionService(true, Set.of());
        StubUserMapper userMapper = new StubUserMapper();
        AttendanceServiceImpl service = new AttendanceServiceImpl(attendanceMapper, permissionService, userMapper);

        AttendanceRecordEntity olderFail = seedAttendance(2L, 3L, LocalDate.now().minusDays(2));
        olderFail.setCheckInResult(AttendanceCheckInStatus.OUT_OF_RANGE);
        attendanceMapper.insert(olderFail);
        AttendanceRecordEntity latestFail = seedAttendance(2L, 3L, LocalDate.now());
        latestFail.setCheckInResult(AttendanceCheckInStatus.LOCATION_NOT_CONFIGURED);
        attendanceMapper.insert(latestFail);
        AttendanceRecordEntity success = seedAttendance(2L, 3L, LocalDate.now().minusDays(1));
        success.setCheckInResult(AttendanceCheckInStatus.CHECK_IN_SUCCESS);
        attendanceMapper.insert(success);

        mockLoginUser(1L, true);
        AttendanceQueryRequest request = new AttendanceQueryRequest();
        request.setUserId(3L);

        AttendanceAbnormalUserSummaryVO result = (AttendanceAbnormalUserSummaryVO) service.queryAbnormalUserSummary(request);

        Assert.assertEquals(Long.valueOf(3L), result.getUserId());
        Assert.assertEquals(Long.valueOf(2L), result.getAbnormalCount());
        Assert.assertEquals(LocalDate.now(), result.getRecentAbnormalDate());
        Assert.assertEquals(AttendanceCheckInStatus.LOCATION_NOT_CONFIGURED, result.getRecentAbnormalType());
    }

    private void mockLoginUser(Long userId, boolean superAdmin) {
        LoginUser loginUser = new LoginUser(userId, "tester", "测试用户", superAdmin);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, java.util.List.of())
        );
    }

    private UserEntity user(Long id, Long unitId, String treePath) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setUnitId(unitId);
        user.setTreePath(treePath);
        return user;
    }

    private AttendanceRecordEntity seedAttendance(Long unitId, Long userId, LocalDate attendanceDate) {
        AttendanceRecordEntity entity = new AttendanceRecordEntity();
        entity.setUnitId(unitId);
        entity.setUserId(userId);
        entity.setAttendanceDate(attendanceDate);
        entity.setCheckInTime(attendanceDate.atTime(9, 0));
        entity.setCheckOutTime(attendanceDate.atTime(18, 0));
        entity.setCheckInAddress("办公楼");
        entity.setCheckOutAddress("办公楼");
        entity.setValidFlag(1);
        return entity;
    }

    private AttendanceLocationVO attendanceLocation(Long unitId, String name, String longitude, String latitude, Integer radiusMeters) {
        AttendanceLocationVO location = new AttendanceLocationVO();
        location.setId(unitId + 100);
        location.setUnitId(unitId);
        location.setLocationName(name);
        location.setLongitude(new BigDecimal(longitude));
        location.setLatitude(new BigDecimal(latitude));
        location.setRadiusMeters(radiusMeters);
        location.setAddress("单位办公区");
        location.setStatus(1);
        return location;
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
            return superAdmin ? Set.of() : scopeUserIds;
        }
    }

    private static class StubUserMapper implements UserMapper {
        private final Map<Long, UserEntity> users = new LinkedHashMap<>();

        @Override
        public UserEntity findById(Long id) {
            return users.get(id);
        }

        @Override
        public UserEntity findByUsername(String username) {
            return null;
        }

        @Override
        public int insertUser(UserEntity entity) {
            users.put(entity.getId(), entity);
            return 1;
        }

        @Override
        public long countPageByUserId(Long userId, com.example.lecturesystem.modules.user.dto.UserQueryRequest request) {
            return 0;
        }

        @Override
        public List<com.example.lecturesystem.modules.user.vo.UserListItemVO> queryPageByUserId(Long userId, com.example.lecturesystem.modules.user.dto.UserQueryRequest request) {
            return List.of();
        }

        @Override
        public long countPageByTreePath(String treePathPrefix, com.example.lecturesystem.modules.user.dto.UserQueryRequest request) {
            return 0;
        }

        @Override
        public List<com.example.lecturesystem.modules.user.vo.UserListItemVO> queryPageByTreePath(String treePathPrefix, com.example.lecturesystem.modules.user.dto.UserQueryRequest request) {
            return List.of();
        }

        @Override
        public long countPage(com.example.lecturesystem.modules.user.dto.UserQueryRequest request) {
            return 0;
        }

        @Override
        public List<com.example.lecturesystem.modules.user.vo.UserListItemVO> queryPage(com.example.lecturesystem.modules.user.dto.UserQueryRequest request) {
            return List.of();
        }

        @Override
        public com.example.lecturesystem.modules.user.vo.UserDetailVO detailById(Long id) {
            return null;
        }

        @Override
        public com.example.lecturesystem.modules.user.vo.UserDetailVO detailByIdAndTreePath(Long id, String treePathPrefix) {
            return null;
        }

        @Override
        public int updateUser(UserEntity entity) {
            users.put(entity.getId(), entity);
            return 1;
        }

        @Override
        public int logicalDelete(Long id, String updateUser, LocalDateTime updateTime) {
            users.remove(id);
            return 1;
        }

        @Override
        public int updatePassword(Long id, String passwordHash, String updateUser, LocalDateTime updateTime) {
            UserEntity target = users.get(id);
            if (target != null) {
                target.setPasswordHash(passwordHash);
            }
            return 1;
        }
    }

    private static class InMemoryAttendanceMapper implements AttendanceMapper {
        private final Map<Long, AttendanceRecordEntity> data = new LinkedHashMap<>();
        private final Map<Long, String> userTreePaths = new LinkedHashMap<>();
        private final Map<Long, AttendanceLocationVO> attendanceLocations = new LinkedHashMap<>();
        private final Map<Long, String> unitNames = new LinkedHashMap<>();
        private long sequence = 1L;

        @Override
        public int insert(AttendanceRecordEntity entity) {
            if (entity.getId() == null) {
                entity.setId(sequence++);
            } else if (entity.getId() >= sequence) {
                sequence = entity.getId() + 1;
            }
            entity.setCreateTime(LocalDateTime.now());
            data.put(entity.getId(), cloneEntity(entity));
            return 1;
        }

        @Override
        public AttendanceRecordEntity findById(Long id) {
            AttendanceRecordEntity entity = data.get(id);
            return entity == null ? null : cloneEntity(entity);
        }

        @Override
        public AttendanceRecordEntity findByUserIdAndDate(Long userId, LocalDate attendanceDate) {
            for (AttendanceRecordEntity entity : data.values()) {
                if (userId.equals(entity.getUserId()) && attendanceDate.equals(entity.getAttendanceDate())) {
                    return cloneEntity(entity);
                }
            }
            return null;
        }

        @Override
        public AttendanceLocationVO findAttendanceLocationByUnitId(Long unitId) {
            return attendanceLocations.get(unitId);
        }

        @Override
        public String findUnitNameById(Long unitId) {
            return unitNames.get(unitId);
        }

        @Override
        public int update(AttendanceRecordEntity entity) {
            AttendanceRecordEntity target = data.get(entity.getId());
            target.setUnitId(entity.getUnitId());
            target.setUserId(entity.getUserId());
            target.setAttendanceDate(entity.getAttendanceDate());
            target.setCheckInTime(entity.getCheckInTime());
            target.setCheckOutTime(entity.getCheckOutTime());
            target.setCheckInAddress(entity.getCheckInAddress());
            target.setCheckOutAddress(entity.getCheckOutAddress());
            target.setValidFlag(entity.getValidFlag());
            return 1;
        }

        @Override
        public int updateValidFlag(Long id, Integer validFlag) {
            AttendanceRecordEntity target = data.get(id);
            target.setValidFlag(validFlag);
            return 1;
        }

        @Override
        public int deleteById(Long id) {
            return data.remove(id) == null ? 0 : 1;
        }

        @Override
        public long countDistinctUsersByRange(String treePathPrefix, LocalDate startDate, LocalDate endDate, Integer validFlag) {
            Set<Long> userIds = new java.util.LinkedHashSet<>();
            for (AttendanceRecordEntity entity : data.values()) {
                if (startDate != null && entity.getAttendanceDate().isBefore(startDate)) {
                    continue;
                }
                if (endDate != null && entity.getAttendanceDate().isAfter(endDate)) {
                    continue;
                }
                if (validFlag != null && !validFlag.equals(entity.getValidFlag())) {
                    continue;
                }
                userIds.add(entity.getUserId());
            }
            return userIds.size();
        }

        @Override
        public List<AttendanceRecordListItemVO> queryList(AttendanceQueryRequest request) {
            List<AttendanceRecordListItemVO> result = new ArrayList<>();
            LocalDate start = parseDate(request.getDateFrom());
            LocalDate end = parseDate(request.getDateTo());
            for (AttendanceRecordEntity entity : data.values()) {
                if (start != null && entity.getAttendanceDate().isBefore(start)) {
                    continue;
                }
                if (end != null && entity.getAttendanceDate().isAfter(end)) {
                    continue;
                }
                if (request.getTreePathPrefix() != null && !request.getTreePathPrefix().isBlank()) {
                    String treePath = userTreePaths.get(entity.getUserId());
                    if (treePath == null || !treePath.startsWith(request.getTreePathPrefix())) {
                        continue;
                    }
                }
                if (request.getCheckInStatus() != null && !request.getCheckInStatus().isBlank()) {
                    if (!request.getCheckInStatus().equals(entity.getCheckInResult())) {
                        continue;
                    }
                }
                AttendanceRecordListItemVO item = new AttendanceRecordListItemVO();
                item.setId(entity.getId());
                item.setUnitId(entity.getUnitId());
                item.setUserId(entity.getUserId());
                item.setAttendanceDate(entity.getAttendanceDate());
                item.setCheckInTime(entity.getCheckInTime());
                item.setCheckOutTime(entity.getCheckOutTime());
                item.setCheckInAddress(entity.getCheckInAddress());
                item.setCheckOutAddress(entity.getCheckOutAddress());
                item.setCheckInLatitude(entity.getCheckInLatitude());
                item.setCheckInLongitude(entity.getCheckInLongitude());
                item.setCheckInDistanceMeters(entity.getCheckInDistanceMeters());
                item.setCheckInResult(entity.getCheckInResult());
                item.setCheckInFailReason(entity.getCheckInFailReason());
                item.setValidFlag(entity.getValidFlag());
                item.setCreateTime(entity.getCreateTime());
                result.add(item);
            }
            return result;
        }

        @Override
        public long countByQuery(AttendanceQueryRequest request) {
            return filterEntities(request).size();
        }

        @Override
        public List<AttendanceStatusCountVO> queryStatusCounts(AttendanceQueryRequest request) {
            Map<String, Long> counts = new LinkedHashMap<>();
            for (AttendanceRecordEntity entity : filterEntities(request)) {
                String key = entity.getCheckInResult();
                counts.put(key, counts.getOrDefault(key, 0L) + 1L);
            }
            List<AttendanceStatusCountVO> result = new ArrayList<>();
            for (Map.Entry<String, Long> entry : counts.entrySet()) {
                AttendanceStatusCountVO item = new AttendanceStatusCountVO();
                item.setCheckInStatus(entry.getKey());
                item.setCount(entry.getValue());
                result.add(item);
            }
            return result;
        }

        @Override
        public List<AttendanceAbnormalUserRankVO> queryAbnormalUserRanks(AttendanceQueryRequest request) {
            Map<Long, Long> abnormalCounts = new LinkedHashMap<>();
            Map<Long, Long> totalCounts = new LinkedHashMap<>();
            for (AttendanceRecordEntity entity : filterEntities(withoutStatusFilters(request))) {
                totalCounts.put(entity.getUserId(), totalCounts.getOrDefault(entity.getUserId(), 0L) + 1L);
                if (entity.getCheckInResult() != null
                        && !AttendanceCheckInStatus.CHECK_IN_SUCCESS.equals(entity.getCheckInResult())
                        && !AttendanceCheckInStatus.CHECK_OUT_SUCCESS.equals(entity.getCheckInResult())) {
                    abnormalCounts.put(entity.getUserId(), abnormalCounts.getOrDefault(entity.getUserId(), 0L) + 1L);
                }
            }
            List<AttendanceAbnormalUserRankVO> result = new ArrayList<>();
            for (Map.Entry<Long, Long> entry : abnormalCounts.entrySet()) {
                AttendanceAbnormalUserRankVO item = new AttendanceAbnormalUserRankVO();
                item.setUserId(entry.getKey());
                item.setUsername("user" + entry.getKey());
                item.setRealName("用户" + entry.getKey());
                item.setUnitName("平台单位");
                item.setAbnormalCount(entry.getValue());
                item.setTotalCount(totalCounts.getOrDefault(entry.getKey(), 0L));
                long total = item.getTotalCount() == null ? 0L : item.getTotalCount();
                double rate = total == 0 ? 0D : (entry.getValue() * 100D) / total;
                item.setAbnormalRate(BigDecimal.valueOf(rate).setScale(1, java.math.RoundingMode.HALF_UP));
                result.add(item);
            }
            result.sort(Comparator
                    .comparing(AttendanceAbnormalUserRankVO::getAbnormalCount, Comparator.reverseOrder())
                    .thenComparing(AttendanceAbnormalUserRankVO::getAbnormalRate, Comparator.reverseOrder())
                    .thenComparing(AttendanceAbnormalUserRankVO::getUserId));
            return result.size() > 10 ? result.subList(0, 10) : result;
        }

        @Override
        public List<AttendanceAbnormalTrendPointVO> queryAbnormalTrend(AttendanceQueryRequest request) {
            Map<LocalDate, long[]> counters = new LinkedHashMap<>();
            for (AttendanceRecordEntity entity : filterEntities(withoutStatusFilters(request))) {
                if (request.getUserId() != null && !request.getUserId().equals(entity.getUserId())) {
                    continue;
                }
                long[] values = counters.computeIfAbsent(entity.getAttendanceDate(), key -> new long[]{0L, 0L});
                values[0] += 1L;
                if (entity.getCheckInResult() != null
                        && !AttendanceCheckInStatus.CHECK_IN_SUCCESS.equals(entity.getCheckInResult())
                        && !AttendanceCheckInStatus.CHECK_OUT_SUCCESS.equals(entity.getCheckInResult())) {
                    values[1] += 1L;
                }
            }
            List<AttendanceAbnormalTrendPointVO> result = new ArrayList<>();
            for (Map.Entry<LocalDate, long[]> entry : counters.entrySet()) {
                AttendanceAbnormalTrendPointVO item = new AttendanceAbnormalTrendPointVO();
                item.setAttendanceDate(entry.getKey());
                item.setTotalCount(entry.getValue()[0]);
                item.setAbnormalCount(entry.getValue()[1]);
                double rate = entry.getValue()[0] == 0 ? 0D : (entry.getValue()[1] * 100D) / entry.getValue()[0];
                item.setAbnormalRate(BigDecimal.valueOf(rate).setScale(1, java.math.RoundingMode.HALF_UP));
                result.add(item);
            }
            result.sort(Comparator.comparing(AttendanceAbnormalTrendPointVO::getAttendanceDate));
            return result;
        }

        @Override
        public AttendanceAbnormalUserSummaryVO queryAbnormalUserSummary(AttendanceQueryRequest request) {
            List<AttendanceRecordEntity> filtered = filterEntities(withoutStatusFilters(request));
            AttendanceRecordEntity latest = null;
            long abnormalCount = 0L;
            for (AttendanceRecordEntity entity : filtered) {
                if (request.getUserId() != null && !request.getUserId().equals(entity.getUserId())) {
                    continue;
                }
                if (entity.getCheckInResult() != null
                        && !AttendanceCheckInStatus.CHECK_IN_SUCCESS.equals(entity.getCheckInResult())
                        && !AttendanceCheckInStatus.CHECK_OUT_SUCCESS.equals(entity.getCheckInResult())) {
                    abnormalCount += 1L;
                    if (latest == null
                            || entity.getAttendanceDate().isAfter(latest.getAttendanceDate())
                            || (entity.getAttendanceDate().isEqual(latest.getAttendanceDate())
                            && entity.getId().compareTo(latest.getId()) > 0)) {
                        latest = entity;
                    }
                }
            }
            if (latest == null) {
                return null;
            }
            AttendanceAbnormalUserSummaryVO result = new AttendanceAbnormalUserSummaryVO();
            result.setUserId(latest.getUserId());
            result.setUsername("user" + latest.getUserId());
            result.setRealName("用户" + latest.getUserId());
            result.setUnitName("平台单位");
            result.setAbnormalCount(abnormalCount);
            result.setRecentAbnormalDate(latest.getAttendanceDate());
            result.setRecentAbnormalType(latest.getCheckInResult());
            return result;
        }

        private List<AttendanceRecordEntity> filterEntities(AttendanceQueryRequest request) {
            List<AttendanceRecordEntity> result = new ArrayList<>();
            LocalDate start = parseDate(request.getDateFrom());
            LocalDate end = parseDate(request.getDateTo());
            for (AttendanceRecordEntity entity : data.values()) {
                if (start != null && entity.getAttendanceDate().isBefore(start)) {
                    continue;
                }
                if (end != null && entity.getAttendanceDate().isAfter(end)) {
                    continue;
                }
                if (request.getTreePathPrefix() != null && !request.getTreePathPrefix().isBlank()) {
                    String treePath = userTreePaths.get(entity.getUserId());
                    if (treePath == null || !treePath.startsWith(request.getTreePathPrefix())) {
                        continue;
                    }
                }
                if (request.getCheckInStatus() != null && !request.getCheckInStatus().isBlank()
                        && !request.getCheckInStatus().equals(entity.getCheckInResult())) {
                    continue;
                }
                if (request.getUserId() != null && !request.getUserId().equals(entity.getUserId())) {
                    continue;
                }
                if (Boolean.TRUE.equals(request.getAbnormalOnly())
                        && (AttendanceCheckInStatus.CHECK_IN_SUCCESS.equals(entity.getCheckInResult())
                        || AttendanceCheckInStatus.CHECK_OUT_SUCCESS.equals(entity.getCheckInResult()))) {
                    continue;
                }
                result.add(cloneEntity(entity));
            }
            return result;
        }

        private AttendanceQueryRequest withoutStatusFilters(AttendanceQueryRequest request) {
            AttendanceQueryRequest copy = new AttendanceQueryRequest();
            copy.setTreePathPrefix(request.getTreePathPrefix());
            copy.setKeywords(request.getKeywords());
            copy.setUnitName(request.getUnitName());
            copy.setDateFrom(request.getDateFrom());
            copy.setDateTo(request.getDateTo());
            copy.setUserId(request.getUserId());
            return copy;
        }

        private LocalDate parseDate(String text) {
            return text == null || text.isBlank() ? null : LocalDate.parse(text);
        }

        private AttendanceRecordEntity cloneEntity(AttendanceRecordEntity entity) {
            AttendanceRecordEntity copy = new AttendanceRecordEntity();
            copy.setId(entity.getId());
            copy.setUnitId(entity.getUnitId());
            copy.setUserId(entity.getUserId());
            copy.setAttendanceDate(entity.getAttendanceDate());
            copy.setCheckInTime(entity.getCheckInTime());
            copy.setCheckOutTime(entity.getCheckOutTime());
            copy.setCheckInAddress(entity.getCheckInAddress());
            copy.setCheckOutAddress(entity.getCheckOutAddress());
            copy.setCheckInLatitude(entity.getCheckInLatitude());
            copy.setCheckInLongitude(entity.getCheckInLongitude());
            copy.setCheckInDistanceMeters(entity.getCheckInDistanceMeters());
            copy.setCheckInResult(entity.getCheckInResult());
            copy.setCheckInFailReason(entity.getCheckInFailReason());
            copy.setValidFlag(entity.getValidFlag());
            copy.setCreateTime(entity.getCreateTime());
            return copy;
        }
    }
}
