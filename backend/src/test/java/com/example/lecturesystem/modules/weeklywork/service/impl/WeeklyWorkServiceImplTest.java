package com.example.lecturesystem.modules.weeklywork.service.impl;

import com.example.lecturesystem.modules.auth.security.LoginUser;
import com.example.lecturesystem.modules.permission.service.PermissionService;
import com.example.lecturesystem.modules.user.entity.UserEntity;
import com.example.lecturesystem.modules.user.mapper.UserMapper;
import com.example.lecturesystem.modules.weeklywork.dto.ReviewWeeklyWorkRequest;
import com.example.lecturesystem.modules.weeklywork.dto.SaveWeeklyWorkRequest;
import com.example.lecturesystem.modules.weeklywork.dto.SubmitWeeklyWorkRequest;
import com.example.lecturesystem.modules.weeklywork.dto.WeeklyWorkQueryRequest;
import com.example.lecturesystem.modules.weeklywork.entity.WeeklyWorkApprovalLogEntity;
import com.example.lecturesystem.modules.weeklywork.entity.WeeklyWorkEntity;
import com.example.lecturesystem.modules.weeklywork.mapper.WeeklyWorkMapper;
import com.example.lecturesystem.modules.weeklywork.vo.WeeklyWorkApprovalLogVO;
import com.example.lecturesystem.modules.weeklywork.vo.WeeklyWorkDetailVO;
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
        StubPermissionService permissionService = new StubPermissionService(Set.of(), Set.of(3L));
        StubUserMapper userMapper = new StubUserMapper();
        userMapper.users.put(3L, user(3L, 2L, "/3/"));
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
        StubPermissionService permissionService = new StubPermissionService(Set.of(), Set.of(3L));
        StubUserMapper userMapper = new StubUserMapper();
        userMapper.users.put(3L, user(3L, 2L, "/3/"));
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
        StubPermissionService permissionService = new StubPermissionService(Set.of(1L), Set.of(2L, 3L));
        StubUserMapper userMapper = new StubUserMapper();
        userMapper.users.put(1L, user(1L, 2L, "/1/"));
        userMapper.users.put(2L, user(2L, 2L, "/1/2/"));
        userMapper.users.put(3L, user(3L, 2L, "/1/2/3/"));
        userMapper.users.get(2L).setParentUserId(1L);
        userMapper.users.get(3L).setParentUserId(2L);
        WeeklyWorkEntity entity = seedWeekly(2L, 3L, "2026-W11", "DRAFT");
        weeklyWorkMapper.insert(entity);
        WeeklyWorkServiceImpl service = new WeeklyWorkServiceImpl(weeklyWorkMapper, permissionService, userMapper);

        mockLoginUser(3L, false);
        SubmitWeeklyWorkRequest request = new SubmitWeeklyWorkRequest();
        request.setId(entity.getId());

        service.submit(request);

        WeeklyWorkEntity submitted = weeklyWorkMapper.findById(entity.getId());
        Assert.assertEquals("PENDING_SECTION_CHIEF", submitted.getStatus());
        Assert.assertEquals("USER_2", submitted.getCurrentApprovalNode());
        Assert.assertEquals(Long.valueOf(2L), submitted.getCurrentHandlerUserId());
        Assert.assertEquals("用户2", submitted.getCurrentHandlerUserName());
        Assert.assertEquals(Integer.valueOf(2), submitted.getCurrentFlowOrder());
        Assert.assertEquals(Long.valueOf(2L), submitted.getFinalApproverUserId());
        Assert.assertNull(submitted.getApprovedTime());
        Assert.assertTrue(submitted.getSubmitTime() != null);

        WeeklyWorkDetailVO detail = (WeeklyWorkDetailVO) service.detail(entity.getId());
        Assert.assertEquals("USER_2", detail.getCurrentApprovalNode());
        Assert.assertEquals(Long.valueOf(2L), detail.getCurrentHandlerUserId());
        Assert.assertEquals(Integer.valueOf(2), detail.getCurrentFlowOrder());
        Assert.assertEquals(2, detail.getFlowNodes().size());
        Assert.assertEquals("STAFF", detail.getFlowNodes().get(0).getRoleCode());
        Assert.assertEquals("SECTION_CHIEF", detail.getFlowNodes().get(1).getRoleCode());
    }

    @Test
    public void submitShouldApproveImmediatelyWhenOnlyParentIsSuperAdmin() {
        InMemoryWeeklyWorkMapper weeklyWorkMapper = new InMemoryWeeklyWorkMapper();
        StubPermissionService permissionService = new StubPermissionService(Set.of(1L), Set.of(1L, 3L));
        StubUserMapper userMapper = new StubUserMapper();
        userMapper.users.put(1L, user(1L, 2L, "/1/"));
        userMapper.users.put(3L, user(3L, 2L, "/1/3/"));
        userMapper.users.get(3L).setParentUserId(1L);
        WeeklyWorkEntity entity = seedWeekly(2L, 3L, "2026-W11", "DRAFT");
        weeklyWorkMapper.insert(entity);
        WeeklyWorkServiceImpl service = new WeeklyWorkServiceImpl(weeklyWorkMapper, permissionService, userMapper);

        mockLoginUser(3L, false);
        SubmitWeeklyWorkRequest request = new SubmitWeeklyWorkRequest();
        request.setId(entity.getId());

        service.submit(request);

        WeeklyWorkEntity submitted = weeklyWorkMapper.findById(entity.getId());
        Assert.assertEquals("APPROVED", submitted.getStatus());
        Assert.assertEquals(null, submitted.getCurrentApprovalNode());
        Assert.assertNull(submitted.getCurrentHandlerUserId());
        Assert.assertNull(submitted.getCurrentHandlerUserName());
        Assert.assertNull(submitted.getCurrentFlowOrder());
        Assert.assertNull(submitted.getFinalApproverUserId());
        Assert.assertNotNull(submitted.getApprovedTime());
        Assert.assertTrue(submitted.getSubmitTime() != null);

        WeeklyWorkDetailVO detail = (WeeklyWorkDetailVO) service.detail(entity.getId());
        Assert.assertEquals("APPROVED", detail.getCurrentApprovalNode());
        Assert.assertEquals(1, detail.getFlowNodes().size());
    }

    @Test
    public void reviewReturnShouldRequireCommentAndWriteApprovalLog() {
        InMemoryWeeklyWorkMapper weeklyWorkMapper = new InMemoryWeeklyWorkMapper();
        StubPermissionService permissionService = new StubPermissionService(Set.of(1L), Set.of(2L, 3L, 4L));
        StubUserMapper userMapper = new StubUserMapper();
        userMapper.users.put(1L, user(1L, 2L, "/1/"));
        userMapper.users.put(2L, user(2L, 2L, "/1/2/"));
        userMapper.users.put(3L, user(3L, 2L, "/1/2/3/"));
        userMapper.users.put(4L, user(4L, 2L, "/1/2/4/"));
        userMapper.users.get(2L).setParentUserId(1L);
        userMapper.users.get(3L).setParentUserId(2L);
        userMapper.users.get(4L).setParentUserId(2L);
        WeeklyWorkEntity entity = seedWeekly(2L, 3L, "2026-W11", "PENDING_DEPUTY_LEADER");
        entity.setCurrentApprovalNode("USER_2");
        weeklyWorkMapper.insert(entity);
        WeeklyWorkServiceImpl service = new WeeklyWorkServiceImpl(weeklyWorkMapper, permissionService, userMapper);

        mockLoginUser(2L, false);
        ReviewWeeklyWorkRequest request = new ReviewWeeklyWorkRequest();
        request.setId(entity.getId());
        request.setAction("RETURN");
        request.setReturnTarget("STAFF");
        request.setComment("请先补齐科室意见");

        service.review(request);

        WeeklyWorkEntity returned = weeklyWorkMapper.findById(entity.getId());
        Assert.assertEquals("RETURNED", returned.getStatus());
        Assert.assertEquals("STAFF", returned.getCurrentApprovalNode());
        Assert.assertEquals(Long.valueOf(3L), returned.getCurrentHandlerUserId());
        Assert.assertEquals("用户3", returned.getCurrentHandlerUserName());
        Assert.assertEquals(Integer.valueOf(1), returned.getCurrentFlowOrder());
        Assert.assertEquals(Long.valueOf(2L), returned.getFinalApproverUserId());
        Assert.assertEquals("STAFF", returned.getLastReturnTarget());
        Assert.assertEquals("请先补齐科室意见", returned.getLastReturnComment());
        Assert.assertEquals(Long.valueOf(2L), returned.getLastReviewBy());
        Assert.assertEquals("测试用户", returned.getLastReviewByName());
        Assert.assertNull(returned.getApprovedTime());
        Assert.assertEquals(1, weeklyWorkMapper.queryApprovalLogs(entity.getId()).size());
    }

    @Test
    public void detailShouldContainApprovalMetadata() {
        InMemoryWeeklyWorkMapper weeklyWorkMapper = new InMemoryWeeklyWorkMapper();
        StubPermissionService permissionService = new StubPermissionService(Set.of(1L), Set.of(2L, 3L, 4L));
        StubUserMapper userMapper = new StubUserMapper();
        userMapper.users.put(1L, user(1L, 2L, "/1/"));
        userMapper.users.put(2L, user(2L, 2L, "/1/2/"));
        userMapper.users.put(3L, user(3L, 2L, "/1/2/3/"));
        userMapper.users.put(4L, user(4L, 2L, "/1/2/4/"));
        userMapper.users.get(2L).setParentUserId(1L);
        userMapper.users.get(3L).setParentUserId(2L);
        userMapper.users.get(4L).setParentUserId(2L);
        WeeklyWorkEntity entity = seedWeekly(2L, 3L, "2026-W11", "PENDING_LEGION_LEADER");
        entity.setCurrentApprovalNode("USER_2");
        weeklyWorkMapper.insert(entity);
        WeeklyWorkApprovalLogEntity logEntity = new WeeklyWorkApprovalLogEntity();
        logEntity.setWeeklyWorkId(entity.getId());
        logEntity.setAction("APPROVE");
        logEntity.setFromNode("USER_2");
        logEntity.setToNode("APPROVED");
        logEntity.setReviewerUserId(2L);
        logEntity.setReviewerName("审批人");
        logEntity.setComment("继续流转");
        weeklyWorkMapper.insertApprovalLog(logEntity);
        WeeklyWorkServiceImpl service = new WeeklyWorkServiceImpl(weeklyWorkMapper, permissionService, userMapper);

        mockLoginUser(3L, false);
        WeeklyWorkDetailVO detail = (WeeklyWorkDetailVO) service.detail(entity.getId());

        Assert.assertEquals("USER_2", detail.getCurrentApprovalNode());
        Assert.assertEquals(1, detail.getAvailableReturnTargets().size());
        Assert.assertEquals(1, detail.getApprovalLogs().size());
        Assert.assertEquals(2, detail.getFlowNodes().size());
    }

    @Test
    public void reviewApproveShouldMoveDetailToNextApprover() {
        InMemoryWeeklyWorkMapper weeklyWorkMapper = new InMemoryWeeklyWorkMapper();
        StubPermissionService permissionService = new StubPermissionService(Set.of(1L), Set.of(1L, 2L, 3L, 4L));
        StubUserMapper userMapper = new StubUserMapper();
        userMapper.users.put(1L, user(1L, 2L, "/1/"));
        userMapper.users.put(2L, user(2L, 2L, "/1/2/"));
        userMapper.users.put(3L, user(3L, 2L, "/1/2/3/"));
        userMapper.users.put(4L, user(4L, 2L, "/1/2/3/4/"));
        userMapper.users.get(2L).setParentUserId(1L);
        userMapper.users.get(3L).setParentUserId(2L);
        userMapper.users.get(4L).setParentUserId(3L);
        WeeklyWorkEntity entity = seedWeekly(2L, 4L, "2026-W11", "PENDING_SECTION_CHIEF");
        entity.setCurrentApprovalNode("USER_3");
        weeklyWorkMapper.insert(entity);
        WeeklyWorkServiceImpl service = new WeeklyWorkServiceImpl(weeklyWorkMapper, permissionService, userMapper);

        mockLoginUser(3L, false);
        ReviewWeeklyWorkRequest request = new ReviewWeeklyWorkRequest();
        request.setId(entity.getId());
        request.setAction("APPROVE");

        service.review(request);

        WeeklyWorkDetailVO detail = (WeeklyWorkDetailVO) service.detail(entity.getId());
        Assert.assertEquals("PENDING_LEGION_LEADER", detail.getStatus());
        Assert.assertEquals("USER_2", detail.getCurrentApprovalNode());
        Assert.assertEquals(Long.valueOf(2L), detail.getCurrentHandlerUserId());
        Assert.assertEquals("用户2", detail.getCurrentHandlerUserName());
        Assert.assertEquals(Integer.valueOf(3), detail.getCurrentFlowOrder());
        Assert.assertEquals(Long.valueOf(2L), detail.getFinalApproverUserId());
        Assert.assertNull(detail.getApprovedTime());
        Assert.assertEquals(3, detail.getFlowNodes().size());
    }

    @Test
    public void reviewApproveShouldCompleteFinalApprovalAndWriteApprovedSnapshot() {
        InMemoryWeeklyWorkMapper weeklyWorkMapper = new InMemoryWeeklyWorkMapper();
        StubPermissionService permissionService = new StubPermissionService(Set.of(1L), Set.of(1L, 2L, 3L));
        StubUserMapper userMapper = new StubUserMapper();
        userMapper.users.put(1L, user(1L, 2L, "/1/"));
        userMapper.users.put(2L, user(2L, 2L, "/1/2/"));
        userMapper.users.put(3L, user(3L, 2L, "/1/2/3/"));
        userMapper.users.get(2L).setParentUserId(1L);
        userMapper.users.get(3L).setParentUserId(2L);
        WeeklyWorkEntity entity = seedWeekly(2L, 3L, "2026-W11", "PENDING_SECTION_CHIEF");
        entity.setCurrentApprovalNode("USER_2");
        entity.setCurrentHandlerUserId(2L);
        entity.setCurrentHandlerUserName("用户2");
        entity.setCurrentFlowOrder(2);
        entity.setFinalApproverUserId(2L);
        weeklyWorkMapper.insert(entity);
        WeeklyWorkServiceImpl service = new WeeklyWorkServiceImpl(weeklyWorkMapper, permissionService, userMapper);

        mockLoginUser(2L, false);
        ReviewWeeklyWorkRequest request = new ReviewWeeklyWorkRequest();
        request.setId(entity.getId());
        request.setAction("APPROVE");

        service.review(request);

        WeeklyWorkEntity approved = weeklyWorkMapper.findById(entity.getId());
        Assert.assertEquals("APPROVED", approved.getStatus());
        Assert.assertNull(approved.getCurrentApprovalNode());
        Assert.assertNull(approved.getCurrentHandlerUserId());
        Assert.assertNull(approved.getCurrentHandlerUserName());
        Assert.assertEquals(Integer.valueOf(2), approved.getCurrentFlowOrder());
        Assert.assertEquals(Long.valueOf(2L), approved.getFinalApproverUserId());
        Assert.assertEquals(Long.valueOf(2L), approved.getLastReviewBy());
        Assert.assertEquals("测试用户", approved.getLastReviewByName());
        Assert.assertNotNull(approved.getApprovedTime());
    }

    @Test
    public void submitShouldThrowWhenDatabaseUpdateFails() {
        InMemoryWeeklyWorkMapper weeklyWorkMapper = new InMemoryWeeklyWorkMapper();
        weeklyWorkMapper.failMarkSubmitted = true;
        StubPermissionService permissionService = new StubPermissionService(Set.of(1L), Set.of(2L, 3L));
        StubUserMapper userMapper = new StubUserMapper();
        userMapper.users.put(1L, user(1L, 2L, "/1/"));
        userMapper.users.put(2L, user(2L, 2L, "/1/2/"));
        userMapper.users.put(3L, user(3L, 2L, "/1/2/3/"));
        userMapper.users.get(2L).setParentUserId(1L);
        userMapper.users.get(3L).setParentUserId(2L);
        WeeklyWorkEntity entity = seedWeekly(2L, 3L, "2026-W11", "DRAFT");
        weeklyWorkMapper.insert(entity);
        WeeklyWorkServiceImpl service = new WeeklyWorkServiceImpl(weeklyWorkMapper, permissionService, userMapper);

        mockLoginUser(3L, false);
        SubmitWeeklyWorkRequest request = new SubmitWeeklyWorkRequest();
        request.setId(entity.getId());

        IllegalStateException error = Assert.assertThrows(IllegalStateException.class, () -> service.submit(request));
        Assert.assertEquals("提交周报失败，请稍后重试", error.getMessage());
    }

    @Test
    public void reviewShouldThrowWhenDatabaseUpdateFails() {
        InMemoryWeeklyWorkMapper weeklyWorkMapper = new InMemoryWeeklyWorkMapper();
        weeklyWorkMapper.failUpdateApproval = true;
        StubPermissionService permissionService = new StubPermissionService(Set.of(1L), Set.of(1L, 2L, 3L));
        StubUserMapper userMapper = new StubUserMapper();
        userMapper.users.put(1L, user(1L, 2L, "/1/"));
        userMapper.users.put(2L, user(2L, 2L, "/1/2/"));
        userMapper.users.put(3L, user(3L, 2L, "/1/2/3/"));
        userMapper.users.get(2L).setParentUserId(1L);
        userMapper.users.get(3L).setParentUserId(2L);
        WeeklyWorkEntity entity = seedWeekly(2L, 3L, "2026-W11", "PENDING_SECTION_CHIEF");
        entity.setCurrentApprovalNode("USER_2");
        weeklyWorkMapper.insert(entity);
        WeeklyWorkServiceImpl service = new WeeklyWorkServiceImpl(weeklyWorkMapper, permissionService, userMapper);

        mockLoginUser(2L, false);
        ReviewWeeklyWorkRequest request = new ReviewWeeklyWorkRequest();
        request.setId(entity.getId());
        request.setAction("APPROVE");

        IllegalStateException error = Assert.assertThrows(IllegalStateException.class, () -> service.review(request));
        Assert.assertEquals("更新审批状态失败，请稍后重试", error.getMessage());
    }

    @Test
    public void queryShouldUseDataScopeForNonSuperAdmin() {
        InMemoryWeeklyWorkMapper weeklyWorkMapper = new InMemoryWeeklyWorkMapper();
        StubPermissionService permissionService = new StubPermissionService(Set.of(), Set.of(3L, 4L));
        StubUserMapper userMapper = new StubUserMapper();
        userMapper.users.put(3L, user(3L, 2L, "/3/"));
        userMapper.users.put(4L, user(4L, 2L, "/3/4/"));
        userMapper.users.put(5L, user(5L, 2L, "/5/"));
        weeklyWorkMapper.userTreePaths.put(3L, "/3/");
        weeklyWorkMapper.userTreePaths.put(4L, "/3/4/");
        weeklyWorkMapper.userTreePaths.put(5L, "/5/");
        weeklyWorkMapper.insert(seedWeekly(2L, 3L, "2026-W11", "DRAFT"));
        weeklyWorkMapper.insert(seedWeekly(2L, 4L, "2026-W11", "SUBMITTED"));
        weeklyWorkMapper.insert(seedWeekly(2L, 5L, "2026-W11", "SUBMITTED"));
        WeeklyWorkServiceImpl service = new WeeklyWorkServiceImpl(weeklyWorkMapper, permissionService, userMapper);

        mockLoginUser(3L, false);
        WeeklyWorkQueryRequest request = new WeeklyWorkQueryRequest();
        List<?> result = (List<?>) service.query(request);

        Assert.assertEquals(2, result.size());
        WeeklyWorkListItemVO submitted = (WeeklyWorkListItemVO) result.stream()
                .filter(item -> "SUBMITTED".equals(((WeeklyWorkListItemVO) item).getStatus()))
                .findFirst()
                .orElseThrow();
        Assert.assertEquals(1, submitted.getFlowNodes().size());
    }

    @Test
    public void detailShouldRejectWeeklyWorkOutsideTreePathScope() {
        InMemoryWeeklyWorkMapper weeklyWorkMapper = new InMemoryWeeklyWorkMapper();
        StubPermissionService permissionService = new StubPermissionService(Set.of(), Set.of(3L, 4L, 9L));
        StubUserMapper userMapper = new StubUserMapper();
        userMapper.users.put(3L, user(3L, 2L, "/3/"));
        userMapper.users.put(4L, user(4L, 2L, "/3/4/"));
        userMapper.users.put(9L, user(9L, 2L, "/9/"));
        WeeklyWorkEntity other = seedWeekly(2L, 9L, "2026-W11", "SUBMITTED");
        weeklyWorkMapper.insert(other);
        WeeklyWorkServiceImpl service = new WeeklyWorkServiceImpl(weeklyWorkMapper, permissionService, userMapper);

        mockLoginUser(3L, false);

        IllegalArgumentException error = Assert.assertThrows(IllegalArgumentException.class, () -> service.detail(other.getId()));

        Assert.assertEquals("无权查看该周报", error.getMessage());
    }

    @Test
    public void reviewShouldRejectNonCurrentApprover() {
        InMemoryWeeklyWorkMapper weeklyWorkMapper = new InMemoryWeeklyWorkMapper();
        StubPermissionService permissionService = new StubPermissionService(Set.of(), Set.of(1L, 2L, 3L));
        StubUserMapper userMapper = new StubUserMapper();
        userMapper.users.put(1L, user(1L, 2L, "/1/"));
        userMapper.users.put(2L, user(2L, 2L, "/1/2/"));
        userMapper.users.put(3L, user(3L, 2L, "/1/2/3/"));
        userMapper.users.get(2L).setParentUserId(1L);
        userMapper.users.get(3L).setParentUserId(2L);
        WeeklyWorkEntity entity = seedWeekly(2L, 3L, "2026-W11", "PENDING_SECTION_CHIEF");
        entity.setCurrentApprovalNode("USER_2");
        weeklyWorkMapper.insert(entity);
        WeeklyWorkServiceImpl service = new WeeklyWorkServiceImpl(weeklyWorkMapper, permissionService, userMapper);

        mockLoginUser(1L, false);
        ReviewWeeklyWorkRequest request = new ReviewWeeklyWorkRequest();
        request.setId(entity.getId());
        request.setAction("APPROVE");

        IllegalArgumentException error = Assert.assertThrows(IllegalArgumentException.class, () -> service.review(request));

        Assert.assertEquals("当前用户不是该周报的审批人", error.getMessage());
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
        user.setUsername("user" + id);
        user.setRealName("用户" + id);
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
        private final Set<Long> superAdminUserIds;
        private final Set<Long> scopeUserIds;

        private StubPermissionService(Set<Long> superAdminUserIds, Set<Long> scopeUserIds) {
            this.superAdminUserIds = superAdminUserIds;
            this.scopeUserIds = scopeUserIds;
        }

        @Override
        public boolean isSuperAdmin(Long userId) {
            return superAdminUserIds.contains(userId);
        }

        @Override
        public boolean isUnitAdmin(Long userId) {
            return false;
        }

        @Override
        public Set<Long> queryDataScopeUserIds(Long currentUserId) {
            return isSuperAdmin(currentUserId) ? Set.of() : scopeUserIds;
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

    private static class InMemoryWeeklyWorkMapper implements WeeklyWorkMapper {
        private final Map<Long, WeeklyWorkEntity> data = new LinkedHashMap<>();
        private final Map<Long, List<WeeklyWorkApprovalLogVO>> logs = new LinkedHashMap<>();
        private final Map<Long, String> userTreePaths = new LinkedHashMap<>();
        private boolean failMarkSubmitted = false;
        private boolean failUpdateApproval = false;
        private long sequence = 1L;
        private long logSequence = 1L;

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
            target.setCurrentApprovalNode(entity.getCurrentApprovalNode());
            target.setSubmitTime(null);
            return 1;
        }

        @Override
        public int markSubmitted(Long id,
                                 String status,
                                 String currentApprovalNode,
                                 Long currentHandlerUserId,
                                 String currentHandlerUserName,
                                 Integer currentFlowOrder,
                                 Long finalApproverUserId,
                                 LocalDateTime approvedTime,
                                 LocalDateTime submitTime) {
            if (failMarkSubmitted) {
                return 0;
            }
            WeeklyWorkEntity target = data.get(id);
            target.setStatus(status);
            target.setCurrentApprovalNode(currentApprovalNode);
            target.setCurrentHandlerUserId(currentHandlerUserId);
            target.setCurrentHandlerUserName(currentHandlerUserName);
            target.setCurrentFlowOrder(currentFlowOrder);
            target.setFinalApproverUserId(finalApproverUserId);
            target.setLastReturnTarget(null);
            target.setLastReturnComment(null);
            target.setLastReviewBy(null);
            target.setLastReviewByName(null);
            target.setLastReviewTime(null);
            target.setApprovedTime(approvedTime);
            target.setSubmitTime(submitTime);
            return 1;
        }

        @Override
        public int updateApproval(WeeklyWorkEntity entity) {
            if (failUpdateApproval) {
                return 0;
            }
            WeeklyWorkEntity target = data.get(entity.getId());
            target.setStatus(entity.getStatus());
            target.setCurrentApprovalNode(entity.getCurrentApprovalNode());
            target.setCurrentHandlerUserId(entity.getCurrentHandlerUserId());
            target.setCurrentHandlerUserName(entity.getCurrentHandlerUserName());
            target.setCurrentFlowOrder(entity.getCurrentFlowOrder());
            target.setFinalApproverUserId(entity.getFinalApproverUserId());
            target.setLastReturnTarget(entity.getLastReturnTarget());
            target.setLastReturnComment(entity.getLastReturnComment());
            target.setLastReviewBy(entity.getLastReviewBy());
            target.setLastReviewByName(entity.getLastReviewByName());
            target.setLastReviewTime(entity.getLastReviewTime());
            target.setApprovedTime(entity.getApprovedTime());
            return 1;
        }

        @Override
        public int insertApprovalLog(WeeklyWorkApprovalLogEntity entity) {
            WeeklyWorkApprovalLogVO item = new WeeklyWorkApprovalLogVO();
            item.setId(logSequence++);
            item.setWeeklyWorkId(entity.getWeeklyWorkId());
            item.setAction(entity.getAction());
            item.setFromNode(entity.getFromNode());
            item.setToNode(entity.getToNode());
            item.setReviewerUserId(entity.getReviewerUserId());
            item.setReviewerName(entity.getReviewerName());
            item.setComment(entity.getComment());
            item.setCreateTime(entity.getCreateTime() == null ? LocalDateTime.now() : entity.getCreateTime());
            logs.computeIfAbsent(entity.getWeeklyWorkId(), key -> new ArrayList<>()).add(0, item);
            return 1;
        }

        @Override
        public List<WeeklyWorkApprovalLogVO> queryApprovalLogs(Long weeklyWorkId) {
            return new ArrayList<>(logs.getOrDefault(weeklyWorkId, List.of()));
        }

        @Override
        public long countDistinctSubmittedUsers(String treePathPrefix, String weekNo, String status) {
            Set<Long> userIds = new java.util.LinkedHashSet<>();
            for (WeeklyWorkEntity entity : data.values()) {
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
                if (request.getUserId() != null && !request.getUserId().equals(entity.getUserId())) {
                    continue;
                }
                if (request.getWeekNo() != null && !request.getWeekNo().isEmpty() && !request.getWeekNo().equals(entity.getWeekNo())) {
                    continue;
                }
                if (request.getStatus() != null && !request.getStatus().isEmpty() && !request.getStatus().equals(entity.getStatus())) {
                    continue;
                }
                if (request.getTreePathPrefix() != null && !request.getTreePathPrefix().isBlank()) {
                    String treePath = userTreePaths.get(entity.getUserId());
                    if (treePath == null || !treePath.startsWith(request.getTreePathPrefix())) {
                        continue;
                    }
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
                item.setCurrentApprovalNode(entity.getCurrentApprovalNode());
                item.setCurrentHandlerUserId(entity.getCurrentHandlerUserId());
                item.setCurrentHandlerUserName(entity.getCurrentHandlerUserName());
                item.setCurrentFlowOrder(entity.getCurrentFlowOrder());
                item.setFinalApproverUserId(entity.getFinalApproverUserId());
                item.setLastReturnTarget(entity.getLastReturnTarget());
                item.setLastReturnComment(entity.getLastReturnComment());
                item.setLastReviewByName(entity.getLastReviewByName());
                item.setLastReviewTime(entity.getLastReviewTime());
                item.setSubmitTime(entity.getSubmitTime());
                item.setApprovedTime(entity.getApprovedTime());
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
            copy.setCurrentApprovalNode(entity.getCurrentApprovalNode());
            copy.setCurrentHandlerUserId(entity.getCurrentHandlerUserId());
            copy.setCurrentHandlerUserName(entity.getCurrentHandlerUserName());
            copy.setCurrentFlowOrder(entity.getCurrentFlowOrder());
            copy.setFinalApproverUserId(entity.getFinalApproverUserId());
            copy.setLastReturnTarget(entity.getLastReturnTarget());
            copy.setLastReturnComment(entity.getLastReturnComment());
            copy.setLastReviewBy(entity.getLastReviewBy());
            copy.setLastReviewByName(entity.getLastReviewByName());
            copy.setLastReviewTime(entity.getLastReviewTime());
            copy.setSubmitTime(entity.getSubmitTime());
            copy.setApprovedTime(entity.getApprovedTime());
            copy.setCreateTime(entity.getCreateTime());
            return copy;
        }
    }
}
