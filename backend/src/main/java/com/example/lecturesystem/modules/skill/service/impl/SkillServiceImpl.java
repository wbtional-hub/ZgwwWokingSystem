package com.example.lecturesystem.modules.skill.service.impl;

import com.example.lecturesystem.modules.aiconfig.entity.ProviderConfigEntity;
import com.example.lecturesystem.modules.aiconfig.mapper.ProviderConfigMapper;
import com.example.lecturesystem.modules.aiconfig.support.AiTokenCipherSupport;
import com.example.lecturesystem.modules.aipermission.service.AiPermissionService;
import com.example.lecturesystem.modules.agent.support.KnowledgeCitationContext;
import com.example.lecturesystem.modules.agent.support.OpenAiCompatibleChatClient;
import com.example.lecturesystem.modules.auth.security.LoginUser;
import com.example.lecturesystem.modules.knowledge.dto.KnowledgeSearchRequest;
import com.example.lecturesystem.modules.knowledge.mapper.KnowledgeBaseMapper;
import com.example.lecturesystem.modules.knowledge.mapper.KnowledgeChunkMapper;
import com.example.lecturesystem.modules.knowledge.vo.KnowledgeSearchResultVO;
import com.example.lecturesystem.modules.operationlog.service.OperationLogService;
import com.example.lecturesystem.modules.permission.service.PermissionService;
import com.example.lecturesystem.modules.permission.support.CurrentUserFacade;
import com.example.lecturesystem.modules.skill.dto.PublishSkillVersionRequest;
import com.example.lecturesystem.modules.skill.dto.RunSkillValidationRequest;
import com.example.lecturesystem.modules.skill.dto.SaveSkillBindingRequest;
import com.example.lecturesystem.modules.skill.dto.SaveSkillRequest;
import com.example.lecturesystem.modules.skill.dto.SaveSkillTestCaseRequest;
import com.example.lecturesystem.modules.skill.dto.SaveSkillVersionRequest;
import com.example.lecturesystem.modules.skill.dto.SkillQueryRequest;
import com.example.lecturesystem.modules.skill.dto.SkillTestCaseQueryRequest;
import com.example.lecturesystem.modules.skill.entity.SkillEntity;
import com.example.lecturesystem.modules.skill.entity.SkillKbBindingEntity;
import com.example.lecturesystem.modules.skill.entity.SkillTestCaseEntity;
import com.example.lecturesystem.modules.skill.entity.SkillValidationResultEntity;
import com.example.lecturesystem.modules.skill.entity.SkillValidationRunEntity;
import com.example.lecturesystem.modules.skill.entity.SkillVersionEntity;
import com.example.lecturesystem.modules.skill.mapper.SkillKbBindingMapper;
import com.example.lecturesystem.modules.skill.mapper.SkillMapper;
import com.example.lecturesystem.modules.skill.mapper.SkillTestCaseMapper;
import com.example.lecturesystem.modules.skill.mapper.SkillValidationResultMapper;
import com.example.lecturesystem.modules.skill.mapper.SkillValidationRunMapper;
import com.example.lecturesystem.modules.skill.mapper.SkillVersionMapper;
import com.example.lecturesystem.modules.skill.service.SkillService;
import com.example.lecturesystem.modules.skill.vo.SkillListItemVO;
import com.example.lecturesystem.modules.skill.vo.SkillValidationDetailVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class SkillServiceImpl implements SkillService {
    private final SkillMapper skillMapper;
    private final SkillVersionMapper skillVersionMapper;
    private final SkillKbBindingMapper skillKbBindingMapper;
    private final SkillTestCaseMapper skillTestCaseMapper;
    private final SkillValidationRunMapper skillValidationRunMapper;
    private final SkillValidationResultMapper skillValidationResultMapper;
    private final ProviderConfigMapper providerConfigMapper;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final KnowledgeChunkMapper knowledgeChunkMapper;
    private final AiTokenCipherSupport aiTokenCipherSupport;
    private final OpenAiCompatibleChatClient chatClient;
    private final CurrentUserFacade currentUserFacade;
    private final PermissionService permissionService;
    private final AiPermissionService aiPermissionService;
    private final OperationLogService operationLogService;

    public SkillServiceImpl(SkillMapper skillMapper,
                            SkillVersionMapper skillVersionMapper,
                            SkillKbBindingMapper skillKbBindingMapper,
                            SkillTestCaseMapper skillTestCaseMapper,
                            SkillValidationRunMapper skillValidationRunMapper,
                            SkillValidationResultMapper skillValidationResultMapper,
                            ProviderConfigMapper providerConfigMapper,
                            KnowledgeBaseMapper knowledgeBaseMapper,
                            KnowledgeChunkMapper knowledgeChunkMapper,
                            AiTokenCipherSupport aiTokenCipherSupport,
                            OpenAiCompatibleChatClient chatClient,
                            CurrentUserFacade currentUserFacade,
                            PermissionService permissionService,
                            AiPermissionService aiPermissionService,
                            OperationLogService operationLogService) {
        this.skillMapper = skillMapper;
        this.skillVersionMapper = skillVersionMapper;
        this.skillKbBindingMapper = skillKbBindingMapper;
        this.skillTestCaseMapper = skillTestCaseMapper;
        this.skillValidationRunMapper = skillValidationRunMapper;
        this.skillValidationResultMapper = skillValidationResultMapper;
        this.providerConfigMapper = providerConfigMapper;
        this.knowledgeBaseMapper = knowledgeBaseMapper;
        this.knowledgeChunkMapper = knowledgeChunkMapper;
        this.aiTokenCipherSupport = aiTokenCipherSupport;
        this.chatClient = chatClient;
        this.currentUserFacade = currentUserFacade;
        this.permissionService = permissionService;
        this.aiPermissionService = aiPermissionService;
        this.operationLogService = operationLogService;
    }

    @Override
    public Object list(SkillQueryRequest request) {
        LoginUser loginUser = currentUserFacade.currentLoginUser();
        SkillQueryRequest safeRequest = request == null ? new SkillQueryRequest() : request;
        if (isAdmin(loginUser) || aiPermissionService.canTrainSkill(loginUser.getUserId())) {
            return skillMapper.queryList(safeRequest);
        }
        List<SkillListItemVO> publishedList = skillMapper.queryPublishedList(safeRequest);
        List<SkillListItemVO> authorizedList = new ArrayList<>();
        for (SkillListItemVO item : publishedList) {
            if (item.getId() != null && aiPermissionService.canViewSkill(loginUser.getUserId(), item.getId())) {
                authorizedList.add(item);
            }
        }
        return authorizedList;
    }

    @Override
    @Transactional
    public Long saveSkill(SaveSkillRequest request) {
        LoginUser loginUser = currentUserFacade.currentLoginUser();
        LocalDateTime now = LocalDateTime.now();
        String skillCode = normalizeRequired(request.getSkillCode(), "skill code is required");
        String skillName = normalizeRequired(request.getSkillName(), "skill name is required");
        if (request.getId() == null) {
            requireTrainPermission(null);
            if (skillMapper.findByCode(skillCode) != null) {
                throw new IllegalArgumentException("skill code already exists");
            }
            SkillEntity entity = new SkillEntity();
            entity.setSkillCode(skillCode);
            entity.setSkillName(skillName);
            entity.setDomainType(normalize(request.getDomainType()));
            entity.setSkillType(normalize(request.getSkillType()));
            entity.setDescription(normalize(request.getDescription()));
            entity.setStatus(request.getStatus());
            entity.setOwnerUserId(loginUser.getUserId());
            entity.setCreateTime(now);
            entity.setUpdateTime(now);
            entity.setCreateUser(loginUser.getUsername());
            entity.setUpdateUser(loginUser.getUsername());
            entity.setIsDeleted(Boolean.FALSE);
            skillMapper.insert(entity);
            operationLogService.log("SKILL", "CREATE", entity.getId(), "create skill: " + skillName);
            return entity.getId();
        }
        SkillEntity existed = requireSkill(request.getId());
        requireTrainPermission(existed.getId());
        SkillEntity sameCode = skillMapper.findByCode(skillCode);
        if (sameCode != null && !sameCode.getId().equals(existed.getId())) {
            throw new IllegalArgumentException("skill code already exists");
        }
        existed.setSkillCode(skillCode);
        existed.setSkillName(skillName);
        existed.setDomainType(normalize(request.getDomainType()));
        existed.setSkillType(normalize(request.getSkillType()));
        existed.setDescription(normalize(request.getDescription()));
        existed.setStatus(request.getStatus());
        existed.setUpdateTime(now);
        existed.setUpdateUser(loginUser.getUsername());
        skillMapper.update(existed);
        operationLogService.log("SKILL", "UPDATE", existed.getId(), "update skill: " + skillName);
        return existed.getId();
    }

    @Override
    @Transactional
    public Long saveSkillVersion(SaveSkillVersionRequest request) {
        LoginUser loginUser = currentUserFacade.currentLoginUser();
        SkillEntity skill = requireSkill(request.getSkillId());
        requireTrainPermission(skill.getId());
        String versionNo = normalizeRequired(request.getVersionNo(), "version no is required");
        if (request.getId() == null && skillVersionMapper.findBySkillIdAndVersionNo(request.getSkillId(), versionNo) != null) {
            throw new IllegalArgumentException("version already exists");
        }
        if (request.getId() == null) {
            SkillVersionEntity entity = new SkillVersionEntity();
            entity.setSkillId(request.getSkillId());
            entity.setVersionNo(versionNo);
            entity.setProviderConfigId(request.getProviderConfigId());
            entity.setModelCode(normalize(request.getModelCode()));
            entity.setSystemPrompt(normalizeRequired(request.getSystemPrompt(), "system prompt is required"));
            entity.setTaskPrompt(normalize(request.getTaskPrompt()));
            entity.setOutputTemplate(normalize(request.getOutputTemplate()));
            entity.setForbiddenRules(normalize(request.getForbiddenRules()));
            entity.setCitationRules(normalize(request.getCitationRules()));
            entity.setValidationStatus("PENDING");
            entity.setPublishStatus("DRAFT");
            entity.setCreateTime(LocalDateTime.now());
            entity.setCreateUser(loginUser.getUsername());
            skillVersionMapper.insert(entity);
            operationLogService.log("SKILL", "SAVE_VERSION", entity.getId(), "save skill version: " + versionNo);
            return entity.getId();
        }
        SkillVersionEntity existed = requireVersion(request.getId());
        requireTrainPermission(existed.getSkillId());
        existed.setProviderConfigId(request.getProviderConfigId());
        existed.setModelCode(normalize(request.getModelCode()));
        existed.setSystemPrompt(normalizeRequired(request.getSystemPrompt(), "system prompt is required"));
        existed.setTaskPrompt(normalize(request.getTaskPrompt()));
        existed.setOutputTemplate(normalize(request.getOutputTemplate()));
        existed.setForbiddenRules(normalize(request.getForbiddenRules()));
        existed.setCitationRules(normalize(request.getCitationRules()));
        skillVersionMapper.update(existed);
        operationLogService.log("SKILL", "UPDATE_VERSION", existed.getId(), "update skill version: " + existed.getVersionNo());
        return existed.getId();
    }

    @Override
    @Transactional
    public void publishSkillVersion(PublishSkillVersionRequest request) {
        SkillVersionEntity version = requireVersion(request.getSkillVersionId());
        requirePublishPermission(version.getSkillId());
        skillVersionMapper.markPublished(version.getSkillId(), version.getId());
        operationLogService.log("SKILL", "PUBLISH_VERSION", version.getId(), "publish skill version: " + version.getVersionNo());
    }

    @Override
    @Transactional
    public void saveSkillBinding(SaveSkillBindingRequest request) {
        requireSkill(request.getSkillId());
        SkillVersionEntity version = requireVersion(request.getSkillVersionId());
        requireTrainPermission(version.getSkillId());
        if (knowledgeBaseMapper.findById(request.getBaseId()) == null) {
            throw new IllegalArgumentException("knowledge base not found");
        }
        SkillKbBindingEntity existed = skillKbBindingMapper.findBySkillVersionId(request.getSkillVersionId());
        skillKbBindingMapper.deleteBySkillVersionId(request.getSkillVersionId());
        SkillKbBindingEntity entity = existed == null ? new SkillKbBindingEntity() : existed;
        entity.setSkillId(request.getSkillId());
        entity.setSkillVersionId(request.getSkillVersionId());
        entity.setBaseId(request.getBaseId());
        entity.setCategoryId(request.getCategoryId());
        entity.setCreateTime(LocalDateTime.now());
        skillKbBindingMapper.insert(entity);
        operationLogService.log("SKILL", "SAVE_BINDING", request.getSkillVersionId(), "bind knowledge base: " + request.getBaseId());
    }

    @Override
    public Object listTestCases(SkillTestCaseQueryRequest request) {
        SkillVersionEntity version = requireVersion(request.getSkillVersionId());
        requireTrainPermission(version.getSkillId());
        return skillTestCaseMapper.queryBySkillVersionId(request.getSkillVersionId());
    }

    @Override
    @Transactional
    public Long saveTestCase(SaveSkillTestCaseRequest request) {
        Long skillId = request.getSkillId();
        if (request.getId() != null) {
            SkillTestCaseEntity existed = skillTestCaseMapper.findById(request.getId());
            if (existed == null) {
                throw new IllegalArgumentException("test case not found");
            }
            requireTrainPermission(existed.getSkillId());
            existed.setCaseType(normalizeRequired(request.getCaseType(), "case type is required"));
            existed.setQuestionText(normalizeRequired(request.getQuestionText(), "question is required"));
            existed.setExpectedPoints(normalize(request.getExpectedPoints()));
            existed.setExpectedFormat(normalize(request.getExpectedFormat()));
            existed.setStandardAnswer(normalize(request.getStandardAnswer()));
            existed.setStatus(request.getStatus() == null ? 1 : request.getStatus());
            skillTestCaseMapper.update(existed);
            return existed.getId();
        }
        requireTrainPermission(skillId);
        SkillTestCaseEntity entity = new SkillTestCaseEntity();
        entity.setSkillId(request.getSkillId());
        entity.setSkillVersionId(request.getSkillVersionId());
        entity.setCaseType(normalizeRequired(request.getCaseType(), "case type is required"));
        entity.setQuestionText(normalizeRequired(request.getQuestionText(), "question is required"));
        entity.setExpectedPoints(normalize(request.getExpectedPoints()));
        entity.setExpectedFormat(normalize(request.getExpectedFormat()));
        entity.setStandardAnswer(normalize(request.getStandardAnswer()));
        entity.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        entity.setCreateTime(LocalDateTime.now());
        skillTestCaseMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional
    public Object runValidation(RunSkillValidationRequest request) {
        SkillVersionEntity version = requireVersion(request.getSkillVersionId());
        requireTrainPermission(version.getSkillId());
        SkillKbBindingEntity binding = requireBinding(version.getId());
        LoginUser user = currentUserFacade.currentLoginUser();
        if (!isAdmin(user) && !aiPermissionService.canAnalyzeKnowledgeBase(user.getUserId(), binding.getBaseId())) {
            throw new IllegalArgumentException("current user cannot analyze this knowledge base");
        }
        ProviderConfigEntity provider = requireProvider(version.getProviderConfigId());
        String apiToken = aiTokenCipherSupport.decrypt(provider.getApiTokenCipher());
        List<SkillTestCaseEntity> cases = skillTestCaseMapper.queryActiveEntitiesBySkillVersionId(version.getId());
        if (cases.isEmpty()) {
            throw new IllegalArgumentException("please add validation cases first");
        }

        SkillValidationRunEntity run = new SkillValidationRunEntity();
        run.setSkillId(version.getSkillId());
        run.setSkillVersionId(version.getId());
        run.setProviderConfigId(version.getProviderConfigId());
        run.setModelCode(version.getModelCode());
        run.setRunStatus("RUNNING");
        run.setStartTime(LocalDateTime.now());
        run.setCreateTime(LocalDateTime.now());
        skillValidationRunMapper.insert(run);

        List<SkillValidationResultEntity> results = new ArrayList<>();
        int passCount = 0;
        int citationCount = 0;
        BigDecimal totalScore = BigDecimal.ZERO;

        for (SkillTestCaseEntity testCase : cases) {
            KnowledgeCitationContext context = buildContext(binding.getBaseId(), testCase.getQuestionText(), 5);
            String answer = chatClient.chat(provider.getApiBaseUrl(), apiToken, version.getModelCode(), buildSystemPrompt(version), buildUserPrompt(version, testCase.getQuestionText(), context));
            ScorePack scorePack = scoreAnswer(answer, context, testCase);
            if (scorePack.pass) {
                passCount++;
            }
            if (scorePack.hasCitation) {
                citationCount++;
            }
            totalScore = totalScore.add(scorePack.score);

            SkillValidationResultEntity entity = new SkillValidationResultEntity();
            entity.setRunId(run.getId());
            entity.setTestCaseId(testCase.getId());
            entity.setAnswerText(answer);
            entity.setHitChunks(context.toChunkIds());
            entity.setScore(scorePack.score);
            entity.setIsPass(scorePack.pass);
            entity.setFailReason(scorePack.failReason);
            results.add(entity);
        }
        skillValidationResultMapper.batchInsert(results);

        BigDecimal caseCount = BigDecimal.valueOf(cases.size());
        run.setRunStatus("SUCCESS");
        run.setFinishTime(LocalDateTime.now());
        run.setPassRate(BigDecimal.valueOf(passCount).multiply(BigDecimal.valueOf(100)).divide(caseCount, 2, RoundingMode.HALF_UP));
        run.setCitationRate(BigDecimal.valueOf(citationCount).multiply(BigDecimal.valueOf(100)).divide(caseCount, 2, RoundingMode.HALF_UP));
        run.setAvgScore(totalScore.divide(caseCount, 2, RoundingMode.HALF_UP));
        skillValidationRunMapper.update(run);
        skillVersionMapper.updateValidation(version.getId(), "SUCCESS", run.getAvgScore());
        operationLogService.log("SKILL", "RUN_VALIDATION", run.getId(), "run validation, version=" + version.getVersionNo());
        return skillValidationRunMapper.queryDetail(run.getId());
    }

    @Override
    public Object getValidationDetail(Long runId) {
        SkillValidationDetailVO detail = skillValidationRunMapper.queryDetail(runId);
        if (detail == null) {
            throw new IllegalArgumentException("validation run not found");
        }
        detail.setResults(skillValidationResultMapper.queryByRunId(runId));
        return detail;
    }

    @Override
    public Object getPublishedVersion(Long skillId) {
        LoginUser user = currentUserFacade.currentLoginUser();
        if (!isAdmin(user) && !aiPermissionService.canTrainSkill(user.getUserId()) && !aiPermissionService.canViewSkill(user.getUserId(), skillId)) {
            throw new IllegalArgumentException("current user cannot view this skill");
        }
        return skillVersionMapper.queryLatestPublishedBySkillId(skillId);
    }

    private KnowledgeCitationContext buildContext(Long baseId, String question, int topN) {
        KnowledgeSearchRequest request = new KnowledgeSearchRequest();
        request.setBaseId(baseId);
        request.setKeywords(question);
        request.setEffectiveOnly(Boolean.TRUE);
        request.setTopN(topN);
        List<KnowledgeSearchResultVO> list = knowledgeChunkMapper.search(request);
        KnowledgeCitationContext context = new KnowledgeCitationContext();
        if (list != null) {
            context.getChunks().addAll(list);
        }
        return context;
    }

    private String buildSystemPrompt(SkillVersionEntity version) {
        StringBuilder builder = new StringBuilder();
        builder.append(version.getSystemPrompt() == null ? "You are a professional assistant." : version.getSystemPrompt());
        if (version.getForbiddenRules() != null) {
            builder.append("\nForbidden rules: ").append(version.getForbiddenRules());
        }
        if (version.getCitationRules() != null) {
            builder.append("\nCitation rules: ").append(version.getCitationRules());
        }
        if (version.getOutputTemplate() != null) {
            builder.append("\nOutput template: ").append(version.getOutputTemplate());
        }
        return builder.toString();
    }

    private String buildUserPrompt(SkillVersionEntity version, String question, KnowledgeCitationContext context) {
        StringBuilder builder = new StringBuilder();
        if (version.getTaskPrompt() != null) {
            builder.append(version.getTaskPrompt()).append("\n\n");
        }
        builder.append("Answer the question strictly based on the knowledge below, and append a citation section at the end.\n\n")
                .append("Knowledge context:\n")
                .append(context.toPromptContext())
                .append("Question:\n")
                .append(question);
        return builder.toString();
    }

    private ScorePack scoreAnswer(String answer, KnowledgeCitationContext context, SkillTestCaseEntity testCase) {
        BigDecimal score = BigDecimal.ZERO;
        boolean hasCitation = context.getChunks() != null && !context.getChunks().isEmpty() && answer != null && answer.toLowerCase().contains("citation");
        if (answer != null && !answer.trim().isEmpty()) {
            score = score.add(BigDecimal.valueOf(50));
        }
        if (hasCitation) {
            score = score.add(BigDecimal.valueOf(20));
        }
        if (testCase.getExpectedPoints() != null) {
            List<String> points = Arrays.stream(testCase.getExpectedPoints().split("[,\n;]"))
                    .map(String::trim)
                    .filter(item -> !item.isEmpty())
                    .toList();
            int matched = 0;
            for (String point : points) {
                if (answer != null && answer.contains(point)) {
                    matched++;
                }
            }
            if (!points.isEmpty()) {
                score = score.add(BigDecimal.valueOf(30L * matched / points.size()));
            }
        } else if (testCase.getStandardAnswer() != null && answer != null && answer.length() > 20) {
            score = score.add(BigDecimal.valueOf(20));
        }
        if (score.compareTo(BigDecimal.valueOf(100)) > 0) {
            score = BigDecimal.valueOf(100);
        }
        boolean pass = score.compareTo(BigDecimal.valueOf(60)) >= 0;
        ScorePack pack = new ScorePack();
        pack.score = score;
        pack.pass = pass;
        pack.hasCitation = hasCitation;
        pack.failReason = pass ? null : "answer completeness or citation is not enough";
        return pack;
    }

    private SkillEntity requireSkill(Long id) {
        SkillEntity entity = skillMapper.findById(id);
        if (entity == null) {
            throw new IllegalArgumentException("skill not found");
        }
        return entity;
    }

    private SkillVersionEntity requireVersion(Long id) {
        SkillVersionEntity entity = skillVersionMapper.findById(id);
        if (entity == null) {
            throw new IllegalArgumentException("skill version not found");
        }
        return entity;
    }

    private SkillKbBindingEntity requireBinding(Long skillVersionId) {
        SkillKbBindingEntity entity = skillKbBindingMapper.findBySkillVersionId(skillVersionId);
        if (entity == null) {
            throw new IllegalArgumentException("please bind a knowledge base first");
        }
        return entity;
    }

    private ProviderConfigEntity requireProvider(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("please configure an AI provider first");
        }
        ProviderConfigEntity entity = providerConfigMapper.findById(id);
        if (entity == null) {
            throw new IllegalArgumentException("AI provider not found");
        }
        return entity;
    }

    private void requireTrainPermission(Long skillId) {
        LoginUser user = currentUserFacade.currentLoginUser();
        if (isAdmin(user)) {
            return;
        }
        boolean allowed = skillId == null ? aiPermissionService.canTrainSkill(user.getUserId()) : aiPermissionService.canTrainSkill(user.getUserId()) || aiPermissionService.canTrainSkill(user.getUserId(), skillId);
        if (!allowed) {
            throw new IllegalArgumentException("current user cannot train this skill");
        }
    }

    private void requirePublishPermission(Long skillId) {
        LoginUser user = currentUserFacade.currentLoginUser();
        if (isAdmin(user)) {
            return;
        }
        boolean allowed = skillId == null ? aiPermissionService.canPublishSkill(user.getUserId()) : aiPermissionService.canPublishSkill(user.getUserId()) || aiPermissionService.canPublishSkill(user.getUserId(), skillId);
        if (!allowed) {
            throw new IllegalArgumentException("current user cannot publish this skill");
        }
    }

    private boolean isAdmin(LoginUser user) {
        return permissionService.isSuperAdmin(user.getUserId());
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

    private static class ScorePack {
        private BigDecimal score;
        private boolean pass;
        private boolean hasCitation;
        private String failReason;
    }
}