package com.example.lecturesystem.modules.usermodule.support;

public class SystemModuleDefinition {
    private String moduleCode;
    private String moduleName;
    private String routePath;
    private String routeName;
    private Integer sortOrder;
    private Boolean enabled;
    private Boolean adminOnly;
    private String parentCode;

    public SystemModuleDefinition() {
    }

    public SystemModuleDefinition(String moduleCode,
                                  String moduleName,
                                  String routePath,
                                  String routeName,
                                  Integer sortOrder,
                                  Boolean enabled,
                                  Boolean adminOnly,
                                  String parentCode) {
        this.moduleCode = moduleCode;
        this.moduleName = moduleName;
        this.routePath = routePath;
        this.routeName = routeName;
        this.sortOrder = sortOrder;
        this.enabled = enabled;
        this.adminOnly = adminOnly;
        this.parentCode = parentCode;
    }

    public String getModuleCode() {
        return moduleCode;
    }

    public void setModuleCode(String moduleCode) {
        this.moduleCode = moduleCode;
    }

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public String getRoutePath() {
        return routePath;
    }

    public void setRoutePath(String routePath) {
        this.routePath = routePath;
    }

    public String getRouteName() {
        return routeName;
    }

    public void setRouteName(String routeName) {
        this.routeName = routeName;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Boolean getAdminOnly() {
        return adminOnly;
    }

    public void setAdminOnly(Boolean adminOnly) {
        this.adminOnly = adminOnly;
    }

    public String getParentCode() {
        return parentCode;
    }

    public void setParentCode(String parentCode) {
        this.parentCode = parentCode;
    }
}
