package com.example.lecturesystem.modules.operationlog.dto;

import com.example.lecturesystem.modules.permission.support.TreePathScopedRequest;

public class OperationLogQueryRequest implements TreePathScopedRequest {
    private String treePathPrefix;
    private String moduleName;
    private String operatorName;
    private String startTime;
    private String endTime;

    @Override
    public String getTreePathPrefix() {
        return treePathPrefix;
    }

    @Override
    public void setTreePathPrefix(String treePathPrefix) {
        this.treePathPrefix = treePathPrefix;
    }

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public void setOperatorName(String operatorName) {
        this.operatorName = operatorName;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }
}
