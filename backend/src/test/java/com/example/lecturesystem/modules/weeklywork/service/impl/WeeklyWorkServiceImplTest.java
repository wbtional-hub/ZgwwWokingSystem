package com.example.lecturesystem.modules.weeklywork.service.impl;

import com.example.lecturesystem.modules.auth.security.LoginUser;
import com.example.lecturesystem.modules.permission.service.PermissionService;
import com.example.lecturesystem.modules.user.entity.UserEntity;
import com.example.lecturesystem.modules.user.mapper.UserMapper;
import com.example.lecturesystem.modules.weeklywork.dto.SaveWeeklyWorkRequest;
import com.example.lecturesystem.modules.weeklywork.dto.SubmitWeeklyWorkRequest;
import com.example.lecturesystem.modules.weeklywork.dto.WeeklyWorkQueryRequest;
import com.example.lecturesystem.modules.weeklywork.entity.WeeklyWorkEntity;
import com.example.lecturesystem.modules.weeklywork.mapper.WeeklyWorkMapper;
import com.example.lecturesystem.modules.weeklywork.vo.WeeklyWorkListItemVO;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class WeeklyWorkServiceImplTest {
    @After
    public void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void saveDraftShouldInsertWhenCurrentWeekNotExists() {
        InMemoryWeeklyWorkMapper weeklyWorkMapper = new InMemoryWeeklyWorkMapper();
        StubPermissionService permissionService = new StubPermissionService(false, Set.of(3L));
        StubUserMapper userMapper = new StubUserMapper();
        userMapper.users.put(3L, user(3L, 2L));
        WeeklyWorkServiceImpl service = new WeeklyWorkServiceImpl(weeklyWorkMapper, permissionService, userMapper);

        mockLoginUser(3L, false);
        SaveWeeklyWorkRequest request = new SaveWeeklyWorkRequest();
        request.setWeekNo("2026-W11");
        request.setWorkPlan("计划");
        request.setWorkContent("内容");
        request.setRemark("备注");

        Long id = service.saveDraft(request);

        Assert.assertEquals(Long.valueOf(1L), id);
        WeeklyWorkEntity saved = weeklyWorkMapper.findById(id);
        Assert.assertEquals("DRAFT", saved.getStatus());
        Assert.assertEquals(Long.valueOf(3L), saved.getUserId());
        Assert.assertEquals("2026-W11", saved.getWeekNo());
    }

    @Test
    public void saveDraftShouldRejectOverwriteSubmittedRecord() {
        InMemoryWeeklyWorkMapper weeklyWorkMapper = new InMemoryWeeklyWorkMapper();
        StubPermissionService permissionService = new StubPermissionService(false, Set.of(3L));
        StubUserMapper userMapper = new StubUserMapper();
        userMapper.users.put(3L, user(3L, 2L));
        weeklyWorkMapper.insert(seedWeekly(2L, 3L, "2026-W11", "SUBMITTED"));
        WeeklyWorkServiceImpl service = new WeeklyWorkServiceImpl(weeklyWorkMapper, permissionService, userMapper);

        mockLoginUser(3L, false);
        SaveWeeklyWorkRequest request = new SaveWeeklyWorkRequest();
        request.setWeekNo("2026-W11");

        try {
            service.saveDraft(request);
            Assert.fail("预期不允许覆盖已提交周工作");
        } catch (IllegalArgumentException ex) {
            Assert.assertEquals("当前周工作已提交，不能直接覆盖草稿", ex.getMessage());
        }
    }

    @Test
    public void submitShouldUpdateStatusAndSubmitTime() {
        InMemoryWeeklyWorkMapper weeklyWorkMapper = new InMemoryWeeklyWorkMapper();
        StubPermissionService permissionService = new StubPermissionService(false, Set.of(3L));
        StubUserMapper userMapper = new StubUserMapper();
        userMapper.users.put(3L, user(3L, 2L));
        WeeklyWorkEntity entity = seedWeekly(2L, 3L, "2026-W11", "DRAFT");
        weeklyWorkMapper.insert(entity);
        WeeklyWorkServiceImpl service = new WeeklyWorkServiceImpl(weeklyWorkMapper, permissionService, userMapper);

        mockLoginUser(3L, false);
        SubmitWeeklyWorkRequest request = new SubmitWeeklyWorkRequest();
        request.setId(entity.getId());

        service.submit(request);

        WeeklyWorkEntity submitted = weeklyWorkMapper.findById(entity.getId());
        Assert.assertEquals("SUBMITTED", submitted.getStatus());
        Assert.assertTrue(submitted.getSubmitTime() != null);
    }

    @Test
    public void queryShouldUseDataScopeForNonSuperAdmin() {
        InMemoryWeeklyWorkMapper weeklyWorkMapper = new InMemoryWeeklyWorkMapper();
        StubPermissionService permissionService = new StubPermissionService(false, Set.of(3L, 4L));
        StubUserMapper userMapper = new StubUserMapper();
        userMapper.users.put(3L, user(3L, 2L));
        weeklyWorkMapper.insert(seedWeekly(2L, 3L, "2026-W11", "DRAFT"));
        weeklyWorkMapper.insert(seedWeekly(2L, 4L, "2026-W11", "SUBMITTED"));
        weeklyWorkMapper.insert(seedWeekly(2L, 5L, "2026-W11", "SUBMITTED"));
        WeeklyWorkServiceImpl service = new WeeklyWorkServiceImpl(weeklyWorkMapper, permissionService, userMapper);

        mockLoginUser(3L, false);
        WeeklyWorkQueryRequest request = new WeeklyWorkQueryRequest();
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

    private WeeklyWorkEntity seedWeekly(Long unitId, Long userId, String weekNo, String status) {
        WeeklyWorkEntity entity = new WeeklyWorkEntity();
        entity.setUnitId(unitId);
        entity.setUserId(userId);
        entity.setWeekNo(weekNo);
        entity.setStatus(status);
        entity.setWorkPlan("计划");
        entity.setWorkContent("内容");
        entity.setRemark("备注");
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
            for (UserEntity user : users.values()) {
                if (username.equals(user.getUsername())) {
                    return user;
                }
            }
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

    private static class InMemoryWeeklyWorkMapper implements WeeklyWorkMapper {
        private final Map<Long, WeeklyWorkEntity> data = new LinkedHashMap<>();
        private long sequence = 1L;

        @Override
        public WeeklyWorkEntity findByUserIdAndWeekNo(Long userId, String weekNo) {
            for (WeeklyWorkEntity entity : data.values()) {
                if (userId.equals(entity.getUserId()) && weekNo.equals(entity.getWeekNo())) {
                    return cloneEntity(entity);
                }
            }
            return null;
        }

        @Override
        public WeeklyWorkEntity findById(Long id) {
            WeeklyWorkEntity entity = data.get(id);
            return entity == null ? null : cloneEntity(entity);
        }

        @Override
        public int insert(WeeklyWorkEntity entity) {
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
        public int updateDraft(WeeklyWorkEntity entity) {
            WeeklyWorkEntity target = data.get(entity.getId());
            target.setStatus(entity.getStatus());
            target.setWorkPlan(entity.getWorkPlan());
            target.setWorkContent(entity.getWorkContent());
            target.setRemark(entity.getRemark());
            target.setSubmitTime(null);
            return 1;
        }

        @Override
        public int markSubmitted(Long id, LocalDateTime submitTime) {
            WeeklyWorkEntity target = data.get(id);
            target.setStatus("SUBMITTED");
            target.setSubmitTime(submitTime);
            return 1;
        }

        @Override
        public int updateStatus(Long id, String status) {
            WeeklyWorkEntity target = data.get(id);
            target.setStatus(status);
            return 1;
        }

        @Override
        public long countDistinctSubmittedUsers(Long unitId, String weekNo, String status) {
            Set<Long> userIds = new java.util.LinkedHashSet<>();
            for (WeeklyWorkEntity entity : data.values()) {
                if (unitId != null && !unitId.equals(entity.getUnitId())) {
                    continue;
                }
                if (weekNo != null && !weekNo.isEmpty() && !weekNo.equals(entity.getWeekNo())) {
                    continue;
                }
                if (status != null && !status.isEmpty() && !status.equals(entity.getStatus())) {
                    continue;
                }
                userIds.add(entity.getUserId());
            }
            return userIds.size();
        }

        @Override
        public List<WeeklyWorkListItemVO> queryList(com.example.lecturesystem.modules.weeklywork.dto.WeeklyWorkQueryRequest request) {
            List<WeeklyWorkListItemVO> result = new ArrayList<>();
            for (WeeklyWorkEntity entity : data.values()) {
                if (request.getUnitId() != null && !request.getUnitId().equals(entity.getUnitId())) {
                    continue;
                }
                if (request.getUserId() != null && !request.getUserId().equals(entity.getUserId())) {
                    continue;
                }
                if (request.getWeekNo() != null && !request.getWeekNo().isEmpty() && !request.getWeekNo().equals(entity.getWeekNo())) {
                    continue;
                }
                if (request.getStatus() != null && !request.getStatus().isEmpty() && !request.getStatus().equals(entity.getStatus())) {
                    continue;
                }
                WeeklyWorkListItemVO item = new WeeklyWorkListItemVO();
                item.setId(entity.getId());
                item.setUnitId(entity.getUnitId());
                item.setUserId(entity.getUserId());
                item.setWeekNo(entity.getWeekNo());
                item.setStatus(entity.getStatus());
                item.setWorkPlan(entity.getWorkPlan());
                item.setWorkContent(entity.getWorkContent());
                item.setRemark(entity.getRemark());
                item.setSubmitTime(entity.getSubmitTime());
                item.setCreateTime(entity.getCreateTime());
                result.add(item);
            }
            return result;
        }

        private WeeklyWorkEntity cloneEntity(WeeklyWorkEntity entity) {
            WeeklyWorkEntity copy = new WeeklyWorkEntity();
            copy.setId(entity.getId());
            copy.setUnitId(entity.getUnitId());
            copy.setUserId(entity.getUserId());
            copy.setWeekNo(entity.getWeekNo());
            copy.setStatus(entity.getStatus());
            copy.setWorkPlan(entity.getWorkPlan());
            copy.setWorkContent(entity.getWorkContent());
            copy.setRemark(entity.getRemark());
            copy.setSubmitTime(entity.getSubmitTime());
            copy.setCreateTime(entity.getCreateTime());
            return copy;
        }
    }
}
