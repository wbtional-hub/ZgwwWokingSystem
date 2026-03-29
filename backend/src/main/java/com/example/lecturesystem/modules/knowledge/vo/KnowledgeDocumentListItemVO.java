package com.example.lecturesystem.modules.knowledge.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class KnowledgeDocumentListItemVO {
    private Long id;
    private Long baseId;
    private Long categoryId;
    private String docTitle;
    private String fileName;
    private String docType;
    private String policyRegion;
    private String policyLevel;
    private LocalDate effectiveDate;
    private LocalDate expireDate;
    private String parseStatus;
    private Integer status;
    private LocalDateTime updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getBaseId() { return baseId; }
    public void setBaseId(Long baseId) { this.baseId = baseId; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public String getDocTitle() { return docTitle; }
    public void setDocTitle(String docTitle) { this.docTitle = docTitle; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getDocType() { return docType; }
    public void setDocType(String docType) { this.docType = docType; }
    public String getPolicyRegion() { return policyRegion; }
    public void setPolicyRegion(String policyRegion) { this.policyRegion = policyRegion; }
    public String getPolicyLevel() { return policyLevel; }
    public void setPolicyLevel(String policyLevel) { this.policyLevel = policyLevel; }
    public LocalDate getEffectiveDate() { return effectiveDate; }
    public void setEffectiveDate(LocalDate effectiveDate) { this.effectiveDate = effectiveDate; }
    public LocalDate getExpireDate() { return expireDate; }
    public void setExpireDate(LocalDate expireDate) { this.expireDate = expireDate; }
    public String getParseStatus() { return parseStatus; }
    public void setParseStatus(String parseStatus) { this.parseStatus = parseStatus; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}