package com.example.lecturesystem.modules.aipermission.vo;

import java.util.List;

public class CurrentAiPermissionVO {
    private Boolean admin;
    private Boolean canManageProvider;
    private List<UserAiPermissionListItemVO> aiPermissions;
    private List<UserKnowledgePermissionListItemVO> knowledgePermissions;
    private List<UserSkillPermissionListItemVO> skillPermissions;

    public Boolean getAdmin() { return admin; }
    public void setAdmin(Boolean admin) { this.admin = admin; }
    public Boolean getCanManageProvider() { return canManageProvider; }
    public void setCanManageProvider(Boolean canManageProvider) { this.canManageProvider = canManageProvider; }
    public List<UserAiPermissionListItemVO> getAiPermissions() { return aiPermissions; }
    public void setAiPermissions(List<UserAiPermissionListItemVO> aiPermissions) { this.aiPermissions = aiPermissions; }
    public List<UserKnowledgePermissionListItemVO> getKnowledgePermissions() { return knowledgePermissions; }
    public void setKnowledgePermissions(List<UserKnowledgePermissionListItemVO> knowledgePermissions) { this.knowledgePermissions = knowledgePermissions; }
    public List<UserSkillPermissionListItemVO> getSkillPermissions() { return skillPermissions; }
    public void setSkillPermissions(List<UserSkillPermissionListItemVO> skillPermissions) { this.skillPermissions = skillPermissions; }
}