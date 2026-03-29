package com.example.lecturesystem.modules.statistics.service;

public interface StatisticsService {
    Object overview();

    Object overview(String weekNo, String unitName);

    Object orgRank(String weekNo, String unitName);

    Object redList(String weekNo, String unitName);

    Object yellowList(String weekNo, String unitName);

    Object trend(String weekNo, String unitName);
}
