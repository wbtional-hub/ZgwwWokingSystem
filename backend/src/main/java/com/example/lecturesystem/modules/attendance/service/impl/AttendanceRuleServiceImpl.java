package com.example.lecturesystem.modules.attendance.service.impl;

import com.example.lecturesystem.modules.attendance.dto.SaveAttendanceRuleRequest;
import com.example.lecturesystem.modules.attendance.entity.AttendanceRuleEntity;
import com.example.lecturesystem.modules.attendance.mapper.AttendanceMapper;
import com.example.lecturesystem.modules.attendance.mapper.AttendanceRuleMapper;
import com.example.lecturesystem.modules.attendance.service.AttendanceRuleService;
import com.example.lecturesystem.modules.attendance.vo.AttendanceRuleVO;
import com.example.lecturesystem.modules.permission.service.PermissionService;
import com.example.lecturesystem.modules.permission.support.CurrentUserFacade;
import com.example.lecturesystem.modules.user.entity.UserEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
public class AttendanceRuleServiceImpl implements AttendanceRuleService {
    private final AttendanceRuleMapper attendanceRuleMapper;
    private final AttendanceMapper attendanceMapper;
    private final CurrentUserFacade currentUserFacade;
    private final PermissionService permissionService;

    public AttendanceRuleServiceImpl(AttendanceRuleMapper attendanceRuleMapper,
                                     AttendanceMapper attendanceMapper,
                                     CurrentUserFacade currentUserFacade,
                                     PermissionService permissionService) {
        this.attendanceRuleMapper = attendanceRuleMapper;
        this.attendanceMapper = attendanceMapper;
        this.currentUserFacade = currentUserFacade;
        this.permissionService = permissionService;
    }

    @Override
    public Object queryCurrentRule() {
        UserEntity currentUser = currentUserFacade.currentUserEntity();
        Long unitId = requireCurrentUnitId(currentUser);
        AttendanceRuleVO detail = attendanceRuleMapper.detailByUnitId(unitId);
        if (detail != null) {
            return detail;
        }
        AttendanceRuleVO empty = new AttendanceRuleVO();
        empty.setUnitId(unitId);
        empty.setUnitName(attendanceMapper.findUnitNameById(unitId));
        empty.setStatus(1);
        empty.setLateGraceMinutes(0);
        empty.setEarlyLeaveGraceMinutes(0);
        return empty;
    }

    @Override
    @Transactional
    public Long saveCurrentRule(SaveAttendanceRuleRequest request) {
        UserEntity currentUser = currentUserFacade.currentUserEntity();
        requireRuleManagePermission(currentUser);
        Long unitId = requireCurrentUnitId(currentUser);
        LocalTime workStartTime = parseLocalTime(request.getWorkStartTime(), "上班时间不能为空");
        LocalTime workEndTime = parseLocalTime(request.getWorkEndTime(), "下班时间不能为空");
        if (!workEndTime.isAfter(workStartTime)) {
            throw new IllegalArgumentException("下班时间必须晚于上班时间");
        }
        Integer lateGraceMinutes = normalizeMinutes(request.getLateGraceMinutes(), "迟到宽限分钟不能小于 0");
        Integer earlyLeaveGraceMinutes = normalizeMinutes(request.getEarlyLeaveGraceMinutes(), "早退宽限分钟不能小于 0");
        AttendanceRuleEntity existed = attendanceRuleMapper.findByUnitId(unitId);
        if (existed == null) {
            AttendanceRuleEntity entity = new AttendanceRuleEntity();
            entity.setUnitId(unitId);
            entity.setWorkStartTime(workStartTime);
            entity.setWorkEndTime(workEndTime);
            entity.setLateGraceMinutes(lateGraceMinutes);
            entity.setEarlyLeaveGraceMinutes(earlyLeaveGraceMinutes);
            entity.setStatus(request.getStatus() == null ? 1 : request.getStatus());
            entity.setCreateTime(LocalDateTime.now());
            entity.setUpdateTime(LocalDateTime.now());
            attendanceRuleMapper.insert(entity);
            return entity.getId();
        }
        existed.setWorkStartTime(workStartTime);
        existed.setWorkEndTime(workEndTime);
        existed.setLateGraceMinutes(lateGraceMinutes);
        existed.setEarlyLeaveGraceMinutes(earlyLeaveGraceMinutes);
        existed.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        existed.setUpdateTime(LocalDateTime.now());
        attendanceRuleMapper.update(existed);
        return existed.getId();
    }

    private void requireRuleManagePermission(UserEntity currentUser) {
        boolean superAdmin = permissionService.isSuperAdmin(currentUser.getId());
        boolean unitAdmin = permissionService.isUnitAdmin(currentUser.getId());
        if (!superAdmin && !unitAdmin) {
            throw new IllegalArgumentException("当前用户无权配置考勤规则");
        }
    }

    private Long requireCurrentUnitId(UserEntity currentUser) {
        if (currentUser.getUnitId() == null) {
            throw new IllegalArgumentException("当前用户未绑定单位，无法配置考勤规则");
        }
        return currentUser.getUnitId();
    }

    private Integer normalizeMinutes(Integer value, String message) {
        if (value == null) {
            return 0;
        }
        if (value < 0) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private LocalTime parseLocalTime(String text, String emptyMessage) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException(emptyMessage);
        }
        String normalized = text.trim();
        if (normalized.length() == 5) {
            normalized = normalized + ":00";
        }
        return LocalTime.parse(normalized);
    }
}
