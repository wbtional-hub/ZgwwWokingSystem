package com.example.lecturesystem.modules.skill.entity;

import java.time.LocalDateTime;

public class SkillEntity {
    private Long id;
    private String skillCode;
    private String skillName;
    private String domainType;
    private String skillType;
    private String description;
    private Integer status;
    private Long ownerUserId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String createUser;
    private String updateUser;
    private Boolean isDeleted;

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
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    public String getCreateUser() { return createUser; }
    public void setCreateUser(String createUser) { this.createUser = createUser; }
    public String getUpdateUser() { return updateUser; }
    public void setUpdateUser(String updateUser) { this.updateUser = updateUser; }
    public Boolean getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Boolean isDeleted) { this.isDeleted = isDeleted; }
}