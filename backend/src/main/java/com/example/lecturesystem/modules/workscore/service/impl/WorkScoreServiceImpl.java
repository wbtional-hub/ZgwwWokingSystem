package com.example.lecturesystem.modules.workscore.service.impl;

import com.example.lecturesystem.modules.auth.security.LoginUser;
import com.example.lecturesystem.modules.operationlog.service.OperationLogService;
import com.example.lecturesystem.modules.permission.support.CurrentUserFacade;
import com.example.lecturesystem.modules.permission.support.DataScopeService;
import com.example.lecturesystem.modules.permission.service.PermissionService;
import com.example.lecturesystem.modules.workscore.dto.CalculateWorkScoreRequest;
import com.example.lecturesystem.modules.workscore.dto.WorkScoreQueryRequest;
import com.example.lecturesystem.modules.workscore.entity.WorkScoreEntity;
import com.example.lecturesystem.modules.workscore.mapper.WorkScoreMapper;
import com.example.lecturesystem.modules.workscore.service.WorkScoreService;
import com.example.lecturesystem.modules.workscore.vo.WorkScoreCandidateVO;
import com.example.lecturesystem.modules.workscore.vo.WorkScoreListItemVO;
import com.example.lecturesystem.modules.user.entity.UserEntity;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.WeekFields;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class WorkScoreServiceImpl implements WorkScoreService {
    private static final BigDecimal ATTENDANCE_FULL_SCORE = BigDecimal.valueOf(40);
    private static final BigDecimal WEEKLY_WORK_FULL_SCORE = BigDecimal.valueOf(40);
    private static final BigDecimal DISCIPLINE_FULL_SCORE = BigDecimal.valueOf(20);
    private static final BigDecimal WEEKLY_MISSING_MAX_SCORE = BigDecimal.valueOf(59);

    private final WorkScoreMapper workScoreMapper;
    private final PermissionService permissionService;
    private final OperationLogService operationLogService;
    private final CurrentUserFacade currentUserFacade;
    private final DataScopeService dataScopeService;

    public WorkScoreServiceImpl(WorkScoreMapper workScoreMapper,
                                PermissionService permissionService) {
        this(workScoreMapper, permissionService, new OperationLogService() {
            @Override
            public void log(String moduleName, String actionName, Long bizId, String content) {
            }

            @Override
            public Object query(com.example.lecturesystem.modules.operationlog.dto.OperationLogQueryRequest request) {
                return java.util.List.of();
            }
        }, null, new DataScopeService());
    }

    @Autowired
    public WorkScoreServiceImpl(WorkScoreMapper workScoreMapper,
                                PermissionService permissionService,
                                OperationLogService operationLogService,
                                CurrentUserFacade currentUserFacade,
                                DataScopeService dataScopeService) {
        this.workScoreMapper = workScoreMapper;
        this.permissionService = permissionService;
        this.operationLogService = operationLogService;
        this.currentUserFacade = currentUserFacade;
        this.dataScopeService = dataScopeService;
    }

    @Override
    public Object query(WorkScoreQueryRequest request) {
        WorkScoreQueryRequest normalizedRequest = request == null ? new WorkScoreQueryRequest() : request;
        if (currentUserFacade != null && dataScopeService != null) {
            dataScopeService.injectTreePathScope(normalizedRequest, currentUserFacade.currentUserEntity());
        }
        normalizedRequest.setWeekNo(normalizeText(normalizedRequest.getWeekNo()));
        normalizedRequest.setUnitName(normalizeText(normalizedRequest.getUnitName()));
        normalizedRequest.setStatus(normalizeText(normalizedRequest.getStatus()));
        if (normalizedRequest.getSortByTotalDesc() == null) {
            normalizedRequest.setSortByTotalDesc(Boolean.TRUE);
        }
        return workScoreMapper.queryList(normalizedRequest);
    }

    @Override
    public Object detail(Long id) {
        WorkScoreListItemVO detail = workScoreMapper.findDetailById(id, currentTreePathPrefix());
        if (detail == null) {
            throw new IllegalArgumentException("评分记录不存在");
        }
        return detail;
    }

    @Override
    @Transactional
    public Object calculate(CalculateWorkScoreRequest request) {
        LoginUser loginUser = currentLoginUser();
        requireAdmin(loginUser);
        String weekNo = normalizeWeekNo(request.getWeekNo());
        LocalDate weekStart = parseWeekStart(weekNo);
        LocalDate weekEnd = weekStart.plusDays(6);

        List<WorkScoreCandidateVO> candidates = workScoreMapper.queryCandidates(
                weekNo,
                weekStart,
                weekEnd,
                currentTreePathPrefix()
        );
        int calculatedCount = 0;
        for (WorkScoreCandidateVO candidate : candidates) {
            WorkScoreEntity entity = new WorkScoreEntity();
            entity.setWeekNo(weekNo);
            entity.setUnitId(candidate.getUnitId());
            entity.setUserId(candidate.getUserId());
            int attendanceDays = candidate.getAttendanceDays() == null ? 0 : candidate.getAttendanceDays();
            entity.setAttendanceDays(attendanceDays);
            entity.setAttendanceScore(calculateAttendanceScore(attendanceDays));
            entity.setWeeklyWorkStatus(candidate.getWeeklyWorkStatus());
            entity.setWeeklyWorkScore(calculateWeeklyWorkScore(candidate.getWeeklyWorkStatus()));
            entity.setDisciplineScore(DISCIPLINE_FULL_SCORE);
            entity.setDisciplineRemark("最小评分阶段默认满分");
            BigDecimal totalScore = entity.getAttendanceScore().add(entity.getWeeklyWorkScore()).add(entity.getDisciplineScore());
            if (!"APPROVED".equals(candidate.getWeeklyWorkStatus()) && !"SUBMITTED".equals(candidate.getWeeklyWorkStatus())) {
                totalScore = totalScore.min(WEEKLY_MISSING_MAX_SCORE);
                entity.setDisciplineRemark("周报未提交，总分封顶 59 分");
            }
            entity.setTotalScore(totalScore);
            entity.setStatus(resolveStatus(entity.getTotalScore()));
            entity.setCalculateTime(LocalDateTime.now());
            workScoreMapper.upsert(entity);
            calculatedCount++;
        }

        List<WorkScoreEntity> rankingList = workScoreMapper.queryWeekRanking(weekNo);
        int rank = 1;
        for (WorkScoreEntity entity : rankingList) {
            entity.setLevel(rank++);
            entity.setStatus(resolveStatus(entity.getTotalScore()));
            workScoreMapper.upsert(entity);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("weekNo", weekNo);
        result.put("calculatedCount", calculatedCount);
        operationLogService.log(
                "WORK_SCORE",
                "CALCULATE",
                null,
                "计算评分：周次=" + weekNo + "，生成/覆盖 " + calculatedCount + " 条结果"
        );
        return result;
    }

    @Override
    public byte[] exportReport(String weekNo) {
        LoginUser loginUser = currentLoginUser();
        requireAdmin(loginUser);
        WorkScoreQueryRequest request = new WorkScoreQueryRequest();
        request.setWeekNo(normalizeWeekNo(weekNo));
        request.setSortByTotalDesc(Boolean.TRUE);

        @SuppressWarnings("unchecked")
        List<WorkScoreListItemVO> list = (List<WorkScoreListItemVO>) query(request);
        LocalDate weekStart = parseWeekStart(request.getWeekNo());
        LocalDate weekEnd = weekStart.plusDays(6);
        BigDecimal averageScore = calculateAverageScore(list);
        List<WorkScoreListItemVO> topTen = list.stream().limit(10).collect(Collectors.toList());
        List<WorkScoreListItemVO> redList = list.stream()
                .filter(item -> "RED".equals(item.getStatus()))
                .collect(Collectors.toList());
        List<Map<String, Object>> orgAverageList = buildOrgAverageList(list);

        try (XWPFDocument document = new XWPFDocument(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            addTitle(document, "每周工作评分通报");
            addLine(document, "周次：" + request.getWeekNo());
            addLine(document, "时间范围：" + weekStart + " 至 " + weekEnd);
            addLine(document, "总体情况：共 " + list.size() + " 人，平均分 " + formatScore(averageScore));

            addSectionTitle(document, "Top10 优秀人员表");
            addScoreTable(document, topTen, true);

            addSectionTitle(document, "红牌人员表");
            addScoreTable(document, redList, false);

            addSectionTitle(document, "组织平均分排名表");
            addOrgAverageTable(document, orgAverageList);

            document.write(outputStream);
            operationLogService.log("WORK_SCORE", "EXPORT_REPORT", null, "导出评分通报：周次=" + request.getWeekNo());
            return outputStream.toByteArray();
        } catch (Exception ex) {
            throw new IllegalArgumentException("导出评分通报失败");
        }
    }

    private BigDecimal calculateAttendanceScore(int attendanceDays) {
        int cappedDays = Math.min(Math.max(attendanceDays, 0), 5);
        return ATTENDANCE_FULL_SCORE
                .multiply(BigDecimal.valueOf(cappedDays))
                .divide(BigDecimal.valueOf(5), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateWeeklyWorkScore(String weeklyWorkStatus) {
        if ("APPROVED".equals(weeklyWorkStatus)) {
            return WEEKLY_WORK_FULL_SCORE;
        }
        if ("SUBMITTED".equals(weeklyWorkStatus)) {
            return BigDecimal.valueOf(30).setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    private String resolveStatus(BigDecimal totalScore) {
        if (totalScore == null) {
            return "RED";
        }
        if (totalScore.compareTo(BigDecimal.valueOf(75)) >= 0) {
            return "NORMAL";
        }
        if (totalScore.compareTo(BigDecimal.valueOf(60)) >= 0) {
            return "YELLOW";
        }
        return "RED";
    }

    private BigDecimal calculateAverageScore(List<WorkScoreListItemVO> list) {
        if (list.isEmpty()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal total = list.stream()
                .map(WorkScoreListItemVO::getTotalScore)
                .filter(score -> score != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(list.size()), 2, RoundingMode.HALF_UP);
    }

    private List<Map<String, Object>> buildOrgAverageList(List<WorkScoreListItemVO> list) {
        Map<String, Map<String, Object>> grouped = new LinkedHashMap<>();
        for (WorkScoreListItemVO item : list) {
            String key = item.getUnitName() == null || item.getUnitName().isBlank() ? "未分组" : item.getUnitName();
            Map<String, Object> current = grouped.computeIfAbsent(key, value -> {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("unitName", value);
                map.put("count", 0);
                map.put("total", BigDecimal.ZERO);
                return map;
            });
            current.put("count", (Integer) current.get("count") + 1);
            current.put("total", ((BigDecimal) current.get("total")).add(item.getTotalScore() == null ? BigDecimal.ZERO : item.getTotalScore()));
        }
        return grouped.values().stream()
                .map(item -> {
                    int count = (Integer) item.get("count");
                    BigDecimal total = (BigDecimal) item.get("total");
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("unitName", item.get("unitName"));
                    map.put("count", count);
                    map.put("averageScore", count == 0 ? BigDecimal.ZERO : total.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP));
                    return map;
                })
                .sorted((a, b) -> ((BigDecimal) b.get("averageScore")).compareTo((BigDecimal) a.get("averageScore")))
                .collect(Collectors.toList());
    }

    private void addTitle(XWPFDocument document, String text) {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun run = paragraph.createRun();
        run.setBold(true);
        run.setFontSize(16);
        run.setText(text);
    }

    private void addSectionTitle(XWPFDocument document, String text) {
        XWPFParagraph paragraph = document.createParagraph();
        XWPFRun run = paragraph.createRun();
        run.setBold(true);
        run.setFontSize(12);
        run.setText(text);
    }

    private void addLine(XWPFDocument document, String text) {
        XWPFParagraph paragraph = document.createParagraph();
        XWPFRun run = paragraph.createRun();
        run.setText(text);
    }

    private void addScoreTable(XWPFDocument document, List<WorkScoreListItemVO> list, boolean includeStatus) {
        XWPFTable table = document.createTable(Math.max(list.size() + 1, 2), includeStatus ? 5 : 4);
        table.getRow(0).getCell(0).setText("排名");
        table.getRow(0).getCell(1).setText("姓名");
        table.getRow(0).getCell(2).setText("组织");
        table.getRow(0).getCell(3).setText("总分");
        if (includeStatus) {
            table.getRow(0).getCell(4).setText("状态");
        }
        for (int i = 0; i < list.size(); i++) {
            WorkScoreListItemVO item = list.get(i);
            table.getRow(i + 1).getCell(0).setText(String.valueOf(item.getLevel() == null ? i + 1 : item.getLevel()));
            table.getRow(i + 1).getCell(1).setText(item.getRealName() == null ? "-" : item.getRealName());
            table.getRow(i + 1).getCell(2).setText(item.getUnitName() == null ? "-" : item.getUnitName());
            table.getRow(i + 1).getCell(3).setText(formatScore(item.getTotalScore()));
            if (includeStatus) {
                table.getRow(i + 1).getCell(4).setText(item.getStatus() == null ? "-" : item.getStatus());
            }
        }
    }

    private void addOrgAverageTable(XWPFDocument document, List<Map<String, Object>> list) {
        XWPFTable table = document.createTable(Math.max(list.size() + 1, 2), 4);
        table.getRow(0).getCell(0).setText("排名");
        table.getRow(0).getCell(1).setText("组织");
        table.getRow(0).getCell(2).setText("平均分");
        table.getRow(0).getCell(3).setText("人数");
        for (int i = 0; i < list.size(); i++) {
            Map<String, Object> item = list.get(i);
            table.getRow(i + 1).getCell(0).setText(String.valueOf(i + 1));
            table.getRow(i + 1).getCell(1).setText(String.valueOf(item.get("unitName")));
            table.getRow(i + 1).getCell(2).setText(formatScore((BigDecimal) item.get("averageScore")));
            table.getRow(i + 1).getCell(3).setText(String.valueOf(item.get("count")));
        }
    }

    private String formatScore(BigDecimal score) {
        return score == null ? "0.00" : score.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String normalizeWeekNo(String weekNo) {
        String normalized = normalizeText(weekNo);
        if (normalized == null) {
            throw new IllegalArgumentException("周次不能为空");
        }
        return normalized;
    }

    private String normalizeText(String text) {
        if (text == null) {
            return null;
        }
        String trimmed = text.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private LocalDate parseWeekStart(String weekNo) {
        String[] parts = weekNo.split("-W");
        if (parts.length != 2) {
            throw new IllegalArgumentException("周次格式应为 yyyy-Www");
        }
        int year = Integer.parseInt(parts[0]);
        int week = Integer.parseInt(parts[1]);
        WeekFields weekFields = WeekFields.ISO;
        return LocalDate.of(year, 1, 4)
                .with(weekFields.weekOfWeekBasedYear(), week)
                .with(weekFields.dayOfWeek(), DayOfWeek.MONDAY.getValue());
    }

    private LoginUser currentLoginUser() {
        if (currentUserFacade != null) {
            return currentUserFacade.currentLoginUser();
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof LoginUser loginUser)) {
            throw new IllegalArgumentException("当前未登录");
        }
        return loginUser;
    }

    private void requireAdmin(LoginUser loginUser) {
        if (!loginUser.isAdmin()) {
            throw new IllegalArgumentException("仅管理员可执行该操作");
        }
    }

    private String currentTreePathPrefix() {
        if (currentUserFacade == null || dataScopeService == null) {
            return null;
        }
        UserEntity currentUser = currentUserFacade.currentUserEntity();
        return dataScopeService.buildTreePathPrefix(currentUser);
    }
}
