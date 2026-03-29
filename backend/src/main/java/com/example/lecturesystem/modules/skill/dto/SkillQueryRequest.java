package com.example.lecturesystem.modules.skill.dto;

public class SkillQueryRequest {
    private String keywords;
    private String domainType;
    private Integer status;
    private String publishStatus;

    public String getKeywords() { return keywords; }
    public void setKeywords(String keywords) { this.keywords = keywords; }
    public String getDomainType() { return domainType; }
    public void setDomainType(String domainType) { this.domainType = domainType; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getPublishStatus() { return publishStatus; }
    public void setPublishStatus(String publishStatus) { this.publishStatus = publishStatus; }
}