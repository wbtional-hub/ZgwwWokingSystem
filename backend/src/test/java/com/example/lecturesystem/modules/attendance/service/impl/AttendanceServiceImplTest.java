package com.example.lecturesystem.modules.attendance.service.impl;

import com.example.lecturesystem.modules.attendance.dto.AttendanceQueryRequest;
import com.example.lecturesystem.modules.attendance.dto.CheckInRequest;
import com.example.lecturesystem.modules.attendance.entity.AttendanceRecordEntity;
import com.example.lecturesystem.modules.attendance.mapper.AttendanceMapper;
import com.example.lecturesystem.modules.attendance.vo.AttendanceRecordListItemVO;
import com.example.lecturesystem.modules.auth.security.LoginUser;
import com.example.lecturesystem.modules.permission.service.PermissionService;
import com.example.lecturesystem.modules.user.entity.UserEntity;
import com.example.lecturesystem.modules.user.mapper.UserMapper;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
        userMapper.users.put(3L, user(3L, 2L));
        AttendanceServiceImpl service = new AttendanceServiceImpl(attendanceMapper, permissionService, userMapper);

        mockLoginUser(3L, false);
        CheckInRequest request = new CheckInRequest();
        request.setAddress("办公楼");

        Map<?, ?> result = (Map<?, ?>) service.checkIn(request);

        Assert.assertEquals("CHECK_IN", result.get("action"));
        Assert.assertEquals(1, attendanceMapper.data.size());
        Assert.assertEquals(LocalDate.now(), attendanceMapper.data.get(1L).getAttendanceDate());
        Assert.assertNotNull(attendanceMapper.data.get(1L).getCheckInTime());
        Assert.assertEquals(Integer.valueOf(1), attendanceMapper.data.get(1L).getValidFlag());
    }

    @Test
    public void secondCheckInShouldFillCheckOutTime() {
        InMemoryAttendanceMapper attendanceMapper = new InMemoryAttendanceMapper();
        StubPermissionService permissionService = new StubPermissionService(false, Set.of(3L));
        StubUserMapper userMapper = new StubUserMapper();
        userMapper.users.put(3L, user(3L, 2L));
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

        Map<?, ?> result = (Map<?, ?>) service.checkIn(request);

        Assert.assertEquals("CHECK_OUT", result.get("action"));
        Assert.assertNotNull(attendanceMapper.data.get(existed.getId()).getCheckOutTime());
        Assert.assertEquals("园区大门", attendanceMapper.data.get(existed.getId()).getCheckOutAddress());
    }

    @Test
    public void queryShouldUseDataScopeForNonSuperAdmin() {
        InMemoryAttendanceMapper attendanceMapper = new InMemoryAttendanceMapper();
        StubPermissionService permissionService = new StubPermissionService(false, Set.of(3L, 4L));
        StubUserMapper userMapper = new StubUserMapper();
        userMapper.users.put(3L, user(3L, 2L));
        AttendanceServiceImpl service = new AttendanceServiceImpl(attendanceMapper, permissionService, userMapper);

        attendanceMapper.insert(seedAttendance(2L, 3L, LocalDate.now().minusDays(1)));
        attendanceMapper.insert(seedAttendance(2L, 4L, LocalDate.now()));
        attendanceMapper.insert(seedAttendance(2L, 5L, LocalDate.now()));

        mockLoginUser(3L, false);
        AttendanceQueryRequest request = new AttendanceQueryRequest();
        List<?> result = (List<?>) service.query(request);

        Assert.assertEquals(2, result.size());
    }

    private void mockLoginUser(Long userId, boolean superAdmin) {
        LoginUser loginUser = new LoginUser(userId, "tester", "测试用户", superAdmin);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, java.util.List.of())
        );
    }

    private UserEntity user(Long id, Long unitId) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setUnitId(unitId);
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
        public long countDistinctUsersByRange(Long unitId, LocalDate startDate, LocalDate endDate, Integer validFlag) {
            Set<Long> userIds = new java.util.LinkedHashSet<>();
            for (AttendanceRecordEntity entity : data.values()) {
                if (unitId != null && !unitId.equals(entity.getUnitId())) {
                    continue;
                }
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
                if (request.getUnitId() != null && !request.getUnitId().equals(entity.getUnitId())) {
                    continue;
                }
                if (start != null && entity.getAttendanceDate().isBefore(start)) {
                    continue;
                }
                if (end != null && entity.getAttendanceDate().isAfter(end)) {
                    continue;
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
                item.setValidFlag(entity.getValidFlag());
                item.setCreateTime(entity.getCreateTime());
                result.add(item);
            }
            return result;
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
            copy.setValidFlag(entity.getValidFlag());
            copy.setCreateTime(entity.getCreateTime());
            return copy;
        }
    }
}
