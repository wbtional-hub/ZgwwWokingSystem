package com.example.lecturesystem.modules.agent.service.impl;

import com.example.lecturesystem.modules.agent.dto.AgentChatRequest;
import com.example.lecturesystem.modules.agent.dto.AgentSessionQueryRequest;
import com.example.lecturesystem.modules.agent.dto.CreateAgentSessionRequest;
import com.example.lecturesystem.modules.agent.dto.UpdateAgentSessionStatusRequest;
import com.example.lecturesystem.modules.agent.entity.AgentMessageEntity;
import com.example.lecturesystem.modules.agent.entity.AgentSessionEntity;
import com.example.lecturesystem.modules.agent.mapper.AgentMessageMapper;
import com.example.lecturesystem.modules.agent.mapper.AgentSessionMapper;
import com.example.lecturesystem.modules.agent.service.AgentService;
import com.example.lecturesystem.modules.agent.support.KnowledgeCitationContext;
import com.example.lecturesystem.modules.agent.support.OpenAiCompatibleChatClient;
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
import com.example.lecturesystem.modules.operationlog.service.OperationLogService;
import com.example.lecturesystem.modules.permission.service.PermissionService;
import com.example.lecturesystem.modules.permission.support.CurrentUserFacade;
import com.example.lecturesystem.modules.skill.entity.SkillVersionEntity;
import com.example.lecturesystem.modules.skill.mapper.SkillVersionMapper;
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
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AgentServiceImpl implements AgentService {
    private static final String DEFAULT_SESSION_TITLE = "New session";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_ARCHIVED = "ARCHIVED";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AgentSessionMapper agentSessionMapper;
    private final AgentMessageMapper agentMessageMapper;
    private final SkillVersionMapper skillVersionMapper;
    private final ProviderConfigMapper providerConfigMapper;
    private final KnowledgeChunkMapper knowledgeChunkMapper;
    private final AiTokenCipherSupport aiTokenCipherSupport;
    private final OpenAiCompatibleChatClient chatClient;
    private final CurrentUserFacade currentUserFacade;
    private final PermissionService permissionService;
    private final AiPermissionService aiPermissionService;
    private final OperationLogService operationLogService;

    public AgentServiceImpl(AgentSessionMapper agentSessionMapper,
                            AgentMessageMapper agentMessageMapper,
                            SkillVersionMapper skillVersionMapper,
                            ProviderConfigMapper providerConfigMapper,
                            KnowledgeChunkMapper knowledgeChunkMapper,
                            AiTokenCipherSupport aiTokenCipherSupport,
                            OpenAiCompatibleChatClient chatClient,
                            CurrentUserFacade currentUserFacade,
                            PermissionService permissionService,
                            AiPermissionService aiPermissionService,
                            OperationLogService operationLogService) {
        this.agentSessionMapper = agentSessionMapper;
        this.agentMessageMapper = agentMessageMapper;
        this.skillVersionMapper = skillVersionMapper;
        this.providerConfigMapper = providerConfigMapper;
        this.knowledgeChunkMapper = knowledgeChunkMapper;
        this.aiTokenCipherSupport = aiTokenCipherSupport;
        this.chatClient = chatClient;
        this.currentUserFacade = currentUserFacade;
        this.permissionService = permissionService;
        this.aiPermissionService = aiPermissionService;
        this.operationLogService = operationLogService;
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

        AgentSessionEntity entity = new AgentSessionEntity();
        entity.setUserId(user.getUserId());
        entity.setSkillId(request.getSkillId());
        entity.setSkillVersionId(version.getId());
        entity.setProviderConfigId(version.getProviderConfigId());
        entity.setModelCode(version.getModelCode());
        entity.setBaseId(baseId);
        entity.setSessionTitle(DEFAULT_SESSION_TITLE);
        entity.setStatus(STATUS_ACTIVE);
        entity.setCreateTime(LocalDateTime.now());
        agentSessionMapper.insert(entity);
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
        ProviderConfigEntity provider = requireProvider(session.getProviderConfigId());
        String apiToken = aiTokenCipherSupport.decrypt(provider.getApiTokenCipher());
        KnowledgeCitationContext context = buildContext(session.getBaseId(), request.getQuestion(), 5);

        AgentMessageEntity userMessage = new AgentMessageEntity();
        userMessage.setSessionId(session.getId());
        userMessage.setMessageRole("user");
        userMessage.setMessageText(request.getQuestion());
        userMessage.setCreateTime(LocalDateTime.now());
        agentMessageMapper.insert(userMessage);

        String answer = chatClient.chat(
                provider.getApiBaseUrl(),
                apiToken,
                session.getModelCode(),
                buildSystemPrompt(version),
                buildUserPrompt(version, request.getQuestion(), context)
        );

        AgentMessageEntity assistantMessage = new AgentMessageEntity();
        assistantMessage.setSessionId(session.getId());
        assistantMessage.setMessageRole("assistant");
        assistantMessage.setMessageText(answer);
        assistantMessage.setCitedChunkIds(context.toChunkIds());
        assistantMessage.setCreateTime(LocalDateTime.now());
        agentMessageMapper.insert(assistantMessage);

        if (DEFAULT_SESSION_TITLE.equals(session.getSessionTitle())) {
            String newTitle = buildSessionTitle(request.getQuestion());
            session.setSessionTitle(newTitle);
            agentSessionMapper.updateTitle(session.getId(), newTitle);
        }

        operationLogService.log("AGENT", "CHAT", session.getId(), "chat in AI workbench");

        AgentChatResultVO result = new AgentChatResultVO();
        result.setSessionId(session.getId());
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

    private ProviderConfigEntity requireProvider(Long providerId) {
        ProviderConfigEntity entity = providerConfigMapper.findById(providerId);
        if (entity == null) {
            throw new IllegalArgumentException("AI provider not found");
        }
        return entity;
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

    private void requireAgentPermission(LoginUser user) {
        if (!permissionService.isSuperAdmin(user.getUserId())
                && (!aiPermissionService.canUseAi(user.getUserId()) || !aiPermissionService.canUseAgent(user.getUserId()))) {
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
        builder.append("Answer strictly based on the knowledge below and include a citation section at the end.\n\nKnowledge context:\n")
                .append(context.toPromptContext())
                .append("Question:\n")
                .append(question);
        return builder.toString();
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

    private String buildExportLogContent(String type, AgentSessionQueryRequest request, int count) {
        AgentSessionQueryRequest safeRequest = request == null ? new AgentSessionQueryRequest() : request;
        return "export consultation ledger type=" + type
                + ", count=" + count
                + ", startDate=" + valueOrBlank(safeRequest.getStartDate())
                + ", endDate=" + valueOrBlank(safeRequest.getEndDate())
                + ", skillId=" + valueOrBlank(safeRequest.getSkillId())
                + ", status=" + valueOrBlank(safeRequest.getStatus());
    }
}