package com.example.lecturesystem.modules.user.service.impl;

import com.example.lecturesystem.modules.auth.security.LoginUser;
import com.example.lecturesystem.modules.operationlog.dto.OperationLogQueryRequest;
import com.example.lecturesystem.modules.operationlog.service.OperationLogService;
import com.example.lecturesystem.modules.permission.support.DataScopeService;
import com.example.lecturesystem.modules.user.dto.CreateUserRequest;
import com.example.lecturesystem.modules.user.dto.UpdateUserRequest;
import com.example.lecturesystem.modules.user.dto.UserQueryRequest;
import com.example.lecturesystem.modules.user.entity.UserEntity;
import com.example.lecturesystem.modules.user.mapper.UserMapper;
import com.example.lecturesystem.modules.user.vo.UserDetailVO;
import com.example.lecturesystem.modules.user.vo.UserListItemVO;
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

public class UserServiceImplTest {
    @After
    public void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void createUserShouldInsertNewUser() {
        InMemoryUserMapper userMapper = new InMemoryUserMapper();
        UserServiceImpl service = new UserServiceImpl(userMapper);
        mockLoginUser(1L, "admin");

        CreateUserRequest request = new CreateUserRequest();
        request.setUnitId(2L);
        request.setUsername("zhangsan");
        request.setPassword("123456");
        request.setRealName("张三");
        request.setJobTitle("讲师");
        request.setMobile("13800000000");
        request.setStatus(1);

        Long userId = service.createUser(request);

        Assert.assertNotNull(userId);
        UserEntity saved = userMapper.findById(userId);
        Assert.assertEquals("zhangsan", saved.getUsername());
        Assert.assertEquals("张三", saved.getRealName());
        Assert.assertEquals(Boolean.FALSE, saved.getIsDeleted());
    }

    @Test
    public void queryPageShouldFilterDeletedUsers() {
        InMemoryUserMapper userMapper = new InMemoryUserMapper();
        userMapper.insertSeed(seedUser(1L, "lisi", "李四", 1, false));
        userMapper.insertSeed(seedUser(2L, "wangwu", "王五", 1, true));
        UserServiceImpl service = new UserServiceImpl(userMapper);
        mockLoginUser(1L, "admin");

        UserQueryRequest request = new UserQueryRequest();
        Object page = service.queryPage(request);

        Assert.assertEquals(1L, ((com.example.lecturesystem.modules.user.vo.UserPageVO) page).getTotal().longValue());
    }

    @Test
    public void deleteUserShouldMarkDeleted() {
        InMemoryUserMapper userMapper = new InMemoryUserMapper();
        userMapper.insertSeed(seedUser(1L, "lisi", "李四", 1, false));
        UserServiceImpl service = new UserServiceImpl(userMapper);
        mockLoginUser(1L, "admin");

        service.deleteUser(1L);

        Assert.assertTrue(userMapper.deletedUserIds.contains(1L));
    }

    @Test
    public void updateUserShouldChangeBasicInfo() {
        InMemoryUserMapper userMapper = new InMemoryUserMapper();
        userMapper.insertSeed(seedUser(1L, "lisi", "李四", 1, false));
        UserServiceImpl service = new UserServiceImpl(userMapper);
        mockLoginUser(1L, "admin");

        UpdateUserRequest request = new UpdateUserRequest();
        request.setId(1L);
        request.setRealName("李四新");
        request.setJobTitle("高级讲师");
        request.setMobile("13900000000");
        request.setStatus(0);
        service.updateUser(request);

        UserEntity updated = userMapper.findById(1L);
        Assert.assertEquals("李四新", updated.getRealName());
        Assert.assertEquals("高级讲师", updated.getJobTitle());
        Assert.assertEquals(Integer.valueOf(0), updated.getStatus());
    }

    @Test
    public void detailShouldContainOrgTreeFields() {
        InMemoryUserMapper userMapper = new InMemoryUserMapper();
        UserEntity seeded = seedUser(1L, "lisi", "李四", 1, false);
        seeded.setParentUserId(9L);
        seeded.setLevelNo(3);
        seeded.setTreePath("/2/9/1/");
        userMapper.insertSeed(seeded);
        UserServiceImpl service = new UserServiceImpl(userMapper);
        mockLoginUser(1L, "admin");

        UserDetailVO detail = (UserDetailVO) service.detail(1L);

        Assert.assertEquals(Long.valueOf(9L), detail.getParentUserId());
        Assert.assertEquals(Integer.valueOf(3), detail.getLevelNo());
        Assert.assertEquals("/2/9/1/", detail.getTreePath());
    }

    @Test
    public void queryPageShouldReturnCurrentUserAndDescendantsForNonAdmin() {
        InMemoryUserMapper userMapper = new InMemoryUserMapper();
        UserEntity root = seedUser(2L, "root", "根用户", 1, false);
        root.setTreePath("/2/");
        root.setLevelNo(1);
        UserEntity child = seedUser(3L, "child", "下级用户", 1, false);
        child.setParentUserId(2L);
        child.setTreePath("/2/3/");
        child.setLevelNo(2);
        UserEntity other = seedUser(9L, "other", "其他用户", 1, false);
        other.setTreePath("/9/");
        other.setLevelNo(1);
        userMapper.insertSeed(root);
        userMapper.insertSeed(child);
        userMapper.insertSeed(other);
        UserServiceImpl service = new UserServiceImpl(userMapper);
        mockNormalLoginUser(2L, "root");

        UserQueryRequest request = new UserQueryRequest();

        com.example.lecturesystem.modules.user.vo.UserPageVO page = (com.example.lecturesystem.modules.user.vo.UserPageVO) service.queryPage(request);

        Assert.assertEquals(2L, page.getTotal().longValue());
        Assert.assertEquals(2, page.getList().size());
        Assert.assertEquals(Long.valueOf(2L), page.getScopeUserCount());
        Assert.assertEquals("SUB_TREE", page.getScopeType());
        Assert.assertEquals("当前可查看自己及下级，共 2 人", page.getScopeDescription());
    }

    @Test
    public void detailShouldAllowDescendantForNonAdmin() {
        InMemoryUserMapper userMapper = new InMemoryUserMapper();
        UserEntity root = seedUser(2L, "root", "根用户", 1, false);
        root.setTreePath("/2/");
        UserEntity child = seedUser(3L, "child", "下级用户", 1, false);
        child.setParentUserId(2L);
        child.setTreePath("/2/3/");
        userMapper.insertSeed(root);
        userMapper.insertSeed(child);
        UserServiceImpl service = new UserServiceImpl(userMapper);
        mockNormalLoginUser(2L, "root");

        UserDetailVO detail = (UserDetailVO) service.detail(3L);

        Assert.assertEquals(Long.valueOf(3L), detail.getId());
    }

    @Test
    public void detailShouldWriteDataAccessLog() {
        InMemoryUserMapper userMapper = new InMemoryUserMapper();
        UserEntity seeded = seedUser(1L, "lisi", "李四", 1, false);
        userMapper.insertSeed(seeded);
        StubOperationLogService operationLogService = new StubOperationLogService();
        UserServiceImpl service = new UserServiceImpl(userMapper, operationLogService, new DataScopeService());
        mockLoginUser(1L, "admin");

        service.detail(1L);

        Assert.assertEquals("DATA_ACCESS", operationLogService.moduleName);
        Assert.assertEquals("VIEW_USER_DETAIL", operationLogService.actionName);
        Assert.assertEquals(Long.valueOf(1L), operationLogService.bizId);
        Assert.assertTrue(operationLogService.content.contains("查看了用户"));
    }

    private void mockLoginUser(Long userId, String username) {
        LoginUser loginUser = new LoginUser(userId, username, "管理员", true);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, List.of())
        );
    }

    private void mockNormalLoginUser(Long userId, String username) {
        LoginUser loginUser = new LoginUser(userId, username, "普通用户", false);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, List.of())
        );
    }

    private UserEntity seedUser(Long id, String username, String realName, Integer status, boolean deleted) {
        UserEntity entity = new UserEntity();
        entity.setId(id);
        entity.setUnitId(2L);
        entity.setUsername(username);
        entity.setPasswordHash("hashed");
        entity.setRealName(realName);
        entity.setStatus(status);
        entity.setIsDeleted(deleted);
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        entity.setCreateUser("seed");
        entity.setUpdateUser("seed");
        return entity;
    }

    private static class InMemoryUserMapper implements UserMapper {
        private final Map<Long, UserEntity> users = new LinkedHashMap<>();
        private final List<Long> deletedUserIds = new ArrayList<>();
        private long sequence = 10L;

        void insertSeed(UserEntity entity) {
            users.put(entity.getId(), cloneUser(entity));
            if (entity.getId() >= sequence) {
                sequence = entity.getId() + 1;
            }
        }

        @Override
        public UserEntity findById(Long id) {
            UserEntity entity = users.get(id);
            if (entity == null || Boolean.TRUE.equals(entity.getIsDeleted())) {
                return null;
            }
            return cloneUser(entity);
        }

        @Override
        public UserEntity findByUsername(String username) {
            for (UserEntity entity : users.values()) {
                if (username.equals(entity.getUsername()) && !Boolean.TRUE.equals(entity.getIsDeleted())) {
                    return cloneUser(entity);
                }
            }
            return null;
        }

        @Override
        public int insertUser(UserEntity entity) {
            entity.setId(sequence++);
            users.put(entity.getId(), cloneUser(entity));
            return 1;
        }

        @Override
        public long countPageByUserId(Long userId, UserQueryRequest request) {
            UserEntity entity = users.get(userId);
            return entity == null || Boolean.TRUE.equals(entity.getIsDeleted()) ? 0 : 1;
        }

        @Override
        public List<UserListItemVO> queryPageByUserId(Long userId, UserQueryRequest request) {
            UserEntity entity = users.get(userId);
            if (entity == null || Boolean.TRUE.equals(entity.getIsDeleted())) {
                return List.of();
            }
            return queryPage(request).stream().filter(item -> userId.equals(item.getId())).toList();
        }

        @Override
        public long countPageByTreePath(String treePathPrefix, UserQueryRequest request) {
            return queryPageByTreePath(treePathPrefix, request).size();
        }

        @Override
        public List<UserListItemVO> queryPageByTreePath(String treePathPrefix, UserQueryRequest request) {
            return queryPage(request).stream()
                    .filter(item -> {
                        UserEntity entity = users.get(item.getId());
                        return entity != null
                                && entity.getTreePath() != null
                                && entity.getTreePath().startsWith(treePathPrefix);
                    })
                    .toList();
        }

        @Override
        public long countPage(UserQueryRequest request) {
            return queryPage(request).size();
        }

        @Override
        public List<UserListItemVO> queryPage(UserQueryRequest request) {
            List<UserListItemVO> result = new ArrayList<>();
            for (UserEntity entity : users.values()) {
                if (Boolean.TRUE.equals(entity.getIsDeleted())) {
                    continue;
                }
                if (request.getStatus() != null && !request.getStatus().equals(entity.getStatus())) {
                    continue;
                }
                if (request.getKeywords() != null && !request.getKeywords().isBlank()) {
                    String keywords = request.getKeywords();
                    boolean matched = entity.getUsername().contains(keywords)
                            || entity.getRealName().contains(keywords)
                            || (entity.getMobile() != null && entity.getMobile().contains(keywords));
                    if (!matched) {
                        continue;
                    }
                }
                UserListItemVO item = new UserListItemVO();
                item.setId(entity.getId());
                item.setUnitId(entity.getUnitId());
                item.setUsername(entity.getUsername());
                item.setRealName(entity.getRealName());
                item.setJobTitle(entity.getJobTitle());
                item.setMobile(entity.getMobile());
                item.setStatus(entity.getStatus());
                item.setParentUserId(entity.getParentUserId());
                item.setLevelNo(entity.getLevelNo());
                item.setCreateTime(entity.getCreateTime());
                item.setUpdateTime(entity.getUpdateTime());
                result.add(item);
            }
            return result;
        }

        @Override
        public UserDetailVO detailById(Long id) {
            UserEntity entity = findById(id);
            if (entity == null) {
                return null;
            }
            UserDetailVO detail = new UserDetailVO();
            detail.setId(entity.getId());
            detail.setUnitId(entity.getUnitId());
            detail.setParentUserId(entity.getParentUserId());
            detail.setLevelNo(entity.getLevelNo());
            detail.setTreePath(entity.getTreePath());
            detail.setUsername(entity.getUsername());
            detail.setRealName(entity.getRealName());
            detail.setJobTitle(entity.getJobTitle());
            detail.setMobile(entity.getMobile());
            detail.setStatus(entity.getStatus());
            detail.setCreateTime(entity.getCreateTime());
            detail.setUpdateTime(entity.getUpdateTime());
            return detail;
        }

        @Override
        public UserDetailVO detailByIdAndTreePath(Long id, String treePathPrefix) {
            UserEntity entity = findById(id);
            if (entity == null || entity.getTreePath() == null || !entity.getTreePath().startsWith(treePathPrefix)) {
                return null;
            }
            return detailById(id);
        }

        @Override
        public int updateUser(UserEntity entity) {
            UserEntity target = users.get(entity.getId());
            target.setRealName(entity.getRealName());
            target.setJobTitle(entity.getJobTitle());
            target.setMobile(entity.getMobile());
            target.setStatus(entity.getStatus());
            target.setUpdateTime(entity.getUpdateTime());
            target.setUpdateUser(entity.getUpdateUser());
            return 1;
        }

        @Override
        public int logicalDelete(Long id, String updateUser, LocalDateTime updateTime) {
            UserEntity target = users.get(id);
            target.setIsDeleted(Boolean.TRUE);
            target.setUpdateUser(updateUser);
            target.setUpdateTime(updateTime);
            deletedUserIds.add(id);
            return 1;
        }

        @Override
        public int updatePassword(Long id, String passwordHash, String updateUser, LocalDateTime updateTime) {
            UserEntity target = users.get(id);
            target.setPasswordHash(passwordHash);
            target.setUpdateUser(updateUser);
            target.setUpdateTime(updateTime);
            return 1;
        }

        private UserEntity cloneUser(UserEntity source) {
            UserEntity target = new UserEntity();
            target.setId(source.getId());
            target.setUnitId(source.getUnitId());
            target.setParentUserId(source.getParentUserId());
            target.setLevelNo(source.getLevelNo());
            target.setTreePath(source.getTreePath());
            target.setUsername(source.getUsername());
            target.setPasswordHash(source.getPasswordHash());
            target.setRealName(source.getRealName());
            target.setJobTitle(source.getJobTitle());
            target.setMobile(source.getMobile());
            target.setStatus(source.getStatus());
            target.setCreateTime(source.getCreateTime());
            target.setUpdateTime(source.getUpdateTime());
            target.setCreateUser(source.getCreateUser());
            target.setUpdateUser(source.getUpdateUser());
            target.setIsDeleted(source.getIsDeleted());
            return target;
        }
    }

    private static class StubOperationLogService implements OperationLogService {
        private String moduleName;
        private String actionName;
        private Long bizId;
        private String content;

        @Override
        public void log(String moduleName, String actionName, Long bizId, String content) {
            this.moduleName = moduleName;
            this.actionName = actionName;
            this.bizId = bizId;
            this.content = content;
        }

        @Override
        public Object query(OperationLogQueryRequest request) {
            return List.of();
        }
    }
}
