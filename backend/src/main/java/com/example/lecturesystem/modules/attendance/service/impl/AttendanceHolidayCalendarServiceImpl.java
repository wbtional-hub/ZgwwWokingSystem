package com.example.lecturesystem.modules.attendance.service.impl;

import com.example.lecturesystem.modules.agent.support.OpenAiCompatibleChatClient;
import com.example.lecturesystem.modules.aiconfig.entity.ProviderConfigEntity;
import com.example.lecturesystem.modules.aiconfig.mapper.ProviderConfigMapper;
import com.example.lecturesystem.modules.aiconfig.support.AiTokenCipherSupport;
import com.example.lecturesystem.modules.attendance.dto.SaveAttendanceHolidayCalendarRequest;
import com.example.lecturesystem.modules.attendance.entity.AttendanceHolidayCalendarEntity;
import com.example.lecturesystem.modules.attendance.mapper.AttendanceHolidayCalendarMapper;
import com.example.lecturesystem.modules.attendance.service.AttendanceHolidayCalendarService;
import com.example.lecturesystem.modules.attendance.vo.AttendanceHolidayCalendarVO;
import com.example.lecturesystem.modules.permission.service.PermissionService;
import com.example.lecturesystem.modules.permission.support.CurrentUserFacade;
import com.example.lecturesystem.modules.user.entity.UserEntity;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
public class AttendanceHolidayCalendarServiceImpl implements AttendanceHolidayCalendarService {
    private static final Logger log = LoggerFactory.getLogger(AttendanceHolidayCalendarServiceImpl.class);
    private static final int AI_RESPONSE_LOG_LIMIT = 1000;
    private static final String DAY_TYPE_HOLIDAY = "HOLIDAY";
    private static final String DAY_TYPE_WEEKDAY_REST = "WEEKDAY_REST";
    private static final String DAY_TYPE_MAKEUP_WORKDAY = "MAKEUP_WORKDAY";
    private static final String SOURCE_MANUAL = "MANUAL";
    private static final String SOURCE_AI = "AI";
    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_CONFIRMED = "CONFIRMED";

    private final AttendanceHolidayCalendarMapper holidayCalendarMapper;
    private final CurrentUserFacade currentUserFacade;
    private final PermissionService permissionService;
    private final ProviderConfigMapper providerConfigMapper;
    private final AiTokenCipherSupport aiTokenCipherSupport;
    private final OpenAiCompatibleChatClient chatClient;
    private final ObjectMapper objectMapper;

    public AttendanceHolidayCalendarServiceImpl(AttendanceHolidayCalendarMapper holidayCalendarMapper,
                                                CurrentUserFacade currentUserFacade,
                                                PermissionService permissionService,
                                                ProviderConfigMapper providerConfigMapper,
                                                AiTokenCipherSupport aiTokenCipherSupport,
                                                OpenAiCompatibleChatClient chatClient,
                                                ObjectMapper objectMapper) {
        this.holidayCalendarMapper = holidayCalendarMapper;
        this.currentUserFacade = currentUserFacade;
        this.permissionService = permissionService;
        this.providerConfigMapper = providerConfigMapper;
        this.aiTokenCipherSupport = aiTokenCipherSupport;
        this.chatClient = chatClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<AttendanceHolidayCalendarVO> queryByYear(Integer year) {
        requireMaintainer();
        int targetYear = normalizeYear(year);
        return holidayCalendarMapper.selectByYear(targetYear).stream()
                .map(this::toVO)
                .toList();
    }

    @Override
    @Transactional
    public List<AttendanceHolidayCalendarVO> generateAiCandidates(Integer year) {
        UserEntity currentUser = currentUserFacade.currentUserEntity();
        int targetYear = normalizeYear(year);
        log.info("ATTENDANCE_HOLIDAY_AI_GENERATE_START year={}, userId={}, username={}",
                targetYear, currentUser.getId(), currentUser.getUsername());
        requireMaintainer(currentUser);
        if (holidayCalendarMapper.countConfirmedByYear(targetYear) > 0) {
            log.warn("ATTENDANCE_HOLIDAY_AI_GENERATE_REJECTED_CONFIRMED_EXISTS year={}, userId={}",
                    targetYear, currentUser.getId());
            throw new IllegalArgumentException("该年度已有已确认或已生效节假日数据，请人工确认后再重新生成");
        }

        List<SaveAttendanceHolidayCalendarRequest> candidates = requestAiCandidates(targetYear);
        LocalDateTime now = LocalDateTime.now();
        try {
            for (SaveAttendanceHolidayCalendarRequest candidate : candidates) {
                AttendanceHolidayCalendarEntity existed = holidayCalendarMapper.selectByDate(candidate.getCalendarDate());
                AttendanceHolidayCalendarEntity entity = existed == null ? new AttendanceHolidayCalendarEntity() : existed;
                entity.setCalendarDate(candidate.getCalendarDate());
                entity.setCalendarYear(targetYear);
                entity.setDayType(normalizeDayType(candidate.getDayType()));
                entity.setName(trimToNull(candidate.getName()));
                entity.setEnabled(false);
                entity.setSource(SOURCE_AI);
                entity.setStatus(STATUS_DRAFT);
                entity.setRemark(trimToNull(candidate.getRemark()));
                entity.setUpdateTime(now);
                if (entity.getId() == null) {
                    entity.setCreateTime(now);
                    holidayCalendarMapper.insert(entity);
                } else {
                    holidayCalendarMapper.update(entity);
                }
            }
        } catch (RuntimeException ex) {
            log.error("ATTENDANCE_HOLIDAY_AI_SAVE_FAILED year={}, candidateCount={}, message={}",
                    targetYear, candidates.size(), ex.getMessage(), ex);
            throw ex;
        }
        log.info("ATTENDANCE_HOLIDAY_AI_GENERATE_DONE year={}, candidateCount={}, userId={}",
                targetYear, candidates.size(), currentUser.getId());
        return queryByYear(targetYear);
    }

    @Override
    @Transactional
    public Long save(SaveAttendanceHolidayCalendarRequest request) {
        requireMaintainer();
        if (request == null) {
            throw new IllegalArgumentException("保存参数不能为空");
        }
        LocalDate calendarDate = request.getCalendarDate();
        if (calendarDate == null) {
            throw new IllegalArgumentException("日期不能为空");
        }
        String dayType = normalizeDayType(request.getDayType());
        int year = calendarDate.getYear();
        AttendanceHolidayCalendarEntity sameDate = holidayCalendarMapper.selectByDate(calendarDate);
        if (sameDate != null && !Objects.equals(sameDate.getId(), request.getId())) {
            throw new IllegalArgumentException("同一天只能维护一条节假日规则");
        }

        LocalDateTime now = LocalDateTime.now();
        AttendanceHolidayCalendarEntity entity;
        if (request.getId() == null) {
            entity = new AttendanceHolidayCalendarEntity();
            entity.setCreateTime(now);
        } else {
            entity = holidayCalendarMapper.selectById(request.getId());
            if (entity == null) {
                throw new IllegalArgumentException("节假日记录不存在");
            }
        }
        entity.setCalendarDate(calendarDate);
        entity.setCalendarYear(year);
        entity.setDayType(dayType);
        entity.setName(trimToNull(request.getName()));
        entity.setEnabled(request.getEnabled() == null ? Boolean.TRUE : request.getEnabled());
        entity.setSource(normalizeSource(request.getSource()));
        entity.setStatus(normalizeStatus(request.getStatus(), Boolean.TRUE.equals(entity.getEnabled())));
        entity.setRemark(trimToNull(request.getRemark()));
        entity.setUpdateTime(now);

        if (entity.getId() == null) {
            holidayCalendarMapper.insert(entity);
        } else {
            holidayCalendarMapper.update(entity);
        }
        return entity.getId();
    }

    @Override
    @Transactional
    public void delete(Long id) {
        requireMaintainer();
        if (id == null) {
            throw new IllegalArgumentException("记录ID不能为空");
        }
        holidayCalendarMapper.deleteById(id);
    }

    @Override
    @Transactional
    public List<AttendanceHolidayCalendarVO> confirmYear(Integer year) {
        requireMaintainer();
        int targetYear = normalizeYear(year);
        if (holidayCalendarMapper.countEnabledDuplicateByYear(targetYear) > 0) {
            throw new IllegalArgumentException("同一年存在同一天多条已生效记录，请处理冲突后再确认");
        }
        holidayCalendarMapper.confirmYear(targetYear, LocalDateTime.now());
        return queryByYear(targetYear);
    }

    private List<SaveAttendanceHolidayCalendarRequest> requestAiCandidates(int year) {
        ProviderConfigEntity provider = providerConfigMapper.findFirstEnabledSuccess();
        if (provider == null || trimToNull(provider.getDefaultModel()) == null) {
            log.warn("ATTENDANCE_HOLIDAY_AI_PROVIDER_NOT_READY year={}, providerFound={}",
                    year, provider != null);
            throw new IllegalArgumentException("AI模型配置未就绪，请先配置可用的AI接入");
        }
        String token = aiTokenCipherSupport.decrypt(provider.getApiTokenCipher());
        String systemPrompt = "你是中国大陆年度节假日安排结构化助手。只输出JSON数组，不要输出解释文字。"
                + "dayType只能是HOLIDAY、WEEKDAY_REST、MAKEUP_WORKDAY。日期格式必须是yyyy-MM-dd。";
        String userPrompt = "请参考国务院办公厅" + year + "年节假日安排，生成该年度中国大陆法定节假日、"
                + "工作日调整休息日、补班工作日候选数据。"
                + "仅输出JSON数组，数组项字段为calendarDate、dayType、name、remark，所有日期必须属于" + year + "年。";
        log.info("ATTENDANCE_HOLIDAY_AI_CALL_START year={}, providerId={}, providerName={}, model={}",
                year, provider.getId(), provider.getProviderName(), provider.getDefaultModel());
        String content;
        try {
            content = chatClient.chat(
                    provider.getApiBaseUrl(),
                    token,
                    provider.getDefaultModel(),
                    systemPrompt,
                    userPrompt
            );
        } catch (RuntimeException ex) {
            log.error("ATTENDANCE_HOLIDAY_AI_CALL_FAILED year={}, providerId={}, providerName={}, model={}, message={}",
                    year, provider.getId(), provider.getProviderName(), provider.getDefaultModel(), ex.getMessage(), ex);
            if (isAiRateLimited(ex)) {
                throw new IllegalArgumentException("AI服务当前调用频率受限或额度不足，请稍后重试，或检查AI接入配置。", ex);
            }
            throw ex;
        }
        return parseAndValidateAiResult(content, year);
    }

    private boolean isAiRateLimited(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && (message.contains("HTTP状态码=429") || message.contains("HTTP 429"))) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private List<SaveAttendanceHolidayCalendarRequest> parseAndValidateAiResult(String rawContent, int year) {
        String json = extractJsonArray(rawContent);
        List<SaveAttendanceHolidayCalendarRequest> parsed;
        try {
            parsed = objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception ex) {
            log.warn("ATTENDANCE_HOLIDAY_AI_JSON_PARSE_FAILED year={}, responsePreview={}, message={}",
                    year, abbreviate(rawContent, AI_RESPONSE_LOG_LIMIT), ex.getMessage());
            throw new IllegalArgumentException("AI返回结果不是合法JSON数组，请重新生成", ex);
        }
        Map<LocalDate, SaveAttendanceHolidayCalendarRequest> deduped = new LinkedHashMap<>();
        for (SaveAttendanceHolidayCalendarRequest item : parsed) {
            if (item == null || item.getCalendarDate() == null) {
                log.warn("ATTENDANCE_HOLIDAY_AI_ITEM_SKIPPED_EMPTY_DATE year={}", year);
                continue;
            }
            if (item.getCalendarDate().getYear() != year) {
                log.warn("ATTENDANCE_HOLIDAY_AI_INVALID_YEAR year={}, calendarDate={}",
                        year, item.getCalendarDate());
                throw new IllegalArgumentException("AI返回了非请求年度日期：" + item.getCalendarDate());
            }
            item.setDayType(normalizeDayType(item.getDayType()));
            item.setEnabled(false);
            item.setSource(SOURCE_AI);
            item.setStatus(STATUS_DRAFT);
            if (deduped.containsKey(item.getCalendarDate())) {
                log.warn("ATTENDANCE_HOLIDAY_AI_DUPLICATE_DATE year={}, calendarDate={}",
                        year, item.getCalendarDate());
            }
            deduped.put(item.getCalendarDate(), item);
        }
        if (deduped.isEmpty()) {
            log.warn("ATTENDANCE_HOLIDAY_AI_EMPTY_VALID_RESULT year={}, responsePreview={}",
                    year, abbreviate(rawContent, AI_RESPONSE_LOG_LIMIT));
            throw new IllegalArgumentException("AI未生成有效节假日候选数据");
        }
        log.info("ATTENDANCE_HOLIDAY_AI_PARSE_DONE year={}, rawCount={}, validCount={}",
                year, parsed.size(), deduped.size());
        return new ArrayList<>(deduped.values());
    }

    private String extractJsonArray(String rawContent) {
        String text = trimToNull(rawContent);
        if (text == null) {
            log.warn("ATTENDANCE_HOLIDAY_AI_EMPTY_RESPONSE");
            throw new IllegalArgumentException("AI返回结果为空");
        }
        int start = text.indexOf('[');
        int end = text.lastIndexOf(']');
        if (start < 0 || end < start) {
            log.warn("ATTENDANCE_HOLIDAY_AI_JSON_ARRAY_NOT_FOUND responsePreview={}",
                    abbreviate(rawContent, AI_RESPONSE_LOG_LIMIT));
            throw new IllegalArgumentException("AI返回结果未包含JSON数组");
        }
        return text.substring(start, end + 1);
    }

    private AttendanceHolidayCalendarVO toVO(AttendanceHolidayCalendarEntity entity) {
        AttendanceHolidayCalendarVO vo = new AttendanceHolidayCalendarVO();
        vo.setId(entity.getId());
        vo.setCalendarDate(entity.getCalendarDate());
        vo.setYear(entity.getCalendarYear());
        vo.setWeekday(resolveWeekday(entity.getCalendarDate()));
        vo.setDayType(entity.getDayType());
        vo.setDayTypeLabel(resolveDayTypeLabel(entity.getDayType()));
        vo.setName(entity.getName());
        vo.setEnabled(entity.getEnabled());
        vo.setRemark(entity.getRemark());
        vo.setSource(entity.getSource());
        vo.setStatus(entity.getStatus());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }

    private void requireMaintainer() {
        requireMaintainer(currentUserFacade.currentUserEntity());
    }

    private void requireMaintainer(UserEntity currentUser) {
        boolean allowed = permissionService.isSuperAdmin(currentUser.getId())
                || permissionService.isUnitAdmin(currentUser.getId());
        if (!allowed) {
            log.warn("ATTENDANCE_HOLIDAY_MAINTAIN_PERMISSION_DENIED userId={}, username={}",
                    currentUser.getId(), currentUser.getUsername());
            throw new IllegalArgumentException("当前用户无权维护考勤节假日");
        }
    }

    private int normalizeYear(Integer year) {
        int targetYear = year == null ? LocalDate.now().getYear() : year;
        if (targetYear < 2000 || targetYear > 2100) {
            throw new IllegalArgumentException("年度范围不合法");
        }
        return targetYear;
    }

    private String normalizeDayType(String dayType) {
        String normalized = trimToNull(dayType);
        if (normalized == null) {
            log.warn("ATTENDANCE_HOLIDAY_INVALID_DAY_TYPE empty dayType");
            throw new IllegalArgumentException("日期类型不能为空");
        }
        normalized = normalized.toUpperCase(Locale.ROOT);
        if (!DAY_TYPE_HOLIDAY.equals(normalized)
                && !DAY_TYPE_WEEKDAY_REST.equals(normalized)
                && !DAY_TYPE_MAKEUP_WORKDAY.equals(normalized)) {
            log.warn("ATTENDANCE_HOLIDAY_INVALID_DAY_TYPE dayType={}", normalized);
            throw new IllegalArgumentException("日期类型仅支持 HOLIDAY / WEEKDAY_REST / MAKEUP_WORKDAY");
        }
        return normalized;
    }

    private String normalizeSource(String source) {
        String normalized = trimToNull(source);
        if (normalized == null) {
            return SOURCE_MANUAL;
        }
        normalized = normalized.toUpperCase(Locale.ROOT);
        return SOURCE_AI.equals(normalized) ? SOURCE_AI : SOURCE_MANUAL;
    }

    private String normalizeStatus(String status, boolean enabled) {
        String normalized = trimToNull(status);
        if (normalized == null) {
            return enabled ? STATUS_CONFIRMED : STATUS_DRAFT;
        }
        normalized = normalized.toUpperCase(Locale.ROOT);
        return STATUS_CONFIRMED.equals(normalized) ? STATUS_CONFIRMED : STATUS_DRAFT;
    }

    private String resolveDayTypeLabel(String dayType) {
        String normalized = dayType == null ? "" : dayType;
        return switch (normalized) {
            case DAY_TYPE_HOLIDAY -> "法定节假日";
            case DAY_TYPE_WEEKDAY_REST -> "调整休息日";
            case DAY_TYPE_MAKEUP_WORKDAY -> "补班工作日";
            default -> normalized;
        };
    }

    private String resolveWeekday(LocalDate date) {
        if (date == null) {
            return "";
        }
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return switch (dayOfWeek) {
            case MONDAY -> "星期一";
            case TUESDAY -> "星期二";
            case WEDNESDAY -> "星期三";
            case THURSDAY -> "星期四";
            case FRIDAY -> "星期五";
            case SATURDAY -> "星期六";
            case SUNDAY -> "星期日";
        };
    }

    private String trimToNull(String text) {
        if (text == null) {
            return null;
        }
        String trimmed = text.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String abbreviate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }
}
