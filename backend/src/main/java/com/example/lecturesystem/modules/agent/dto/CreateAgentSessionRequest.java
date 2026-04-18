package com.example.lecturesystem.modules.agent.dto;

public class CreateAgentSessionRequest {
    private Long skillId;
    private Long baseId;
    private String question;
    private String skillHint;
    private String sourceScene;

    public Long getSkillId() { return skillId; }
    public void setSkillId(Long skillId) { this.skillId = skillId; }
    public Long getBaseId() { return baseId; }
    public void setBaseId(Long baseId) { this.baseId = baseId; }
    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
    public String getSkillHint() { return skillHint; }
    public void setSkillHint(String skillHint) { this.skillHint = skillHint; }
    public String getSourceScene() { return sourceScene; }
    public void setSourceScene(String sourceScene) { this.sourceScene = sourceScene; }
}
