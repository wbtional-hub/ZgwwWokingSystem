package com.example.lecturesystem.modules.skill.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class SkillValidationDetailVO {
    private Long runId;
    private Long skillId;
    private Long skillVersionId;
    private String runStatus;
    private BigDecimal passRate;
    private BigDecimal citationRate;
    private BigDecimal avgScore;
    private LocalDateTime startTime;
    private LocalDateTime finishTime;
    private List<SkillValidationResultVO> results;

    public Long getRunId() { return runId; }
    public void setRunId(Long runId) { this.runId = runId; }
    public Long getSkillId() { return skillId; }
    public void setSkillId(Long skillId) { this.skillId = skillId; }
    public Long getSkillVersionId() { return skillVersionId; }
    public void setSkillVersionId(Long skillVersionId) { this.skillVersionId = skillVersionId; }
    public String getRunStatus() { return runStatus; }
    public void setRunStatus(String runStatus) { this.runStatus = runStatus; }
    public BigDecimal getPassRate() { return passRate; }
    public void setPassRate(BigDecimal passRate) { this.passRate = passRate; }
    public BigDecimal getCitationRate() { return citationRate; }
    public void setCitationRate(BigDecimal citationRate) { this.citationRate = citationRate; }
    public BigDecimal getAvgScore() { return avgScore; }
    public void setAvgScore(BigDecimal avgScore) { this.avgScore = avgScore; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getFinishTime() { return finishTime; }
    public void setFinishTime(LocalDateTime finishTime) { this.finishTime = finishTime; }
    public List<SkillValidationResultVO> getResults() { return results; }
    public void setResults(List<SkillValidationResultVO> results) { this.results = results; }
}