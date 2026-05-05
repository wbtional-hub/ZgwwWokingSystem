package com.example.lecturesystem.modules.attendance.service.impl;

import com.example.lecturesystem.modules.attendance.entity.AttendanceRecordEntity;
import com.example.lecturesystem.modules.attendance.entity.AttendanceReminderCandidateEntity;
import com.example.lecturesystem.modules.attendance.entity.AttendanceReminderLogEntity;
import com.example.lecturesystem.modules.attendance.mapper.AttendanceReminderLogMapper;
import com.example.lecturesystem.modules.attendance.mapper.AttendanceReminderQueryMapper;
import com.example.lecturesystem.modules.attendance.service.AttendanceReminderService;
import com.example.lecturesystem.modules.attendance.service.AttendanceWorkdayService;
import com.example.lecturesystem.modules.attendance.support.AttendanceReminderProperties;
import com.example.lecturesystem.modules.attendance.support.AttendanceReminderType;
import com.example.lecturesystem.modules.auth.security.LoginUser;
import com.example.lecturesystem.modules.auth.service.WechatTemplateMessageService;
import com.example.lecturesystem.modules.param.service.ParamService;
import com.example.lecturesystem.modules.permission.service.PermissionService;
import com.example.lecturesystem.modules.permission.support.CurrentUserFacade;
import com.example.lecturesystem.modules.user.entity.UserEntity;
import com.example.lecturesystem.modules.user.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AttendanceReminderServiceImpl implements AttendanceReminderService {
    private static final Logger log = LoggerFactory.getLogger(AttendanceReminderServiceImpl.class);
    private static final String PARAM_REMINDER_ENABLED = "ATTENDANCE_REMINDER_ENABLED";
    private static final String PARAM_REMINDER_ENTRY_URL = "ATTENDANCE_REMINDER_ENTRY_URL";
    private static final String PARAM_CHECKIN_TEMPLATE_ID = "ATTENDANCE_REMINDER_CHECKIN_TEMPLATE_ID";
    private static final String PARAM_CHECKOUT_TEMPLATE_ID = "ATTENDANCE_REMINDER_CHECKOUT_TEMPLATE_ID";
    private static final DateTimeFormatter TEMPLATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final AttendanceReminderQueryMapper attendanceReminderQueryMapper;
    private final AttendanceReminderLogMapper attendanceReminderLogMapper;
    private final WechatTemplateMessageService wechatTemplateMessageService;
    private final AttendanceReminderProperties attendanceReminderProperties;
    private final ParamService paramService;
    private final CurrentUserFacade currentUserFacade;
    private final PermissionService permissionService;
    private final UserMapper userMapper;
    @Autowired(required = false)
    private AttendanceWorkdayService attendanceWorkdayService;

    public AttendanceReminderServiceImpl(AttendanceReminderQueryMapper attendanceReminderQueryMapper,
                                         AttendanceReminderLogMapper attendanceReminderLogMapper,
                                         WechatTemplateMessageService wechatTemplateMessageService,
                                         AttendanceReminderProperties attendanceReminderProperties,
                                         ParamService paramService,
                                         CurrentUserFacade currentUserFacade,
                                         PermissionService permissionService,
                                         UserMapper userMapper) {
        this.attendanceReminderQueryMapper = attendanceReminderQueryMapper;
        this.attendanceReminderLogMapper = attendanceReminderLogMapper;
        this.wechatTemplateMessageService = wechatTemplateMessageService;
        this.attendanceReminderProperties = attendanceReminderProperties;
        this.paramService = paramService;
        this.currentUserFacade = currentUserFacade;
        this.permissionService = permissionService;
        this.userMapper = userMapper;
    }

    @Override
    public Map<String, Object> sendScheduledReminders(AttendanceReminderType reminderType) {
        Map<String, Object> result = buildBaseResult(reminderType);
        if (!isReminderEnabled()) {
            result.put("message", "考勤提醒开关未开启，请先设置 attendance.reminder.enabled=true");
            log.warn("attendance reminder skipped: type={}, reason={}",
                    reminderType.getCode(), result.get("message"));
            return result;
        }

        try {
            validateReminderConfiguration(reminderType);
        } catch (IllegalStateException ex) {
            result.put("message", ex.getMessage());
            log.error("attendance reminder configuration invalid: type={}, message={}",
                    reminderType.getCode(), ex.getMessage());
            return result;
        }

        LocalDate today = LocalDate.now();
        LocalTime nowTime = LocalTime.now();
        if (!isWorkday(today)) {
            result.put("message", "今日非工作日，已跳过考勤提醒");
            log.info("attendance reminder skipped: type={}, date={}, reason={}",
                    reminderType.getCode(), today, result.get("message"));
            return result;
        }

        List<AttendanceReminderCandidateEntity> candidates = attendanceReminderQueryMapper.queryReminderCandidates();
        List<Long> userIds = candidates.stream().map(AttendanceReminderCandidateEntity::getUserId).distinct().toList();
        Map<Long, AttendanceRecordEntity> todayRecordMap = buildTodayRecordMap(today, userIds);

        int sentCount = 0;
        int skippedCount = 0;
        int failedCount = 0;
        List<String> details = new ArrayList<>();

        log.info("attendance reminder start: type={}, date={}, time={}, candidateCount={}",
                reminderType.getCode(), today, nowTime, candidates.size());

        for (AttendanceReminderCandidateEntity candidate : candidates) {
            ReminderDecision decision = evaluateReminder(candidate, todayRecordMap.get(candidate.getUserId()), nowTime);
            log.info("attendance reminder evaluate: type={}, userId={}, username={}, action={}, shouldSend={}, reason={}",
                    reminderType.getCode(),
                    candidate.getUserId(),
                    candidate.getUsername(),
                    decision.action(),
                    decision.shouldSend(),
                    decision.reason());

            if (!decision.shouldSend()) {
                skippedCount += 1;
                log.info("attendance reminder skipped: type={}, userId={}, username={}, reason={}",
                        reminderType.getCode(), candidate.getUserId(), candidate.getUsername(), decision.reason());
                continue;
            }

            if (!reminderType.matchesAction(decision.action())) {
                skippedCount += 1;
                log.info("attendance reminder skipped: type={}, userId={}, username={}, reason={}",
                        reminderType.getCode(),
                        candidate.getUserId(),
                        candidate.getUsername(),
                        "当前动作与提醒类型不匹配: " + decision.action());
                continue;
            }

            DispatchOutcome outcome = dispatchReminder(candidate, reminderType, today);
            if ("SKIPPED_DUPLICATE".equals(outcome.sendStatus())) {
                skippedCount += 1;
                continue;
            }
            details.add(candidate.getUserId() + ":" + outcome.sendStatus());
            if (outcome.success()) {
                sentCount += 1;
            } else {
                failedCount += 1;
            }
        }

        result.put("candidateCount", candidates.size());
        result.put("sentCount", sentCount);
        result.put("skippedCount", skippedCount);
        result.put("failedCount", failedCount);
        result.put("details", details);
        result.put("message", "ok");
        log.info("attendance reminder finished: type={}, date={}, candidateCount={}, sentCount={}, skippedCount={}, failedCount={}",
                reminderType.getCode(), today, candidates.size(), sentCount, skippedCount, failedCount);
        return result;
    }

    @Override
    public Map<String, Object> runNow(AttendanceReminderType reminderType) {
        requireAdmin();
        log.info("attendance reminder manual run: type={}", reminderType.getCode());
        return sendScheduledReminders(reminderType);
    }

    @Override
    public Map<String, Object> testSend(Long userId, AttendanceReminderType reminderType) {
        requireAdmin();
        validateReminderConfiguration(reminderType);

        AttendanceReminderCandidateEntity candidate = attendanceReminderQueryMapper.queryReminderCandidates().stream()
                .filter(item -> item.getUserId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("目标用户不满足提醒条件，请先确认账号状态、openid 和 attendance_workbench 权限"));

        LocalDate today = LocalDate.now();
        log.info("attendance reminder manual test start: type={}, userId={}, username={}",
                reminderType.getCode(), candidate.getUserId(), candidate.getUsername());
        DispatchOutcome outcome = dispatchReminder(candidate, reminderType, today);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("userId", candidate.getUserId());
        result.put("username", candidate.getUsername());
        result.put("realName", candidate.getRealName());
        result.put("reminderType", reminderType.getCode());
        result.put("sendStatus", outcome.sendStatus());
        result.put("success", outcome.success());
        result.put("wxErrCode", outcome.wxErrCode());
        result.put("wxErrMsg", outcome.wxErrMsg());
        result.put("targetUrl", outcome.targetUrl());
        return result;
    }

    @Override
    public Map<String, Object> healthCheck() {
        requireAdmin();
        List<AttendanceReminderCandidateEntity> candidates = attendanceReminderQueryMapper.queryReminderCandidates();
        long openidCandidateCount = candidates.stream()
                .map(AttendanceReminderCandidateEntity::getOpenid)
                .filter(this::hasText)
                .count();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("enabled", isReminderEnabled());
        result.put("entryUrlConfigured", hasText(resolveReminderEntryUrl()));
        result.put("checkinTemplateIdConfigured", hasText(resolveReminderTemplateId(AttendanceReminderType.CHECK_IN_MISS)));
        result.put("checkoutTemplateIdConfigured", hasText(resolveReminderTemplateId(AttendanceReminderType.CHECK_OUT_MISS)));
        result.put("checkinCron", attendanceReminderProperties.getResolvedCheckinCron());
        result.put("checkoutCron", attendanceReminderProperties.getResolvedCheckoutCron());
        result.put("candidateCount", candidates.size());
        result.put("openidCandidateCount", openidCandidateCount);
        result.put("effectiveEntryUrl", resolveReminderEntryUrl());
        result.put("configSourceHint", resolveConfigSourceHint());
        result.put("checkinTemplateIdPreview", previewValue(resolveReminderTemplateId(AttendanceReminderType.CHECK_IN_MISS)));
        result.put("checkoutTemplateIdPreview", previewValue(resolveReminderTemplateId(AttendanceReminderType.CHECK_OUT_MISS)));
        if (!isReminderEnabled()) {
            result.put("message", "考勤提醒开关未开启");
        } else if (!hasText(resolveReminderEntryUrl())) {
            result.put("message", "未配置 attendance.reminder.entry-url");
        } else if (!hasText(resolveReminderTemplateId(AttendanceReminderType.CHECK_IN_MISS))) {
            result.put("message", "未配置 attendance.reminder.checkin-template-id");
        } else if (!hasText(resolveReminderTemplateId(AttendanceReminderType.CHECK_OUT_MISS))) {
            result.put("message", "未配置 attendance.reminder.checkout-template-id");
        } else {
            result.put("message", "ok");
        }
        return result;
    }

    private Map<String, Object> buildBaseResult(AttendanceReminderType reminderType) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("enabled", isReminderEnabled());
        result.put("reminderType", reminderType.getCode());
        result.put("candidateCount", 0);
        result.put("sentCount", 0);
        result.put("skippedCount", 0);
        result.put("failedCount", 0);
        result.put("details", new ArrayList<>());
        return result;
    }

    private Map<Long, AttendanceRecordEntity> buildTodayRecordMap(LocalDate attendanceDate, List<Long> userIds) {
        Map<Long, AttendanceRecordEntity> recordMap = new HashMap<>();
        if (userIds == null || userIds.isEmpty()) {
            return recordMap;
        }
        List<AttendanceRecordEntity> records = attendanceReminderQueryMapper.queryTodayRecordsByUserIds(attendanceDate, userIds);
        for (AttendanceRecordEntity record : records) {
            recordMap.putIfAbsent(record.getUserId(), record);
        }
        return recordMap;
    }

    private ReminderDecision evaluateReminder(AttendanceReminderCandidateEntity candidate,
                                              AttendanceRecordEntity record,
                                              LocalTime nowTime) {
        if (candidate == null) {
            return ReminderDecision.skip(null, "candidate 为空");
        }
        if (!isWorkday(LocalDate.now())) {
            return ReminderDecision.skip(null, "非工作日不提醒");
        }
        if (candidate.getWorkStartTime() == null
                || candidate.getAmOffTime() == null
                || candidate.getPmOnTime() == null
                || candidate.getWorkEndTime() == null) {
            return ReminderDecision.skip(null, "考勤规则不完整");
        }

        boolean amOnDone = record != null && record.getCheckInTime() != null;
        boolean pmOffDone = record != null && record.getCheckOutTime() != null;

        if (pmOffDone) {
            return ReminderDecision.skip(null, "今日打卡已完成");
        }

        if (nowTime.isBefore(candidate.getWorkStartTime())) {
            return ReminderDecision.skip(null, "尚未到提醒时段");
        }

        if (nowTime.isBefore(candidate.getAmOffTime())) {
            if (!amOnDone) {
                return ReminderDecision.send("AM_ON");
            }
            return ReminderDecision.skip(null, "上午时段已完成");
        }

        if (nowTime.isBefore(candidate.getPmOnTime())) {
            return ReminderDecision.skip(null, "午休时段不提醒");
        }

        if (nowTime.isBefore(candidate.getWorkEndTime())) {
            return ReminderDecision.skip(null, "下午时段暂不提醒，等待下班提醒触发");
        }

        if (!pmOffDone) {
            return ReminderDecision.send("PM_OFF");
        }
        return ReminderDecision.skip(null, "今日打卡已完成");
    }

    private DispatchOutcome dispatchReminder(AttendanceReminderCandidateEntity candidate,
                                             AttendanceReminderType reminderType,
                                             LocalDate attendanceDate) {
        String templateId = resolveTemplateId(reminderType);
        String targetUrl = buildTargetUrl(reminderType);
        AttendanceReminderLogEntity logEntity = claimReminderSlot(candidate, reminderType, attendanceDate, templateId, targetUrl);
        if (logEntity == null) {
            return new DispatchOutcome(false, "SKIPPED_DUPLICATE", null, null, targetUrl);
        }

        log.info("attendance reminder sending: type={}, userId={}, username={}, openid={}, templateId={}, targetUrl={}",
                reminderType.getCode(),
                candidate.getUserId(),
                candidate.getUsername(),
                maskOpenid(candidate.getOpenid()),
                templateId,
                targetUrl);

        WechatTemplateMessageService.SendRequest request = new WechatTemplateMessageService.SendRequest();
        request.setOpenId(candidate.getOpenid());
        request.setTemplateId(templateId);
        request.setUrl(targetUrl);
        Map<String, WechatTemplateMessageService.TemplateDataItem> templateData = buildTemplateData(reminderType, candidate, attendanceDate);
        log.info("attendance reminder template data: type={}, userId={}, data={}",
                reminderType.getCode(), candidate.getUserId(), maskTemplateDataForLog(templateData));
        request.setData(templateData);

        WechatTemplateMessageService.SendResult sendResult = wechatTemplateMessageService.sendTemplateMessage(request);
        String sendStatus = sendResult.isSuccess() ? "SUCCESS" : "FAILED";
        updateReminderLog(logEntity, sendStatus, sendResult);
        log.info("attendance reminder send result: type={}, userId={}, username={}, sendStatus={}, wxErrCode={}, wxErrMsg={}",
                reminderType.getCode(),
                candidate.getUserId(),
                candidate.getUsername(),
                sendStatus,
                sendResult.getErrCode(),
                sendResult.getErrMsg());
        return new DispatchOutcome(sendResult.isSuccess(), sendStatus, sendResult.getErrCode(), sendResult.getErrMsg(), targetUrl);
    }

    private AttendanceReminderLogEntity claimReminderSlot(AttendanceReminderCandidateEntity candidate,
                                                          AttendanceReminderType reminderType,
                                                          LocalDate attendanceDate,
                                                          String templateId,
                                                          String targetUrl) {
        AttendanceReminderLogEntity logEntity = new AttendanceReminderLogEntity();
        logEntity.setUserId(candidate.getUserId());
        logEntity.setOpenid(candidate.getOpenid());
        logEntity.setAttendanceDate(attendanceDate);
        logEntity.setReminderType(reminderType.getCode());
        logEntity.setTemplateId(templateId);
        logEntity.setTargetUrl(targetUrl);
        logEntity.setSendStatus("PENDING");
        OffsetDateTime now = OffsetDateTime.now(ZoneId.systemDefault());
        logEntity.setCreatedAt(now);
        logEntity.setUpdatedAt(now);
        try {
            attendanceReminderLogMapper.insert(logEntity);
            return logEntity;
        } catch (DuplicateKeyException ex) {
            log.info("attendance reminder duplicate skipped: userId={}, date={}, type={}",
                    candidate.getUserId(), attendanceDate, reminderType.getCode());
            return null;
        }
    }

    private void updateReminderLog(AttendanceReminderLogEntity logEntity,
                                   String sendStatus,
                                   WechatTemplateMessageService.SendResult sendResult) {
        logEntity.setTemplatePayload(sendResult.getRequestPayload());
        logEntity.setSendStatus(sendStatus);
        logEntity.setWxErrCode(sendResult.getErrCode());
        logEntity.setWxErrMsg(sendResult.getErrMsg() == null ? sendResult.getResponseBody() : sendResult.getErrMsg());
        logEntity.setUpdatedAt(OffsetDateTime.now(ZoneId.systemDefault()));
        attendanceReminderLogMapper.updateSendResult(logEntity);
    }

    private Map<String, WechatTemplateMessageService.TemplateDataItem> buildTemplateData(AttendanceReminderType reminderType,
                                                                                          AttendanceReminderCandidateEntity candidate,
                                                                                          LocalDate attendanceDate) {
        Map<String, WechatTemplateMessageService.TemplateDataItem> data = new LinkedHashMap<>();
        boolean checkIn = reminderType == AttendanceReminderType.CHECK_IN_MISS;
        data.put("thing8", new WechatTemplateMessageService.TemplateDataItem(resolveReminderDisplayName(candidate)));
        data.put("time9", new WechatTemplateMessageService.TemplateDataItem(OffsetDateTime.now(ZoneId.systemDefault()).format(TEMPLATE_TIME_FORMATTER)));
        data.put("thing7", new WechatTemplateMessageService.TemplateDataItem(checkIn ? "上班未打卡提醒" : "下班未打卡提醒"));
        data.put("time5", new WechatTemplateMessageService.TemplateDataItem(formatTemplateTime(attendanceDate, resolveReminderDeadline(candidate, reminderType))));
        data.put("thing4", new WechatTemplateMessageService.TemplateDataItem(checkIn ? "今日尚未完成上班打卡，请及时处理" : "今日尚未完成下班打卡，请及时处理"));
        return data;
    }

    private String resolveReminderDisplayName(AttendanceReminderCandidateEntity candidate) {
        String realName = trimToNull(candidate == null ? null : candidate.getRealName());
        if (realName != null) {
            return realName;
        }
        String username = trimToNull(candidate == null ? null : candidate.getUsername());
        return username == null ? "员工" : username;
    }

    private LocalTime resolveReminderDeadline(AttendanceReminderCandidateEntity candidate, AttendanceReminderType reminderType) {
        if (reminderType == AttendanceReminderType.CHECK_IN_MISS) {
            return candidate != null && candidate.getWorkStartTime() != null ? candidate.getWorkStartTime() : LocalTime.of(9, 0);
        }
        return candidate != null && candidate.getWorkEndTime() != null ? candidate.getWorkEndTime() : LocalTime.of(18, 0);
    }

    private String formatTemplateTime(LocalDate date, LocalTime time) {
        LocalDate actualDate = date == null ? LocalDate.now() : date;
        LocalTime actualTime = time == null ? LocalTime.of(9, 0) : time;
        return actualDate.atTime(actualTime).format(TEMPLATE_TIME_FORMATTER);
    }

    private Map<String, String> maskTemplateDataForLog(Map<String, WechatTemplateMessageService.TemplateDataItem> data) {
        Map<String, String> result = new LinkedHashMap<>();
        if (data == null) {
            return result;
        }
        data.forEach((key, item) -> result.put(key, "thing8".equals(key) ? previewValue(item.getValue()) : item.getValue()));
        return result;
    }

    private void validateReminderConfiguration(AttendanceReminderType reminderType) {
        if (!isReminderEnabled()) {
            throw new IllegalStateException("考勤提醒开关未开启，请先设置 attendance.reminder.enabled=true");
        }
        if (!hasText(resolveReminderEntryUrl())) {
            throw new IllegalStateException("未配置考勤提醒入口地址 attendance.reminder.entry-url");
        }
        if (!hasText(resolveTemplateId(reminderType))) {
            if (reminderType == AttendanceReminderType.CHECK_IN_MISS) {
                throw new IllegalStateException("未配置上班提醒模板ID attendance.reminder.checkin-template-id");
            }
            throw new IllegalStateException("未配置下班提醒模板ID attendance.reminder.checkout-template-id");
        }
    }

    private String buildTargetUrl(AttendanceReminderType reminderType) {
        String entryUrl = trimToNull(resolveReminderEntryUrl());
        if (entryUrl == null) {
            throw new IllegalStateException("未配置考勤提醒入口地址 attendance.reminder.entry-url");
        }
        String delimiter = entryUrl.contains("?") ? "&" : "?";
        return entryUrl + delimiter + "source=wx_reminder&type=" + reminderType.getEntryType();
    }

    private String resolveTemplateId(AttendanceReminderType reminderType) {
        return trimToNull(resolveReminderTemplateId(reminderType));
    }

    private boolean isReminderEnabled() {
        String paramValue = getParamRawValue(PARAM_REMINDER_ENABLED);
        if (paramValue != null) {
            return isTruthy(paramValue);
        }
        return attendanceReminderProperties.isEnabled();
    }

    private String resolveReminderEntryUrl() {
        String paramValue = getParamRawValue(PARAM_REMINDER_ENTRY_URL);
        if (paramValue != null) {
            return trimToNull(paramValue);
        }
        return trimToNull(attendanceReminderProperties.getResolvedEntryUrl());
    }

    private String resolveReminderTemplateId(AttendanceReminderType reminderType) {
        if (reminderType == AttendanceReminderType.CHECK_IN_MISS) {
            String paramValue = getParamRawValue(PARAM_CHECKIN_TEMPLATE_ID);
            if (paramValue != null) {
                return trimToNull(paramValue);
            }
            return trimToNull(attendanceReminderProperties.getResolvedCheckinTemplateId());
        }
        String paramValue = getParamRawValue(PARAM_CHECKOUT_TEMPLATE_ID);
        if (paramValue != null) {
            return trimToNull(paramValue);
        }
        return trimToNull(attendanceReminderProperties.getResolvedCheckoutTemplateId());
    }

    private String resolveConfigSourceHint() {
        if (hasParamOverride(PARAM_REMINDER_ENABLED)
                || hasParamOverride(PARAM_REMINDER_ENTRY_URL)
                || hasParamOverride(PARAM_CHECKIN_TEMPLATE_ID)
                || hasParamOverride(PARAM_CHECKOUT_TEMPLATE_ID)) {
            return "sys_param";
        }
        return attendanceReminderProperties.resolveConfigSourceHint();
    }

    private boolean hasParamOverride(String code) {
        return getParamRawValue(code) != null;
    }

    private String getParamRawValue(String code) {
        return paramService.getByCode(code);
    }

    private boolean isTruthy(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return false;
        }
        String lowerValue = normalized.toLowerCase();
        return "true".equals(lowerValue)
                || "1".equals(lowerValue)
                || "yes".equals(lowerValue)
                || "y".equals(lowerValue)
                || "on".equals(lowerValue);
    }

    private boolean isWorkday(LocalDate date) {
        if (date == null) {
            return false;
        }
        if (attendanceWorkdayService != null) {
            try {
                return attendanceWorkdayService.isWorkday(date);
            } catch (RuntimeException ex) {
                log.warn("resolve attendance reminder workday failed date={}, fallback to weekday/weekend rule", date, ex);
            }
        }
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY;
    }

    private void requireAdmin() {
        LoginUser loginUser = currentUserFacade.currentLoginUser();
        if (!permissionService.isSuperAdmin(loginUser.getUserId())) {
            throw new IllegalArgumentException("仅管理员可执行考勤提醒操作");
        }
        UserEntity user = userMapper.findById(loginUser.getUserId());
        if (user == null) {
            throw new IllegalArgumentException("当前登录用户不存在");
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean hasText(String value) {
        return trimToNull(value) != null;
    }

    private String previewValue(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return "";
        }
        if (normalized.length() <= 6) {
            return normalized.substring(0, Math.min(2, normalized.length())) + "***";
        }
        return normalized.substring(0, 4) + "***" + normalized.substring(normalized.length() - 2);
    }

    private String maskOpenid(String openid) {
        String value = trimToNull(openid);
        if (value == null) {
            return "";
        }
        if (value.length() <= 10) {
            return value.substring(0, Math.min(2, value.length())) + "****";
        }
        return value.substring(0, 6) + "****" + value.substring(value.length() - 4);
    }

    private static final class ReminderDecision {
        private final boolean shouldSend;
        private final String action;
        private final String reason;

        private ReminderDecision(boolean shouldSend, String action, String reason) {
            this.shouldSend = shouldSend;
            this.action = action;
            this.reason = reason;
        }

        public static ReminderDecision send(String action) {
            return new ReminderDecision(true, action, null);
        }

        public static ReminderDecision skip(String action, String reason) {
            return new ReminderDecision(false, action, reason);
        }

        public boolean shouldSend() {
            return shouldSend;
        }

        public String action() {
            return action;
        }

        public String reason() {
            return reason;
        }
    }

    private static final class DispatchOutcome {
        private final boolean success;
        private final String sendStatus;
        private final String wxErrCode;
        private final String wxErrMsg;
        private final String targetUrl;

        private DispatchOutcome(boolean success, String sendStatus, String wxErrCode, String wxErrMsg, String targetUrl) {
            this.success = success;
            this.sendStatus = sendStatus;
            this.wxErrCode = wxErrCode;
            this.wxErrMsg = wxErrMsg;
            this.targetUrl = targetUrl;
        }

        public boolean success() {
            return success;
        }

        public String sendStatus() {
            return sendStatus;
        }

        public String wxErrCode() {
            return wxErrCode;
        }

        public String wxErrMsg() {
            return wxErrMsg;
        }

        public String targetUrl() {
            return targetUrl;
        }
    }
}
