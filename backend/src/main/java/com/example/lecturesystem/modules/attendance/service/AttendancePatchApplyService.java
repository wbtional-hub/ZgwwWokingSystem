package com.example.lecturesystem.modules.attendance.service;

import com.example.lecturesystem.modules.attendance.dto.AttendancePatchApplyQueryRequest;
import com.example.lecturesystem.modules.attendance.dto.ReviewAttendancePatchApplyRequest;
import com.example.lecturesystem.modules.attendance.dto.SubmitAttendancePatchApplyRequest;

public interface AttendancePatchApplyService {
    Long submitApply(SubmitAttendancePatchApplyRequest request);
    Object queryMyPage(AttendancePatchApplyQueryRequest request);
    Object queryPendingPage(AttendancePatchApplyQueryRequest request);
    Object detail(Long id);
    void approve(Long id, ReviewAttendancePatchApplyRequest request);
    void reject(Long id, ReviewAttendancePatchApplyRequest request);
}
