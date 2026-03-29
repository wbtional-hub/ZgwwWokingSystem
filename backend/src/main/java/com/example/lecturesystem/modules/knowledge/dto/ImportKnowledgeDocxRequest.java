package com.example.lecturesystem.modules.knowledge.dto;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public class ImportKnowledgeDocxRequest {
    private Long baseId;
    private Long categoryId;
    private String policyRegion;
    private String policyLevel;
    private String summary;
    private String keywords;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate effectiveDate;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate expireDate;

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

    public String getPolicyRegion() {
        return policyRegion;
    }

    public void setPolicyRegion(String policyRegion) {
        this.policyRegion = policyRegion;
    }

    public String getPolicyLevel() {
        return policyLevel;
    }

    public void setPolicyLevel(String policyLevel) {
        this.policyLevel = policyLevel;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getKeywords() {
        return keywords;
    }

    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }

    public LocalDate getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(LocalDate effectiveDate) {
        this.effectiveDate = effectiveDate;
    }

    public LocalDate getExpireDate() {
        return expireDate;
    }

    public void setExpireDate(LocalDate expireDate) {
        this.expireDate = expireDate;
    }
}