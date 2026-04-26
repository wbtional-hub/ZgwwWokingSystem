package com.example.lecturesystem.modules.attendance.support;

import com.example.lecturesystem.modules.attendance.mapper.AttendanceWeeklyReadonlyScopeMapper;
import com.example.lecturesystem.modules.user.entity.UserEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class AttendanceWeeklyReadonlyScopeService {
    private static final String GENERAL_SECTION_KEYWORD = "综合科";
    private static final String SECTION_CHIEF_KEYWORD = "科长";
    private static final String GENERAL_SECTION_CHIEF_KEYWORD = "综合科科长";

    private final AttendanceWeeklyReadonlyScopeMapper attendanceWeeklyReadonlyScopeMapper;

    public AttendanceWeeklyReadonlyScopeService(AttendanceWeeklyReadonlyScopeMapper attendanceWeeklyReadonlyScopeMapper) {
        this.attendanceWeeklyReadonlyScopeMapper = attendanceWeeklyReadonlyScopeMapper;
    }

    public ReadonlyScope resolve(UserEntity currentUser) {
        if (currentUser == null) {
            return ReadonlyScope.disabled();
        }
        String unitName = normalizeText(attendanceWeeklyReadonlyScopeMapper.findUnitNameById(currentUser.getUnitId()));
        boolean generalSectionChief = isGeneralSectionChief(currentUser, unitName);
        if (!generalSectionChief
                || currentUser.getId() == null
                || currentUser.getUnitId() == null
                || currentUser.getLevelNo() == null) {
            return ReadonlyScope.disabled(unitName);
        }
        String treePathPrefix = normalizeTreePath(currentUser.getTreePath());
        if (treePathPrefix == null) {
            return ReadonlyScope.disabled(unitName);
        }
        List<Long> userIds = attendanceWeeklyReadonlyScopeMapper.queryCrossDeptReadonlyUserIds(
                currentUser.getId(),
                currentUser.getUnitId(),
                treePathPrefix,
                currentUser.getLevelNo()
        );
        return new ReadonlyScope(true, unitName, new LinkedHashSet<>(userIds));
    }

    public boolean isGeneralSectionChief(UserEntity currentUser) {
        if (currentUser == null) {
            return false;
        }
        String unitName = normalizeText(attendanceWeeklyReadonlyScopeMapper.findUnitNameById(currentUser.getUnitId()));
        return isGeneralSectionChief(currentUser, unitName);
    }

    public String buildScopeDescription(UserEntity currentUser, long totalUserCount, int crossDeptUserCount) {
        if (crossDeptUserCount <= 0) {
            return "当前可查看本部门及下级，共 " + totalUserCount + " 人";
        }
        return "当前可查看本部门及下级，另可跨部门只读查看其他部门下级，共 "
                + totalUserCount
                + " 人（跨部门只读 "
                + crossDeptUserCount
                + " 人）";
    }

    private boolean isGeneralSectionChief(UserEntity currentUser, String unitName) {
        String jobTitle = normalizeText(currentUser.getJobTitle());
        if (jobTitle == null) {
            return false;
        }
        if (jobTitle.contains(GENERAL_SECTION_CHIEF_KEYWORD)) {
            return true;
        }
        return unitName != null
                && unitName.contains(GENERAL_SECTION_KEYWORD)
                && jobTitle.contains(SECTION_CHIEF_KEYWORD);
    }

    private String normalizeTreePath(String treePath) {
        String normalized = normalizeText(treePath);
        if (normalized == null) {
            return null;
        }
        return normalized.endsWith("/") ? normalized : normalized + "/";
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    public static final class ReadonlyScope {
        private final boolean generalSectionChief;
        private final String unitName;
        private final Set<Long> crossDeptReadonlyUserIds;

        private ReadonlyScope(boolean generalSectionChief, String unitName, Set<Long> crossDeptReadonlyUserIds) {
            this.generalSectionChief = generalSectionChief;
            this.unitName = unitName;
            this.crossDeptReadonlyUserIds = crossDeptReadonlyUserIds == null ? Set.of() : Set.copyOf(crossDeptReadonlyUserIds);
        }

        public static ReadonlyScope disabled() {
            return new ReadonlyScope(false, null, Set.of());
        }

        public static ReadonlyScope disabled(String unitName) {
            return new ReadonlyScope(false, unitName, Set.of());
        }

        public boolean isGeneralSectionChief() {
            return generalSectionChief;
        }

        public String getUnitName() {
            return unitName;
        }

        public boolean canViewCrossDeptUser(Long userId) {
            return userId != null && crossDeptReadonlyUserIds.contains(userId);
        }

        public int crossDeptUserCount() {
            return crossDeptReadonlyUserIds.size();
        }

        public List<Long> requestUserIds() {
            return new ArrayList<>(crossDeptReadonlyUserIds);
        }
    }
}
