package com.example.lecturesystem.modules.skill.vo;

import java.time.LocalDateTime;

public class SkillListItemVO {
    private Long id;
    private String skillCode;
    private String skillName;
    private String domainType;
    private String skillType;
    private String description;
    private Integer status;
    private Long ownerUserId;
    private Long publishedVersionId;
    private String publishedVersionNo;
    private String publishStatus;
    private Long baseId;
    private String baseName;
    private LocalDateTime updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSkillCode() { return skillCode; }
    public void setSkillCode(String skillCode) { this.skillCode = skillCode; }
    public String getSkillName() { return skillName; }
    public void setSkillName(String skillName) { this.skillName = skillName; }
    public String getDomainType() { return domainType; }
    public void setDomainType(String domainType) { this.domainType = domainType; }
    public String getSkillType() { return skillType; }
    public void setSkillType(String skillType) { this.skillType = skillType; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Long getOwnerUserId() { return ownerUserId; }
    public void setOwnerUserId(Long ownerUserId) { this.ownerUserId = ownerUserId; }
    public Long getPublishedVersionId() { return publishedVersionId; }
    public void setPublishedVersionId(Long publishedVersionId) { this.publishedVersionId = publishedVersionId; }
    public String getPublishedVersionNo() { return publishedVersionNo; }
    public void setPublishedVersionNo(String publishedVersionNo) { this.publishedVersionNo = publishedVersionNo; }
    public String getPublishStatus() { return publishStatus; }
    public void setPublishStatus(String publishStatus) { this.publishStatus = publishStatus; }
    public Long getBaseId() { return baseId; }
    public void setBaseId(Long baseId) { this.baseId = baseId; }
    public String getBaseName() { return baseName; }
    public void setBaseName(String baseName) { this.baseName = baseName; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}