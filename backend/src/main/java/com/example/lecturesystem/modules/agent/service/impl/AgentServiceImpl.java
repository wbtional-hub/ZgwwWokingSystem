package com.example.lecturesystem.modules.agent.service.impl;

import com.example.lecturesystem.modules.agent.dto.AgentChatRequest;
import com.example.lecturesystem.modules.agent.dto.AgentSessionQueryRequest;
import com.example.lecturesystem.modules.agent.dto.CreateAgentSessionRequest;
import com.example.lecturesystem.modules.agent.dto.UpdateAgentSessionStatusRequest;
import com.example.lecturesystem.modules.agent.entity.AiAgentUsageEntity;
import com.example.lecturesystem.modules.agent.entity.AgentMessageEntity;
import com.example.lecturesystem.modules.agent.entity.AgentSessionEntity;
import com.example.lecturesystem.modules.agent.entity.AgentUserPreferenceEntity;
import com.example.lecturesystem.modules.agent.mapper.AiAgentUsageMapper;
import com.example.lecturesystem.modules.agent.mapper.AgentMessageMapper;
import com.example.lecturesystem.modules.agent.mapper.AgentSessionMapper;
import com.example.lecturesystem.modules.agent.mapper.AgentUserPreferenceMapper;
import com.example.lecturesystem.modules.agent.service.AgentService;
import com.example.lecturesystem.modules.agent.support.KnowledgeCitationContext;
import com.example.lecturesystem.modules.agent.support.OpenAiCompatibleChatClient;
import com.example.lecturesystem.modules.agent.support.PolicyRouteIntent;
import com.example.lecturesystem.modules.agent.support.PolicyRouteService;
import com.example.lecturesystem.modules.agent.support.policy.AiPolicyConsultService;
import com.example.lecturesystem.modules.agent.vo.AgentChatResultVO;
import com.example.lecturesystem.modules.agent.vo.AgentExpertMetricVO;
import com.example.lecturesystem.modules.agent.vo.AgentMessageVO;
import com.example.lecturesystem.modules.agent.vo.AgentMonthlyReportVO;
import com.example.lecturesystem.modules.agent.vo.AgentMonthlySummaryVO;
import com.example.lecturesystem.modules.agent.vo.AgentRankItemVO;
import com.example.lecturesystem.modules.agent.vo.AgentSessionStatsVO;
import com.example.lecturesystem.modules.agent.vo.AgentSessionTrendVO;
import com.example.lecturesystem.modules.agent.vo.AgentSessionVO;
import com.example.lecturesystem.modules.agent.vo.AgentTrendPointVO;
import com.example.lecturesystem.modules.aiconfig.entity.ProviderConfigEntity;
import com.example.lecturesystem.modules.aiconfig.mapper.ProviderConfigMapper;
import com.example.lecturesystem.modules.aiconfig.support.AiTokenCipherSupport;
import com.example.lecturesystem.modules.aipermission.service.AiPermissionService;
import com.example.lecturesystem.modules.auth.security.LoginUser;
import com.example.lecturesystem.modules.knowledge.dto.KnowledgeSearchRequest;
import com.example.lecturesystem.modules.knowledge.mapper.KnowledgeChunkMapper;
import com.example.lecturesystem.modules.knowledge.service.PolicyCatalogService;
import com.example.lecturesystem.modules.knowledge.support.PolicyKnowledgeSupport;
import com.example.lecturesystem.modules.knowledge.vo.KnowledgeSearchResultVO;
import com.example.lecturesystem.modules.operationlog.service.OperationLogService;
import com.example.lecturesystem.modules.param.service.ParamService;
import com.example.lecturesystem.modules.permission.service.PermissionService;
import com.example.lecturesystem.modules.permission.support.CurrentUserFacade;
import com.example.lecturesystem.modules.skill.entity.SkillVersionEntity;
import com.example.lecturesystem.modules.skill.mapper.SkillVersionMapper;
import com.example.lecturesystem.modules.skill.vo.SkillVersionDetailVO;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.example.lecturesystem.modules.logcenter.service.LogCenterService;
import com.example.lecturesystem.modules.aiuserprofile.service.AiUserProfileService;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Collections;
import java.util.Objects;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class AgentServiceImpl implements AgentService {
    private static final String DEFAULT_SESSION_TITLE = "New session";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_ARCHIVED = "ARCHIVED";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String QUOTA_PARAM_ENABLED = "AI_AGENT_MONTHLY_TOKEN_QUOTA_ENABLED";
    private static final String QUOTA_PARAM_VALUE = "AI_AGENT_MONTHLY_TOKEN_QUOTA";
    private static final String QUOTA_EXCEEDED_MESSAGE = "本月 AI Token 额度已用完，请联系管理员调整额度。";
    private static final String LOG_EVENT_SESSION_CREATE = "SESSION_CREATE";
    private static final String LOG_EVENT_AI_CHAT = "AI_CHAT";
    private static final String LOG_EVENT_KNOWLEDGE_HIT = "KNOWLEDGE_HIT";
    private static final String LOG_EVENT_PROVIDER_CALL_SUCCESS = "PROVIDER_CALL_SUCCESS";
    private static final String LOG_EVENT_PROVIDER_CALL_FAILED = "PROVIDER_CALL_FAILED";
    private static final String LOG_EVENT_ASSISTANT_MESSAGE_SAVED = "ASSISTANT_MESSAGE_SAVED";
    private static final String LOG_EVENT_RESULT_FLOW_READY = "RESULT_FLOW_READY";
    private static final String LOG_EVENT_FAST_PATH_HIT = "FAST_PATH_HIT";
    private static final String LOG_EVENT_STREAM_START = "STREAM_START";
    private static final String LOG_EVENT_STREAM_DELTA = "STREAM_DELTA";
    private static final String LOG_EVENT_STREAM_DONE = "STREAM_DONE";
    private static final String LOG_EVENT_STREAM_EMPTY_RESULT = "STREAM_EMPTY_RESULT";
    private static final String LOG_EVENT_STREAM_DONE_WITHOUT_DELTA = "STREAM_DONE_WITHOUT_DELTA";
    private static final String LOG_EVENT_STREAM_ERROR = "STREAM_ERROR";
    private static final String LOG_EVENT_POLICY_ROUTE_COMPARE = "POLICY_ROUTE_COMPARE";
    private static final String LOG_EVENT_POLICY_BOUNDARY_CATALOG = "POLICY_BOUNDARY_CATALOG";
    private static final String LOG_EVENT_PROVIDER_STREAM_START = "PROVIDER_STREAM_START";
    private static final String LOG_EVENT_PROVIDER_STREAM_DELTA = "PROVIDER_STREAM_DELTA";
    private static final String LOG_EVENT_PROVIDER_STREAM_DONE = "PROVIDER_STREAM_DONE";
    private static final String LOG_EVENT_PROVIDER_STREAM_FALLBACK = "PROVIDER_STREAM_FALLBACK";
    private static final String LOG_EVENT_PROVIDER_STREAM_ERROR = "PROVIDER_STREAM_ERROR";
    private static final String USAGE_SOURCE_FALLBACK_NO_AI_PERMISSION = "FALLBACK_NO_AI_PERMISSION";
    private static final String USAGE_SOURCE_FALLBACK_NO_PROVIDER = "FALLBACK_NO_PROVIDER";
    private static final String USAGE_SOURCE_FALLBACK_PROVIDER_FAILED = "FALLBACK_PROVIDER_FAILED";
    private static final String USAGE_SOURCE_FAST_PATH_STRUCTURED = "FAST_PATH_STRUCTURED";
    private static final String USAGE_SOURCE_PROVIDER_STREAM_USAGE = "PROVIDER_STREAM_USAGE";
    private static final String USAGE_SOURCE_PROVIDER_STREAM_NO_USAGE = "PROVIDER_STREAM_NO_USAGE";
    private static final String USAGE_SOURCE_PROVIDER_STREAM_FALLBACK_SEGMENTED = "PROVIDER_STREAM_FALLBACK_SEGMENTED";
    private static final String ANSWER_SOURCE_LLM_PROVIDER = "LLM_PROVIDER";
    private static final String ANSWER_SOURCE_KNOWLEDGE_FALLBACK_STRUCTURED = "KNOWLEDGE_FALLBACK_STRUCTURED";
    private static final String USAGE_SOURCE_FALLBACK_NO_POLICY_EVIDENCE = "FALLBACK_NO_POLICY_EVIDENCE";
    private static final String MODEL_CODE_FAST_PATH = "FAST_PATH";
    private static final String USAGE_SOURCE_SYSTEM_META = "SYSTEM_META";
private static final String ANSWER_SOURCE_SYSTEM_META = "SYSTEM_META";
    private static final String FALLBACK_REASON_NO_AI_PERMISSION = "当前账号未开通 AI 问答权限，已切换为知识库兜底模式。";
    private static final String FALLBACK_REASON_NO_PROVIDER = "当前系统未检测到可用的 AI Provider，已切换为知识库兜底模式。";
    private static final String FALLBACK_REASON_AI_UNAVAILABLE = "当前 AI 服务暂不可用，已切换为知识库兜底模式。";
    private static final List<String> SEARCH_STOP_TERMS = List.of("请问", "目前", "现在", "有哪些", "有啥", "什么", "怎么", "如何", "可以", "吗", "呢", "一下");
    private static final List<String> SEARCH_HINT_TERMS = List.of(
        "厦门市", "厦门", "福建省", "福建", "人才", "政策", "人才政策", "补贴", "住房", "安居",
        "认定", "申报", "创业", "项目", "企业", "引进", "高层次", "落户", "奖励", "经费", "支持",
        "服务保障", "人才服务", "绿色通道", "子女入学", "子女教育", "医疗保障", "医疗服务",
        "健康体检", "配偶就业", "落户服务", "待遇保障"
);

    private static final String CLEAN_POLICY_DOC_TITLE = "厦门市人才政策（清洗导入版）";
    private static final String RAW_POLICY_DOC_TITLE = "2025年厦门市人才";
    private static final List<String> LIST_QUESTION_KEYWORDS = List.of("有哪些", "包含哪些", "包括哪些", "列举", "清单", "名单", "主要有哪些");
    private static final List<String> PROCESS_QUESTION_KEYWORDS = List.of("如何申请", "怎么申请", "申报", "流程", "材料", "入口", "审核", "公示", "认定");
    private static final List<String> CONDITION_QUESTION_KEYWORDS = List.of("条件", "要求", "适用对象", "资格", "适合", "能不能申请");
    private static final List<String> BENEFIT_QUESTION_KEYWORDS = List.of(
        "补助多少",
        "补贴多少",
        "奖励多少",
        "支持多少",
        "安家补贴",
        "待遇",
        "人才待遇",
        "保障待遇",
        "待遇保障",
        "子女入学",
        "子女就学",
        "子女教育",
        "医疗保障",
        "医疗服务",
        "健康体检",
        "服务保障",
        "人才服务",
        "绿色通道",
        "配偶就业",
        "落户服务"
);
    private static final List<String> SPECIAL_TOPIC_KEYWORDS = List.of(
        "双百计划",
        "特聘岗位",
        "专项资金",
        "群鹭兴厦",
        "博士后",
        "人工智能",
        "台湾特聘专家",
        "住房",
        "子女教育",
        "子女入学",
        "子女就学",
        "孩子上学",
        "小孩上学",
        "医疗保障",
        "医疗服务",
        "健康体检",
        "就医",
        "服务保障",
        "人才服务",
        "人才服务卡",
        "绿色通道",
        "一站式服务",
        "配偶就业",
        "落户服务",
        "人才待遇",
        "待遇保障"
);

private static final List<String> SERVICE_GUARANTEE_TERMS = List.of(
        "服务保障",
        "人才服务",
        "人才服务卡",
        "绿色通道",
        "一站式服务",
        "子女教育",
        "子女入学",
        "子女就学",
        "孩子上学",
        "小孩上学",
        "随迁子女",
        "医疗保障",
        "医疗服务",
        "健康体检",
        "就医",
        "体检",
        "保健",
        "配偶就业",
        "落户服务",
        "居留服务",
        "人才待遇",
        "待遇保障"
);
    private static final List<String> PROCESS_SECTION_TERMS = List.of(
        "申报与评选", "遴选程序", "遴选与申请流程", "申请流程", "申报流程", "五步流程",
        "组织申报", "资格核查", "部门联审", "综合评审", "公示确定", "研究确认",
        "资金拨付", "兑现申请", "材料清单", "申报材料", "审核程序");
    private static final List<String> CONDITION_SECTION_TERMS = List.of("申报条件", "基本条件", "对象", "适用对象", "资格", "申报要求", "认定条件");
    private static final List<String> BENEFIT_SECTION_TERMS = List.of(
        "支持政策",
        "补助标准",
        "安家补贴",
        "资金扶持",
        "奖励",
        "补贴标准",
        "资助标准",
        "管理期",
        "待遇",
        "服务保障",
        "人才服务",
        "子女入学",
        "子女教育",
        "医疗保障",
        "医疗服务",
        "健康体检",
        "绿色通道",
        "配偶就业",
        "落户服务"
);
    private static final List<String> OVERVIEW_SECTION_TERMS = List.of("导入目录", "政策正文", "咨询主题", "检索建议", "总览", "目录", "总则", "意见", "实施办法");
    private static final List<String> ABSTRACT_SUMMARY_TERMS = List.of("框架摘要", "基础框架", "主框架", "政策框架");
    private static final String PROCESS_EVIDENCE_MISSING_MESSAGE = "已确认该政策/项目存在，当前知识库已命中部分政策依据，但缺少完整申请流程/申报通知/材料清单依据";
    private static final List<PolicyAliasMapping> POLICY_ALIAS_MAPPINGS = List.of(
            new PolicyAliasMapping("厦门市引进高层次创新创业人才“双百计划”实施意见", List.of("双百计划")),
            new PolicyAliasMapping("厦门市高层次人才特聘岗位实施方案", List.of("特聘岗位")),
            new PolicyAliasMapping("厦门市高层次人才专项资金管理办法", List.of("专项资金")),
            new PolicyAliasMapping("关于更加精准有效集聚人才加快推进高质量发展的意见", List.of("群鹭兴厦", "厦门人才总纲", "厦门人才意见")),
            new PolicyAliasMapping("厦门市电子信息产业人才项目实施办法", List.of("电子信息人才", "电子信息产业人才")),
            new PolicyAliasMapping("厦门市机械装备产业人才项目实施办法", List.of("机械装备人才", "机械装备产业人才")),
            new PolicyAliasMapping("厦门市商贸物流产业人才项目实施办法", List.of("商贸物流人才", "商贸物流产业人才")),
            new PolicyAliasMapping("厦门市金融服务产业人才项目实施办法", List.of("金融人才", "金融服务人才", "金融服务产业人才")),
            new PolicyAliasMapping("厦门市生物医药产业人才项目实施办法", List.of("生物医药人才", "生物医药产业人才")),
            new PolicyAliasMapping("厦门市新能源和新材料产业人才项目实施办法", List.of("新能源和新材料人才", "新能源和新材料产业人才", "新能源新材料人才")),
            new PolicyAliasMapping("厦门市文旅创意产业人才项目实施办法", List.of("文旅创意人才", "文旅创意产业人才")),
            new PolicyAliasMapping("厦门市海洋经济人才项目实施办法", List.of("海洋经济人才", "海洋经济产业人才")),
            new PolicyAliasMapping("厦门市重点产业骨干人才项目实施办法", List.of("重点产业骨干人才")),
            new PolicyAliasMapping("厦门市教育人才项目实施办法", List.of("教育人才")),
            new PolicyAliasMapping("厦门市卫生健康人才项目实施办法", List.of("卫生健康人才")),
            new PolicyAliasMapping("厦门市社会工作人才项目实施办法", List.of("社会工作人才")),
            new PolicyAliasMapping("厦门市台湾特聘专家（专才）项目实施办法", List.of("台湾特聘专家", "台湾专才", "台湾特聘专才")),
            new PolicyAliasMapping("厦门市支持人工智能领域人才发展的若干措施", List.of("人工智能人才", "AI人才", "ai人才")),
            new PolicyAliasMapping("关于进一步加强博士后工作的若干措施", List.of("博士后")),
            new PolicyAliasMapping("厦门市引进高层次人才住房补贴实施意见", List.of("住房", "住房补贴", "住房类人才政策"))
    );

    private final AgentSessionMapper agentSessionMapper;
    private final AgentMessageMapper agentMessageMapper;
    private final AgentUserPreferenceMapper agentUserPreferenceMapper;
    private final SkillVersionMapper skillVersionMapper;
    private final ProviderConfigMapper providerConfigMapper;
    private final KnowledgeChunkMapper knowledgeChunkMapper;
    private final AiTokenCipherSupport aiTokenCipherSupport;
    private final OpenAiCompatibleChatClient chatClient;
    private final CurrentUserFacade currentUserFacade;
    private final PermissionService permissionService;
    private final AiPermissionService aiPermissionService;
    private final OperationLogService operationLogService;
    private final AiAgentUsageMapper aiAgentUsageMapper;
    private final ParamService paramService;
private final PolicyRouteService policyRouteService;
private final PolicyCatalogService policyCatalogService;
private final AiPolicyConsultService aiPolicyConsultService;
private final AiUserProfileService aiUserProfileService;
private static final String SOURCE_SCENE_AI_WORKBENCH = "AI_WORKBENCH";
    private static final String SOURCE_SCENE_MOBILE_POLICY_CONSULTANT = "MOBILE_POLICY_CONSULTANT";
    private final LogCenterService logCenterService;

    public AgentServiceImpl(AgentSessionMapper agentSessionMapper,
                            AgentMessageMapper agentMessageMapper,
                            AgentUserPreferenceMapper agentUserPreferenceMapper,
                            SkillVersionMapper skillVersionMapper,
                            ProviderConfigMapper providerConfigMapper,
                            KnowledgeChunkMapper knowledgeChunkMapper,
                            AiTokenCipherSupport aiTokenCipherSupport,
                            OpenAiCompatibleChatClient chatClient,
                            CurrentUserFacade currentUserFacade,
                            PermissionService permissionService,
                            AiPermissionService aiPermissionService,
                            OperationLogService operationLogService,
                            LogCenterService logCenterService,
                            AiAgentUsageMapper aiAgentUsageMapper,
                            ParamService paramService,
                            PolicyRouteService policyRouteService,
                            PolicyCatalogService policyCatalogService,
                            AiPolicyConsultService aiPolicyConsultService,
                            AiUserProfileService aiUserProfileService) {
        this.agentSessionMapper = agentSessionMapper;
        this.agentMessageMapper = agentMessageMapper;
        this.agentUserPreferenceMapper = agentUserPreferenceMapper;
        this.skillVersionMapper = skillVersionMapper;
        this.providerConfigMapper = providerConfigMapper;
        this.knowledgeChunkMapper = knowledgeChunkMapper;
        this.aiTokenCipherSupport = aiTokenCipherSupport;
        this.chatClient = chatClient;
        this.currentUserFacade = currentUserFacade;
        this.permissionService = permissionService;
        this.aiPermissionService = aiPermissionService;
        this.operationLogService = operationLogService;
        this.logCenterService = logCenterService;
        this.aiAgentUsageMapper = aiAgentUsageMapper;
        this.paramService = paramService;
        this.policyRouteService = policyRouteService;
        this.policyCatalogService = policyCatalogService;
        this.aiPolicyConsultService = aiPolicyConsultService;
        this.aiUserProfileService = aiUserProfileService;
    }

    @Override
    @Transactional
    public Object createSession(CreateAgentSessionRequest request) {
        LoginUser user = currentUserFacade.currentLoginUser();
        requireAgentPermission(user);
        requireSkillUse(user, request.getSkillId());
        SkillVersionDetailVO version = requirePublishedVersion(request.getSkillId());
        Long baseId = request.getBaseId() != null ? request.getBaseId() : version.getBaseId();
        if (baseId == null) {
            throw new IllegalArgumentException("skill is not bound to a knowledge base");
        }
        requireKnowledgeAnalyze(user, baseId);

        String requestSourceScene = request == null ? null : request.getSourceScene();
        String sourceScene;
        if (requestSourceScene == null || requestSourceScene.trim().isEmpty()) {
            sourceScene = "AI_WORKBENCH";
        } else {
            String normalizedSourceScene = requestSourceScene.trim().toUpperCase().replace('-', '_').replace(' ', '_');
            if ("POLICY_CONSULTANT".equals(normalizedSourceScene) || "MOBILE_POLICY_CONSULTANT".equals(normalizedSourceScene)) {
                sourceScene = "MOBILE_POLICY_CONSULTANT";
            } else {
                sourceScene = "AI_WORKBENCH";
            }
        }

        AgentSessionEntity entity = new AgentSessionEntity();
        entity.setUserId(user.getUserId());
        entity.setSkillId(request.getSkillId());
        entity.setSkillVersionId(version.getId());
        entity.setProviderConfigId(version.getProviderConfigId());
        entity.setModelCode(version.getModelCode());
        entity.setBaseId(baseId);
        entity.setSessionTitle(DEFAULT_SESSION_TITLE);
        entity.setSourceScene(sourceScene);
        entity.setStatus(STATUS_ACTIVE);
        entity.setCreateTime(LocalDateTime.now());
        

        agentSessionMapper.insert(entity);
        logCenterService.recordAiChainSuccess(
                LOG_EVENT_SESSION_CREATE,
                entity.getId(),
                user.getUserId(),
                sourceScene,
                "sessionId=" + entity.getId() + ", skillId=" + entity.getSkillId() + ", baseId=" + entity.getBaseId()
        );
        return agentSessionMapper.queryDetail(entity.getId());
    }

    @Override
    public Object querySessions(AgentSessionQueryRequest request) {
        return queryAuthorizedSessions(request);
    }

    @Override
    public Object querySessionStats(AgentSessionQueryRequest request) {
        List<AgentSessionVO> list = queryAuthorizedSessions(request);
        AgentSessionStatsVO stats = new AgentSessionStatsVO();
        stats.setTotalSessionCount(list.size());

        int activeCount = 0;
        int todayCount = 0;
        int totalMessageCount = 0;
        Set<Long> skillIds = new HashSet<>();
        LocalDate today = LocalDate.now();

        for (AgentSessionVO item : list) {
            if (STATUS_ACTIVE.equalsIgnoreCase(item.getStatus())) {
                activeCount++;
            }
            if (item.getCreateTime() != null && today.equals(item.getCreateTime().toLocalDate())) {
                todayCount++;
            }
            totalMessageCount += item.getMessageCount() == null ? 0 : item.getMessageCount();
            if (item.getSkillId() != null) {
                skillIds.add(item.getSkillId());
            }
        }

        stats.setActiveSessionCount(activeCount);
        stats.setTodaySessionCount(todayCount);
        stats.setTotalMessageCount(totalMessageCount);
        stats.setDistinctSkillCount(skillIds.size());
        return stats;
    }

    @Override
    public Object querySessionTrend(AgentSessionQueryRequest request) {
        AgentSessionQueryRequest safeRequest = normalizeTrendRequest(buildAuthorizedRequest(request));
        AgentSessionTrendVO trend = new AgentSessionTrendVO();
        trend.setDailySessions(fillMissingDays(agentSessionMapper.queryDailyTrend(safeRequest), safeRequest.getStartDate(), safeRequest.getEndDate()));
        trend.setSkillRanking(agentSessionMapper.querySkillRanking(safeRequest));
        trend.setUserRanking(agentSessionMapper.queryUserRanking(safeRequest));
        return trend;
    }

    @Override
    public Object queryMonthlyReport(AgentSessionQueryRequest request) {
        AgentSessionQueryRequest safeRequest = normalizeMonthlyRequest(buildAuthorizedRequest(request));
        AgentMonthlySummaryVO summary = agentSessionMapper.queryMonthlySummary(safeRequest);
        List<AgentTrendPointVO> monthlyTrend = fillMissingMonths(agentSessionMapper.queryMonthlyTrend(safeRequest), safeRequest.getYear());
        List<AgentRankItemVO> skillRanking = agentSessionMapper.queryMonthlySkillRanking(safeRequest);
        List<AgentRankItemVO> baseRanking = agentSessionMapper.queryMonthlyBaseRanking(safeRequest);
        List<AgentRankItemVO> userRanking = agentSessionMapper.queryMonthlyUserRanking(safeRequest);
        List<AgentExpertMetricVO> expertRanking = agentSessionMapper.queryMonthlyExpertRanking(safeRequest);

        AgentSessionQueryRequest previousYearRequest = cloneForYear(safeRequest, safeRequest.getYear() - 1);
        List<AgentTrendPointVO> previousYearTrend = fillMissingMonths(agentSessionMapper.queryMonthlyTrend(previousYearRequest), previousYearRequest.getYear());
        int currentMonth = LocalDate.now().getMonthValue();
        int currentMonthValue = valueForMonth(monthlyTrend, safeRequest.getYear(), currentMonth);
        int previousMonthValue = valueForPreviousMonth(monthlyTrend, previousYearTrend, safeRequest.getYear(), currentMonth);
        int previousYearSameMonthValue = valueForMonth(previousYearTrend, safeRequest.getYear() - 1, currentMonth);

        AgentMonthlyReportVO report = new AgentMonthlyReportVO();
        report.setYear(safeRequest.getYear());
        report.setTotalSessionCount(safeInt(summary == null ? null : summary.getTotalSessionCount()));
        report.setTotalMessageCount(safeInt(summary == null ? null : summary.getTotalMessageCount()));
        report.setAssistantMessageCount(safeInt(summary == null ? null : summary.getAssistantMessageCount()));
        report.setCitedMessageCount(safeInt(summary == null ? null : summary.getCitedMessageCount()));
        report.setCitationHitRate(rate(report.getCitedMessageCount(), report.getAssistantMessageCount()));
        report.setMonthlySessions(monthlyTrend);
        report.setPreviousYearMonthlySessions(previousYearTrend);
        report.setSkillRanking(skillRanking == null ? List.of() : skillRanking);
        report.setBaseRanking(baseRanking == null ? List.of() : baseRanking);
        report.setUserRanking(userRanking == null ? List.of() : userRanking);
        report.setExpertRanking(expertRanking == null ? List.of() : expertRanking);
        report.setActiveMonthCount((int) monthlyTrend.stream().filter(item -> item.getValue() != null && item.getValue() > 0).count());
        report.setAverageMonthlySessions(Math.round(report.getTotalSessionCount() / 12.0f));
        report.setCurrentMonthSessionCount(currentMonthValue);
        report.setMonthOverMonthRate(compareRate(currentMonthValue, previousMonthValue));
        report.setYearOverYearRate(compareRate(currentMonthValue, previousYearSameMonthValue));
        report.setTopSkillLabel(report.getSkillRanking().isEmpty() ? "-" : report.getSkillRanking().get(0).getLabel());
        report.setTopBaseLabel(report.getBaseRanking().isEmpty() ? "-" : report.getBaseRanking().get(0).getLabel());
        report.setTopExpertLabel(report.getExpertRanking().isEmpty() ? "-" : report.getExpertRanking().get(0).getLabel());
        return report;
    }

    @Override
    public byte[] exportMonthlyReportExcel(AgentSessionQueryRequest request) {
        AgentMonthlyReportVO report = (AgentMonthlyReportVO) queryMonthlyReport(request);
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            writeMonthlyOverviewSheet(workbook, report);
            writeTrendSheet(workbook, report.getMonthlySessions());
            writeTrendCompareSheet(workbook, report.getMonthlySessions(), report.getPreviousYearMonthlySessions());
            writeRankSheet(workbook, "skill-ranking", report.getSkillRanking());
            writeRankSheet(workbook, "knowledge-base-ranking", report.getBaseRanking());
            writeRankSheet(workbook, "user-ranking", report.getUserRanking());
            writeExpertRankSheet(workbook, report.getExpertRanking());
            workbook.write(outputStream);
            operationLogService.log("AGENT", "EXPORT_MONTHLY_REPORT", 0L,
                    "export monthly report year=" + report.getYear() + ", totalSessions=" + report.getTotalSessionCount());
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("failed to export monthly report", ex);
        }
    }
    @Override
    public byte[] exportSessions(AgentSessionQueryRequest request) {
        List<AgentSessionVO> list = queryAuthorizedSessions(request);
        StringBuilder builder = new StringBuilder();
        builder.append('\uFEFF');
        builder.append("Session ID,Session Title,Status,Username,Real Name,Skill,Knowledge Base,Model,Message Count,Last Message Time,Create Time\r\n");
        for (AgentSessionVO item : list) {
            builder.append(csvValue(item.getId())).append(',')
                    .append(csvValue(item.getSessionTitle())).append(',')
                    .append(csvValue(item.getStatus())).append(',')
                    .append(csvValue(item.getUsername())).append(',')
                    .append(csvValue(item.getRealName())).append(',')
                    .append(csvValue(item.getSkillName())).append(',')
                    .append(csvValue(item.getBaseName())).append(',')
                    .append(csvValue(item.getModelCode())).append(',')
                    .append(csvValue(item.getMessageCount())).append(',')
                    .append(csvValue(formatDateTime(item.getLastMessageTime()))).append(',')
                    .append(csvValue(formatDateTime(item.getCreateTime())))
                    .append("\r\n");
        }
        operationLogService.log("AGENT", "EXPORT_SESSION_LEDGER", 0L, buildExportLogContent("csv", request, list.size()));
        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public byte[] exportSessionsExcel(AgentSessionQueryRequest request) {
        List<AgentSessionVO> list = queryAuthorizedSessions(request);
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("session-ledger");
            Row headerRow = sheet.createRow(0);
            String[] headers = {"Session ID", "Session Title", "Status", "Username", "Real Name", "Skill", "Knowledge Base", "Model", "Message Count", "Last Message Time", "Create Time"};
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }
            for (int i = 0; i < list.size(); i++) {
                AgentSessionVO item = list.get(i);
                Row row = sheet.createRow(i + 1);
                row.createCell(0).setCellValue(valueOrBlank(item.getId()));
                row.createCell(1).setCellValue(valueOrBlank(item.getSessionTitle()));
                row.createCell(2).setCellValue(valueOrBlank(item.getStatus()));
                row.createCell(3).setCellValue(valueOrBlank(item.getUsername()));
                row.createCell(4).setCellValue(valueOrBlank(item.getRealName()));
                row.createCell(5).setCellValue(valueOrBlank(item.getSkillName()));
                row.createCell(6).setCellValue(valueOrBlank(item.getBaseName()));
                row.createCell(7).setCellValue(valueOrBlank(item.getModelCode()));
                row.createCell(8).setCellValue(valueOrBlank(item.getMessageCount()));
                row.createCell(9).setCellValue(formatDateTime(item.getLastMessageTime()));
                row.createCell(10).setCellValue(formatDateTime(item.getCreateTime()));
            }
            autoSizeColumns(sheet, headers.length);
            workbook.write(outputStream);
            operationLogService.log("AGENT", "EXPORT_SESSION_LEDGER", 0L, buildExportLogContent("excel", request, list.size()));
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("failed to export consultation ledger", ex);
        }
    }

    @Override
    @Transactional
    public void updateSessionStatus(UpdateAgentSessionStatusRequest request) {
        AgentSessionEntity session = requireSession(request.getSessionId());
        LoginUser user = currentUserFacade.currentLoginUser();
        if (!permissionService.isSuperAdmin(user.getUserId()) && !session.getUserId().equals(user.getUserId())) {
            throw new IllegalArgumentException("no permission to update this session");
        }
        requireSkillUse(user, session.getSkillId());
        String status = normalizeStatus(request.getStatus());
        agentSessionMapper.updateStatus(session.getId(), status);
        operationLogService.log("AGENT", "UPDATE_SESSION_STATUS", session.getId(), "update session status to " + status);
    }

    @Override
    @Transactional
    public Object chat(AgentChatRequest request) {
        LoginUser user = currentUserFacade.currentLoginUser();
        requireAgentPermission(user);
        AgentSessionEntity session = requireSession(request.getSessionId());
        if (!permissionService.isSuperAdmin(user.getUserId()) && !session.getUserId().equals(user.getUserId())) {
            throw new IllegalArgumentException("no permission to access this session");
        }
        if (!STATUS_ACTIVE.equalsIgnoreCase(session.getStatus())) {
            throw new IllegalArgumentException("session is archived and cannot continue chatting");
        }
        requireSkillUse(user, session.getSkillId());
        requireKnowledgeAnalyze(user, session.getBaseId());

        SkillVersionEntity version = requireVersion(session.getSkillVersionId());
String sourceScene = normalizeSourceScene(request == null ? null : request.getSourceScene(), session.getSourceScene());

String systemMetaAnswer = resolveSystemMetaAnswer(request.getQuestion(), session, version);
if (normalizeText(systemMetaAnswer) != null) {
    return buildAndSaveSystemMetaChatResult(
            request,
            user,
            session,
            sourceScene,
            systemMetaAnswer,
            resolveCurrentModelCodeForDisplay(session, version)
    );
}

AiPolicyConsultService.ConsultResult policyConsultResult =
        aiPolicyConsultService.consult(session.getId(), user.getUserId(), session.getBaseId(), request.getQuestion(), sourceScene);

KnowledgeCitationContext context = resolvePolicyConsultContextOrSearch(
        session,
        request.getQuestion(),
        sourceScene,
        policyConsultResult
);

PolicyQuestionType questionType = detectPolicyQuestionType(request.getQuestion());
String regionScope = detectPreferredRegionScope(request.getQuestion(), questionType);
AgentUserPreferenceEntity preference = agentUserPreferenceMapper.findByUserId(user.getUserId());
boolean canUseAi = permissionService.isSuperAdmin(user.getUserId()) || aiPermissionService.canUseAi(user.getUserId());
ProviderResolution providerResolution = canUseAi ? resolveProvider(session, version) : null;

String fastPathAnswer = shouldUsePolicyConsultAsFastPath(policyConsultResult, request.getQuestion())
        ? policyConsultResult.answer()
        : resolveFastPathAnswer(request.getQuestion(), questionType, regionScope, sourceScene, session.getBaseId(), context, providerResolution);

        AgentMessageEntity userMessage = new AgentMessageEntity();
        userMessage.setSessionId(session.getId());
        userMessage.setMessageRole("user");
        userMessage.setMessageText(request.getQuestion());
        userMessage.setCreateTime(LocalDateTime.now());
        agentMessageMapper.insert(userMessage);

safeExtractUserProfile(
        user.getUserId(),
        session.getBaseId(),
        sourceScene,
        session.getId(),
        userMessage.getId(),
        request.getQuestion()
);

logCenterService.recordAiChainSuccess(
        LOG_EVENT_AI_CHAT,
                session.getId(),
                user.getUserId(),
                sourceScene,
                "sessionId=" + session.getId() + ", questionLength=" + safeLength(request.getQuestion())
        );
        logCenterService.recordAiChainSuccess(
                LOG_EVENT_KNOWLEDGE_HIT,
                session.getId(),
                user.getUserId(),
                sourceScene,
                "sessionId=" + session.getId() + ", knowledgeHits=" + context.getChunks().size()
        );

        String answer;
        OpenAiCompatibleChatClient.ChatResult chatResult = null;
        String providerName = null;
        String modelCode = providerResolution == null ? normalizeText(session.getModelCode()) : normalizeText(providerResolution.modelCode());
        String usageSource;
        String answerSource;
        boolean noEvidenceForLlm = shouldBlockLlmBecauseNoPolicyEvidence(sourceScene, context, request.getQuestion());

if (normalizeText(fastPathAnswer) != null) {
    answer = fastPathAnswer;
    usageSource = USAGE_SOURCE_FAST_PATH_STRUCTURED;
    answerSource = USAGE_SOURCE_FAST_PATH_STRUCTURED;
    modelCode = MODEL_CODE_FAST_PATH;
    logCenterService.recordAiChainSuccess(
            LOG_EVENT_FAST_PATH_HIT,
            session.getId(),
            user.getUserId(),
            sourceScene,
            "sessionId=" + session.getId()
                    + ", questionType=" + questionType
                    + ", sourceScene=" + sourceScene
                    + ", answerSource=" + USAGE_SOURCE_FAST_PATH_STRUCTURED
    );
} else if (providerResolution == null || noEvidenceForLlm) {
    usageSource = noEvidenceForLlm
            ? USAGE_SOURCE_FALLBACK_NO_POLICY_EVIDENCE
            : (canUseAi ? USAGE_SOURCE_FALLBACK_NO_PROVIDER : USAGE_SOURCE_FALLBACK_NO_AI_PERMISSION);
    answerSource = usageSource;
    answer = buildFallbackAnswer(
            session.getBaseId(),
            request.getQuestion(),
            context,
            preference,
            sourceScene,
            noEvidenceForLlm
                    ? "当前知识库暂未检索到足够政策依据，未调用外部 AI 生成，避免无依据回答。"
                    : (canUseAi ? FALLBACK_REASON_NO_PROVIDER : FALLBACK_REASON_NO_AI_PERMISSION)
    );
} else {
            validateMonthlyTokenQuota(user.getUserId());
            providerName = resolveProviderName(providerResolution.provider());
            try {
                String apiToken = aiTokenCipherSupport.decrypt(providerResolution.provider().getApiTokenCipher());
                String userProfilePromptSummary = safeBuildUserProfilePromptSummary(
        user.getUserId(),
        session.getBaseId(),
        sourceScene,
        request.getQuestion()
);

String conversationContextPrompt = buildConversationContextPrompt(
        session.getId(),
        userMessage.getId()
);

String repeatedQuestionHint = buildRepeatedQuestionHint(
        session.getId(),
        userMessage.getId(),
        request.getQuestion()
);

String systemPrompt = buildSystemPrompt(version, preference, request.getQuestion(), sourceScene);
String userPrompt = buildPolicyAwareUserPrompt(version, request.getQuestion(), context, preference, sourceScene);

userPrompt = appendConversationContextPrompt(userPrompt, conversationContextPrompt);
userPrompt = appendConversationContextPrompt(userPrompt, repeatedQuestionHint);
userPrompt = appendUserProfilePrompt(userPrompt, userProfilePromptSummary);

chatResult = chatClient.chatForUsage(
        providerResolution.provider().getApiBaseUrl(),
        apiToken,
        providerResolution.modelCode(),
        systemPrompt,
        userPrompt
);
                answer = chatResult.getContent();
                usageSource = normalizeText(chatResult.getUsageSource()) == null ? "PROVIDER_NO_USAGE" : chatResult.getUsageSource();
                answerSource = ANSWER_SOURCE_LLM_PROVIDER;
                logCenterService.recordAiChainSuccess(
                        LOG_EVENT_PROVIDER_CALL_SUCCESS,
                        session.getId(),
                        user.getUserId(),
                        sourceScene,
                        "sessionId=" + session.getId() + ", provider=" + valueOrBlank(providerName) + ", model=" + valueOrBlank(providerResolution.modelCode())
                                + ", totalTokens=" + safeUsageInt(chatResult.getTotalTokens())
                                + ", durationMs=" + safeUsageLong(chatResult.getDurationMs())
                );
            } catch (Exception ex) {
                usageSource = USAGE_SOURCE_FALLBACK_PROVIDER_FAILED;
                answerSource = usageSource;
                logCenterService.recordAiChainFailed(
                        LOG_EVENT_PROVIDER_CALL_FAILED,
                        session.getId(),
                        user.getUserId(),
                        sourceScene,
                        "sessionId=" + session.getId() + ", provider=" + valueOrBlank(providerName) + ", model=" + valueOrBlank(providerResolution.modelCode())
                                + ", error=" + valueOrBlank(normalizeText(ex.getMessage()))
                );
                answer = buildFallbackAnswer(session.getBaseId(), request.getQuestion(), context, preference,
                        sourceScene,
                        FALLBACK_REASON_AI_UNAVAILABLE + "原因：" + valueOrBlank(normalizeText(ex.getMessage())));
            }
        }
answer = applyQuestionSubjectTone(answer, request.getQuestion());
        AgentMessageEntity assistantMessage = new AgentMessageEntity();
        assistantMessage.setSessionId(session.getId());
        assistantMessage.setMessageRole("assistant");
        assistantMessage.setMessageText(answer);
        assistantMessage.setCitedChunkIds(context.toChunkIds());
assistantMessage.setCreateTime(LocalDateTime.now());
agentMessageMapper.insert(assistantMessage);

logCenterService.recordAiChainSuccess(
                LOG_EVENT_ASSISTANT_MESSAGE_SAVED,
                assistantMessage.getId(),
                user.getUserId(),
                sourceScene,
                "sessionId=" + session.getId() + ", messageId=" + assistantMessage.getId()
        );

        AiAgentUsageEntity usageEntity = buildUsageEntity(
                session.getId(),
                assistantMessage.getId(),
                user.getUserId(),
                sourceScene,
                providerName,
                modelCode,
                chatResult,
                usageSource
        );
        aiAgentUsageMapper.insert(usageEntity);
        Long monthTotalTokens = safeUsageLong(aiAgentUsageMapper.sumMonthTotalTokensByUserId(user.getUserId()));

        if (DEFAULT_SESSION_TITLE.equals(session.getSessionTitle())) {
            String newTitle = buildSessionTitle(request.getQuestion());
            session.setSessionTitle(newTitle);
            agentSessionMapper.updateTitle(session.getId(), newTitle);
        }

        updateUserPreference(user.getUserId(), request.getQuestion(), preference);
        operationLogService.log("AGENT", "CHAT", session.getId(), "chat in AI workbench");

        AgentChatResultVO result = new AgentChatResultVO();
        result.setSessionId(session.getId());
        result.setSourceScene(sourceScene);
        result.setAnswer(answer);
        result.setCitedChunkIds(context.toChunkIds());
        result.setCitedChunkIdList(context.toChunkIds().isEmpty() ? List.of() : Arrays.asList(context.toChunkIds().split(",")));
        result.setCitedTitles(context.getChunks().stream().map(KnowledgeSearchResultVO::getDocTitle).distinct().collect(Collectors.toList()));
        result.setPromptTokens(usageEntity.getPromptTokens());
        result.setCompletionTokens(usageEntity.getCompletionTokens());
        result.setTotalTokens(usageEntity.getTotalTokens());
        result.setDurationMs(usageEntity.getDurationMs());
        result.setMonthTotalTokens(monthTotalTokens);
        result.setModelCode(usageEntity.getModelCode());
        logCenterService.recordAiChainSuccess(
                LOG_EVENT_RESULT_FLOW_READY,
                session.getId(),
                user.getUserId(),
                sourceScene,
                "sessionId=" + session.getId()
                        + ", answerSource=" + valueOrBlank(answerSource)
                        + ", knowledgeHits=" + context.getChunks().size()
                        + ", modelCode=" + valueOrBlank(usageEntity.getModelCode())
                        + ", monthTotalTokens=" + monthTotalTokens
                        + ", totalTokens=" + usageEntity.getTotalTokens()
        );
        return result;
    }

    @Override
public SseEmitter chatStream(AgentChatRequest request) {
    LoginUser user = currentUserFacade.currentLoginUser();
    requireAgentPermission(user);

    AgentSessionEntity session = requireSession(request.getSessionId());
    if (!permissionService.isSuperAdmin(user.getUserId()) && !session.getUserId().equals(user.getUserId())) {
        throw new IllegalArgumentException("no permission to access this session");
    }

    if (!STATUS_ACTIVE.equalsIgnoreCase(session.getStatus())) {
        throw new IllegalArgumentException("session is archived and cannot continue chatting");
    }

    requireSkillUse(user, session.getSkillId());
    requireKnowledgeAnalyze(user, session.getBaseId());

    SkillVersionEntity version = requireVersion(session.getSkillVersionId());
    String sourceScene = normalizeSourceScene(
            request == null ? null : request.getSourceScene(),
            session.getSourceScene()
    );

    String systemMetaAnswer = resolveSystemMetaAnswer(request.getQuestion(), session, version);
    if (normalizeText(systemMetaAnswer) != null) {
        SseEmitter emitter = new SseEmitter(0L);
        CompletableFuture.runAsync(() -> executeSystemMetaChatStream(
                emitter,
                request,
                user,
                session,
                sourceScene,
                systemMetaAnswer,
                resolveCurrentModelCodeForDisplay(session, version)
        ));
        return emitter;
    }

    AiPolicyConsultService.ConsultResult policyConsultResult =
            aiPolicyConsultService.consult(
                    session.getId(),
                    user.getUserId(),
                    session.getBaseId(),
                    request.getQuestion(),
                    sourceScene
            );

    KnowledgeCitationContext context = resolvePolicyConsultContextOrSearch(
            session,
            request.getQuestion(),
            sourceScene,
            policyConsultResult
    );

    PolicyQuestionType questionType = detectPolicyQuestionType(request.getQuestion());
    String regionScope = detectPreferredRegionScope(request.getQuestion(), questionType);
    AgentUserPreferenceEntity preference = agentUserPreferenceMapper.findByUserId(user.getUserId());

    boolean canUseAi = permissionService.isSuperAdmin(user.getUserId())
            || aiPermissionService.canUseAi(user.getUserId());

    ProviderResolution providerResolution = canUseAi ? resolveProvider(session, version) : null;

    /*
     * 关键修复：
     * 只要政策咨询新链路已经 applied=true 且 answer 不为空，
     * 就强制作为 fastPathAnswer 传入 executeChatStream。
     * 这样条件索引命中结果不会再被后面的 RAG / 大模型覆盖。
     */
    String policyConsultAnswer = policyConsultResult != null
            && policyConsultResult.applied()
            ? normalizeText(policyConsultResult.answer())
            : null;

    String fastPathAnswer;
    if (policyConsultAnswer != null) {
        fastPathAnswer = policyConsultResult.answer();

       

        if (policyConsultResult.context() != null) {
            context.getChunks().clear();
            context.getChunks().addAll(policyConsultResult.context().getChunks());
        }
    } else {
        fastPathAnswer = resolveFastPathAnswer(
                request.getQuestion(),
                questionType,
                regionScope,
                sourceScene,
                session.getBaseId(),
                context,
                providerResolution
        );

        
    }

    SseEmitter emitter = new SseEmitter(0L);
    CompletableFuture.runAsync(() -> executeChatStream(
            emitter,
            request,
            user,
            session,
            version,
            sourceScene,
            context,
            preference,
            canUseAi,
            providerResolution,
            questionType,
            regionScope,
            fastPathAnswer
    ));

    return emitter;
}

    @Override
    public Object queryMessages(Long sessionId) {
        AgentSessionEntity session = requireSession(sessionId);
        LoginUser user = currentUserFacade.currentLoginUser();
        if (!permissionService.isSuperAdmin(user.getUserId()) && !session.getUserId().equals(user.getUserId())) {
            throw new IllegalArgumentException("no permission to access this session");
        }
        requireSkillUse(user, session.getSkillId());
        List<AgentMessageVO> list = agentMessageMapper.queryBySessionId(sessionId);
        for (AgentMessageVO item : list) {
            item.setCitedChunkIdList(item.getCitedChunkIds() == null || item.getCitedChunkIds().isBlank() ? List.of() : Arrays.asList(item.getCitedChunkIds().split(",")));
        }
        return list;
    }

    private void executeChatStream(SseEmitter emitter,
                                   AgentChatRequest request,
                                   LoginUser user,
                                   AgentSessionEntity session,
                                   SkillVersionEntity version,
                                   String sourceScene,
                                   KnowledgeCitationContext context,
                                   AgentUserPreferenceEntity preference,
                                   boolean canUseAi,
                                   ProviderResolution providerResolution,
                                   PolicyQuestionType questionType,
                                   String regionScope,
                                   String fastPathAnswer) {
        boolean noEvidenceForLlm = shouldBlockLlmBecauseNoPolicyEvidence(sourceScene, context, request.getQuestion());

String answerSource = normalizeText(fastPathAnswer) != null ? USAGE_SOURCE_FAST_PATH_STRUCTURED
        : ((providerResolution == null || noEvidenceForLlm)
        ? ANSWER_SOURCE_KNOWLEDGE_FALLBACK_STRUCTURED
        : ANSWER_SOURCE_LLM_PROVIDER);
        final boolean[] streamDeltaSent = {false};
        try {
            sendStreamStart(emitter, session.getId(), user.getUserId(), sourceScene, answerSource);

            AgentMessageEntity userMessage = new AgentMessageEntity();
            userMessage.setSessionId(session.getId());
            userMessage.setMessageRole("user");
            userMessage.setMessageText(request.getQuestion());
            userMessage.setCreateTime(LocalDateTime.now());
           agentMessageMapper.insert(userMessage);

safeExtractUserProfile(
        user.getUserId(),
        session.getBaseId(),
        sourceScene,
        session.getId(),
        userMessage.getId(),
        request.getQuestion()
);

logCenterService.recordAiChainSuccess(
        LOG_EVENT_AI_CHAT,
                    session.getId(),
                    user.getUserId(),
                    sourceScene,
                    "sessionId=" + session.getId() + ", questionLength=" + safeLength(request.getQuestion()) + ", stream=true"
            );
            logCenterService.recordAiChainSuccess(
                    LOG_EVENT_KNOWLEDGE_HIT,
                    session.getId(),
                    user.getUserId(),
                    sourceScene,
                    "sessionId=" + session.getId() + ", knowledgeHits=" + context.getChunks().size() + ", stream=true"
            );

            String answer;
            String usageSource;
            String providerName = null;
            String modelCode = providerResolution == null ? normalizeText(session.getModelCode()) : normalizeText(providerResolution.modelCode());
            OpenAiCompatibleChatClient.ChatResult chatResult = null;

            if (normalizeText(fastPathAnswer) != null) {
    answer = fastPathAnswer;
    usageSource = USAGE_SOURCE_FAST_PATH_STRUCTURED;
    modelCode = MODEL_CODE_FAST_PATH;
    answerSource = USAGE_SOURCE_FAST_PATH_STRUCTURED;

    

    /*
     * 关键修复：
     * fastPathAnswer 已经是确定答案，直接推送给前端。
     * 否则前端会一直等流式 delta，或者后续被 provider stream 覆盖。
     */
    streamDeltaSent[0] = emitStreamAnswer(
            emitter,
            session.getId(),
            user.getUserId(),
            sourceScene,
            answerSource,
            answer,
            splitStructuredStreamSegments(answer)
    ) > 0 || streamDeltaSent[0];

    logCenterService.recordAiChainSuccess(
            LOG_EVENT_FAST_PATH_HIT,
            session.getId(),
            user.getUserId(),
            sourceScene,
            "sessionId=" + session.getId()
                    + ", questionType=" + questionType
                    + ", sourceScene=" + sourceScene
                    + ", answerSource=" + USAGE_SOURCE_FAST_PATH_STRUCTURED
                    + ", stream=true"
                    + ", directStreamSent=" + streamDeltaSent[0]
    );
} else if (providerResolution == null || noEvidenceForLlm) {
    usageSource = noEvidenceForLlm
            ? USAGE_SOURCE_FALLBACK_NO_POLICY_EVIDENCE
            : (canUseAi ? USAGE_SOURCE_FALLBACK_NO_PROVIDER : USAGE_SOURCE_FALLBACK_NO_AI_PERMISSION);
    answerSource = ANSWER_SOURCE_KNOWLEDGE_FALLBACK_STRUCTURED;
    answer = buildFallbackAnswer(
            session.getBaseId(),
            request.getQuestion(),
            context,
            preference,
            sourceScene,
            noEvidenceForLlm
                    ? "当前知识库暂未检索到足够政策依据，未调用外部 AI 生成，避免无依据回答。"
                    : (canUseAi ? FALLBACK_REASON_NO_PROVIDER : FALLBACK_REASON_NO_AI_PERMISSION)
    );
} else {
                validateMonthlyTokenQuota(user.getUserId());
                providerName = resolveProviderName(providerResolution.provider());
                String userProfilePromptSummary = safeBuildUserProfilePromptSummary(
        user.getUserId(),
        session.getBaseId(),
        sourceScene,
        request.getQuestion()
);

String conversationContextPrompt = buildConversationContextPrompt(
        session.getId(),
        userMessage.getId()
);

String repeatedQuestionHint = buildRepeatedQuestionHint(
        session.getId(),
        userMessage.getId(),
        request.getQuestion()
);

String systemPrompt = buildSystemPrompt(version, preference, request.getQuestion(), sourceScene);
String userPrompt = buildPolicyAwareUserPrompt(version, request.getQuestion(), context, preference, sourceScene);

userPrompt = appendConversationContextPrompt(userPrompt, conversationContextPrompt);
userPrompt = appendConversationContextPrompt(userPrompt, repeatedQuestionHint);
userPrompt = appendUserProfilePrompt(userPrompt, userProfilePromptSummary);

try {
                    String apiToken = aiTokenCipherSupport.decrypt(providerResolution.provider().getApiTokenCipher());
                    final Long finalSessionId = session.getId();
                    final Long finalUserId = user.getUserId();
                    final String finalSourceScene = sourceScene;
                    final String finalProviderName = providerName;
                    final String finalModelCode = providerResolution.modelCode();
                    final StringBuilder streamedAnswer = new StringBuilder();
                    boolean[] nativeStreamUsed = {false};
                    boolean[] nativeDeltaSent = {false};
                    try {
                        if (!chatClient.supportsNativeStream(providerResolution.provider().getApiBaseUrl())) {
                            throw new IllegalArgumentException("provider 未声明原生流式支持");
                        }
                        logCenterService.recordAiChainSuccess(
                                LOG_EVENT_PROVIDER_STREAM_START,
                                finalSessionId,
                                finalUserId,
                                finalSourceScene,
                                "sessionId=" + finalSessionId
                                        + ", provider=" + valueOrBlank(finalProviderName)
                                        + ", model=" + valueOrBlank(finalModelCode)
                        );
                        nativeStreamUsed[0] = true;
                        chatResult = chatClient.streamChat(
                                providerResolution.provider().getApiBaseUrl(),
                                apiToken,
                                providerResolution.modelCode(),
                                systemPrompt,
                                userPrompt,
                                delta -> {
                                    if (normalizeText(delta) == null) {
                                        return;
                                    }
                                    streamedAnswer.append(delta);
                                    nativeDeltaSent[0] = true;
                                    streamDeltaSent[0] = true;
                                    sendStreamDelta(emitter, delta);
                                    logCenterService.recordAiChainSuccess(
                                            LOG_EVENT_PROVIDER_STREAM_DELTA,
                                            finalSessionId,
                                            finalUserId,
                                            finalSourceScene,
                                            "sessionId=" + finalSessionId
                                                    + ", provider=" + valueOrBlank(finalProviderName)
                                                    + ", model=" + valueOrBlank(finalModelCode)
                                                    + ", deltaLength=" + safeLength(delta)
                                    );
                                }
                        );
                        answer = chatResult.getContent();
                        usageSource = normalizeText(chatResult.getUsageSource()) == null
                                ? USAGE_SOURCE_PROVIDER_STREAM_NO_USAGE
                                : chatResult.getUsageSource();
                        answerSource = ANSWER_SOURCE_LLM_PROVIDER;
                        logCenterService.recordAiChainSuccess(
                                LOG_EVENT_PROVIDER_STREAM_DONE,
                                session.getId(),
                                user.getUserId(),
                                sourceScene,
                                "sessionId=" + session.getId()
                                        + ", provider=" + valueOrBlank(providerName)
                                        + ", model=" + valueOrBlank(providerResolution.modelCode())
                                        + ", totalTokens=" + safeUsageInt(chatResult.getTotalTokens())
                                        + ", durationMs=" + safeUsageLong(chatResult.getDurationMs())
                                        + ", usageSource=" + valueOrBlank(usageSource)
                        );
                    } catch (Exception streamEx) {
                        logCenterService.recordAiChainFailed(
                                LOG_EVENT_PROVIDER_STREAM_ERROR,
                                session.getId(),
                                user.getUserId(),
                                sourceScene,
                                "sessionId=" + session.getId()
                                        + ", provider=" + valueOrBlank(providerName)
                                        + ", model=" + valueOrBlank(providerResolution.modelCode())
                                        + ", error=" + valueOrBlank(normalizeText(streamEx.getMessage()))
                        );
                        logCenterService.recordAiChainSuccess(
                                LOG_EVENT_PROVIDER_STREAM_FALLBACK,
                                session.getId(),
                                user.getUserId(),
                                sourceScene,
                                "sessionId=" + session.getId()
                                        + ", provider=" + valueOrBlank(providerName)
                                        + ", model=" + valueOrBlank(providerResolution.modelCode())
                                        + ", fallback=SERVER_SEGMENTED"
                        );
                        chatResult = chatClient.chatForUsage(
                                providerResolution.provider().getApiBaseUrl(),
                                apiToken,
                                providerResolution.modelCode(),
                                systemPrompt,
                                userPrompt
                        );
                        answer = chatResult.getContent();
                        usageSource = USAGE_SOURCE_PROVIDER_STREAM_FALLBACK_SEGMENTED;
                        answerSource = ANSWER_SOURCE_LLM_PROVIDER;
                        String remainingAnswer = resolveRemainingStreamAnswer(answer, streamedAnswer.toString(), nativeDeltaSent[0]);
                        streamDeltaSent[0] = emitStreamAnswer(
                                emitter,
                                session.getId(),
                                user.getUserId(),
                                sourceScene,
                                answerSource,
                                remainingAnswer,
                                splitGenericStreamSegments(remainingAnswer)
                        ) > 0 || streamDeltaSent[0];
                        logCenterService.recordAiChainSuccess(
                                LOG_EVENT_PROVIDER_CALL_SUCCESS,
                                session.getId(),
                                user.getUserId(),
                                sourceScene,
                                "sessionId=" + session.getId() + ", provider=" + valueOrBlank(providerName) + ", model=" + valueOrBlank(providerResolution.modelCode())
                                        + ", totalTokens=" + safeUsageInt(chatResult.getTotalTokens())
                                        + ", durationMs=" + safeUsageLong(chatResult.getDurationMs())
                                        + ", streamMode=SERVER_SEGMENTED"
                        );
                    }
                } catch (Exception ex) {
                    usageSource = USAGE_SOURCE_FALLBACK_PROVIDER_FAILED;
                    answerSource = ANSWER_SOURCE_KNOWLEDGE_FALLBACK_STRUCTURED;
                    logCenterService.recordAiChainFailed(
                            LOG_EVENT_PROVIDER_CALL_FAILED,
                            session.getId(),
                            user.getUserId(),
                            sourceScene,
                            "sessionId=" + session.getId() + ", provider=" + valueOrBlank(providerName) + ", model=" + valueOrBlank(providerResolution.modelCode())
                                    + ", error=" + valueOrBlank(normalizeText(ex.getMessage()))
                                    + ", streamMode=SERVER_SEGMENTED"
                    );
                    answer = buildFallbackAnswer(
                            session.getBaseId(),
                            request.getQuestion(),
                            context,
                            preference,
                            sourceScene,
                            FALLBACK_REASON_AI_UNAVAILABLE + "原因：" + valueOrBlank(normalizeText(ex.getMessage()))
                    );
                }
            }

            if (!streamDeltaSent[0]) {
    answer = applyQuestionSubjectTone(answer, request.getQuestion());
}

answer = ensureNonEmptyStreamAnswer(answer, session.getId(), user.getUserId(), sourceScene, answerSource);

if (!streamDeltaSent[0]) {
                List<String> segments = ANSWER_SOURCE_LLM_PROVIDER.equals(answerSource)
                        ? splitGenericStreamSegments(answer)
                        : splitStructuredStreamSegments(answer);
                streamDeltaSent[0] = emitStreamAnswer(
                        emitter,
                        session.getId(),
                        user.getUserId(),
                        sourceScene,
                        answerSource,
                        answer,
                        segments
                ) > 0;
            }

            AgentMessageEntity assistantMessage = new AgentMessageEntity();
            assistantMessage.setSessionId(session.getId());
            assistantMessage.setMessageRole("assistant");
            assistantMessage.setMessageText(answer);
           assistantMessage.setCitedChunkIds(context.toChunkIds());
assistantMessage.setCreateTime(LocalDateTime.now());
agentMessageMapper.insert(assistantMessage);



logCenterService.recordAiChainSuccess(
                    LOG_EVENT_ASSISTANT_MESSAGE_SAVED,
                    assistantMessage.getId(),
                    user.getUserId(),
                    sourceScene,
                    "sessionId=" + session.getId() + ", messageId=" + assistantMessage.getId() + ", stream=true"
            );

            AiAgentUsageEntity usageEntity = buildUsageEntity(
                    session.getId(),
                    assistantMessage.getId(),
                    user.getUserId(),
                    sourceScene,
                    providerName,
                    modelCode,
                    chatResult,
                    usageSource
            );
            aiAgentUsageMapper.insert(usageEntity);
            Long monthTotalTokens = safeUsageLong(aiAgentUsageMapper.sumMonthTotalTokensByUserId(user.getUserId()));

            if (DEFAULT_SESSION_TITLE.equals(session.getSessionTitle())) {
                String newTitle = buildSessionTitle(request.getQuestion());
                session.setSessionTitle(newTitle);
                agentSessionMapper.updateTitle(session.getId(), newTitle);
            }

            updateUserPreference(user.getUserId(), request.getQuestion(), preference);
            operationLogService.log("AGENT", "CHAT_STREAM", session.getId(), "chat stream in AI workbench");

            AgentChatResultVO result = buildChatResultVO(session, sourceScene, answer, context, usageEntity, monthTotalTokens);
            if (!streamDeltaSent[0]) {
                logCenterService.recordAiChainSuccess(
                        LOG_EVENT_STREAM_DONE_WITHOUT_DELTA,
                        session.getId(),
                        user.getUserId(),
                        sourceScene,
                        "sessionId=" + session.getId() + ", answerSource=" + valueOrBlank(answerSource)
                );
            }
            sendDoneMeta(emitter, result, answerSource);
            sendDoneEvent(emitter, session.getId(), user.getUserId(), sourceScene, answerSource, usageEntity, monthTotalTokens, context.getChunks().size());
            emitter.complete();
        } catch (Exception ex) {
            logCenterService.recordAiChainFailed(
                    LOG_EVENT_STREAM_ERROR,
                    session.getId(),
                    user.getUserId(),
                    sourceScene,
                    "sessionId=" + session.getId() + ", error=" + valueOrBlank(normalizeText(ex.getMessage()))
            );
            sendStreamError(emitter, ex.getMessage());
            emitter.completeWithError(ex);
        }
    }
    private List<AgentSessionVO> queryAuthorizedSessions(AgentSessionQueryRequest request) {
        AgentSessionQueryRequest safeRequest = normalizeDateRange(buildAuthorizedRequest(request));
        List<AgentSessionVO> rawList = agentSessionMapper.queryList(safeRequest);
        LoginUser user = currentUserFacade.currentLoginUser();
        if (permissionService.isSuperAdmin(user.getUserId())) {
            return rawList;
        }
        List<AgentSessionVO> authorizedList = new ArrayList<>();
        for (AgentSessionVO item : rawList) {
            if (item.getSkillId() != null && aiPermissionService.canUseSkill(user.getUserId(), item.getSkillId())) {
                authorizedList.add(item);
            }
        }
        return authorizedList;
    }

    private AgentSessionQueryRequest buildAuthorizedRequest(AgentSessionQueryRequest request) {
        LoginUser user = currentUserFacade.currentLoginUser();
        AgentSessionQueryRequest safeRequest = request == null ? new AgentSessionQueryRequest() : request;
        if (!permissionService.isSuperAdmin(user.getUserId())) {
            safeRequest.setUserId(user.getUserId());
        }
        return safeRequest;
    }

    private AgentSessionQueryRequest normalizeDateRange(AgentSessionQueryRequest request) {
        if (request == null) {
            return new AgentSessionQueryRequest();
        }
        LocalDate startDate = request.getStartDate();
        LocalDate endDate = request.getEndDate();
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            request.setStartDate(endDate);
            request.setEndDate(startDate);
        }
        return request;
    }

    private AgentSessionQueryRequest normalizeTrendRequest(AgentSessionQueryRequest request) {
        AgentSessionQueryRequest safeRequest = normalizeDateRange(request);
        LocalDate startDate = safeRequest.getStartDate();
        LocalDate endDate = safeRequest.getEndDate();
        if (startDate == null && endDate == null) {
            endDate = LocalDate.now();
            startDate = endDate.minusDays(6);
        } else if (startDate == null) {
            startDate = endDate.minusDays(6);
        } else if (endDate == null) {
            endDate = startDate.plusDays(6);
        }
        safeRequest.setStartDate(startDate);
        safeRequest.setEndDate(endDate);
        return safeRequest;
    }

    private AgentSessionQueryRequest normalizeMonthlyRequest(AgentSessionQueryRequest request) {
        AgentSessionQueryRequest safeRequest = normalizeDateRange(request);
        if (safeRequest.getYear() == null) {
            safeRequest.setYear(LocalDate.now().getYear());
        }
        return safeRequest;
    }

    private AgentSessionQueryRequest cloneForYear(AgentSessionQueryRequest request, int year) {
        AgentSessionQueryRequest clone = new AgentSessionQueryRequest();
        clone.setUserId(request.getUserId());
        clone.setSkillId(request.getSkillId());
        clone.setStatus(request.getStatus());
        clone.setKeywords(request.getKeywords());
        clone.setYear(year);
        return clone;
    }

    private List<AgentTrendPointVO> fillMissingDays(List<AgentTrendPointVO> dbPoints, LocalDate startDate, LocalDate endDate) {
        List<AgentTrendPointVO> result = new ArrayList<>();
        if (startDate == null || endDate == null) {
            return result;
        }
        LocalDate cursor = startDate;
        while (!cursor.isAfter(endDate)) {
            String label = cursor.toString();
            Integer value = 0;
            if (dbPoints != null) {
                for (AgentTrendPointVO item : dbPoints) {
                    if (label.equals(item.getLabel())) {
                        value = item.getValue();
                        break;
                    }
                }
            }
            AgentTrendPointVO point = new AgentTrendPointVO();
            point.setLabel(label);
            point.setValue(value);
            result.add(point);
            cursor = cursor.plusDays(1);
        }
        return result;
    }

    private List<AgentTrendPointVO> fillMissingMonths(List<AgentTrendPointVO> dbPoints, Integer year) {
        List<AgentTrendPointVO> result = new ArrayList<>();
        int safeYear = year == null ? LocalDate.now().getYear() : year;
        for (int month = 1; month <= 12; month++) {
            String label = YearMonth.of(safeYear, month).toString();
            Integer value = 0;
            if (dbPoints != null) {
                for (AgentTrendPointVO item : dbPoints) {
                    if (label.equals(item.getLabel())) {
                        value = item.getValue();
                        break;
                    }
                }
            }
            AgentTrendPointVO point = new AgentTrendPointVO();
            point.setLabel(label);
            point.setValue(value);
            result.add(point);
        }
        return result;
    }

    private int valueForMonth(List<AgentTrendPointVO> list, int year, int month) {
        String label = YearMonth.of(year, month).toString();
        for (AgentTrendPointVO item : list) {
            if (label.equals(item.getLabel())) {
                return safeInt(item.getValue());
            }
        }
        return 0;
    }

    private int valueForPreviousMonth(List<AgentTrendPointVO> currentYearList, List<AgentTrendPointVO> previousYearList, int year, int month) {
        YearMonth previous = YearMonth.of(year, month).minusMonths(1);
        List<AgentTrendPointVO> source = previous.getYear() == year ? currentYearList : previousYearList;
        return valueForMonth(source, previous.getYear(), previous.getMonthValue());
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private Double rate(Integer numerator, Integer denominator) {
        if (denominator == null || denominator == 0) {
            return 0D;
        }
        return Math.round((numerator == null ? 0D : numerator * 10000D / denominator)) / 100D;
    }

    private Double compareRate(int currentValue, int previousValue) {
        if (previousValue <= 0) {
            return currentValue > 0 ? 100D : 0D;
        }
        return Math.round(((currentValue - previousValue) * 10000D / previousValue)) / 100D;
    }
    private void writeMonthlyOverviewSheet(XSSFWorkbook workbook, AgentMonthlyReportVO report) {
        Sheet sheet = workbook.createSheet("monthly-overview");
        String[][] rows = {
                {"Year", valueOrBlank(report.getYear())},
                {"Total Sessions", valueOrBlank(report.getTotalSessionCount())},
                {"Total Messages", valueOrBlank(report.getTotalMessageCount())},
                {"Active Months", valueOrBlank(report.getActiveMonthCount())},
                {"Avg Monthly Sessions", valueOrBlank(report.getAverageMonthlySessions())},
                {"Current Month Sessions", valueOrBlank(report.getCurrentMonthSessionCount())},
                {"Month over Month", formatRate(report.getMonthOverMonthRate())},
                {"Year over Year", formatRate(report.getYearOverYearRate())},
                {"Assistant Replies", valueOrBlank(report.getAssistantMessageCount())},
                {"Replies With Citation", valueOrBlank(report.getCitedMessageCount())},
                {"Knowledge Hit Rate", formatRate(report.getCitationHitRate())},
                {"Top Skill", valueOrBlank(report.getTopSkillLabel())},
                {"Top Knowledge Base", valueOrBlank(report.getTopBaseLabel())},
                {"Top Expert", valueOrBlank(report.getTopExpertLabel())}
        };
        for (int i = 0; i < rows.length; i++) {
            Row row = sheet.createRow(i);
            row.createCell(0).setCellValue(rows[i][0]);
            row.createCell(1).setCellValue(rows[i][1]);
        }
        autoSizeColumns(sheet, 2);
    }

    private void writeTrendSheet(XSSFWorkbook workbook, List<AgentTrendPointVO> trendList) {
        Sheet sheet = workbook.createSheet("monthly-trend");
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("Month");
        header.createCell(1).setCellValue("Session Count");
        for (int i = 0; i < trendList.size(); i++) {
            AgentTrendPointVO item = trendList.get(i);
            Row row = sheet.createRow(i + 1);
            row.createCell(0).setCellValue(valueOrBlank(item.getLabel()));
            row.createCell(1).setCellValue(safeInt(item.getValue()));
        }
        autoSizeColumns(sheet, 2);
    }

    private void writeTrendCompareSheet(XSSFWorkbook workbook, List<AgentTrendPointVO> currentYearList, List<AgentTrendPointVO> previousYearList) {
        Sheet sheet = workbook.createSheet("year-compare-trend");
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("Month");
        header.createCell(1).setCellValue("Current Year Sessions");
        header.createCell(2).setCellValue("Previous Year Sessions");
        int size = Math.max(currentYearList == null ? 0 : currentYearList.size(), previousYearList == null ? 0 : previousYearList.size());
        for (int i = 0; i < size; i++) {
            AgentTrendPointVO current = currentYearList != null && i < currentYearList.size() ? currentYearList.get(i) : null;
            AgentTrendPointVO previous = previousYearList != null && i < previousYearList.size() ? previousYearList.get(i) : null;
            Row row = sheet.createRow(i + 1);
            row.createCell(0).setCellValue(current != null ? valueOrBlank(current.getLabel()) : valueOrBlank(previous == null ? "" : previous.getLabel()));
            row.createCell(1).setCellValue(current == null ? 0 : safeInt(current.getValue()));
            row.createCell(2).setCellValue(previous == null ? 0 : safeInt(previous.getValue()));
        }
        autoSizeColumns(sheet, 3);
    }

    private void writeExpertRankSheet(XSSFWorkbook workbook, List<AgentExpertMetricVO> list) {
        Sheet sheet = workbook.createSheet("expert-ranking");
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("Expert");
        header.createCell(1).setCellValue("Expert Level");
        header.createCell(2).setCellValue("Session Count");
        header.createCell(3).setCellValue("Message Count");
        header.createCell(4).setCellValue("Assistant Replies");
        header.createCell(5).setCellValue("Replies With Citation");
        header.createCell(6).setCellValue("Citation Hit Rate");
        for (int i = 0; i < list.size(); i++) {
            AgentExpertMetricVO item = list.get(i);
            Row row = sheet.createRow(i + 1);
            row.createCell(0).setCellValue(valueOrBlank(item.getLabel()));
            row.createCell(1).setCellValue(valueOrBlank(item.getExpertLevel()));
            row.createCell(2).setCellValue(safeInt(item.getSessionCount()));
            row.createCell(3).setCellValue(safeInt(item.getMessageCount()));
            row.createCell(4).setCellValue(safeInt(item.getAssistantMessageCount()));
            row.createCell(5).setCellValue(safeInt(item.getCitedMessageCount()));
            row.createCell(6).setCellValue(formatRate(item.getCitationHitRate()));
        }
        autoSizeColumns(sheet, 7);
    }

    private void writeRankSheet(XSSFWorkbook workbook, String sheetName, List<AgentRankItemVO> list) {
        Sheet sheet = workbook.createSheet(sheetName);
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("Name");
        header.createCell(1).setCellValue("Session Count");
        header.createCell(2).setCellValue("Message Count");
        for (int i = 0; i < list.size(); i++) {
            AgentRankItemVO item = list.get(i);
            Row row = sheet.createRow(i + 1);
            row.createCell(0).setCellValue(valueOrBlank(item.getLabel()));
            row.createCell(1).setCellValue(safeInt(item.getSessionCount()));
            row.createCell(2).setCellValue(safeInt(item.getMessageCount()));
        }
        autoSizeColumns(sheet, 3);
    }

    private void autoSizeColumns(Sheet sheet, int columnCount) {
        for (int i = 0; i < columnCount; i++) {
            sheet.autoSizeColumn(i);
            sheet.setColumnWidth(i, Math.min(sheet.getColumnWidth(i) + 1024, 18000));
        }
    }

    private SkillVersionDetailVO requirePublishedVersion(Long skillId) {
        SkillVersionDetailVO detail = skillVersionMapper.queryLatestPublishedBySkillId(skillId);
        if (detail == null) {
            throw new IllegalArgumentException("published skill version not found");
        }
        return detail;
    }

    private SkillVersionEntity requireVersion(Long versionId) {
        SkillVersionEntity entity = skillVersionMapper.findById(versionId);
        if (entity == null) {
            throw new IllegalArgumentException("skill version not found");
        }
        return entity;
    }

    private AgentSessionEntity requireSession(Long sessionId) {
        AgentSessionEntity entity = agentSessionMapper.findById(sessionId);
        if (entity == null) {
            throw new IllegalArgumentException("session not found");
        }
        return entity;
    }

    private ProviderResolution resolveProvider(AgentSessionEntity session, SkillVersionEntity version) {
        ProviderConfigEntity provider = findUsableProvider(session == null ? null : session.getProviderConfigId());
        String modelCode = normalizeText(session == null ? null : session.getModelCode());
        if (provider != null) {
            if (modelCode == null) {
                modelCode = normalizeText(provider.getDefaultModel());
            }
            if (modelCode != null) {
                return new ProviderResolution(provider, modelCode);
            }
        }

        provider = findUsableProvider(version == null ? null : version.getProviderConfigId());
        modelCode = normalizeText(version == null ? null : version.getModelCode());
        if (provider != null) {
            if (modelCode == null) {
                modelCode = normalizeText(provider.getDefaultModel());
            }
            if (modelCode != null) {
                return new ProviderResolution(provider, modelCode);
            }
        }

        provider = providerConfigMapper.findFirstEnabledSuccess();
        if (!isProviderUsable(provider)) {
            return null;
        }
        modelCode = normalizeText(provider.getDefaultModel());
        if (modelCode == null) {
            return null;
        }
        return new ProviderResolution(provider, modelCode);
    }

    private ProviderConfigEntity findUsableProvider(Long providerId) {
        if (providerId == null) {
            return null;
        }
        ProviderConfigEntity provider = providerConfigMapper.findById(providerId);
        return isProviderUsable(provider) ? provider : null;
    }

    private boolean isProviderUsable(ProviderConfigEntity provider) {
        return provider != null
                && Integer.valueOf(1).equals(provider.getStatus())
                && "SUCCESS".equalsIgnoreCase(normalizeText(provider.getConnectStatus()))
                && normalizeText(provider.getApiBaseUrl()) != null
                && normalizeText(provider.getApiTokenCipher()) != null;
    }

    private String normalizeSourceScene(String requestSourceScene, String sessionSourceScene) {
        String candidate = normalizeText(requestSourceScene);
        if (candidate == null) {
            candidate = normalizeText(sessionSourceScene);
        }
        if (candidate == null) {
            return SOURCE_SCENE_AI_WORKBENCH;
        }
        String normalized = candidate.trim().toUpperCase().replace('-', '_').replace(' ', '_');
        if ("POLICY_CONSULTANT".equals(normalized) || SOURCE_SCENE_MOBILE_POLICY_CONSULTANT.equals(normalized)) {
            return SOURCE_SCENE_MOBILE_POLICY_CONSULTANT;
        }
        return SOURCE_SCENE_AI_WORKBENCH;
    }

    private void validateMonthlyTokenQuota(Long userId) {
        if (userId == null) {
            return;
        }
        if (!"1".equals(normalizeText(paramService.getByCode(QUOTA_PARAM_ENABLED)))) {
            return;
        }
        Long quota = parseLongValue(paramService.getByCode(QUOTA_PARAM_VALUE));
        if (quota == null || quota <= 0L) {
            return;
        }
        Long monthTotalTokens = safeUsageLong(aiAgentUsageMapper.sumMonthTotalTokensByUserId(userId));
        if (monthTotalTokens >= quota) {
            throw new IllegalArgumentException(QUOTA_EXCEEDED_MESSAGE);
        }
    }

    private Long parseLongValue(String value) {
        String normalized = normalizeText(value);
        if (normalized == null) {
            return null;
        }
        try {
            return Long.parseLong(normalized);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private AiAgentUsageEntity buildUsageEntity(Long sessionId,
                                                Long messageId,
                                                Long userId,
                                                String sourceScene,
                                                String providerName,
                                                String modelCode,
                                                OpenAiCompatibleChatClient.ChatResult chatResult,
                                                String usageSource) {
        AiAgentUsageEntity entity = new AiAgentUsageEntity();
        entity.setSessionId(sessionId);
        entity.setMessageId(messageId);
        entity.setUserId(userId);
        entity.setSourceScene(sourceScene);
        entity.setProviderName(normalizeText(providerName));
        entity.setModelCode(normalizeText(modelCode));
        entity.setPromptTokens(safeUsageInt(chatResult == null ? null : chatResult.getPromptTokens()));
        entity.setCompletionTokens(safeUsageInt(chatResult == null ? null : chatResult.getCompletionTokens()));
        entity.setTotalTokens(safeUsageInt(chatResult == null ? null : chatResult.getTotalTokens()));
        entity.setDurationMs(safeUsageLong(chatResult == null ? null : chatResult.getDurationMs()));
        entity.setUsageSource(normalizeText(usageSource) == null ? "PROVIDER_NO_USAGE" : usageSource);
        entity.setCreateTime(LocalDateTime.now());
        return entity;
    }

    private AgentChatResultVO buildChatResultVO(AgentSessionEntity session,
                                                String sourceScene,
                                                String answer,
                                                KnowledgeCitationContext context,
                                                AiAgentUsageEntity usageEntity,
                                                Long monthTotalTokens) {
        AgentChatResultVO result = new AgentChatResultVO();
        result.setSessionId(session.getId());
        result.setSourceScene(sourceScene);
        result.setAnswer(answer);
        result.setCitedChunkIds(context.toChunkIds());
        result.setCitedChunkIdList(context.toChunkIds().isEmpty() ? List.of() : Arrays.asList(context.toChunkIds().split(",")));
        result.setCitedTitles(context.getChunks().stream().map(KnowledgeSearchResultVO::getDocTitle).distinct().collect(Collectors.toList()));
        result.setPromptTokens(usageEntity.getPromptTokens());
        result.setCompletionTokens(usageEntity.getCompletionTokens());
        result.setTotalTokens(usageEntity.getTotalTokens());
        result.setDurationMs(usageEntity.getDurationMs());
        result.setMonthTotalTokens(monthTotalTokens);
        result.setModelCode(usageEntity.getModelCode());
        return result;
    }

    private void safeExtractUserProfile(Long userId,
                                    Long baseId,
                                    String sourceScene,
                                    Long sessionId,
                                    Long messageId,
                                    String question) {
    logCenterService.recordAiChainSuccess(
            "AI_USER_PROFILE_EXTRACT_TRY",
            sessionId,
            userId,
            sourceScene,
            "baseId=" + valueOrBlank(baseId)
                    + ", messageId=" + valueOrBlank(messageId)
                    + ", questionLength=" + safeLength(question)
    );

    if (aiUserProfileService == null) {
        logCenterService.recordAiChainFailed(
                "AI_USER_PROFILE_SERVICE_NULL",
                sessionId,
                userId,
                sourceScene,
                "messageId=" + valueOrBlank(messageId)
        );
        return;
    }

    if (userId == null || normalizeText(question) == null) {
        logCenterService.recordAiChainFailed(
                "AI_USER_PROFILE_EXTRACT_SKIP",
                sessionId,
                userId,
                sourceScene,
                "userId=" + valueOrBlank(userId)
                        + ", questionLength=" + safeLength(question)
        );
        return;
    }

    try {
        aiUserProfileService.extractAndUpdateFromMessage(
                userId,
                baseId,
                sourceScene,
                sessionId,
                messageId,
                question
        );

        logCenterService.recordAiChainSuccess(
                "AI_USER_PROFILE_EXTRACT_DONE",
                sessionId,
                userId,
                sourceScene,
                "messageId=" + valueOrBlank(messageId)
        );
    } catch (Exception ex) {
        logCenterService.recordAiChainFailed(
                "AI_USER_PROFILE_EXTRACT_FAILED",
                sessionId,
                userId,
                sourceScene,
                "messageId=" + valueOrBlank(messageId)
                        + ", error=" + valueOrBlank(normalizeText(ex.getMessage()))
        );
    }
}

private String safeBuildUserProfilePromptSummary(Long userId,
                                                 Long baseId,
                                                 String sourceScene,
                                                 String question) {
    if (aiUserProfileService == null || userId == null) {
        return "";
    }

    try {
        String summary = aiUserProfileService.buildPromptSummaryForQuestion(
                userId,
                baseId,
                sourceScene,
                question
        );
        return normalizeText(summary) == null ? "" : summary.trim();
    } catch (Exception ex) {
        logCenterService.recordAiChainFailed(
                "AI_USER_PROFILE_PROMPT_SUMMARY_FAILED",
                null,
                userId,
                sourceScene,
                "baseId=" + valueOrBlank(baseId)
                        + ", error=" + valueOrBlank(normalizeText(ex.getMessage()))
        );
        return "";
    }
}

private String appendUserProfilePrompt(String prompt, String userProfilePromptSummary) {
    if (normalizeText(userProfilePromptSummary) == null) {
        return prompt;
    }

    String basePrompt = prompt == null ? "" : prompt;
    return basePrompt + "\n\n" + userProfilePromptSummary.trim();
}

private String appendConversationContextPrompt(String prompt, String contextPrompt) {
    if (normalizeText(contextPrompt) == null) {
        return prompt;
    }

    String basePrompt = prompt == null ? "" : prompt;
    return basePrompt + "\n\n" + contextPrompt.trim();
}

private String buildConversationContextPrompt(Long sessionId, Long currentMessageId) {
    if (sessionId == null) {
        return "";
    }

    List<AgentMessageEntity> recentMessages = agentMessageMapper.queryRecentEntityBySessionId(sessionId, 20);
    if (recentMessages == null || recentMessages.isEmpty()) {
        return "";
    }

    Collections.reverse(recentMessages);

    StringBuilder builder = new StringBuilder();
    builder.append("【当前主题历史上下文】\n");
    builder.append("以下内容是用户在当前主题内的连续对话历史。回答本轮问题时必须结合上下文，不要聊了这句忘了上句。\n");
    builder.append("如果用户重复问了前面已经问过的问题，应自然提示“上面已经提到过，我再帮您整理一下重点”。\n");
    builder.append("如果用户补充了新条件，应基于新条件修正或细化前面的判断。\n");
    builder.append("如果用户是在帮别人问、假设场景或泛问，不要把这些信息误当成用户本人画像。\n\n");

    int totalLength = 0;
    for (AgentMessageEntity message : recentMessages) {
        if (message == null || Objects.equals(message.getId(), currentMessageId)) {
            continue;
        }

        String role = normalizeText(message.getMessageRole());
        String text = normalizeText(message.getMessageText());
        if (text == null) {
            continue;
        }

        String roleLabel = "assistant".equalsIgnoreCase(role) ? "助手" : "用户";
        String line = roleLabel + "：" + text + "\n";

        totalLength += line.length();
        if (totalLength > 3500) {
            builder.append("……以上为当前主题最近部分上下文，较早内容已省略。\n");
            break;
        }

        builder.append(line);
    }

    return builder.toString();
}

private String buildRepeatedQuestionHint(Long sessionId, Long currentMessageId, String question) {
    String normalizedQuestion = normalizeForRepeatCheck(question);
    if (sessionId == null || normalizedQuestion == null) {
        return "";
    }

    List<AgentMessageEntity> recentMessages = agentMessageMapper.queryRecentEntityBySessionId(sessionId, 20);
    if (recentMessages == null || recentMessages.isEmpty()) {
        return "";
    }

    for (AgentMessageEntity message : recentMessages) {
        if (message == null || Objects.equals(message.getId(), currentMessageId)) {
            continue;
        }

        String role = normalizeText(message.getMessageRole());
        if (!"user".equalsIgnoreCase(role)) {
            continue;
        }

        String oldQuestion = normalizeForRepeatCheck(message.getMessageText());
        if (oldQuestion == null) {
            continue;
        }

        if (oldQuestion.equals(normalizedQuestion)
                || oldQuestion.contains(normalizedQuestion)
                || normalizedQuestion.contains(oldQuestion)) {
            return "【重复提问提示】\n"
                    + "用户本轮问题与当前主题前文高度相似。回答时请自然承接上下文，可以说："
                    + "“上面已经提到过，我再帮您整理一下重点。”然后再给出更清晰、更简洁的归纳。\n";
        }
    }

    return "";
}

private String normalizeForRepeatCheck(String text) {
    String normalized = normalizeText(text);
    if (normalized == null) {
        return null;
    }
    return normalized
            .replace("？", "")
            .replace("?", "")
            .replace("。", "")
            .replace(".", "")
            .replace("，", "")
            .replace(",", "")
            .replace("、", "")
            .replace("；", "")
            .replace(";", "")
            .replace("：", "")
            .replace(":", "")
            .replace(" ", "")
            .trim();
}

private String resolveSystemMetaAnswer(String question,
                                       AgentSessionEntity session,
                                       SkillVersionEntity version) {
    String q = compactQuestionText(question);
    if (q == null) {
        return null;
    }

    if (isModelQuestion(q)) {
        return "当前政策咨询助手配置使用的语言模型为 "
                + resolveCurrentModelCodeForDisplay(session, version)
                + "。具体模型以后台 AI 接入配置和 Skill 版本配置为准。";
    }

    if (isCorrectionFeedback(q)) {
        return "抱歉，刚才回答与您的问题不匹配。请您重新输入要查询的政策问题，或直接说明需要纠正哪一部分，我会按新的问题重新检索。";
    }

    if (isAssistantIdentityQuestion(q)) {
        return "我是政策咨询助手，主要用于回答人才政策、申报条件、补助标准、办理流程、材料依据等问题。";
    }

    if (isClearlyOutOfPolicyQuestion(q)) {
        return "这个问题不属于当前政策咨询助手的服务范围。当前助手主要回答人才政策、申报条件、补助标准、办理流程、材料依据等问题。"
                + "如果要咨询人才政策，请输入具体政策名称或问题，例如“双百计划补助标准是什么”“住房补贴怎么申请”。";
    }

    return null;
}

private String compactQuestionText(String text) {
    String normalized = normalizeText(text);
    if (normalized == null) {
        return null;
    }
    String value = normalized
            .replaceAll("\\s+", "")
            .replaceAll("[？?。！!，,、；;：:]", "");
    return normalizeText(value);
}

private boolean isModelQuestion(String question) {
    if (question == null) {
        return false;
    }
    return question.contains("语言模型")
            || question.contains("什么模型")
            || question.contains("模型是什么")
            || question.contains("你用的模型")
            || question.contains("你是什么模型")
            || question.contains("当前模型")
            || question.contains("模型版本");
}

private boolean isCorrectionFeedback(String question) {
    if (question == null) {
        return false;
    }
    return question.contains("回答错误")
            || question.contains("回答错了")
            || question.contains("答错")
            || question.contains("不对")
            || question.contains("错了")
            || question.contains("不准确")
            || question.contains("重新回答")
            || question.contains("不是这个");
}

private boolean isAssistantIdentityQuestion(String question) {
    if (question == null) {
        return false;
    }
    return question.contains("你是谁")
            || question.contains("你是什么")
            || question.contains("你能做什么")
            || question.contains("你可以做什么");
}
private boolean isClearlyOutOfPolicyQuestion(String question) {
    if (question == null) {
        return false;
    }

    // 先保护政策类问题，避免误拦截
    if (looksLikePolicyQuestion(question)) {
        return false;
    }

    return containsAny(question, List.of(
            "天气",
            "气温",
            "下雨",
            "降雨",
            "台风",
            "空气质量",
            "几点",
            "现在时间",
            "今天几号",
            "星期几",
            "新闻",
            "热搜",
            "股票",
            "股价",
            "汇率",
            "翻译",
            "作文",
            "写一篇",
            "写文章",
            "数学题",
            "物理题",
            "化学题",
            "代码怎么写",
            "帮我写代码",
            "旅游攻略",
            "菜谱",
            "做饭",
            "电影",
            "音乐"
    ));
}

private boolean looksLikePolicyQuestion(String question) {
    if (question == null) {
        return false;
    }

    return containsAny(question, List.of(
            "政策",
            "人才",
            "补助",
            "补贴",
            "奖励",
            "资助",
            "申报",
            "申请",
            "认定",
            "条件",
            "材料",
            "流程",
            "办理",
            "兑现",
            "拨付",
            "双百",
            "特聘岗位",
            "专项资金",
            "群鹭兴厦",
            "高层次",
            "博士后",
            "住房补贴",
            "服务保障",
            "子女教育",
            "医疗保障",
            "子女入学",
"子女就学",
"子女教育",
"孩子上学",
"医疗保障",
"医疗服务",
"健康体检",
"服务保障",
"人才服务",
"绿色通道",
"配偶就业",
"落户服务",
"待遇保障",
            "台湾特聘",
            "百人计划",
            "四大经济",
            "福建省",
            "厦门市"
    ));
}
private String resolveCurrentModelCodeForDisplay(AgentSessionEntity session,
                                                 SkillVersionEntity version) {
    String modelCode = normalizeText(session == null ? null : session.getModelCode());
    if (modelCode != null) {
        return modelCode;
    }

    modelCode = normalizeText(version == null ? null : version.getModelCode());
    if (modelCode != null) {
        return modelCode;
    }

    ProviderConfigEntity provider = providerConfigMapper.findFirstEnabledSuccess();
    modelCode = normalizeText(provider == null ? null : provider.getDefaultModel());
    if (modelCode != null) {
        return modelCode;
    }

    return "当前模型信息暂未配置";
}
private AgentChatResultVO buildAndSaveSystemMetaChatResult(AgentChatRequest request,
                                                           LoginUser user,
                                                           AgentSessionEntity session,
                                                           String sourceScene,
                                                           String answer,
                                                           String modelCode) {
    AgentMessageEntity userMessage = new AgentMessageEntity();
    userMessage.setSessionId(session.getId());
    userMessage.setMessageRole("user");
    userMessage.setMessageText(request.getQuestion());
    userMessage.setCreateTime(LocalDateTime.now());
    agentMessageMapper.insert(userMessage);

    AgentMessageEntity assistantMessage = new AgentMessageEntity();
    assistantMessage.setSessionId(session.getId());
    assistantMessage.setMessageRole("assistant");
    assistantMessage.setMessageText(answer);
    assistantMessage.setCitedChunkIds("");
    assistantMessage.setCreateTime(LocalDateTime.now());
    agentMessageMapper.insert(assistantMessage);

    AiAgentUsageEntity usageEntity = new AiAgentUsageEntity();
    usageEntity.setSessionId(session.getId());
    usageEntity.setMessageId(assistantMessage.getId());
    usageEntity.setUserId(user.getUserId());
    usageEntity.setSourceScene(sourceScene);
    usageEntity.setProviderName("SYSTEM");
    usageEntity.setModelCode(modelCode);
    usageEntity.setPromptTokens(0);
    usageEntity.setCompletionTokens(0);
    usageEntity.setTotalTokens(0);
    usageEntity.setDurationMs(0L);
    usageEntity.setUsageSource(USAGE_SOURCE_SYSTEM_META);
    usageEntity.setCreateTime(LocalDateTime.now());
    aiAgentUsageMapper.insert(usageEntity);

    Long monthTotalTokens = safeUsageLong(aiAgentUsageMapper.sumMonthTotalTokensByUserId(user.getUserId()));

    if (DEFAULT_SESSION_TITLE.equals(session.getSessionTitle())) {
        String newTitle = buildSessionTitle(request.getQuestion());
        session.setSessionTitle(newTitle);
        agentSessionMapper.updateTitle(session.getId(), newTitle);
    }

    operationLogService.log("AGENT", "CHAT_SYSTEM_META", session.getId(), "system meta answer in policy consultant");

    AgentChatResultVO result = new AgentChatResultVO();
    result.setSessionId(session.getId());
    result.setSourceScene(sourceScene);
    result.setAnswer(answer);
    result.setCitedChunkIds("");
    result.setCitedChunkIdList(List.of());
    result.setCitedTitles(List.of());
    result.setPromptTokens(0);
    result.setCompletionTokens(0);
    result.setTotalTokens(0);
    result.setDurationMs(0L);
    result.setMonthTotalTokens(monthTotalTokens);
    result.setModelCode(modelCode);
    return result;
}
private void executeSystemMetaChatStream(SseEmitter emitter,
                                         AgentChatRequest request,
                                         LoginUser user,
                                         AgentSessionEntity session,
                                         String sourceScene,
                                         String answer,
                                         String modelCode) {
    try {
        sendStreamStart(emitter, session.getId(), user.getUserId(), sourceScene, ANSWER_SOURCE_SYSTEM_META);

        AgentMessageEntity userMessage = new AgentMessageEntity();
        userMessage.setSessionId(session.getId());
        userMessage.setMessageRole("user");
        userMessage.setMessageText(request.getQuestion());
        userMessage.setCreateTime(LocalDateTime.now());
        agentMessageMapper.insert(userMessage);

        AgentMessageEntity assistantMessage = new AgentMessageEntity();
        assistantMessage.setSessionId(session.getId());
        assistantMessage.setMessageRole("assistant");
        assistantMessage.setMessageText(answer);
        assistantMessage.setCitedChunkIds("");
        assistantMessage.setCreateTime(LocalDateTime.now());

        sendStreamDelta(emitter, answer);

        agentMessageMapper.insert(assistantMessage);

        AiAgentUsageEntity usageEntity = new AiAgentUsageEntity();
        usageEntity.setSessionId(session.getId());
        usageEntity.setMessageId(assistantMessage.getId());
        usageEntity.setUserId(user.getUserId());
        usageEntity.setSourceScene(sourceScene);
        usageEntity.setProviderName("SYSTEM");
        usageEntity.setModelCode(modelCode);
        usageEntity.setPromptTokens(0);
        usageEntity.setCompletionTokens(0);
        usageEntity.setTotalTokens(0);
        usageEntity.setDurationMs(0L);
        usageEntity.setUsageSource(USAGE_SOURCE_SYSTEM_META);
        usageEntity.setCreateTime(LocalDateTime.now());
        aiAgentUsageMapper.insert(usageEntity);

        Long monthTotalTokens = safeUsageLong(aiAgentUsageMapper.sumMonthTotalTokensByUserId(user.getUserId()));

        if (DEFAULT_SESSION_TITLE.equals(session.getSessionTitle())) {
            String newTitle = buildSessionTitle(request.getQuestion());
            session.setSessionTitle(newTitle);
            agentSessionMapper.updateTitle(session.getId(), newTitle);
        }

        AgentChatResultVO result = new AgentChatResultVO();
        result.setSessionId(session.getId());
        result.setSourceScene(sourceScene);
        result.setAnswer(answer);
        result.setCitedChunkIds("");
        result.setCitedChunkIdList(List.of());
        result.setCitedTitles(List.of());
        result.setPromptTokens(0);
        result.setCompletionTokens(0);
        result.setTotalTokens(0);
        result.setDurationMs(0L);
        result.setMonthTotalTokens(monthTotalTokens);
        result.setModelCode(modelCode);

        sendDoneMeta(emitter, result, ANSWER_SOURCE_SYSTEM_META);
        sendDoneEvent(emitter, session.getId(), user.getUserId(), sourceScene, ANSWER_SOURCE_SYSTEM_META, usageEntity, monthTotalTokens, 0);
        emitter.complete();
    } catch (Exception ex) {
        sendStreamError(emitter, ex.getMessage());
        emitter.completeWithError(ex);
    }
}
    private void sendStreamStart(SseEmitter emitter,
                                 Long sessionId,
                                 Long userId,
                                 String sourceScene,
                                 String answerSource) throws IOException {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("sessionId", sessionId);
        data.put("answerSource", answerSource);
        sendSseEvent(emitter, "start", data);
        logCenterService.recordAiChainSuccess(
                LOG_EVENT_STREAM_START,
                sessionId,
                userId,
                sourceScene,
                "sessionId=" + sessionId + ", answerSource=" + valueOrBlank(answerSource)
        );
    }

    private int emitStreamAnswer(SseEmitter emitter,
                                 Long sessionId,
                                 Long userId,
                                 String sourceScene,
                                 String answerSource,
                                 String answer,
                                 List<String> segments) throws IOException {
        List<String> safeSegments = (segments == null || segments.isEmpty()) ? List.of(valueOrBlank(answer)) : segments;
        int emitted = 0;
        for (int i = 0; i < safeSegments.size(); i++) {
            String text = safeSegments.get(i);
            if (normalizeText(text) == null) {
                continue;
            }
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("text", text);
            sendSseEvent(emitter, "delta", data);
            logCenterService.recordAiChainSuccess(
                    LOG_EVENT_STREAM_DELTA,
                    sessionId,
                    userId,
                    sourceScene,
                    "sessionId=" + sessionId
                            + ", answerSource=" + valueOrBlank(answerSource)
                            + ", segmentIndex=" + (i + 1)
                            + ", segmentLength=" + safeLength(text)
            );
            emitted++;
        }
        return emitted;
    }

    private void sendStreamDelta(SseEmitter emitter, String text) throws IOException {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("text", text);
        sendSseEvent(emitter, "delta", data);
    }

    private void sendDoneMeta(SseEmitter emitter,
                              AgentChatResultVO result,
                              String answerSource) throws IOException {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("citations", result.getCitedTitles());
        data.put("answerSource", answerSource);
        data.put("modelCode", result.getModelCode());
        data.put("totalTokens", result.getTotalTokens());
        data.put("durationMs", result.getDurationMs());
        data.put("monthTotalTokens", result.getMonthTotalTokens());
        data.put("promptTokens", result.getPromptTokens());
        data.put("completionTokens", result.getCompletionTokens());
        data.put("sessionId", result.getSessionId());
        sendSseEvent(emitter, "done_meta", data);
    }

    private void sendDoneEvent(SseEmitter emitter,
                               Long sessionId,
                               Long userId,
                               String sourceScene,
                               String answerSource,
                               AiAgentUsageEntity usageEntity,
                               Long monthTotalTokens,
                               int knowledgeHits) throws IOException {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("ok", true);
        sendSseEvent(emitter, "done", data);
        logCenterService.recordAiChainSuccess(
                LOG_EVENT_STREAM_DONE,
                sessionId,
                userId,
                sourceScene,
                "sessionId=" + sessionId
                        + ", answerSource=" + valueOrBlank(answerSource)
                        + ", modelCode=" + valueOrBlank(usageEntity.getModelCode())
                        + ", totalTokens=" + safeUsageInt(usageEntity.getTotalTokens())
                        + ", monthTotalTokens=" + safeUsageLong(monthTotalTokens)
        );
        logCenterService.recordAiChainSuccess(
                LOG_EVENT_RESULT_FLOW_READY,
                sessionId,
                userId,
                sourceScene,
                "sessionId=" + sessionId
                        + ", answerSource=" + valueOrBlank(answerSource)
                        + ", knowledgeHits=" + knowledgeHits
                        + ", modelCode=" + valueOrBlank(usageEntity.getModelCode())
                        + ", monthTotalTokens=" + safeUsageLong(monthTotalTokens)
                        + ", totalTokens=" + safeUsageInt(usageEntity.getTotalTokens())
                        + ", stream=true"
        );
    }

    private void sendStreamError(SseEmitter emitter, String message) {
        try {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("message", valueOrDefault(normalizeText(message), "stream error"));
            sendSseEvent(emitter, "error", data);
        } catch (IOException ignored) {
        }
    }

    private void sendSseEvent(SseEmitter emitter, String eventName, Object data) throws IOException {
        emitter.send(SseEmitter.event().name(eventName).data(data, MediaType.APPLICATION_JSON));
    }

    private String ensureNonEmptyStreamAnswer(String answer,
                                              Long sessionId,
                                              Long userId,
                                              String sourceScene,
                                              String answerSource) {
        if (normalizeText(answer) != null) {
            return answer;
        }
        logCenterService.recordAiChainSuccess(
                LOG_EVENT_STREAM_EMPTY_RESULT,
                sessionId,
                userId,
                sourceScene,
                "sessionId=" + sessionId + ", answerSource=" + valueOrBlank(answerSource)
        );
        return "当前未获取到可展示内容，请稍后重试或补充更具体条件。";
    }

    private List<String> splitStructuredStreamSegments(String answer) {
        LinkedHashSet<String> segments = new LinkedHashSet<>();
        if (normalizeText(answer) == null) {
            return List.of();
        }
        String normalized = answer.replace("\r\n", "\n").replace('\r', '\n');
        for (String line : normalized.split("\n")) {
            String text = normalizeText(line);
            if (text == null) {
                continue;
            }
            segments.add(text + "\n");
        }
        return new ArrayList<>(segments);
    }

    private List<String> splitGenericStreamSegments(String answer) {
        if (normalizeText(answer) == null) {
            return List.of();
        }
        List<String> segments = new ArrayList<>();
        String normalized = answer.replace("\r\n", "\n").replace('\r', '\n');
        for (String block : normalized.split("\n")) {
            String text = normalizeText(block);
            if (text == null) {
                continue;
            }
            if (text.matches("^\\d+[.、].*") || text.startsWith("- ") || text.endsWith("：") || text.endsWith(":")) {
                segments.add(text + "\n");
                continue;
            }
            for (String sentence : text.split("(?<=[。！？!?；;])")) {
                String piece = normalizeText(sentence);
                if (piece != null) {
                    segments.add(piece);
                }
            }
        }
        return segments.isEmpty() ? List.of(answer) : segments;
    }

    private String resolveRemainingStreamAnswer(String fullAnswer, String streamedAnswer, boolean deltaSent) {
        if (!deltaSent || normalizeText(streamedAnswer) == null) {
            return fullAnswer;
        }
        if (fullAnswer != null && fullAnswer.startsWith(streamedAnswer)) {
            return fullAnswer.substring(streamedAnswer.length());
        }
        return fullAnswer;
    }

    private String resolveProviderName(ProviderConfigEntity provider) {
        if (provider == null) {
            return null;
        }
        String providerName = normalizeText(provider.getProviderName());
        if (providerName != null) {
            return providerName;
        }
        try {
            Method getNameMethod = provider.getClass().getMethod("getName");
            Object value = getNameMethod.invoke(provider);
            return value == null ? null : normalizeText(String.valueOf(value));
        } catch (Exception ignored) {
            return null;
        }
    }

    private Integer safeUsageInt(Integer value) {
        return value == null ? 0 : Math.max(value, 0);
    }

    private Long safeUsageLong(Long value) {
        return value == null ? 0L : Math.max(value, 0L);
    }

    private int safeLength(String value) {
        return value == null ? 0 : value.length();
    }
private KnowledgeCitationContext buildConditionReverseContext(Long baseId,
                                                              String question,
                                                              String sourceScene,
                                                              int topN) {
    KnowledgeCitationContext context = new KnowledgeCitationContext();
    if (baseId == null) {
        return context;
    }

    LinkedHashSet<Long> chunkIds = new LinkedHashSet<>();
    List<String> candidates = buildConditionReverseCandidates(question);
    String regionScope = detectPreferredRegionScope(question, PolicyQuestionType.CONDITION);
    int limit = Math.max(topN, 10);

    /*
     * 第 0 轮：精准政策强召回。
     * 这一步很关键：用户问“本科生/CFA/软件工程可以申请什么”，
     * 不能只靠自然搜索，要直接扫描可能包含这些条件的产业人才项目。
     */
    appendExactPolicyConditionReverseChunks(
            context,
            chunkIds,
            baseId,
            question,
            regionScope,
            sourceScene,
            limit
    );

    /*
     * 第 1 轮：严格条件类切片召回。
     */
    if (context.getChunks().size() < 6) {
        appendContextChunks(
                context,
                chunkIds,
                baseId,
                candidates,
                limit,
                limit,
                PolicyQuestionType.CONDITION,
                regionScope,
                sourceScene,
                item -> isConditionReverseChunk(item, question)
        );
    }

    /*
     * 第 2 轮：放宽到候选词命中。
     */
    if (context.getChunks().size() < 6) {
        appendContextChunks(
                context,
                chunkIds,
                baseId,
                candidates,
                limit,
                limit,
                PolicyQuestionType.CONDITION,
                regionScope,
                sourceScene,
                item -> containsAny(buildChunkSearchText(item), candidates)
        );
    }

    /*
     * 第 3 轮：本科/金融证书专门强召回。
     */
    if (context.getChunks().size() < 6
            && containsAny(question, List.of("本科", "本科生", "学士", "学士学位", "CFA", "FRM", "CPA", "金融"))) {
        appendContextChunks(
                context,
                chunkIds,
                baseId,
                List.of(
                        "金融服务产业人才项目 本科及以上 CFA FRM CPA",
                        "金融服务产业人才 申报条件 本科及以上",
                        "所在机构推荐申报 本科及以上 高级职称",
                        "金融服务产业人才项目 申报方式 CFA FRM CPA 精算 法律职业资格"
                ),
                limit,
                limit,
                PolicyQuestionType.CONDITION,
                regionScope,
                sourceScene,
                item -> containsAny(buildChunkSearchText(item), List.of(
                        "金融服务产业人才",
                        "本科及以上",
                        "本科以上",
                        "CFA",
                        "FRM",
                        "CPA",
                        "精算",
                        "法律职业",
                        "高级职称",
                        "所在机构推荐"
                ))
        );
    }

    return context;
}
    private KnowledgeCitationContext buildContext(Long baseId, String question, int topN, String sourceScene) {
        KnowledgeCitationContext context = new KnowledgeCitationContext();
        if (baseId == null || topN <= 0) {
            return context;
        }
        PolicyRouteIntent routeIntent = policyRouteService.route(question);
        PolicyQuestionType questionType = resolvePolicyQuestionType(routeIntent, question);
        List<String> formalPolicyNames = extractFormalPolicyNames(question);
        List<String> specialTopicTerms = extractMatchedSpecialTopicTerms(question);
        String regionScope = normalizeText(routeIntent.getRegionScope()) != null
                ? routeIntent.getRegionScope()
                : detectPreferredRegionScope(question, questionType);
        int contextLimit = resolveContextLimit(questionType, topN);
        LinkedHashSet<Long> chunkIds = new LinkedHashSet<>();
        List<String> catalogCandidates = policyCatalogService.resolveCandidateSearchTerms(baseId, routeIntent, Math.min(contextLimit, 4));
        if (!catalogCandidates.isEmpty()) {
            appendContextChunks(context, chunkIds, baseId, catalogCandidates, Math.min(contextLimit, 4), contextLimit, questionType, regionScope, sourceScene,
                    item -> matchesPolicyIntent(item, formalPolicyNames, specialTopicTerms) || containsAny(buildChunkSearchText(item), catalogCandidates));
        }
        if (questionType == PolicyQuestionType.LIST) {
            appendContextChunks(context, chunkIds, baseId, extractStructuredListRecallCandidates(question), Math.min(contextLimit, 6), contextLimit, questionType, regionScope, sourceScene,
                    item -> isPreferredListChunk(item, formalPolicyNames, specialTopicTerms));
            appendContextChunks(context, chunkIds, baseId, extractStructuredTopicRecallCandidates(question), contextLimit, contextLimit, questionType, regionScope, sourceScene,
                    item -> !isAbstractSummaryChunk(item));
            if (context.getChunks().size() < contextLimit) {
                appendContextChunks(context, chunkIds, baseId, extractStructuredSearchCandidates(question), contextLimit, contextLimit, questionType, regionScope, sourceScene,
                        item -> !isAbstractSummaryChunk(item));
            }
            return context;
        }
        if (questionType == PolicyQuestionType.PROCESS) {
    boolean doubleHundredProcess = isDoubleHundredApplyFlowQuestion(question, questionType);

    if (doubleHundredProcess) {
        appendContextChunks(
                context,
                chunkIds,
                baseId,
                buildDoubleHundredStrictRecallCandidates(question),
                Math.min(Math.max(contextLimit, 8), 8),
                Math.max(contextLimit, 8),
                questionType,
                regionScope,
                sourceScene,
                this::isDoubleHundredProcessChunk
        );
    }

    appendContextChunks(
            context,
            chunkIds,
            baseId,
            extractStructuredTopicRecallCandidates(question),
            Math.min(contextLimit, 4),
            Math.max(contextLimit, 8),
            questionType,
            regionScope,
            sourceScene,
            item -> matchesPolicyIntent(item, formalPolicyNames, specialTopicTerms)
                    || (doubleHundredProcess && isDoubleHundredProcessChunk(item))
    );

    appendContextChunks(
            context,
            chunkIds,
            baseId,
            extractStructuredProcessRecallCandidates(question),
            contextLimit,
            Math.max(contextLimit, 8),
            questionType,
            regionScope,
            sourceScene,
            item -> isProcessChunk(item)
                    && (!doubleHundredProcess || isDoubleHundredProcessChunk(item))
    );

    if (context.getChunks().size() < contextLimit) {
        appendContextChunks(
                context,
                chunkIds,
                baseId,
                extractStructuredSearchCandidates(question),
                contextLimit,
                Math.max(contextLimit, 8),
                questionType,
                regionScope,
                sourceScene,
                item -> (matchesPolicyIntent(item, formalPolicyNames, specialTopicTerms) || isProcessChunk(item))
                        && (!doubleHundredProcess || isDoubleHundredProcessChunk(item))
        );
    }

    if (context.getChunks().size() < contextLimit && !doubleHundredProcess) {
        appendContextChunks(
                context,
                chunkIds,
                baseId,
                extractStructuredSearchCandidates(question),
                contextLimit,
                contextLimit,
                questionType,
                regionScope,
                sourceScene
        );
    }
    return context;
}
        if (questionType == PolicyQuestionType.CONDITION) {
            appendContextChunks(context, chunkIds, baseId, extractStructuredTopicRecallCandidates(question), Math.min(contextLimit, 4), contextLimit, questionType, regionScope, sourceScene,
                    item -> matchesPolicyIntent(item, formalPolicyNames, specialTopicTerms));
            appendContextChunks(context, chunkIds, baseId, extractStructuredConditionRecallCandidates(question), contextLimit, contextLimit, questionType, regionScope, sourceScene,
                    this::isConditionChunk);
            if (context.getChunks().size() < contextLimit) {
                appendContextChunks(context, chunkIds, baseId, extractStructuredSearchCandidates(question), contextLimit, contextLimit, questionType, regionScope, sourceScene,
                        item -> matchesPolicyIntent(item, formalPolicyNames, specialTopicTerms) || isConditionChunk(item));
            }
            return context;
        }
        if (questionType == PolicyQuestionType.BENEFIT) {
            appendContextChunks(context, chunkIds, baseId, extractStructuredTopicRecallCandidates(question), Math.min(contextLimit, 4), contextLimit, questionType, regionScope, sourceScene,
                    item -> matchesPolicyIntent(item, formalPolicyNames, specialTopicTerms));
            appendContextChunks(context, chunkIds, baseId, extractStructuredBenefitRecallCandidates(question), contextLimit, contextLimit, questionType, regionScope, sourceScene,
                    this::isBenefitChunk);
            if (context.getChunks().size() < contextLimit) {
                appendContextChunks(context, chunkIds, baseId, extractStructuredSearchCandidates(question), contextLimit, contextLimit, questionType, regionScope, sourceScene,
                        item -> matchesPolicyIntent(item, formalPolicyNames, specialTopicTerms) || isBenefitChunk(item));
            }
            return context;
        }
        if (questionType == PolicyQuestionType.BOUNDARY) {
            appendContextChunks(context, chunkIds, baseId, extractStructuredTopicRecallCandidates(question), contextLimit, contextLimit, questionType, null, sourceScene);
            appendContextChunks(context, chunkIds, baseId, extractStructuredSearchCandidates(question), contextLimit, contextLimit, questionType, null, sourceScene);
            return context;
        }
        if (questionType == PolicyQuestionType.SPECIAL_TOPIC) {
            appendContextChunks(context, chunkIds, baseId, extractStructuredTopicRecallCandidates(question), Math.min(contextLimit, 5), contextLimit, questionType, regionScope, sourceScene,
                    item -> matchesPolicyIntent(item, formalPolicyNames, specialTopicTerms));
            appendContextChunks(context, chunkIds, baseId, extractStructuredSpecialTopicSupplementCandidates(question), contextLimit, contextLimit, questionType, regionScope, sourceScene,
                    item -> matchesPolicyIntent(item, formalPolicyNames, specialTopicTerms));
            if (context.getChunks().size() < contextLimit) {
                appendContextChunks(context, chunkIds, baseId, extractStructuredSearchCandidates(question), contextLimit, contextLimit, questionType, regionScope, sourceScene,
                        item -> matchesPolicyIntent(item, formalPolicyNames, specialTopicTerms));
            }
            return context;
        }
        appendContextChunks(context, chunkIds, baseId, extractStructuredSearchCandidates(question), contextLimit, contextLimit, questionType, regionScope, sourceScene);
        return context;
    }

    private void appendContextChunks(KnowledgeCitationContext context,
                                     LinkedHashSet<Long> chunkIds,
                                     Long baseId,
                                     List<String> candidates,
                                     int stageLimit,
                                     int requestTopN) {
        appendContextChunks(context, chunkIds, baseId, candidates, stageLimit, requestTopN, PolicyQuestionType.GENERAL, null, null, null);
    }

    private void appendContextChunks(KnowledgeCitationContext context,
                                     LinkedHashSet<Long> chunkIds,
                                     Long baseId,
                                     List<String> candidates,
                                     int stageLimit,
                                     int requestTopN,
                                     PolicyQuestionType questionType,
                                     String regionScope,
                                     String sourceScene) {
        appendContextChunks(context, chunkIds, baseId, candidates, stageLimit, requestTopN, questionType, regionScope, sourceScene, null);
    }

    private void appendContextChunks(KnowledgeCitationContext context,
                                     LinkedHashSet<Long> chunkIds,
                                     Long baseId,
                                     List<String> candidates,
                                     int stageLimit,
                                     int requestTopN,
                                     PolicyQuestionType questionType,
                                     String regionScope,
                                     String sourceScene,
                                     Predicate<KnowledgeSearchResultVO> filter) {
        if (context == null || baseId == null || stageLimit <= 0 || requestTopN <= 0 || candidates == null || candidates.isEmpty()) {
            return;
        }
        for (String candidate : candidates) {
            if (context.getChunks().size() >= stageLimit) {
                return;
            }
            KnowledgeSearchRequest request = new KnowledgeSearchRequest();
            request.setBaseId(baseId);
            request.setKeywords(candidate);
            request.setQuestionType(mapQuestionTypeForSearch(questionType));
            request.setRegionScope(regionScope);
            request.setScenePriority(normalizeSourceScene(sourceScene, sourceScene));
            request.setSearchable(Boolean.TRUE);
            request.setEffectiveOnly(Boolean.TRUE);
            request.setTopN(requestTopN);
            List<KnowledgeSearchResultVO> list = knowledgeChunkMapper.search(request);
            if (list == null || list.isEmpty()) {
                continue;
            }
            for (KnowledgeSearchResultVO item : list) {
                if (context.getChunks().size() >= stageLimit) {
                    return;
                }
                if (filter != null && !filter.test(item)) {
                    continue;
                }
                Long chunkId = item.getChunkId();
                if (chunkId != null && !chunkIds.add(chunkId)) {
                    continue;
                }
                context.getChunks().add(item);
            }
        }
    }

    private int resolveContextLimit(PolicyQuestionType questionType, int topN) {
        if (questionType == PolicyQuestionType.LIST) {
            return Math.max(topN, 6);
        }
        if (questionType == PolicyQuestionType.PROCESS) {
            return Math.max(topN, 6);
        }
        if (questionType == PolicyQuestionType.SPECIAL_TOPIC) {
            return Math.max(topN, 6);
        }
        if (questionType == PolicyQuestionType.CONDITION || questionType == PolicyQuestionType.BENEFIT) {
            return Math.max(topN, 5);
        }
        if (questionType == PolicyQuestionType.FAQ || questionType == PolicyQuestionType.BOUNDARY) {
            return Math.max(topN, 4);
        }
        return topN;
    }

    private String detectPreferredRegionScope(String question, PolicyQuestionType questionType) {
        String normalized = normalizeText(question);
        if (normalized == null) {
            return null;
        }
        boolean xm = normalized.contains("厦门");
        boolean fj = normalized.contains("福建");
        if (questionType == PolicyQuestionType.BOUNDARY && xm && fj) {
            return null;
        }
        if (xm && !fj) {
            return "XM";
        }
        if (fj && !xm) {
            return "FJ";
        }
        String helperScope = PolicyKnowledgeSupport.resolveRegionScope(question);
        return "UNKNOWN".equalsIgnoreCase(helperScope) ? null : helperScope;
    }

    private String mapQuestionTypeForSearch(PolicyQuestionType questionType) {
        if (questionType == null) {
            return null;
        }
        return switch (questionType) {
            case LIST -> "list";
            case PROCESS -> "process";
            case CONDITION -> "condition";
            case BENEFIT -> "benefit";
            case SPECIAL_TOPIC -> "special_topic";
            case FAQ -> "faq";
            default -> null;
        };
    }

    private String resolvePolicyRouteSkills(String question) {
        return String.join(", ", policyRouteService.route(question).getRouteSkills());
    }

    private void requireAgentPermission(LoginUser user) {
        if (!permissionService.isSuperAdmin(user.getUserId())
                && !aiPermissionService.canUseAgent(user.getUserId())) {
            throw new IllegalArgumentException("current user cannot use AI workbench");
        }
    }

    private void requireSkillUse(LoginUser user, Long skillId) {
        if (!permissionService.isSuperAdmin(user.getUserId()) && !aiPermissionService.canUseSkill(user.getUserId(), skillId)) {
            throw new IllegalArgumentException("current user cannot use this skill");
        }
    }

    private void requireKnowledgeAnalyze(LoginUser user, Long baseId) {
        if (!permissionService.isSuperAdmin(user.getUserId()) && !aiPermissionService.canAnalyzeKnowledgeBase(user.getUserId(), baseId)) {
            throw new IllegalArgumentException("current user cannot analyze this knowledge base");
        }
    }
    private String buildSystemPrompt(SkillVersionEntity version,
                                     AgentUserPreferenceEntity preference,
                                     String question,
                                     String sourceScene) {
        PolicyQuestionType questionType = detectPolicyQuestionType(question);
        boolean mobileScene = SOURCE_SCENE_MOBILE_POLICY_CONSULTANT.equals(sourceScene);
        StringBuilder builder = new StringBuilder();
        builder.append(version.getSystemPrompt() == null ? "You are a professional assistant." : version.getSystemPrompt());
        if (preference != null && normalizeText(preference.getHabitSummary()) != null) {
            builder.append("\nUser habit profile: ").append(preference.getHabitSummary());
        }
        if (version.getForbiddenRules() != null) {
            builder.append("\nForbidden rules: ").append(version.getForbiddenRules());
        }
        if (version.getCitationRules() != null) {
            builder.append("\nCitation rules: ").append(version.getCitationRules());
        }
        if (!mobileScene && version.getOutputTemplate() != null) {
            builder.append("\nOutput template: ").append(version.getOutputTemplate());
        } else if (mobileScene) {
            builder.append("\nMobile answer mode: keep the response compact and avoid long fixed multi-section templates.");
            if (questionType == PolicyQuestionType.LIST) {
                builder.append("\nUse: conclusion, policy list, one short hint, citations.");
            } else if (questionType == PolicyQuestionType.PROCESS) {
                builder.append("\nUse: conclusion, process or evidence gap, one short handling suggestion, citations.");
            } else if (questionType == PolicyQuestionType.CONDITION || questionType == PolicyQuestionType.BENEFIT) {
                builder.append("\nUse: conclusion, key conditions or amount, one short hint, citations.");
            }
        }
        appendPolicyConsultationRules(builder, question, sourceScene);

builder.append("\n【连续对话规则】\n");
builder.append("1. 你正在和用户在同一个主题中连续聊天，必须参考当前主题历史上下文。\n");
builder.append("2. 如果用户重复询问前面已经问过的问题，要自然提醒“上面已经提到过，我再帮您整理一下重点”，不要像第一次听到一样回答。\n");
builder.append("3. 如果用户补充了新信息，要基于新信息修正或细化前面的判断。\n");
builder.append("4. 如果用户问“那我呢、这个呢、还有吗、继续”等省略问题，必须结合前文理解指代。\n");
builder.append("5. 不要把帮别人问、假设场景、泛问条件误当成用户本人长期画像。\n");
builder.append("6. 回答要像政策顾问一样自然承接上下文，而不是每次重新开始。\n");
builder.append("7. 如果用户指出前文不准确、补充政策原文或提供新的政策依据，要先承认用户补充的信息有价值，再基于知识库和上下文修正回答，不要机械重复“依据不足”。\n");

return builder.toString();
    }

    private String buildUserPrompt(SkillVersionEntity version,
                                   String question,
                                   KnowledgeCitationContext context,
                                   AgentUserPreferenceEntity preference,
                                   String sourceScene) {
        String promptKnowledgeContext = buildPromptKnowledgeContext(question, context, sourceScene);
        StringBuilder builder = new StringBuilder();
        if (version.getTaskPrompt() != null) {
            builder.append(version.getTaskPrompt()).append("\n\n");
        }
        if (preference != null && normalizeText(preference.getPreferredAnswerStyle()) != null) {
            builder.append("Preferred answer style: ").append(preference.getPreferredAnswerStyle()).append("\n");
        }
        if (!SOURCE_SCENE_MOBILE_POLICY_CONSULTANT.equals(sourceScene)
                && preference != null
                && normalizeText(preference.getRecentTopics()) != null) {
            builder.append("Recent user topics: ").append(preference.getRecentTopics()).append("\n\n");
        }
        if (SOURCE_SCENE_MOBILE_POLICY_CONSULTANT.equals(sourceScene)) {
            builder.append("Keep the default answer concise for mobile policy consultation, usually within 220 to 300 Chinese characters unless the user explicitly asks for a detailed interpretation.\n\n");
        }
        if (shouldForceProcessEvidenceMissingPrompt(question, context)) {
    builder.append("Hard rule: The specific policy/project has been confirmed to exist from the matched knowledge. You must explicitly say: ");
    builder.append("“已确认该政策/项目存在，当前知识库已命中部分政策依据，但缺少完整申请流程/申报通知/材料清单依据”。");
    builder.append(" Do not say the policy has no related content, do not say the policy itself does not exist, and do not say it is completely unconfirmed. ");
    builder.append("You may summarize currently known support targets, funding standards, and fund rules first, then explain that process materials are not yet complete.\n\n");
}
        builder.append("Answer strictly based on the knowledge below and include a citation section at the end.\n\nKnowledge context:\n")
                .append(promptKnowledgeContext)
                .append("Question:\n")
                .append(question);
        return builder.toString();
    }

    private String buildPolicyAwareUserPrompt(SkillVersionEntity version,
                                              String question,
                                              KnowledgeCitationContext context,
                                              AgentUserPreferenceEntity preference,
                                              String sourceScene) {
        PolicyQuestionType questionType = detectPolicyQuestionType(question);
        String basePrompt = buildUserPrompt(version, question, context, preference, sourceScene);
        String defaultMobileRule = "Keep the default answer concise for mobile policy consultation, usually within 220 to 300 Chinese characters unless the user explicitly asks for a detailed interpretation.\n\n";
        if (SOURCE_SCENE_MOBILE_POLICY_CONSULTANT.equals(sourceScene)
                && (questionType == PolicyQuestionType.LIST || questionType == PolicyQuestionType.PROCESS)) {
            basePrompt = basePrompt.replace(defaultMobileRule, "");
        }
        StringBuilder builder = new StringBuilder(basePrompt);
        String promptKnowledgeContext = buildPromptKnowledgeContext(question, context, sourceScene);
        String promptContext = "\n\nKnowledge context:\n" + promptKnowledgeContext + "Question:\n" + question;
        int promptContextIndex = builder.indexOf(promptContext);
        if (promptContextIndex < 0) {
            promptContextIndex = builder.length();
        }
        StringBuilder rules = new StringBuilder();
        rules.append("Active routing skills: ").append(resolvePolicyRouteSkills(question)).append(".\n\n");
                if (isConditionReverseQuestion(question)) {
            rules.append("This is a CONDITION_REVERSE policy matching question. ")
                    .append("The user is asking which policies may match a condition such as education, degree, certificate, profession, title, or industry. ")
                    .append("Do not answer that there is no policy only because no policy is named exactly after the condition. ")
                    .append("Explain that the condition may be an entry requirement rather than a standalone policy name. ")
                    .append("List matched policy or project names from the knowledge context, and for each one explain: matched condition, additional requirements, and uncertainty. ")
                    .append("For undergraduate/bachelor questions, pay special attention to clauses like 本科及以上学历、学士学位、所在机构推荐、CFA、FRM、CPA、精算、法律职业资格、高级职称. ")
                    .append("Answer in Chinese with a warm policy-consultant tone.\n\n");
        }
        if (isUserCorrectionOrEvidenceSupplement(question)) {
    rules.append("The user is correcting the previous answer or supplementing policy evidence. ")
            .append("You must acknowledge the user's supplement first, then re-check the knowledge context. ")
            .append("Do not mechanically answer that evidence is insufficient. ")
            .append("If the user's provided clause is not fully verified by the knowledge base, say that the supplement is useful but still needs official source verification. ")
            .append("Use a warm correction style such as: “您这个补充很关键，我先按您提到的政策方向重新核对。” ")
            .append("Then explain what can be preliminarily judged and what still requires official policy text.\n\n");
}
        if (SOURCE_SCENE_MOBILE_POLICY_CONSULTANT.equals(sourceScene)) {
            if (questionType == PolicyQuestionType.LIST) {
                rules.append("For MOBILE_POLICY_CONSULTANT list questions, you may answer slightly longer than usual so that 5 to 8 concrete policy or project names can be listed clearly.\n\n");
                rules.append("Use this short structure only: 结论 / 政策或项目清单 / 一句提示 / 政策依据. Avoid long six-part templates.\n\n");
            } else if (questionType == PolicyQuestionType.PROCESS) {
                rules.append("For MOBILE_POLICY_CONSULTANT process questions, concise wording is still preferred, but you may exceed 300 Chinese characters when needed to present steps and evidence gaps clearly.\n\n");
                rules.append("Use this short structure only: 结论 / 申请流程或流程缺口说明 / 办理建议 / 政策依据.\n\n");
            } else if (questionType == PolicyQuestionType.CONDITION || questionType == PolicyQuestionType.BENEFIT) {
                rules.append("Use this short structure only: 结论 / 关键条件或金额 / 一句提示 / 政策依据.\n\n");
            }
        }
        if (questionType == PolicyQuestionType.LIST) {
            rules.append("For LIST questions, output a concrete list of matched policy or project names first. Each item must include one short explanatory sentence. When the knowledge supports it, try to list 5 to 8 items. Do not answer with only abstract wording such as 核心框架, 政策基础框架, or 主框架. End with exactly: “以下为当前知识库已命中的主要政策/项目，不代表厦门市完整官方清单”。\n\n");
        } else if (questionType == PolicyQuestionType.PROCESS) {
            rules.append("For PROCESS questions, answer in steps when process evidence is available. If the policy or project is confirmed but the matched knowledge still lacks a complete process, notice, or material checklist, explicitly state: “")
                    .append(PROCESS_EVIDENCE_MISSING_MESSAGE)
                    .append("”。 Do not rewrite it as policy non-existence or no related content.\n\n");
        } else if (questionType == PolicyQuestionType.CONDITION) {
            rules.append("For CONDITION questions, answer the conditions directly. Prioritize eligibility, applicable targets, qualification thresholds, and whether the applicant fits the policy.\n\n");
        } else if (questionType == PolicyQuestionType.BENEFIT) {
            rules.append("For BENEFIT questions, answer the amount or support standard directly. Prioritize target tier, subsidy amount, reward amount, support standard, and management period when the knowledge provides them.\n\n");
        } else if (questionType == PolicyQuestionType.SPECIAL_TOPIC) {
            rules.append("For SPECIAL_TOPIC questions, prioritize the matched formal policy or专题章节 first, then supplement with overarching opinions or fund rules when relevant. Do not start from unrelated industry projects.\n\n");
        } else if (questionType == PolicyQuestionType.FAQ) {
            rules.append("For FAQ questions, answer the direct conclusion first, then add one or two supporting policy points.\n\n");
        } else if (questionType == PolicyQuestionType.BOUNDARY) {
            rules.append("For BOUNDARY questions, explicitly separate 厦门市级政策 and 福建省级政策. Do not present provincial policies as the current Xiamen city policy list. If both are relevant, answer in two clearly separated parts.\n\n");
        }
        if (questionType == PolicyQuestionType.PROCESS && shouldForceProcessEvidenceMissingPrompt(question, context)) {
    rules.append("Hard rule: The specific policy/project has been confirmed to exist from the matched knowledge. You must explicitly say: “")
            .append(PROCESS_EVIDENCE_MISSING_MESSAGE)
            .append("”. Do not say the policy has no related content, do not say the policy itself does not exist, and do not say it is completely unconfirmed. You may summarize currently known support targets, funding standards, and fund rules first, then explain that process materials are not yet complete.\n\n");
}
        builder.insert(promptContextIndex, rules);
        return builder.toString();
    }
private boolean shouldUsePolicyConsultAsFastPath(AiPolicyConsultService.ConsultResult result,
                                                 String question) {
    if (result == null || !result.applied()) {
        return false;
    }
    /*
     * 条件反查类问题不能走普通 FAST_PATH。
     * 例如“本科生可以申请什么政策”，本质是按学历/证书/专业条件反查可适配政策，
     * 不能简单返回“当前知识库未命中足够专题证据”。
     */
    if (isConditionReverseQuestion(question)) {
        return false;
    }
    AiPolicyConsultService.DiagnosticTrace trace = result.trace();
    if (trace == null) {
        return false;
    }
    /*
 * 用户在纠错、反驳或补充政策依据时，不能直接走 FAST_PATH 依据不足。
 * 这类问题需要交给 LLM 结合上下文重新组织回答，避免显得“没听懂用户刚才说的话”。
 */
if (isUserCorrectionOrEvidenceSupplement(question)) {
    return false;
}

    // 1. 精准 FAQ 命中：继续快速返回，0 Token
    if (trace.faqHit()) {
        return true;
    }

    /*
     * 2. 综合分析 / 多政策比较 / 个人匹配类问题：
     * 不要因为某一个专题证据不足就提前 FAST_PATH 返回。
     */
    if (shouldUseLlmForPolicyAnalysis(question)) {
        return false;
    }

    /*
     * 3. 带限定词的清单类问题：
     * 例如“涉及个税的条款、涉及社保的条款、有哪些申报材料、哪些不能重复享受”。
     * 这类问题不要返回普通依据不足；如果后续 context 有证据，应交给外部 AI 组织。
     */
    if (isNarrowPolicyListQuestion(question)) {
        return false;
    }

    // 4. 明确依据不足：不要调用外部 AI，避免无依据生成
    if (isPolicyEvidenceInsufficient(result)) {
        return true;
    }

    // 5. 其他已命中知识库证据的问题，交给 glm-4.7 组织答案
    return false;
}

private boolean isConditionReverseQuestion(String question) {
    String text = normalizeText(question);
    if (text == null) {
        return false;
    }

    boolean hasCondition = containsAny(text, List.of(
            "本科生",
            "本科",
            "本科学历",
            "本科及以上",
            "学士",
            "学士学位",
            "大学本科",
            "硕士",
            "研究生",
            "博士",
            "博士后",
            "CFA",
            "FRM",
            "CPA",
            "精算",
            "法律职业",
            "高级职称",
            "中级职称",
            "职称",
            "资格证书",
            "技能证书",
            "软件工程",
            "软件开发",
            "人工智能",
            "金融",
            "金融服务"
    ));

    boolean asksPolicyMatch = containsAny(text, List.of(
            "可以申请",
            "能申请",
            "能不能申请",
            "能否申请",
            "适合申请",
            "适合什么",
            "申请什么",
            "申请哪些",
            "哪些政策",
            "什么政策",
            "人才政策",
            "补贴",
            "补助",
            "政策"
    ));

    return hasCondition && asksPolicyMatch;
}
private List<String> resolveConditionReverseSeedPolicies(String question) {
    String text = normalizeText(question);
    if (text == null) {
        return List.of();
    }

    LinkedHashSet<String> policies = new LinkedHashSet<>();

    boolean bachelorLike = containsAny(text, List.of(
            "本科生", "本科", "本科学历", "本科及以上", "学士", "学士学位", "大学本科"
    ));

    boolean financeLike = containsAny(text, List.of(
            "金融", "金融服务", "CFA", "FRM", "CPA", "精算", "法律职业"
    ));

    boolean softwareLike = containsAny(text, List.of(
            "软件工程", "软件", "软件开发", "信息技术", "计算机", "人工智能", "AI"
    ));

    boolean titleLike = containsAny(text, List.of(
            "高级职称", "中级职称", "职称", "资格证书", "技能证书"
    ));

    /*
     * 本科/学士类泛问，不能只搜“双百计划”。
     * 应优先扫描各产业人才项目的申报条件。
     */
    if (bachelorLike) {
        policies.add("厦门市金融服务产业人才项目实施办法");
        policies.add("厦门市电子信息产业人才项目实施办法");
        policies.add("厦门市重点产业骨干人才项目实施办法");
        policies.add("厦门市商贸物流产业人才项目实施办法");
        policies.add("厦门市文旅创意产业人才项目实施办法");
        policies.add("厦门市新能源和新材料产业人才项目实施办法");
        policies.add("厦门市生物医药产业人才项目实施办法");
    }

    if (financeLike) {
        policies.add("厦门市金融服务产业人才项目实施办法");
    }

    if (softwareLike) {
        policies.add("厦门市电子信息产业人才项目实施办法");
        policies.add("厦门市重点产业骨干人才项目实施办法");
        policies.add("厦门市支持人工智能领域人才发展的若干措施");
    }

    if (titleLike) {
        policies.add("厦门市重点产业骨干人才项目实施办法");
        policies.add("厦门市金融服务产业人才项目实施办法");
        policies.add("厦门市电子信息产业人才项目实施办法");
    }

    return new ArrayList<>(policies);
}
private void appendExactPolicyConditionReverseChunks(KnowledgeCitationContext context,
                                                     LinkedHashSet<Long> chunkIds,
                                                     Long baseId,
                                                     String question,
                                                     String regionScope,
                                                     String sourceScene,
                                                     int limit) {
    if (context == null || baseId == null || limit <= 0) {
        return;
    }

    List<String> policyNames = resolveConditionReverseSeedPolicies(question);
    if (policyNames.isEmpty()) {
        return;
    }

    for (String policyName : policyNames) {
        if (context.getChunks().size() >= limit) {
            return;
        }

        List<KnowledgeSearchResultVO> matchedChunks = searchExactPolicyChunks(
                baseId,
                policyName,
                regionScope,
                sourceScene
        );

        if (matchedChunks == null || matchedChunks.isEmpty()) {
            continue;
        }

        for (KnowledgeSearchResultVO item : matchedChunks) {
            if (context.getChunks().size() >= limit) {
                return;
            }

            if (!isStrongConditionReverseSeedChunk(item, question, policyName)) {
                continue;
            }

            Long chunkId = item.getChunkId();
            if (chunkId != null && !chunkIds.add(chunkId)) {
                continue;
            }

            context.getChunks().add(item);
        }
    }
}
private boolean isStrongConditionReverseSeedChunk(KnowledgeSearchResultVO item,
                                                  String question,
                                                  String seedPolicyName) {
    if (item == null) {
        return false;
    }

    String text = buildChunkSearchText(item);
    if (normalizeText(text) == null) {
        return false;
    }

    String upperText = text.toUpperCase();
    String policyText = String.join(" ",
            valueOrBlank(seedPolicyName),
            valueOrBlank(resolvePolicyDisplayName(item)),
            valueOrBlank(item.getPolicyName()),
            valueOrBlank(item.getDocTitle()),
            valueOrBlank(item.getChapterTitle()),
            valueOrBlank(item.getSectionTitle()),
            valueOrBlank(item.getHeadingPath())
    );

    boolean policyMatched = containsKeyword(policyText, seedPolicyName)
            || containsAny(policyText, List.of(
            "金融服务产业人才",
            "电子信息产业人才",
            "重点产业骨干人才",
            "人工智能领域人才",
            "商贸物流产业人才",
            "文旅创意产业人才",
            "新能源和新材料产业人才",
            "生物医药产业人才"
    ));

    if (!policyMatched) {
        return false;
    }

    boolean conditionSection = containsAny(text, List.of(
            "申报条件",
            "基本条件",
            "适用对象",
            "资格要求",
            "认定条件",
            "申报方式",
            "所在机构推荐",
            "推荐申报",
            "学历",
            "学位",
            "职称",
            "资格证书"
    ));

    boolean conditionMatched = containsAny(text, buildConditionReverseCandidates(question))
            || upperText.contains("CFA")
            || upperText.contains("FRM")
            || upperText.contains("CPA")
            || containsAny(text, List.of(
            "本科及以上",
            "本科以上",
            "学士",
            "学士学位",
            "高级职称",
            "精算",
            "法律职业资格",
            "所在机构推荐申报"
    ));

    return conditionSection && conditionMatched;
}
private List<String> buildConditionReverseCandidates(String question) {
    String text = normalizeText(question);
    if (text == null) {
        return List.of();
    }

    LinkedHashSet<String> candidates = new LinkedHashSet<>();

    candidates.add(text);
    candidates.add("申报条件");
    candidates.add("适用对象");
    candidates.add("资格要求");
    candidates.add("认定条件");
    candidates.add("申报方式");
    candidates.add("所在机构推荐申报");

    if (containsAny(text, List.of("本科生", "本科", "本科学历", "本科及以上", "学士", "学士学位", "大学本科"))) {
        candidates.add("本科及以上学历");
        candidates.add("本科及以上");
        candidates.add("本科学历");
        candidates.add("学士学位");
        candidates.add("学士");
        candidates.add("大学本科");
        candidates.add("普通高校毕业生");
        candidates.add("高校毕业生");
        candidates.add("青年人才");
    }

    if (containsAny(text, List.of("金融", "金融服务", "CFA", "FRM", "CPA", "精算", "法律职业"))) {
        candidates.add("金融服务产业人才");
        candidates.add("金融服务产业人才项目");
        candidates.add("CFA");
        candidates.add("FRM");
        candidates.add("CPA");
        candidates.add("精算");
        candidates.add("法律职业资格");
        candidates.add("高级职称");
        candidates.add("所在机构推荐");
    }

    if (containsAny(text, List.of("软件工程", "软件", "软件开发", "信息技术", "计算机", "人工智能", "AI"))) {
        candidates.add("电子信息产业人才");
        candidates.add("软件信息技术");
        candidates.add("电子信息");
        candidates.add("人工智能人才");
        candidates.add("重点产业骨干人才");
        candidates.add("群鹭兴厦");
    }

    if (containsAny(text, List.of("高级职称", "中级职称", "职称"))) {
        candidates.add("高级职称");
        candidates.add("中级职称");
        candidates.add("专业技术职称");
        candidates.add("资格要求");
        candidates.add("认定条件");
    }

    return new ArrayList<>(candidates);
}

private boolean isConditionReverseChunk(KnowledgeSearchResultVO item, String question) {
    if (item == null) {
        return false;
    }

    String text = buildChunkSearchText(item);
    if (normalizeText(text) == null) {
        return false;
    }

    boolean conditionSection = containsAny(text, List.of(
            "申报条件",
            "基本条件",
            "适用对象",
            "资格要求",
            "认定条件",
            "申报方式",
            "所在机构推荐",
            "推荐申报",
            "学历",
            "职称",
            "资格证书"
    ));

    boolean matchedCondition = containsAny(text, buildConditionReverseCandidates(question));

    return conditionSection && matchedCondition;
}
private boolean shouldUseLlmForPolicyAnalysis(String question) {
    String normalized = normalizeText(question);
    if (normalized == null) {
        return false;
    }
    if (isConditionReverseQuestion(question)) {
        return true;
    }
    // 明显的综合分析、政策匹配、对比判断类问题
    if (containsAny(normalized, List.of(
            "哪个更适合",
            "更适合我",
            "适合哪个",
            "适合什么",
            "怎么判断",
            "如何判断",
            "帮我分析",
            "分析一下",
            "比较一下",
            "对比一下",
            "哪个政策",
            "哪些政策适合",
            "可以申请哪些",
            "可以申请什么",
            "能申请哪些",
            "能申请什么",
            "我可以申请",
            "我能申请",
            "我适合",
            "帮我判断",
            "政策匹配"
    ))) {
        return true;
    }

    // 同一个问题里出现多个政策主题，也应交给 AI 综合判断
    return countMatchedPolicyTopics(normalized) >= 2;
}
private int countMatchedPolicyTopics(String question) {
    if (question == null) {
        return 0;
    }

    int count = 0;

    if (containsAny(question, List.of("博士后", "博士后工作站"))) {
        count++;
    }
    if (containsAny(question, List.of("住房", "住房补贴", "安居", "租房", "购房"))) {
        count++;
    }
    if (containsAny(question, List.of("AI", "人工智能", "AI人才", "人工智能人才"))) {
        count++;
    }
    if (containsAny(question, List.of("双百", "双百计划", "创新创业人才"))) {
        count++;
    }
    if (containsAny(question, List.of("特聘岗位"))) {
        count++;
    }
    if (containsAny(question, List.of("专项资金", "创业资金", "创业扶持资金"))) {
        count++;
    }
    if (containsAny(question, List.of("台湾特聘", "台湾专才", "台湾人才"))) {
        count++;
    }
    if (containsAny(question, List.of("福建省", "省级", "百人计划", "高层次人才认定"))) {
        count++;
    }
    if (containsAny(question, SERVICE_GUARANTEE_TERMS)) {
    count++;
}

    return count;
}
private boolean isPolicyEvidenceInsufficient(AiPolicyConsultService.ConsultResult result) {
    if (result == null) {
        return false;
    }

    AiPolicyConsultService.DiagnosticTrace trace = result.trace();
    String text = String.join(" ",
            valueOrBlank(result.answer()),
            valueOrBlank(trace == null ? null : trace.validationSummary()),
            
            valueOrBlank(trace == null ? null : trace.answerMode())
    );

    return containsAny(text, List.of(
            "依据不足",
            "未命中足够",
            "缺少完整",
            "暂不能把未命中的细节当作已确认事实",
            "当前未命中足够的 AI 政策专题证据",
            "当前知识库暂未命中足够"
    ));
}

private KnowledgeCitationContext resolvePolicyConsultContextOrSearch(AgentSessionEntity session,
                                                                     String question,
                                                                     String sourceScene,
                                                                     AiPolicyConsultService.ConsultResult policyConsultResult) {
    
       /*
     * 条件反查类问题：优先按扩展条件重新检索。
     * 例如“本科生可以申请什么政策”，需要检索“本科及以上、学士、申报条件、适用对象”等条件词。
     */
    if (isConditionReverseQuestion(question)) {
    KnowledgeCitationContext conditionContext = buildConditionReverseContext(
            session.getBaseId(),
            question,
            sourceScene,
            10
    );

    int hitCount = conditionContext == null || conditionContext.getChunks() == null
            ? 0
            : conditionContext.getChunks().size();

    logCenterService.recordAiChainSuccess(
            "POLICY_CONDITION_REVERSE_SEARCH",
            session.getId(),
            session.getUserId(),
            sourceScene,
            "question=" + valueOrBlank(normalizeText(question))
                    + ", candidates=" + String.join("|", buildConditionReverseCandidates(question))
                    + ", seedPolicies=" + String.join("|", resolveConditionReverseSeedPolicies(question))
                    + ", hitCount=" + hitCount
    );

    if (conditionContext != null
            && conditionContext.getChunks() != null
            && !conditionContext.getChunks().isEmpty()) {
        return conditionContext;
    }
}
   
    /*
 * 用户纠错或补充了政策依据时，优先按用户原话重新检索。
 * 例如用户补充“金融服务产业人才项目、本科及以上学历、CFA/FRM/CPA”等，
 * 这些词本身就是很强的检索线索。
 */
if (isUserCorrectionOrEvidenceSupplement(question)) {
    KnowledgeCitationContext rebuiltContext = buildContext(session.getBaseId(), question, 10, sourceScene);
    if (rebuiltContext != null
            && rebuiltContext.getChunks() != null
            && !rebuiltContext.getChunks().isEmpty()) {
        return rebuiltContext;
    }
}
    /*
     * 综合分析 / 多政策比较 / 个人匹配类问题：
     * 优先重新按原问题检索，尽量命中多个专题，而不是只使用 policyConsultResult 中偏向某一个专题的 context。
     */
    if (shouldUseLlmForPolicyAnalysis(question)) {
        KnowledgeCitationContext rebuiltContext = buildContext(session.getBaseId(), question, 8, sourceScene);
        if (rebuiltContext != null
                && rebuiltContext.getChunks() != null
                && !rebuiltContext.getChunks().isEmpty()) {
            return rebuiltContext;
        }
    }

    if (policyConsultResult != null
            && policyConsultResult.context() != null
            && policyConsultResult.context().getChunks() != null
            && !policyConsultResult.context().getChunks().isEmpty()) {
        return policyConsultResult.context();
    }

    return buildContext(session.getBaseId(), question, 5, sourceScene);
}

private boolean shouldBlockLlmBecauseNoPolicyEvidence(String sourceScene,
                                                      KnowledgeCitationContext context,
                                                      String question) {
    if (!SOURCE_SCENE_MOBILE_POLICY_CONSULTANT.equals(sourceScene)) {
        return false;
    }

    if (context != null && context.getChunks() != null && !context.getChunks().isEmpty()) {
        return false;
    }

    return looksLikePolicyQuestion(compactQuestionText(question));
}
private boolean isNarrowPolicyListQuestion(String question) {
    String text = normalizeText(question);
    if (text == null) {
        return false;
    }

    /*
     * 必须是“清单/有哪些”类表达，同时又带有明确限定词。
     * 这样可以避免误伤普通问题：
     * - 厦门有哪些人才政策        → 可以走总览
     * - 厦门有哪些涉及个税的条款  → 不能走总览
     */
    boolean looksLikeListQuestion = containsAny(text, List.of(
            "有哪些",
            "有什么",
            "包括哪些",
            "包含哪些",
            "涉及哪些",
            "哪些政策",
            "哪些条款",
            "哪些要求"
    ));

    if (!looksLikeListQuestion) {
        return false;
    }

    boolean hasNarrowKeyword = containsAny(text, List.of(
            "个税",
            "个人所得税",
            "社保个税",
            "纳税",
            "税收",
            "税务",
            "扣税",
            "综合经济贡献",

            "社保",
            "医保",
            "公积金",

            "材料",
            "申报材料",
            "材料清单",
            "证明材料",

            "流程",
            "办理流程",
            "申报流程",
            "怎么申请",
            "如何申请",

            "管理期",
            "服务期",
            "考核",
            "年度考核",

            "重复享受",
            "不能重复",
            "叠加享受",
            "同时享受",

            "拨付",
            "兑现",
            "发放",
            "资金拨付",

            "追回",
            "终止资助",
            "违规",
            "风险",
            "审计"
    ));

    return hasNarrowKeyword;
}

private boolean isUserCorrectionOrEvidenceSupplement(String question) {
    String text = normalizeText(question);
    if (text == null) {
        return false;
    }

    boolean correctionOrSupplement = containsAny(text, List.of(
            "不对",
            "不是",
            "应该",
            "我记得",
            "我看到",
            "我查到",
            "政策如下",
            "条款如下",
            "规定如下",
            "文件里面",
            "政策里面",
            "申报方式里面",
            "原文",
            "依据如下",
            "补充一下"
    ));

    boolean policyEvidenceLike = looksLikePolicyQuestion(text)
            || containsAny(text, List.of(
            "本科及以上",
            "本科以上",
            "金融服务产业人才",
            "金融服务产业人才项目",
            "CFA",
            "FRM",
            "CPA",
            "精算",
            "法律职业",
            "高级职称",
            "博士学位",
            "所在机构推荐申报",
            "申报方式"
    ));

    return correctionOrSupplement && policyEvidenceLike;
}

private boolean isContextDependentFollowupQuestion(String question) {
    String text = normalizeText(question);
    if (text == null) {
        return false;
    }

    return containsAny(text, List.of(
            "我可以申请什么",
            "我还能申请什么",
            "那我呢",
            "这个呢",
            "刚才那个",
            "上面那个",
            "还有吗",
            "继续",
            "再说一下",
            "重新整理",
            "总结一下",
            "帮我整理一下"
    ));
}
private String applyQuestionSubjectTone(String answer, String question) {
    if (normalizeText(answer) == null) {
        return answer;
    }

    String text = normalizeText(question);
    if (text == null) {
        return answer;
    }

    String result = answer;

    if (containsAny(text, List.of("我女儿", "女儿", "我儿子", "儿子", "孩子", "小孩"))) {
        if (!result.contains("帮女儿") && !result.contains("帮孩子") && !result.contains("和您本人情况分开")) {
            return "如果是帮孩子咨询，这个判断要和您本人情况分开看。\n\n" + result;
        }
    }

    if (containsAny(text, List.of("如果我是", "假如我是", "假设我是", "比如我是"))) {
        result = result
                .replace("结论：作为博士后", "结论：如果按博士后身份来看")
                .replace("作为博士后", "如果按博士后身份来看")
                .replace("您可申请", "一般可以重点关注")
                .replace("您可以申请", "一般可以重点关注");

        if (!result.contains("假设") && !result.contains("如果按")) {
            return "这是一个假设场景，我先按您提出的身份条件判断，不把它作为您本人长期画像。\n\n" + result;
        }
        return result;
    }

    if (isContextDependentFollowupQuestion(text) && !result.contains("上面已经") && !result.contains("前面已经")) {
        return "结合上面已经聊到的信息，我再帮您整理一下重点。\n\n" + result;
    }

    return result;
}

    private String resolveFastPathAnswer(String question,
                                         PolicyQuestionType questionType,
                                         String regionScope,
                                         String sourceScene,
                                         Long baseId,
                                         KnowledgeCitationContext context,
                                         ProviderResolution providerResolution) {
        if (isConditionReverseQuestion(question)) {
        return null;
    }
        if (isNarrowPolicyListQuestion(question)) {
        return null;
    }
        if (!SOURCE_SCENE_MOBILE_POLICY_CONSULTANT.equals(sourceScene)
                || providerResolution == null
                || !isFastPathQuestionType(questionType)) {
            return null;
        }
        PolicyRouteIntent routeIntent = policyRouteService.route(question);
        if (routeIntent.getRouteSkills().contains("XM_FJ_BOUNDARY_COMPARE")) {
            logCenterService.recordAiChainSuccess(
                    LOG_EVENT_POLICY_ROUTE_COMPARE,
                    null,
                    null,
                    sourceScene,
                    "question=" + valueOrBlank(normalizeText(question)) + ", routeSkills=" + String.join("|", routeIntent.getRouteSkills())
            );
        }
        PolicyCatalogService.CatalogAnswer catalogAnswer = policyCatalogService.buildCatalogAnswer(baseId, routeIntent, sourceScene);
        if (catalogAnswer.hasAnswer()) {
            if (routeIntent.getRouteSkills().contains("XM_FJ_BOUNDARY_COMPARE")) {
                logCenterService.recordAiChainSuccess(
                        LOG_EVENT_POLICY_BOUNDARY_CATALOG,
                        null,
                        null,
                        sourceScene,
                        "answerSource=FAST_PATH_CATALOG_COMPARE"
                );
            }
            return catalogAnswer.answer();
        }
        if (context == null || context.getChunks().size() < 2) {
            return null;
        }
        if ((questionType == PolicyQuestionType.CONDITION || questionType == PolicyQuestionType.BENEFIT)
                && isComplexPersonalizedQuestion(question)) {
            return null;
        }
        List<KnowledgeSearchResultVO> rankedChunks = selectFallbackChunks(question, questionType, regionScope, context.getChunks());
        if (!hasFastPathEvidence(question, questionType, regionScope, rankedChunks)) {
            return null;
        }
        return buildFastPathStructuredAnswer(question, questionType, regionScope, sourceScene, baseId, rankedChunks);
    }

    private boolean isFastPathQuestionType(PolicyQuestionType questionType) {
    /*
     * 只保留目录/清单类 FAST_PATH。
     * FAQ 已由 AiPolicyFaqService 精准命中处理。
     * 条件、补助、流程、比较、综合判断类问题交给外部 AI 基于知识库证据组织答案。
     */
    return questionType == PolicyQuestionType.LIST;
}

    private boolean isComplexPersonalizedQuestion(String question) {
        String normalized = normalizeText(question);
        if (normalized == null) {
            return false;
        }
        if (normalized.length() > 40) {
            return true;
        }
        if (normalized.matches(".*\\d+.*")) {
            return true;
        }
        return containsAny(normalized, List.of(
                "我是", "我们", "本人", "本公司", "企业", "公司",
                "本科", "硕士", "博士", "大专", "年龄", "年薪", "月薪",
                "缴税", "纳税", "社保", "合同", "劳动合同", "是否符合",
                "适不适合", "能申请哪些", "同时", "并且", "已在", "想申请"
        ));
    }

    private boolean hasFastPathEvidence(String question,
                                        PolicyQuestionType questionType,
                                        String regionScope,
                                        List<KnowledgeSearchResultVO> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return false;
        }
        if (questionType == PolicyQuestionType.LIST) {
            LinkedHashSet<String> policies = new LinkedHashSet<>();
            for (KnowledgeSearchResultVO item : chunks) {
                if (isFastPathEvidenceChunk(question, questionType, regionScope, item)) {
                    policies.add(resolvePolicyDisplayName(item));
                }
                if (policies.size() >= 2) {
                    return true;
                }
            }
            return false;
        }
        if (questionType == PolicyQuestionType.BOUNDARY) {
            boolean hasXm = false;
            boolean hasFj = false;
            for (KnowledgeSearchResultVO item : chunks) {
                if (!isFastPathEvidenceChunk(question, questionType, regionScope, item)) {
                    continue;
                }
                if ("XM".equalsIgnoreCase(normalizeText(item.getRegionScope()))) {
                    hasXm = true;
                }
                if ("FJ".equalsIgnoreCase(normalizeText(item.getRegionScope()))) {
                    hasFj = true;
                }
            }
            return hasXm || hasFj;
        }
        if (questionType == PolicyQuestionType.PROCESS) {
    for (KnowledgeSearchResultVO item : chunks) {
        if (isFastPathEvidenceChunk(question, questionType, regionScope, item)) {
            return true;
        }
    }
    return false;
}

int matched = 0;
for (KnowledgeSearchResultVO item : chunks) {
    if (isFastPathEvidenceChunk(question, questionType, regionScope, item)) {
        matched++;
    }
    if (matched >= 2) {
        return true;
    }
}
return false;
    }

    private boolean isFastPathEvidenceChunk(String question,
                                        PolicyQuestionType questionType,
                                        String regionScope,
                                        KnowledgeSearchResultVO item) {
    if (!isFallbackUsableChunk(item)) {
        return false;
    }
    if (questionType != PolicyQuestionType.BOUNDARY
            && normalizeText(regionScope) != null
            && !matchesRegionScope(item, regionScope)) {
        return false;
    }
    if (normalizeText(resolvePolicyDisplayName(item)) == null) {
        return false;
    }
    String topicType = normalizeText(item.getTopicType());
    return switch (questionType) {
        case LIST -> "list".equalsIgnoreCase(topicType)
                || "special_topic".equalsIgnoreCase(topicType)
                || "faq".equalsIgnoreCase(topicType)
                || normalizeText(item.getPolicyName()) != null;
        case FAQ -> "faq".equalsIgnoreCase(topicType)
                || normalizeText(item.getPolicyName()) != null;
        case PROCESS -> "process".equalsIgnoreCase(topicType)
                || isProcessChunk(item)
                || hasDoubleHundredApplyFlowEvidence(List.of(item));
        case CONDITION -> "condition".equalsIgnoreCase(topicType) || isConditionChunk(item);
        case BENEFIT -> "benefit".equalsIgnoreCase(topicType) || isBenefitChunk(item);
        case BOUNDARY -> normalizeText(item.getRegionScope()) != null
                && (normalizeText(item.getPolicyName()) != null || containsAny(buildChunkSearchText(item), extractStructuredSpecificPolicyTerms(question)));
        default -> false;
    };
}

    private String buildFastPathStructuredAnswer(String question,
                                             PolicyQuestionType questionType,
                                             String regionScope,
                                             String sourceScene,
                                             Long baseId,
                                             List<KnowledgeSearchResultVO> chunks) {
    return switch (questionType) {
        case LIST -> buildFastPathStrictListAnswer(question, regionScope, sourceScene, baseId, chunks);
        case FAQ -> buildFastPathFaqAnswer(regionScope, chunks);
        case BOUNDARY -> buildFastPathBoundaryAnswer(chunks);
        case CONDITION -> buildFastPathConditionAnswer(regionScope, chunks);
        case BENEFIT -> buildFastPathBenefitAnswer(regionScope, chunks);
        case PROCESS -> {
            if (isDoubleHundredApplyFlowQuestion(question, PolicyQuestionType.PROCESS)
                    && hasDoubleHundredApplyFlowEvidence(chunks)) {
                yield buildDoubleHundredApplyFlowAnswer();
            }
            yield buildProcessFallbackAnswer(question, regionScope, chunks);
        }
        default -> null;
    };
}

    private String buildFastPathListAnswer(String regionScope, List<KnowledgeSearchResultVO> chunks) {
        List<KnowledgeSearchResultVO> selected = new ArrayList<>();
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        for (KnowledgeSearchResultVO item : chunks) {
            String key = normalizeText(resolvePolicyDisplayName(item));
            if (key == null || seen.contains(key)) {
                continue;
            }
            seen.add(key);
            selected.add(item);
            if (selected.size() >= 6) {
                break;
            }
        }
        StringBuilder builder = new StringBuilder();
        builder.append("结论：");
        builder.append(resolveRegionLabel(regionScope) == null ? "当前知识库已命中一批人才政策/项目。" : resolveRegionLabel(regionScope) + "当前已命中一批人才政策/项目。");
        builder.append("\n政策/项目清单：\n");
        for (int i = 0; i < selected.size(); i++) {
            KnowledgeSearchResultVO item = selected.get(i);
            builder.append(i + 1).append(". ").append(resolvePolicyDisplayName(item)).append("：").append(buildListItemSummary(item)).append("\n");
        }
        builder.append("提示：以下为当前知识库已命中的主要政策/项目，不代表完整官方清单。\n");
        appendCitationsBlock(builder, collectFallbackCitations(selected, regionScope));
        return builder.toString().trim();
    }

    private String buildFastPathStrictListAnswer(String question,
                                                 String regionScope,
                                                 String sourceScene,
                                                 Long baseId,
                                                 List<KnowledgeSearchResultVO> chunks) {
        if (isHousingTopicListQuestion(question, sourceScene)) {
            String housingAnswer = buildHousingTopicListAnswer(baseId, question, regionScope, sourceScene, chunks);
            if (normalizeText(housingAnswer) != null) {
                return housingAnswer;
            }
        }
        if (isCatalogOverviewListQuestion(question, sourceScene)) {
            String catalogAnswer = buildCatalogListAnswer(baseId, question, regionScope, sourceScene, chunks);
            if (normalizeText(catalogAnswer) != null) {
                return catalogAnswer;
            }
        }
        List<FastPathListItem> selected = selectFastPathListItems(chunks);
        LinkedHashMap<ListPolicyGroup, List<FastPathListItem>> grouped = groupFastPathListItems(selected, 4);
        StringBuilder builder = new StringBuilder();
        String regionLabel = resolveRegionLabel(regionScope);
        builder.append("结论：");
        builder.append(regionLabel == null ? "当前知识库已命中的主要人才政策/项目如下。" : regionLabel + "当前已命中的主要人才政策/项目如下。");
        if (grouped.isEmpty()) {
            builder.append("\n政策/项目清单：\n");
            builder.append("1. 当前命中结果以说明性材料为主，暂未识别出可直接列示的正式政策名称。\n");
        } else {
            for (Map.Entry<ListPolicyGroup, List<FastPathListItem>> entry : grouped.entrySet()) {
                List<FastPathListItem> items = entry.getValue();
                if (items == null || items.isEmpty()) {
                    continue;
                }
                builder.append("\n").append(entry.getKey().label()).append("：\n");
                for (int i = 0; i < items.size(); i++) {
                    FastPathListItem fastPathItem = items.get(i);
                    builder.append(i + 1)
                            .append(". ")
                            .append(fastPathItem.policyName())
                            .append("：")
                            .append(buildListItemSummary(fastPathItem.chunk()))
                            .append("\n");
                }
            }
        }
        builder.append("以下为当前知识库已命中的主要政策/项目，不代表完整官方清单\n");
        appendCitationsBlock(builder, collectFallbackCitations(selected.stream().map(FastPathListItem::chunk).collect(Collectors.toList()), regionScope));
        return builder.toString().trim();
    }

    private boolean isHousingTopicListQuestion(String question, String sourceScene) {
        String normalized = normalizeText(question);
        if (!SOURCE_SCENE_MOBILE_POLICY_CONSULTANT.equals(sourceScene) || normalized == null) {
            return false;
        }
        return containsAny(normalized, LIST_QUESTION_KEYWORDS)
                && containsAny(normalized, List.of("住房", "住房补贴", "安居", "租房", "购房"));
    }

    private String buildHousingTopicListAnswer(Long baseId,
                                               String question,
                                               String regionScope,
                                               String sourceScene,
                                               List<KnowledgeSearchResultVO> chunks) {
        List<FastPathListItem> selected = collectXiamenHousingPolicyItems(baseId, question, regionScope, sourceScene, chunks);
        if (selected.isEmpty()) {
            return null;
        }
        String regionLabel = resolveRegionLabel(regionScope);
        StringBuilder builder = new StringBuilder();
        builder.append("结论：");
        builder.append(regionLabel == null ? "当前知识库已命中的住房类人才政策如下。" : regionLabel + "当前已命中的住房类人才政策如下。");
        builder.append("\n住房类政策清单：\n");
        for (int i = 0; i < selected.size(); i++) {
            FastPathListItem item = selected.get(i);
            builder.append(i + 1)
                    .append(". ")
                    .append(item.policyName())
                    .append("：")
                    .append(buildHousingListItemSummary(item.policyName(), item.chunk()))
                    .append("\n");
        }
        builder.append("以下为当前知识库已命中的主要政策/项目，不代表完整官方清单\n");
        appendCitationsBlock(builder, collectFallbackCitations(selected.stream().map(FastPathListItem::chunk).collect(Collectors.toList()), regionScope));
        return builder.toString().trim();
    }

    private List<FastPathListItem> collectXiamenHousingPolicyItems(Long baseId,
                                                                   String question,
                                                                   String regionScope,
                                                                   String sourceScene,
                                                                   List<KnowledgeSearchResultVO> chunks) {
        List<FastPathListItem> selected = new ArrayList<>();
        int order = 0;
        for (String policyName : resolveXiamenHousingWhitelist(question)) {
            List<KnowledgeSearchResultVO> matchedChunks = searchExactPolicyChunks(baseId, policyName, regionScope, sourceScene);
            if (matchedChunks.isEmpty()) {
                continue;
            }
            KnowledgeSearchResultVO exactChunk = resolveExactPolicySummaryChunk(policyName, matchedChunks, chunks, List.of());
            if (exactChunk == null || !isHousingTopicChunk(exactChunk)) {
                continue;
            }
            selected.add(new FastPathListItem(
                    exactChunk,
                    policyName,
                    ListPolicyGroup.PUBLIC,
                    4000 - order++
            ));
        }
        return selected;
    }

    private List<String> resolveXiamenHousingWhitelist(String question) {
        LinkedHashSet<String> whitelist = new LinkedHashSet<>();
        whitelist.add("厦门市引进高层次人才住房补贴实施意见");
        if (containsAny(normalizeText(question), List.of("安居", "住房"))) {
            whitelist.add("厦门市高层次人才安居政策");
        }
        if (containsAny(normalizeText(question), List.of("租房", "住房"))) {
            whitelist.add("厦门市高层次人才租房支持政策");
        }
        if (containsAny(normalizeText(question), List.of("购房", "住房"))) {
            whitelist.add("厦门市高层次人才购房支持政策");
        }
        return new ArrayList<>(whitelist);
    }

    private boolean isHousingTopicChunk(KnowledgeSearchResultVO item) {
        if (!isFastPathListUsableChunk(item)) {
            return false;
        }
        String text = buildChunkSearchText(item);
        return containsAny(text, List.of("住房", "住房补贴", "安居", "租房", "购房"));
    }

    private String resolveHousingPolicyName(KnowledgeSearchResultVO item) {
        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        String directPolicyName = canonicalizePolicyListName(resolveFastPathListPolicyName(item));
        if (normalizeText(directPolicyName) != null) {
            candidates.add(directPolicyName);
        }
        candidates.addAll(extractWeakPolicyNames(valueOrBlank(item == null ? null : item.getPolicyName())));
        candidates.addAll(extractWeakPolicyNames(valueOrBlank(item == null ? null : item.getDocTitle())));
        candidates.addAll(extractWeakPolicyNames(valueOrBlank(item == null ? null : item.getChapterTitle())));
        candidates.addAll(extractWeakPolicyNames(valueOrBlank(item == null ? null : item.getSectionTitle())));
        candidates.addAll(extractWeakPolicyNames(valueOrBlank(item == null ? null : item.getHeadingPath())));
        candidates.addAll(extractWeakPolicyNames(valueOrBlank(item == null ? null : item.getSnippet())));
        candidates.addAll(extractWeakPolicyNames(buildChunkSearchText(item)));
        for (String candidate : candidates) {
            String normalized = canonicalizePolicyListName(candidate);
            if (isValidHousingPolicyListItem(normalized)) {
                return normalized;
            }
        }
        String text = buildChunkSearchText(item);
        if (containsAny(text, List.of("住房", "住房补贴", "安居", "租房", "购房"))) {
            return "厦门市引进高层次人才住房补贴实施意见";
        }
        return null;
    }

    private boolean isValidHousingPolicyListItem(String text) {
        String candidate = canonicalizePolicyListName(text);
        if (candidate == null || isInvalidPolicyListItem(candidate)) {
            return false;
        }
        if (containsAny(candidate, List.of("2025年厦门市人才", "问答增强版", "使用边界", "检索建议", "导入说明"))) {
            return false;
        }
        if (!containsAny(candidate, List.of("住房", "住房补贴", "安居", "租房", "购房"))) {
            return false;
        }
        return candidate.endsWith("实施意见")
                || candidate.endsWith("实施方案")
                || candidate.endsWith("管理办法")
                || candidate.endsWith("若干措施")
                || candidate.endsWith("通知")
                || candidate.endsWith("工作方案")
                || candidate.endsWith("项目实施办法")
                || candidate.endsWith("意见")
                || "厦门市引进高层次人才住房补贴实施意见".equals(candidate);
    }

    private boolean isCatalogOverviewListQuestion(String question, String sourceScene) {
        String normalized = normalizeText(question);
        if (!SOURCE_SCENE_MOBILE_POLICY_CONSULTANT.equals(sourceScene) || normalized == null) {
            return false;
        }
        boolean regionMatched = containsAny(normalized, List.of("厦门", "厦门市", "福建", "福建省"));
        boolean listMatched = containsAny(normalized, LIST_QUESTION_KEYWORDS);
        boolean topicSpecific = !extractFormalPolicyNames(question).isEmpty() || !extractMatchedSpecialTopicTerms(question).isEmpty();
        return regionMatched && listMatched && !topicSpecific;
    }

    private String buildCatalogListAnswer(Long baseId,
                                          String question,
                                          String regionScope,
                                          String sourceScene,
                                          List<KnowledgeSearchResultVO> chunks) {
        List<KnowledgeSearchResultVO> catalogChunks = searchCatalogOverviewChunks(baseId, question, regionScope, sourceScene);
        if (catalogChunks.isEmpty()) {
            return null;
        }
        List<FastPathListItem> selected = isXiamenOverviewListQuestion(question, regionScope, sourceScene)
                ? collectXiamenOverviewPolicyItems(baseId, regionScope, sourceScene, chunks, catalogChunks)
                : collectCatalogPolicyItems(catalogChunks, chunks);
        if (selected.isEmpty()) {
            return null;
        }
        LinkedHashMap<ListPolicyGroup, List<FastPathListItem>> grouped = groupFastPathListItems(selected, 5);
        if (grouped.isEmpty()) {
            return null;
        }
        String regionLabel = resolveRegionLabel(regionScope);
        StringBuilder builder = new StringBuilder();
        builder.append("结论：");
        builder.append(regionLabel == null ? "当前知识库已命中的主要人才政策/项目如下。" : regionLabel + "当前已命中的主要人才政策/项目如下。");
        for (Map.Entry<ListPolicyGroup, List<FastPathListItem>> entry : grouped.entrySet()) {
            List<FastPathListItem> items = entry.getValue();
            if (items == null || items.isEmpty()) {
                continue;
            }
            builder.append("\n").append(entry.getKey().label()).append("：\n");
            for (int i = 0; i < items.size(); i++) {
                FastPathListItem item = items.get(i);
                builder.append(i + 1)
                        .append(". ")
                        .append(item.policyName())
                        .append("：")
                        .append(buildListItemSummary(item.policyName(), item.chunk()))
                        .append("\n");
            }
        }
        builder.append("以下为当前知识库已命中的主要政策/项目，不代表完整官方清单\n");
        appendCitationsBlock(builder, collectFallbackCitations(selected.stream().map(FastPathListItem::chunk).collect(Collectors.toList()), regionScope));
        return builder.toString().trim();
    }

    private boolean isXiamenOverviewListQuestion(String question, String regionScope, String sourceScene) {
        if (!isCatalogOverviewListQuestion(question, sourceScene)) {
            return false;
        }
        String normalizedQuestion = normalizeText(question);
        String normalizedRegion = normalizeText(regionScope);
        return containsAny(normalizedQuestion, List.of("厦门", "厦门市"))
                || containsKeyword(normalizedRegion, "厦门")
                || "xiamen".equalsIgnoreCase(normalizedRegion)
                || "xm".equalsIgnoreCase(normalizedRegion);
    }

    private List<KnowledgeSearchResultVO> searchCatalogOverviewChunks(Long baseId,
                                                                      String question,
                                                                      String regionScope,
                                                                      String sourceScene) {
        if (baseId == null) {
            return List.of();
        }
        LinkedHashMap<Long, KnowledgeSearchResultVO> collected = new LinkedHashMap<>();
        for (String keyword : resolveCatalogOverviewKeywords(question, regionScope)) {
            KnowledgeSearchRequest request = new KnowledgeSearchRequest();
            request.setBaseId(baseId);
            request.setKeywords(keyword);
            request.setQuestionType("list");
            request.setRegionScope(regionScope);
            request.setDocType("main");
            request.setScenePriority(normalizeSourceScene(sourceScene, sourceScene));
            request.setSearchable(Boolean.TRUE);
            request.setEffectiveOnly(Boolean.TRUE);
            request.setTopN(8);
            List<KnowledgeSearchResultVO> list = knowledgeChunkMapper.search(request);
            if (list == null || list.isEmpty()) {
                continue;
            }
            for (KnowledgeSearchResultVO item : list) {
                if (!isCatalogOverviewChunk(item, question, regionScope)) {
                    continue;
                }
                Long chunkId = item.getChunkId();
                if (chunkId != null) {
                    collected.putIfAbsent(chunkId, item);
                }
            }
        }
        List<KnowledgeSearchResultVO> result = new ArrayList<>(collected.values());
        result.sort(Comparator.comparing(item -> item.getChunkNo() == null ? Integer.MAX_VALUE : item.getChunkNo()));
        return result;
    }

    private List<String> resolveCatalogOverviewKeywords(String question, String regionScope) {
        LinkedHashSet<String> keywords = new LinkedHashSet<>();
        boolean fj = isFujianListQuestion(question, regionScope);
        if (fj) {
            keywords.add("福建省人才政策总览");
            keywords.add("省级主干政策");
            keywords.add("2025年申报政策目录");
            keywords.add("专项支持");
            keywords.add("四大经济");
            keywords.add("博士后");
            keywords.add("台湾人才");
            keywords.add("福建省人才政策（清洗导入版 v2）");
        } else {
            keywords.add("厦门市人才政策总览");
            keywords.add("市级统领政策");
            keywords.add("产业人才项目总览");
            keywords.add("公共类专项人才项目");
            keywords.add("厦门市人才政策（清洗导入版 v2）");
        }
        return new ArrayList<>(keywords);
    }

    private boolean isCatalogOverviewChunk(KnowledgeSearchResultVO item, String question, String regionScope) {
        if (!isFastPathListUsableChunk(item) || !"main".equalsIgnoreCase(normalizeText(item.getDocType()))) {
            return false;
        }
        if (normalizeText(regionScope) != null && !matchesRegionScope(item, regionScope)) {
            return false;
        }
        String headingText = String.join(" ",
                valueOrBlank(item.getDocTitle()),
                valueOrBlank(item.getHeadingPath()),
                valueOrBlank(item.getChapterTitle()),
                valueOrBlank(item.getSectionTitle()));
        return containsAny(headingText, resolveCatalogOverviewKeywords(question, regionScope));
    }

    private List<FastPathListItem> collectCatalogPolicyItems(List<KnowledgeSearchResultVO> catalogChunks,
                                                             List<KnowledgeSearchResultVO> supplementChunks) {
        LinkedHashMap<String, FastPathListItem> items = new LinkedHashMap<>();
        for (KnowledgeSearchResultVO chunk : catalogChunks) {
            for (String policyName : extractCatalogPolicyNames(chunk)) {
                String normalized = normalizeText(policyName);
                if (normalized == null || items.containsKey(normalized)) {
                    continue;
                }
                KnowledgeSearchResultVO summaryChunk = resolveCatalogSummaryChunk(policyName, supplementChunks, chunk);
                boolean fromPolicyName = summaryChunk != null
                        && normalizeText(summaryChunk.getPolicyName()) != null
                        && normalizeText(summaryChunk.getPolicyName()).equalsIgnoreCase(normalized);
                items.put(normalized, new FastPathListItem(
                        summaryChunk == null ? chunk : summaryChunk,
                        policyName,
                        classifyListPolicyGroup(policyName),
                        scoreCatalogPolicyItem(summaryChunk == null ? chunk : summaryChunk, fromPolicyName)
                ));
            }
        }
        return items.values().stream()
                .sorted(Comparator.comparingInt(FastPathListItem::score).reversed()
                        .thenComparing(item -> item.chunk().getChunkNo() == null ? Integer.MAX_VALUE : item.chunk().getChunkNo()))
                .limit(15)
                .collect(Collectors.toList());
    }

    private List<FastPathListItem> collectXiamenOverviewPolicyItems(Long baseId,
                                                                    String regionScope,
                                                                    String sourceScene,
                                                                    List<KnowledgeSearchResultVO> supplementChunks,
                                                                    List<KnowledgeSearchResultVO> catalogChunks) {
        List<FastPathListItem> selected = new ArrayList<>();
        int order = 0;
        for (Map.Entry<ListPolicyGroup, List<String>> entry : resolveXiamenOverviewWhitelist().entrySet()) {
            for (String policyName : entry.getValue()) {
                List<KnowledgeSearchResultVO> matchedChunks = searchExactPolicyChunks(baseId, policyName, regionScope, sourceScene);
                if (matchedChunks.isEmpty()) {
                    continue;
                }
                KnowledgeSearchResultVO exactChunk = resolveExactPolicySummaryChunk(policyName, matchedChunks, supplementChunks, catalogChunks);
                if (exactChunk == null) {
                    continue;
                }
                selected.add(new FastPathListItem(
                        exactChunk,
                        policyName,
                        entry.getKey(),
                        5000 - order++
                ));
            }
        }
        return selected;
    }

    private LinkedHashMap<ListPolicyGroup, List<String>> resolveXiamenOverviewWhitelist() {
        LinkedHashMap<ListPolicyGroup, List<String>> groups = new LinkedHashMap<>();
        groups.put(ListPolicyGroup.LEADING, List.of(
                "厦门市引进高层次创新创业人才“双百计划”实施意见",
                "厦门市高层次人才特聘岗位实施方案",
                "厦门市高层次人才专项资金管理办法",
                "关于更加精准有效集聚人才加快推进高质量发展的意见"
        ));
        groups.put(ListPolicyGroup.INDUSTRY, List.of(
                "厦门市电子信息产业人才项目实施办法",
                "厦门市机械装备产业人才项目实施办法",
                "厦门市商贸物流产业人才项目实施办法",
                "厦门市金融服务产业人才项目实施办法",
                "厦门市生物医药产业人才项目实施办法",
                "厦门市新能源和新材料产业人才项目实施办法",
                "厦门市文旅创意产业人才项目实施办法",
                "厦门市海洋经济人才项目实施办法",
                "厦门市重点产业骨干人才项目实施办法"
        ));
        groups.put(ListPolicyGroup.PUBLIC, List.of(
                "厦门市教育人才项目实施办法",
                "厦门市卫生健康人才项目实施办法",
                "厦门市社会工作人才项目实施办法",
                "厦门市台湾特聘专家（专才）项目实施办法"
        ));
        return groups;
    }

    private List<KnowledgeSearchResultVO> searchExactPolicyChunks(Long baseId,
                                                                  String policyName,
                                                                  String regionScope,
                                                                  String sourceScene) {
        if (baseId == null || normalizeText(policyName) == null) {
            return List.of();
        }
        LinkedHashMap<Long, KnowledgeSearchResultVO> collected = new LinkedHashMap<>();
        List<String> searchTerms = resolvePolicySearchTerms(policyName);
        for (String searchTerm : searchTerms) {
            KnowledgeSearchRequest request = new KnowledgeSearchRequest();
            request.setBaseId(baseId);
            request.setKeywords(searchTerm);
            request.setQuestionType("list");
            request.setRegionScope(regionScope);
            request.setScenePriority(normalizeSourceScene(sourceScene, sourceScene));
            request.setSearchable(Boolean.TRUE);
            request.setEffectiveOnly(Boolean.TRUE);
            request.setTopN(8);
            List<KnowledgeSearchResultVO> list = knowledgeChunkMapper.search(request);
            if (list == null || list.isEmpty()) {
                continue;
            }
            for (KnowledgeSearchResultVO item : list) {
                if (!isFastPathListUsableChunk(item)) {
                    continue;
                }
                Long chunkId = item.getChunkId();
                if (chunkId != null) {
                    collected.putIfAbsent(chunkId, item);
                }
            }
        }
        if (collected.isEmpty()) {
            return List.of();
        }
        List<KnowledgeSearchResultVO> matched = new ArrayList<>();
        String canonical = canonicalizePolicyListName(policyName);
        for (KnowledgeSearchResultVO item : collected.values()) {
            if (matchesExactPolicyCandidate(item, canonical, searchTerms)) {
                matched.add(item);
            }
        }
        return matched;
    }

    private List<String> resolvePolicySearchTerms(String policyName) {
        LinkedHashSet<String> terms = new LinkedHashSet<>();
        String canonical = canonicalizePolicyListName(policyName);
        if (normalizeText(canonical) != null) {
            terms.add(canonical);
        }
        if (normalizeText(policyName) != null) {
            terms.add(policyName);
        }
        for (PolicyAliasMapping mapping : POLICY_ALIAS_MAPPINGS) {
            if (!normalizeText(mapping.canonicalName()).equalsIgnoreCase(normalizeText(canonical))) {
                continue;
            }
            terms.addAll(mapping.aliases());
            String compactName = mapping.canonicalName().replace("厦门市", "").replace("福建省", "").trim();
            if (normalizeText(compactName) != null) {
                terms.add(compactName);
            }
            break;
        }
        return new ArrayList<>(terms);
    }

    private KnowledgeSearchResultVO resolveExactPolicySummaryChunk(String policyName,
                                                                   List<KnowledgeSearchResultVO> primaryChunks,
                                                                   List<KnowledgeSearchResultVO> supplementChunks,
                                                                   List<KnowledgeSearchResultVO> catalogChunks) {
        KnowledgeSearchResultVO exact = resolveExactPolicyChunk(policyName, primaryChunks);
        if (exact != null) {
            return exact;
        }
        exact = resolveExactPolicyChunk(policyName, supplementChunks);
        if (exact != null) {
            return exact;
        }
        return resolveExactPolicyChunk(policyName, catalogChunks);
    }

    private KnowledgeSearchResultVO resolveExactPolicyChunk(String policyName, List<KnowledgeSearchResultVO> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return null;
        }
        String canonical = canonicalizePolicyListName(policyName);
        List<String> searchTerms = resolvePolicySearchTerms(policyName);
        for (KnowledgeSearchResultVO item : chunks) {
            if (!isFastPathListUsableChunk(item)) {
                continue;
            }
            String itemPolicyName = canonicalizePolicyListName(item.getPolicyName());
            if (normalizeText(itemPolicyName) != null && normalizeText(itemPolicyName).equalsIgnoreCase(normalizeText(canonical))) {
                return item;
            }
        }
        for (KnowledgeSearchResultVO item : chunks) {
            if (!isFastPathListUsableChunk(item)) {
                continue;
            }
            if (matchesExactPolicyCandidate(item, canonical, searchTerms)) {
                return item;
            }
        }
        return null;
    }

    private boolean matchesExactPolicyCandidate(KnowledgeSearchResultVO item, String canonicalPolicyName, List<String> searchTerms) {
        if (!isFastPathListUsableChunk(item)) {
            return false;
        }
        String itemPolicyName = canonicalizePolicyListName(item.getPolicyName());
        if (normalizeText(itemPolicyName) != null
                && normalizeText(itemPolicyName).equalsIgnoreCase(normalizeText(canonicalPolicyName))) {
            return true;
        }
        String text = buildChunkSearchText(item);
        for (String term : searchTerms) {
            if (containsKeyword(text, term)) {
                return true;
            }
        }
        return false;
    }

    private List<String> extractCatalogPolicyNames(KnowledgeSearchResultVO item) {
        LinkedHashSet<String> names = new LinkedHashSet<>();
        if (item == null) {
            return List.of();
        }
        names.addAll(extractWeakPolicyNames(valueOrBlank(item.getPolicyName())));
        names.addAll(extractWeakPolicyNames(valueOrBlank(item.getSnippet())));
        names.addAll(extractWeakPolicyNames(valueOrBlank(item.getHeadingPath())));
        names.addAll(extractWeakPolicyNames(valueOrBlank(item.getSectionTitle())));
        names.addAll(extractWeakPolicyNames(valueOrBlank(item.getChapterTitle())));
        List<String> resolved = new ArrayList<>();
        for (String name : names) {
            String canonical = canonicalizePolicyListName(name);
            if (isValidFormalPolicyListItem(canonical)) {
                resolved.add(canonical);
            }
        }
        return resolved;
    }

    private KnowledgeSearchResultVO resolveCatalogSummaryChunk(String policyName,
                                                               List<KnowledgeSearchResultVO> supplementChunks,
                                                               KnowledgeSearchResultVO fallbackChunk) {
        String canonical = canonicalizePolicyListName(policyName);
        for (KnowledgeSearchResultVO item : supplementChunks) {
            if (!isFastPathListUsableChunk(item)) {
                continue;
            }
            String itemName = canonicalizePolicyListName(resolveFastPathListPolicyName(item));
            if (normalizeText(itemName) != null && normalizeText(itemName).equalsIgnoreCase(normalizeText(canonical))) {
                return item;
            }
            if (containsKeyword(buildChunkSearchText(item), policyName) || containsKeyword(buildChunkSearchText(item), canonical)) {
                return item;
            }
        }
        return fallbackChunk;
    }

    private int scoreCatalogPolicyItem(KnowledgeSearchResultVO item, boolean fromPolicyName) {
        int score = scoreFastPathListItem(item, fromPolicyName);
        if ("main".equalsIgnoreCase(normalizeText(item.getDocType()))) {
            score += 400;
        }
        String headingText = String.join(" ",
                valueOrBlank(item.getHeadingPath()),
                valueOrBlank(item.getChapterTitle()),
                valueOrBlank(item.getSectionTitle()));
        if (containsAny(headingText, List.of("总览", "统领", "目录", "专项", "项目总览"))) {
            score += 120;
        }
        return score;
    }

    private boolean isFujianListQuestion(String question, String regionScope) {
        String normalizedQuestion = normalizeText(question);
        String normalizedRegion = normalizeText(regionScope);
        return containsAny(normalizedQuestion, List.of("福建", "福建省"))
                || "fujian".equalsIgnoreCase(normalizedRegion)
                || "fj".equalsIgnoreCase(normalizedRegion)
                || containsKeyword(normalizedRegion, "福建");
    }

    private List<FastPathListItem> selectFastPathListItems(List<KnowledgeSearchResultVO> chunks) {
        List<FastPathListItem> ranked = new ArrayList<>();
        for (KnowledgeSearchResultVO item : chunks) {
            if (!isFastPathListUsableChunk(item)) {
                continue;
            }
            String policyName = resolveFastPathListPolicyName(item);
            if (policyName == null) {
                continue;
            }
            boolean fromPolicyName = normalizeText(item.getPolicyName()) != null
                    && policyName.equals(item.getPolicyName().trim());
            ranked.add(new FastPathListItem(
                    item,
                    policyName,
                    classifyListPolicyGroup(policyName),
                    scoreFastPathListItem(item, fromPolicyName)
            ));
        }
        ranked.sort(Comparator
                .comparingInt(FastPathListItem::score).reversed()
                .thenComparing(item -> item.chunk().getChunkNo() == null ? Integer.MAX_VALUE : item.chunk().getChunkNo()));
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        List<FastPathListItem> selected = new ArrayList<>();
        for (FastPathListItem item : ranked) {
            String key = normalizeText(item.policyName());
            if (key == null || seen.contains(key)) {
                continue;
            }
            seen.add(key);
            selected.add(item);
            if (selected.size() >= 12) {
                break;
            }
        }
        return selected;
    }

    private LinkedHashMap<ListPolicyGroup, List<FastPathListItem>> groupFastPathListItems(List<FastPathListItem> items, int maxPerGroup) {
        LinkedHashMap<ListPolicyGroup, List<FastPathListItem>> grouped = new LinkedHashMap<>();
        grouped.put(ListPolicyGroup.LEADING, new ArrayList<>());
        grouped.put(ListPolicyGroup.INDUSTRY, new ArrayList<>());
        grouped.put(ListPolicyGroup.PUBLIC, new ArrayList<>());
        if (items == null || items.isEmpty()) {
            return new LinkedHashMap<>();
        }
        for (FastPathListItem item : items) {
            List<FastPathListItem> bucket = grouped.get(item.group());
            if (bucket == null) {
                bucket = grouped.get(ListPolicyGroup.INDUSTRY);
            }
            if (bucket.size() >= maxPerGroup) {
                continue;
            }
            bucket.add(item);
        }
        grouped.entrySet().removeIf(entry -> entry.getValue() == null || entry.getValue().isEmpty());
        return grouped;
    }

    private boolean isFastPathListUsableChunk(KnowledgeSearchResultVO item) {
        if (!isFallbackUsableChunk(item)) {
            return false;
        }
        if ("routing".equalsIgnoreCase(normalizeText(item.getTopicType()))
                || "route_help".equalsIgnoreCase(normalizeText(item.getTopicType()))) {
            return false;
        }
        String metadataText = String.join(" ",
                valueOrBlank(item.getDocType()),
                valueOrBlank(item.getTopicType()),
                valueOrBlank(item.getDocTitle()),
                valueOrBlank(item.getHeadingPath()),
                valueOrBlank(item.getChapterTitle()),
                valueOrBlank(item.getSectionTitle()));
        return !containsAny(metadataText, List.of(
                "问答增强版", "适合作为知识库专题补充文档", "使用边界", "检索建议", "导入说明", "导入目录",
                "文档定位", "高频问答", "路由建议"
        ));
    }

    private String resolveFastPathListPolicyName(KnowledgeSearchResultVO item) {
        if (item == null) {
            return null;
        }
        String policyName = canonicalizePolicyListName(item.getPolicyName());
        if (isValidFormalPolicyListItem(policyName)) {
            return policyName;
        }
        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        candidates.addAll(extractWeakPolicyNames(valueOrBlank(item.getPolicyName())));
        candidates.addAll(extractWeakPolicyNames(valueOrBlank(item.getDocTitle())));
        candidates.addAll(extractWeakPolicyNames(valueOrBlank(item.getChapterTitle())));
        candidates.addAll(extractWeakPolicyNames(valueOrBlank(item.getSectionTitle())));
        candidates.addAll(extractWeakPolicyNames(valueOrBlank(item.getHeadingPath())));
        candidates.addAll(extractWeakPolicyNames(valueOrBlank(item.getSnippet())));
        candidates.addAll(extractWeakPolicyNames(buildChunkSearchText(item)));
        for (String candidate : candidates) {
            String normalized = canonicalizePolicyListName(candidate);
            if (isValidFormalPolicyListItem(normalized)) {
                return normalized;
            }
        }
        return null;
    }

    private String canonicalizePolicyListName(String text) {
        String candidate = normalizePolicyListCandidate(text);
        if (candidate == null) {
            return null;
        }
        for (PolicyAliasMapping mapping : POLICY_ALIAS_MAPPINGS) {
            if (containsKeyword(candidate, mapping.canonicalName()) || containsKeyword(mapping.canonicalName(), candidate)) {
                return mapping.canonicalName();
            }
            for (String alias : mapping.aliases()) {
                if (containsKeyword(candidate, alias) || containsKeyword(alias, candidate)) {
                    return mapping.canonicalName();
                }
            }
        }
        return candidate;
    }

    private List<String> extractWeakPolicyNames(String text) {
        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        String normalized = normalizeText(text);
        if (normalized == null) {
            return List.of();
        }
        candidates.addAll(PolicyKnowledgeSupport.extractFormalPolicyNames(normalized));
        Matcher quotedMatcher = Pattern.compile("《[^》]{2,60}》").matcher(normalized);
        while (quotedMatcher.find()) {
            candidates.add(quotedMatcher.group());
        }
        Matcher projectMatcher = Pattern.compile("[\\u4e00-\\u9fa5A-Za-z0-9（）()]{2,40}(人才项目|骨干人才项目|特聘专家（专才）项目|特聘专家\\(专才\\)项目)").matcher(normalized);
        while (projectMatcher.find()) {
            candidates.add(projectMatcher.group());
        }
        for (String segment : normalized.split("[\\r\\n|｜/>→；;。！？]")) {
            String candidate = normalizePolicyListCandidate(segment);
            if (candidate != null) {
                candidates.add(candidate);
            }
        }
        return new ArrayList<>(candidates);
    }

    private String normalizePolicyListCandidate(String text) {
        String normalized = normalizeText(text);
        if (normalized == null) {
            return null;
        }
        String candidate = normalized
                .replace('\u3000', ' ')
                .replace("“", "")
                .replace("”", "")
                .trim();
        candidate = candidate.replaceAll("^[0-9一二三四五六七八九十]+[.、]\\s*", "");
        candidate = candidate.replaceAll("^(正文|资料|Chunk|文档定位|导入说明|导入目录|使用边界|检索建议|高频问答|路由建议|政策依据)[:：]\\s*", "");
        candidate = candidate.replaceAll("[,，;；。！？!?.]+$", "").trim();
        return normalizeText(candidate);
    }

    private boolean isValidFormalPolicyListItem(String text) {
        String candidate = normalizePolicyListCandidate(text);
        if (candidate == null || candidate.length() < 4 || candidate.length() > 60) {
            return false;
        }
        if (isInvalidPolicyListItem(candidate)) {
            return false;
        }
        if (candidate.startsWith("《") && candidate.endsWith("》")) {
            return true;
        }
        for (String suffix : List.of("实施意见", "实施方案", "管理办法", "若干措施", "通知", "工作方案", "项目实施办法", "意见")) {
            if (candidate.endsWith(suffix)) {
                return true;
            }
        }
        return looksLikeTalentProjectName(candidate);
    }

    private boolean isInvalidPolicyListItem(String text) {
        String normalized = normalizePolicyListCandidate(text);
        if (normalized == null) {
            return true;
        }
        if (containsAny(normalized, List.of(
                "A类人才项目", "B类人才项目", "C类人才项目", "创新个人", "创新团队", "创业人才",
                "制造业与软件信息（人工智能）产业人才项目", "制造业与软件信息(人工智能)产业人才项目"
        )) || normalized.endsWith("类人才项目")) {
            return true;
        }
        if (containsAny(normalized, List.of(
                "问答增强版", "适合作为知识库专题补充文档", "使用边界", "检索建议", "导入说明", "导入目录",
                "文档定位", "正文", "高频问答", "路由建议", "政策依据", "资料", "Chunk",
                "如需回答", "应继续命中", "适合作为", "建议作为"
        ))) {
            return true;
        }
        return normalized.contains("：")
                || normalized.contains(":")
                || normalized.contains("，")
                || normalized.contains(",")
                || normalized.contains("。");
    }

    private boolean looksLikeTalentProjectName(String text) {
        String normalized = normalizePolicyListCandidate(text);
        if (normalized == null || normalized.length() > 40) {
            return false;
        }
        if (normalized.endsWith("项目") && normalized.contains("人才")) {
            return true;
        }
        return normalized.contains("人才项目")
                || normalized.contains("骨干人才项目")
                || normalized.contains("特聘专家（专才）项目")
                || normalized.contains("特聘专家(专才)项目");
    }

    private int scoreFastPathListItem(KnowledgeSearchResultVO item, boolean fromPolicyName) {
        int score = 0;
        String docType = normalizeText(item.getDocType());
        if ("main".equalsIgnoreCase(docType) && fromPolicyName) {
            score += 1000;
        } else if ("topic".equalsIgnoreCase(docType) && fromPolicyName) {
            score += 800;
        } else if (fromPolicyName) {
            score += 600;
        } else {
            score += 300;
        }
        if (Boolean.TRUE.equals(item.getSearchable())) {
            score += 20;
        }
        return score;
    }

    private ListPolicyGroup classifyListPolicyGroup(String policyName) {
        if (containsAny(policyName, List.of("教育人才", "卫生健康人才", "社会工作人才", "台湾特聘专家", "住房", "博士后", "子女教育", "医疗保障", "服务保障"))) {
            return ListPolicyGroup.PUBLIC;
        }
        if (containsAny(policyName, List.of("双百计划", "特聘岗位", "专项资金", "百人计划"))
                || (policyName.contains("意见") && !policyName.contains("项目"))) {
            return ListPolicyGroup.LEADING;
        }
        return ListPolicyGroup.INDUSTRY;
    }

    private String buildFastPathFaqAnswer(String regionScope, List<KnowledgeSearchResultVO> chunks) {
        List<String> points = extractFallbackClauses(chunks, List.of("对象", "条件", "流程", "支持", "补贴", "奖励"), 2);
        StringBuilder builder = new StringBuilder();
        builder.append("结论：");
        builder.append(resolveRegionLabel(regionScope) == null ? "当前知识库已命中相关政策依据。" : "当前已按" + resolveRegionLabel(regionScope) + "口径命中相关政策依据。");
        builder.append("\n关键信息：\n");
        for (int i = 0; i < points.size(); i++) {
            builder.append(i + 1).append(". ").append(points.get(i)).append("\n");
        }
        builder.append("提示：如需继续聚焦流程、条件或金额，可继续补充具体政策名称或申报对象。\n");
        appendCitationsBlock(builder, collectFallbackCitations(chunks, regionScope));
        return builder.toString().trim();
    }

    private String buildFastPathBoundaryAnswer(List<KnowledgeSearchResultVO> chunks) {
        StringBuilder builder = new StringBuilder();
        builder.append("结论：当前知识库同时包含厦门市级和福建省级人才政策，不能混答为同一层级清单。\n");
        builder.append("边界说明：\n");
        builder.append("1. 厦门市级政策：优先用于回答厦门本地项目、住房补贴、产业人才项目等落地政策。\n");
        builder.append("2. 福建省级政策：优先用于回答省级认定、全省专项支持或省级引才计划。\n");
        builder.append("提示：如需落地办理，请先明确是问福建省级政策还是厦门市执行政策。\n");
        appendCitationsBlock(builder, collectFallbackCitations(chunks, null));
        return builder.toString().trim();
    }

    private String buildFastPathConditionAnswer(String regionScope, List<KnowledgeSearchResultVO> chunks) {
        LinkedHashSet<String> points = new LinkedHashSet<>();
        addFallbackPoint(points, "适用对象", chunks, List.of("适用对象", "对象", "支持对象", "申报对象"));
        addFallbackPoint(points, "基本条件", chunks, List.of("基本条件", "申报条件", "资格", "要求", "认定条件"));
        addFallbackPoint(points, "学历年龄", chunks, List.of("学历", "学位", "年龄"));
        addFallbackPoint(points, "合同缴税", chunks, List.of("劳动合同", "合同", "缴税", "纳税", "社保"));
        StringBuilder builder = new StringBuilder();
        builder.append("结论：");
        builder.append(resolveRegionLabel(regionScope) == null ? "当前知识库已命中相关申报条件。" : "当前已按" + resolveRegionLabel(regionScope) + "口径命中相关申报条件。");
        builder.append("\n关键条件：\n");
        int index = 1;
        for (String point : points) {
            builder.append(index++).append(". ").append(point).append("\n");
            if (index > 4) {
                break;
            }
        }
        builder.append("提示：如需进一步判断是否符合，请再补充学历、工作地、合同年限或纳税情况。\n");
        appendCitationsBlock(builder, collectFallbackCitations(chunks, regionScope));
        return builder.toString().trim();
    }

    private String buildFastPathBenefitAnswer(String regionScope, List<KnowledgeSearchResultVO> chunks) {
        LinkedHashSet<String> points = new LinkedHashSet<>();
        addFallbackPoint(points, "支持金额", chunks, List.of("补助", "补贴", "奖励", "资助", "安家补贴", "万元"));
        addFallbackPoint(points, "拨付方式", chunks, List.of("拨付", "兑现", "发放", "分期", "一次性"));
        addFallbackPoint(points, "管理期", chunks, List.of("管理期", "考核", "退出", "追回"));
        StringBuilder builder = new StringBuilder();
        builder.append("结论：");
        builder.append(resolveRegionLabel(regionScope) == null ? "当前知识库已命中相关支持标准。" : "当前已按" + resolveRegionLabel(regionScope) + "口径命中相关支持标准。");
        builder.append("\n关键金额/规则：\n");
        int index = 1;
        for (String point : points) {
            builder.append(index++).append(". ").append(point).append("\n");
            if (index > 4) {
                break;
            }
        }
        builder.append("提示：不同档次、对象和管理期可能影响最终金额，办理前仍需核对正式政策口径。\n");
        appendCitationsBlock(builder, collectFallbackCitations(chunks, regionScope));
        return builder.toString().trim();
    }

    private void appendCitationsBlock(StringBuilder builder, LinkedHashSet<String> citations) {
        if (builder == null || citations == null || citations.isEmpty()) {
            return;
        }
        builder.append("政策依据：\n");
        citations.forEach(line -> builder.append(line).append("\n"));
    }

    private String buildPromptKnowledgeContext(String question,
                                               KnowledgeCitationContext context,
                                               String sourceScene) {
        if (context == null || context.getChunks().isEmpty()) {
            return "No matched knowledge.\n";
        }
        PolicyQuestionType questionType = detectPolicyQuestionType(question);
        String regionScope = detectPreferredRegionScope(question, questionType);
        List<KnowledgeSearchResultVO> rankedChunks = selectFallbackChunks(question, questionType, regionScope, context.getChunks());
        List<KnowledgeSearchResultVO> selected = new ArrayList<>();
        int limit = resolvePromptContextLimit(questionType, sourceScene);
        for (KnowledgeSearchResultVO item : rankedChunks) {
            if (!isPromptContextChunk(item, questionType)) {
                continue;
            }
            selected.add(item);
            if (selected.size() >= limit) {
                break;
            }
        }
        if (selected.isEmpty()) {
            for (KnowledgeSearchResultVO item : rankedChunks) {
                selected.add(item);
                if (selected.size() >= limit) {
                    break;
                }
            }
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < selected.size(); i++) {
            KnowledgeSearchResultVO item = selected.get(i);
            builder.append("[Source ").append(i + 1).append("] ")
                    .append(resolvePolicyDisplayName(item));
            if (normalizeText(item.getSectionTitle()) != null) {
                builder.append(" / ").append(item.getSectionTitle().trim());
            } else if (normalizeText(item.getChapterTitle()) != null) {
                builder.append(" / ").append(item.getChapterTitle().trim());
            }
            builder.append("\n")
                    .append(valueOrDefault(abbreviate(item.getSnippet(), SOURCE_SCENE_MOBILE_POLICY_CONSULTANT.equals(sourceScene) ? 110 : 180), "No snippet."))
                    .append("\n\n");
        }
        return builder.toString();
    }

    private int resolvePromptContextLimit(PolicyQuestionType questionType, String sourceScene) {
        boolean mobileScene = SOURCE_SCENE_MOBILE_POLICY_CONSULTANT.equals(sourceScene);
        if (questionType == PolicyQuestionType.CONDITION || questionType == PolicyQuestionType.BENEFIT) {
            return mobileScene ? 3 : 4;
        }
        if (questionType == PolicyQuestionType.FAQ || questionType == PolicyQuestionType.BOUNDARY) {
            return mobileScene ? 3 : 4;
        }
        if (questionType == PolicyQuestionType.LIST || questionType == PolicyQuestionType.PROCESS || questionType == PolicyQuestionType.SPECIAL_TOPIC) {
            return mobileScene ? 4 : 5;
        }
        return mobileScene ? 3 : 4;
    }

    private boolean isPromptContextChunk(KnowledgeSearchResultVO item, PolicyQuestionType questionType) {
        if (!isFallbackUsableChunk(item)) {
            return false;
        }
        if (questionType != PolicyQuestionType.BOUNDARY) {
            String text = String.join(" ",
                    valueOrBlank(item.getDocTitle()),
                    valueOrBlank(item.getHeadingPath()),
                    valueOrBlank(item.getChapterTitle()),
                    valueOrBlank(item.getSectionTitle()));
            if (containsAny(text, List.of("导入说明", "检索建议", "使用边界", "路由说明", "导入目录"))
                    || "routing".equalsIgnoreCase(normalizeText(item.getTopicType()))
                    || "route_help".equalsIgnoreCase(normalizeText(item.getTopicType()))) {
                return false;
            }
        }
        return normalizeText(item.getSnippet()) != null;
    }

    private String buildSessionTitle(String question) {
        if (question == null) {
            return DEFAULT_SESSION_TITLE;
        }
        String trimmed = question.trim();
        if (trimmed.isEmpty()) {
            return DEFAULT_SESSION_TITLE;
        }
        return trimmed.length() > 24 ? trimmed.substring(0, 24) : trimmed;
    }

    private String buildFallbackAnswer(Long baseId,
                                       String question,
                                       KnowledgeCitationContext context,
                                       AgentUserPreferenceEntity preference,
                                       String sourceScene,
                                       String fallbackReason) {
        StringBuilder builder = new StringBuilder();
        builder.append("当前为知识库兜底答复。\n");
        if (preference != null && normalizeText(preference.getHabitSummary()) != null) {
            builder.append("已参考你的咨询习惯：").append(preference.getHabitSummary().trim()).append("\n");
        }
        if (normalizeText(fallbackReason) != null) {
            builder.append(fallbackReason.trim()).append("\n\n");
        } else {
            builder.append("\n");
        }

        PolicyRouteIntent routeIntent = policyRouteService.route(question);
        PolicyQuestionType questionType = resolvePolicyQuestionType(routeIntent, question);
        String regionScope = normalizeText(routeIntent.getRegionScope()) != null
                ? routeIntent.getRegionScope()
                : detectPreferredRegionScope(question, questionType);
        if (routeIntent.getRouteSkills().contains("XM_FJ_BOUNDARY_COMPARE")) {
            logCenterService.recordAiChainSuccess(
                    LOG_EVENT_POLICY_ROUTE_COMPARE,
                    null,
                    null,
                    sourceScene,
                    "question=" + valueOrBlank(normalizeText(question)) + ", routeSkills=" + String.join("|", routeIntent.getRouteSkills())
            );
        }
        PolicyCatalogService.CatalogAnswer catalogAnswer = policyCatalogService.buildCatalogAnswer(baseId, routeIntent, sourceScene);
        if (catalogAnswer.hasAnswer() && (questionType == PolicyQuestionType.LIST || questionType == PolicyQuestionType.BOUNDARY)) {
            if (routeIntent.getRouteSkills().contains("XM_FJ_BOUNDARY_COMPARE")) {
                logCenterService.recordAiChainSuccess(
                        LOG_EVENT_POLICY_BOUNDARY_CATALOG,
                        null,
                        null,
                        sourceScene,
                        "answerSource=FALLBACK_CATALOG_COMPARE"
                );
            }
            builder.append(catalogAnswer.answer());
            return builder.toString();
        }
        if (context == null || context.getChunks().isEmpty()) {
            builder.append("暂未在知识库中检索到与该问题直接对应的政策依据。\n")
                    .append("建议补充以下信息后再提问：\n")
                    .append("1. 所在地区，例如厦门市、福建省。\n")
                    .append("2. 人才层次、企业类型或申报主体。\n")
                    .append("3. 具体主题，例如认定、补贴、住房、创业扶持。\n");
            if (normalizeText(question) != null) {
                builder.append("\n本次问题：").append(question.trim()).append("\n");
            }
            return builder.toString();
        }
        builder.append(buildStructuredFallbackAnswer(baseId, question, questionType, regionScope, sourceScene, context.getChunks()));
        LinkedHashSet<String> citations = collectFallbackCitations(context.getChunks(), regionScope);
        if (!citations.isEmpty()) {
            builder.append("\n\n政策依据：\n");
            citations.forEach(line -> builder.append(line).append("\n"));
        }
        return builder.toString();
    }


    private String buildStructuredFallbackAnswer(Long baseId,
                                                 String question,
                                                 PolicyQuestionType questionType,
                                                 String regionScope,
                                                 String sourceScene,
                                                 List<KnowledgeSearchResultVO> chunks) {
        PolicyRouteIntent routeIntent = policyRouteService.route(question);
        if (routeIntent.getRouteSkills().contains("XM_FJ_BOUNDARY_COMPARE")) {
            logCenterService.recordAiChainSuccess(
                    LOG_EVENT_POLICY_ROUTE_COMPARE,
                    null,
                    null,
                    sourceScene,
                    "question=" + valueOrBlank(normalizeText(question)) + ", routeSkills=" + String.join("|", routeIntent.getRouteSkills())
            );
        }
        PolicyCatalogService.CatalogAnswer catalogAnswer = policyCatalogService.buildCatalogAnswer(baseId, routeIntent, sourceScene);
        if (catalogAnswer.hasAnswer() && (questionType == PolicyQuestionType.LIST || questionType == PolicyQuestionType.BOUNDARY)) {
            if (routeIntent.getRouteSkills().contains("XM_FJ_BOUNDARY_COMPARE")) {
                logCenterService.recordAiChainSuccess(
                        LOG_EVENT_POLICY_BOUNDARY_CATALOG,
                        null,
                        null,
                        sourceScene,
                        "answerSource=STRUCTURED_FALLBACK_CATALOG_COMPARE"
                );
            }
            return catalogAnswer.answer();
        }
        List<KnowledgeSearchResultVO> rankedChunks = selectFallbackChunks(question, questionType, regionScope, chunks);
if (rankedChunks.isEmpty()) {
    return "当前知识库已命中结果不足，建议补充地区、政策名称、申报对象或补贴主题后再继续提问。";
}

if (isDoubleHundredApplyFlowQuestion(question, questionType)
        && hasDoubleHundredApplyFlowEvidence(rankedChunks)) {
    return buildDoubleHundredApplyFlowAnswer();
}

return switch (questionType) {
    case LIST -> buildListFallbackAnswer(question, regionScope, sourceScene, rankedChunks);
    case PROCESS -> buildProcessFallbackAnswer(question, regionScope, rankedChunks);
    case CONDITION -> buildConditionFallbackAnswer(question, regionScope, sourceScene, rankedChunks);
    case BENEFIT -> buildBenefitFallbackAnswer(question, regionScope, sourceScene, rankedChunks);
    case SPECIAL_TOPIC, BOUNDARY -> buildSpecialTopicFallbackAnswer(question, questionType, regionScope, sourceScene, rankedChunks);
    default -> buildGeneralFallbackAnswer(question, regionScope, sourceScene, rankedChunks);
};
    }

    private boolean isDoubleHundredApplyFlowQuestion(String question, PolicyQuestionType questionType) {
    String q = question == null ? "" : question;
    if (questionType != PolicyQuestionType.PROCESS) {
        return false;
    }
    boolean hasPolicy = q.contains("双百计划") || q.contains("创新团队") || q.contains("创业人才");
    boolean hasFlowWord = q.contains("申请")
            || q.contains("申报")
            || q.contains("流程")
            || q.contains("步骤")
            || q.contains("怎么")
            || q.contains("如何");
    return hasPolicy && hasFlowWord;
}

private boolean hasDoubleHundredApplyFlowEvidence(List<KnowledgeSearchResultVO> chunks) {
    if (chunks == null || chunks.isEmpty()) {
        return false;
    }
    for (KnowledgeSearchResultVO item : chunks) {
        String text = buildChunkSearchText(item);
        boolean hasPolicy = containsAny(text, List.of(
                "双百计划",
                "创新团队",
                "创业人才",
                "厦门市引进高层次创新创业人才“双百计划”实施意见",
                "夏委组〔2024〕46号",
                "夏委组 46号",
                "高层次创新创业人才"
        ));
        boolean hasFlow = containsAny(text, List.of(
                "遴选与申请流程",
                "申请流程",
                "申报流程",
                "五步流程",
                "组织申报",
                "资格核查",
                "部门联审",
                "综合评审",
                "研究确认",
                "专项办",
                "材料评审",
                "现场答辩",
                "复核考察",
                "建议人选",
                "审定"
        ));
        if (hasPolicy && hasFlow) {
            return true;
        }
    }
    return false;
}

private String joinProcessTexts(String... values) {
    StringBuilder sb = new StringBuilder();
    if (values != null) {
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                if (sb.length() > 0) {
                    sb.append(' ');
                }
                sb.append(v);
            }
        }
    }
    return sb.toString();
}

private String buildDoubleHundredApplyFlowAnswer() {
    return """
**结论**
“双百计划”申请前，需先区分申报类别。根据当前知识库已命中的内容，创新团队、创业人才一般按五步流程推进。

**申请流程**
1. 组织申报：专项办发布申报公告，申报人（团队）按要求通过遴选系统提交材料。
2. 资格核查：各区（开发区）对申报人和项目开展书面审核与现场核查，重点查看基本资格、在岗履职、平台建设、项目推进等情况。
3. 部门联审：市人社局、市科技局按所属产业领域进行分组联审，提出审核意见。
4. 综合评审：采取材料评审、现场答辩、复核考察等方式，形成初步人选名单。
5. 研究确认：专项办根据评审与考察情况提出建议人选，报专项小组审定后确定正式入选名单。

**补充说明**
如果继续确认具体材料清单、申报入口、年度时间节点，仍需以当年度申报公告和遴选系统要求为准。
""";
}

private boolean shouldForceProcessEvidenceMissingPrompt(String question, KnowledgeCitationContext context) {
    if (!hasStructuredSpecificPolicyHitWithoutProcessEvidence(question, context)) {
        return false;
    }
    return context == null
            || context.getChunks() == null
            || !hasDoubleHundredApplyFlowEvidence(context.getChunks());
}

    private String buildListFallbackAnswer(String question,
                                           String regionScope,
                                           String sourceScene,
                                           List<KnowledgeSearchResultVO> chunks) {
        List<String> formalPolicyNames = extractFormalPolicyNames(question);
        List<String> specialTopicTerms = extractMatchedSpecialTopicTerms(question);
        int maxItems = SOURCE_SCENE_MOBILE_POLICY_CONSULTANT.equals(sourceScene) ? 6 : 8;
        List<KnowledgeSearchResultVO> candidates = new ArrayList<>();
        for (KnowledgeSearchResultVO item : chunks) {
            if (!isFallbackUsableChunk(item)) {
                continue;
            }
            if ("guide".equalsIgnoreCase(normalizeText(item.getDocType()))
                    || "routing".equalsIgnoreCase(normalizeText(item.getTopicType()))
                    || "route_help".equalsIgnoreCase(normalizeText(item.getTopicType()))) {
                continue;
            }
            String topicType = normalizeText(item.getTopicType());
            if ("list".equalsIgnoreCase(topicType)
                    || "special_topic".equalsIgnoreCase(topicType)
                    || "faq".equalsIgnoreCase(topicType)
                    || "main-summary".equalsIgnoreCase(topicType)
                    || matchesPolicyIntent(item, formalPolicyNames, specialTopicTerms)) {
                candidates.add(item);
            }
        }
        if (candidates.isEmpty()) {
            candidates = chunks;
        }
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        List<KnowledgeSearchResultVO> selected = new ArrayList<>();
        for (KnowledgeSearchResultVO item : candidates) {
            String key = normalizeText(resolvePolicyDisplayName(item));
            if (key == null || seen.contains(key)) {
                continue;
            }
            seen.add(key);
            selected.add(item);
            if (selected.size() >= maxItems) {
                break;
            }
        }
        String regionLabel = resolveRegionLabel(regionScope);
        StringBuilder builder = new StringBuilder();
        builder.append("根据当前知识库命中，");
        if (regionLabel != null) {
            builder.append(regionLabel);
        } else {
            builder.append("当前咨询范围内");
        }
        builder.append("主要人才政策/项目包括：\n");
        for (int i = 0; i < selected.size(); i++) {
            KnowledgeSearchResultVO item = selected.get(i);
            builder.append(i + 1)
                    .append(". ")
                    .append(resolvePolicyDisplayName(item))
                    .append("：")
                    .append(buildListItemSummary(item))
                    .append("\n");
        }
        builder.append("以下为当前知识库已命中的主要政策/项目，不代表完整官方清单");
        return builder.toString();
    }

    private String buildProcessFallbackAnswer(String question,
                                          String regionScope,
                                          List<KnowledgeSearchResultVO> chunks) {

    if (isDoubleHundredApplyFlowQuestion(question, PolicyQuestionType.PROCESS)) {
        List<KnowledgeSearchResultVO> doubleHundredChunks = new ArrayList<>();
        for (KnowledgeSearchResultVO item : chunks) {
            if (isDoubleHundredProcessChunk(item)) {
                doubleHundredChunks.add(item);
            }
        }

        if (hasDoubleHundredApplyFlowEvidence(!doubleHundredChunks.isEmpty() ? doubleHundredChunks : chunks)) {
            return buildDoubleHundredApplyFlowAnswer();
        }

        return "已确认命中的政策/项目为：厦门市引进高层次创新创业人才“双百计划”实施意见。\n"
                + PROCESS_EVIDENCE_MISSING_MESSAGE;
    }

    List<String> policyTerms = extractStructuredSpecificPolicyTerms(question);
    List<KnowledgeSearchResultVO> policyMatched = new ArrayList<>();
    List<KnowledgeSearchResultVO> processMatched = new ArrayList<>();

    for (KnowledgeSearchResultVO item : chunks) {
        if (containsAny(buildChunkSearchText(item), policyTerms)) {
            policyMatched.add(item);
        }
        if (isProcessChunk(item)) {
            processMatched.add(item);
        }
    }

    if (!policyMatched.isEmpty() && !processMatched.isEmpty()) {
        List<KnowledgeSearchResultVO> filteredProcess = new ArrayList<>();
        for (KnowledgeSearchResultVO item : processMatched) {
            if (containsAny(buildChunkSearchText(item), policyTerms)
                    || normalizeText(resolvePolicyDisplayName(item)).equals(normalizeText(resolvePolicyDisplayName(policyMatched.get(0))))) {
                filteredProcess.add(item);
            }
        }
        if (!filteredProcess.isEmpty()) {
            processMatched = filteredProcess;
        }
    }

    String primaryPolicy = resolvePrimaryPolicyName(!policyMatched.isEmpty() ? policyMatched : chunks);
    List<String> steps = extractFallbackProcessSteps(processMatched);

    if (steps.size() == 1 && looksLikeFundingProcessOnly(steps.get(0)) && !policyMatched.isEmpty()) {
        steps = List.of();
    }

    StringBuilder builder = new StringBuilder();
    if (primaryPolicy != null) {
        builder.append("已确认命中的政策/项目为：").append(primaryPolicy).append("。\n");
    } else if (resolveRegionLabel(regionScope) != null) {
        builder.append("当前已优先按").append(resolveRegionLabel(regionScope)).append("口径整理申报流程依据。\n");
    }

    if (!steps.isEmpty()) {
        builder.append("根据当前知识库已命中的流程依据，可整理为以下步骤：\n");
        for (int i = 0; i < steps.size(); i++) {
            builder.append(i + 1).append(". ").append(steps.get(i)).append("\n");
        }
        if (!policyMatched.isEmpty() && steps.size() < 2) {
            builder.append(PROCESS_EVIDENCE_MISSING_MESSAGE);
        }
    } else if (!policyMatched.isEmpty()) {
        builder.append(PROCESS_EVIDENCE_MISSING_MESSAGE);
    } else {
        builder.append("当前知识库已命中部分申报依据，建议进一步补充具体政策名称、申报主体或年份，以便继续缩小流程范围。");
    }
    return builder.toString().trim();
}
private boolean looksLikeFundingProcessOnly(String step) {
    String normalized = normalizeText(step);
    if (normalized == null) {
        return false;
    }
    boolean hasFundingWord = containsAny(normalized, List.of("拨付", "兑现", "发放"));
    boolean hasApplyWord = containsAny(normalized, List.of("组织申报", "资格核查", "部门联审", "综合评审", "研究确认", "受理", "审核"));
    return hasFundingWord && !hasApplyWord;
}

    private String buildConditionFallbackAnswer(String question,
                                                String regionScope,
                                                String sourceScene,
                                                List<KnowledgeSearchResultVO> chunks) {
        String primaryPolicy = resolvePrimaryPolicyName(chunks);
        int maxPoints = SOURCE_SCENE_MOBILE_POLICY_CONSULTANT.equals(sourceScene) ? 3 : 5;
        LinkedHashSet<String> points = new LinkedHashSet<>();
        addFallbackPoint(points, "适用对象", chunks, List.of("适用对象", "对象", "支持对象", "申报对象"));
        addFallbackPoint(points, "基本条件", chunks, List.of("基本条件", "申报条件", "资格", "要求", "认定条件"));
        addFallbackPoint(points, "学历年龄", chunks, List.of("学历", "学位", "年龄"));
        addFallbackPoint(points, "合同缴税", chunks, List.of("劳动合同", "合同", "缴税", "纳税", "社保"));
        addFallbackPoint(points, "工作地服务期", chunks, List.of("工作地", "在厦", "在闽", "服务期", "任职"));
        if (points.isEmpty()) {
            points.addAll(extractFallbackClauses(chunks, List.of("条件", "对象", "资格", "要求"), maxPoints));
        }
        StringBuilder builder = new StringBuilder();
        if (primaryPolicy != null) {
            builder.append("当前优先命中的政策/专题为：").append(primaryPolicy).append("。\n");
        } else if (resolveRegionLabel(regionScope) != null) {
            builder.append("当前已优先按").append(resolveRegionLabel(regionScope)).append("口径整理申报条件。\n");
        }
        builder.append("根据当前知识库已命中的条件依据，可重点关注：\n");
        int index = 1;
        for (String point : points) {
            builder.append(index++).append(". ").append(point).append("\n");
            if (index > maxPoints) {
                break;
            }
        }
        return builder.toString().trim();
    }

    private String buildBenefitFallbackAnswer(String question,
                                              String regionScope,
                                              String sourceScene,
                                              List<KnowledgeSearchResultVO> chunks) {
        String primaryPolicy = resolvePrimaryPolicyName(chunks);
        int maxPoints = SOURCE_SCENE_MOBILE_POLICY_CONSULTANT.equals(sourceScene) ? 3 : 5;
        LinkedHashSet<String> points = new LinkedHashSet<>();
        addFallbackPoint(points, "支持金额", chunks, List.of("补助", "补贴", "奖励", "资助", "安家补贴", "万元"));
        addFallbackPoint(points, "拨付方式", chunks, List.of("拨付", "兑现", "发放", "分期", "一次性"));
        addFallbackPoint(points, "支持规则", chunks, List.of("支持政策", "资金扶持", "标准", "档次"));
        addFallbackPoint(points, "管理期", chunks, List.of("管理期", "考核", "退出", "追回"));
        if (points.isEmpty()) {
            points.addAll(extractFallbackClauses(chunks, List.of("补助", "补贴", "奖励", "支持", "万元"), maxPoints));
        }
        StringBuilder builder = new StringBuilder();
        if (primaryPolicy != null) {
            builder.append("当前优先命中的政策/专题为：").append(primaryPolicy).append("。\n");
        } else if (resolveRegionLabel(regionScope) != null) {
            builder.append("当前已优先按").append(resolveRegionLabel(regionScope)).append("口径整理待遇标准。\n");
        }
        builder.append("根据当前知识库已命中的待遇依据，可重点把握：\n");
        int index = 1;
        for (String point : points) {
            builder.append(index++).append(". ").append(point).append("\n");
            if (index > maxPoints) {
                break;
            }
        }
        return builder.toString().trim();
    }

    private String buildSpecialTopicFallbackAnswer(String question,
                                                   PolicyQuestionType questionType,
                                                   String regionScope,
                                                   String sourceScene,
                                                   List<KnowledgeSearchResultVO> chunks) {
        String primaryPolicy = resolvePrimaryPolicyName(chunks);
        int maxPoints = SOURCE_SCENE_MOBILE_POLICY_CONSULTANT.equals(sourceScene) ? 3 : 5;
        LinkedHashSet<String> points = new LinkedHashSet<>();
        points.addAll(extractFallbackClauses(chunks, List.of("对象", "条件", "流程", "支持", "补贴", "奖励"), maxPoints));
        StringBuilder builder = new StringBuilder();
        if (primaryPolicy != null) {
            builder.append("当前优先命中的正式政策/专题为：").append(primaryPolicy).append("。\n");
        } else {
            builder.append("当前已命中相关专题政策依据。\n");
        }
        if (!points.isEmpty()) {
            builder.append("结合当前知识库依据，可先把握以下要点：\n");
            int index = 1;
            for (String point : points) {
                builder.append(index++).append(". ").append(point).append("\n");
                if (index > maxPoints) {
                    break;
                }
            }
        }
        String boundaryNote = buildFallbackBoundaryNote(question, questionType, regionScope, chunks);
        if (normalizeText(boundaryNote) != null) {
            builder.append(boundaryNote);
        }
        return builder.toString().trim();
    }

private String buildConditionReverseFallbackAnswer(String question,
                                                   String regionScope,
                                                   List<KnowledgeSearchResultVO> chunks) {
    List<KnowledgeSearchResultVO> usable = chunks == null ? List.of() : selectFallbackChunks(
            question,
            PolicyQuestionType.CONDITION,
            regionScope,
            chunks
    );

    StringBuilder builder = new StringBuilder();
    builder.append("结论：");
    builder.append("这类问题不能简单理解为“是否有一个叫本科生政策的项目”。");
    builder.append("本科/学士、证书、职称、专业方向通常是部分人才项目的准入条件之一，需要和行业、单位、证书、职称、成果等条件一起判断。\n");

    if (usable.isEmpty()) {
        builder.append("当前知识库暂未检索到足够可直接匹配的条件条款，建议继续补充或优化申报条件类专题切片。\n");
        return builder.toString().trim();
    }

    builder.append("当前知识库可先关注以下方向：\n");

    LinkedHashSet<String> seen = new LinkedHashSet<>();
    int index = 1;
    for (KnowledgeSearchResultVO item : usable) {
        String policyName = resolvePolicyDisplayName(item);
        if (normalizeText(policyName) == null || seen.contains(policyName)) {
            continue;
        }
        seen.add(policyName);

        builder.append(index++).append(". ").append(policyName).append("：");
        String text = buildChunkSearchText(item);

        if (containsAny(text, List.of("本科及以上", "学士", "学士学位"))) {
            builder.append("命中“本科/学士”相关准入条件。");
        } else if (containsAny(text, List.of("CFA", "FRM", "CPA", "精算", "法律职业"))) {
            builder.append("命中金融资格证书或专业资格相关条件。");
        } else if (containsAny(text, List.of("高级职称", "职称"))) {
            builder.append("命中职称或专业技术资格相关条件。");
        } else if (containsAny(text, List.of("软件", "电子信息", "人工智能"))) {
            builder.append("命中软件信息技术或重点产业方向。");
        } else {
            builder.append("命中相关申报条件或适用对象。");
        }

        builder.append("仍需继续核对完整申报条件、单位推荐要求、证书/职称/成果等附加要求。\n");

        if (index > 5) {
            break;
        }
    }

    if (seen.isEmpty()) {
        builder.append("当前命中内容仍偏弱，建议优先优化“申报条件、适用对象、申报方式”类切片。\n");
    }

    builder.append("提示：本科/学士一般不是充分条件，通常还要叠加行业方向、所在单位、资格证书、职称或项目成果等要求。");
    return builder.toString().trim();
}
    private String buildGeneralFallbackAnswer(String question,
                                              String regionScope,
                                              String sourceScene,
                                              List<KnowledgeSearchResultVO> chunks) {
                if (isConditionReverseQuestion(question)) {
            return buildConditionReverseFallbackAnswer(question, regionScope, chunks);
        }
        if (detectPolicyQuestionType(question) == PolicyQuestionType.PROCESS) {
            return buildProcessFallbackAnswer(question, regionScope, chunks);
        }
        if (detectPolicyQuestionType(question) == PolicyQuestionType.LIST) {
            return buildListFallbackAnswer(question, regionScope, sourceScene, chunks);
        }
        String primaryPolicy = resolvePrimaryPolicyName(chunks);
        int maxPoints = SOURCE_SCENE_MOBILE_POLICY_CONSULTANT.equals(sourceScene) ? 3 : 4;
        List<String> points = extractFallbackClauses(chunks, List.of("对象", "条件", "流程", "支持", "补贴", "奖励"), maxPoints);
        StringBuilder builder = new StringBuilder();
        if (primaryPolicy != null) {
            builder.append("当前优先命中的政策/专题为：").append(primaryPolicy).append("。\n");
        }
        builder.append("根据当前知识库已命中的依据，可先把握以下内容：\n");
        for (int i = 0; i < points.size(); i++) {
            builder.append(i + 1).append(". ").append(points.get(i)).append("\n");
        }
        String boundaryNote = buildFallbackBoundaryNote(question, PolicyQuestionType.GENERAL, regionScope, chunks);
        if (normalizeText(boundaryNote) != null) {
            builder.append(boundaryNote);
        }
        return builder.toString().trim();
    }

    private List<KnowledgeSearchResultVO> selectFallbackChunks(String question,
                                                               PolicyQuestionType questionType,
                                                               String regionScope,
                                                               List<KnowledgeSearchResultVO> chunks) {
        List<String> exactTerms = extractStructuredSpecificPolicyTerms(question);
        List<KnowledgeSearchResultVO> usable = new ArrayList<>();
        for (KnowledgeSearchResultVO item : chunks) {
            if (isFallbackUsableChunk(item)) {
                usable.add(item);
            }
        }
        if (usable.isEmpty()) {
            usable = new ArrayList<>(chunks);
        }
        if (questionType != PolicyQuestionType.BOUNDARY) {
            List<KnowledgeSearchResultVO> regionMatched = new ArrayList<>();
            for (KnowledgeSearchResultVO item : usable) {
                if (matchesRegionScope(item, regionScope)) {
                    regionMatched.add(item);
                }
            }
            if (!regionMatched.isEmpty()) {
                usable = regionMatched;
            }
        }
        usable.sort(Comparator
                .comparingInt((KnowledgeSearchResultVO item) -> scoreFallbackChunk(item, questionType, regionScope, exactTerms)).reversed()
                .thenComparing(item -> item.getChunkNo() == null ? Integer.MAX_VALUE : item.getChunkNo()));
        return usable;
    }

    private int scoreFallbackChunk(KnowledgeSearchResultVO item,
                                   PolicyQuestionType questionType,
                                   String regionScope,
                                   List<String> exactTerms) {
        int score = 0;
        if (matchesRegionScope(item, regionScope)) {
            score += 300;
        } else if (normalizeText(regionScope) != null && normalizeText(item.getRegionScope()) != null) {
            score -= 50;
        }
        if (Boolean.TRUE.equals(item.getSearchable())) {
            score += 30;
        }
        if (containsAny(buildChunkSearchText(item), exactTerms)) {
            score += 220;
        }
        String docType = normalizeText(item.getDocType());
        if ("topic".equalsIgnoreCase(docType)) {
            score += 120;
        } else if ("main".equalsIgnoreCase(docType)) {
            score += 80;
        } else if ("guide".equalsIgnoreCase(docType)) {
            score -= 200;
        }
        String topicType = normalizeText(item.getTopicType());
        if (questionType == PolicyQuestionType.LIST) {
            if ("list".equalsIgnoreCase(topicType) || "special_topic".equalsIgnoreCase(topicType) || "faq".equalsIgnoreCase(topicType)) {
                score += 120;
            }
        } else if (questionType == PolicyQuestionType.PROCESS) {
            if ("process".equalsIgnoreCase(topicType) || isProcessChunk(item)) {
                score += 140;
            }
        } else if (questionType == PolicyQuestionType.CONDITION) {
            if ("condition".equalsIgnoreCase(topicType) || isConditionChunk(item)) {
                score += 140;
            }
        } else if (questionType == PolicyQuestionType.BENEFIT) {
            if ("benefit".equalsIgnoreCase(topicType) || isBenefitChunk(item)) {
                score += 140;
            }
        } else if (questionType == PolicyQuestionType.SPECIAL_TOPIC || questionType == PolicyQuestionType.BOUNDARY) {
            if ("special_topic".equalsIgnoreCase(topicType) || "faq".equalsIgnoreCase(topicType)) {
                score += 120;
            }
        }
        return score;
    }

    private boolean isFallbackUsableChunk(KnowledgeSearchResultVO item) {
        if (item == null) {
            return false;
        }
        if (Boolean.FALSE.equals(item.getSearchable())) {
            return false;
        }
        return !"guide".equalsIgnoreCase(normalizeText(item.getDocType()));
    }

    private boolean matchesRegionScope(KnowledgeSearchResultVO item, String regionScope) {
        String expected = normalizeText(regionScope);
        if (expected == null || item == null) {
            return false;
        }
        return expected.equalsIgnoreCase(normalizeText(item.getRegionScope()));
    }

    private String resolvePolicyDisplayName(KnowledgeSearchResultVO item) {
        if (item == null) {
            return "未命名政策";
        }
        String policyName = normalizeText(item.getPolicyName());
        if (policyName != null) {
            return item.getPolicyName().trim();
        }
        String docTitle = normalizeText(item.getDocTitle());
        if (docTitle != null) {
            return item.getDocTitle().trim();
        }
        return "未命名政策";
    }

    private String resolvePrimaryPolicyName(List<KnowledgeSearchResultVO> chunks) {
        for (KnowledgeSearchResultVO item : chunks) {
            String policyName = normalizeText(resolvePolicyDisplayName(item));
            if (policyName != null) {
                return resolvePolicyDisplayName(item);
            }
        }
        return null;
    }

    private String buildListItemSummary(KnowledgeSearchResultVO item) {
        String policyName = resolvePolicyDisplayName(item);
        if (policyName.contains("双百计划")) {
            return "聚焦高层次创新创业人才引进、评审和支持安排。";
        }
        if (policyName.contains("特聘岗位")) {
            return "聚焦高层次人才特聘岗位设置、管理和支持规则。";
        }
        if (policyName.contains("专项资金")) {
            return "聚焦高层次人才专项资金使用、拨付和管理要求。";
        }
        if (policyName.contains("住房")) {
            return "聚焦高层次人才住房补贴条件、标准和发放规则。";
        }
        if (policyName.contains("博士后")) {
            return "聚焦博士后招收、资助、平台建设和培养支持。";
        }
        if (policyName.contains("人工智能")) {
            return "聚焦人工智能领域人才引进、培养和专项支持措施。";
        }
        if (policyName.contains("百人计划")) {
            return "聚焦省级高层次创业创新人才引进申报和支持安排。";
        }
        if (policyName.contains("产业人才项目实施办法")) {
            return "聚焦对应重点产业人才项目的申报、评审和资金支持。";
        }
        if (policyName.contains("关于更加精准有效集聚人才加快推进高质量发展的意见")) {
            return "属于人才工作总纲，统筹人才引进、培养、支持和服务保障。";
        }
        String topicType = normalizeText(item.getTopicType());
        if ("process".equalsIgnoreCase(topicType)) {
            return "已命中其申报流程、评审或兑现相关依据。";
        }
        if ("condition".equalsIgnoreCase(topicType)) {
            return "已命中其适用对象和申报条件相关依据。";
        }
        if ("benefit".equalsIgnoreCase(topicType)) {
            return "已命中其补助标准、奖励或资金扶持相关依据。";
        }
        String clause = fallbackFirstClause(item.getSnippet());
        if (normalizeText(clause) != null) {
            return abbreviate(clause, 36) + "。";
        }
        return "属于当前知识库已命中的重点政策/项目。";
    }

    /*
    private String buildListItemSummary(String policyName, KnowledgeSearchResultVO item) {
        String canonicalName = canonicalizePolicyListName(policyName);
        if (normalizeText(canonicalName) == null) {
            canonicalName = policyName;
        }
        if (containsKeyword(canonicalName, "鍙岀櫨璁″垝")) {
            return "鑱氱劍楂樺眰娆″垱鏂板垱涓氫汉鎵嶅紩杩涖€佽瘎瀹″拰鏀寔瀹夋帓銆?;
        }
        if (containsKeyword(canonicalName, "鐗硅仒宀椾綅")) {
            return "鑱氱劍楂樺眰娆′汉鎵嶇壒鑱樺矖浣嶈缃€佸紩杩涖€佺鐞嗗拰鏀寔瑙勫垯銆?;
        }
        if (containsKeyword(canonicalName, "涓撻」璧勯噾")) {
            return "鑱氱劍楂樺眰娆′汉鎵嶄笓椤硅祫閲戠殑鎷ㄤ粯銆佷娇鐢ㄥ拰绠＄悊瑙勫垯銆?;
        }
        if (containsKeyword(canonicalName, "鍏充簬鏇村姞绮惧噯鏈夋晥闆嗚仛浜烘墠鍔犲揩鎺ㄨ繘楂樿川閲忓彂灞曠殑鎰忚")) {
            return "灞炰簬浜烘墠宸ヤ綔鎬荤翰锛岀粺绛逛汉鎵嶅紩杩涖€佸煿鍏汇€佹敮鎸佸拰鏈嶅姟淇濋殰銆?;
        }
        if (containsKeyword(canonicalName, "浜т笟浜烘墠椤圭洰瀹炴柦鍔炴硶")) {
            return "鑱氱劍瀵瑰簲閲嶇偣浜т笟浜烘墠椤圭洰鐨勭敵鎶ャ€佽瘎瀹″拰璧勯噾鏀寔銆?;
        }
        if (containsAny(canonicalName, List.of(
                "鏁欒偛浜烘墠椤圭洰瀹炴柦鍔炴硶",
                "鍗敓鍋ュ悍浜烘墠椤圭洰瀹炴柦鍔炴硶",
                "绀句細宸ヤ綔浜烘墠椤圭洰瀹炴柦鍔炴硶",
                "鍙版咕鐗硅仒涓撳锛堜笓鎵嶏級椤圭洰瀹炴柦鍔炴硶"
        ))) {
            return "鑱氱劍鍏叡棰嗗煙浜烘墠椤圭洰鐨勫紩杩涖€佹敮鎸佸拰绠＄悊瀹夋帓銆?;
        }
        String itemPolicyName = canonicalizePolicyListName(item == null ? null : item.getPolicyName());
        if (normalizeText(itemPolicyName) != null && normalizeText(itemPolicyName).equalsIgnoreCase(normalizeText(canonicalName))) {
            String clause = fallbackFirstClause(item.getSnippet());
            if (normalizeText(clause) != null && !isInvalidPolicyListItem(clause)) {
                return abbreviate(clause, 36) + "銆?;
            }
        }
        return buildListItemSummary(item);
    }
    */

    private String buildListItemSummary(String policyName, KnowledgeSearchResultVO item) {
        String canonicalName = canonicalizePolicyListName(policyName);
        if (normalizeText(canonicalName) == null) {
            canonicalName = policyName;
        }
        if (containsKeyword(canonicalName, "双百计划")) {
            return "聚焦高层次创新创业人才引进、评审和支持安排。";
        }
        if (containsKeyword(canonicalName, "特聘岗位")) {
            return "聚焦高层次人才特聘岗位设置、引进、管理和支持规则。";
        }
        if (containsKeyword(canonicalName, "专项资金")) {
            return "聚焦高层次人才专项资金的拨付、使用和管理规则。";
        }
        if (containsKeyword(canonicalName, "关于更加精准有效集聚人才加快推进高质量发展的意见")) {
            return "属于人才工作总纲，统筹人才引进、培养、支持和服务保障。";
        }
        if (containsAny(canonicalName, List.of("住房", "住房补贴", "安居", "租房", "购房"))) {
            return buildHousingListItemSummary(canonicalName, item);
        }
        if (containsKeyword(canonicalName, "产业人才项目实施办法")) {
            return "聚焦对应重点产业人才项目的申报、评审和资金支持。";
        }
        if (containsAny(canonicalName, List.of(
                "教育人才项目实施办法",
                "卫生健康人才项目实施办法",
                "社会工作人才项目实施办法",
                "台湾特聘专家（专才）项目实施办法"
        ))) {
            return "聚焦对应公共领域人才项目的引进、支持和管理安排。";
        }
        String itemPolicyName = canonicalizePolicyListName(item == null ? null : item.getPolicyName());
        if (normalizeText(itemPolicyName) != null && normalizeText(itemPolicyName).equalsIgnoreCase(normalizeText(canonicalName))) {
            String clause = fallbackFirstClause(item.getSnippet());
            if (normalizeText(clause) != null && !isInvalidPolicyListItem(clause)) {
                return abbreviate(clause, 36) + "。";
            }
        }
        return buildListItemSummary(item);
    }

    private String buildHousingListItemSummary(String policyName, KnowledgeSearchResultVO item) {
        String canonicalName = canonicalizePolicyListName(policyName);
        if (normalizeText(canonicalName) == null) {
            canonicalName = policyName;
        }
        if (containsAny(canonicalName, List.of("住房补贴", "住房"))) {
            return "聚焦高层次人才住房补贴的申请对象、标准和发放安排。";
        }
        if (containsKeyword(canonicalName, "安居")) {
            return "聚焦安居支持对象、申请条件和保障方式。";
        }
        if (containsKeyword(canonicalName, "租房")) {
            return "聚焦租房支持对象、补贴标准和兑现安排。";
        }
        if (containsKeyword(canonicalName, "购房")) {
            return "聚焦购房支持对象、补贴标准和兑现安排。";
        }
        return "聚焦住房保障相关支持对象、条件和兑现安排。";
    }

    private List<String> extractFallbackProcessSteps(List<KnowledgeSearchResultVO> processChunks) {
        List<String> orderedSteps = List.of(
        "组织申报",
        "资格核查",
        "部门联审",
        "综合评审",
        "公示确定",
        "研究确认",
        "受理",
        "审核",
        "拨付",
        "兑现"
);
        LinkedHashSet<String> steps = new LinkedHashSet<>();
        for (String step : orderedSteps) {
            for (KnowledgeSearchResultVO item : processChunks) {
                if (containsKeyword(buildChunkSearchText(item), step)) {
                    steps.add(step + "：" + extractProcessStepDescription(item, step));
                    break;
                }
            }
        }
        if (steps.isEmpty()) {
            for (KnowledgeSearchResultVO item : processChunks) {
                String heading = normalizeText(item.getSectionTitle());
                if (heading == null) {
                    heading = normalizeText(item.getChapterTitle());
                }
                if (heading != null) {
                    steps.add(heading + "：" + extractProcessStepDescription(item, heading));
                }
                if (steps.size() >= 4) {
                    break;
                }
            }
        }
        return new ArrayList<>(steps);
    }

    private String extractProcessStepDescription(KnowledgeSearchResultVO item, String stepKeyword) {
        String clause = extractClauseByKeywords(item.getSnippet(), List.of(stepKeyword));
        if (normalizeText(clause) == null) {
            clause = fallbackFirstClause(item.getSnippet());
        }
        if (normalizeText(clause) != null) {
            return abbreviate(clause, 56);
        }
        if (normalizeText(item.getSectionTitle()) != null) {
            return "当前已命中" + item.getSectionTitle().trim() + "相关依据";
        }
        return "当前知识库已命中该环节的相关政策依据";
    }

    private void addFallbackPoint(LinkedHashSet<String> points,
                                  String label,
                                  List<KnowledgeSearchResultVO> chunks,
                                  List<String> keywords) {
        String clause = null;
        for (KnowledgeSearchResultVO item : chunks) {
            clause = extractClauseByKeywords(item.getSnippet(), keywords);
            if (normalizeText(clause) != null) {
                break;
            }
        }
        if (normalizeText(clause) != null) {
            points.add(label + "：" + abbreviate(clause, 72));
        }
    }

    private List<String> extractFallbackClauses(List<KnowledgeSearchResultVO> chunks, List<String> keywords, int maxCount) {
        LinkedHashSet<String> clauses = new LinkedHashSet<>();
        for (KnowledgeSearchResultVO item : chunks) {
            String clause = extractClauseByKeywords(item.getSnippet(), keywords);
            if (normalizeText(clause) == null) {
                clause = fallbackFirstClause(item.getSnippet());
            }
            if (normalizeText(clause) != null) {
                clauses.add(abbreviate(clause, 80));
            }
            if (clauses.size() >= maxCount) {
                break;
            }
        }
        return new ArrayList<>(clauses);
    }

    private String extractClauseByKeywords(String text, List<String> keywords) {
        if (normalizeText(text) == null) {
            return null;
        }
        for (String clause : text.replace('\r', '\n').split("[。；;\\n]")) {
            String normalizedClause = normalizeText(clause);
            if (normalizedClause == null) {
                continue;
            }
            for (String keyword : keywords) {
                if (containsKeyword(normalizedClause, keyword)) {
                    return normalizedClause;
                }
            }
        }
        return null;
    }

    private String fallbackFirstClause(String text) {
        if (normalizeText(text) == null) {
            return null;
        }
        for (String clause : text.replace('\r', '\n').split("[。；;\\n]")) {
            String normalizedClause = normalizeText(clause);
            if (normalizedClause != null) {
                return normalizedClause;
            }
        }
        return null;
    }

    private String buildFallbackBoundaryNote(String question,
                                             PolicyQuestionType questionType,
                                             String regionScope,
                                             List<KnowledgeSearchResultVO> chunks) {
        boolean hasXm = false;
        boolean hasFj = false;
        for (KnowledgeSearchResultVO item : chunks) {
            if ("XM".equalsIgnoreCase(normalizeText(item.getRegionScope()))) {
                hasXm = true;
            }
            if ("FJ".equalsIgnoreCase(normalizeText(item.getRegionScope()))) {
                hasFj = true;
            }
        }
        if ("XM".equalsIgnoreCase(normalizeText(regionScope))) {
            return "\n边界说明：本次回答已优先按厦门市级政策口径整理；如有福建省级内容命中，仅作为上位补充，不能直接视为厦门市现行清单。";
        }
        if ("FJ".equalsIgnoreCase(normalizeText(regionScope))) {
            return "\n边界说明：本次回答已优先按福建省级政策口径整理；若用户后续追问厦门落地政策，需再切换到厦门市级专题核对。";
        }
        if (questionType == PolicyQuestionType.BOUNDARY || (hasXm && hasFj)) {
            return "\n边界说明：当前知识库同时包含厦门市级与福建省级政策，解读时需先区分省级指导性政策与市级落地政策。";
        }
        return null;
    }

    private String resolveRegionLabel(String regionScope) {
        String normalized = normalizeText(regionScope);
        if ("XM".equalsIgnoreCase(normalized)) {
            return "厦门市";
        }
        if ("FJ".equalsIgnoreCase(normalized)) {
            return "福建省";
        }
        return null;
    }

    private LinkedHashSet<String> collectFallbackCitations(List<KnowledgeSearchResultVO> chunks, String regionScope) {
        LinkedHashSet<String> citations = new LinkedHashSet<>();
        for (KnowledgeSearchResultVO item : selectFallbackChunks(null, PolicyQuestionType.GENERAL, regionScope, chunks)) {
            citations.add("- " + resolvePolicyDisplayName(item));
            if (citations.size() >= 5) {
                break;
            }
        }
        return citations;
    }

    private void appendPolicyConsultationRules(StringBuilder builder, String question, String sourceScene) {
        if (builder == null) {
            return;
        }
        builder.append("\nPolicy consultation rules:");
        builder.append("\n- When the user mentions 人才计划, you may interpret it as programs, projects, engineering tracks, recognitions, and support measures within talent policies.");
        builder.append("\n- If the knowledge base does not contain a complete city-level list, do not directly answer 不适用 or 无法确认. Summarize the policy categories that can be inferred first, then clearly state that the available list may be incomplete.");
        builder.append("\n- If there are upper-level policies, policy categories, or inductive conclusions, prefer those conclusions plus an incompleteness note instead of mechanical placeholder wording.");
        if (SOURCE_SCENE_MOBILE_POLICY_CONSULTANT.equals(sourceScene)) {
            builder.append("\n- For MOBILE_POLICY_CONSULTANT, keep answers concise by default. Unless the user explicitly asks for a detailed interpretation, try to stay within about 220 to 300 Chinese characters.");
        }
        if (normalizeText(question) != null && question.contains("人才计划")) {
            builder.append("\n- The current question includes 人才计划, so broaden retrieval and reasoning toward 人才政策, 人才项目, 人才工程, 人才认定, and 人才支持政策.");
        }
        builder.append("\n- If a specific policy or project is confirmed but the retrieval lacks full application process evidence, explicitly state that the policy/project exists and that only partial policy basis is currently available, while complete application process, application notice, or material checklist evidence is still missing.");
        builder.append("\n- In that situation, do not rewrite the problem as policy non-existence or no related content. Prefer known support targets, funding standards, and fund rules plus a clear incompleteness note.");
    }

    private void updateUserPreference(Long userId, String question, AgentUserPreferenceEntity existed) {
    if (userId == null || normalizeText(question) == null) {
        return;
    }

    AgentUserPreferenceEntity entity = existed == null ? new AgentUserPreferenceEntity() : existed;
    entity.setUserId(userId);

    String preferredAnswerStyle = detectPreferredAnswerStyle(question, existed);
    String recentTopics = mergeRecentTopics(existed == null ? null : existed.getRecentTopics(), question);

    entity.setPreferredAnswerStyle(limitDbText(preferredAnswerStyle, 64));
    entity.setRecentTopics(limitDbText(recentTopics, 480));
    entity.setLastQuestion(limitDbText(question, 480));

    String habitSummary = buildHabitSummary(entity);
    entity.setHabitSummary(limitDbText(habitSummary, 480));

    entity.setUpdateTime(LocalDateTime.now());
    agentUserPreferenceMapper.upsert(entity);
}

    private String detectPreferredAnswerStyle(String question, AgentUserPreferenceEntity existed) {
        String normalized = normalizeText(question);
        if (normalized == null) {
            return existed == null ? "PROFESSIONAL" : valueOrDefault(existed.getPreferredAnswerStyle(), "PROFESSIONAL");
        }
        if (normalized.contains("简洁") || normalized.contains("直接") || normalized.contains("一句话")) {
            return "CONCISE";
        }
        if (normalized.contains("详细") || normalized.contains("解读") || normalized.contains("全面") || normalized.contains("分析")) {
            return "DETAILED";
        }
        if (normalized.contains("流程") || normalized.contains("步骤") || normalized.contains("怎么申请") || normalized.contains("如何申请")) {
            return "STEP_BY_STEP";
        }
        if (normalized.contains("依据") || normalized.contains("出处") || normalized.contains("引用")) {
            return "CITATION_FIRST";
        }
        return existed == null ? "PROFESSIONAL" : valueOrDefault(existed.getPreferredAnswerStyle(), "PROFESSIONAL");
    }

    private String mergeRecentTopics(String existingTopics, String question) {
        LinkedHashSet<String> topics = new LinkedHashSet<>();
        for (String topic : splitCsv(existingTopics)) {
            if (normalizeText(topic) != null) {
                topics.add(topic.trim());
            }
        }
        for (String candidate : extractSearchCandidates(question)) {
            String normalized = normalizeText(candidate);
            if (normalized == null || normalized.length() < 2) {
                continue;
            }
            topics.add(abbreviate(normalized.replace(" ", ""), 32));
            if (topics.size() >= 6) {
                break;
            }
        }
        return String.join(",", topics);
    }

    private String buildHabitSummary(AgentUserPreferenceEntity entity) {
        List<String> parts = new ArrayList<>();
        if (normalizeText(entity.getPreferredAnswerStyle()) != null) {
            parts.add("偏好" + entity.getPreferredAnswerStyle() + "风格");
        }
        if (normalizeText(entity.getRecentTopics()) != null) {
            parts.add("近期关注" + entity.getRecentTopics().replace(",", "、"));
        }
        return String.join("；", parts);
    }

    private List<String> splitCsv(String value) {
        String normalized = normalizeText(value);
        if (normalized == null) {
            return List.of();
        }
        return Arrays.stream(normalized.split(","))
                .map(this::normalizeText)
                .filter(item -> item != null && !item.isBlank())
                .collect(Collectors.toList());
    }

    private String normalizeStatus(String status) {
        if (status == null) {
            throw new IllegalArgumentException("status is required");
        }
        String value = status.trim().toUpperCase();
        if (!STATUS_ACTIVE.equals(value) && !STATUS_ARCHIVED.equals(value)) {
            throw new IllegalArgumentException("unsupported session status");
        }
        return value;
    }
private String limitDbText(String text, int maxLength) {
    String normalized = normalizeText(text);
    if (normalized == null) {
        return null;
    }
    if (maxLength <= 0) {
        return "";
    }
    if (normalized.length() <= maxLength) {
        return normalized;
    }
    if (maxLength <= 3) {
        return normalized.substring(0, maxLength);
    }
    return normalized.substring(0, maxLength - 3) + "...";
}
    private String normalizeText(String text) {
        if (text == null) {
            return null;
        }
        String value = text.trim();
        return value.isEmpty() ? null : value;
    }

    private PolicyQuestionType detectPolicyQuestionType(String question) {
        return switch (PolicyKnowledgeSupport.detectQuestionType(question)) {
            case LIST -> PolicyQuestionType.LIST;
            case PROCESS -> PolicyQuestionType.PROCESS;
            case CONDITION -> PolicyQuestionType.CONDITION;
            case BENEFIT -> PolicyQuestionType.BENEFIT;
            case SPECIAL_TOPIC -> PolicyQuestionType.SPECIAL_TOPIC;
            case FAQ -> PolicyQuestionType.FAQ;
            case BOUNDARY -> PolicyQuestionType.BOUNDARY;
            default -> PolicyQuestionType.GENERAL;
        };
    }

    private PolicyQuestionType resolvePolicyQuestionType(PolicyRouteIntent routeIntent, String question) {
        if (routeIntent == null || routeIntent.getQuestionType() == null) {
            return detectPolicyQuestionType(question);
        }
        return switch (routeIntent.getQuestionType()) {
            case LIST -> PolicyQuestionType.LIST;
            case PROCESS -> PolicyQuestionType.PROCESS;
            case CONDITION -> PolicyQuestionType.CONDITION;
            case BENEFIT -> PolicyQuestionType.BENEFIT;
            case SPECIAL_TOPIC -> PolicyQuestionType.SPECIAL_TOPIC;
            case FAQ -> PolicyQuestionType.FAQ;
            case BOUNDARY -> PolicyQuestionType.BOUNDARY;
            default -> PolicyQuestionType.GENERAL;
        };
    }

    private List<String> extractFormalPolicyNames(String question) {
        LinkedHashSet<String> names = new LinkedHashSet<>();
        String normalized = normalizeText(question);
        if (normalized == null) {
            return List.of();
        }
        names.addAll(PolicyKnowledgeSupport.extractFormalPolicyNames(normalized));
        for (PolicyAliasMapping mapping : POLICY_ALIAS_MAPPINGS) {
            if (containsAny(normalized, mapping.aliases())) {
                names.add(mapping.canonicalName());
            }
        }
        return new ArrayList<>(names);
    }

    private List<String> extractMatchedAliasTerms(String question) {
        LinkedHashSet<String> terms = new LinkedHashSet<>();
        String normalized = normalizeText(question);
        if (normalized == null) {
            return List.of();
        }
        terms.addAll(PolicyKnowledgeSupport.extractMatchedAliases(normalized));
        for (PolicyAliasMapping mapping : POLICY_ALIAS_MAPPINGS) {
            for (String alias : mapping.aliases()) {
                if (containsKeyword(normalized, alias)) {
                    terms.add(alias);
                }
            }
        }
        return new ArrayList<>(terms);
    }

    private List<String> extractMatchedSpecialTopicTerms(String question) {
        LinkedHashSet<String> terms = new LinkedHashSet<>(extractMatchedAliasTerms(question));
        String normalized = normalizeText(question);
        if (normalized == null) {
            return new ArrayList<>(terms);
        }
        if (normalized.contains("福建省高层次人才认定")) {
            terms.add("福建省高层次人才认定");
        }
        if (normalized.contains("百人计划")) {
            terms.add("省引才百人计划");
        }
        if (normalized.contains("四大经济")) {
            terms.add("四大经济");
        }
        for (String keyword : SPECIAL_TOPIC_KEYWORDS) {
            if (containsKeyword(normalized, keyword)) {
                terms.add(keyword);
            }
        }
        return new ArrayList<>(terms);
    }

    private List<String> extractStructuredSearchCandidates(String question) {
        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        String normalized = normalizeText(question);
        if (normalized == null) {
            return List.of();
        }
        candidates.add(normalized);
        candidates.addAll(extractFormalPolicyNames(normalized));

        String simplified = normalized.replaceAll("[，。！？；：、,.;:?!]+", " ");
        for (String stop : SEARCH_STOP_TERMS) {
            simplified = simplified.replace(stop, " ");
        }
        simplified = simplified.replaceAll("\\s+", " ").trim();
        if (!simplified.isEmpty()) {
            candidates.add(simplified);
            candidates.add(simplified.replace(" ", ""));
        }

        for (String term : SEARCH_HINT_TERMS) {
            if (containsKeyword(normalized, term)) {
                candidates.add(term);
            }
        }

        candidates.addAll(extractMatchedAliasTerms(normalized));
        candidates.addAll(extractMatchedSpecialTopicTerms(normalized));

        PolicyQuestionType questionType = detectPolicyQuestionType(normalized);
        if (questionType == PolicyQuestionType.LIST) {
            candidates.add(CLEAN_POLICY_DOC_TITLE);
            candidates.add("导入目录");
            candidates.add("政策正文");
            candidates.add("咨询主题");
            candidates.add("检索建议");
        }
        if (questionType == PolicyQuestionType.PROCESS) {
            candidates.addAll(PROCESS_SECTION_TERMS);
        }
        if (questionType == PolicyQuestionType.CONDITION) {
            candidates.addAll(CONDITION_SECTION_TERMS);
        }
        if (questionType == PolicyQuestionType.BENEFIT) {
            candidates.addAll(BENEFIT_SECTION_TERMS);
        }
        if (containsKeyword(normalized, "人才计划")) {
            candidates.add("人才政策");
            candidates.add("人才项目");
            candidates.add("人才工程");
            candidates.add("人才认定");
            candidates.add("人才支持政策");
        }
        if (containsKeyword(normalized, "厦门") && containsKeyword(normalized, "人才") && containsKeyword(normalized, "政策")) {
            candidates.add("厦门市人才政策");
            candidates.add("厦门人才政策");
        }
        if (containsKeyword(normalized, "福建") && containsKeyword(normalized, "人才") && containsKeyword(normalized, "政策")) {
            candidates.add("福建省人才政策");
            candidates.add("福建人才政策");
        }
        return new ArrayList<>(candidates);
    }

    private List<String> extractStructuredTopicRecallCandidates(String question) {
        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        candidates.addAll(extractFormalPolicyNames(question));
        candidates.addAll(extractMatchedSpecialTopicTerms(question));
        candidates.addAll(extractMatchedAliasTerms(question));
        for (String candidate : extractStructuredSearchCandidates(question)) {
            if (!isProcessRecallCandidate(candidate)) {
                candidates.add(candidate);
            }
        }
        return new ArrayList<>(candidates);
    }

    private List<String> extractStructuredListRecallCandidates(String question) {
        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        candidates.add(CLEAN_POLICY_DOC_TITLE);
        candidates.add("导入目录");
        candidates.add("政策正文");
        candidates.add("咨询主题");
        candidates.add("检索建议");
        candidates.add("人才政策");
        candidates.add("实施办法");
        candidates.add("意见");
        candidates.addAll(extractStructuredTopicRecallCandidates(question));
        candidates.addAll(extractStructuredSearchCandidates(question));
        return new ArrayList<>(candidates);
    }

    private List<String> extractStructuredProcessRecallCandidates(String question) {
    LinkedHashSet<String> candidates = new LinkedHashSet<>(extractStructuredTopicRecallCandidates(question));
    candidates.addAll(PROCESS_SECTION_TERMS);
    candidates.add("申请流程");
    candidates.add("申报流程");
    candidates.add("遴选与申请流程");
    candidates.add("五步流程");
    candidates.add("申报材料");
    candidates.add("材料清单");
    candidates.add("兑现申请");
    candidates.add("组织申报");
    candidates.add("资格核查");
    candidates.add("部门联审");
    candidates.add("综合评审");
    candidates.add("公示确定");
    candidates.add("研究确认");
    return new ArrayList<>(candidates);
}
private List<String> buildDoubleHundredStrictRecallCandidates(String question) {
    LinkedHashSet<String> candidates = new LinkedHashSet<>();
    candidates.addAll(extractStructuredTopicRecallCandidates(question));
    candidates.add("双百计划 遴选与申请流程");
    candidates.add("双百计划 五步流程");
    candidates.add("双百计划 组织申报 资格核查 部门联审 综合评审 研究确认");
    candidates.add("创新团队 遴选与申请流程");
    candidates.add("创业人才 遴选与申请流程");
    candidates.add("厦门市引进高层次创新创业人才“双百计划”实施意见 申请流程");
    candidates.add("夏委组〔2024〕46号 遴选与申请流程");
    candidates.add("专项办 组织申报 资格核查 部门联审 综合评审 研究确认");
    candidates.add("材料评审 现场答辩 复核考察 建议人选 审定");
    return new ArrayList<>(candidates);
}

private boolean isDoubleHundredProcessChunk(KnowledgeSearchResultVO item) {
    if (!isProcessChunk(item)) {
        return false;
    }
    String text = buildChunkSearchText(item);
    boolean hasPolicy = containsAny(text, List.of(
            "双百计划",
            "创新团队",
            "创业人才",
            "厦门市引进高层次创新创业人才“双百计划”实施意见",
            "夏委组〔2024〕46号",
            "夏委组 46号",
            "高层次创新创业人才"
    ));
    boolean hasFlow = containsAny(text, List.of(
            "遴选与申请流程",
            "申请流程",
            "申报流程",
            "五步流程",
            "组织申报",
            "资格核查",
            "部门联审",
            "综合评审",
            "研究确认",
            "专项办",
            "材料评审",
            "现场答辩",
            "复核考察",
            "建议人选",
            "审定"
    ));
    return hasPolicy && hasFlow;
}

    private List<String> extractStructuredConditionRecallCandidates(String question) {
        LinkedHashSet<String> candidates = new LinkedHashSet<>(extractStructuredTopicRecallCandidates(question));
        candidates.addAll(CONDITION_SECTION_TERMS);
        candidates.add("申报条件");
        candidates.add("适用对象");
        candidates.add("资格要求");
        return new ArrayList<>(candidates);
    }

    private List<String> extractStructuredBenefitRecallCandidates(String question) {
    LinkedHashSet<String> candidates = new LinkedHashSet<>(extractStructuredTopicRecallCandidates(question));
    candidates.addAll(BENEFIT_SECTION_TERMS);
    candidates.add("补助标准");
    candidates.add("资金扶持");
    candidates.add("安家补贴");

    if (containsAny(question, SERVICE_GUARANTEE_TERMS)) {
        candidates.add("服务保障");
        candidates.add("人才服务");
        candidates.add("人才服务卡");
        candidates.add("绿色通道");
        candidates.add("一站式服务");
        candidates.add("子女入学");
        candidates.add("子女教育");
        candidates.add("子女就学");
        candidates.add("医疗保障");
        candidates.add("医疗服务");
        candidates.add("健康体检");
        candidates.add("配偶就业");
        candidates.add("落户服务");
        candidates.add("人才待遇");
        candidates.add("待遇保障");
    }

    return new ArrayList<>(candidates);
}

    private List<String> extractStructuredSpecialTopicSupplementCandidates(String question) {
        LinkedHashSet<String> candidates = new LinkedHashSet<>(extractStructuredTopicRecallCandidates(question));
        if (containsAny(question, List.of(
        "住房",
        "住房补贴",
        "子女教育",
        "子女入学",
        "子女就学",
        "医疗保障",
        "医疗服务",
        "健康体检",
        "服务保障",
        "人才服务",
        "绿色通道",
        "配偶就业",
        "落户服务"
))) {
    candidates.add("服务保障");
    candidates.add("人才服务");
    candidates.add("住房补贴");
    candidates.add("子女教育");
    candidates.add("子女入学");
    candidates.add("医疗保障");
    candidates.add("医疗服务");
    candidates.add("健康体检");
    candidates.add("配偶就业");
    candidates.add("落户服务");
}
        candidates.add("关于更加精准有效集聚人才加快推进高质量发展的意见");
        candidates.add("厦门市高层次人才专项资金管理办法");
        return new ArrayList<>(candidates);
    }

    private boolean isPreferredListChunk(KnowledgeSearchResultVO item, List<String> formalPolicyNames, List<String> specialTopicTerms) {
        boolean hasTopicIntent = (formalPolicyNames != null && !formalPolicyNames.isEmpty())
                || (specialTopicTerms != null && !specialTopicTerms.isEmpty());
        if (isAbstractSummaryChunk(item)) {
            return false;
        }
        if (hasTopicIntent) {
            return matchesPolicyIntent(item, formalPolicyNames, specialTopicTerms)
                    || containsAny(buildChunkSearchText(item), OVERVIEW_SECTION_TERMS);
        }
        return containsKeyword(item == null ? null : item.getDocTitle(), CLEAN_POLICY_DOC_TITLE)
                || containsKeyword(item == null ? null : item.getDocTitle(), RAW_POLICY_DOC_TITLE)
                || containsAny(buildChunkSearchText(item), OVERVIEW_SECTION_TERMS);
    }

    private boolean matchesPolicyIntent(KnowledgeSearchResultVO item, List<String> formalPolicyNames, List<String> specialTopicTerms) {
        String text = buildChunkSearchText(item);
        return containsAny(text, formalPolicyNames) || containsAny(text, specialTopicTerms);
    }

    private boolean isAbstractSummaryChunk(KnowledgeSearchResultVO item) {
        return containsAny(buildChunkSearchText(item), ABSTRACT_SUMMARY_TERMS);
    }

    private boolean isProcessChunk(KnowledgeSearchResultVO item) {
        return containsAny(buildChunkSearchText(item), PROCESS_SECTION_TERMS)
                || containsAny(buildChunkSearchText(item), List.of("申请", "申报", "流程", "材料", "清单", "受理", "审核", "公示", "拨付", "兑现"));
    }

    private boolean isConditionChunk(KnowledgeSearchResultVO item) {
        return containsAny(buildChunkSearchText(item), CONDITION_SECTION_TERMS)
                || containsAny(buildChunkSearchText(item), List.of("条件", "对象", "资格", "要求", "适用对象"));
    }

    private boolean isBenefitChunk(KnowledgeSearchResultVO item) {
    String text = buildChunkSearchText(item);
    return containsAny(text, BENEFIT_SECTION_TERMS)
            || containsAny(text, SERVICE_GUARANTEE_TERMS)
            || containsAny(text, List.of("补助", "补贴", "奖励", "支持", "安家补贴", "资金扶持", "管理期"));
}

    private boolean hasStructuredSpecificPolicyHitWithoutProcessEvidence(String question, KnowledgeCitationContext context) {
        return contextMatchesStructuredSpecificPolicy(question, context) && !contextHasStructuredProcessEvidence(context);
    }

    private boolean contextMatchesStructuredSpecificPolicy(String question, KnowledgeCitationContext context) {
        if (context == null || context.getChunks().isEmpty()) {
            return false;
        }
        List<String> terms = extractStructuredSpecificPolicyTerms(question);
        if (terms.isEmpty()) {
            return false;
        }
        for (KnowledgeSearchResultVO item : context.getChunks()) {
            if (containsAny(buildChunkSearchText(item), terms)) {
                return true;
            }
        }
        return false;
    }

    private boolean contextHasStructuredProcessEvidence(KnowledgeCitationContext context) {
        if (context == null || context.getChunks().isEmpty()) {
            return false;
        }
        for (KnowledgeSearchResultVO item : context.getChunks()) {
            if (isProcessChunk(item)) {
                return true;
            }
        }
        return false;
    }

    private List<String> extractStructuredSpecificPolicyTerms(String question) {
        LinkedHashSet<String> terms = new LinkedHashSet<>(extractFormalPolicyNames(question));
        terms.addAll(extractMatchedSpecialTopicTerms(question));
        terms.addAll(extractMatchedAliasTerms(question));
        return new ArrayList<>(terms);
    }

    private boolean containsAny(String text, List<String> terms) {
        String normalized = normalizeText(text);
        if (normalized == null || terms == null || terms.isEmpty()) {
            return false;
        }
        for (String term : terms) {
            if (containsKeyword(normalized, term)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsKeyword(String text, String keyword) {
        String normalizedText = normalizeText(text);
        String normalizedKeyword = normalizeText(keyword);
        if (normalizedText == null || normalizedKeyword == null) {
            return false;
        }
        return normalizedText.contains(normalizedKeyword)
                || normalizedText.toLowerCase().contains(normalizedKeyword.toLowerCase());
    }

    private boolean isSpecificProjectProcessQuestion(String question) {
    return detectPolicyQuestionType(question) == PolicyQuestionType.PROCESS
            && !extractStructuredSpecificPolicyTerms(question).isEmpty();
}

    private boolean isProcessQuestion(String question) {
    return detectPolicyQuestionType(question) == PolicyQuestionType.PROCESS;
}

    private List<String> extractProjectRecallCandidates(String question) {
    return extractStructuredTopicRecallCandidates(question);
}

    private List<String> extractProcessRecallCandidates(String question) {
    return extractStructuredProcessRecallCandidates(question);
}

    private boolean isProcessRecallCandidate(String candidate) {
    return containsAny(candidate, PROCESS_SECTION_TERMS)
            || containsAny(candidate, List.of("申请", "申报", "流程", "材料", "清单", "办理", "受理", "推荐", "审核", "入口", "通知", "指南", "公示"));
}

    private boolean hasSpecificPolicyHitWithoutProcessEvidence(String question, KnowledgeCitationContext context) {
    return hasStructuredSpecificPolicyHitWithoutProcessEvidence(question, context);
}

    private boolean contextMatchesSpecificPolicy(String question, KnowledgeCitationContext context) {
    return contextMatchesStructuredSpecificPolicy(question, context);
}

    private boolean contextHasProcessEvidence(KnowledgeCitationContext context) {
    return contextHasStructuredProcessEvidence(context);
}

    private List<String> extractSpecificPolicyTerms(String question) {
    return extractStructuredSpecificPolicyTerms(question);
}

    private String buildChunkSearchText(KnowledgeSearchResultVO item) {
        if (item == null) {
            return "";
        }
        return String.join(" ",
                valueOrBlank(item.getPolicyName()),
                valueOrBlank(item.getPolicyAliases()),
                valueOrBlank(item.getPolicyNo()),
                valueOrBlank(item.getDocTitle()),
                valueOrBlank(item.getChapterTitle()),
                valueOrBlank(item.getSectionTitle()),
                valueOrBlank(item.getHeadingPath()),
                valueOrBlank(item.getSnippet()),
                valueOrBlank(item.getPolicyRegion()),
                valueOrBlank(item.getRegionScope()));
    }

    private List<String> extractSearchCandidates(String question) {
    return extractStructuredSearchCandidates(question);
}

    private String abbreviate(String text, int maxLength) {
    return limitDbText(text, maxLength);
}

    private String csvValue(Object value) {
        String text = value == null ? "" : String.valueOf(value);
        String normalized = text.replace("\r", " ").replace("\n", " ").replace("\"", "\"\"");
        return '"' + normalized + '"';
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? "" : value.format(DATE_TIME_FORMATTER);
    }

    private String formatRate(Double value) {
        if (value == null) {
            return "0.00%";
        }
        return String.format("%.2f%%", value);
    }

    private String valueOrBlank(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String valueOrDefault(String value, String defaultValue) {
        return normalizeText(value) == null ? defaultValue : value.trim();
    }

    private String buildExportLogContent(String type, AgentSessionQueryRequest request, int count) {
        AgentSessionQueryRequest safeRequest = request == null ? new AgentSessionQueryRequest() : request;
        return "export consultation ledger type=" + type
                + ", count=" + count
                + ", startDate=" + valueOrBlank(safeRequest.getStartDate())
                + ", endDate=" + valueOrBlank(safeRequest.getEndDate())
                + ", skillId=" + valueOrBlank(safeRequest.getSkillId())
                + ", status=" + valueOrBlank(safeRequest.getStatus());
    }

    private enum PolicyQuestionType {
        GENERAL,
        LIST,
        PROCESS,
        CONDITION,
        BENEFIT,
        SPECIAL_TOPIC,
        FAQ,
        BOUNDARY
    }

    private record PolicyAliasMapping(String canonicalName, List<String> aliases) {
    }

    private enum ListPolicyGroup {
        LEADING("统领政策"),
        INDUSTRY("重点产业/专项项目"),
        PUBLIC("公共类专项");

        private final String label;

        ListPolicyGroup(String label) {
            this.label = label;
        }

        private String label() {
            return label;
        }
    }

    private record FastPathListItem(KnowledgeSearchResultVO chunk,
                                    String policyName,
                                    ListPolicyGroup group,
                                    int score) {
    }

    private record ProviderResolution(ProviderConfigEntity provider, String modelCode) {
    }
}
