package com.example.lecturesystem.modules.aipermission.dto;

import jakarta.validation.constraints.NotNull;

public class SaveUserSkillPermissionRequest {
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotNull(message = "技能ID不能为空")
    private Long skillId;

    @NotNull(message = "状态不能为空")
    private Integer status;

    private Boolean canView;
    private Boolean canUse;
    private Boolean canTrain;
    private Boolean canPublish;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getSkillId() { return skillId; }
    public void setSkillId(Long skillId) { this.skillId = skillId; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Boolean getCanView() { return canView; }
    public void setCanView(Boolean canView) { this.canView = canView; }
    public Boolean getCanUse() { return canUse; }
    public void setCanUse(Boolean canUse) { this.canUse = canUse; }
    public Boolean getCanTrain() { return canTrain; }
    public void setCanTrain(Boolean canTrain) { this.canTrain = canTrain; }
    public Boolean getCanPublish() { return canPublish; }
    public void setCanPublish(Boolean canPublish) { this.canPublish = canPublish; }
}