package com.example.lecturesystem.modules.workscore.dto;

import com.example.lecturesystem.modules.permission.support.TreePathScopedRequest;

public class WorkScoreQueryRequest implements TreePathScopedRequest {
    private String treePathPrefix;
    private String weekNo;
    private String unitName;
    private String status;
    private Boolean sortByTotalDesc;

    @Override
    public String getTreePathPrefix() { return treePathPrefix; }
    @Override
    public void setTreePathPrefix(String treePathPrefix) { this.treePathPrefix = treePathPrefix; }
    public String getWeekNo() { return weekNo; }
    public void setWeekNo(String weekNo) { this.weekNo = weekNo; }
    public String getUnitName() { return unitName; }
    public void setUnitName(String unitName) { this.unitName = unitName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Boolean getSortByTotalDesc() { return sortByTotalDesc; }
    public void setSortByTotalDesc(Boolean sortByTotalDesc) { this.sortByTotalDesc = sortByTotalDesc; }
}
