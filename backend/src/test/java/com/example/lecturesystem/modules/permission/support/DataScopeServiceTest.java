package com.example.lecturesystem.modules.permission.support;

import com.example.lecturesystem.modules.permission.mapper.PermissionMapper;
import com.example.lecturesystem.modules.user.entity.UserEntity;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class DataScopeServiceTest {
    @Test
    public void queryScopedUserIdsShouldReuseTreePathCache() {
        AtomicInteger invokeCount = new AtomicInteger();
        DataScopeService service = new DataScopeService(new PermissionMapper() {
            @Override
            public boolean existsUserRole(Long userId, String roleCode) {
                return false;
            }

            @Override
            public List<Long> queryUserIdsByTreePathPrefix(String treePathPrefix) {
                invokeCount.incrementAndGet();
                return List.of(3L, 4L);
            }

            @Override
            public int insertUserRole(Long userId, String roleCode) {
                return 0;
            }
        });

        UserEntity currentUser = new UserEntity();
        currentUser.setId(3L);
        currentUser.setRole("USER");
        currentUser.setTreePath("/3/");

        Assert.assertEquals(2, service.queryScopedUserIds(currentUser).size());
        Assert.assertEquals(2, service.queryScopedUserIds(currentUser).size());
        Assert.assertEquals(1, invokeCount.get());
    }

    @Test
    public void describeScopeShouldReturnReadableSummary() {
        DataScopeService service = new DataScopeService();
        UserEntity currentUser = new UserEntity();
        currentUser.setId(3L);
        currentUser.setRole("USER");
        currentUser.setTreePath("/3/");

        Assert.assertEquals(DataScopeType.SUB_TREE, service.resolveScopeType(currentUser));
        Assert.assertEquals("当前可查看自己及下级，共 2 人", service.describeScope(currentUser, 2L));
    }
}
