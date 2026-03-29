package com.example.lecturesystem.modules.skill.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class SaveSkillTestCaseRequest {
    private Long id;

    @NotNull(message = "技能ID不能为空")
    private Long skillId;

    @NotNull(message = "技能版本ID不能为空")
    private Long skillVersionId;

    @NotBlank(message = "用例类型不能为空")
    private String caseType;

    @NotBlank(message = "验证问题不能为空")
    private String questionText;

    private String expectedPoints;
    private String expectedFormat;
    private String standardAnswer;
    private Integer status = 1;

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
}