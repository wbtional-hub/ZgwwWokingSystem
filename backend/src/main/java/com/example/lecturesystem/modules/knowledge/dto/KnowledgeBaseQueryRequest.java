package com.example.lecturesystem.modules.knowledge.dto;

public class KnowledgeBaseQueryRequest {
    private String keywords;
    private String domainType;
    private Integer status;

    public String getKeywords() {
        return keywords;
    }

    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }

    public String getDomainType() {
        return domainType;
    }

    public void setDomainType(String domainType) {
        this.domainType = domainType;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
