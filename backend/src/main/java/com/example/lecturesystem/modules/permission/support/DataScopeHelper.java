package com.example.lecturesystem.modules.permission.support;

import org.springframework.stereotype.Component;

@Component
public class DataScopeHelper {
    private final CurrentUserFacade currentUserFacade;

    public DataScopeHelper(CurrentUserFacade currentUserFacade) {
        this.currentUserFacade = currentUserFacade;
    }

    public DataScopeContext currentScope() {
        return currentUserFacade.currentDataScope();
    }

    public <T extends UnitScopedRequest> T injectUnitScope(T request) {
        if (request == null) {
            return null;
        }
        DataScopeContext scope = currentScope();
        if (!scope.isSuperAdmin()) {
            request.setUnitId(scope.getUnitId());
        }
        return request;
    }

    public void validateReadableUnit(Long unitId) {
        DataScopeContext scope = currentScope();
        if (scope.isSuperAdmin()) {
            return;
        }
        if (unitId == null || !unitId.equals(scope.getUnitId())) {
            throw new IllegalArgumentException("无权查看当前单位数据");
        }
    }
}
