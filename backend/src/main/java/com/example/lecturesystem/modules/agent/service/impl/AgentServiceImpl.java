package com.example.lecturesystem.modules.agent.service.impl;

import com.example.lecturesystem.modules.agent.dto.AgentChatRequest;
import com.example.lecturesystem.modules.agent.dto.AgentSessionQueryRequest;
import com.example.lecturesystem.modules.agent.dto.CreateAgentSessionRequest;
import com.example.lecturesystem.modules.agent.dto.UpdateAgentSessionStatusRequest;
import com.example.lecturesystem.modules.agent.entity.AgentMessageEntity;
import com.example.lecturesystem.modules.agent.entity.AgentSessionEntity;
import com.example.lecturesystem.modules.agent.entity.AgentUserPreferenceEntity;
import com.example.lecturesystem.modules.agent.mapper.AgentMessageMapper;
import com.example.lecturesystem.modules.agent.mapper.AgentSessionMapper;
import com.example.lecturesystem.modules.agent.mapper.AgentUserPreferenceMapper;
import com.example.lecturesystem.modules.agent.service.AgentService;
import com.example.lecturesystem.modules.agent.support.KnowledgeCitationContext;
import com.example.lecturesystem.modules.agent.support.OpenAiCompatibleChatClient;
import com.example.lecturesystem.modules.agent.support.policy.PolicyEligibilityMatchTool;
import com.example.lecturesystem.modules.agent.support.policy.PolicyKnowledgeRetrievalTool;
import com.example.lecturesystem.modules.agent.support.policy.PolicyQuestionClassifier;
import com.example.lecturesystem.modules.agent.support.policy.PolicySkillStrategy;
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
import com.example.lecturesystem.modules.knowledge.vo.KnowledgeSearchResultVO;
import com.example.lecturesystem.modules.logcenter.service.LogCenterService;
import com.example.lecturesystem.modules.operationlog.service.OperationLogService;
import com.example.lecturesystem.modules.permission.service.PermissionService;
import com.example.lecturesystem.modules.permission.support.CurrentUserFacade;
import com.example.lecturesystem.modules.skill.dto.SkillQueryRequest;
import com.example.lecturesystem.modules.skill.entity.SkillVersionEntity;
import com.example.lecturesystem.modules.skill.mapper.SkillMapper;
import com.example.lecturesystem.modules.skill.mapper.SkillVersionMapper;
import com.example.lecturesystem.modules.skill.vo.SkillListItemVO;
import com.example.lecturesystem.modules.skill.vo.SkillVersionDetailVO;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AgentServiceImpl implements AgentService {
    private static final String DEFAULT_SESSION_TITLE = "New session";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_ARCHIVED = "ARCHIVED";
    private static final String SOURCE_SCENE_AI_WORKBENCH = "AI_WORKBENCH";
    private static final String SOURCE_SCENE_MOBILE_POLICY_CONSULTANT = "MOBILE_POLICY_CONSULTANT";
    private static final String DEFAULT_POLICY_SKILL_CODE = "talent_policy_consultant";
    private static final int DEFAULT_CHAT_KNOWLEDGE_TOP_N = 5;
    private static final int MOBILE_POLICY_CHAT_KNOWLEDGE_TOP_N = 3;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String FALLBACK_REASON_NO_AI_PERMISSION = "AI permission is unavailable. Switched to knowledge fallback.";
    private static final String FALLBACK_REASON_NO_PROVIDER = "No available AI provider. Switched to knowledge fallback.";
    private static final String FALLBACK_REASON_AI_UNAVAILABLE = "AI service is unavailable. Switched to knowledge fallback.";
    private static final List<String> SEARCH_STOP_TERMS = List.of("\u8bf7\u95ee", "\u76ee\u524d", "\u73b0\u5728", "\u6709\u54ea\u4e9b", "\u6709\u5565", "\u4ec0\u4e48", "\u600e\u4e48", "\u5982\u4f55", "\u53ef\u4ee5", "\u5417", "\u5462", "\u4e00\u4e0b");
    private static final List<String> SEARCH_HINT_TERMS = List.of(
            "\u53a6\u95e8\u5e02", "\u53a6\u95e8", "\u798f\u5efa\u7701", "\u798f\u5efa", "\u4eba\u624d", "\u653f\u7b56", "\u4eba\u624d\u653f\u7b56", "\u8865\u8d34", "\u4f4f\u623f", "\u5b89\u5c45",
            "\u8ba4\u5b9a", "\u7533\u62a5", "\u521b\u4e1a", "\u9879\u76ee", "\u4f01\u4e1a", "\u5f15\u8fdb", "\u9ad8\u5c42\u6b21", "\u843d\u6237", "\u5956\u52b1", "\u7ecf\u8d39", "\u652f\u6301"
    );
    private final AgentSessionMapper agentSessionMapper;
    private final AgentMessageMapper agentMessageMapper;
    private final AgentUserPreferenceMapper agentUserPreferenceMapper;
    private final SkillMapper skillMapper;
    private final SkillVersionMapper skillVersionMapper;
    private final ProviderConfigMapper providerConfigMapper;
    private final KnowledgeChunkMapper knowledgeChunkMapper;
    private final AiTokenCipherSupport aiTokenCipherSupport;
    private final OpenAiCompatibleChatClient chatClient;
    private final CurrentUserFacade currentUserFacade;
    private final PermissionService permissionService;
    private final AiPermissionService aiPermissionService;
    private final LogCenterService logCenterService;
    private final OperationLogService operationLogService;
    private final PolicyQuestionClassifier policyQuestionClassifier;
    private final PolicySkillStrategy policySkillStrategy;
    private final PolicyKnowledgeRetrievalTool policyKnowledgeRetrievalTool;
    private final PolicyEligibilityMatchTool policyEligibilityMatchTool;

    public AgentServiceImpl(AgentSessionMapper agentSessionMapper,
                            AgentMessageMapper agentMessageMapper,
                            AgentUserPreferenceMapper agentUserPreferenceMapper,
                            SkillMapper skillMapper,
                            SkillVersionMapper skillVersionMapper,
                            ProviderConfigMapper providerConfigMapper,
                            KnowledgeChunkMapper knowledgeChunkMapper,
                            AiTokenCipherSupport aiTokenCipherSupport,
                            OpenAiCompatibleChatClient chatClient,
                            CurrentUserFacade currentUserFacade,
                            PermissionService permissionService,
                            AiPermissionService aiPermissionService,
                            LogCenterService logCenterService,
                            OperationLogService operationLogService) {
        this.agentSessionMapper = agentSessionMapper;
        this.agentMessageMapper = agentMessageMapper;
        this.agentUserPreferenceMapper = agentUserPreferenceMapper;
        this.skillMapper = skillMapper;
        this.skillVersionMapper = skillVersionMapper;
        this.providerConfigMapper = providerConfigMapper;
        this.knowledgeChunkMapper = knowledgeChunkMapper;
        this.aiTokenCipherSupport = aiTokenCipherSupport;
        this.chatClient = chatClient;
        this.currentUserFacade = currentUserFacade;
        this.permissionService = permissionService;
        this.aiPermissionService = aiPermissionService;
        this.logCenterService = logCenterService;
        this.operationLogService = operationLogService;
        this.policyQuestionClassifier = new PolicyQuestionClassifier();
        this.policySkillStrategy = new PolicySkillStrategy();
        this.policyKnowledgeRetrievalTool = new PolicyKnowledgeRetrievalTool(knowledgeChunkMapper);
        this.policyEligibilityMatchTool = new PolicyEligibilityMatchTool();
    }

    @Override
    @Transactional
    public Object createSession(CreateAgentSessionRequest request) {
        CreateAgentSessionRequest safeRequest = request == null ? new CreateAgentSessionRequest() : request;
        LoginUser user = currentUserFacade.currentLoginUser();
        requireAgentPermission(user);
        SkillSelection selection = resolveSkillSelection(user, safeRequest);
        SkillVersionDetailVO version = requirePublishedVersion(selection.skillId());
        Long baseId = safeRequest.getBaseId() != null ? safeRequest.getBaseId() : version.getBaseId();
        if (baseId == null) {
            throw new IllegalArgumentException("skill is not bound to a knowledge base");
        }
        requireKnowledgeAnalyze(user, baseId);

        String sourceScene = normalizeSourceScene(safeRequest.getSourceScene());
        AgentSessionEntity entity = new AgentSessionEntity();
        entity.setUserId(user.getUserId());
        entity.setSkillId(selection.skillId());
        entity.setSkillVersionId(version.getId());
        entity.setProviderConfigId(version.getProviderConfigId());
        entity.setModelCode(version.getModelCode());
        entity.setBaseId(baseId);
        entity.setSessionTitle(DEFAULT_SESSION_TITLE);
        entity.setSourceScene(sourceScene);
        entity.setStatus(STATUS_ACTIVE);
        entity.setCreateTime(LocalDateTime.now());
        agentSessionMapper.insert(entity);

        operationLogService.log("AGENT", "CREATE_SESSION", entity.getId(), "create session in " + sourceScene);
        operationLogService.log("AGENT", "CREATE_SESSION", entity.getId(), "create session in " + sourceScene);
        recordAiEvent("SESSION_CREATE", "INFO", "SESSION_CREATED", "AI session created for the current source scene.",
                "Check skillId, skillMatchMode and sourceScene if skill routing needs verification.", null,
                aiData(
                        "sessionId", entity.getId(),
                        "userId", user.getUserId(),
                        "skillId", selection.skillId(),
                        "skillName", selection.skillName(),
                        "skillMatchMode", selection.matchMode(),
                        "baseId", baseId,
                        "sourceScene", sourceScene
                ));
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
        try {
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

            recordAiEvent("MONTHLY_REPORT", "INFO", "MONTHLY_REPORT_READY", "Monthly report aggregation finished.",
                    "Use sourceScene and time range to verify monthly report inputs.", null,
                    aiData(
                            "year", safeRequest.getYear(),
                            "sourceScene", safeRequest.getSourceScene(),
                            "totalSessions", report.getTotalSessionCount(),
                            "totalMessages", report.getTotalMessageCount()
                    ));
            return report;
        } catch (Exception ex) {
            recordAiEvent("MONTHLY_REPORT", "ERROR", "MONTHLY_REPORT_FAILED", "Monthly report aggregation failed.",
                    "Check report filters, sourceScene, and mapper aggregation logic.", "AI_MONTHLY_REPORT_FAILED",
                    aiData(
                            "year", safeRequest.getYear(),
                            "sourceScene", safeRequest.getSourceScene(),
                            "errorMessage", normalizeText(ex.getMessage())
                    ));
            throw ex;
        }
    }

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
        builder.append("Session ID,Session Title,Source Scene,Status,Username,Real Name,Skill,Knowledge Base,Model,Message Count,Last Message Time,Create Time\r\n");
        for (AgentSessionVO item : list) {
            builder.append(csvValue(item.getId())).append(',')
                    .append(csvValue(item.getSessionTitle())).append(',')
                    .append(csvValue(item.getSourceScene())).append(',')
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
            String[] headers = {"Session ID", "Session Title", "Source Scene", "Status", "Username", "Real Name", "Skill", "Knowledge Base", "Model", "Message Count", "Last Message Time", "Create Time"};
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }
            for (int i = 0; i < list.size(); i++) {
                AgentSessionVO item = list.get(i);
                Row row = sheet.createRow(i + 1);
                row.createCell(0).setCellValue(valueOrBlank(item.getId()));
                row.createCell(1).setCellValue(valueOrBlank(item.getSessionTitle()));
                row.createCell(2).setCellValue(valueOrBlank(item.getSourceScene()));
                row.createCell(3).setCellValue(valueOrBlank(item.getStatus()));
                row.createCell(4).setCellValue(valueOrBlank(item.getUsername()));
                row.createCell(5).setCellValue(valueOrBlank(item.getRealName()));
                row.createCell(6).setCellValue(valueOrBlank(item.getSkillName()));
                row.createCell(7).setCellValue(valueOrBlank(item.getBaseName()));
                row.createCell(8).setCellValue(valueOrBlank(item.getModelCode()));
                row.createCell(9).setCellValue(valueOrBlank(item.getMessageCount()));
                row.createCell(10).setCellValue(formatDateTime(item.getLastMessageTime()));
                row.createCell(11).setCellValue(formatDateTime(item.getCreateTime()));
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
        String sourceScene = normalizeSourceScene(normalizeText(request.getSourceScene()) != null ? request.getSourceScene() : session.getSourceScene());
        if (!permissionService.isSuperAdmin(user.getUserId()) && !session.getUserId().equals(user.getUserId())) {
            recordAiEvent("AI_CHAT", "WARN", "SESSION_FORBIDDEN", "Current user cannot access this session.",
                    "Check session owner and current login user.", "AI_SESSION_FORBIDDEN",
                    aiData("sessionId", request.getSessionId(), "userId", user.getUserId(), "sourceScene", sourceScene));
            throw new IllegalArgumentException("no permission to access this session");
        }
        if (!STATUS_ACTIVE.equalsIgnoreCase(session.getStatus())) {
            recordAiEvent("AI_CHAT", "WARN", "SESSION_ARCHIVED", "Current session is archived and cannot continue.",
                    "Create a new session or reactivate the session before chatting again.", "AI_SESSION_ARCHIVED",
                    aiData("sessionId", request.getSessionId(), "status", session.getStatus(), "sourceScene", sourceScene));
            throw new IllegalArgumentException("session is archived and cannot continue chatting");
        }
        requireSkillUse(user, session.getSkillId());
        requireKnowledgeAnalyze(user, session.getBaseId());

        SkillVersionEntity version = requireVersion(session.getSkillVersionId());
        PolicyQuestionClassifier.Analysis questionAnalysis = policyQuestionClassifier.analyze(request.getQuestion());
        PolicySkillStrategy.Decision skillDecision = policySkillStrategy.decide(sourceScene, questionAnalysis, resolveChatKnowledgeTopN(sourceScene));
        recordAiEvent("AI_CHAT", "INFO", "REQUEST_ENTERED", "AI main chain accepted the user question.",
                "Continue checking skill routing, retrieval, provider and result-flow logs.", null,
                aiData(
                        "sessionId", session.getId(),
                        "userId", user.getUserId(),
                        "sourceScene", sourceScene,
                        "skillId", session.getSkillId(),
                        "skillHint", request.getSkillHint(),
                        "questionType", questionAnalysis.getQuestionType(),
                        "extractedFields", questionAnalysis.toFieldMap(),
                        "missingFields", questionAnalysis.getMissingFields(),
                        "needFollowup", skillDecision.isNeedFollowup(),
                        "retrievalStrategy", skillDecision.getRetrievalStrategy(),
                        "question", abbreviate(request.getQuestion(), 120)
                ));

        PolicyKnowledgeRetrievalTool.Result retrievalResult = policyKnowledgeRetrievalTool.retrieve(
                session.getBaseId(),
                request.getQuestion(),
                questionAnalysis,
                skillDecision,
                extractSearchCandidates(request.getQuestion())
        );
        KnowledgeCitationContext context = retrievalResult.getContext();
        PolicyEligibilityMatchTool.Result eligibilityMatch = PolicyQuestionClassifier.QUESTION_TYPE_ELIGIBILITY.equals(questionAnalysis.getQuestionType())
                ? policyEligibilityMatchTool.match(questionAnalysis, retrievalResult)
                : null;
        AgentUserPreferenceEntity preference = agentUserPreferenceMapper.findByUserId(user.getUserId());
        boolean canUseAi = permissionService.isSuperAdmin(user.getUserId()) || aiPermissionService.canUseAi(user.getUserId());

        recordAiEvent("AI_CHAT", "INFO", "KNOWLEDGE_HIT", context.getChunks().isEmpty() ? "No knowledge chunks were hit." : "Knowledge chunks were hit.",
                "Check retrievalStrategy, topChunksType and citedChunkIds for retrieval quality.", null,
                aiData(
                        "sessionId", session.getId(),
                        "sourceScene", sourceScene,
                        "regionPriority", resolveRegionPriority(sourceScene, request.getQuestion()),
                        "questionType", questionAnalysis.getQuestionType(),
                        "extractedFields", questionAnalysis.toFieldMap(),
                        "missingFields", questionAnalysis.getMissingFields(),
                        "needFollowup", skillDecision.isNeedFollowup(),
                        "skillId", session.getSkillId(),
                        "baseId", session.getBaseId(),
                        "knowledgeTopN", skillDecision.getTopN(),
                        "knowledgeHitCount", context.getChunks().size(),
                        "retrievalStrategy", retrievalResult.getRetrievalStrategy(),
                        "topChunksType", retrievalResult.getTopChunkTypes(),
                        "citedChunkIds", context.toChunkIds(),
                        "titles", context.getChunks().stream().map(KnowledgeSearchResultVO::getDocTitle).distinct().collect(Collectors.toList())
                ));

        AgentMessageEntity userMessage = new AgentMessageEntity();
        userMessage.setSessionId(session.getId());
        userMessage.setMessageRole("user");
        userMessage.setMessageText(request.getQuestion());
        userMessage.setCreateTime(LocalDateTime.now());
        try {
            agentMessageMapper.insert(userMessage);
        } catch (Exception ex) {
            recordAiEvent("AI_CHAT", "ERROR", "USER_MESSAGE_SAVE_FAILED", "Failed to persist user message.",
                    "Check ai_agent_message schema, transaction state and database connection.", "AI_MESSAGE_SAVE_FAILED",
                    aiData("sessionId", session.getId(), "messageRole", "user", "sourceScene", sourceScene, "errorMessage", normalizeText(ex.getMessage())));
            throw ex;
        }
        recordAiEvent("AI_CHAT", "INFO", "USER_MESSAGE_SAVED", "User question persisted to ai_agent_message.",
                "If no answer returns later, keep checking provider and assistant-message logs.", null,
                aiData("sessionId", session.getId(), "messageRole", "user", "sourceScene", sourceScene, "question", abbreviate(request.getQuestion(), 120)));

        String answer;
        long providerCallStartedAt = 0L;
        long providerCallElapsedMs = 0L;
        boolean knowledgeFallbackUsed = false;
        boolean followupTriggered = skillDecision.isNeedFollowup();
        ProviderResolution providerResolution = null;
        if (followupTriggered) {
            answer = buildFollowupQuestion(questionAnalysis);
            recordAiEvent("AI_CHAT", "INFO", "FOLLOWUP_REQUIRED", "Eligibility question needs key fields before answering.",
                    "Provide region, work status, industry or theme before doing eligibility matching.", null,
                    aiData("sessionId", session.getId(), "sourceScene", sourceScene, "questionType", questionAnalysis.getQuestionType(), "missingFields", questionAnalysis.getMissingFields(), "needFollowup", true));
        } else {
            providerResolution = canUseAi ? resolveProvider(session, version) : null;
            if (providerResolution == null) {
                recordAiEvent("AI_CHAT", "WARN", "PROVIDER_MISSING",
                        canUseAi ? "No available AI provider; switched to knowledge fallback." : "AI permission disabled; switched to knowledge fallback.",
                        "Check provider config, provider test result and AI permission.",
                        canUseAi ? "AI_PROVIDER_MISSING" : "AI_PERMISSION_DISABLED",
                        aiData("sessionId", session.getId(), "sourceScene", sourceScene, "canUseAi", canUseAi));
                answer = buildFallbackAnswer(request.getQuestion(), sourceScene, context, preference, questionAnalysis, retrievalResult, eligibilityMatch,
                        canUseAi ? FALLBACK_REASON_NO_PROVIDER : FALLBACK_REASON_NO_AI_PERMISSION);
                knowledgeFallbackUsed = true;
            } else {
                recordAiEvent("AI_CHAT", "INFO", "PROVIDER_SELECTED", "Provider and model have been selected for this chat call.",
                        "Use providerId, providerName and modelCode for troubleshooting.", null,
                        aiData(
                                "sessionId", session.getId(),
                                "sourceScene", sourceScene,
                                "questionType", questionAnalysis.getQuestionType(),
                                "retrievalStrategy", retrievalResult.getRetrievalStrategy(),
                                "providerId", providerResolution.provider().getId(),
                                "providerName", providerResolution.provider().getProviderName(),
                                "modelCode", providerResolution.modelCode(),
                                "connectTimeoutMs", chatClient.getConnectTimeoutMillis(),
                                "requestTimeoutMs", chatClient.getRequestTimeoutMillis()
                        ));
                recordAiEvent("AI_CHAT", "INFO", "PROVIDER_CALL_START", "Provider call started.",
                        "Use timeout settings and downstream logs if the response is slow.", null,
                        aiData(
                                "sessionId", session.getId(),
                                "sourceScene", sourceScene,
                                "questionType", questionAnalysis.getQuestionType(),
                                "retrievalStrategy", retrievalResult.getRetrievalStrategy(),
                                "topChunksType", retrievalResult.getTopChunkTypes(),
                                "providerId", providerResolution.provider().getId(),
                                "providerName", providerResolution.provider().getProviderName(),
                                "modelCode", providerResolution.modelCode(),
                                "connectTimeoutMs", chatClient.getConnectTimeoutMillis(),
                                "requestTimeoutMs", chatClient.getRequestTimeoutMillis()
                        ));
                try {
                    providerCallStartedAt = System.currentTimeMillis();
                    String apiToken = aiTokenCipherSupport.decrypt(providerResolution.provider().getApiTokenCipher());
                    answer = chatClient.chat(
                            providerResolution.provider().getApiBaseUrl(),
                            apiToken,
                            providerResolution.modelCode(),
                            buildSystemPrompt(version, preference),
                            buildUserPrompt(version, request.getQuestion(), sourceScene, context, preference, questionAnalysis, retrievalResult, eligibilityMatch)
                    );
                    providerCallElapsedMs = Math.max(0L, System.currentTimeMillis() - providerCallStartedAt);
                    recordAiEvent("AI_CHAT", "INFO", "PROVIDER_CALL_SUCCESS", "Provider call succeeded.",
                            "Use answerLength and cited chunks to evaluate first-answer quality.", null,
                            aiData(
                                    "sessionId", session.getId(),
                                    "sourceScene", sourceScene,
                                    "questionType", questionAnalysis.getQuestionType(),
                                    "providerId", providerResolution.provider().getId(),
                                    "providerName", providerResolution.provider().getProviderName(),
                                    "modelCode", providerResolution.modelCode(),
                                    "connectTimeoutMs", chatClient.getConnectTimeoutMillis(),
                                    "requestTimeoutMs", chatClient.getRequestTimeoutMillis(),
                                    "durationMs", providerCallElapsedMs,
                                    "answerLength", answer == null ? 0 : answer.length()
                            ));
                } catch (Exception ex) {
                    providerCallElapsedMs = providerCallStartedAt > 0L ? Math.max(0L, System.currentTimeMillis() - providerCallStartedAt) : 0L;
                    knowledgeFallbackUsed = true;
                    recordAiEvent("AI_CHAT", "ERROR", "PROVIDER_CALL_FAILED", "Provider call failed and fallback will be used.",
                            "Check provider connectivity, model configuration and downstream error details.", "AI_PROVIDER_CALL_FAILED",
                            aiData(
                                    "sessionId", session.getId(),
                                    "sourceScene", sourceScene,
                                    "questionType", questionAnalysis.getQuestionType(),
                                    "providerId", providerResolution.provider().getId(),
                                    "providerName", providerResolution.provider().getProviderName(),
                                    "modelCode", providerResolution.modelCode(),
                                    "connectTimeoutMs", chatClient.getConnectTimeoutMillis(),
                                    "requestTimeoutMs", chatClient.getRequestTimeoutMillis(),
                                    "durationMs", providerCallElapsedMs,
                                    "exceptionType", rootCauseType(ex),
                                    "timeoutLike", isTimeoutLike(ex),
                                    "errorMessage", normalizeText(ex.getMessage())
                            ));
                    answer = buildFallbackAnswer(request.getQuestion(), sourceScene, context, preference, questionAnalysis, retrievalResult, eligibilityMatch,
                            FALLBACK_REASON_AI_UNAVAILABLE + " Reason: " + normalizeText(ex.getMessage()));
                }
            }
        }

        if (providerResolution != null && knowledgeFallbackUsed) {
            recordAiEvent("AI_CHAT", "WARN", "KNOWLEDGE_FALLBACK_USED", "Knowledge fallback answered after provider failure.",
                    "Check provider timeout, context length and model selection for first-answer optimization.", null,
                    aiData(
                            "sessionId", session.getId(),
                            "sourceScene", sourceScene,
                            "questionType", questionAnalysis.getQuestionType(),
                            "providerId", providerResolution.provider().getId(),
                            "providerName", providerResolution.provider().getProviderName(),
                            "modelCode", providerResolution.modelCode(),
                            "durationMs", providerCallElapsedMs,
                            "knowledgeFallback", true,
                            "fallbackTriggered", true,
                            "topChunksType", retrievalResult.getTopChunkTypes(),
                            "knowledgeHitCount", context.getChunks().size(),
                            "citedChunkIds", context.toChunkIds()
                    ));
        }

        AgentMessageEntity assistantMessage = new AgentMessageEntity();
        assistantMessage.setSessionId(session.getId());
        assistantMessage.setMessageRole("assistant");
        assistantMessage.setMessageText(answer);
        assistantMessage.setCitedChunkIds(context.toChunkIds());
        assistantMessage.setCreateTime(LocalDateTime.now());
        try {
            agentMessageMapper.insert(assistantMessage);
        } catch (Exception ex) {
            recordAiEvent("AI_CHAT", "ERROR", "ASSISTANT_MESSAGE_SAVE_FAILED", "Failed to persist assistant message.",
                    "Check ai_agent_message schema, transaction state and database connection.", "AI_MESSAGE_SAVE_FAILED",
                    aiData("sessionId", session.getId(), "messageRole", "assistant", "sourceScene", sourceScene, "errorMessage", normalizeText(ex.getMessage())));
            throw ex;
        }
        recordAiEvent("AI_CHAT", "INFO", "ASSISTANT_MESSAGE_SAVED", "Assistant message persisted to ai_agent_message.",
                "If ledger or monthly report still miss data, continue checking sourceScene and result-flow logs.", null,
                aiData(
                        "sessionId", session.getId(),
                        "messageRole", "assistant",
                        "sourceScene", sourceScene,
                        "questionType", questionAnalysis.getQuestionType(),
                        "needFollowup", followupTriggered,
                        "fallbackTriggered", knowledgeFallbackUsed,
                        "citedChunkIds", context.toChunkIds(),
                        "answerLength", answer == null ? 0 : answer.length()
                ));

        if (DEFAULT_SESSION_TITLE.equals(session.getSessionTitle())) {
            String newTitle = buildSessionTitle(request.getQuestion());
            session.setSessionTitle(newTitle);
            agentSessionMapper.updateTitle(session.getId(), newTitle);
        }

        updateUserPreference(user.getUserId(), request.getQuestion(), preference);
        operationLogService.log("AGENT", "CHAT", session.getId(), "chat in " + sourceScene);
        recordAiEvent("RESULT_FLOW", "INFO", "RESULT_FLOW_READY", "Result flow can now be consumed by ledger, monthly report and logs.",
                "If front-end still cannot see data, re-check sourceScene filters.", null,
                aiData(
                        "sessionId", session.getId(),
                        "sourceScene", sourceScene,
                        "questionType", questionAnalysis.getQuestionType(),
                        "retrievalStrategy", retrievalResult.getRetrievalStrategy(),
                        "topChunksType", retrievalResult.getTopChunkTypes(),
                        "needFollowup", followupTriggered,
                        "fallbackTriggered", knowledgeFallbackUsed,
                        "skillId", session.getSkillId(),
                        "baseId", session.getBaseId(),
                        "hasKnowledgeHit", !context.getChunks().isEmpty()
                ));

        AgentChatResultVO result = new AgentChatResultVO();
        com.example.lecturesystem.modules.skill.entity.SkillEntity skillEntity = skillMapper.findById(session.getSkillId());
        result.setSessionId(session.getId());
        result.setSkillId(session.getSkillId());
        result.setSkillName(normalizeText(skillEntity == null ? null : skillEntity.getSkillName()));
        result.setSourceScene(sourceScene);
        result.setSkillMatchMode("SESSION_REUSE");
        result.setAnswer(answer);
        result.setCitedChunkIds(context.toChunkIds());
        result.setCitedChunkIdList(context.toChunkIds().isEmpty() ? List.of() : Arrays.asList(context.toChunkIds().split(",")));
        result.setCitedTitles(context.getChunks().stream().map(KnowledgeSearchResultVO::getDocTitle).distinct().collect(Collectors.toList()));
        return result;
    }
                    answer = buildFallbackAnswer(request.getQuestion(), sourceScene, context, preference, questionAnalysis, retrievalResult, eligibilityMatch,
                            FALLBACK_REASON_AI_UNAVAILABLE + " 鍘熷洜锛? + normalizeText(ex.getMessage()));
                }
            }
        }

        if (providerResolution != null && knowledgeFallbackUsed) {
            recordAiEvent("AI_CHAT", "WARN", "鐭ヨ瘑搴撳厹搴曟垚鍔?, "AI Provider 澶辫触鍚庡凡鍒囨崲涓虹煡璇嗗簱鍏滃簳鍥炵瓟銆?,
                    "鑻ュ悗缁粛闇€浼樺寲棣栫瓟浣撻獙锛岃浼樺厛妫€鏌?Provider 瓒呮椂銆並nowledge 涓婁笅鏂囬暱搴﹀拰妯″瀷閫夋嫨銆?, null,
                    aiData(
                            "sessionId", session.getId(),
                            "sourceScene", sourceScene,
                            "questionType", questionAnalysis.getQuestionType(),
                            "providerId", providerResolution.provider().getId(),
                            "providerName", providerResolution.provider().getProviderName(),
                            "modelCode", providerResolution.modelCode(),
                            "durationMs", providerCallElapsedMs,
                            "knowledgeFallback", true,
                            "fallbackTriggered", true,
                            "topChunksType", retrievalResult.getTopChunkTypes(),
                            "knowledgeHitCount", context.getChunks().size(),
                            "citedChunkIds", context.toChunkIds()
                    ));
        }

        AgentMessageEntity assistantMessage = new AgentMessageEntity();
        assistantMessage.setSessionId(session.getId());
        assistantMessage.setMessageRole("assistant");
        assistantMessage.setMessageText(answer);
        assistantMessage.setCitedChunkIds(context.toChunkIds());
        assistantMessage.setCreateTime(LocalDateTime.now());
        try {
            agentMessageMapper.insert(assistantMessage);
        } catch (Exception ex) {
            recordAiEvent("AI_CHAT", "ERROR", "AI鍥炲钀藉簱澶辫触", "AI 鍥炲鍐欏叆 ai_agent_message 澶辫触銆?,
                    "璇锋鏌ユ秷鎭〃缁撴瀯銆佷簨鍔＄姸鎬佸拰鏁版嵁搴撹繛鎺ャ€?, "AI_MESSAGE_SAVE_FAILED",
                    aiData(
                            "sessionId", session.getId(),
                            "messageRole", "assistant",
                            "sourceScene", sourceScene,
                            "errorMessage", normalizeText(ex.getMessage())
                    ));
            throw ex;
        }
        recordAiEvent("AI_CHAT", "INFO", "AI鍥炲钀藉簱鎴愬姛", "AI 鍥炲宸插啓鍏?ai_agent_message銆?,
                "鑻ュ彴璐︽垨鏈堟姤浠嶆煡涓嶅埌鏁版嵁锛岃缁х画妫€鏌ユ潵婧愬満鏅拰鎵挎帴鏃ュ織銆?, null,
                aiData(
                        "sessionId", session.getId(),
                        "messageRole", "assistant",
                        "sourceScene", sourceScene,
                        "questionType", questionAnalysis.getQuestionType(),
                        "needFollowup", followupTriggered,
                        "fallbackTriggered", knowledgeFallbackUsed,
                        "citedChunkIds", context.toChunkIds(),
                        "answerLength", answer == null ? 0 : answer.length()
                ));

        if (DEFAULT_SESSION_TITLE.equals(session.getSessionTitle())) {
            String newTitle = buildSessionTitle(request.getQuestion());
            session.setSessionTitle(newTitle);
            agentSessionMapper.updateTitle(session.getId(), newTitle);
        }

        updateUserPreference(user.getUserId(), request.getQuestion(), preference);
        operationLogService.log("AGENT", "CHAT", session.getId(), "chat in " + sourceScene);
        recordAiEvent("RESULT_FLOW", "INFO", "AI缁撴灉鍥炴祦鎵挎帴鎴愬姛", "鏈 AI 浼氳瘽宸插叿澶囧彴璐︺€佹湀鎶ュ拰鏃ュ織鎵挎帴鏉′欢銆?,
                "濡傚墠绔粛鐪嬩笉鍒版暟鎹紝璇风粨鍚堟潵婧愬満鏅繃婊ゆ潯浠跺鏌ャ€?, null,
                aiData(
                        "sessionId", session.getId(),
                        "sourceScene", sourceScene,
                        "questionType", questionAnalysis.getQuestionType(),
                        "retrievalStrategy", retrievalResult.getRetrievalStrategy(),
                        "topChunksType", retrievalResult.getTopChunkTypes(),
                        "needFollowup", followupTriggered,
                        "fallbackTriggered", knowledgeFallbackUsed,
                        "skillId", session.getSkillId(),
                        "baseId", session.getBaseId(),
                        "hasKnowledgeHit", !context.getChunks().isEmpty()
                ));

        AgentChatResultVO result = new AgentChatResultVO();
        com.example.lecturesystem.modules.skill.entity.SkillEntity skillEntity = skillMapper.findById(session.getSkillId());
        result.setSessionId(session.getId());
        result.setSkillId(session.getSkillId());
        result.setSkillName(normalizeText(skillEntity == null ? null : skillEntity.getSkillName()));
        result.setSourceScene(sourceScene);
        result.setSkillMatchMode("SESSION_REUSE");
        result.setAnswer(answer);
        result.setCitedChunkIds(context.toChunkIds());
        result.setCitedChunkIdList(context.toChunkIds().isEmpty() ? List.of() : Arrays.asList(context.toChunkIds().split(",")));
        result.setCitedTitles(context.getChunks().stream().map(KnowledgeSearchResultVO::getDocTitle).distinct().collect(Collectors.toList()));
        return result;
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
        clone.setSourceScene(request.getSourceScene());
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

    private SkillSelection resolveSkillSelection(LoginUser user, CreateAgentSessionRequest request) {
        Long explicitSkillId = request == null ? null : request.getSkillId();
        if (explicitSkillId != null) {
            requireSkillUse(user, explicitSkillId);
            com.example.lecturesystem.modules.skill.entity.SkillEntity skillEntity = skillMapper.findById(explicitSkillId);
            return new SkillSelection(explicitSkillId, normalizeText(skillEntity == null ? null : skillEntity.getSkillName()), "EXPLICIT_ID");
        }

        List<SkillListItemVO> candidates = queryUsablePublishedSkills(user);
        if (candidates.isEmpty()) {
            recordAiEvent("SESSION_CREATE", "WARN", "Skill 鍖归厤澶辫触", "褰撳墠璐﹀彿娌℃湁鍙敤鐨勫凡鍙戝竷 Skill銆?,
                    "璇峰厛鍦?AI 鏉冮檺閰嶇疆涓紑閫?Skill 浣跨敤鏉冮檺锛屽苟纭 Skill 宸插彂甯冦€?, "AI_SKILL_UNAVAILABLE",
                    aiData("userId", user.getUserId(), "sourceScene", request == null ? null : request.getSourceScene()));
            throw new IllegalArgumentException("no published skill is available");
        }

        String skillHint = normalizeText(request == null ? null : request.getSkillHint());
        if (skillHint != null) {
            for (SkillListItemVO item : candidates) {
                if (matchesSkillHint(item, skillHint)) {
                    return new SkillSelection(item.getId(), normalizeText(item.getSkillName()), "EXPLICIT_HINT");
                }
            }
        }

        String sourceScene = normalizeSourceScene(request == null ? null : request.getSourceScene());
        if (SOURCE_SCENE_MOBILE_POLICY_CONSULTANT.equals(sourceScene)) {
            for (SkillListItemVO item : candidates) {
                if (DEFAULT_POLICY_SKILL_CODE.equalsIgnoreCase(normalizeText(item.getSkillCode()))) {
                    return new SkillSelection(item.getId(), normalizeText(item.getSkillName()), "DEFAULT_POLICY_SKILL");
                }
            }
        }

        String question = normalizeText(request == null ? null : request.getQuestion());
        if (question != null) {
            String normalizedQuestion = question.toLowerCase();
            for (SkillListItemVO item : candidates) {
                String skillCode = normalizeText(item.getSkillCode());
                String skillName = normalizeText(item.getSkillName());
                String domainType = normalizeText(item.getDomainType());
                if ((skillCode != null && normalizedQuestion.contains(skillCode.toLowerCase()))
                        || (skillName != null && normalizedQuestion.contains(skillName.toLowerCase()))
                        || (domainType != null && normalizedQuestion.contains(domainType.toLowerCase()))) {
                    return new SkillSelection(item.getId(), normalizeText(item.getSkillName()), "AUTO_MATCH");
                }
            }
        }

        SkillListItemVO fallback = candidates.get(0);
        return new SkillSelection(fallback.getId(), normalizeText(fallback.getSkillName()), "FALLBACK_FIRST_PUBLISHED");
    }

    private List<SkillListItemVO> queryUsablePublishedSkills(LoginUser user) {
        SkillQueryRequest request = new SkillQueryRequest();
        request.setStatus(1);
        request.setPublishStatus("PUBLISHED");
        List<SkillListItemVO> publishedSkills = skillMapper.queryPublishedList(request);
        if (publishedSkills == null || publishedSkills.isEmpty()) {
            return List.of();
        }
        if (permissionService.isSuperAdmin(user.getUserId())) {
            return publishedSkills;
        }
        List<SkillListItemVO> usableSkills = new ArrayList<>();
        for (SkillListItemVO item : publishedSkills) {
            if (item.getId() != null && aiPermissionService.canUseSkill(user.getUserId(), item.getId())) {
                usableSkills.add(item);
            }
        }
        return usableSkills;
    }

    private boolean matchesSkillHint(SkillListItemVO item, String skillHint) {
        String normalizedHint = normalizeText(skillHint);
        if (normalizedHint == null || item == null) {
            return false;
        }
        String normalizedCode = normalizeText(item.getSkillCode());
        String normalizedName = normalizeText(item.getSkillName());
        String comparableHint = normalizedHint.replace("@", "");
        return comparableHint.equalsIgnoreCase(valueOrBlank(normalizedCode))
                || comparableHint.equalsIgnoreCase(valueOrBlank(normalizedName));
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
    private String resolveRegionPriority(String sourceScene, String question) {
        if (!SOURCE_SCENE_MOBILE_POLICY_CONSULTANT.equalsIgnoreCase(normalizeSourceScene(sourceScene))) {
            return null;
        }
        String normalized = normalizeText(question);
        if (normalized == null) {
            return null;
        }
        if (normalized.contains("\u53a6\u95e8\u5e02") || normalized.contains("\u53a6\u95e8")) {
            return "\u53a6\u95e8\u5e02";
        }
        if (normalized.contains("\u798f\u5efa\u7701") || normalized.contains("\u798f\u5efa")) {
            return "\u798f\u5efa\u7701";
        }
        return null;
    }

    private boolean shouldWarnProvinceReferenceOnly(String sourceScene, String question, KnowledgeCitationContext context) {
        if (!"\u53a6\u95e8\u5e02".equals(resolveRegionPriority(sourceScene, question)) || context == null || context.getChunks().isEmpty()) {
            return false;
        }
        int limit = Math.min(context.getChunks().size(), 3);
        int xiamenCount = 0;
        int provinceCount = 0;
        for (int i = 0; i < limit; i++) {
            KnowledgeSearchResultVO item = context.getChunks().get(i);
            if (containsRegionSignal(item, "\u53a6\u95e8\u5e02", "\u53a6\u95e8")) {
                xiamenCount++;
                continue;
            }
            if (containsRegionSignal(item, "\u798f\u5efa\u7701", "\u798f\u5efa")) {
                provinceCount++;
            }
        }
        return provinceCount > 0 && xiamenCount < provinceCount;
    }

    private boolean containsRegionSignal(KnowledgeSearchResultVO item, String... regionTerms) {
        if (item == null) {
            return false;
        }
        return containsAny(item.getHeadingPath(), regionTerms)
                || containsAny(item.getSnippet(), regionTerms)
                || containsAny(item.getDocTitle(), regionTerms);
    }

    private boolean containsAny(String text, String... terms) {
        String normalized = normalizeText(text);
        if (normalized == null || terms == null) {
            return false;
        }
        for (String term : terms) {
            if (term != null && normalized.contains(term)) {
                return true;
            }
        }
        return false;
    }

    private void requireAgentPermission(LoginUser user) {
        if (!permissionService.isSuperAdmin(user.getUserId())
                && !aiPermissionService.canUseAgent(user.getUserId())) {
            recordAiEvent("AI_PERMISSION", "WARN", "AGENT_PERMISSION_FORBIDDEN", "Current user cannot use AI workbench.",
                    "Check canUseAgent permission.", "AI_AGENT_FORBIDDEN",
                    aiData("userId", user.getUserId()));
            throw new IllegalArgumentException("current user cannot use AI workbench");
        }
    }

    private void requireSkillUse(LoginUser user, Long skillId) {
        if (skillId == null) {
            recordAiEvent("AI_PERMISSION", "WARN", "SKILL_REQUIRED", "Skill is required for the current request.",
                    "Check session binding or explicit @skill routing.", "AI_SKILL_REQUIRED",
                    aiData("userId", user.getUserId()));
            throw new IllegalArgumentException("skill is required");
        }
        if (!permissionService.isSuperAdmin(user.getUserId()) && !aiPermissionService.canUseSkill(user.getUserId(), skillId)) {
            recordAiEvent("AI_PERMISSION", "WARN", "SKILL_FORBIDDEN", "Current user cannot use this skill.",
                    "Check user-skill authorization.", "AI_SKILL_FORBIDDEN",
                    aiData("userId", user.getUserId(), "skillId", skillId));
            throw new IllegalArgumentException("current user cannot use this skill");
        }
    }

    private void requireKnowledgeAnalyze(LoginUser user, Long baseId) {
        if (baseId == null) {
            recordAiEvent("AI_PERMISSION", "WARN", "KNOWLEDGE_BASE_REQUIRED", "Knowledge base is missing for the current skill.",
                    "Bind a knowledge base to the published skill version.", "AI_BASE_REQUIRED",
                    aiData("userId", user.getUserId()));
            throw new IllegalArgumentException("knowledge base is required");
        }
        if (!permissionService.isSuperAdmin(user.getUserId()) && !aiPermissionService.canAnalyzeKnowledgeBase(user.getUserId(), baseId)) {
            recordAiEvent("AI_PERMISSION", "WARN", "KNOWLEDGE_BASE_FORBIDDEN", "Current user cannot analyze this knowledge base.",
                    "Check knowledge-base authorization.", "AI_BASE_FORBIDDEN",
                    aiData("userId", user.getUserId(), "baseId", baseId));
            throw new IllegalArgumentException("current user cannot analyze this knowledge base");
        }
    }
        int limit = Math.min(context.getChunks().size(), 3);
        int xiamenCount = 0;
        int provinceCount = 0;
        for (int i = 0; i < limit; i++) {
            KnowledgeSearchResultVO item = context.getChunks().get(i);
            if (containsRegionSignal(item, "鍘﹂棬甯?, "鍘﹂棬")) {
                xiamenCount++;
                continue;
            }
            if (containsRegionSignal(item, "绂忓缓鐪?, "绂忓缓")) {
                provinceCount++;
            }
        }
        return provinceCount > 0 && xiamenCount < provinceCount;
    }

    private boolean containsRegionSignal(KnowledgeSearchResultVO item, String... regionTerms) {
        if (item == null) {
            return false;
        }
        return containsAny(item.getHeadingPath(), regionTerms)
                || containsAny(item.getSnippet(), regionTerms)
                || containsAny(item.getDocTitle(), regionTerms);
    }

    private boolean containsAny(String text, String... terms) {
        String normalized = normalizeText(text);
        if (normalized == null || terms == null) {
            return false;
        }
        for (String term : terms) {
            if (term != null && normalized.contains(term)) {
                return true;
            }
        }
        return false;
    }

    private void requireAgentPermission(LoginUser user) {
        if (!permissionService.isSuperAdmin(user.getUserId())
                && !aiPermissionService.canUseAgent(user.getUserId())) {
            recordAiEvent("AI_PERMISSION", "WARN", "AI鏉冮檺鏍￠獙澶辫触", "褰撳墠鐢ㄦ埛娌℃湁 AI 宸ヤ綔鍙颁娇鐢ㄦ潈闄愩€?,
                    "璇锋鏌?AI 鏉冮檺閰嶇疆涓殑 canUseAgent 閰嶇疆銆?, "AI_AGENT_FORBIDDEN",
                    aiData("userId", user.getUserId()));
            throw new IllegalArgumentException("current user cannot use AI workbench");
        }
    }

    private void requireSkillUse(LoginUser user, Long skillId) {
        if (skillId == null) {
            recordAiEvent("AI_PERMISSION", "WARN", "Skill鏍￠獙澶辫触", "褰撳墠璇锋眰缂哄皯 Skill 淇℃伅銆?,
                    "璇风‘璁や細璇濇垨鏄惧紡 @skill 鏄惁宸叉纭懡涓?Skill銆?, "AI_SKILL_REQUIRED",
                    aiData("userId", user.getUserId()));
            throw new IllegalArgumentException("skill is required");
        }
        if (!permissionService.isSuperAdmin(user.getUserId()) && !aiPermissionService.canUseSkill(user.getUserId(), skillId)) {
            recordAiEvent("AI_PERMISSION", "WARN", "Skill鏉冮檺鏍￠獙澶辫触", "褰撳墠鐢ㄦ埛娌℃湁璇?Skill 鐨勪娇鐢ㄦ潈闄愩€?,
                    "璇锋鏌?AI 鏉冮檺閰嶇疆涓殑 user-skill 鎺堟潈璁板綍銆?, "AI_SKILL_FORBIDDEN",
                    aiData("userId", user.getUserId(), "skillId", skillId));
            throw new IllegalArgumentException("current user cannot use this skill");
        }
    }

    private void requireKnowledgeAnalyze(LoginUser user, Long baseId) {
        if (baseId == null) {
            recordAiEvent("AI_PERMISSION", "WARN", "鐭ヨ瘑搴撴牎楠屽け璐?, "褰撳墠 Skill 鏈粦瀹氱煡璇嗗簱锛岀己灏?baseId銆?,
                    "璇峰湪 Skills 涓績涓哄綋鍓嶇増鏈粦瀹氱煡璇嗗簱銆?, "AI_BASE_REQUIRED",
                    aiData("userId", user.getUserId()));
            throw new IllegalArgumentException("knowledge base is required");
        }
        if (!permissionService.isSuperAdmin(user.getUserId()) && !aiPermissionService.canAnalyzeKnowledgeBase(user.getUserId(), baseId)) {
            recordAiEvent("AI_PERMISSION", "WARN", "鐭ヨ瘑搴撴潈闄愭牎楠屽け璐?, "褰撳墠鐢ㄦ埛娌℃湁璇ョ煡璇嗗簱鐨勫垎鏋愭潈闄愩€?,
                    "璇锋鏌?AI 鏉冮檺閰嶇疆涓殑 knowledge 鎺堟潈璁板綍銆?, "AI_BASE_FORBIDDEN",
                    aiData("userId", user.getUserId(), "baseId", baseId));
            throw new IllegalArgumentException("current user cannot analyze this knowledge base");
        }
    }

    private String buildSystemPrompt(SkillVersionEntity version, AgentUserPreferenceEntity preference) {
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
        if (version.getOutputTemplate() != null) {
    private String buildFollowupQuestion(PolicyQuestionClassifier.Analysis questionAnalysis) {
        if (questionAnalysis == null || questionAnalysis.getMissingFields().isEmpty()) {
            return "To make a reliable eligibility judgment, I still need a few key fields before continuing.";
        }
        List<String> prompts = new ArrayList<>();
        for (String field : questionAnalysis.getMissingFields()) {
            if ("region".equals(field)) {
                prompts.add("which policy region you are asking about (Xiamen or Fujian)");
            } else if ("workStatus".equals(field)) {
                prompts.add("whether you already work in Xiamen or plan to come to Xiamen");
            } else if ("industryOrTheme".equals(field)) {
                prompts.add("your industry, enterprise type, or the exact consultation theme");
            }
        }
        StringBuilder builder = new StringBuilder("For an accurate answer, please provide: ");
        for (int index = 0; index < prompts.size(); index++) {
            builder.append(index + 1).append(") ").append(prompts.get(index));
            if (index + 1 < prompts.size()) {
                builder.append("; ");
            }
        }
        builder.append(".");
        return builder.toString();
    }

    private String buildFallbackAnswer(String question,
                                       String sourceScene,
                                       KnowledgeCitationContext context,
                                       AgentUserPreferenceEntity preference,
                                       PolicyQuestionClassifier.Analysis questionAnalysis,
                                       PolicyKnowledgeRetrievalTool.Result retrievalResult,
                                       PolicyEligibilityMatchTool.Result eligibilityMatch,
                                       String fallbackReason) {
        StringBuilder builder = new StringBuilder();
        builder.append("The system has switched to knowledge fallback mode.\\n");
        if (preference != null && normalizeText(preference.getHabitSummary()) != null) {
            builder.append("User habit summary: ").append(preference.getHabitSummary().trim()).append("\\n");
        }
        if (normalizeText(fallbackReason) != null) {
            builder.append(fallbackReason.trim()).append("\\n\\n");
        } else {
            builder.append("\\n");
        }
        if (shouldWarnProvinceReferenceOnly(sourceScene, question, context)) {
            builder.append("Note: the direct hits are mainly province-level materials. They can be used only as upper-level references and do not equal the current Xiamen policy list.\\n\\n");
        }

        if (context == null || context.getChunks().isEmpty()) {
            builder.append("No direct policy clauses were hit, so a reliable conclusion cannot be given yet.\\n")
                    .append("Please provide the policy region, talent type, education level, industry, or the exact policy/program name.\\n");
            if (normalizeText(question) != null) {
                builder.append("Original question: ").append(question.trim()).append("\\n");
            }
            return builder.toString();
        }

        builder.append("Based on the currently matched policy chunks, here is a cautious summary:\\n");
        if (eligibilityMatch != null) {
            builder.append("Satisfied conditions: ").append(eligibilityMatch.getSatisfiedConditions()).append("\\n");
            if (!eligibilityMatch.getMissingConditions().isEmpty()) {
                builder.append("Missing conditions: ").append(eligibilityMatch.getMissingConditions()).append("\\n");
            }
            if (!eligibilityMatch.getReferenceOnlyNotes().isEmpty()) {
                builder.append("Reference-only notes: ").append(eligibilityMatch.getReferenceOnlyNotes()).append("\\n");
            }
        }
        int limit = Math.min(context.getChunks().size(), 3);
        for (int i = 0; i < limit; i++) {
            KnowledgeSearchResultVO item = context.getChunks().get(i);
            builder.append(i + 1).append(". ").append(valueOrDefault(item.getDocTitle(), "Untitled policy material"));
            if (normalizeText(item.getHeadingPath()) != null) {
                builder.append(" / ").append(item.getHeadingPath().trim());
            }
            if (normalizeText(item.getPolicyRegion()) != null) {
                builder.append(" / ").append(item.getPolicyRegion().trim());
            }
            builder.append("\\n   ").append(valueOrDefault(abbreviate(item.getSnippet(), 160), "No snippet available for this chunk.")).append("\\n");
        }

        builder.append("\\nSuggestions:\\n")
                .append("1. For eligibility questions, continue to provide key fields such as region, industry and work status.\\n")
                .append("2. For detail questions, provide the exact policy or program name whenever possible.\\n")
                .append("3. The current answer is based on matched knowledge chunks and does not replace formal recognition or authority review.\\n");

        LinkedHashSet<String> citations = new LinkedHashSet<>();
        for (KnowledgeSearchResultVO item : context.getChunks()) {
            String title = valueOrDefault(item.getDocTitle(), "Untitled policy material");
            String chunkLabel = item.getChunkId() == null ? "-" : String.valueOf(item.getChunkId());
            citations.add("- " + title + " (chunk " + chunkLabel + ")");
            if (citations.size() >= 5) {
                break;
            }
        }
        if (!citations.isEmpty()) {
            builder.append("\\nCitations:\\n");
            citations.forEach(line -> builder.append(line).append("\\n"));
        }
        return builder.toString();
    }

    private void updateUserPreference(Long userId, String question, AgentUserPreferenceEntity existed) {
        if (userId == null || normalizeText(question) == null) {
            return;
        }
        AgentUserPreferenceEntity entity = existed == null ? new AgentUserPreferenceEntity() : existed;
        entity.setUserId(userId);
        entity.setPreferredAnswerStyle(detectPreferredAnswerStyle(question, existed));
        entity.setRecentTopics(mergeRecentTopics(existed == null ? null : existed.getRecentTopics(), question));
        entity.setLastQuestion(abbreviate(question, 500));
        entity.setHabitSummary(buildHabitSummary(entity));
        entity.setUpdateTime(LocalDateTime.now());
        agentUserPreferenceMapper.upsert(entity);
    }

    private String detectPreferredAnswerStyle(String question, AgentUserPreferenceEntity existed) {
        String normalized = normalizeText(question);
        if (normalized == null) {
            return existed == null ? "PROFESSIONAL" : valueOrDefault(existed.getPreferredAnswerStyle(), "PROFESSIONAL");
        }
        if (normalized.contains("brief") || normalized.contains("concise") || normalized.contains("direct")) {
            return "CONCISE";
        }
        if (normalized.contains("detail") || normalized.contains("detailed") || normalized.contains("full")) {
            return "DETAILED";
        }
        if (normalized.contains("step") || normalized.contains("steps")) {
            return "STEP_BY_STEP";
        }
        if (normalized.contains("citation") || normalized.contains("source") || normalized.contains("reference")) {
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
            builder.append("宸叉弧瓒虫潯浠讹細").append(eligibilityMatch.getSatisfiedConditions()).append("\n");
            if (!eligibilityMatch.getMissingConditions().isEmpty()) {
                builder.append("浠嶇己灏戜俊鎭細").append(eligibilityMatch.getMissingConditions()).append("\n");
            }
            if (!eligibilityMatch.getReferenceOnlyNotes().isEmpty()) {
                builder.append("浠呭彲鍙傝€冿細").append(eligibilityMatch.getReferenceOnlyNotes()).append("\n");
    private String buildHabitSummary(AgentUserPreferenceEntity entity) {
        List<String> parts = new ArrayList<>();
        if (normalizeText(entity.getPreferredAnswerStyle()) != null) {
            parts.add("prefers " + entity.getPreferredAnswerStyle() + " style");
        }
        if (normalizeText(entity.getRecentTopics()) != null) {
            parts.add("recent topics " + entity.getRecentTopics().replace(",", " / "));
        }
        return String.join("; ", parts);
    }
                builder.append(" / ").append(item.getPolicyRegion().trim());
            }
            builder.append("\n   ")
                    .append(valueOrDefault(abbreviate(item.getSnippet(), 160), "褰撳墠鐗囨鏆傛棤鍙睍绀烘鏂囥€?))
                    .append("\n");
        }

        builder.append("\n寤鸿锛歕n")
                .append("1. 鑻ユ偍瑕佸仛璧勬牸鍒ゆ柇锛岃缁х画琛ュ厖鍦板尯銆佽涓氥€佸伐浣滅姸鎬佺瓑鍏抽敭鏉′欢锛沑n")
                .append("2. 鑻ユ偍瑕佹煡涓撻」缁嗗垯锛岃灏介噺鎻愪緵鏄庣‘鏀跨瓥鍚嶇О鎴栭」鐩悕锛沑n")
                .append("3. 褰撳墠缁撴灉鍩轰簬宸插懡涓殑鐭ヨ瘑鐗囨鏁寸悊锛屼笉鏇夸唬姝ｅ紡璁ゅ畾銆佺敵鎶ュ鏍告垨涓荤閮ㄩ棬鍙ｅ緞銆俓n");

        LinkedHashSet<String> citations = new LinkedHashSet<>();
        for (KnowledgeSearchResultVO item : context.getChunks()) {
            String title = valueOrDefault(item.getDocTitle(), "鏈懡鍚嶆斂绛栬祫鏂?);
            String chunkLabel = item.getChunkId() == null ? "-" : String.valueOf(item.getChunkId());
            citations.add("- " + title + "锛坈hunk " + chunkLabel + "锛?);
            if (citations.size() >= 5) {
                break;
            }
        }
        if (!citations.isEmpty()) {
            builder.append("\n鏀跨瓥渚濇嵁锛歕n");
            citations.forEach(line -> builder.append(line).append("\n"));
        }
        return builder.toString();
    }

    private void updateUserPreference(Long userId, String question, AgentUserPreferenceEntity existed) {
        if (userId == null || normalizeText(question) == null) {
            return;
        }
        AgentUserPreferenceEntity entity = existed == null ? new AgentUserPreferenceEntity() : existed;
        entity.setUserId(userId);
        entity.setPreferredAnswerStyle(detectPreferredAnswerStyle(question, existed));
        entity.setRecentTopics(mergeRecentTopics(existed == null ? null : existed.getRecentTopics(), question));
        entity.setLastQuestion(abbreviate(question, 500));
        entity.setHabitSummary(buildHabitSummary(entity));
        entity.setUpdateTime(LocalDateTime.now());
        agentUserPreferenceMapper.upsert(entity);
    }

    private String detectPreferredAnswerStyle(String question, AgentUserPreferenceEntity existed) {
        String normalized = normalizeText(question);
        if (normalized == null) {
            return existed == null ? "PROFESSIONAL" : valueOrDefault(existed.getPreferredAnswerStyle(), "PROFESSIONAL");
        }
        if (normalized.contains("绠€鐭?) || normalized.contains("绠€娲?) || normalized.contains("鐩存帴")) {
            return "CONCISE";
        }
        if (normalized.contains("璇︾粏") || normalized.contains("鍏ㄩ潰") || normalized.contains("灞曞紑") || normalized.contains("鍏蜂綋")) {
            return "DETAILED";
        }
        if (normalized.contains("姝ラ") || normalized.contains("涓€姝ユ") || normalized.contains("鍒嗘")) {
            return "STEP_BY_STEP";
        }
        if (normalized.contains("渚濇嵁") || normalized.contains("鍑哄") || normalized.contains("寮曠敤")) {
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
    private List<String> extractSearchCandidates(String question) {
        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        String normalized = normalizeText(question);
        if (normalized == null) {
            return List.of();
        }
        candidates.add(normalized);

        String simplified = normalized
                .replace('\uFF1F', ' ')
                .replace('?', ' ')
                .replace('\uFF0C', ' ')
                .replace(',', ' ')
                .replace('\u3002', ' ')
                .replace('\uFF1B', ' ')
                .replace(';', ' ')
                .replace('\uFF1A', ' ')
                .replace(':', ' ')
                .replace('\u3001', ' ');
        for (String stop : SEARCH_STOP_TERMS) {
            simplified = simplified.replace(stop, " ");
        }
        simplified = simplified.replaceAll("\\\\s+", " ").trim();
        if (!simplified.isEmpty()) {
            candidates.add(simplified);
            candidates.add(simplified.replace(" ", ""));
        }

        for (String term : SEARCH_HINT_TERMS) {
            if (normalized.contains(term)) {
                candidates.add(term);
            }
        }

        if (normalized.contains("\u53a6\u95e8") && normalized.contains("\u4eba\u624d") && normalized.contains("\u653f\u7b56")) {
            candidates.add("\u53a6\u95e8\u5e02\u4eba\u624d\u653f\u7b56");
            candidates.add("\u53a6\u95e8\u4eba\u624d\u653f\u7b56");
        }
        if (normalized.contains("\u798f\u5efa") && normalized.contains("\u4eba\u624d") && normalized.contains("\u653f\u7b56")) {
            candidates.add("\u798f\u5efa\u7701\u4eba\u624d\u653f\u7b56");
            candidates.add("\u798f\u5efa\u4eba\u624d\u653f\u7b56");
        }
        return new ArrayList<>(candidates);
    }
        }
        return root == null ? null : root.getClass().getSimpleName();
    }

    private boolean isTimeoutLike(Throwable throwable) {
        Throwable root = throwable;
        while (root != null) {
            String message = normalizeText(root.getMessage());
            if (root instanceof java.net.http.HttpTimeoutException
                    || (message != null && message.toLowerCase().contains("timed out"))) {
                return true;
            }
            root = root.getCause();
        }
        return false;
    }

    private String normalizeSourceScene(String sourceScene) {
        String normalized = normalizeText(sourceScene);
        if (normalized == null) {
            return SOURCE_SCENE_AI_WORKBENCH;
        }
        String value = normalized.trim().toUpperCase().replace('-', '_').replace(' ', '_');
        return switch (value) {
            case "MOBILE_POLICY_CONSULTANT", "POLICY_CONSULTANT" -> SOURCE_SCENE_MOBILE_POLICY_CONSULTANT;
            default -> SOURCE_SCENE_AI_WORKBENCH;
        };
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

    private String normalizeText(String text) {
        if (text == null) {
            return null;
        }
        String value = text.trim();
        return value.isEmpty() ? null : value;
    }

    private List<String> extractSearchCandidates(String question) {
        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        String normalized = normalizeText(question);
        if (normalized == null) {
            return List.of();
        }
        candidates.add(normalized);

        String simplified = normalized
                .replace('锛?, ' ')
                .replace('?', ' ')
                .replace('锛?, ' ')
                .replace(',', ' ')
                .replace('銆?, ' ')
                .replace('锛?, ' ')
                .replace(';', ' ')
                .replace('锛?, ' ')
                .replace(':', ' ')
                .replace('銆?, ' ');
        for (String stop : SEARCH_STOP_TERMS) {
            simplified = simplified.replace(stop, " ");
        }
        simplified = simplified.replaceAll("\\s+", " ").trim();
        if (!simplified.isEmpty()) {
            candidates.add(simplified);
            candidates.add(simplified.replace(" ", ""));
        }

        for (String term : SEARCH_HINT_TERMS) {
            if (normalized.contains(term)) {
                candidates.add(term);
            }
        }

        if (normalized.contains("鍘﹂棬") && normalized.contains("浜烘墠") && normalized.contains("鏀跨瓥")) {
            candidates.add("鍘﹂棬甯備汉鎵嶆斂绛?);
            candidates.add("鍘﹂棬浜烘墠鏀跨瓥");
        }
        if (normalized.contains("绂忓缓") && normalized.contains("浜烘墠") && normalized.contains("鏀跨瓥")) {
            candidates.add("绂忓缓鐪佷汉鎵嶆斂绛?);
            candidates.add("绂忓缓浜烘墠鏀跨瓥");
        }
        return new ArrayList<>(candidates);
    }

    private String abbreviate(String text, int maxLength) {
        String normalized = normalizeText(text);
        if (normalized == null || maxLength <= 0 || normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength) + "...";
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
                + ", sourceScene=" + valueOrBlank(safeRequest.getSourceScene())
                + ", skillId=" + valueOrBlank(safeRequest.getSkillId())
                + ", status=" + valueOrBlank(safeRequest.getStatus());
    }

    private record SkillSelection(Long skillId, String skillName, String matchMode) {
    }

    private record ProviderResolution(ProviderConfigEntity provider, String modelCode) {
    }
}


