package com.example.lecturesystem.modules.orgtree.entity;

public class OrgNodeEntity {
    private Long id;
    private Long unitId;
    private Long userId;
    private Long parentUserId;
    private Integer levelNo;
    private String treePath;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUnitId() { return unitId; }
    public void setUnitId(Long unitId) { this.unitId = unitId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getParentUserId() { return parentUserId; }
    public void setParentUserId(Long parentUserId) { this.parentUserId = parentUserId; }
    public Integer getLevelNo() { return levelNo; }
    public void setLevelNo(Integer levelNo) { this.levelNo = levelNo; }
    public String getTreePath() { return treePath; }
    public void setTreePath(String treePath) { this.treePath = treePath; }
}
