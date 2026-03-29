package com.example.lecturesystem.modules.knowledge.dto;

import jakarta.validation.constraints.NotNull;

public class KnowledgeSearchRequest {
    @NotNull(message = "知识库ID不能为空")
    private Long baseId;
    private Long categoryId;
    private String keywords;
    private String policyRegion;
    private Boolean effectiveOnly;
    private Integer topN;

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

    public String getPolicyRegion() {
        return policyRegion;
    }

    public void setPolicyRegion(String policyRegion) {
        this.policyRegion = policyRegion;
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
}