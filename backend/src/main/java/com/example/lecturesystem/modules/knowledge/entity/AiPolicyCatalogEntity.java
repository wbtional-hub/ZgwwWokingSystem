package com.example.lecturesystem.modules.knowledge.entity;

import java.time.LocalDateTime;

public class AiPolicyCatalogEntity {
    private Long id;
    private Long baseId;
    private String regionScope;
    private String policyGroup;
    private String policyName;
    private String policyAliases;
    private String policyNo;
    private String policyType;
    private String topicTags;
    private String shortSummary;
    private String sourceDocType;
    private String sourceDocName;
    private String sourceChunkIds;
    private Boolean searchable;
    private Integer sortOrder;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getBaseId() { return baseId; }
    public void setBaseId(Long baseId) { this.baseId = baseId; }
    public String getRegionScope() { return regionScope; }
    public void setRegionScope(String regionScope) { this.regionScope = regionScope; }
    public String getPolicyGroup() { return policyGroup; }
    public void setPolicyGroup(String policyGroup) { this.policyGroup = policyGroup; }
    public String getPolicyName() { return policyName; }
    public void setPolicyName(String policyName) { this.policyName = policyName; }
    public String getPolicyAliases() { return policyAliases; }
    public void setPolicyAliases(String policyAliases) { this.policyAliases = policyAliases; }
    public String getPolicyNo() { return policyNo; }
    public void setPolicyNo(String policyNo) { this.policyNo = policyNo; }
    public String getPolicyType() { return policyType; }
    public void setPolicyType(String policyType) { this.policyType = policyType; }
    public String getTopicTags() { return topicTags; }
    public void setTopicTags(String topicTags) { this.topicTags = topicTags; }
    public String getShortSummary() { return shortSummary; }
    public void setShortSummary(String shortSummary) { this.shortSummary = shortSummary; }
    public String getSourceDocType() { return sourceDocType; }
    public void setSourceDocType(String sourceDocType) { this.sourceDocType = sourceDocType; }
    public String getSourceDocName() { return sourceDocName; }
    public void setSourceDocName(String sourceDocName) { this.sourceDocName = sourceDocName; }
    public String getSourceChunkIds() { return sourceChunkIds; }
    public void setSourceChunkIds(String sourceChunkIds) { this.sourceChunkIds = sourceChunkIds; }
    public Boolean getSearchable() { return searchable; }
    public void setSearchable(Boolean searchable) { this.searchable = searchable; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
