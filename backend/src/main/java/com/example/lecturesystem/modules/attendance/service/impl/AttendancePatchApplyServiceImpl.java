package com.example.lecturesystem.modules.attendance.service.impl;

import com.example.lecturesystem.modules.attendance.dto.AttendancePatchApplyQueryRequest;
import com.example.lecturesystem.modules.attendance.dto.ReviewAttendancePatchApplyRequest;
import com.example.lecturesystem.modules.attendance.dto.SubmitAttendancePatchApplyRequest;
import com.example.lecturesystem.modules.attendance.entity.AttendancePatchApplyEntity;
import com.example.lecturesystem.modules.attendance.entity.AttendancePatchApplyLogEntity;
import com.example.lecturesystem.modules.attendance.entity.AttendancePatchApplyNodeEntity;
import com.example.lecturesystem.modules.attendance.entity.AttendanceRecordEntity;
import com.example.lecturesystem.modules.attendance.mapper.AttendanceMapper;
import com.example.lecturesystem.modules.attendance.mapper.AttendancePatchApplyLogMapper;
import com.example.lecturesystem.modules.attendance.mapper.AttendancePatchApplyMapper;
import com.example.lecturesystem.modules.attendance.mapper.AttendancePatchApplyNodeMapper;
import com.example.lecturesystem.modules.attendance.service.AttendancePatchApprovalNoticeService;
import com.example.lecturesystem.modules.attendance.service.AttendancePatchApplyService;
import com.example.lecturesystem.modules.attendance.support.AttendanceApplyType;
import com.example.lecturesystem.modules.attendance.support.AttendanceCheckInStatus;
import com.example.lecturesystem.modules.attendance.support.AttendanceWeeklyReadonlyScopeService;
import com.example.lecturesystem.modules.attendance.vo.AttendancePatchApplyDetailVO;
import com.example.lecturesystem.modules.attendance.vo.AttendancePatchApplyListItemVO;
import com.example.lecturesystem.modules.attendance.vo.AttendancePatchApplyPageVO;
import com.example.lecturesystem.modules.permission.service.PermissionService;
import com.example.lecturesystem.modules.permission.support.CurrentUserFacade;
import com.example.lecturesystem.modules.permission.support.DataScopeService;
import com.example.lecturesystem.modules.user.entity.UserEntity;
import com.example.lecturesystem.modules.user.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class AttendancePatchApplyServiceImpl implements AttendancePatchApplyService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AttendancePatchApplyServiceImpl.class);

    private static final String PATCH_TYPE_AM_ON = "AM_ON";
    private static final String PATCH_TYPE_AM_OFF = "AM_OFF";
    private static final String PATCH_TYPE_PM_ON = "PM_ON";
    private static final String PATCH_TYPE_PM_OFF = "PM_OFF";

    private static final String PATCH_STATUS_PENDING = "PENDING";
    private static final String PATCH_STATUS_APPROVED = "APPROVED";
    private static final String PATCH_STATUS_REJECTED = "REJECTED";
    private static final String PATCH_ADDRESS_TEXT_MAKEUP = "补打卡审批";
private static final String PATCH_ADDRESS_TEXT_EVIDENCE = "取证审批";
    private static final String APPLY_TYPE_TEXT_MAKEUP = "补打卡";
    private static final String APPLY_TYPE_TEXT_EVIDENCE = "取证";

    private static final String NODE_STATUS_WAITING = "WAITING";
    private static final String NODE_STATUS_PENDING = "PENDING";
    private static final String NODE_STATUS_APPROVED = "APPROVED";
    private static final String NODE_STATUS_REJECTED = "REJECTED";
    private static final String NODE_STATUS_SKIPPED = "SKIPPED";

    private static final String ACTION_TYPE_SUBMIT = "SUBMIT";
    private static final String ACTION_TYPE_APPROVE = "APPROVE";
    private static final String ACTION_TYPE_REJECT = "REJECT";
    private static final String ACTION_TYPE_FORWARD = "FORWARD";
    private static final String ACTION_TYPE_SYSTEM = "SYSTEM";

    private static final String NODE_CODE_DIRECT_LEADER = "DIRECT_LEADER";
    private static final String NODE_CODE_FINAL_LEADER = "FINAL_LEADER";
    private static final String NODE_NAME_DIRECT_LEADER = "分管审核";
    private static final String NODE_NAME_UPPER_LEADER = "上级审核";
    private static final String NODE_NAME_FINAL_LEADER = "团长终审";
    private static final String NODE_NAME_PENDING_CONFIG = "待配置审批人";
    private static final String NODE_NAME_FINISHED = "审批完成";
    private static final String NODE_NAME_REJECTED = "审批驳回";

    private static final int MAX_APPROVER_COUNT = 2;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AttendancePatchApplyMapper attendancePatchApplyMapper;
    private final AttendancePatchApplyNodeMapper attendancePatchApplyNodeMapper;
    private final AttendancePatchApplyLogMapper attendancePatchApplyLogMapper;
    private final AttendanceMapper attendanceMapper;
    private final CurrentUserFacade currentUserFacade;
    private final DataScopeService dataScopeService;
    private final UserMapper userMapper;
    private final PermissionService permissionService;
    private final AttendanceWeeklyReadonlyScopeService attendanceWeeklyReadonlyScopeService;
    private final AttendancePatchApprovalNoticeService attendancePatchApprovalNoticeService;

    public AttendancePatchApplyServiceImpl(AttendancePatchApplyMapper attendancePatchApplyMapper,
                                           AttendancePatchApplyNodeMapper attendancePatchApplyNodeMapper,
                                           AttendancePatchApplyLogMapper attendancePatchApplyLogMapper,
                                           AttendanceMapper attendanceMapper,
                                           CurrentUserFacade currentUserFacade,
                                           DataScopeService dataScopeService,
                                           UserMapper userMapper,
                                           PermissionService permissionService,
                                           AttendanceWeeklyReadonlyScopeService attendanceWeeklyReadonlyScopeService,
                                           AttendancePatchApprovalNoticeService attendancePatchApprovalNoticeService) {
        this.attendancePatchApplyMapper = attendancePatchApplyMapper;
        this.attendancePatchApplyNodeMapper = attendancePatchApplyNodeMapper;
        this.attendancePatchApplyLogMapper = attendancePatchApplyLogMapper;
        this.attendanceMapper = attendanceMapper;
        this.currentUserFacade = currentUserFacade;
        this.dataScopeService = dataScopeService;
        this.userMapper = userMapper;
        this.permissionService = permissionService;
        this.attendanceWeeklyReadonlyScopeService = attendanceWeeklyReadonlyScopeService;
        this.attendancePatchApprovalNoticeService = attendancePatchApprovalNoticeService;
    }

    @Override
    @Transactional
    public Long submitApply(SubmitAttendancePatchApplyRequest request) {
        UserEntity currentUser = currentUserFacade.currentUserEntity();
        if (currentUser.getUnitId() == null) {
            throw new IllegalArgumentException("当前用户未绑定单位，无法提交补打卡申请");
        }

        LocalDate attendanceDate = parseDate(request.getAttendanceDate());
        LocalDateTime patchTime = parseDateTime(request.getPatchTime());
        String patchType = normalizePatchType(request.getPatchType());
        String applyType = AttendanceApplyType.normalize(request == null ? null : request.getApplyType());
        String reason = normalizeText(request.getReason());
        if (reason == null) {
            throw new IllegalArgumentException("补卡原因不能为空");
        }
        if (!attendanceDate.equals(patchTime.toLocalDate())) {
            throw new IllegalArgumentException("补卡时间必须属于考勤日期当天");
        }

        if (attendancePatchApplyMapper.findPendingByUserDateType(currentUser.getId(), attendanceDate, patchType) != null) {
            throw new IllegalArgumentException("当天该节点已存在待审批记录，请勿重复提交");
        }

        AttendanceRecordEntity existedRecord = attendanceMapper.findByUserIdAndDate(currentUser.getId(), attendanceDate);
        validatePatchSubmitAgainstRecord(existedRecord, patchType, patchTime);

        List<UserEntity> approvalChain = resolveApprovalChain(currentUser);
        if (approvalChain.isEmpty()) {
            LOGGER.warn(
                    "No attendance patch approver found, applyType={}, submitterUserId={}, submitterName={}, parentUserId={}, action=configure_parent_user_or_patch_approver",
                    applyType,
                    currentUser.getId(),
                    resolveDisplayName(currentUser),
                    currentUser.getParentUserId()
            );
            throw new IllegalArgumentException("\u672a\u627e\u5230\u53ef\u7528\u5ba1\u6279\u4eba\uff0c\u8bf7\u8054\u7cfb\u7ba1\u7406\u5458\u914d\u7f6e\u76f4\u5c5e\u4e0a\u7ea7\u6216\u8865\u6253\u5361\u5ba1\u6279\u4eba\u3002");
        }

        AttendancePatchApplyEntity entity = new AttendancePatchApplyEntity();
        entity.setUserId(currentUser.getId());
        entity.setUnitId(currentUser.getUnitId());
        entity.setAttendanceDate(attendanceDate);
        entity.setPatchType(patchType);
        entity.setApplyType(applyType);
        entity.setPatchTime(patchTime);
        entity.setReason(reason);
        entity.setAttachmentsJson(normalizeText(request.getAttachmentsJson()));
        entity.setStatus(PATCH_STATUS_PENDING);
        entity.setValidFlag(1);
        attendancePatchApplyMapper.insert(entity);

        List<AttendancePatchApplyNodeEntity> nodes = buildApprovalNodes(entity.getId(), approvalChain);
        if (!nodes.isEmpty()) {
            attendancePatchApplyNodeMapper.batchInsert(nodes);
        }
        insertLog(buildLog(
                entity.getId(),
                ACTION_TYPE_SUBMIT,
                currentUser.getId(),
                resolveDisplayName(currentUser),
                null,
                null,
                "提交" + resolveApplyTypeText(applyType) + "申请"
        ));
        if (nodes.isEmpty()) {
            insertLog(buildLog(
                    entity.getId(),
                    ACTION_TYPE_SYSTEM,
                    null,
                    "SYSTEM",
                    null,
                    NODE_NAME_PENDING_CONFIG,
                    "未找到可用审批人，请联系管理员配置审批链"
            ));
        } else {
            AttendancePatchApplyNodeEntity firstNode = nodes.get(0);
            insertLog(buildLog(
                    entity.getId(),
                    ACTION_TYPE_FORWARD,
                    null,
                    "SYSTEM",
                    firstNode.getNodeCode(),
                    firstNode.getNodeName(),
                    "流转至" + firstNode.getApproverName()
            ));
            notifyPendingApproval(entity, firstNode);
        }
        return entity.getId();
    }

    @Override
    public Object queryMyPage(AttendancePatchApplyQueryRequest request) {
        UserEntity currentUser = currentUserFacade.currentUserEntity();
        AttendancePatchApplyQueryRequest normalized = normalizeQueryRequest(request);
        AttendancePatchApplyPageVO page = new AttendancePatchApplyPageVO();
        page.setPageNo(normalized.getPageNo());
        page.setPageSize(normalized.getPageSize());
        page.setTotal(attendancePatchApplyMapper.countMyPage(currentUser.getId(), normalized));
        List<AttendancePatchApplyListItemVO> list = attendancePatchApplyMapper.queryMyPage(currentUser.getId(), normalized);
        enrichMyPageItems(list);
        page.setList(list);
        return page;
    }

    @Override
    public Object queryPendingPage(AttendancePatchApplyQueryRequest request) {
        UserEntity currentUser = currentUserFacade.currentUserEntity();
        AttendancePatchApplyQueryRequest normalized = normalizeQueryRequest(request);
        AttendancePatchApplyPageVO page = new AttendancePatchApplyPageVO();
        page.setPageNo(normalized.getPageNo());
        page.setPageSize(normalized.getPageSize());
        page.setTotal(attendancePatchApplyMapper.countPendingPage(null, currentUser.getId(), normalized));
        List<AttendancePatchApplyListItemVO> list = attendancePatchApplyMapper.queryPendingPage(null, currentUser.getId(), normalized);
        enrichPendingPageItems(list, currentUser, normalizeText(normalized.getStatus()));
        page.setList(list);
        return page;
    }

    @Override
    public Object detail(Long id) {
        AttendancePatchApplyEntity entity = requirePatchApply(id);
        UserEntity currentUser = currentUserFacade.currentUserEntity();
        UserEntity targetUser = requireUser(entity.getUserId());
        AttendanceWeeklyReadonlyScopeService.ReadonlyScope readonlyScope = resolveReadonlyScope(currentUser);
        validateReadableApply(currentUser, targetUser, readonlyScope);
        AttendancePatchApplyDetailVO detail = attendancePatchApplyMapper.detailById(id);
        if (detail == null) {
            throw new IllegalArgumentException("补打卡申请不存在");
        }
        enrichDetailItem(detail, currentUser, targetUser, readonlyScope);
        return detail;
    }

    @Override
    @Transactional
    public void approve(Long id, ReviewAttendancePatchApplyRequest request) {
        AttendancePatchApplyEntity entity = requirePatchApply(id);
        UserEntity currentUser = currentUserFacade.currentUserEntity();
        validateReviewableApply(currentUser, entity);

        AttendancePatchApplyNodeEntity currentNode = requireCurrentPendingNodeForApprover(entity.getId(), currentUser.getId());
        LocalDateTime now = LocalDateTime.now();
        String comment = normalizeText(request == null ? null : request.getApproveComment());

        currentNode.setStatus(NODE_STATUS_APPROVED);
        currentNode.setApproveTime(now);
        currentNode.setApproveComment(comment);
        currentNode.setUpdateTime(now);
        attendancePatchApplyNodeMapper.updateReview(currentNode);

        AttendancePatchApplyNodeEntity nextNode = findNextWaitingNode(entity.getId(), currentNode.getNodeOrder());
        entity.setApproveUserId(currentUser.getId());
        entity.setApproveTime(now);
        entity.setApproveComment(comment);
        entity.setUpdateTime(now);

        insertLog(buildLog(
                entity.getId(),
                ACTION_TYPE_APPROVE,
                currentUser.getId(),
                resolveDisplayName(currentUser),
                currentNode.getNodeCode(),
                currentNode.getNodeName(),
                comment
        ));

        if (nextNode != null) {
            attendancePatchApplyNodeMapper.activateNode(nextNode.getId(), now);
            entity.setStatus(PATCH_STATUS_PENDING);
            attendancePatchApplyMapper.updateStatusAndReview(entity);
            insertLog(buildLog(
                    entity.getId(),
                    ACTION_TYPE_FORWARD,
                    null,
                    "SYSTEM",
                    nextNode.getNodeCode(),
                    nextNode.getNodeName(),
                    "流转至" + nextNode.getApproverName()
            ));
            notifyPendingApproval(entity, nextNode);
            return;
        }

        applyApprovedAttendance(entity);
        entity.setStatus(PATCH_STATUS_APPROVED);
        attendancePatchApplyMapper.updateStatusAndReview(entity);
    }

    @Override
    @Transactional
    public void reject(Long id, ReviewAttendancePatchApplyRequest request) {
        AttendancePatchApplyEntity entity = requirePatchApply(id);
        UserEntity currentUser = currentUserFacade.currentUserEntity();
        validateReviewableApply(currentUser, entity);
        String comment = normalizeText(request == null ? null : request.getApproveComment());
        if (comment == null) {
            throw new IllegalArgumentException("拒绝补打卡时请填写审批意见");
        }

        AttendancePatchApplyNodeEntity currentNode = requireCurrentPendingNodeForApprover(entity.getId(), currentUser.getId());
        LocalDateTime now = LocalDateTime.now();
        currentNode.setStatus(NODE_STATUS_REJECTED);
        currentNode.setApproveTime(now);
        currentNode.setApproveComment(comment);
        currentNode.setUpdateTime(now);
        attendancePatchApplyNodeMapper.updateReview(currentNode);
        attendancePatchApplyNodeMapper.updateRemainingNodeStatus(
                entity.getId(),
                currentNode.getNodeOrder() + 1,
                NODE_STATUS_SKIPPED,
                now
        );

        entity.setStatus(PATCH_STATUS_REJECTED);
        entity.setApproveUserId(currentUser.getId());
        entity.setApproveTime(now);
        entity.setApproveComment(comment);
        entity.setUpdateTime(now);
        attendancePatchApplyMapper.updateStatusAndReview(entity);

        insertLog(buildLog(
                entity.getId(),
                ACTION_TYPE_REJECT,
                currentUser.getId(),
                resolveDisplayName(currentUser),
                currentNode.getNodeCode(),
                currentNode.getNodeName(),
                comment
        ));
    }

    private void enrichMyPageItems(List<AttendancePatchApplyListItemVO> list) {
        Map<Long, List<AttendancePatchApplyNodeEntity>> nodeMap = queryNodeMap(list);
        Map<Long, AttendancePatchApplyLogEntity> latestLogMap = queryLatestLogMap(list);
        for (AttendancePatchApplyListItemVO item : list) {
            AttendancePatchApplyNodeEntity currentNode = resolveCurrentNode(nodeMap.get(item.getId()), item.getStatus());
            AttendancePatchApplyLogEntity latestLog = latestLogMap.get(item.getId());
            applyItemContext(item, currentNode, latestLog, false, false);
        }
    }

    private void enrichPendingPageItems(List<AttendancePatchApplyListItemVO> list,
                                        UserEntity currentUser,
                                        String requestedStatus) {
        Map<Long, List<AttendancePatchApplyNodeEntity>> nodeMap = queryNodeMap(list);
        Map<Long, AttendancePatchApplyLogEntity> latestLogMap = queryLatestLogMap(list);
        for (AttendancePatchApplyListItemVO item : list) {
            List<AttendancePatchApplyNodeEntity> nodes = nodeMap.get(item.getId());
            AttendancePatchApplyNodeEntity currentNode = resolveCurrentNode(nodes, item.getStatus());
            AttendancePatchApplyNodeEntity reviewerNode = resolveReviewerNode(nodes, currentUser.getId(), requestedStatus);
            boolean canApprove = reviewerNode != null
                    && NODE_STATUS_PENDING.equals(reviewerNode.getStatus())
                    && currentUser.getId() != null
                    && currentUser.getId().equals(reviewerNode.getApproverUserId());
            applyItemContext(item, currentNode, latestLogMap.get(item.getId()), canApprove, false);
            applyReviewerResultContext(item, reviewerNode);
        }
    }

    private void enrichDetailItem(AttendancePatchApplyListItemVO item,
                                  UserEntity currentUser,
                                  UserEntity targetUser,
                                  AttendanceWeeklyReadonlyScopeService.ReadonlyScope readonlyScope) {
        List<AttendancePatchApplyNodeEntity> nodes = attendancePatchApplyNodeMapper.queryByApplyId(item.getId());
        AttendancePatchApplyNodeEntity currentNode = resolveCurrentNode(nodes, item.getStatus());
        AttendancePatchApplyLogEntity latestLog = firstOrNull(attendancePatchApplyLogMapper.queryByApplyId(item.getId()));
        boolean canApprove = currentNode != null
                && NODE_STATUS_PENDING.equals(currentNode.getStatus())
                && currentUser.getId() != null
                && currentUser.getId().equals(currentNode.getApproverUserId())
                && !currentUser.getId().equals(targetUser.getId())
                && !isCrossDeptReadonlyTarget(currentUser, targetUser, readonlyScope);
        boolean readonlyMode = !currentUser.getId().equals(targetUser.getId()) && !canApprove;
        applyItemContext(item, currentNode, latestLog, canApprove, readonlyMode);
    }

    private void applyItemContext(AttendancePatchApplyListItemVO item,
                                  AttendancePatchApplyNodeEntity currentNode,
                                  AttendancePatchApplyLogEntity latestLog,
                                  boolean canApprove,
                                  boolean readonlyMode) {
        item.setApplyTypeText(resolveApplyTypeText(item.getApplyType()));
        item.setCanApprove(canApprove);
        item.setReadonlyMode(readonlyMode);
        if (currentNode != null) {
            item.setCurrentNodeCode(currentNode.getNodeCode());
            item.setCurrentNodeName(currentNode.getNodeName());
            item.setCurrentApproverUserId(currentNode.getApproverUserId());
            item.setCurrentApproverName(currentNode.getApproverName());
        } else if (PATCH_STATUS_PENDING.equals(item.getStatus())) {
            item.setCurrentNodeCode("CONFIG_PENDING");
            item.setCurrentNodeName(NODE_NAME_PENDING_CONFIG);
            item.setCurrentApproverUserId(null);
            item.setCurrentApproverName(null);
        } else if (PATCH_STATUS_APPROVED.equals(item.getStatus())) {
            item.setCurrentNodeCode("FINISHED");
            item.setCurrentNodeName(NODE_NAME_FINISHED);
            item.setCurrentApproverUserId(item.getApproveUserId());
            item.setCurrentApproverName(resolveApproveDisplayName(item));
        } else if (PATCH_STATUS_REJECTED.equals(item.getStatus())) {
            item.setCurrentNodeCode("REJECTED");
            item.setCurrentNodeName(NODE_NAME_REJECTED);
            item.setCurrentApproverUserId(item.getApproveUserId());
            item.setCurrentApproverName(resolveApproveDisplayName(item));
        }

        if (latestLog != null) {
            item.setLatestActionType(latestLog.getActionType());
            item.setLatestActionComment(latestLog.getComment());
            item.setLatestActionTime(latestLog.getCreateTime());
        }
    }

    private AttendancePatchApplyNodeEntity resolveCurrentNode(List<AttendancePatchApplyNodeEntity> nodes, String applyStatus) {
        if (nodes == null || nodes.isEmpty()) {
            return null;
        }
        AttendancePatchApplyNodeEntity pendingNode = nodes.stream()
                .filter(node -> NODE_STATUS_PENDING.equals(node.getStatus()))
                .min(Comparator.comparing(AttendancePatchApplyNodeEntity::getNodeOrder))
                .orElse(null);
        if (pendingNode != null) {
            return pendingNode;
        }
        if (PATCH_STATUS_REJECTED.equals(applyStatus)) {
            return nodes.stream()
                    .filter(node -> NODE_STATUS_REJECTED.equals(node.getStatus()))
                    .max(Comparator.comparing(AttendancePatchApplyNodeEntity::getNodeOrder))
                    .orElse(null);
        }
        if (PATCH_STATUS_APPROVED.equals(applyStatus)) {
            return nodes.stream()
                    .filter(node -> NODE_STATUS_APPROVED.equals(node.getStatus()))
                    .max(Comparator.comparing(AttendancePatchApplyNodeEntity::getNodeOrder))
                    .orElse(null);
        }
        return nodes.stream()
                .filter(node -> !NODE_STATUS_SKIPPED.equals(node.getStatus()))
                .max(Comparator.comparing(AttendancePatchApplyNodeEntity::getNodeOrder))
                .orElse(null);
    }

    private AttendancePatchApplyNodeEntity resolveReviewerNode(List<AttendancePatchApplyNodeEntity> nodes,
                                                               Long approverUserId,
                                                               String requestedStatus) {
        if (nodes == null || nodes.isEmpty() || approverUserId == null) {
            return null;
        }
        List<AttendancePatchApplyNodeEntity> ownedNodes = nodes.stream()
                .filter(node -> approverUserId.equals(node.getApproverUserId()))
                .toList();
        if (ownedNodes.isEmpty()) {
            return null;
        }
        if (PATCH_STATUS_PENDING.equals(requestedStatus)) {
            return ownedNodes.stream()
                    .filter(node -> NODE_STATUS_PENDING.equals(node.getStatus()))
                    .min(Comparator.comparing(AttendancePatchApplyNodeEntity::getNodeOrder))
                    .orElse(null);
        }
        if (PATCH_STATUS_APPROVED.equals(requestedStatus)) {
            return ownedNodes.stream()
                    .filter(node -> NODE_STATUS_APPROVED.equals(node.getStatus()))
                    .max(Comparator.comparing(AttendancePatchApplyNodeEntity::getNodeOrder))
                    .orElse(null);
        }
        if (PATCH_STATUS_REJECTED.equals(requestedStatus)) {
            return ownedNodes.stream()
                    .filter(node -> NODE_STATUS_REJECTED.equals(node.getStatus()))
                    .max(Comparator.comparing(AttendancePatchApplyNodeEntity::getNodeOrder))
                    .orElse(null);
        }
        AttendancePatchApplyNodeEntity pendingNode = ownedNodes.stream()
                .filter(node -> NODE_STATUS_PENDING.equals(node.getStatus()))
                .min(Comparator.comparing(AttendancePatchApplyNodeEntity::getNodeOrder))
                .orElse(null);
        if (pendingNode != null) {
            return pendingNode;
        }
        return ownedNodes.stream()
                .filter(node -> NODE_STATUS_APPROVED.equals(node.getStatus()) || NODE_STATUS_REJECTED.equals(node.getStatus()))
                .max(Comparator
                        .comparing(AttendancePatchApplyNodeEntity::getApproveTime, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(AttendancePatchApplyNodeEntity::getNodeOrder))
                .orElse(null);
    }

    private void applyReviewerResultContext(AttendancePatchApplyListItemVO item,
                                            AttendancePatchApplyNodeEntity reviewerNode) {
        if (item == null || reviewerNode == null) {
            return;
        }
        if (NODE_STATUS_APPROVED.equals(reviewerNode.getStatus()) || NODE_STATUS_REJECTED.equals(reviewerNode.getStatus())) {
            item.setApproveUserId(reviewerNode.getApproverUserId());
            item.setApproveRealName(reviewerNode.getApproverName());
            item.setApproveUsername(null);
            item.setApproveTime(reviewerNode.getApproveTime());
            item.setApproveComment(reviewerNode.getApproveComment());
        }
    }

    private Map<Long, List<AttendancePatchApplyNodeEntity>> queryNodeMap(List<AttendancePatchApplyListItemVO> list) {
        Map<Long, List<AttendancePatchApplyNodeEntity>> nodeMap = new HashMap<>();
        List<Long> applyIds = collectApplyIds(list);
        if (applyIds.isEmpty()) {
            return nodeMap;
        }
        for (AttendancePatchApplyNodeEntity node : attendancePatchApplyNodeMapper.queryByApplyIds(applyIds)) {
            nodeMap.computeIfAbsent(node.getApplyId(), key -> new ArrayList<>()).add(node);
        }
        return nodeMap;
    }

    private Map<Long, AttendancePatchApplyLogEntity> queryLatestLogMap(List<AttendancePatchApplyListItemVO> list) {
        Map<Long, AttendancePatchApplyLogEntity> logMap = new HashMap<>();
        List<Long> applyIds = collectApplyIds(list);
        if (applyIds.isEmpty()) {
            return logMap;
        }
        for (AttendancePatchApplyLogEntity log : attendancePatchApplyLogMapper.queryLatestByApplyIds(applyIds)) {
            logMap.put(log.getApplyId(), log);
        }
        return logMap;
    }

    private List<Long> collectApplyIds(List<AttendancePatchApplyListItemVO> list) {
        List<Long> applyIds = new ArrayList<>();
        if (list == null) {
            return applyIds;
        }
        for (AttendancePatchApplyListItemVO item : list) {
            if (item != null && item.getId() != null) {
                applyIds.add(item.getId());
            }
        }
        return applyIds;
    }

    private AttendancePatchApplyNodeEntity requireCurrentPendingNodeForApprover(Long applyId, Long approverUserId) {
        AttendancePatchApplyNodeEntity node = attendancePatchApplyNodeMapper.findCurrentPendingNodeForApprover(applyId, approverUserId);
        if (node == null) {
            throw new IllegalArgumentException("当前用户不是该申请的有效审批人");
        }
        return node;
    }

    private AttendancePatchApplyNodeEntity findNextWaitingNode(Long applyId, Integer currentNodeOrder) {
        List<AttendancePatchApplyNodeEntity> nodes = attendancePatchApplyNodeMapper.queryByApplyId(applyId);
        return nodes.stream()
                .filter(node -> NODE_STATUS_WAITING.equals(node.getStatus()))
                .filter(node -> currentNodeOrder == null || node.getNodeOrder() > currentNodeOrder)
                .min(Comparator.comparing(AttendancePatchApplyNodeEntity::getNodeOrder))
                .orElse(null);
    }

    private void notifyPendingApproval(AttendancePatchApplyEntity entity, AttendancePatchApplyNodeEntity pendingNode) {
        if (attendancePatchApprovalNoticeService == null || pendingNode == null) {
            return;
        }
        try {
            attendancePatchApprovalNoticeService.notifyPendingApproval(entity, pendingNode);
        } catch (RuntimeException ex) {
            LOGGER.warn("attendance patch approval notice ignored: applyId={}, nodeId={}, approverUserId={}, reason={}",
                    entity == null ? null : entity.getId(),
                    pendingNode.getId(),
                    pendingNode.getApproverUserId(),
                    ex.getMessage(),
                    ex);
        }
    }

    private List<AttendancePatchApplyNodeEntity> buildApprovalNodes(Long applyId, List<UserEntity> approvalChain) {
        List<AttendancePatchApplyNodeEntity> nodes = new ArrayList<>();
        for (int index = 0; index < approvalChain.size(); index += 1) {
            UserEntity approver = approvalChain.get(index);
            AttendancePatchApplyNodeEntity node = new AttendancePatchApplyNodeEntity();
            node.setApplyId(applyId);
            node.setNodeOrder(index + 1);
            node.setNodeCode(index == 0 ? NODE_CODE_DIRECT_LEADER : NODE_CODE_FINAL_LEADER);
            if (approvalChain.size() == 1) {
                node.setNodeName(NODE_NAME_UPPER_LEADER);
            } else {
                node.setNodeName(index == 0 ? NODE_NAME_DIRECT_LEADER : NODE_NAME_FINAL_LEADER);
            }
            node.setApproverUserId(approver.getId());
            node.setApproverName(buildApproverName(approver));
            node.setStatus(index == 0 ? NODE_STATUS_PENDING : NODE_STATUS_WAITING);
            nodes.add(node);
        }
        return nodes;
    }

    private List<UserEntity> resolveApprovalChain(UserEntity applicant) {
        List<UserEntity> chain = new ArrayList<>();
        Set<Long> visitedUserIds = new HashSet<>();
        Long currentParentUserId = applicant == null ? null : applicant.getParentUserId();
        while (currentParentUserId != null
                && visitedUserIds.add(currentParentUserId)
                && chain.size() < MAX_APPROVER_COUNT) {
            UserEntity parentUser = requireUser(currentParentUserId);
            if (!permissionService.isSuperAdmin(parentUser.getId())) {
                chain.add(parentUser);
            }
            currentParentUserId = parentUser.getParentUserId();
        }
        return chain;
    }

    private AttendancePatchApplyLogEntity buildLog(Long applyId,
                                                   String actionType,
                                                   Long operatorUserId,
                                                   String operatorName,
                                                   String nodeCode,
                                                   String nodeName,
                                                   String comment) {
        AttendancePatchApplyLogEntity log = new AttendancePatchApplyLogEntity();
        log.setApplyId(applyId);
        log.setActionType(actionType);
        log.setOperatorUserId(operatorUserId);
        log.setOperatorName(operatorName);
        log.setNodeCode(nodeCode);
        log.setNodeName(nodeName);
        log.setComment(comment);
        return log;
    }

    private void insertLog(AttendancePatchApplyLogEntity log) {
        attendancePatchApplyLogMapper.insert(log);
    }

    private void applyApprovedAttendance(AttendancePatchApplyEntity applyEntity) {
        AttendanceRecordEntity record = attendanceMapper.findByUserIdAndDate(applyEntity.getUserId(), applyEntity.getAttendanceDate());
        validatePatchSubmitAgainstRecord(record, applyEntity.getPatchType(), applyEntity.getPatchTime());

        boolean isInsert = false;
        if (record == null) {
            record = new AttendanceRecordEntity();
            record.setUnitId(applyEntity.getUnitId());
            record.setUserId(applyEntity.getUserId());
            record.setAttendanceDate(applyEntity.getAttendanceDate());
            record.setValidFlag(1);
            isInsert = true;
        }

        applyPatchTimeToRecord(record, applyEntity.getPatchType(), applyEntity.getPatchTime());
        applyPatchAddressToRecord(record, applyEntity.getPatchType(), resolvePatchAddressText(applyEntity.getApplyType()));

        record.setCheckType(resolveLastCheckType(record));
        record.setCheckTime(resolveLastCheckTime(record));
        record.setCheckInResult(resolveRecordStatus(record));
        record.setCheckInFailReason(null);
        record.setLocationSource("PATCH_APPROVAL");
        record.setLocationProvider("BACKOFFICE");
        if (record.getValidFlag() == null) {
            record.setValidFlag(1);
        }

        if (isInsert) {
            attendanceMapper.insert(record);
        } else {
            attendanceMapper.update(record);
        }
    }

    private void validatePatchSubmitAgainstRecord(AttendanceRecordEntity record,
                                                  String patchType,
                                                  LocalDateTime patchTime) {
        if (PATCH_TYPE_AM_ON.equals(patchType)) {
            validateNodePatch(record, patchTime, patchType, null, record == null ? null : record.getAmOffTime(), "上午上班");
            return;
        }
        if (PATCH_TYPE_AM_OFF.equals(patchType)) {
            validateNodePatch(record, patchTime, patchType, record == null ? null : latestBefore(record, PATCH_TYPE_AM_OFF),
                    record == null ? null : record.getPmOnTime(), "上午下班");
            return;
        }
        if (PATCH_TYPE_PM_ON.equals(patchType)) {
            validateNodePatch(record, patchTime, patchType, record == null ? null : latestBefore(record, PATCH_TYPE_PM_ON),
                    record == null ? null : record.getCheckOutTime(), "下午上班");
            return;
        }
        if (PATCH_TYPE_PM_OFF.equals(patchType)) {
            validateNodePatch(record, patchTime, patchType, record == null ? null : latestBefore(record, PATCH_TYPE_PM_OFF),
                    null, "下午下班");
            return;
        }
        throw new IllegalArgumentException("补打卡类型不合法");
    }

    private void validateNodePatch(AttendanceRecordEntity record,
                                   LocalDateTime patchTime,
                                   String patchType,
                                   LocalDateTime previousTime,
                                   LocalDateTime nextTime,
                                   String label) {
        if (record != null && resolveNodeTime(record, patchType) != null) {
            throw new IllegalArgumentException(label + "节点已存在记录，无需重复申请");
        }
        if (previousTime != null && !patchTime.isAfter(previousTime)) {
            throw new IllegalArgumentException(label + "补卡时间必须晚于已存在的前序节点时间");
        }
        if (nextTime != null && !patchTime.isBefore(nextTime)) {
            throw new IllegalArgumentException(label + "补卡时间必须早于已存在的后续节点时间");
        }
    }

    private LocalDateTime latestBefore(AttendanceRecordEntity record, String patchType) {
        if (PATCH_TYPE_AM_OFF.equals(patchType)) {
            return record.getCheckInTime();
        }
        if (PATCH_TYPE_PM_ON.equals(patchType)) {
            if (record.getAmOffTime() != null) {
                return record.getAmOffTime();
            }
            return record.getCheckInTime();
        }
        if (PATCH_TYPE_PM_OFF.equals(patchType)) {
            if (record.getPmOnTime() != null) {
                return record.getPmOnTime();
            }
            if (record.getAmOffTime() != null) {
                return record.getAmOffTime();
            }
            return record.getCheckInTime();
        }
        return null;
    }

    private LocalDateTime resolveNodeTime(AttendanceRecordEntity record, String patchType) {
        if (record == null) {
            return null;
        }
        if (PATCH_TYPE_AM_ON.equals(patchType)) {
            return record.getCheckInTime();
        }
        if (PATCH_TYPE_AM_OFF.equals(patchType)) {
            return record.getAmOffTime();
        }
        if (PATCH_TYPE_PM_ON.equals(patchType)) {
            return record.getPmOnTime();
        }
        if (PATCH_TYPE_PM_OFF.equals(patchType)) {
            return record.getCheckOutTime();
        }
        return null;
    }

    private void applyPatchTimeToRecord(AttendanceRecordEntity record,
                                        String patchType,
                                        LocalDateTime patchTime) {
        if (PATCH_TYPE_AM_ON.equals(patchType)) {
            record.setCheckInTime(patchTime);
            return;
        }
        if (PATCH_TYPE_AM_OFF.equals(patchType)) {
            record.setAmOffTime(patchTime);
            return;
        }
        if (PATCH_TYPE_PM_ON.equals(patchType)) {
            record.setPmOnTime(patchTime);
            return;
        }
        if (PATCH_TYPE_PM_OFF.equals(patchType)) {
            record.setCheckOutTime(patchTime);
            return;
        }
        throw new IllegalArgumentException("补打卡类型不合法");
    }

    private void applyPatchAddressToRecord(AttendanceRecordEntity record, String patchType, String patchAddressText) {
    String addressText = normalizeText(patchAddressText) == null
            ? PATCH_ADDRESS_TEXT_MAKEUP
            : patchAddressText;

    if (PATCH_TYPE_AM_ON.equals(patchType)) {
        if (normalizeText(record.getCheckInAddress()) == null) {
            record.setCheckInAddress(addressText);
        }
        return;
    }
    if (PATCH_TYPE_AM_OFF.equals(patchType)) {
        if (normalizeText(record.getAmOffAddress()) == null) {
            record.setAmOffAddress(addressText);
        }
        return;
    }
    if (PATCH_TYPE_PM_ON.equals(patchType)) {
        if (normalizeText(record.getPmOnAddress()) == null) {
            record.setPmOnAddress(addressText);
        }
        return;
    }
    if (PATCH_TYPE_PM_OFF.equals(patchType) && normalizeText(record.getCheckOutAddress()) == null) {
        record.setCheckOutAddress(addressText);
    }
}

    private String resolveLastCheckType(AttendanceRecordEntity record) {
        if (record.getCheckOutTime() != null) {
            return PATCH_TYPE_PM_OFF;
        }
        if (record.getPmOnTime() != null) {
            return PATCH_TYPE_PM_ON;
        }
        if (record.getAmOffTime() != null) {
            return PATCH_TYPE_AM_OFF;
        }
        if (record.getCheckInTime() != null) {
            return PATCH_TYPE_AM_ON;
        }
        return PATCH_TYPE_AM_ON;
    }

    private LocalDateTime resolveLastCheckTime(AttendanceRecordEntity record) {
        if (record.getCheckOutTime() != null) {
            return record.getCheckOutTime();
        }
        if (record.getPmOnTime() != null) {
            return record.getPmOnTime();
        }
        if (record.getAmOffTime() != null) {
            return record.getAmOffTime();
        }
        return record.getCheckInTime();
    }

    private String resolveRecordStatus(AttendanceRecordEntity record) {
        return record.getCheckOutTime() == null
                ? AttendanceCheckInStatus.CHECK_IN_SUCCESS
                : AttendanceCheckInStatus.CHECK_OUT_SUCCESS;
    }

    private AttendancePatchApplyEntity requirePatchApply(Long id) {
        AttendancePatchApplyEntity entity = attendancePatchApplyMapper.findById(id);
        if (entity == null || entity.getValidFlag() == null || entity.getValidFlag() != 1) {
            throw new IllegalArgumentException("补打卡申请不存在");
        }
        return entity;
    }

    private void validateReadableApply(UserEntity currentUser,
                                       UserEntity targetUser,
                                       AttendanceWeeklyReadonlyScopeService.ReadonlyScope readonlyScope) {
        if (currentUser.getId().equals(targetUser.getId())) {
            return;
        }
        String treePathPrefix = dataScopeService.buildTreePathPrefix(currentUser);
        if (dataScopeService.isReadableTreePath(treePathPrefix, targetUser.getTreePath())) {
            return;
        }
        if (readonlyScope.canViewCrossDeptUser(targetUser.getId())) {
            return;
        }
        throw new IllegalArgumentException("无权查看该补打卡申请");
    }

    private void validateReviewableApply(UserEntity currentUser, AttendancePatchApplyEntity entity) {
        if (!PATCH_STATUS_PENDING.equals(entity.getStatus())) {
            throw new IllegalArgumentException("该补打卡申请已处理，请勿重复审批");
        }
        if (currentUser.getId().equals(entity.getUserId())) {
            throw new IllegalArgumentException("不能审批自己的补打卡申请");
        }
        UserEntity targetUser = requireUser(entity.getUserId());
        AttendanceWeeklyReadonlyScopeService.ReadonlyScope readonlyScope = resolveReadonlyScope(currentUser);
        if (isCrossDeptReadonlyTarget(currentUser, targetUser, readonlyScope)) {
            throw new IllegalArgumentException("跨部门只读监管仅支持查看，不允许审批");
        }
        requireCurrentPendingNodeForApprover(entity.getId(), currentUser.getId());
    }

    private AttendanceWeeklyReadonlyScopeService.ReadonlyScope resolveReadonlyScope(UserEntity currentUser) {
        if (attendanceWeeklyReadonlyScopeService == null) {
            return AttendanceWeeklyReadonlyScopeService.ReadonlyScope.disabled();
        }
        return attendanceWeeklyReadonlyScopeService.resolve(currentUser);
    }

    private boolean isCrossDeptReadonlyTarget(UserEntity currentUser,
                                              UserEntity targetUser,
                                              AttendanceWeeklyReadonlyScopeService.ReadonlyScope readonlyScope) {
        if (targetUser == null || currentUser == null) {
            return false;
        }
        String treePathPrefix = dataScopeService.buildTreePathPrefix(currentUser);
        boolean ownTree = dataScopeService.isReadableTreePath(treePathPrefix, targetUser.getTreePath());
        return !ownTree && readonlyScope.canViewCrossDeptUser(targetUser.getId());
    }

    private UserEntity requireUser(Long userId) {
        UserEntity user = userMapper.findById(userId);
        if (user == null) {
            throw new IllegalArgumentException("申请用户不存在");
        }
        return user;
    }

    private AttendancePatchApplyQueryRequest normalizeQueryRequest(AttendancePatchApplyQueryRequest request) {
        AttendancePatchApplyQueryRequest normalized = request == null ? new AttendancePatchApplyQueryRequest() : request;
        if (normalized.getPageNo() == null || normalized.getPageNo() < 1) {
            normalized.setPageNo(1);
        }
        if (normalized.getPageSize() == null || normalized.getPageSize() < 1) {
            normalized.setPageSize(10);
        }
        if (normalized.getPageSize() > 100) {
            normalized.setPageSize(100);
        }
        validateDateRange(normalized.getDateFrom(), normalized.getDateTo());
        return normalized;
    }

    private void validateDateRange(String dateFrom, String dateTo) {
        LocalDate from = normalizeText(dateFrom) == null ? null : parseDate(dateFrom);
        LocalDate to = normalizeText(dateTo) == null ? null : parseDate(dateTo);
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("开始日期不能晚于结束日期");
        }
    }

    private LocalDate parseDate(String text) {
        return LocalDate.parse(text);
    }

    private LocalDateTime parseDateTime(String text) {
        String normalized = normalizeText(text);
        if (normalized == null) {
            throw new IllegalArgumentException("补卡时间不能为空");
        }
        String value = normalized.replace('T', ' ');
        if (value.length() == 16) {
            value = value + ":00";
        }
        return LocalDateTime.parse(value, DATE_TIME_FORMATTER);
    }

    private String normalizePatchType(String patchType) {
        String normalized = normalizeText(patchType);
        if (normalized == null) {
            throw new IllegalArgumentException("补卡类型不能为空");
        }
        String upper = normalized.toUpperCase();
        if (!PATCH_TYPE_AM_ON.equals(upper)
                && !PATCH_TYPE_AM_OFF.equals(upper)
                && !PATCH_TYPE_PM_ON.equals(upper)
                && !PATCH_TYPE_PM_OFF.equals(upper)) {
            throw new IllegalArgumentException("补卡类型不合法");
        }
        return upper;
    }
private String resolvePatchAddressText(String applyType) {
    return AttendanceApplyType.EVIDENCE.equals(AttendanceApplyType.normalize(applyType))
            ? PATCH_ADDRESS_TEXT_EVIDENCE
            : PATCH_ADDRESS_TEXT_MAKEUP;
}
    private String resolveApplyTypeText(String applyType) {
        return AttendanceApplyType.EVIDENCE.equals(AttendanceApplyType.normalize(applyType))
                ? APPLY_TYPE_TEXT_EVIDENCE
                : APPLY_TYPE_TEXT_MAKEUP;
    }

    private String resolveDisplayName(UserEntity user) {
        if (user == null) {
            return null;
        }
        String realName = normalizeText(user.getRealName());
        if (realName != null) {
            return realName;
        }
        return normalizeText(user.getUsername());
    }

    private String buildApproverName(UserEntity user) {
        String displayName = resolveDisplayName(user);
        String jobTitle = normalizeText(user == null ? null : user.getJobTitle());
        if (displayName == null) {
            return jobTitle;
        }
        if (jobTitle == null) {
            return displayName;
        }
        return displayName + "（" + jobTitle + "）";
    }

    private String resolveApproveDisplayName(AttendancePatchApplyListItemVO item) {
        String realName = normalizeText(item.getApproveRealName());
        if (realName != null) {
            return realName;
        }
        return normalizeText(item.getApproveUsername());
    }

    private <T> T firstOrNull(List<T> list) {
        return list == null || list.isEmpty() ? null : list.get(0);
    }

    private String normalizeText(String text) {
        if (text == null) {
            return null;
        }
        String normalized = text.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
