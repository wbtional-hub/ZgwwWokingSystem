package com.example.lecturesystem.modules.agent.support;

import com.example.lecturesystem.modules.knowledge.support.PolicyKnowledgeSupport;

import java.util.ArrayList;
import java.util.List;

public class PolicyRouteIntent {
    private String question;
    private String regionScope;
    private PolicyKnowledgeSupport.PolicyQuestionType questionType;
    private List<String> topicTags = new ArrayList<>();
    private List<String> formalPolicyNames = new ArrayList<>();
    private List<String> routeSkills = new ArrayList<>();
    private boolean overviewList;
    private boolean topicList;

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
    public String getRegionScope() { return regionScope; }
    public void setRegionScope(String regionScope) { this.regionScope = regionScope; }
    public PolicyKnowledgeSupport.PolicyQuestionType getQuestionType() { return questionType; }
    public void setQuestionType(PolicyKnowledgeSupport.PolicyQuestionType questionType) { this.questionType = questionType; }
    public List<String> getTopicTags() { return topicTags; }
    public void setTopicTags(List<String> topicTags) { this.topicTags = topicTags; }
    public List<String> getFormalPolicyNames() { return formalPolicyNames; }
    public void setFormalPolicyNames(List<String> formalPolicyNames) { this.formalPolicyNames = formalPolicyNames; }
    public List<String> getRouteSkills() { return routeSkills; }
    public void setRouteSkills(List<String> routeSkills) { this.routeSkills = routeSkills; }
    public boolean isOverviewList() { return overviewList; }
    public void setOverviewList(boolean overviewList) { this.overviewList = overviewList; }
    public boolean isTopicList() { return topicList; }
    public void setTopicList(boolean topicList) { this.topicList = topicList; }
}
