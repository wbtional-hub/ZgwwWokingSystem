package com.example.lecturesystem.modules.agent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class AgentChatRequest {
    @NotNull(message = "会话ID不能为空")
    private Long sessionId;

    @NotBlank(message = "问题不能为空")
    private String question;

    private String sourceScene;
    private String skillHint;

    // 前端点击联想问题时可携带；普通手工输入时可以为空
    private Long intentId;
    private String policyKey;
    private String topicType;
    private String questionType;
    private String regionScope;

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getSourceScene() {
        return sourceScene;
    }

    public void setSourceScene(String sourceScene) {
        this.sourceScene = sourceScene;
    }

    public String getSkillHint() {
        return skillHint;
    }

    public void setSkillHint(String skillHint) {
        this.skillHint = skillHint;
    }

    public Long getIntentId() {
        return intentId;
    }

    public void setIntentId(Long intentId) {
        this.intentId = intentId;
    }

    public String getPolicyKey() {
        return policyKey;
    }

    public void setPolicyKey(String policyKey) {
        this.policyKey = policyKey;
    }

    public String getTopicType() {
        return topicType;
    }

    public void setTopicType(String topicType) {
        this.topicType = topicType;
    }

    public String getQuestionType() {
        return questionType;
    }

    public void setQuestionType(String questionType) {
        this.questionType = questionType;
    }

    public String getRegionScope() {
        return regionScope;
    }

    public void setRegionScope(String regionScope) {
        this.regionScope = regionScope;
    }
}