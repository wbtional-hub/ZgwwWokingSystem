package com.example.lecturesystem.modules.attendance.service.impl;

import com.example.lecturesystem.modules.attendance.dto.AttendancePatchApplyQueryRequest;
import com.example.lecturesystem.modules.attendance.dto.ReviewAttendancePatchApplyRequest;
import com.example.lecturesystem.modules.attendance.dto.SubmitAttendancePatchApplyRequest;
import com.example.lecturesystem.modules.attendance.entity.AttendancePatchApplyEntity;
import com.example.lecturesystem.modules.attendance.entity.AttendanceRecordEntity;
import com.example.lecturesystem.modules.attendance.mapper.AttendanceMapper;
import com.example.lecturesystem.modules.attendance.mapper.AttendancePatchApplyMapper;
import com.example.lecturesystem.modules.attendance.service.AttendancePatchApplyService;
import com.example.lecturesystem.modules.attendance.support.AttendanceCheckInStatus;
import com.example.lecturesystem.modules.attendance.vo.AttendancePatchApplyDetailVO;
import com.example.lecturesystem.modules.attendance.vo.AttendancePatchApplyPageVO;
import com.example.lecturesystem.modules.permission.support.CurrentUserFacade;
import com.example.lecturesystem.modules.permission.support.DataScopeService;
import com.example.lecturesystem.modules.user.entity.UserEntity;
import com.example.lecturesystem.modules.user.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class AttendancePatchApplyServiceImpl implements AttendancePatchApplyService {
    private static final String PATCH_TYPE_CHECK_IN = "CHECK_IN";
    private static final String PATCH_TYPE_CHECK_OUT = "CHECK_OUT";
    private static final String PATCH_STATUS_PENDING = "PENDING";
    private static final String PATCH_STATUS_APPROVED = "APPROVED";
    private static final String PATCH_STATUS_REJECTED = "REJECTED";
    private static final String PATCH_ADDRESS_TEXT = "补打卡审批";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AttendancePatchApplyMapper attendancePatchApplyMapper;
    private final AttendanceMapper attendanceMapper;
    private final CurrentUserFacade currentUserFacade;
    private final DataScopeService dataScopeService;
    private final UserMapper userMapper;

    public AttendancePatchApplyServiceImpl(AttendancePatchApplyMapper attendancePatchApplyMapper,
                                           AttendanceMapper attendanceMapper,
                                           CurrentUserFacade currentUserFacade,
                                           DataScopeService dataScopeService,
                                           UserMapper userMapper) {
        this.attendancePatchApplyMapper = attendancePatchApplyMapper;
        this.attendanceMapper = attendanceMapper;
        this.currentUserFacade = currentUserFacade;
        this.dataScopeService = dataScopeService;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional
    public Long submitApply(SubmitAttendancePatchApplyRequest request) {
        UserEntity currentUser = currentUserFacade.currentUserEntity();
        if (currentUser.getUnitId() == null) {
            throw new IllegalArgumentException("当前用户未绑定单位，无法提交补卡申请");
        }
        LocalDate attendanceDate = parseDate(request.getAttendanceDate());
        LocalDateTime patchTime = parseDateTime(request.getPatchTime());
        String patchType = normalizePatchType(request.getPatchType());
        String reason = normalizeText(request.getReason());
        if (reason == null) {
            throw new IllegalArgumentException("补卡原因不能为空");
        }
        if (!attendanceDate.equals(patchTime.toLocalDate())) {
            throw new IllegalArgumentException("补卡时间必须属于考勤日期当天");
        }
        if (attendancePatchApplyMapper.findPendingByUserDateType(currentUser.getId(), attendanceDate, patchType) != null) {
            throw new IllegalArgumentException("当天该类型补卡申请已存在待审批记录，请勿重复提交");
        }
        AttendanceRecordEntity existedRecord = attendanceMapper.findByUserIdAndDate(currentUser.getId(), attendanceDate);
        if (PATCH_TYPE_CHECK_IN.equals(patchType) && existedRecord != null && existedRecord.getCheckInTime() != null) {
            throw new IllegalArgumentException("当天已存在上班打卡记录，无需补上班卡");
        }
        if (PATCH_TYPE_CHECK_OUT.equals(patchType) && existedRecord != null && existedRecord.getCheckOutTime() != null) {
            throw new IllegalArgumentException("当天已存在下班打卡记录，无需补下班卡");
        }

        AttendancePatchApplyEntity entity = new AttendancePatchApplyEntity();
        entity.setUserId(currentUser.getId());
        entity.setUnitId(currentUser.getUnitId());
        entity.setAttendanceDate(attendanceDate);
        entity.setPatchType(patchType);
        entity.setPatchTime(patchTime);
        entity.setReason(reason);
        entity.setStatus(PATCH_STATUS_PENDING);
        entity.setValidFlag(1);
        attendancePatchApplyMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public Object queryMyPage(AttendancePatchApplyQueryRequest request) {
        UserEntity currentUser = currentUserFacade.currentUserEntity();
        AttendancePatchApplyQueryRequest normalized = normalizeQueryRequest(request);
        AttendancePatchApplyPageVO page = new AttendancePatchApplyPageVO();
        page.setPageNo(normalized.getPageNo());
        page.setPageSize(normalized.getPageSize());
        page.setTotal(attendancePatchApplyMapper.countMyPage(currentUser.getId(), normalized));
        page.setList(attendancePatchApplyMapper.queryMyPage(currentUser.getId(), normalized));
        return page;
    }

    @Override
    public Object queryPendingPage(AttendancePatchApplyQueryRequest request) {
        UserEntity currentUser = currentUserFacade.currentUserEntity();
        AttendancePatchApplyQueryRequest normalized = normalizeQueryRequest(request);
        if (normalizeText(normalized.getStatus()) == null) {
            normalized.setStatus(PATCH_STATUS_PENDING);
        }
        String treePathPrefix = dataScopeService.buildTreePathPrefix(currentUser);
        AttendancePatchApplyPageVO page = new AttendancePatchApplyPageVO();
        page.setPageNo(normalized.getPageNo());
        page.setPageSize(normalized.getPageSize());
        page.setTotal(attendancePatchApplyMapper.countPendingPage(treePathPrefix, currentUser.getId(), normalized));
        page.setList(attendancePatchApplyMapper.queryPendingPage(treePathPrefix, currentUser.getId(), normalized));
        return page;
    }

    @Override
    public Object detail(Long id) {
        AttendancePatchApplyEntity entity = requirePatchApply(id);
        UserEntity currentUser = currentUserFacade.currentUserEntity();
        validateReadableApply(currentUser, entity);
        AttendancePatchApplyDetailVO detail = attendancePatchApplyMapper.detailById(id);
        if (detail == null) {
            throw new IllegalArgumentException("补卡申请不存在");
        }
        return detail;
    }

    @Override
    @Transactional
    public void approve(Long id, ReviewAttendancePatchApplyRequest request) {
        AttendancePatchApplyEntity entity = requirePatchApply(id);
        UserEntity currentUser = currentUserFacade.currentUserEntity();
        validateReviewableApply(currentUser, entity);
        applyApprovedAttendance(entity);
        entity.setStatus(PATCH_STATUS_APPROVED);
        entity.setApproveUserId(currentUser.getId());
        entity.setApproveTime(LocalDateTime.now());
        entity.setApproveComment(normalizeText(request == null ? null : request.getApproveComment()));
        entity.setUpdateTime(LocalDateTime.now());
        attendancePatchApplyMapper.updateReview(entity);
    }

    @Override
    @Transactional
    public void reject(Long id, ReviewAttendancePatchApplyRequest request) {
        AttendancePatchApplyEntity entity = requirePatchApply(id);
        UserEntity currentUser = currentUserFacade.currentUserEntity();
        validateReviewableApply(currentUser, entity);
        String comment = normalizeText(request == null ? null : request.getApproveComment());
        if (comment == null) {
            throw new IllegalArgumentException("拒绝补卡时请填写审批意见");
        }
        entity.setStatus(PATCH_STATUS_REJECTED);
        entity.setApproveUserId(currentUser.getId());
        entity.setApproveTime(LocalDateTime.now());
        entity.setApproveComment(comment);
        entity.setUpdateTime(LocalDateTime.now());
        attendancePatchApplyMapper.updateReview(entity);
    }

    private void applyApprovedAttendance(AttendancePatchApplyEntity applyEntity) {
        AttendanceRecordEntity record = attendanceMapper.findByUserIdAndDate(applyEntity.getUserId(), applyEntity.getAttendanceDate());
        if (record == null) {
            if (!PATCH_TYPE_CHECK_IN.equals(applyEntity.getPatchType())) {
                throw new IllegalArgumentException("请先有上班记录，或先补上班卡");
            }
            AttendanceRecordEntity insertEntity = new AttendanceRecordEntity();
            insertEntity.setUnitId(applyEntity.getUnitId());
            insertEntity.setUserId(applyEntity.getUserId());
            insertEntity.setAttendanceDate(applyEntity.getAttendanceDate());
            insertEntity.setCheckInTime(applyEntity.getPatchTime());
            insertEntity.setCheckInAddress(PATCH_ADDRESS_TEXT);
            insertEntity.setCheckType(PATCH_TYPE_CHECK_IN);
            insertEntity.setCheckTime(applyEntity.getPatchTime());
            insertEntity.setCheckInResult(AttendanceCheckInStatus.CHECK_IN_SUCCESS);
            insertEntity.setLocationSource("PATCH_APPROVAL");
            insertEntity.setLocationProvider("BACKOFFICE");
            insertEntity.setValidFlag(1);
            attendanceMapper.insert(insertEntity);
            return;
        }

        if (PATCH_TYPE_CHECK_IN.equals(applyEntity.getPatchType())) {
            if (record.getCheckInTime() != null) {
                throw new IllegalArgumentException("当天已存在上班打卡记录，不能重复补上班卡");
            }
            record.setCheckInTime(applyEntity.getPatchTime());
            if (normalizeText(record.getCheckInAddress()) == null) {
                record.setCheckInAddress(PATCH_ADDRESS_TEXT);
            }
        } else {
            if (record.getCheckInTime() == null) {
                throw new IllegalArgumentException("请先有上班记录，或先补上班卡");
            }
            if (record.getCheckOutTime() != null) {
                throw new IllegalArgumentException("当天已存在下班打卡记录，不能重复补下班卡");
            }
            if (!applyEntity.getPatchTime().isAfter(record.getCheckInTime())) {
                throw new IllegalArgumentException("补下班卡时间必须晚于上班时间");
            }
            record.setCheckOutTime(applyEntity.getPatchTime());
            if (normalizeText(record.getCheckOutAddress()) == null) {
                record.setCheckOutAddress(PATCH_ADDRESS_TEXT);
            }
        }
        record.setCheckType(resolveCheckType(record.getCheckInTime(), record.getCheckOutTime()));
        record.setCheckTime(resolveCheckTime(record.getCheckType(), record.getCheckInTime(), record.getCheckOutTime()));
        record.setCheckInResult(resolveRecordStatus(record));
        record.setCheckInFailReason(null);
        record.setLocationSource("PATCH_APPROVAL");
        record.setLocationProvider("BACKOFFICE");
        if (record.getValidFlag() == null) {
            record.setValidFlag(1);
        }
        attendanceMapper.update(record);
    }

    private AttendancePatchApplyEntity requirePatchApply(Long id) {
        AttendancePatchApplyEntity entity = attendancePatchApplyMapper.findById(id);
        if (entity == null || entity.getValidFlag() == null || entity.getValidFlag() != 1) {
            throw new IllegalArgumentException("补卡申请不存在");
        }
        return entity;
    }

    private void validateReadableApply(UserEntity currentUser, AttendancePatchApplyEntity entity) {
        if (currentUser.getId().equals(entity.getUserId())) {
            return;
        }
        UserEntity targetUser = requireUser(entity.getUserId());
        dataScopeService.validateReadableUser(currentUser, targetUser, "无权查看该补卡申请");
    }

    private void validateReviewableApply(UserEntity currentUser, AttendancePatchApplyEntity entity) {
        if (!PATCH_STATUS_PENDING.equals(entity.getStatus())) {
            throw new IllegalArgumentException("该补卡申请已处理，请勿重复审批");
        }
        if (currentUser.getId().equals(entity.getUserId())) {
            throw new IllegalArgumentException("不能审批自己的补卡申请");
        }
        UserEntity targetUser = requireUser(entity.getUserId());
        dataScopeService.validateReadableUser(currentUser, targetUser, "无权审批该补卡申请");
    }

    private UserEntity requireUser(Long userId) {
        UserEntity user = userMapper.findById(userId);
        if (user == null) {
            throw new IllegalArgumentException("申请用户不存在");
        }
        return user;
    }

    private AttendancePatchApplyQueryRequest normalizeQueryRequest(AttendancePatchApplyQueryRequest request) {
        AttendancePatchApplyQueryRequest normalized = request == null ? new AttendancePatchApplyQueryRequest() : request;
        if (normalized.getPageNo() == null || normalized.getPageNo() < 1) {
            normalized.setPageNo(1);
        }
        if (normalized.getPageSize() == null || normalized.getPageSize() < 1) {
            normalized.setPageSize(10);
        }
        if (normalized.getPageSize() > 100) {
            normalized.setPageSize(100);
        }
        validateDateRange(normalized.getDateFrom(), normalized.getDateTo());
        return normalized;
    }

    private void validateDateRange(String dateFrom, String dateTo) {
        LocalDate from = normalizeText(dateFrom) == null ? null : parseDate(dateFrom);
        LocalDate to = normalizeText(dateTo) == null ? null : parseDate(dateTo);
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("开始日期不能晚于结束日期");
        }
    }

    private String resolveCheckType(LocalDateTime checkInTime, LocalDateTime checkOutTime) {
        return checkOutTime != null ? PATCH_TYPE_CHECK_OUT : PATCH_TYPE_CHECK_IN;
    }

    private LocalDateTime resolveCheckTime(String checkType, LocalDateTime checkInTime, LocalDateTime checkOutTime) {
        if (PATCH_TYPE_CHECK_OUT.equals(checkType) && checkOutTime != null) {
            return checkOutTime;
        }
        return checkInTime != null ? checkInTime : checkOutTime;
    }

    private String resolveRecordStatus(AttendanceRecordEntity record) {
        return record.getCheckOutTime() == null ? AttendanceCheckInStatus.CHECK_IN_SUCCESS : AttendanceCheckInStatus.CHECK_OUT_SUCCESS;
    }

    private LocalDate parseDate(String text) {
        return LocalDate.parse(text);
    }

    private LocalDateTime parseDateTime(String text) {
        String normalized = normalizeText(text);
        if (normalized == null) {
            throw new IllegalArgumentException("补卡时间不能为空");
        }
        String value = normalized.replace('T', ' ');
        if (value.length() == 16) {
            value = value + ":00";
        }
        return LocalDateTime.parse(value, DATE_TIME_FORMATTER);
    }

    private String normalizePatchType(String patchType) {
        String normalized = normalizeText(patchType);
        if (normalized == null) {
            throw new IllegalArgumentException("补卡类型不能为空");
        }
        String upper = normalized.toUpperCase();
        if (!PATCH_TYPE_CHECK_IN.equals(upper) && !PATCH_TYPE_CHECK_OUT.equals(upper)) {
            throw new IllegalArgumentException("补卡类型不合法");
        }
        return upper;
    }

    private String normalizeText(String text) {
        if (text == null) {
            return null;
        }
        String normalized = text.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
