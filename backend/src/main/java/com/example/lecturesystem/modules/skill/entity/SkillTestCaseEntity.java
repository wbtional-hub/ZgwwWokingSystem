package com.example.lecturesystem.modules.skill.entity;

import java.time.LocalDateTime;

public class SkillTestCaseEntity {
    private Long id;
    private Long skillId;
    private Long skillVersionId;
    private String caseType;
    private String questionText;
    private String expectedPoints;
    private String expectedFormat;
    private String standardAnswer;
    private Integer status;
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSkillId() { return skillId; }
    public void setSkillId(Long skillId) { this.skillId = skillId; }
    public Long getSkillVersionId() { return skillVersionId; }
    public void setSkillVersionId(Long skillVersionId) { this.skillVersionId = skillVersionId; }
    public String getCaseType() { return caseType; }
    public void setCaseType(String caseType) { this.caseType = caseType; }
    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }
    public String getExpectedPoints() { return expectedPoints; }
    public void setExpectedPoints(String expectedPoints) { this.expectedPoints = expectedPoints; }
    public String getExpectedFormat() { return expectedFormat; }
    public void setExpectedFormat(String expectedFormat) { this.expectedFormat = expectedFormat; }
    public String getStandardAnswer() { return standardAnswer; }
    public void setStandardAnswer(String standardAnswer) { this.standardAnswer = standardAnswer; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}