package com.example.lecturesystem.modules.permission.support;

public class DataScopeContext {
    private final Long currentUserId;
    private final Long unitId;
    private final boolean superAdmin;

    public DataScopeContext(Long currentUserId, Long unitId, boolean superAdmin) {
        this.currentUserId = currentUserId;
        this.unitId = unitId;
        this.superAdmin = superAdmin;
    }

    public Long getCurrentUserId() {
        return currentUserId;
    }

    public Long getUnitId() {
        return unitId;
    }

    public boolean isSuperAdmin() {
        return superAdmin;
    }
}
