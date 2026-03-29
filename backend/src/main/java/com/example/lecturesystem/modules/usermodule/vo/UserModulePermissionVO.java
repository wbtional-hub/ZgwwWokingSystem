package com.example.lecturesystem.modules.usermodule.vo;

import java.util.ArrayList;
import java.util.List;

public class UserModulePermissionVO {
    private Long userId;
    private Boolean admin;
    private List<String> moduleCodes = new ArrayList<>();

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Boolean getAdmin() {
        return admin;
    }

    public void setAdmin(Boolean admin) {
        this.admin = admin;
    }

    public List<String> getModuleCodes() {
        return moduleCodes;
    }

    public void setModuleCodes(List<String> moduleCodes) {
        this.moduleCodes = moduleCodes;
    }
}
