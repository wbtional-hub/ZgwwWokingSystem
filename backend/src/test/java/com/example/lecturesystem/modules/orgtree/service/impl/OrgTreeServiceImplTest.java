package com.example.lecturesystem.modules.orgtree.service.impl;

import com.example.lecturesystem.modules.auth.security.LoginUser;
import com.example.lecturesystem.modules.auth.security.JwtTokenService;
import com.example.lecturesystem.modules.auth.service.impl.AuthServiceImpl;
import com.example.lecturesystem.modules.auth.support.PasswordPolicyValidator;
import com.example.lecturesystem.modules.auth.support.Sm3PasswordCodec;
import com.example.lecturesystem.modules.orgtree.dto.CreateChildUserRequest;
import com.example.lecturesystem.modules.orgtree.dto.MoveNodeRequest;
import com.example.lecturesystem.modules.orgtree.dto.UpdateOrgNodeRequest;
import com.example.lecturesystem.modules.orgtree.entity.OrgNodeEntity;
import com.example.lecturesystem.modules.orgtree.mapper.OrgTreeMapper;
import com.example.lecturesystem.modules.orgtree.vo.OrgTreeNodeVO;
import com.example.lecturesystem.modules.permission.mapper.PermissionMapper;
import com.example.lecturesystem.modules.permission.service.PermissionService;
import com.example.lecturesystem.modules.user.entity.UserEntity;
import com.example.lecturesystem.modules.user.mapper.UserMapper;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class OrgTreeServiceImplTest {
    @After
    public void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void createChildShouldCalculateLevelAndTreePath() {
        OrgTreeMapper orgTreeMapper = mock(OrgTreeMapper.class);
        UserMapper userMapper = mock(UserMapper.class);
        PermissionService permissionService = mock(PermissionService.class);
        PermissionMapper permissionMapper = mock(PermissionMapper.class);
        OrgTreeServiceImpl service = new OrgTreeServiceImpl(orgTreeMapper, userMapper, permissionService, permissionMapper);
        Sm3PasswordCodec sm3PasswordCodec = new Sm3PasswordCodec();
        AtomicReference<UserEntity> createdUserRef = new AtomicReference<>();

        mockLoginUser(2L, false);
        when(orgTreeMapper.findByUserId(2L)).thenReturn(node(2L, 2L, null, 1, "/2"));
        when(userMapper.findById(2L)).thenReturn(user(2L, 2L, null, 1, "/2"));
        when(userMapper.findById(5L)).thenAnswer(invocation -> createdUserRef.get());
        when(userMapper.findByUsername("child1")).thenAnswer(invocation -> createdUserRef.get());
        doAnswer(invocation -> {
            UserEntity entity = invocation.getArgument(0);
            entity.setId(5L);
            createdUserRef.set(entity);
            return 1;
        }).when(userMapper).insertUser(any(UserEntity.class));

        CreateChildUserRequest request = new CreateChildUserRequest();
        request.setParentUserId(2L);
        request.setUsername("child1");
        request.setPassword("Admin2026");
        request.setRealName("下级用户");
        request.setJobTitle("讲师");
        request.setMobile("13800000005");
        request.setStatus(1);

        Long userId = service.createChild(request);

        Assert.assertEquals(Long.valueOf(5L), userId);
        Assert.assertEquals("SM3", createdUserRef.get().getPasswordAlgo());
        Assert.assertNotNull(createdUserRef.get().getPasswordSalt());
        Assert.assertTrue(sm3PasswordCodec.matches("Admin2026", createdUserRef.get().getPasswordSalt(), createdUserRef.get().getPasswordHash()));
        verify(orgTreeMapper).updateUserNodeInfo(5L, 2L, 2L, 2, "/2/5/");
        verify(permissionMapper).insertUserRole(5L, "ORG_USER");

        AuthServiceImpl authService = new AuthServiceImpl(
                userMapper,
                permissionMapper,
                new JwtTokenService("change-this-secret-in-production", 7200)
        );
        com.example.lecturesystem.modules.auth.dto.LoginRequest loginRequest = new com.example.lecturesystem.modules.auth.dto.LoginRequest();
        loginRequest.setUsername("child1");
        loginRequest.setPassword("Admin2026");
        Assert.assertNotNull(authService.login(loginRequest).getToken());
    }

    @Test
    public void createChildShouldAutoInitRootNodeForCurrentUser() {
        OrgTreeMapper orgTreeMapper = mock(OrgTreeMapper.class);
        UserMapper userMapper = mock(UserMapper.class);
        PermissionService permissionService = mock(PermissionService.class);
        PermissionMapper permissionMapper = mock(PermissionMapper.class);
        OrgTreeServiceImpl service = new OrgTreeServiceImpl(orgTreeMapper, userMapper, permissionService, permissionMapper);

        mockLoginUser(2L, false);
        when(orgTreeMapper.findByUserId(2L)).thenReturn(node(2L, 2L, null, null, null));
        when(userMapper.findById(2L)).thenReturn(user(2L, 2L, null, null, null));
        when(userMapper.findByUsername("child1")).thenReturn(null);
        doAnswer(invocation -> {
            UserEntity entity = invocation.getArgument(0);
            entity.setId(6L);
            return 1;
        }).when(userMapper).insertUser(any(UserEntity.class));

        CreateChildUserRequest request = new CreateChildUserRequest();
        request.setParentUserId(2L);
        request.setUsername("child1");
        request.setPassword("Admin2026");
        request.setRealName("一级用户");
        request.setStatus(1);

        Long userId = service.createChild(request);

        Assert.assertEquals(Long.valueOf(6L), userId);
        verify(orgTreeMapper).updateUserNodeInfo(2L, null, 2L, 1, "/2/");
        verify(orgTreeMapper).updateUserNodeInfo(6L, 2L, 2L, 2, "/2/6/");
    }

    @Test
    public void createChildShouldRejectDifferentUnitIdFromParent() {
        OrgTreeMapper orgTreeMapper = mock(OrgTreeMapper.class);
        UserMapper userMapper = mock(UserMapper.class);
        PermissionService permissionService = mock(PermissionService.class);
        PermissionMapper permissionMapper = mock(PermissionMapper.class);
        OrgTreeServiceImpl service = new OrgTreeServiceImpl(orgTreeMapper, userMapper, permissionService, permissionMapper);

        mockLoginUser(2L, false);
        when(orgTreeMapper.findByUserId(2L)).thenReturn(node(2L, 2L, null, 1, "/2/"));
        when(userMapper.findById(2L)).thenReturn(user(2L, 2L, null, 1, "/2/"));
        when(userMapper.findByUsername("child1")).thenReturn(null);

        CreateChildUserRequest request = new CreateChildUserRequest();
        request.setParentUserId(2L);
        request.setUsername("child1");
        request.setPassword("Admin2026");
        request.setRealName("下级用户");
        request.setUnitId(9L);
        request.setStatus(1);

        IllegalArgumentException error = Assert.assertThrows(IllegalArgumentException.class, () -> service.createChild(request));

        Assert.assertEquals("新增下级必须继承上级节点所属单位", error.getMessage());
    }

    @Test
    public void createChildShouldRejectWeakPassword() {
        OrgTreeMapper orgTreeMapper = mock(OrgTreeMapper.class);
        UserMapper userMapper = mock(UserMapper.class);
        PermissionService permissionService = mock(PermissionService.class);
        PermissionMapper permissionMapper = mock(PermissionMapper.class);
        OrgTreeServiceImpl service = new OrgTreeServiceImpl(orgTreeMapper, userMapper, permissionService, permissionMapper);

        mockLoginUser(2L, false);
        when(orgTreeMapper.findByUserId(2L)).thenReturn(node(2L, 2L, null, 1, "/2/"));
        when(userMapper.findById(2L)).thenReturn(user(2L, 2L, null, 1, "/2/"));
        when(userMapper.findByUsername("child1")).thenReturn(null);

        CreateChildUserRequest request = new CreateChildUserRequest();
        request.setParentUserId(2L);
        request.setUsername("child1");
        request.setPassword("admin123");
        request.setRealName("涓嬬骇鐢ㄦ埛");
        request.setStatus(1);

        IllegalArgumentException error = Assert.assertThrows(IllegalArgumentException.class, () -> service.createChild(request));

        Assert.assertEquals(PasswordPolicyValidator.MESSAGE, error.getMessage());
    }

    @Test
    public void queryTreeShouldReturnCurrentUserWithDescendants() {
        OrgTreeMapper orgTreeMapper = mock(OrgTreeMapper.class);
        UserMapper userMapper = mock(UserMapper.class);
        PermissionService permissionService = mock(PermissionService.class);
        PermissionMapper permissionMapper = mock(PermissionMapper.class);
        OrgTreeServiceImpl service = new OrgTreeServiceImpl(orgTreeMapper, userMapper, permissionService, permissionMapper);

        mockLoginUser(2L, false);
        when(userMapper.findById(2L)).thenReturn(user(2L, 2L, null, 1, "/2/"));
        when(orgTreeMapper.findByUserId(2L)).thenReturn(node(2L, 2L, null, 1, "/2/"));
        when(orgTreeMapper.querySubtreeNodes(2L, "/2/%")).thenReturn(List.of(
                treeNode(2L, null, 2L, 1, "/2/", "u2"),
                treeNode(3L, 2L, 2L, 2, "/2/3/", "u3"),
                treeNode(4L, 3L, 2L, 3, "/2/3/4/", "u4")
        ));

        List<?> result = (List<?>) service.queryTree();

        Assert.assertEquals(1, result.size());
        OrgTreeNodeVO root = (OrgTreeNodeVO) result.get(0);
        Assert.assertEquals(Long.valueOf(2L), root.getUserId());
        Assert.assertEquals(1, root.getChildren().size());
        Assert.assertEquals(Long.valueOf(3L), root.getChildren().get(0).getUserId());
    }

    @Test
    public void queryChildrenShouldReturnDirectChildren() {
        OrgTreeMapper orgTreeMapper = mock(OrgTreeMapper.class);
        UserMapper userMapper = mock(UserMapper.class);
        PermissionService permissionService = mock(PermissionService.class);
        PermissionMapper permissionMapper = mock(PermissionMapper.class);
        OrgTreeServiceImpl service = new OrgTreeServiceImpl(orgTreeMapper, userMapper, permissionService, permissionMapper);

        mockLoginUser(2L, false);
        when(userMapper.findById(2L)).thenReturn(user(2L, 2L, null, 1, "/2/"));
        when(orgTreeMapper.findByUserId(2L)).thenReturn(node(2L, 2L, null, 1, "/2/"));
        when(userMapper.findById(3L)).thenReturn(user(3L, 2L, 2L, 2, "/2/3/"));
        when(permissionService.isSuperAdmin(2L)).thenReturn(false);
        when(permissionService.queryDataScopeUserIds(2L)).thenReturn(Set.of(2L, 3L, 4L, 5L));
        when(orgTreeMapper.queryChildrenNodes(2L, 3L)).thenReturn(List.of(
                treeNode(4L, 3L, 2L, 3, "/2/3/4/", "u4"),
                treeNode(5L, 3L, 2L, 3, "/2/3/5/", "u5")
        ));

        List<?> result = (List<?>) service.queryChildren(3L);

        Assert.assertEquals(2, result.size());
        Assert.assertEquals(Long.valueOf(4L), ((OrgTreeNodeVO) result.get(0)).getUserId());
    }

    @Test
    public void queryAncestorsShouldReturnAncestorChain() {
        OrgTreeMapper orgTreeMapper = mock(OrgTreeMapper.class);
        UserMapper userMapper = mock(UserMapper.class);
        PermissionService permissionService = mock(PermissionService.class);
        PermissionMapper permissionMapper = mock(PermissionMapper.class);
        OrgTreeServiceImpl service = new OrgTreeServiceImpl(orgTreeMapper, userMapper, permissionService, permissionMapper);

        mockLoginUser(2L, false);
        when(userMapper.findById(2L)).thenReturn(user(2L, 2L, null, 1, "/2/"));
        when(orgTreeMapper.findByUserId(2L)).thenReturn(node(2L, 2L, null, 1, "/2/"));
        when(userMapper.findById(4L)).thenReturn(user(4L, 2L, 3L, 3, "/2/3/4/"));
        when(orgTreeMapper.findByUserId(4L)).thenReturn(node(4L, 2L, 3L, 3, "/2/3/4/"));
        when(permissionService.isSuperAdmin(2L)).thenReturn(false);
        when(permissionService.queryDataScopeUserIds(2L)).thenReturn(Set.of(2L, 3L, 4L));
        when(orgTreeMapper.queryAncestorNodes(2L, "/2/3/4/")).thenReturn(List.of(
                treeNode(2L, null, 2L, 1, "/2/", "u2"),
                treeNode(3L, 2L, 2L, 2, "/2/3/", "u3"),
                treeNode(4L, 3L, 2L, 3, "/2/3/4/", "u4")
        ));

        List<?> result = (List<?>) service.queryAncestors(4L);

        Assert.assertEquals(3, result.size());
        Assert.assertEquals(Long.valueOf(2L), ((OrgTreeNodeVO) result.get(0)).getUserId());
        Assert.assertEquals(Long.valueOf(4L), ((OrgTreeNodeVO) result.get(2)).getUserId());
    }

    @Test
    public void moveNodeShouldUpdateSubtreeAfterValidation() {
        OrgTreeMapper orgTreeMapper = mock(OrgTreeMapper.class);
        UserMapper userMapper = mock(UserMapper.class);
        PermissionService permissionService = mock(PermissionService.class);
        PermissionMapper permissionMapper = mock(PermissionMapper.class);
        OrgTreeServiceImpl service = new OrgTreeServiceImpl(orgTreeMapper, userMapper, permissionService, permissionMapper);

        mockLoginUser(2L, false);
        when(orgTreeMapper.findByUserId(3L)).thenReturn(node(3L, 2L, 2L, 2, "/2/3/"));
        when(orgTreeMapper.findByUserId(5L)).thenReturn(node(5L, 2L, 2L, 2, "/2/5/"));
        when(userMapper.findById(2L)).thenReturn(user(2L, 2L, null, 1, "/2/"));
        when(permissionService.isSuperAdmin(2L)).thenReturn(false);
        when(permissionService.queryDataScopeUserIds(2L)).thenReturn(Set.of(2L, 3L, 4L, 5L));

        MoveNodeRequest request = new MoveNodeRequest();
        request.setUserId(3L);
        request.setTargetParentUserId(5L);

        service.moveNode(request);

        verify(orgTreeMapper).updateSubtreeAfterMove(3L, "/2/3/", "/2/5/3/", 1, 5L);
    }

    @Test
    public void updateNodeShouldRejectUnitChange() {
        OrgTreeMapper orgTreeMapper = mock(OrgTreeMapper.class);
        UserMapper userMapper = mock(UserMapper.class);
        PermissionService permissionService = mock(PermissionService.class);
        PermissionMapper permissionMapper = mock(PermissionMapper.class);
        OrgTreeServiceImpl service = new OrgTreeServiceImpl(orgTreeMapper, userMapper, permissionService, permissionMapper);

        mockLoginUser(2L, false);
        when(userMapper.findById(2L)).thenReturn(user(2L, 2L, null, 1, "/2/"));
        when(userMapper.findById(3L)).thenReturn(user(3L, 2L, 2L, 2, "/2/3/"));
        when(orgTreeMapper.findByUserId(3L)).thenReturn(node(3L, 2L, 2L, 2, "/2/3/"));
        when(permissionService.isSuperAdmin(2L)).thenReturn(false);
        when(permissionService.queryDataScopeUserIds(2L)).thenReturn(Set.of(2L, 3L));

        UpdateOrgNodeRequest request = new UpdateOrgNodeRequest();
        request.setUserId(3L);
        request.setRealName("已改名");
        request.setUnitId(8L);

        IllegalArgumentException error = Assert.assertThrows(IllegalArgumentException.class, () -> service.updateNode(request));

        Assert.assertEquals("组织树节点单位不可在当前页面直接修改", error.getMessage());
    }

    private void mockLoginUser(Long userId, boolean superAdmin) {
        LoginUser loginUser = new LoginUser(userId, "tester", "测试用户", superAdmin);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, java.util.List.of())
        );
    }

    private UserEntity user(Long id, Long unitId, Long parentUserId, Integer levelNo, String treePath) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setUnitId(unitId);
        user.setParentUserId(parentUserId);
        user.setLevelNo(levelNo);
        user.setTreePath(treePath);
        user.setCreateTime(LocalDateTime.now());
        return user;
    }

    private OrgNodeEntity node(Long userId, Long unitId, Long parentUserId, Integer levelNo, String treePath) {
        OrgNodeEntity entity = new OrgNodeEntity();
        entity.setUserId(userId);
        entity.setUnitId(unitId);
        entity.setParentUserId(parentUserId);
        entity.setLevelNo(levelNo);
        entity.setTreePath(treePath);
        return entity;
    }

    private OrgTreeNodeVO treeNode(Long userId, Long parentUserId, Long unitId, Integer levelNo, String treePath, String username) {
        OrgTreeNodeVO node = new OrgTreeNodeVO();
        node.setUserId(userId);
        node.setParentUserId(parentUserId);
        node.setUnitId(unitId);
        node.setLevelNo(levelNo);
        node.setTreePath(treePath);
        node.setUsername(username);
        return node;
    }
}
