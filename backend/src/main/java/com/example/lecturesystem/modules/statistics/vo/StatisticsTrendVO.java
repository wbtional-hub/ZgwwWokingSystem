package com.example.lecturesystem.modules.statistics.vo;

import java.math.BigDecimal;

public class StatisticsTrendVO {
    private String weekNo;
    private Long scoreCount;
    private BigDecimal averageScore;
    private Long redCount;
    private Long yellowCount;

    public String getWeekNo() {
        return weekNo;
    }

    public void setWeekNo(String weekNo) {
        this.weekNo = weekNo;
    }

    public Long getScoreCount() {
        return scoreCount;
    }

    public void setScoreCount(Long scoreCount) {
        this.scoreCount = scoreCount;
    }

    public BigDecimal getAverageScore() {
        return averageScore;
    }

    public void setAverageScore(BigDecimal averageScore) {
        this.averageScore = averageScore;
    }

    public Long getRedCount() {
        return redCount;
    }

    public void setRedCount(Long redCount) {
        this.redCount = redCount;
    }

    public Long getYellowCount() {
        return yellowCount;
    }

    public void setYellowCount(Long yellowCount) {
        this.yellowCount = yellowCount;
    }
}
