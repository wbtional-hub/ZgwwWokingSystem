package com.example.lecturesystem.modules.skill.vo;

public class SkillTestCaseListItemVO {
    private Long id;
    private String caseType;
    private String questionText;
    private String expectedPoints;
    private String expectedFormat;
    private String standardAnswer;
    private Integer status;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
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
}