package com.example.lecturesystem.modules.attendance.controller;

import com.example.lecturesystem.common.ApiResponse;
import com.example.lecturesystem.modules.attendance.dto.AttendanceQueryRequest;
import com.example.lecturesystem.modules.attendance.dto.CheckInRequest;
import com.example.lecturesystem.modules.attendance.dto.SaveAttendanceRequest;
import com.example.lecturesystem.modules.attendance.service.AttendanceService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {
    private final AttendanceService attendanceService;

    public AttendanceController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
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
}
