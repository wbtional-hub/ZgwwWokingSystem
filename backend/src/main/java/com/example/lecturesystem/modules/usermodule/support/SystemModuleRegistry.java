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

        register(map, "home", "首页", "/home", "home", 10, true, false, null);
        register(map, "policy_consultant", "手机端政策咨询", "/policy-consultant", "policy-consultant", 20, true, false, null);

        register(map, "unit", "单位管理", "/units", "units", 30, true, true, null);
        register(map, "param", "参数管理", "/params", "params", 40, true, true, null);
        register(map, "knowledge", "知识库中心", "/knowledge", "knowledge", 50, true, false, null);
        register(map, "skill", "Skills 中心", "/skills", "skills", 60, true, false, null);

        register(map, "ai_workbench", "AI 工作台", "/ai-workbench", "ai-workbench", 70, true, false, null);
        register(map, "ai_ledger", "咨询台账", "/ai-ledger", "ai-ledger", 80, true, false, null);
        register(map, "ai_monthly_report", "月度报表", "/ai-monthly-report", "ai-monthly-report", 90, true, false, null);
        register(map, "expert", "专家台账", "/experts", "experts", 100, true, false, null);
        register(map, "ai_provider", "AI 接入区", "/ai-provider", "ai-provider", 110, true, true, null);
        register(map, "ai_permission", "AI 权限配置", "/ai-permissions", "ai-permissions", 120, true, true, null);

        register(map, "log_center", "日志中台", "/log-center", "log-center", 130, true, true, null);
        register(map, "operationlog", "操作日志", "/operation-logs", "operation-logs", 140, true, true, null);
        register(map, "orgtree", "组织架构", "/org-tree", "org-tree", 150, true, true, null);

        register(map, "attendance_workbench", "考勤工作台", "/attendance", "attendance", 160, true, false, null);
        register(map, "attendance_stats", "考勤统计", "/attendance/stats", "attendance-stats", 170, true, false, null);
        register(map, "attendance_patch_apply", "考勤申请", "/attendance/patch-apply", "attendance-patch-apply", 180, true, false, null);
register(map, "attendance_patch_approvals", "考勤审批", "/attendance/patch-approvals", "attendance-patch-approvals", 190, true, false, null);
        register(map, "attendance_rules", "考勤规则", "/attendance/rules", "attendance-rules", 200, true, false, null);

        register(map, "weeklywork", "周报管理", "/weekly-work", "weekly-work", 210, true, false, null);
        register(map, "score", "工作评分", "/scores", "scores", 220, true, true, null);
        register(map, "statistics", "统计分析", "/statistics", "statistics", 230, true, false, null);

        register(map, "profile", "个人中心", "/profile", "profile", 240, true, false, null);
        register(map, "user", "用户管理", "/users", "users", 250, true, true, null);

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
        if (moduleCode == null) {
            return null;
        }
        String normalized = moduleCode.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            return null;
        }

        // 兼容旧的粗粒度考勤权限码
        if ("attendance".equals(normalized)) {
            return "attendance_workbench";
        }

        return normalized;
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