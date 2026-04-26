package com.example.lecturesystem.modules.agent.support.policy;

import java.util.ArrayList;
import java.util.List;

public class AiPolicyChunkSearchQuery {
    private Long baseId;
    private List<String> regionScopes = new ArrayList<>();
    private String policyKey;
    private List<String> policyKeys = new ArrayList<>();
    private List<String> docTypes = new ArrayList<>();
    private List<String> topicTypes = new ArrayList<>();
    private List<String> questionTypes = new ArrayList<>();
    private String keywords;
    private Boolean enabled = Boolean.TRUE;
    private Integer topN = 20;

    public Long getBaseId() { return baseId; }
    public void setBaseId(Long baseId) { this.baseId = baseId; }
    public List<String> getRegionScopes() { return regionScopes; }
    public void setRegionScopes(List<String> regionScopes) { this.regionScopes = regionScopes; }
    public String getPolicyKey() { return policyKey; }
    public void setPolicyKey(String policyKey) { this.policyKey = policyKey; }
    public List<String> getPolicyKeys() { return policyKeys; }
    public void setPolicyKeys(List<String> policyKeys) { this.policyKeys = policyKeys; }
    public List<String> getDocTypes() { return docTypes; }
    public void setDocTypes(List<String> docTypes) { this.docTypes = docTypes; }
    public List<String> getTopicTypes() { return topicTypes; }
    public void setTopicTypes(List<String> topicTypes) { this.topicTypes = topicTypes; }
    public List<String> getQuestionTypes() { return questionTypes; }
    public void setQuestionTypes(List<String> questionTypes) { this.questionTypes = questionTypes; }
    public String getKeywords() { return keywords; }
    public void setKeywords(String keywords) { this.keywords = keywords; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public Integer getTopN() { return topN; }
    public void setTopN(Integer topN) { this.topN = topN; }
}
