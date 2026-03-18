package com.example.lecturesystem.modules.statistics.vo;

import java.math.BigDecimal;

public class StatisticsOrgRankVO {
    private Long unitId;
    private String unitName;
    private Long scoreCount;
    private BigDecimal averageScore;
    private Long redCount;

    public Long getUnitId() {
        return unitId;
    }

    public void setUnitId(Long unitId) {
        this.unitId = unitId;
    }

    public String getUnitName() {
        return unitName;
    }

    public void setUnitName(String unitName) {
        this.unitName = unitName;
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
}
