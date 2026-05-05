package com.example.lecturesystem.modules.attendance.service.impl;

import com.example.lecturesystem.modules.attendance.entity.AttendancePatchApplyEntity;
import com.example.lecturesystem.modules.attendance.entity.AttendancePatchApplyNodeEntity;
import com.example.lecturesystem.modules.attendance.service.AttendancePatchApprovalNoticeService;
import com.example.lecturesystem.modules.attendance.support.AttendanceApplyType;
import com.example.lecturesystem.modules.auth.service.WechatTemplateMessageService;
import com.example.lecturesystem.modules.param.service.ParamService;
import com.example.lecturesystem.modules.user.entity.UserEntity;
import com.example.lecturesystem.modules.user.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AttendancePatchApprovalNoticeServiceImpl implements AttendancePatchApprovalNoticeService {
    private static final Logger log = LoggerFactory.getLogger(AttendancePatchApprovalNoticeServiceImpl.class);

    private static final String PARAM_NOTICE_ENABLED = "ATTENDANCE_PATCH_APPROVAL_NOTICE_ENABLED";
    private static final String PARAM_TEMPLATE_ID = "ATTENDANCE_PATCH_APPROVAL_TEMPLATE_ID";
    private static final String PARAM_ENTRY_URL = "ATTENDANCE_PATCH_APPROVAL_ENTRY_URL";
    private static final String PARAM_REMINDER_ENTRY_URL = "ATTENDANCE_REMINDER_ENTRY_URL";
    private static final DateTimeFormatter TEMPLATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ParamService paramService;
    private final UserMapper userMapper;
    private final WechatTemplateMessageService wechatTemplateMessageService;

    public AttendancePatchApprovalNoticeServiceImpl(ParamService paramService,
                                                   UserMapper userMapper,
                                                   WechatTemplateMessageService wechatTemplateMessageService) {
        this.paramService = paramService;
        this.userMapper = userMapper;
        this.wechatTemplateMessageService = wechatTemplateMessageService;
    }

    @Override
    public void notifyPendingApproval(AttendancePatchApplyEntity applyEntity, AttendancePatchApplyNodeEntity pendingNode) {
        if (applyEntity == null || pendingNode == null) {
            return;
        }
        try {
            doNotifyPendingApproval(applyEntity, pendingNode);
        } catch (Exception ex) {
            log.warn("attendance patch approval notice failed: applyId={}, nodeId={}, approverUserId={}, reason={}",
                    applyEntity.getId(), pendingNode.getId(), pendingNode.getApproverUserId(), ex.getMessage(), ex);
        }
    }

    private void doNotifyPendingApproval(AttendancePatchApplyEntity applyEntity, AttendancePatchApplyNodeEntity pendingNode) {
        if (!isNoticeEnabled()) {
            log.info("attendance patch approval notice skipped: applyId={}, nodeId={}, approverUserId={}, reason=notice_disabled",
                    applyEntity.getId(), pendingNode.getId(), pendingNode.getApproverUserId());
            return;
        }

        String templateId = trimToNull(paramService.getByCode(PARAM_TEMPLATE_ID));
        if (templateId == null) {
            log.warn("attendance patch approval notice skipped: applyId={}, nodeId={}, approverUserId={}, reason=template_id_missing",
                    applyEntity.getId(), pendingNode.getId(), pendingNode.getApproverUserId());
            return;
        }

        String targetUrl = resolveEntryUrl();
        if (targetUrl == null) {
            log.warn("attendance patch approval notice skipped: applyId={}, nodeId={}, approverUserId={}, reason=entry_url_missing",
                    applyEntity.getId(), pendingNode.getId(), pendingNode.getApproverUserId());
            return;
        }

        UserEntity approver = pendingNode.getApproverUserId() == null ? null : userMapper.findById(pendingNode.getApproverUserId());
        String openId = trimToNull(approver == null ? null : approver.getWechatOpenId());
        if (openId == null) {
            log.warn("attendance patch approval notice skipped: applyId={}, nodeId={}, approverUserId={}, reason=openid_missing",
                    applyEntity.getId(), pendingNode.getId(), pendingNode.getApproverUserId());
            return;
        }

        UserEntity applicant = applyEntity.getUserId() == null ? null : userMapper.findById(applyEntity.getUserId());
        WechatTemplateMessageService.SendRequest request = new WechatTemplateMessageService.SendRequest();
        request.setOpenId(openId);
        request.setTemplateId(templateId);
        request.setUrl(targetUrl);
        request.setData(buildTemplateData(applyEntity, applicant));

        WechatTemplateMessageService.SendResult result = wechatTemplateMessageService.sendTemplateMessage(request);
        if (result.isSuccess()) {
            log.info("attendance patch approval notice sent: applyId={}, nodeId={}, approverUserId={}, templateId={}, wxErrCode={}, wxErrMsg={}",
                    applyEntity.getId(), pendingNode.getId(), pendingNode.getApproverUserId(), templateId,
                    result.getErrCode(), result.getErrMsg());
        } else {
            log.warn("attendance patch approval notice send failed: applyId={}, nodeId={}, approverUserId={}, templateId={}, wxErrCode={}, wxErrMsg={}",
                    applyEntity.getId(), pendingNode.getId(), pendingNode.getApproverUserId(), templateId,
                    result.getErrCode(), result.getErrMsg());
        }
    }

    private Map<String, WechatTemplateMessageService.TemplateDataItem> buildTemplateData(AttendancePatchApplyEntity applyEntity,
                                                                                        UserEntity applicant) {
        Map<String, WechatTemplateMessageService.TemplateDataItem> data = new LinkedHashMap<>();
        String applyTime = formatApplyTime(applyEntity);
        data.put("thing1", new WechatTemplateMessageService.TemplateDataItem(resolveDisplayName(applicant)));
        data.put("time2", new WechatTemplateMessageService.TemplateDataItem(applyTime));
        data.put("time3", new WechatTemplateMessageService.TemplateDataItem(applyTime));
        data.put("thing4", new WechatTemplateMessageService.TemplateDataItem(resolveApplyMatter(applyEntity)));
        data.put("phrase5", new WechatTemplateMessageService.TemplateDataItem("待审核"));
        return data;
    }

    private String resolveDisplayName(UserEntity user) {
        String realName = trimToNull(user == null ? null : user.getRealName());
        if (realName != null) {
            return realName;
        }
        String username = trimToNull(user == null ? null : user.getUsername());
        return username == null ? "员工" : username;
    }

    private String resolveApplyMatter(AttendancePatchApplyEntity applyEntity) {
        String applyType = AttendanceApplyType.normalize(applyEntity == null ? null : applyEntity.getApplyType());
        if (AttendanceApplyType.EVIDENCE.equals(applyType)) {
            return "取证/外勤留痕";
        }
        if (AttendanceApplyType.MAKEUP.equals(applyType)) {
            return "补打卡申请";
        }
        return "补打卡/取证待审核";
    }

    private String formatApplyTime(AttendancePatchApplyEntity applyEntity) {
        LocalDateTime patchTime = applyEntity == null ? null : applyEntity.getPatchTime();
        if (patchTime != null) {
            return patchTime.format(TEMPLATE_TIME_FORMATTER);
        }
        LocalDate attendanceDate = applyEntity == null ? null : applyEntity.getAttendanceDate();
        return (attendanceDate == null ? LocalDate.now() : attendanceDate)
                .atTime(LocalTime.MIDNIGHT)
                .format(TEMPLATE_TIME_FORMATTER);
    }

    private boolean isNoticeEnabled() {
        String value = trimToNull(paramService.getByCode(PARAM_NOTICE_ENABLED));
        if (value == null) {
            log.info("attendance patch approval notice disabled: reason=param_missing");
            return false;
        }
        return "1".equals(value) || "true".equalsIgnoreCase(value);
    }

    private String resolveEntryUrl() {
        String entryUrl = trimToNull(paramService.getByCode(PARAM_ENTRY_URL));
        if (entryUrl != null) {
            return entryUrl;
        }
        return trimToNull(paramService.getByCode(PARAM_REMINDER_ENTRY_URL));
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
