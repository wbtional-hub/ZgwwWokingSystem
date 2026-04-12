package com.example.lecturesystem.modules.usermodule.support;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class SystemModuleRegistry {
    private final Map<String, SystemModuleDefinition> definitions;

    public SystemModuleRegistry() {
        Map<String, SystemModuleDefinition> map = new LinkedHashMap<>();
        register(map, "unit", "单位管理", "/units", "units", 10, true, true, null);
        register(map, "param", "参数管理", "/params", "params", 20, true, true, null);
        register(map, "knowledge", "知识库中心", "/knowledge", "knowledge", 30, true, false, null);
        register(map, "skill", "Skills 中心", "/skills", "skills", 40, true, false, null);
        register(map, "policy_consultant", "手机端政策咨询", "/policy-consultant", "policy-consultant", 45, true, false, null);
        register(map, "ai_workbench", "AI 工作台", "/ai-workbench", "ai-workbench", 50, true, false, null);
        register(map, "ai_ledger", "咨询台账", "/ai-ledger", "ai-ledger", 60, true, false, null);
        register(map, "ai_monthly_report", "月度报表", "/ai-monthly-report", "ai-monthly-report", 70, true, false, null);
        register(map, "expert", "专家台账", "/experts", "experts", 80, true, false, null);
        register(map, "ai_provider", "AI 接入区", "/ai-provider", "ai-provider", 90, true, true, null);
        register(map, "ai_permission", "AI 权限配置", "/ai-permissions", "ai-permissions", 100, true, true, null);
        register(map, "operationlog", "操作日志", "/operation-logs", "operation-logs", 110, true, true, null);
        register(map, "orgtree", "组织架构", "/org-tree", "org-tree", 120, true, true, null);
        register(map, "attendance", "签到管理", "/attendance", "attendance", 130, true, false, null);
        register(map, "weeklywork", "周报管理", "/weekly-work", "weekly-work", 140, true, false, null);
        register(map, "score", "工作评分", "/scores", "scores", 150, true, true, null);
        register(map, "statistics", "统计分析", "/statistics", "statistics", 160, true, false, null);
        register(map, "user", "用户管理", "/users", "users", 170, true, true, null);
        this.definitions = Map.copyOf(map);
    }

    public List<SystemModuleDefinition> listEnabledDefinitions() {
        return definitions.values().stream()
                .filter(item -> Boolean.TRUE.equals(item.getEnabled()))
                .sorted((left, right) -> Integer.compare(left.getSortOrder(), right.getSortOrder()))
                .collect(Collectors.toList());
    }

    public boolean containsEnabledModule(String moduleCode) {
        if (moduleCode == null) {
            return false;
        }
        SystemModuleDefinition definition = definitions.get(normalizeModuleCode(moduleCode));
        return definition != null && Boolean.TRUE.equals(definition.getEnabled());
    }

    public List<String> listEnabledModuleCodes() {
        return listEnabledDefinitions().stream()
                .map(SystemModuleDefinition::getModuleCode)
                .collect(Collectors.toList());
    }

    public String normalizeModuleCode(String moduleCode) {
        return moduleCode == null ? null : moduleCode.trim().toLowerCase(Locale.ROOT);
    }

    private void register(Map<String, SystemModuleDefinition> map,
                          String moduleCode,
                          String moduleName,
                          String routePath,
                          String routeName,
                          int sortOrder,
                          boolean enabled,
                          boolean adminOnly,
                          String parentCode) {
        map.put(moduleCode, new SystemModuleDefinition(
                moduleCode,
                moduleName,
                routePath,
                routeName,
                sortOrder,
                enabled,
                adminOnly,
                parentCode
        ));
    }
}
