package com.example.lecturesystem.modules.agent.dto;

import java.util.ArrayList;
import java.util.List;

public class PolicyQuestionAnalysis {

    private String originalQuestion;
    private List<String> conditionCodes = new ArrayList<>();
    private boolean conditionReverseQuery;
    private String targetSubject;

    public String getOriginalQuestion() {
        return originalQuestion;
    }

    public void setOriginalQuestion(String originalQuestion) {
        this.originalQuestion = originalQuestion;
    }

    public List<String> getConditionCodes() {
        return conditionCodes;
    }

    public void setConditionCodes(List<String> conditionCodes) {
        this.conditionCodes = conditionCodes == null ? new ArrayList<>() : conditionCodes;
    }

    public boolean isConditionReverseQuery() {
        return conditionReverseQuery;
    }

    public void setConditionReverseQuery(boolean conditionReverseQuery) {
        this.conditionReverseQuery = conditionReverseQuery;
    }

    public String getTargetSubject() {
        return targetSubject;
    }

    public void setTargetSubject(String targetSubject) {
        this.targetSubject = targetSubject;
    }
}