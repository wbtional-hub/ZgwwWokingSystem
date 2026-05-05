package com.example.lecturesystem.modules.attendance.service;

import com.example.lecturesystem.modules.attendance.entity.AttendancePatchApplyEntity;
import com.example.lecturesystem.modules.attendance.entity.AttendancePatchApplyNodeEntity;

public interface AttendancePatchApprovalNoticeService {
    void notifyPendingApproval(AttendancePatchApplyEntity applyEntity, AttendancePatchApplyNodeEntity pendingNode);
}
