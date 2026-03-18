package com.example.lecturesystem.modules.unit.service.impl;

import com.example.lecturesystem.modules.auth.security.LoginUser;
import com.example.lecturesystem.modules.permission.service.PermissionService;
import com.example.lecturesystem.modules.unit.dto.CreateUnitRequest;
import com.example.lecturesystem.modules.unit.dto.SaveAttendanceLocationRequest;
import com.example.lecturesystem.modules.unit.entity.AttendanceLocationEntity;
import com.example.lecturesystem.modules.unit.entity.UnitEntity;
import com.example.lecturesystem.modules.unit.mapper.UnitMapper;
import com.example.lecturesystem.modules.unit.vo.AttendanceLocationVO;
import com.example.lecturesystem.modules.unit.vo.UnitListItemVO;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class UnitServiceImplTest {
    @After
    public void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void createUnitShouldOnlySaveUnitMainInfo() {
        InMemoryUnitMapper unitMapper = new InMemoryUnitMapper();
        StubPermissionService permissionService = new StubPermissionService(true);
        UnitServiceImpl service = new UnitServiceImpl(unitMapper, permissionService);
        mockLoginUser(1L, "admin");

        CreateUnitRequest request = new CreateUnitRequest();
        request.setUnitName("第二讲师团");
        request.setUnitCode("UNIT_002");
        request.setStatus(1);

        Long unitId = service.createUnit(request);

        Assert.assertNotNull(unitId);
        Assert.assertEquals(Long.valueOf(1L), unitId);
        Assert.assertEquals("UNIT_002", unitMapper.units.get(1L).getUnitCode());
        Assert.assertEquals(Long.valueOf(1L), unitMapper.units.get(1L).getAdminUserId());
    }

    @Test
    public void listUnitsShouldReturnCreatedUnits() {
        InMemoryUnitMapper unitMapper = new InMemoryUnitMapper();
        UnitEntity unit = new UnitEntity();
        unit.setId(1L);
        unit.setUnitName("平台管理单位");
        unit.setUnitCode("PLATFORM");
        unit.setStatus(1);
        unit.setCreateTime(LocalDateTime.now());
        unitMapper.units.put(1L, unit);

        UnitServiceImpl service = new UnitServiceImpl(unitMapper, new StubPermissionService(true));
        mockLoginUser(1L, "admin");

        List<?> list = (List<?>) service.listUnits();

        Assert.assertEquals(1, list.size());
        UnitListItemVO item = (UnitListItemVO) list.get(0);
        Assert.assertEquals("PLATFORM", item.getUnitCode());
        Assert.assertNull(item.getAdminUsername());
    }

    @Test
    public void createUnitShouldRejectNonSuperAdmin() {
        UnitServiceImpl service = new UnitServiceImpl(new InMemoryUnitMapper(), new StubPermissionService(false));
        mockLoginUser(2L, "user");

        CreateUnitRequest request = new CreateUnitRequest();
        request.setUnitName("无权限单位");
        request.setUnitCode("UNIT_X");
        request.setStatus(1);

        try {
            service.createUnit(request);
            Assert.fail("预期非超级管理员不能创建单位");
        } catch (IllegalArgumentException ex) {
            Assert.assertEquals("仅管理员可执行该操作", ex.getMessage());
        }
    }

    @Test
    public void saveAttendanceLocationShouldInsertWhenMissing() {
        InMemoryUnitMapper unitMapper = new InMemoryUnitMapper();
        UnitEntity unit = new UnitEntity();
        unit.setId(1L);
        unit.setUnitName("平台管理单位");
        unit.setUnitCode("PLATFORM");
        unit.setStatus(1);
        unitMapper.units.put(1L, unit);
        UnitServiceImpl service = new UnitServiceImpl(unitMapper, new StubPermissionService(true));
        mockLoginUser(1L, "admin");

        SaveAttendanceLocationRequest request = new SaveAttendanceLocationRequest();
        request.setUnitId(1L);
        request.setLocationName("主打卡点");
        request.setLatitude(new BigDecimal("39.909230"));
        request.setLongitude(new BigDecimal("116.397428"));
        request.setRadiusMeters(300);
        request.setAddress("北京市东城区");
        request.setStatus(1);

        Long id = service.saveAttendanceLocation(request);

        Assert.assertNotNull(id);
        Assert.assertEquals("主打卡点", unitMapper.attendanceLocations.get(1L).getLocationName());
    }

    @Test
    public void queryAttendanceLocationShouldReturnSavedLocation() {
        InMemoryUnitMapper unitMapper = new InMemoryUnitMapper();
        UnitEntity unit = new UnitEntity();
        unit.setId(1L);
        unit.setUnitName("平台管理单位");
        unit.setUnitCode("PLATFORM");
        unit.setStatus(1);
        unitMapper.units.put(1L, unit);
        AttendanceLocationEntity entity = new AttendanceLocationEntity();
        entity.setId(11L);
        entity.setUnitId(1L);
        entity.setLocationName("主打卡点");
        entity.setLatitude(new BigDecimal("39.909230"));
        entity.setLongitude(new BigDecimal("116.397428"));
        entity.setRadiusMeters(200);
        entity.setAddress("北京市东城区");
        entity.setStatus(1);
        unitMapper.attendanceLocations.put(1L, entity);
        UnitServiceImpl service = new UnitServiceImpl(unitMapper, new StubPermissionService(true));
        mockLoginUser(1L, "admin");

        AttendanceLocationVO location = (AttendanceLocationVO) service.queryAttendanceLocation(1L);

        Assert.assertNotNull(location);
        Assert.assertEquals("主打卡点", location.getLocationName());
    }

    @Test
    public void listUnitsShouldExposeAttendanceLocationSummary() {
        InMemoryUnitMapper unitMapper = new InMemoryUnitMapper();
        UnitEntity unit = new UnitEntity();
        unit.setId(1L);
        unit.setUnitName("平台管理单位");
        unit.setUnitCode("PLATFORM");
        unit.setStatus(1);
        unit.setCreateTime(LocalDateTime.now());
        unitMapper.units.put(1L, unit);

        AttendanceLocationEntity entity = new AttendanceLocationEntity();
        entity.setId(11L);
        entity.setUnitId(1L);
        entity.setLocationName("主打卡点");
        entity.setLatitude(new BigDecimal("39.909230"));
        entity.setLongitude(new BigDecimal("116.397428"));
        entity.setRadiusMeters(250);
        entity.setAddress("北京市东城区东华门街道");
        entity.setStatus(1);
        unitMapper.attendanceLocations.put(1L, entity);

        UnitServiceImpl service = new UnitServiceImpl(unitMapper, new StubPermissionService(true));
        mockLoginUser(1L, "admin");

        List<?> list = (List<?>) service.listUnits();

        Assert.assertEquals(1, list.size());
        UnitListItemVO item = (UnitListItemVO) list.get(0);
        Assert.assertEquals("主打卡点", item.getAttendanceLocationName());
        Assert.assertEquals(new BigDecimal("116.397428"), item.getAttendanceLocationLongitude());
        Assert.assertEquals(new BigDecimal("39.909230"), item.getAttendanceLocationLatitude());
        Assert.assertEquals(Integer.valueOf(250), item.getAttendanceLocationRadiusMeters());
    }

    private void mockLoginUser(Long userId, String username) {
        LoginUser loginUser = new LoginUser(userId, username, "测试用户", true);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, List.of())
        );
    }

    private static class StubPermissionService implements PermissionService {
        private final boolean superAdmin;

        private StubPermissionService(boolean superAdmin) {
            this.superAdmin = superAdmin;
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
            return Set.of();
        }
    }

    private static class InMemoryUnitMapper implements UnitMapper {
        private final Map<Long, UnitEntity> units = new LinkedHashMap<>();
        private final Map<Long, AttendanceLocationEntity> attendanceLocations = new LinkedHashMap<>();
        private long sequence = 1L;
        private long locationSequence = 100L;

        @Override
        public UnitEntity findById(Long id) {
            return units.get(id);
        }

        @Override
        public UnitEntity findByUnitCode(String unitCode) {
            for (UnitEntity unit : units.values()) {
                if (unitCode.equals(unit.getUnitCode())) {
                    return unit;
                }
            }
            return null;
        }

        @Override
        public int insertUnit(UnitEntity entity) {
            entity.setId(sequence++);
            units.put(entity.getId(), entity);
            return 1;
        }

        @Override
        public int insertLegacyUnit(UnitEntity entity) {
            return 1;
        }

        @Override
        public List<UnitListItemVO> queryUnitList() {
            List<UnitListItemVO> result = new ArrayList<>();
            for (UnitEntity unit : units.values()) {
                UnitListItemVO item = new UnitListItemVO();
                item.setId(unit.getId());
                item.setUnitName(unit.getUnitName());
                item.setUnitCode(unit.getUnitCode());
                item.setStatus(unit.getStatus());
                item.setAdminUserId(unit.getAdminUserId());
                item.setCreateTime(unit.getCreateTime());
                AttendanceLocationEntity location = attendanceLocations.get(unit.getId());
                if (location != null) {
                    item.setAttendanceLocationId(location.getId());
                    item.setAttendanceLocationName(location.getLocationName());
                    item.setAttendanceLocationAddress(location.getAddress());
                    item.setAttendanceLocationLongitude(location.getLongitude());
                    item.setAttendanceLocationLatitude(location.getLatitude());
                    item.setAttendanceLocationRadiusMeters(location.getRadiusMeters());
                    item.setAttendanceLocationStatus(location.getStatus());
                }
                result.add(item);
            }
            return result;
        }

        @Override
        public List<UnitListItemVO> queryUnitListByIds(Set<Long> unitIds) {
            return queryUnitList().stream().filter(item -> unitIds.contains(item.getId())).toList();
        }

        @Override
        public int updateUnit(UnitEntity entity) {
            units.put(entity.getId(), entity);
            return 1;
        }

        @Override
        public int updateLegacyUnit(UnitEntity entity) {
            return 1;
        }

        @Override
        public int deleteUnit(Long id) {
            units.remove(id);
            return 1;
        }

        @Override
        public int deleteLegacyUnit(Long id) {
            return 1;
        }

        @Override
        public int countOrgNodesByUnitId(Long id) {
            return 0;
        }

        @Override
        public int countUsersByUnitId(Long id) {
            return 0;
        }

        @Override
        public AttendanceLocationVO findAttendanceLocationByUnitId(Long unitId) {
            AttendanceLocationEntity entity = attendanceLocations.get(unitId);
            if (entity == null) {
                return null;
            }
            AttendanceLocationVO vo = new AttendanceLocationVO();
            vo.setId(entity.getId());
            vo.setUnitId(entity.getUnitId());
            vo.setLocationName(entity.getLocationName());
            vo.setLatitude(entity.getLatitude());
            vo.setLongitude(entity.getLongitude());
            vo.setRadiusMeters(entity.getRadiusMeters());
            vo.setAddress(entity.getAddress());
            vo.setStatus(entity.getStatus());
            vo.setCreateTime(entity.getCreateTime());
            vo.setUpdateTime(entity.getUpdateTime());
            return vo;
        }

        @Override
        public int insertAttendanceLocation(AttendanceLocationEntity entity) {
            entity.setId(locationSequence++);
            attendanceLocations.put(entity.getUnitId(), entity);
            return 1;
        }

        @Override
        public int updateAttendanceLocation(AttendanceLocationEntity entity) {
            AttendanceLocationEntity existing = attendanceLocations.get(entity.getUnitId());
            if (existing != null) {
                entity.setCreateTime(existing.getCreateTime());
            }
            attendanceLocations.put(entity.getUnitId(), entity);
            return 1;
        }
    }
}
