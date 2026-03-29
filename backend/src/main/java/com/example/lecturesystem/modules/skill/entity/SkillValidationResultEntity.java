package com.example.lecturesystem.modules.skill.entity;

import java.math.BigDecimal;

public class SkillValidationResultEntity {
    private Long id;
    private Long runId;
    private Long testCaseId;
    private String answerText;
    private String hitChunks;
    private BigDecimal score;
    private Boolean isPass;
    private String failReason;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getRunId() { return runId; }
    public void setRunId(Long runId) { this.runId = runId; }
    public Long getTestCaseId() { return testCaseId; }
    public void setTestCaseId(Long testCaseId) { this.testCaseId = testCaseId; }
    public String getAnswerText() { return answerText; }
    public void setAnswerText(String answerText) { this.answerText = answerText; }
    public String getHitChunks() { return hitChunks; }
    public void setHitChunks(String hitChunks) { this.hitChunks = hitChunks; }
    public BigDecimal getScore() { return score; }
    public void setScore(BigDecimal score) { this.score = score; }
    public Boolean getIsPass() { return isPass; }
    public void setIsPass(Boolean isPass) { this.isPass = isPass; }
    public String getFailReason() { return failReason; }
    public void setFailReason(String failReason) { this.failReason = failReason; }
}