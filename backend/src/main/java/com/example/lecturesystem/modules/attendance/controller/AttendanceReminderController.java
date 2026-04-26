package com.example.lecturesystem.modules.attendance.controller;

import com.example.lecturesystem.common.ApiResponse;
import com.example.lecturesystem.modules.attendance.service.AttendanceReminderService;
import com.example.lecturesystem.modules.attendance.support.AttendanceReminderType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/attendance/reminder")
public class AttendanceReminderController {
    private final AttendanceReminderService attendanceReminderService;

    public AttendanceReminderController(AttendanceReminderService attendanceReminderService) {
        this.attendanceReminderService = attendanceReminderService;
    }

    @PostMapping("/run-now")
    public ApiResponse<?> runNow(@RequestParam("type") String type) {
        try {
            return ApiResponse.success(attendanceReminderService.runNow(AttendanceReminderType.fromCode(type)));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ApiResponse.fail(ex.getMessage());
        }
    }

    @PostMapping("/test-send")
    public ApiResponse<?> testSend(@RequestBody(required = false) TestSendRequest request,
                                   @RequestParam(value = "userId", required = false) Long userId,
                                   @RequestParam(value = "type", required = false) String type) {
        try {
            Long resolvedUserId = request != null && request.getUserId() != null ? request.getUserId() : userId;
            String resolvedType = request != null && request.getType() != null ? request.getType() : type;
            if (resolvedUserId == null) {
                return ApiResponse.fail("userId不能为空");
            }
            if (resolvedType == null || resolvedType.isBlank()) {
                return ApiResponse.fail("type不能为空");
            }
            return ApiResponse.success(
                    attendanceReminderService.testSend(resolvedUserId, AttendanceReminderType.fromCode(resolvedType))
            );
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ApiResponse.fail(ex.getMessage());
        }
    }

    @GetMapping("/health")
    public ApiResponse<?> health() {
        try {
            return ApiResponse.success(attendanceReminderService.healthCheck());
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ApiResponse.fail(ex.getMessage());
        }
    }

    public static class TestSendRequest {
        private Long userId;
        private String type;

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }
}
