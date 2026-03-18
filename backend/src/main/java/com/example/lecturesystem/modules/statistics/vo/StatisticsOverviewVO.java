package com.example.lecturesystem.modules.statistics.vo;

import java.math.BigDecimal;

public class StatisticsOverviewVO {
    private Long scoreCount;
    private BigDecimal averageScore;
    private Long redCount;
    private Long normalCount;
    private Long yellowCount;

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

    public Long getNormalCount() {
        return normalCount;
    }

    public void setNormalCount(Long normalCount) {
        this.normalCount = normalCount;
    }

    public Long getYellowCount() {
        return yellowCount;
    }

    public void setYellowCount(Long yellowCount) {
        this.yellowCount = yellowCount;
    }
}
