package com.example.lecturesystem.modules.attendance.support;

import com.example.lecturesystem.modules.attendance.entity.AttendancePatchApplyEntity;
import com.example.lecturesystem.modules.attendance.entity.AttendanceRecordEntity;
import com.example.lecturesystem.modules.attendance.entity.AttendanceRuleEntity;
import com.example.lecturesystem.modules.attendance.vo.AttendanceNodeStatusVO;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class AttendanceNodeStateResolver {
    public static final String NODE_AM_ON = "AM_ON";
    public static final String NODE_AM_OFF = "AM_OFF";
    public static final String NODE_PM_ON = "PM_ON";
    public static final String NODE_PM_OFF = "PM_OFF";

    private static final String PATCH_STATUS_PENDING = "PENDING";
    private static final String PATCH_STATUS_APPROVED = "APPROVED";
    private static final String PATCH_STATUS_REJECTED = "REJECTED";
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private AttendanceNodeStateResolver() {
    }

    public static ResolvedAttendanceState resolve(AttendanceRuleEntity rule,
                                                  AttendanceRecordEntity record,
                                                  List<AttendancePatchApplyEntity> patchApplies,
                                                  LocalTime referenceTime,
                                                  String evidenceRequiredNodeCode) {
        Map<String, AttendancePatchApplyEntity> latestApplyMap = resolveLatestApplyMap(patchApplies);
        List<AttendanceNodeStatusVO> nodeStates = new ArrayList<>(4);
        nodeStates.add(resolveNode(rule, record, latestApplyMap.get(NODE_AM_ON), referenceTime, NODE_AM_ON, evidenceRequiredNodeCode));
        nodeStates.add(resolveNode(rule, record, latestApplyMap.get(NODE_AM_OFF), referenceTime, NODE_AM_OFF, evidenceRequiredNodeCode));
        nodeStates.add(resolveNode(rule, record, latestApplyMap.get(NODE_PM_ON), referenceTime, NODE_PM_ON, evidenceRequiredNodeCode));
        nodeStates.add(resolveNode(rule, record, latestApplyMap.get(NODE_PM_OFF), referenceTime, NODE_PM_OFF, evidenceRequiredNodeCode));
        return buildResolvedState(nodeStates, rule, record);
    }

    private static Map<String, AttendancePatchApplyEntity> resolveLatestApplyMap(List<AttendancePatchApplyEntity> patchApplies) {
        Map<String, AttendancePatchApplyEntity> result = new LinkedHashMap<>();
        if (patchApplies == null || patchApplies.isEmpty()) {
            return result;
        }
        patchApplies.stream()
                .filter(item -> item != null && item.getPatchType() != null)
                .sorted(Comparator
                        .comparing(AttendancePatchApplyEntity::getCreateTime, Comparator.nullsLast(LocalDateTime::compareTo))
                        .thenComparing(AttendancePatchApplyEntity::getId, Comparator.nullsLast(Long::compareTo))
                        .reversed())
                .forEach(item -> result.putIfAbsent(item.getPatchType(), item));
        return result;
    }

    private static AttendanceNodeStatusVO resolveNode(AttendanceRuleEntity rule,
                                                      AttendanceRecordEntity record,
                                                      AttendancePatchApplyEntity latestApply,
                                                      LocalTime referenceTime,
                                                      String nodeCode,
                                                      String evidenceRequiredNodeCode) {
        AttendanceNodeStatusVO node = new AttendanceNodeStatusVO();
        node.setNodeCode(nodeCode);
        node.setNodeTitleShort(resolveNodeTitle(nodeCode));
        node.setTimeRangeText(resolveTimeRangeText(rule, nodeCode));

        LocalDateTime actualPunchTime = resolveActualPunchTime(record, nodeCode);
        node.setActualPunchTime(actualPunchTime);

        if (actualPunchTime != null) {
            String resolvedStatus = classifyResolvedStatus(rule, nodeCode, actualPunchTime);
            if (latestApply != null && PATCH_STATUS_APPROVED.equals(latestApply.getStatus())) {
                String applyType = AttendanceApplyType.normalize(latestApply.getApplyType());
                node.setStatusCode(AttendanceApplyType.EVIDENCE.equals(applyType)
                        ? AttendanceCheckInStatus.EVIDENCE_APPROVED
                        : AttendanceCheckInStatus.MAKEUP_APPROVED);
                node.setSimpleRemark(buildApprovedRemark(applyType, resolvedStatus));
            } else {
                node.setStatusCode(resolvedStatus);
                node.setSimpleRemark(buildResolvedRemark(resolvedStatus));
            }
            node.setCanPunch(false);
            node.setCanApplyMakeup(false);
            node.setCanEvidence(false);
            node.setNeedEarlyConfirm(false);
            node.setFinalResolved(true);
            return node;
        }

        if (nodeCode.equals(evidenceRequiredNodeCode)) {
            node.setStatusCode(AttendanceCheckInStatus.EVIDENCE_REQUIRED);
            node.setCanPunch(false);
            node.setCanApplyMakeup(false);
            node.setCanEvidence(true);
            node.setNeedEarlyConfirm(false);
            node.setFinalResolved(false);
            node.setSimpleRemark("超出允许打卡范围，请提交取证说明");
            return node;
        }

        BaseNodeState baseState = buildBaseNodeState(rule, nodeCode, referenceTime);
        if (latestApply != null && PATCH_STATUS_PENDING.equals(latestApply.getStatus())) {
            String applyType = AttendanceApplyType.normalize(latestApply.getApplyType());
            node.setStatusCode(AttendanceApplyType.EVIDENCE.equals(applyType)
                    ? AttendanceCheckInStatus.EVIDENCE_PENDING
                    : AttendanceCheckInStatus.MAKEUP_PENDING);
            node.setCanPunch(false);
            node.setCanApplyMakeup(false);
            node.setCanEvidence(false);
            node.setNeedEarlyConfirm(false);
            node.setFinalResolved(false);
            node.setSimpleRemark(AttendanceApplyType.EVIDENCE.equals(applyType)
                    ? "已提交取证，等待审批"
                    : "已提交补打卡，等待审批");
            return node;
        }

        if (latestApply != null && PATCH_STATUS_REJECTED.equals(latestApply.getStatus())) {
            String applyType = AttendanceApplyType.normalize(latestApply.getApplyType());
            node.setStatusCode(AttendanceApplyType.EVIDENCE.equals(applyType)
                    ? AttendanceCheckInStatus.EVIDENCE_REJECTED
                    : AttendanceCheckInStatus.MAKEUP_REJECTED);
            node.setCanPunch(baseState.canPunch);
            node.setCanApplyMakeup(baseState.canApplyMakeup);
            node.setCanEvidence(baseState.canEvidence);
            node.setNeedEarlyConfirm(baseState.needEarlyConfirm);
            node.setFinalResolved(false);
            node.setSimpleRemark((AttendanceApplyType.EVIDENCE.equals(applyType)
                    ? "取证未通过，"
                    : "补打卡未通过，") + baseState.fallbackRemark);
            return node;
        }

        node.setStatusCode(baseState.statusCode);
        node.setCanPunch(baseState.canPunch);
        node.setCanApplyMakeup(baseState.canApplyMakeup);
        node.setCanEvidence(baseState.canEvidence);
        node.setNeedEarlyConfirm(baseState.needEarlyConfirm);
        node.setFinalResolved(false);
        node.setSimpleRemark(baseState.remark);
        return node;
    }

    private static BaseNodeState buildBaseNodeState(AttendanceRuleEntity rule,
                                                    String nodeCode,
                                                    LocalTime referenceTime) {
        if (rule == null) {
            return new BaseNodeState(
                    AttendanceCheckInStatus.UNPUNCHED,
                    false,
                    false,
                    false,
                    false,
                    "当前未读取到考勤规则"
            );
        }

        LocalTime now = referenceTime == null ? LocalTime.now() : referenceTime;
        int lateGraceMinutes = resolveNonNegativeMinutes(rule.getLateGraceMinutes());
        int earlyLeaveGraceMinutes = resolveNonNegativeMinutes(rule.getEarlyLeaveGraceMinutes());

        if (NODE_AM_ON.equals(nodeCode)) {
            if (rule.getAmOffTime() != null && !now.isBefore(rule.getAmOffTime())) {
                return new BaseNodeState(
                        AttendanceCheckInStatus.NEED_MAKEUP,
                        false,
                        true,
                        true,
                        false,
                        "上午上班节点已过普通打卡时段"
                );
            }
            LocalTime lateThreshold = rule.getWorkStartTime() == null
                    ? null
                    : rule.getWorkStartTime().plusMinutes(lateGraceMinutes);
            if (lateThreshold != null && now.isAfter(lateThreshold)) {
                return new BaseNodeState(
                        AttendanceCheckInStatus.LATE,
                        true,
                        false,
                        false,
                        false,
                        "当前仍可打卡，打卡后记为迟到"
                );
            }
            return new BaseNodeState(
                    AttendanceCheckInStatus.NORMAL,
                    true,
                    false,
                    false,
                    false,
                    "当前可正常打卡"
            );
        }

        if (NODE_AM_OFF.equals(nodeCode)) {
            if (rule.getWorkStartTime() != null && now.isBefore(rule.getWorkStartTime())) {
                return new BaseNodeState(
                        AttendanceCheckInStatus.UNPUNCHED,
                        false,
                        false,
                        false,
                        false,
                        "未到上午下班打卡时段"
                );
            }
            if (rule.getPmOnTime() != null && !now.isBefore(rule.getPmOnTime())) {
                return new BaseNodeState(
                        AttendanceCheckInStatus.NEED_MAKEUP,
                        false,
                        true,
                        true,
                        false,
                        "上午下班节点已过普通打卡时段"
                );
            }
            LocalTime normalStart = rule.getAmOffTime() == null
                    ? null
                    : rule.getAmOffTime().minusMinutes(earlyLeaveGraceMinutes);
            if (normalStart != null && now.isBefore(normalStart)) {
                return new BaseNodeState(
                        AttendanceCheckInStatus.EARLY,
                        true,
                        false,
                        false,
                        true,
                        "当前可打卡，打卡后记为早退"
                );
            }
            return new BaseNodeState(
                    AttendanceCheckInStatus.NORMAL,
                    true,
                    false,
                    false,
                    false,
                    "当前可正常打卡"
            );
        }

        if (NODE_PM_ON.equals(nodeCode)) {
            if (rule.getPmOnTime() != null && now.isBefore(rule.getPmOnTime())) {
                return new BaseNodeState(
                        AttendanceCheckInStatus.UNPUNCHED,
                        false,
                        false,
                        false,
                        false,
                        "未到下午上班打卡时段"
                );
            }
            if (rule.getWorkEndTime() != null && !now.isBefore(rule.getWorkEndTime())) {
                return new BaseNodeState(
                        AttendanceCheckInStatus.NEED_MAKEUP,
                        false,
                        true,
                        true,
                        false,
                        "下午上班节点已过普通打卡时段"
                );
            }
            LocalTime lateThreshold = rule.getPmOnTime() == null
                    ? null
                    : rule.getPmOnTime().plusMinutes(lateGraceMinutes);
            if (lateThreshold != null && now.isAfter(lateThreshold)) {
                return new BaseNodeState(
                        AttendanceCheckInStatus.LATE,
                        true,
                        false,
                        false,
                        false,
                        "当前仍可打卡，打卡后记为迟到"
                );
            }
            return new BaseNodeState(
                    AttendanceCheckInStatus.NORMAL,
                    true,
                    false,
                    false,
                    false,
                    "当前可正常打卡"
            );
        }

        if (rule.getPmOnTime() != null && now.isBefore(rule.getPmOnTime())) {
            return new BaseNodeState(
                    AttendanceCheckInStatus.UNPUNCHED,
                    false,
                    false,
                    false,
                    false,
                    "未到下午下班打卡时段"
            );
        }
        if (referenceTime != null && LocalTime.MAX.equals(referenceTime)) {
            return new BaseNodeState(
                    AttendanceCheckInStatus.NEED_MAKEUP,
                    false,
                    true,
                    true,
                    false,
                    "下午下班节点已过普通打卡时段"
            );
        }
        LocalTime normalStart = rule.getWorkEndTime() == null
                ? null
                : rule.getWorkEndTime().minusMinutes(earlyLeaveGraceMinutes);
        if (normalStart != null && now.isBefore(normalStart)) {
            return new BaseNodeState(
                    AttendanceCheckInStatus.EARLY,
                    true,
                    false,
                    false,
                    true,
                    "当前可打卡，打卡后记为早退"
            );
        }
        return new BaseNodeState(
                AttendanceCheckInStatus.NORMAL,
                true,
                false,
                false,
                false,
                "当前可正常打卡"
        );
    }

    private static ResolvedAttendanceState buildResolvedState(List<AttendanceNodeStatusVO> nodeStates,
                                                              AttendanceRuleEntity rule,
                                                              AttendanceRecordEntity record) {
        AttendanceNodeStatusVO currentNode = null;
        AttendanceNodeStatusVO firstUnresolved = null;
        boolean allResolved = true;
        boolean late = false;
        boolean early = false;

        for (AttendanceNodeStatusVO node : nodeStates) {
            if (Boolean.TRUE.equals(node.getCanPunch()) && currentNode == null) {
                currentNode = node;
            }
            if (!Boolean.TRUE.equals(node.getFinalResolved()) && firstUnresolved == null) {
                firstUnresolved = node;
            }
            if (!Boolean.TRUE.equals(node.getFinalResolved())) {
                allResolved = false;
            }
        }

        if (record != null) {
            late = isLate(rule, record);
            early = isEarly(rule, record);
        }

        return new ResolvedAttendanceState(
                nodeStates,
                currentNode == null ? null : currentNode.getNodeCode(),
                currentNode == null ? "" : resolveActionLabel(currentNode.getNodeCode()),
                currentNode != null,
                allResolved,
                resolveDayHint(currentNode, firstUnresolved, allResolved),
                resolveDayStatusCode(nodeStates, currentNode, firstUnresolved, allResolved, late, early),
                allResolved,
                late,
                early
        );
    }

    private static String resolveDayStatusCode(List<AttendanceNodeStatusVO> nodeStates,
                                               AttendanceNodeStatusVO currentNode,
                                               AttendanceNodeStatusVO firstUnresolved,
                                               boolean allResolved,
                                               boolean late,
                                               boolean early) {
        if (containsStatus(nodeStates, AttendanceCheckInStatus.EVIDENCE_PENDING)
                || containsStatus(nodeStates, AttendanceCheckInStatus.MAKEUP_PENDING)) {
            return resolvePendingStatus(nodeStates);
        }
        if (containsStatus(nodeStates, AttendanceCheckInStatus.EVIDENCE_REQUIRED)) {
            return AttendanceCheckInStatus.EVIDENCE_REQUIRED;
        }
        if (containsStatus(nodeStates, AttendanceCheckInStatus.NEED_MAKEUP)
                || containsStatus(nodeStates, AttendanceCheckInStatus.MAKEUP_REJECTED)
                || containsStatus(nodeStates, AttendanceCheckInStatus.EVIDENCE_REJECTED)) {
            return AttendanceCheckInStatus.NEED_MAKEUP;
        }
        if (allResolved) {
            if (late && early) {
                return AttendanceCheckInStatus.LATE_EARLY;
            }
            if (late) {
                return AttendanceCheckInStatus.LATE;
            }
            if (early) {
                return AttendanceCheckInStatus.EARLY;
            }
            return AttendanceCheckInStatus.NORMAL;
        }
        if (currentNode != null) {
            return currentNode.getStatusCode();
        }
        if (firstUnresolved != null) {
            return firstUnresolved.getStatusCode();
        }
        return AttendanceCheckInStatus.NORMAL;
    }

    private static boolean containsStatus(List<AttendanceNodeStatusVO> nodeStates, String statusCode) {
        for (AttendanceNodeStatusVO node : nodeStates) {
            if (statusCode.equals(node.getStatusCode())) {
                return true;
            }
        }
        return false;
    }

    private static String resolveDayHint(AttendanceNodeStatusVO currentNode,
                                         AttendanceNodeStatusVO firstUnresolved,
                                         boolean allResolved) {
        if (currentNode != null) {
            return currentNode.getSimpleRemark();
        }
        if (allResolved) {
            return "今日四个节点已全部完成";
        }
        if (firstUnresolved != null) {
            return firstUnresolved.getSimpleRemark();
        }
        return "当前暂无可执行节点";
    }

    private static String resolvePendingStatus(List<AttendanceNodeStatusVO> nodeStates) {
        for (AttendanceNodeStatusVO node : nodeStates) {
            if (AttendanceCheckInStatus.EVIDENCE_PENDING.equals(node.getStatusCode())) {
                return AttendanceCheckInStatus.EVIDENCE_PENDING;
            }
        }
        return AttendanceCheckInStatus.MAKEUP_PENDING;
    }

    private static LocalDateTime resolveActualPunchTime(AttendanceRecordEntity record, String nodeCode) {
        if (record == null) {
            return null;
        }
        if (NODE_AM_ON.equals(nodeCode)) {
            return record.getCheckInTime();
        }
        if (NODE_AM_OFF.equals(nodeCode)) {
            return record.getAmOffTime();
        }
        if (NODE_PM_ON.equals(nodeCode)) {
            return record.getPmOnTime();
        }
        return record.getCheckOutTime();
    }

    private static String classifyResolvedStatus(AttendanceRuleEntity rule,
                                                 String nodeCode,
                                                 LocalDateTime actualPunchTime) {
        if (actualPunchTime == null || rule == null) {
            return AttendanceCheckInStatus.NORMAL;
        }
        LocalTime actualTime = actualPunchTime.toLocalTime();
        int lateGraceMinutes = resolveNonNegativeMinutes(rule.getLateGraceMinutes());
        int earlyLeaveGraceMinutes = resolveNonNegativeMinutes(rule.getEarlyLeaveGraceMinutes());
        if (NODE_AM_ON.equals(nodeCode) && rule.getWorkStartTime() != null) {
            return actualTime.isAfter(rule.getWorkStartTime().plusMinutes(lateGraceMinutes))
                    ? AttendanceCheckInStatus.LATE
                    : AttendanceCheckInStatus.NORMAL;
        }
        if (NODE_PM_ON.equals(nodeCode) && rule.getPmOnTime() != null) {
            return actualTime.isAfter(rule.getPmOnTime().plusMinutes(lateGraceMinutes))
                    ? AttendanceCheckInStatus.LATE
                    : AttendanceCheckInStatus.NORMAL;
        }
        if (NODE_AM_OFF.equals(nodeCode) && rule.getAmOffTime() != null) {
            return actualTime.isBefore(rule.getAmOffTime().minusMinutes(earlyLeaveGraceMinutes))
                    ? AttendanceCheckInStatus.EARLY
                    : AttendanceCheckInStatus.NORMAL;
        }
        if (NODE_PM_OFF.equals(nodeCode) && rule.getWorkEndTime() != null) {
            return actualTime.isBefore(rule.getWorkEndTime().minusMinutes(earlyLeaveGraceMinutes))
                    ? AttendanceCheckInStatus.EARLY
                    : AttendanceCheckInStatus.NORMAL;
        }
        return AttendanceCheckInStatus.NORMAL;
    }

    private static String buildResolvedRemark(String resolvedStatus) {
        if (AttendanceCheckInStatus.LATE.equals(resolvedStatus)) {
            return "已打卡，记为迟到";
        }
        if (AttendanceCheckInStatus.EARLY.equals(resolvedStatus)) {
            return "已打卡，记为早退";
        }
        return "已打卡";
    }

    private static String buildApprovedRemark(String applyType, String resolvedStatus) {
        String prefix = AttendanceApplyType.EVIDENCE.equals(applyType) ? "取证已通过，" : "补打卡已通过，";
        if (AttendanceCheckInStatus.LATE.equals(resolvedStatus)) {
            return prefix + "按迟到有效打卡处理";
        }
        if (AttendanceCheckInStatus.EARLY.equals(resolvedStatus)) {
            return prefix + "按早退有效打卡处理";
        }
        return prefix + "按有效打卡处理";
    }

    private static String resolveNodeTitle(String nodeCode) {
        if (NODE_AM_ON.equals(nodeCode)) {
            return "上午上班";
        }
        if (NODE_AM_OFF.equals(nodeCode)) {
            return "上午下班";
        }
        if (NODE_PM_ON.equals(nodeCode)) {
            return "下午上班";
        }
        return "下午下班";
    }

    public static String resolveActionLabel(String action) {
        if (NODE_AM_ON.equals(action)) {
            return "上午上班打卡";
        }
        if (NODE_AM_OFF.equals(action)) {
            return "上午下班打卡";
        }
        if (NODE_PM_ON.equals(action)) {
            return "下午上班打卡";
        }
        if (NODE_PM_OFF.equals(action)) {
            return "下午下班打卡";
        }
        return "今日已完成";
    }

    private static String resolveTimeRangeText(AttendanceRuleEntity rule, String nodeCode) {
        if (rule == null) {
            return "-";
        }
        if (NODE_AM_ON.equals(nodeCode)) {
            return formatWindow(rule.getWorkStartTime(), rule.getAmOffTime());
        }
        if (NODE_AM_OFF.equals(nodeCode)) {
            return formatWindow(rule.getWorkStartTime(), rule.getPmOnTime());
        }
        if (NODE_PM_ON.equals(nodeCode)) {
            return formatWindow(rule.getPmOnTime(), rule.getWorkEndTime());
        }
        return rule.getPmOnTime() == null ? "-" : TIME_FORMATTER.format(rule.getPmOnTime()) + "后";
    }

    private static String formatWindow(LocalTime start, LocalTime end) {
        if (start == null && end == null) {
            return "-";
        }
        if (start == null) {
            return TIME_FORMATTER.format(end) + "前";
        }
        if (end == null) {
            return TIME_FORMATTER.format(start) + "后";
        }
        return TIME_FORMATTER.format(start) + "-" + TIME_FORMATTER.format(end);
    }

    private static int resolveNonNegativeMinutes(Integer minutes) {
        return minutes == null ? 0 : Math.max(minutes, 0);
    }

    private static boolean isLate(AttendanceRuleEntity rule, AttendanceRecordEntity record) {
        if (rule == null || record == null) {
            return false;
        }
        return (record.getCheckInTime() != null
                && rule.getWorkStartTime() != null
                && record.getCheckInTime().toLocalTime().isAfter(rule.getWorkStartTime().plusMinutes(resolveNonNegativeMinutes(rule.getLateGraceMinutes()))))
                || (record.getPmOnTime() != null
                && rule.getPmOnTime() != null
                && record.getPmOnTime().toLocalTime().isAfter(rule.getPmOnTime().plusMinutes(resolveNonNegativeMinutes(rule.getLateGraceMinutes()))));
    }

    private static boolean isEarly(AttendanceRuleEntity rule, AttendanceRecordEntity record) {
        if (rule == null || record == null) {
            return false;
        }
        return (record.getAmOffTime() != null
                && rule.getAmOffTime() != null
                && record.getAmOffTime().toLocalTime().isBefore(rule.getAmOffTime().minusMinutes(resolveNonNegativeMinutes(rule.getEarlyLeaveGraceMinutes()))))
                || (record.getCheckOutTime() != null
                && rule.getWorkEndTime() != null
                && record.getCheckOutTime().toLocalTime().isBefore(rule.getWorkEndTime().minusMinutes(resolveNonNegativeMinutes(rule.getEarlyLeaveGraceMinutes()))));
    }

    public static final class ResolvedAttendanceState {
        private final List<AttendanceNodeStatusVO> nodeStates;
        private final String currentAction;
        private final String currentActionLabel;
        private final boolean currentActionAvailable;
        private final boolean finished;
        private final String hint;
        private final String finalStatusCode;
        private final boolean finalResolved;
        private final boolean late;
        private final boolean early;

        private ResolvedAttendanceState(List<AttendanceNodeStatusVO> nodeStates,
                                        String currentAction,
                                        String currentActionLabel,
                                        boolean currentActionAvailable,
                                        boolean finished,
                                        String hint,
                                        String finalStatusCode,
                                        boolean finalResolved,
                                        boolean late,
                                        boolean early) {
            this.nodeStates = nodeStates;
            this.currentAction = currentAction;
            this.currentActionLabel = currentActionLabel;
            this.currentActionAvailable = currentActionAvailable;
            this.finished = finished;
            this.hint = hint;
            this.finalStatusCode = finalStatusCode;
            this.finalResolved = finalResolved;
            this.late = late;
            this.early = early;
        }

        public List<AttendanceNodeStatusVO> getNodeStates() {
            return nodeStates;
        }

        public String getCurrentAction() {
            return currentAction;
        }

        public String getCurrentActionLabel() {
            return currentActionLabel;
        }

        public boolean isCurrentActionAvailable() {
            return currentActionAvailable;
        }

        public boolean isFinished() {
            return finished;
        }

        public String getHint() {
            return hint;
        }

        public String getFinalStatusCode() {
            return finalStatusCode;
        }

        public boolean isFinalResolved() {
            return finalResolved;
        }

        public boolean isLate() {
            return late;
        }

        public boolean isEarly() {
            return early;
        }

        public boolean isSuccessfulDay() {
            return finalResolved;
        }
    }

    private static final class BaseNodeState {
        private final String statusCode;
        private final boolean canPunch;
        private final boolean canApplyMakeup;
        private final boolean canEvidence;
        private final boolean needEarlyConfirm;
        private final String remark;
        private final String fallbackRemark;

        private BaseNodeState(String statusCode,
                              boolean canPunch,
                              boolean canApplyMakeup,
                              boolean canEvidence,
                              boolean needEarlyConfirm,
                              String remark) {
            this.statusCode = statusCode;
            this.canPunch = canPunch;
            this.canApplyMakeup = canApplyMakeup;
            this.canEvidence = canEvidence;
            this.needEarlyConfirm = needEarlyConfirm;
            this.remark = remark;
            this.fallbackRemark = remark;
        }
    }
}
