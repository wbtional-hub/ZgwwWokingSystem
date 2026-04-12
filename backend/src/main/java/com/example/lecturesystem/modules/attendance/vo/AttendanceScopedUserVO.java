package com.example.lecturesystem.modules.attendance.vo;

public class AttendanceScopedUserVO {
    private Long userId;
    private Long unitId;
    private String username;
    private String realName;
    private String unitName;
    private String treePath;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getUnitId() { return unitId; }
    public void setUnitId(Long unitId) { this.unitId = unitId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getRealName() { return realName; }
    public void setRealName(String realName) { this.realName = realName; }
    public String getUnitName() { return unitName; }
    public void setUnitName(String unitName) { this.unitName = unitName; }
    public String getTreePath() { return treePath; }
    public void setTreePath(String treePath) { this.treePath = treePath; }
}
