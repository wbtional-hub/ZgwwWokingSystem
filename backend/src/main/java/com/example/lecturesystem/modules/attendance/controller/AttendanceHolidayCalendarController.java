package com.example.lecturesystem.modules.attendance.controller;

import com.example.lecturesystem.common.ApiResponse;
import com.example.lecturesystem.modules.attendance.dto.SaveAttendanceHolidayCalendarRequest;
import com.example.lecturesystem.modules.attendance.service.AttendanceHolidayCalendarService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/attendance/holiday-calendar")
public class AttendanceHolidayCalendarController {
    private final AttendanceHolidayCalendarService holidayCalendarService;

    public AttendanceHolidayCalendarController(AttendanceHolidayCalendarService holidayCalendarService) {
        this.holidayCalendarService = holidayCalendarService;
    }

    @GetMapping
    public ApiResponse<?> queryByYear(@RequestParam("year") Integer year) {
        return ApiResponse.success(holidayCalendarService.queryByYear(year));
    }

    @PostMapping("/generate-ai")
    public ApiResponse<?> generateAiCandidates(@RequestParam("year") Integer year) {
        return ApiResponse.success(holidayCalendarService.generateAiCandidates(year));
    }

    @PostMapping("/save")
    public ApiResponse<?> save(@Validated @RequestBody SaveAttendanceHolidayCalendarRequest request) {
        return ApiResponse.success(holidayCalendarService.save(request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<?> delete(@PathVariable Long id) {
        holidayCalendarService.delete(id);
        return ApiResponse.success("ok");
    }

    @PostMapping("/confirm-year")
    public ApiResponse<?> confirmYear(@RequestParam("year") Integer year) {
        return ApiResponse.success(holidayCalendarService.confirmYear(year));
    }
}
