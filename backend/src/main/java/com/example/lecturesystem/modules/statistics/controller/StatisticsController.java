package com.example.lecturesystem.modules.statistics.controller;

import com.example.lecturesystem.common.ApiResponse;
import com.example.lecturesystem.modules.statistics.service.StatisticsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {
    private final StatisticsService statisticsService;

    public StatisticsController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @GetMapping("/overview")
    public ApiResponse<?> overview(@RequestParam(required = false) String weekNo,
                                   @RequestParam(required = false) String unitName) {
        return ApiResponse.success(statisticsService.overview(weekNo, unitName));
    }

    @GetMapping("/orgRank")
    public ApiResponse<?> orgRank(@RequestParam(required = false) String weekNo,
                                  @RequestParam(required = false) String unitName) {
        return ApiResponse.success(statisticsService.orgRank(weekNo, unitName));
    }

    @GetMapping("/redList")
    public ApiResponse<?> redList(@RequestParam(required = false) String weekNo,
                                  @RequestParam(required = false) String unitName) {
        return ApiResponse.success(statisticsService.redList(weekNo, unitName));
    }

    @GetMapping("/yellowList")
    public ApiResponse<?> yellowList(@RequestParam(required = false) String weekNo,
                                     @RequestParam(required = false) String unitName) {
        return ApiResponse.success(statisticsService.yellowList(weekNo, unitName));
    }

    @GetMapping("/trend")
    public ApiResponse<?> trend(@RequestParam(required = false) String unitName) {
        return ApiResponse.success(statisticsService.trend(unitName));
    }
}
