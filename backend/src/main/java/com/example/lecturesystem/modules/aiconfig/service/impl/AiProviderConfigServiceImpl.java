package com.example.lecturesystem.modules.aiconfig.service.impl;

import com.example.lecturesystem.modules.aiconfig.dto.ProviderConfigQueryRequest;
import com.example.lecturesystem.modules.aiconfig.dto.SaveProviderConfigRequest;
import com.example.lecturesystem.modules.aiconfig.dto.TestProviderConfigRequest;
import com.example.lecturesystem.modules.aiconfig.dto.ToggleProviderConfigStatusRequest;
import com.example.lecturesystem.modules.aiconfig.entity.ProviderConfigEntity;
import com.example.lecturesystem.modules.aiconfig.entity.ProviderModelEntity;
import com.example.lecturesystem.modules.aiconfig.mapper.ProviderConfigMapper;
import com.example.lecturesystem.modules.aiconfig.mapper.ProviderModelMapper;
import com.example.lecturesystem.modules.aiconfig.service.AiProviderConfigService;
import com.example.lecturesystem.modules.aiconfig.support.AiProviderConnectivityTester;
import com.example.lecturesystem.modules.aiconfig.support.AiTokenCipherSupport;
import com.example.lecturesystem.modules.aiconfig.vo.ProviderConfigListItemVO;
import com.example.lecturesystem.modules.aiconfig.vo.ProviderModelVO;
import com.example.lecturesystem.modules.aiconfig.vo.ProviderTestResultVO;
import com.example.lecturesystem.modules.auth.security.LoginUser;
import com.example.lecturesystem.modules.operationlog.service.OperationLogService;
import com.example.lecturesystem.modules.permission.service.PermissionService;
import com.example.lecturesystem.modules.permission.support.CurrentUserFacade;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class AiProviderConfigServiceImpl implements AiProviderConfigService {
    private final ProviderConfigMapper providerConfigMapper;
    private final ProviderModelMapper providerModelMapper;
    private final AiTokenCipherSupport aiTokenCipherSupport;
    private final AiProviderConnectivityTester connectivityTester;
    private final CurrentUserFacade currentUserFacade;
    private final PermissionService permissionService;
    private final OperationLogService operationLogService;

    public AiProviderConfigServiceImpl(ProviderConfigMapper providerConfigMapper,
                                       ProviderModelMapper providerModelMapper,
                                       AiTokenCipherSupport aiTokenCipherSupport,
                                       AiProviderConnectivityTester connectivityTester,
                                       CurrentUserFacade currentUserFacade,
                                       PermissionService permissionService,
                                       OperationLogService operationLogService) {
        this.providerConfigMapper = providerConfigMapper;
        this.providerModelMapper = providerModelMapper;
        this.aiTokenCipherSupport = aiTokenCipherSupport;
        this.connectivityTester = connectivityTester;
        this.currentUserFacade = currentUserFacade;
        this.permissionService = permissionService;
        this.operationLogService = operationLogService;
    }

    @Override
    public Object list(ProviderConfigQueryRequest request) {
        requireAdmin();
        List<ProviderConfigListItemVO> list = providerConfigMapper.queryList(request == null ? new ProviderConfigQueryRequest() : request);
        for (ProviderConfigListItemVO item : list) {
            item.setModels(providerModelMapper.queryByProviderId(item.getId()));
        }
        return list;
    }

    @Override
    @Transactional
    public Long save(SaveProviderConfigRequest request) {
        LoginUser loginUser = currentUserFacade.currentLoginUser();
        requireAdmin();
        LocalDateTime now = LocalDateTime.now();
        String providerCode = normalizeRequired(request.getProviderCode(), "接入编码不能为空");
        String providerName = normalizeRequired(request.getProviderName(), "接入名称不能为空");
        String apiBaseUrl = normalizeRequired(request.getApiBaseUrl(), "API Base URL不能为空");
        String apiToken = normalize(request.getApiToken());

        if (request.getId() == null) {
            if (providerConfigMapper.findByCode(providerCode) != null) {
                throw new IllegalArgumentException("接入编码已存在");
            }
            if (apiToken == null) {
                throw new IllegalArgumentException("新增接入时必须填写API Token");
            }
            ProviderConfigEntity entity = new ProviderConfigEntity();
            entity.setProviderCode(providerCode);
            entity.setProviderName(providerName);
            entity.setApiBaseUrl(apiBaseUrl);
            entity.setApiTokenCipher(aiTokenCipherSupport.encrypt(apiToken));
            entity.setTokenMask(aiTokenCipherSupport.mask(apiToken));
            entity.setDefaultModel(normalize(request.getDefaultModel()));
            entity.setConnectStatus("PENDING");
            entity.setStatus(request.getStatus());
            entity.setRemark(normalize(request.getRemark()));
            entity.setCreateTime(now);
            entity.setUpdateTime(now);
            entity.setCreateUser(loginUser.getUsername());
            entity.setUpdateUser(loginUser.getUsername());
            entity.setIsDeleted(Boolean.FALSE);
            providerConfigMapper.insert(entity);
            operationLogService.log("AI_PROVIDER", "CREATE", entity.getId(), "新增AI接入：" + providerName);
            return entity.getId();
        }

        ProviderConfigEntity existed = requireProvider(request.getId());
        ProviderConfigEntity sameCode = providerConfigMapper.findByCode(providerCode);
        if (sameCode != null && !sameCode.getId().equals(existed.getId())) {
            throw new IllegalArgumentException("接入编码已存在");
        }
        existed.setProviderCode(providerCode);
        existed.setProviderName(providerName);
        existed.setApiBaseUrl(apiBaseUrl);
        if (apiToken != null) {
            existed.setApiTokenCipher(aiTokenCipherSupport.encrypt(apiToken));
            existed.setTokenMask(aiTokenCipherSupport.mask(apiToken));
        }
        existed.setDefaultModel(normalize(request.getDefaultModel()));
        existed.setConnectStatus("PENDING");
        existed.setStatus(request.getStatus());
        existed.setRemark(normalize(request.getRemark()));
        existed.setUpdateTime(now);
        existed.setUpdateUser(loginUser.getUsername());
        providerConfigMapper.update(existed);
        operationLogService.log("AI_PROVIDER", "UPDATE", existed.getId(), "编辑AI接入：" + providerName);
        return existed.getId();
    }

    @Override
    @Transactional
    public void toggleStatus(ToggleProviderConfigStatusRequest request) {
        LoginUser loginUser = currentUserFacade.currentLoginUser();
        requireAdmin();
        ProviderConfigEntity existed = requireProvider(request.getId());
        providerConfigMapper.updateStatus(existed.getId(), request.getStatus(), loginUser.getUsername(), LocalDateTime.now());
        operationLogService.log("AI_PROVIDER", "TOGGLE_STATUS", existed.getId(), "切换AI接入状态");
    }

    @Override
    @Transactional
    public Object test(TestProviderConfigRequest request) {
        LoginUser loginUser = currentUserFacade.currentLoginUser();
        requireAdmin();
        ProviderConfigEntity existed = requireProvider(request.getId());
        String apiToken = aiTokenCipherSupport.decrypt(existed.getApiTokenCipher());
        ProviderTestResultVO result = connectivityTester.test(existed.getApiBaseUrl(), apiToken);
        result.setProviderConfigId(existed.getId());
        String defaultModel = existed.getDefaultModel();
        if (defaultModel == null && result.getModels() != null && !result.getModels().isEmpty()) {
            defaultModel = result.getModels().get(0).getModelCode();
        }
        providerConfigMapper.updateTestStatus(existed.getId(), result.getConnectStatus(), defaultModel, loginUser.getUsername(), LocalDateTime.now());
        syncModels(existed.getId(), result.getModels());
        operationLogService.log("AI_PROVIDER", "TEST", existed.getId(), "测试AI接入：" + existed.getProviderName() + "，结果=" + result.getConnectStatus());
        return result;
    }

    private void syncModels(Long providerConfigId, List<ProviderModelVO> models) {
        providerModelMapper.deleteByProviderId(providerConfigId);
        if (models == null || models.isEmpty()) {
            return;
        }
        List<ProviderModelEntity> entities = new ArrayList<>();
        for (ProviderModelVO item : models) {
            ProviderModelEntity entity = new ProviderModelEntity();
            entity.setProviderConfigId(providerConfigId);
            entity.setModelCode(item.getModelCode());
            entity.setModelName(item.getModelName());
            entity.setModelType(item.getModelType());
            entity.setSupportKnowledge(Boolean.TRUE.equals(item.getSupportKnowledge()));
            entity.setSupportSkillTrain(Boolean.TRUE.equals(item.getSupportSkillTrain()));
            entity.setSupportAgentChat(Boolean.TRUE.equals(item.getSupportAgentChat()));
            entity.setSupportAnalysis(Boolean.TRUE.equals(item.getSupportAnalysis()));
            entity.setStatus(item.getStatus() == null ? 1 : item.getStatus());
            entity.setCreateTime(LocalDateTime.now());
            entities.add(entity);
        }
        providerModelMapper.batchInsert(entities);
    }

    private ProviderConfigEntity requireProvider(Long id) {
        ProviderConfigEntity entity = providerConfigMapper.findById(id);
        if (entity == null) {
            throw new IllegalArgumentException("AI接入配置不存在");
        }
        return entity;
    }

    private void requireAdmin() {
        LoginUser loginUser = currentUserFacade.currentLoginUser();
        if (!permissionService.isSuperAdmin(loginUser.getUserId())) {
            throw new IllegalArgumentException("仅管理员可执行该操作");
        }
    }

    private String normalize(String text) {
        if (text == null) {
            return null;
        }
        String trimmed = text.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeRequired(String text, String message) {
        String value = normalize(text);
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }
}