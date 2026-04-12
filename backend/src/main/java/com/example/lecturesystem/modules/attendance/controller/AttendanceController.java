package com.example.lecturesystem.modules.attendance.controller;

import com.example.lecturesystem.common.ApiResponse;
import com.example.lecturesystem.modules.attendance.dto.AttendanceQueryRequest;
import com.example.lecturesystem.modules.attendance.dto.AttendancePatchApplyQueryRequest;
import com.example.lecturesystem.modules.attendance.dto.AttendanceStatsQueryRequest;
import com.example.lecturesystem.modules.attendance.dto.CheckInRequest;
import com.example.lecturesystem.modules.attendance.dto.ReviewAttendancePatchApplyRequest;
import com.example.lecturesystem.modules.attendance.dto.SaveAttendanceRequest;
import com.example.lecturesystem.modules.attendance.dto.SaveAttendanceRuleRequest;
import com.example.lecturesystem.modules.attendance.dto.SubmitAttendancePatchApplyRequest;
import com.example.lecturesystem.modules.attendance.service.AttendancePatchApplyService;
import com.example.lecturesystem.modules.attendance.service.AttendanceRuleService;
import com.example.lecturesystem.modules.attendance.service.AttendanceService;
import com.example.lecturesystem.modules.attendance.service.AttendanceStatisticsService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {
    private final AttendanceService attendanceService;
    private final AttendancePatchApplyService attendancePatchApplyService;
    private final AttendanceRuleService attendanceRuleService;
    private final AttendanceStatisticsService attendanceStatisticsService;

    public AttendanceController(AttendanceService attendanceService,
                                AttendancePatchApplyService attendancePatchApplyService,
                                AttendanceRuleService attendanceRuleService,
                                AttendanceStatisticsService attendanceStatisticsService) {
        this.attendanceService = attendanceService;
        this.attendancePatchApplyService = attendancePatchApplyService;
        this.attendanceRuleService = attendanceRuleService;
        this.attendanceStatisticsService = attendanceStatisticsService;
    }

    @GetMapping("/current-location")
    public ApiResponse<?> queryCurrentAttendanceLocation() {
        return ApiResponse.success(attendanceService.queryCurrentAttendanceLocation());
    }

    @PostMapping("/check-in")
    public ApiResponse<?> checkIn(@RequestBody CheckInRequest request) {
        return ApiResponse.success(attendanceService.checkIn(request));
    }

    @PostMapping("/query")
    public ApiResponse<?> query(@RequestBody AttendanceQueryRequest request) {
        return ApiResponse.success(attendanceService.query(request));
    }

    @PostMapping("/summary")
    public ApiResponse<?> querySummary(@RequestBody AttendanceQueryRequest request) {
        return ApiResponse.success(attendanceService.querySummary(request));
    }

    @PostMapping("/abnormal-monitor")
    public ApiResponse<?> queryAbnormalMonitor(@RequestBody AttendanceQueryRequest request) {
        return ApiResponse.success(attendanceService.queryAbnormalMonitor(request));
    }

    @PostMapping("/abnormal-trend")
    public ApiResponse<?> queryAbnormalTrend(@RequestBody AttendanceQueryRequest request) {
        return ApiResponse.success(attendanceService.queryAbnormalTrend(request));
    }

    @PostMapping("/abnormal-user-summary")
    public ApiResponse<?> queryAbnormalUserSummary(@RequestBody AttendanceQueryRequest request) {
        return ApiResponse.success(attendanceService.queryAbnormalUserSummary(request));
    }

    @PostMapping("/save")
    public ApiResponse<?> save(@Validated @RequestBody SaveAttendanceRequest request) {
        return ApiResponse.success(attendanceService.saveAttendance(request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<?> delete(@PathVariable Long id) {
        attendanceService.deleteAttendance(id);
        return ApiResponse.success("ok");
    }

    @PostMapping("/patch-apply/submit")
    public ApiResponse<?> submitPatchApply(@Validated @RequestBody SubmitAttendancePatchApplyRequest request) {
        return ApiResponse.success(attendancePatchApplyService.submitApply(request));
    }

    @PostMapping("/patch-apply/my-page")
    public ApiResponse<?> queryMyPatchApplyPage(@RequestBody(required = false) AttendancePatchApplyQueryRequest request) {
        return ApiResponse.success(attendancePatchApplyService.queryMyPage(request));
    }

    @GetMapping("/patch-apply/{id}")
    public ApiResponse<?> queryPatchApplyDetail(@PathVariable Long id) {
        return ApiResponse.success(attendancePatchApplyService.detail(id));
    }

    @PostMapping("/patch-apply/pending-page")
    public ApiResponse<?> queryPendingPatchApplyPage(@RequestBody(required = false) AttendancePatchApplyQueryRequest request) {
        return ApiResponse.success(attendancePatchApplyService.queryPendingPage(request));
    }

    @PostMapping("/patch-apply/{id}/approve")
    public ApiResponse<?> approvePatchApply(@PathVariable Long id,
                                            @RequestBody(required = false) ReviewAttendancePatchApplyRequest request) {
        attendancePatchApplyService.approve(id, request);
        return ApiResponse.success("ok");
    }

    @PostMapping("/patch-apply/{id}/reject")
    public ApiResponse<?> rejectPatchApply(@PathVariable Long id,
                                           @RequestBody(required = false) ReviewAttendancePatchApplyRequest request) {
        attendancePatchApplyService.reject(id, request);
        return ApiResponse.success("ok");
    }

    @GetMapping("/rule/current")
    public ApiResponse<?> queryCurrentAttendanceRule() {
        return ApiResponse.success(attendanceRuleService.queryCurrentRule());
    }

    @PostMapping("/rule/save")
    public ApiResponse<?> saveAttendanceRule(@Validated @RequestBody SaveAttendanceRuleRequest request) {
        return ApiResponse.success(attendanceRuleService.saveCurrentRule(request));
    }

    @PostMapping("/team-statistics/query")
    public ApiResponse<?> queryTeamStatistics(@RequestBody(required = false) AttendanceStatsQueryRequest request) {
        return ApiResponse.success(attendanceStatisticsService.queryTeamStatistics(request));
    }
}
