package com.example.lecturesystem.modules.expert.vo;

import java.time.LocalDateTime;

public class SkillOwnerListItemVO {
    private Long id;
    private Long userId;
    private String username;
    private String realName;
    private Long skillId;
    private String skillName;
    private Long skillVersionId;
    private String versionNo;
    private String expertLevel;
    private Integer status;
    private LocalDateTime grantTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getRealName() { return realName; }
    public void setRealName(String realName) { this.realName = realName; }
    public Long getSkillId() { return skillId; }
    public void setSkillId(Long skillId) { this.skillId = skillId; }
    public String getSkillName() { return skillName; }
    public void setSkillName(String skillName) { this.skillName = skillName; }
    public Long getSkillVersionId() { return skillVersionId; }
    public void setSkillVersionId(Long skillVersionId) { this.skillVersionId = skillVersionId; }
    public String getVersionNo() { return versionNo; }
    public void setVersionNo(String versionNo) { this.versionNo = versionNo; }
    public String getExpertLevel() { return expertLevel; }
    public void setExpertLevel(String expertLevel) { this.expertLevel = expertLevel; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public LocalDateTime getGrantTime() { return grantTime; }
    public void setGrantTime(LocalDateTime grantTime) { this.grantTime = grantTime; }
}