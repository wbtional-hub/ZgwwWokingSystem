package com.example.lecturesystem.modules.agent.entity;

import java.time.LocalDateTime;

public class AgentUserPreferenceEntity {
    private Long userId;
    private String habitSummary;
    private String preferredAnswerStyle;
    private String recentTopics;
    private String lastQuestion;
    private LocalDateTime updateTime;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getHabitSummary() {
        return habitSummary;
    }

    public void setHabitSummary(String habitSummary) {
        this.habitSummary = habitSummary;
    }

    public String getPreferredAnswerStyle() {
        return preferredAnswerStyle;
    }

    public void setPreferredAnswerStyle(String preferredAnswerStyle) {
        this.preferredAnswerStyle = preferredAnswerStyle;
    }

    public String getRecentTopics() {
        return recentTopics;
    }

    public void setRecentTopics(String recentTopics) {
        this.recentTopics = recentTopics;
    }

    public String getLastQuestion() {
        return lastQuestion;
    }

    public void setLastQuestion(String lastQuestion) {
        this.lastQuestion = lastQuestion;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
