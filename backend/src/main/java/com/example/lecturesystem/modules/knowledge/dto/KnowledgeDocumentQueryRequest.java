package com.example.lecturesystem.modules.knowledge.dto;

public class KnowledgeDocumentQueryRequest {
    private Long baseId;
    private Long categoryId;
    private String keywords;
    private String policyRegion;
    private Integer status;
    private String parseStatus;

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

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getParseStatus() {
        return parseStatus;
    }

    public void setParseStatus(String parseStatus) {
        this.parseStatus = parseStatus;
    }
}