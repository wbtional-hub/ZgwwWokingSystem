package com.example.lecturesystem.modules.unit.service.impl;

import com.example.lecturesystem.modules.auth.security.LoginUser;
import com.example.lecturesystem.modules.permission.service.PermissionService;
import com.example.lecturesystem.modules.unit.dto.CreateUnitRequest;
import com.example.lecturesystem.modules.unit.dto.SaveAttendanceLocationRequest;
import com.example.lecturesystem.modules.unit.dto.ToggleUnitStatusRequest;
import com.example.lecturesystem.modules.unit.dto.UpdateUnitRequest;
import com.example.lecturesystem.modules.unit.entity.AttendanceLocationEntity;
import com.example.lecturesystem.modules.unit.entity.UnitEntity;
import com.example.lecturesystem.modules.unit.mapper.UnitMapper;
import com.example.lecturesystem.modules.unit.service.UnitService;
import com.example.lecturesystem.modules.user.entity.UserEntity;
import com.example.lecturesystem.modules.user.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;

@Service
public class UnitServiceImpl implements UnitService {
    private final UnitMapper unitMapper;
    private final PermissionService permissionService;
    private final UserMapper userMapper;

    public UnitServiceImpl(UnitMapper unitMapper,
                           PermissionService permissionService) {
        this(unitMapper, permissionService, new UserMapper() {
            @Override
            public com.example.lecturesystem.modules.user.entity.UserEntity findById(Long id) {
                return null;
            }

            @Override
            public com.example.lecturesystem.modules.user.entity.UserEntity findByUsername(String username) {
                return null;
            }

            @Override
            public com.example.lecturesystem.modules.user.entity.UserEntity findByWechatOpenId(String wechatOpenId) {
                return null;
            }

            @Override
            public com.example.lecturesystem.modules.user.entity.UserEntity findByWechatUnionId(String wechatUnionId) {
                return null;
            }

            @Override
            public long countPageByUserId(Long userId, com.example.lecturesystem.modules.user.dto.UserQueryRequest request) {
                return 0;
            }

            @Override
            public java.util.List<com.example.lecturesystem.modules.user.vo.UserListItemVO> queryPageByUserId(Long userId, com.example.lecturesystem.modules.user.dto.UserQueryRequest request) {
                return java.util.List.of();
            }

            @Override
            public long countPageByTreePath(String treePathPrefix, com.example.lecturesystem.modules.user.dto.UserQueryRequest request) {
                return 0;
            }

            @Override
            public java.util.List<com.example.lecturesystem.modules.user.vo.UserListItemVO> queryPageByTreePath(String treePathPrefix, com.example.lecturesystem.modules.user.dto.UserQueryRequest request) {
                return java.util.List.of();
            }

            @Override
            public int insertUser(com.example.lecturesystem.modules.user.entity.UserEntity entity) {
                return 0;
            }

            @Override
            public long countPage(com.example.lecturesystem.modules.user.dto.UserQueryRequest request) {
                return 0;
            }

            @Override
            public java.util.List<com.example.lecturesystem.modules.user.vo.UserListItemVO> queryPage(com.example.lecturesystem.modules.user.dto.UserQueryRequest request) {
                return java.util.List.of();
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
            public int updateUser(com.example.lecturesystem.modules.user.entity.UserEntity entity) {
                return 0;
            }

            @Override
            public int updateWechatBinding(Long id, String wechatOpenId, String wechatUnionId, String updateUser, java.time.LocalDateTime updateTime) {
                return 0;
            }

            @Override
            public int logicalDelete(Long id, String updateUser, java.time.LocalDateTime updateTime) {
                return 0;
            }

            @Override
            public int updatePassword(Long id, String passwordHash, String passwordAlgo, String passwordSalt, Boolean forcePasswordChange, String updateUser, java.time.LocalDateTime updateTime) {
                return 0;
            }

            @Override
            public int updateLoginSecurityState(Long id, Integer loginFailCount, java.time.LocalDateTime lockUntil, String updateUser, java.time.LocalDateTime updateTime) {
                return 0;
            }

        });
    }

    @Autowired
    public UnitServiceImpl(UnitMapper unitMapper,
                           PermissionService permissionService,
                           UserMapper userMapper) {
        this.unitMapper = unitMapper;
        this.permissionService = permissionService;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional
    public Long createUnit(CreateUnitRequest request) {
        LoginUser loginUser = currentLoginUser();
        validateSuperAdmin(loginUser.getUserId());

        String unitName = request.getUnitName().trim();
        String unitCode = request.getUnitCode().trim();
        if (unitMapper.findByUnitCode(unitCode) != null) {
            throw new IllegalArgumentException("单位编码已存在");
        }

        UnitEntity unit = new UnitEntity();
        unit.setUnitName(unitName);
        unit.setUnitCode(unitCode);
        unit.setStatus(request.getStatus());
        unit.setAdminUserId(loginUser.getUserId());
        unit.setCreateTime(LocalDateTime.now());
        unitMapper.insertUnit(unit);
        unitMapper.insertLegacyUnit(unit);
        return unit.getId();
    }

    @Override
    public Object listUnits() {
        LoginUser loginUser = currentLoginUser();
        if (permissionService.isSuperAdmin(loginUser.getUserId())) {
            return unitMapper.queryUnitList();
        }
        UserEntity currentUser = userMapper.findById(loginUser.getUserId());
        if (currentUser == null || currentUser.getUnitId() == null) {
            throw new IllegalArgumentException("当前用户未绑定单位");
        }
        return unitMapper.queryUnitListByIds(Set.of(currentUser.getUnitId()));
    }

    @Override
    @Transactional
    public void updateUnit(UpdateUnitRequest request) {
        LoginUser loginUser = currentLoginUser();
        validateSuperAdmin(loginUser.getUserId());

        UnitEntity existing = requireUnit(request.getId());
        String unitCode = request.getUnitCode().trim();
        UnitEntity sameCodeUnit = unitMapper.findByUnitCode(unitCode);
        if (sameCodeUnit != null && !sameCodeUnit.getId().equals(request.getId())) {
            throw new IllegalArgumentException("单位编码已存在");
        }

        existing.setUnitName(request.getUnitName().trim());
        existing.setUnitCode(unitCode);
        existing.setStatus(request.getStatus());
        existing.setAdminUserId(loginUser.getUserId());
        unitMapper.updateUnit(existing);
        unitMapper.updateLegacyUnit(existing);
    }

    @Override
    @Transactional
    public void deleteUnit(Long id) {
        LoginUser loginUser = currentLoginUser();
        validateSuperAdmin(loginUser.getUserId());

        requireUnit(id);
        if (unitMapper.countOrgNodesByUnitId(id) > 0) {
            throw new IllegalArgumentException("当前单位下仍有关联组织，暂不可删除");
        }
        if (unitMapper.countUsersByUnitId(id) > 0) {
            throw new IllegalArgumentException("当前单位下仍有关联用户，暂不可删除");
        }
        unitMapper.deleteUnit(id);
        unitMapper.deleteLegacyUnit(id);
    }

    @Override
    @Transactional
    public void toggleUnitStatus(ToggleUnitStatusRequest request) {
        LoginUser loginUser = currentLoginUser();
        validateSuperAdmin(loginUser.getUserId());

        UnitEntity existing = requireUnit(request.getId());
        existing.setStatus(request.getStatus());
        existing.setAdminUserId(loginUser.getUserId());
        unitMapper.updateUnit(existing);
        unitMapper.updateLegacyUnit(existing);
    }

    @Override
    public Object queryAttendanceLocation(Long unitId) {
        LoginUser loginUser = currentLoginUser();
        validateReadableUnit(loginUser.getUserId(), unitId);
        requireUnit(unitId);
        return unitMapper.findAttendanceLocationByUnitId(unitId);
    }

    @Override
    @Transactional
    public Long saveAttendanceLocation(SaveAttendanceLocationRequest request) {
        LoginUser loginUser = currentLoginUser();
        validateSuperAdmin(loginUser.getUserId());
        requireUnit(request.getUnitId());

        String locationName = request.getLocationName() == null ? "" : request.getLocationName().trim();
        if (locationName.isEmpty()) {
            throw new IllegalArgumentException("打卡点名称不能为空");
        }
        if (request.getLatitude() == null || request.getLongitude() == null) {
            throw new IllegalArgumentException("请先选择打卡点坐标");
        }

        com.example.lecturesystem.modules.unit.vo.AttendanceLocationVO existing = unitMapper.findAttendanceLocationByUnitId(request.getUnitId());
        AttendanceLocationEntity entity = new AttendanceLocationEntity();
        entity.setUnitId(request.getUnitId());
        entity.setLocationName(locationName);
        entity.setLatitude(request.getLatitude());
        entity.setLongitude(request.getLongitude());
        entity.setRadiusMeters(request.getRadiusMeters() == null ? 100 : request.getRadiusMeters());
        entity.setAddress(request.getAddress() == null ? null : request.getAddress().trim());
        entity.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        entity.setUpdateTime(LocalDateTime.now());
        if (existing == null) {
            entity.setCreateTime(entity.getUpdateTime());
            unitMapper.insertAttendanceLocation(entity);
            return entity.getId();
        }
        entity.setId(existing.getId());
        unitMapper.updateAttendanceLocation(entity);
        return existing.getId();
    }

    private void validateSuperAdmin(Long userId) {
        if (!permissionService.isSuperAdmin(userId)) {
            throw new IllegalArgumentException("仅管理员可执行该操作");
        }
    }

    private UnitEntity requireUnit(Long id) {
        UnitEntity unit = unitMapper.findById(id);
        if (unit == null) {
            throw new IllegalArgumentException("单位不存在");
        }
        return unit;
    }

    private void validateReadableUnit(Long currentUserId, Long unitId) {
        if (permissionService.isSuperAdmin(currentUserId)) {
            return;
        }
        UserEntity currentUser = userMapper.findById(currentUserId);
        if (currentUser == null || !Objects.equals(currentUser.getUnitId(), unitId)) {
            throw new IllegalArgumentException("无权查看该单位打卡点");
        }
    }

    private LoginUser currentLoginUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof LoginUser loginUser)) {
            throw new IllegalArgumentException("当前未登录");
        }
        return loginUser;
    }
}
