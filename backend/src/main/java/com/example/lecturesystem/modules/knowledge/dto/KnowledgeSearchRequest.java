package com.example.lecturesystem.modules.knowledge.dto;

import jakarta.validation.constraints.NotNull;

public class KnowledgeSearchRequest {
    @NotNull(message = "知识库ID不能为空")
    private Long baseId;
    private Long categoryId;
    private String keywords;
    private String regionScope;
    private String policyRegion;
    private String regionPriority;
    private String questionType;
    private String docType;
    private String topicType;
    private String scenePriority;
    private Boolean searchable;
    private Boolean effectiveOnly;
    private Integer topN;
    private Boolean hasRegionScope;
private Boolean hasKeywords;
private Boolean hasQuestionType;
private Boolean hasRegionPriority;

    public Long getBaseId() {
        return baseId;
    }

    public void setBaseId(Long baseId) {
        this.baseId = baseId;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getKeywords() {
        return keywords;
    }

    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }

    public String getRegionScope() {
        return regionScope;
    }

    public void setRegionScope(String regionScope) {
        this.regionScope = regionScope;
    }

    public String getPolicyRegion() {
        return policyRegion;
    }

    public void setPolicyRegion(String policyRegion) {
        this.policyRegion = policyRegion;
    }

    public String getRegionPriority() {
        return regionPriority;
    }

    public void setRegionPriority(String regionPriority) {
        this.regionPriority = regionPriority;
    }

    public String getQuestionType() {
        return questionType;
    }

    public void setQuestionType(String questionType) {
        this.questionType = questionType;
    }

    public String getDocType() {
        return docType;
    }

    public void setDocType(String docType) {
        this.docType = docType;
    }

    public String getTopicType() {
        return topicType;
    }

    public void setTopicType(String topicType) {
        this.topicType = topicType;
    }

    public String getScenePriority() {
        return scenePriority;
    }

    public void setScenePriority(String scenePriority) {
        this.scenePriority = scenePriority;
    }

    public Boolean getSearchable() {
        return searchable;
    }

    public void setSearchable(Boolean searchable) {
        this.searchable = searchable;
    }

    public Boolean getEffectiveOnly() {
        return effectiveOnly;
    }

    public void setEffectiveOnly(Boolean effectiveOnly) {
        this.effectiveOnly = effectiveOnly;
    }

    public Integer getTopN() {
        return topN;
    }

    public void setTopN(Integer topN) {
        this.topN = topN;
    }
    public Boolean getHasRegionScope() {
    return hasRegionScope;
}

public void setHasRegionScope(Boolean hasRegionScope) {
    this.hasRegionScope = hasRegionScope;
}

public Boolean getHasKeywords() {
    return hasKeywords;
}

public void setHasKeywords(Boolean hasKeywords) {
    this.hasKeywords = hasKeywords;
}

public Boolean getHasQuestionType() {
    return hasQuestionType;
}

public void setHasQuestionType(Boolean hasQuestionType) {
    this.hasQuestionType = hasQuestionType;
}

public Boolean getHasRegionPriority() {
    return hasRegionPriority;
}

public void setHasRegionPriority(Boolean hasRegionPriority) {
    this.hasRegionPriority = hasRegionPriority;
}
}
