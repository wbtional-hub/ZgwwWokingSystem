package com.example.lecturesystem.modules.param.service.impl;

import com.example.lecturesystem.modules.auth.security.LoginUser;
import com.example.lecturesystem.modules.config.vo.AmapConfigVO;
import com.example.lecturesystem.modules.param.dto.ParamQueryRequest;
import com.example.lecturesystem.modules.param.dto.SaveParamRequest;
import com.example.lecturesystem.modules.param.dto.ToggleParamStatusRequest;
import com.example.lecturesystem.modules.param.entity.ParamEntity;
import com.example.lecturesystem.modules.param.mapper.ParamMapper;
import com.example.lecturesystem.modules.param.service.ParamService;
import com.example.lecturesystem.modules.permission.service.PermissionService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class ParamServiceImpl implements ParamService {
    private final ParamMapper paramMapper;
    private final PermissionService permissionService;

    public ParamServiceImpl(ParamMapper paramMapper, PermissionService permissionService) {
        this.paramMapper = paramMapper;
        this.permissionService = permissionService;
    }

    @Override
    public Object listParams(ParamQueryRequest request) {
        requireAdmin();
        return paramMapper.queryList(request);
    }

    @Override
    public String getByCode(String code) {
        ParamEntity entity = paramMapper.findByCode(code);
        return entity == null ? null : entity.getParamValue();
    }

    @Override
    public AmapConfigVO queryAmapConfig() {
        requireAdmin();
        AmapConfigVO response = new AmapConfigVO();
        response.setKey(requireEnabledParamValue("Key", "高德地图 Key 未配置或未启用"));
        response.setSecurityJsCode(requireEnabledParamValue("securityJsCode", "高德地图安全密钥未配置或未启用"));
        return response;
    }

    @Override
    @Transactional
    public Long saveParam(SaveParamRequest request) {
        LoginUser loginUser = currentLoginUser();
        requireAdmin();
        LocalDateTime now = LocalDateTime.now();

        if (request.getId() == null) {
            if (paramMapper.findByCode(request.getParamCode().trim()) != null) {
                throw new IllegalArgumentException("参数编码已存在");
            }
            ParamEntity entity = new ParamEntity();
            entity.setParamCode(request.getParamCode().trim());
            entity.setParamName(request.getParamName().trim());
            entity.setParamValue(trimToNull(request.getParamValue()));
            entity.setStatus(request.getStatus());
            entity.setRemark(trimToNull(request.getRemark()));
            entity.setCreateTime(now);
            entity.setUpdateTime(now);
            entity.setCreateUser(loginUser.getUsername());
            entity.setUpdateUser(loginUser.getUsername());
            entity.setIsDeleted(Boolean.FALSE);
            paramMapper.insert(entity);
            return entity.getId();
        }

        ParamEntity existed = requireParam(request.getId());
        ParamEntity sameCode = paramMapper.findByCode(request.getParamCode().trim());
        if (sameCode != null && !sameCode.getId().equals(existed.getId())) {
            throw new IllegalArgumentException("参数编码已存在");
        }
        existed.setParamName(request.getParamName().trim());
        existed.setParamValue(trimToNull(request.getParamValue()));
        existed.setStatus(request.getStatus());
        existed.setRemark(trimToNull(request.getRemark()));
        existed.setUpdateTime(now);
        existed.setUpdateUser(loginUser.getUsername());
        paramMapper.update(existed);
        return existed.getId();
    }

    @Override
    @Transactional
    public void deleteParam(Long id) {
        LoginUser loginUser = currentLoginUser();
        requireAdmin();
        requireParam(id);
        paramMapper.logicalDelete(id, loginUser.getUsername(), LocalDateTime.now());
    }

    @Override
    @Transactional
    public void toggleStatus(ToggleParamStatusRequest request) {
        LoginUser loginUser = currentLoginUser();
        requireAdmin();
        requireParam(request.getId());
        paramMapper.updateStatus(request.getId(), request.getStatus(), loginUser.getUsername(), LocalDateTime.now());
    }

    private ParamEntity requireParam(Long id) {
        ParamEntity entity = paramMapper.findById(id);
        if (entity == null) {
            throw new IllegalArgumentException("参数不存在");
        }
        return entity;
    }

    private String requireEnabledParamValue(String code, String message) {
        ParamEntity entity = paramMapper.findByCode(code);
        if (entity == null || entity.getIsDeleted() == Boolean.TRUE || entity.getStatus() == null || entity.getStatus() != 1) {
            throw new IllegalArgumentException(message);
        }
        String value = trimToNull(entity.getParamValue());
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private LoginUser currentLoginUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof LoginUser loginUser)) {
            throw new IllegalArgumentException("当前未登录");
        }
        return loginUser;
    }

    private void requireAdmin() {
        LoginUser loginUser = currentLoginUser();
        if (!permissionService.isSuperAdmin(loginUser.getUserId())) {
            throw new IllegalArgumentException("仅管理员可执行该操作");
        }
    }
}
