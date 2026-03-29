package com.example.lecturesystem.modules.weeklywork.service.impl;

import com.example.lecturesystem.modules.auth.security.LoginUser;
import com.example.lecturesystem.modules.operationlog.service.OperationLogService;
import com.example.lecturesystem.modules.permission.support.CurrentUserFacade;
import com.example.lecturesystem.modules.permission.support.DataScopeService;
import com.example.lecturesystem.modules.weeklywork.dto.ReviewWeeklyWorkRequest;
import com.example.lecturesystem.modules.weeklywork.dto.SaveWeeklyWorkRequest;
import com.example.lecturesystem.modules.weeklywork.dto.SubmitWeeklyWorkRequest;
import com.example.lecturesystem.modules.weeklywork.dto.WeeklyWorkQueryRequest;
import com.example.lecturesystem.modules.weeklywork.entity.WeeklyWorkApprovalLogEntity;
import com.example.lecturesystem.modules.weeklywork.entity.WeeklyWorkEntity;
import com.example.lecturesystem.modules.weeklywork.mapper.WeeklyWorkMapper;
import com.example.lecturesystem.modules.weeklywork.service.WeeklyWorkService;
import com.example.lecturesystem.modules.weeklywork.vo.WeeklyWorkFlowNodeVO;
import com.example.lecturesystem.modules.weeklywork.vo.WeeklyWorkDetailVO;
import com.example.lecturesystem.modules.weeklywork.vo.WeeklyWorkListItemVO;
import com.example.lecturesystem.modules.permission.service.PermissionService;
import com.example.lecturesystem.modules.user.entity.UserEntity;
import com.example.lecturesystem.modules.user.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class WeeklyWorkServiceImpl implements WeeklyWorkService {
    private static final Logger log = LoggerFactory.getLogger(WeeklyWorkServiceImpl.class);
    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_SUBMITTED = "SUBMITTED";
    private static final String STATUS_PENDING_SECTION_CHIEF = "PENDING_SECTION_CHIEF";
    private static final String STATUS_PENDING_DEPUTY_LEADER = "PENDING_DEPUTY_LEADER";
    private static final String STATUS_PENDING_LEGION_LEADER = "PENDING_LEGION_LEADER";
    private static final String STATUS_RETURNED = "RETURNED";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String ACTION_APPROVE = "APPROVE";
    private static final String ACTION_RETURN = "RETURN";
    private static final String NODE_STAFF = "STAFF";
    private static final String NODE_SECTION_CHIEF = "SECTION_CHIEF";
    private static final String NODE_DEPUTY_LEADER = "DEPUTY_LEADER";
    private static final String NODE_LEGION_LEADER = "LEGION_LEADER";

    private final WeeklyWorkMapper weeklyWorkMapper;
    private final PermissionService permissionService;
    private final UserMapper userMapper;
    private final OperationLogService operationLogService;
    private final CurrentUserFacade currentUserFacade;
    private final DataScopeService dataScopeService;
    private final DataSource dataSource;

    public WeeklyWorkServiceImpl(WeeklyWorkMapper weeklyWorkMapper,
                                 PermissionService permissionService,
                                 UserMapper userMapper) {
        this(weeklyWorkMapper, permissionService, userMapper, new OperationLogService() {
            @Override
            public void log(String moduleName, String actionName, Long bizId, String content) {
            }

            @Override
            public Object query(com.example.lecturesystem.modules.operationlog.dto.OperationLogQueryRequest request) {
                return java.util.List.of();
            }
        }, null, new DataScopeService(), null);
    }

    @Autowired
    public WeeklyWorkServiceImpl(WeeklyWorkMapper weeklyWorkMapper,
                                 PermissionService permissionService,
                                 UserMapper userMapper,
                                 OperationLogService operationLogService,
                                 CurrentUserFacade currentUserFacade,
                                 DataScopeService dataScopeService,
                                 DataSource dataSource) {
        this.weeklyWorkMapper = weeklyWorkMapper;
        this.permissionService = permissionService;
        this.userMapper = userMapper;
        this.operationLogService = operationLogService;
        this.currentUserFacade = currentUserFacade;
        this.dataScopeService = dataScopeService;
        this.dataSource = dataSource;
    }

    @Override
    @Transactional
    public Long saveDraft(SaveWeeklyWorkRequest request) {
        LoginUser loginUser = currentLoginUser();
        UserEntity currentUser = requireCurrentUser(loginUser.getUserId());
        WeeklyWorkEntity existed = weeklyWorkMapper.findByUserIdAndWeekNo(loginUser.getUserId(), request.getWeekNo());

        if (existed == null) {
            WeeklyWorkEntity entity = new WeeklyWorkEntity();
            entity.setUnitId(currentUser.getUnitId());
            entity.setUserId(loginUser.getUserId());
            entity.setWeekNo(request.getWeekNo());
            entity.setStatus(STATUS_DRAFT);
            entity.setCurrentApprovalNode(NODE_STAFF);
            entity.setWorkPlan(request.getWorkPlan());
            entity.setWorkContent(request.getWorkContent());
            entity.setRemark(request.getRemark());
            weeklyWorkMapper.insert(entity);
            return entity.getId();
        }

        if (!STATUS_DRAFT.equals(existed.getStatus()) && !STATUS_RETURNED.equals(existed.getStatus())) {
            throw new IllegalArgumentException("当前周工作已提交，不能直接覆盖草稿");
        }

        existed.setWorkPlan(request.getWorkPlan());
        existed.setWorkContent(request.getWorkContent());
        existed.setRemark(request.getRemark());
        existed.setStatus(STATUS_DRAFT);
        existed.setCurrentApprovalNode(NODE_STAFF);
        weeklyWorkMapper.updateDraft(existed);
        return existed.getId();
    }

    @Override
    @Transactional
    public Object submit(SubmitWeeklyWorkRequest request) {
        LoginUser loginUser = currentLoginUser();
        WeeklyWorkEntity entity = requireWeeklyWork(request.getId());
        UserEntity currentUser = requireCurrentUser(loginUser.getUserId());

        if (!loginUser.getUserId().equals(entity.getUserId())) {
            throw new IllegalArgumentException("只能提交本人的周工作");
        }
        if (!STATUS_DRAFT.equals(entity.getStatus()) && !STATUS_RETURNED.equals(entity.getStatus())) {
            throw new IllegalArgumentException("当前状态不允许提交");
        }
        if (STATUS_RETURNED.equals(entity.getStatus()) && !NODE_STAFF.equals(resolveCurrentApprovalNode(entity))) {
            throw new IllegalArgumentException("当前退回目标不是科员，暂不支持由本人重新提交");
        }

        List<UserEntity> approvalChain = resolveApprovalChain(currentUser);
        String nextStatus = approvalChain.isEmpty() ? STATUS_APPROVED : resolvePendingStatus(0, approvalChain.size());
        String nextApprovalNode = approvalChain.isEmpty() ? null : toApprovalNodeCode(approvalChain.get(0));
        ApprovalSnapshot submitSnapshot = buildSubmitSnapshot(currentUser, approvalChain);
        logSubmitPayload(entity, nextStatus, nextApprovalNode);
        int updated = weeklyWorkMapper.markSubmitted(
                entity.getId(),
                nextStatus,
                nextApprovalNode,
                submitSnapshot.currentHandlerUserId(),
                submitSnapshot.currentHandlerUserName(),
                submitSnapshot.currentFlowOrder(),
                submitSnapshot.finalApproverUserId(),
                submitSnapshot.approvedTime(),
                LocalDateTime.now()
        );
        requireSingleRowUpdate(updated, "提交周报");
        return "ok";
    }

    @Override
    public Object query(WeeklyWorkQueryRequest request) {
        WeeklyWorkQueryRequest normalizedRequest = request == null ? new WeeklyWorkQueryRequest() : request;
        LoginUser loginUser = currentLoginUser();
        if (!permissionService.isSuperAdmin(loginUser.getUserId())) {
            dataScopeService.injectTreePathScope(normalizedRequest, requireCurrentUser(loginUser.getUserId()));
        }

        if (!permissionService.isSuperAdmin(loginUser.getUserId()) && normalizedRequest.getUserId() != null) {
            UserEntity targetUser = requireCurrentUser(normalizedRequest.getUserId());
            dataScopeService.validateReadableUser(requireCurrentUser(loginUser.getUserId()), targetUser, "无权查看指定用户的周工作");
        }

        List<WeeklyWorkListItemVO> records = weeklyWorkMapper.queryList(normalizedRequest);
        for (WeeklyWorkListItemVO item : records) {
            enrichListItem(item, loginUser.getUserId());
        }
        return records;
    }

    @Override
    public Object detail(Long id) {
        LoginUser loginUser = currentLoginUser();
        WeeklyWorkEntity entity = requireWeeklyWork(id);
        validateReadable(loginUser, entity);
        List<UserEntity> approvalChain = resolveApprovalChain(requireCurrentUser(entity.getUserId()));
        WeeklyWorkDetailVO detail = toDetailVO(entity);
        detail.setCurrentApprovalNode(resolveCurrentApprovalNode(entity, approvalChain));
        detail.setFlowNodes(buildFlowNodes(requireCurrentUser(entity.getUserId()), approvalChain));
        detail.setAvailableReturnTargets(resolveReturnTargets(entity, approvalChain));
        detail.setApprovalLogs(weeklyWorkMapper.queryApprovalLogs(entity.getId()));
        return detail;
    }

    @Override
    @Transactional
    public Object review(ReviewWeeklyWorkRequest request) {
        LoginUser loginUser = currentLoginUser();
        WeeklyWorkEntity entity = requireWeeklyWork(request.getId());
        validateReadable(loginUser, entity);

        if (loginUser.getUserId().equals(entity.getUserId())) {
            throw new IllegalArgumentException("不能审核自己的周报");
        }
        if (!isReviewableStatus(entity)) {
            throw new IllegalArgumentException("当前状态不允许审核");
        }

        String action = request.getAction() == null ? "" : request.getAction().trim().toUpperCase();
        List<UserEntity> approvalChain = resolveApprovalChain(requireCurrentUser(entity.getUserId()));
        String currentNode = resolveCurrentApprovalNode(entity, approvalChain);
        if (!ACTION_APPROVE.equals(action) && !ACTION_RETURN.equals(action)) {
            throw new IllegalArgumentException("不支持的审核动作");
        }
        if (!matchesApprovalNode(loginUser.getUserId(), currentNode)) {
            throw new IllegalArgumentException("当前用户不是该周报的审批人");
        }
        WeeklyWorkEntity updateEntity = new WeeklyWorkEntity();
        updateEntity.setId(entity.getId());
        updateEntity.setLastReviewBy(loginUser.getUserId());
        updateEntity.setLastReviewByName(resolveDisplayName(loginUser));
        updateEntity.setLastReviewTime(LocalDateTime.now());
        if (ACTION_APPROVE.equals(action)) {
            applyApprove(updateEntity, entity, approvalChain, currentNode, requireCurrentUser(entity.getUserId()));
        } else {
            applyReturn(updateEntity, entity, approvalChain, request, requireCurrentUser(entity.getUserId()));
        }

        int updated = weeklyWorkMapper.updateApproval(updateEntity);
        requireSingleRowUpdate(updated, "更新审批状态");
        WeeklyWorkApprovalLogEntity approvalLog = buildApprovalLog(entity.getId(), loginUser, currentNode, updateEntity, request);
        logApprovalLogPayload(approvalLog, updateEntity.getStatus());
        int inserted = weeklyWorkMapper.insertApprovalLog(approvalLog);
        requireSingleRowUpdate(inserted, "写入审批日志");
        operationLogService.log(
                "WEEKLY_WORK",
                "REVIEW",
                entity.getId(),
                "审核周报：" + entity.getWeekNo() + "，用户ID=" + entity.getUserId() + "，结果=" + updateEntity.getStatus()
        );
        return "ok";
    }

    private void requireSingleRowUpdate(int affectedRows, String actionName) {
        if (affectedRows != 1) {
            throw new IllegalStateException(actionName + "失败，请稍后重试");
        }
    }

    private void applyApprove(WeeklyWorkEntity updateEntity,
                              WeeklyWorkEntity currentEntity,
                              List<UserEntity> approvalChain,
                              String currentNode,
                              UserEntity reporter) {
        int currentIndex = findApprovalNodeIndex(approvalChain, currentNode);
        if (currentIndex < 0) {
            throw new IllegalArgumentException("当前审批节点不支持通过");
        }
        updateEntity.setFinalApproverUserId(resolveFinalApproverUserId(approvalChain));
        if (currentIndex >= approvalChain.size() - 1) {
            updateEntity.setStatus(STATUS_APPROVED);
            updateEntity.setCurrentApprovalNode(null);
            updateEntity.setCurrentHandlerUserId(null);
            updateEntity.setCurrentHandlerUserName(null);
            updateEntity.setCurrentFlowOrder(resolveFlowOrderForNodeCode(STATUS_APPROVED, reporter, approvalChain));
            updateEntity.setApprovedTime(LocalDateTime.now());
        } else {
            int nextIndex = currentIndex + 1;
            UserEntity nextApprover = approvalChain.get(nextIndex);
            updateEntity.setStatus(resolvePendingStatus(nextIndex, approvalChain.size()));
            updateEntity.setCurrentApprovalNode(toApprovalNodeCode(nextApprover));
            updateEntity.setCurrentHandlerUserId(nextApprover.getId());
            updateEntity.setCurrentHandlerUserName(buildFlowNodeLabel(nextApprover));
            updateEntity.setCurrentFlowOrder(resolveFlowOrderForNodeCode(toApprovalNodeCode(nextApprover), reporter, approvalChain));
            updateEntity.setApprovedTime(null);
        }
        updateEntity.setLastReturnTarget(currentEntity.getLastReturnTarget());
        updateEntity.setLastReturnComment(currentEntity.getLastReturnComment());
    }

    private void applyReturn(WeeklyWorkEntity updateEntity,
                             WeeklyWorkEntity currentEntity,
                             List<UserEntity> approvalChain,
                             ReviewWeeklyWorkRequest request,
                             UserEntity reporter) {
        String comment = normalizeText(request.getComment());
        if (comment == null) {
            throw new IllegalArgumentException("退回时必须填写退回意见");
        }
        String returnTarget = normalizeCode(request.getReturnTarget());
        if (returnTarget == null || !resolveReturnTargets(currentEntity, approvalChain).contains(returnTarget)) {
            throw new IllegalArgumentException("当前审批节点不支持退回到该目标");
        }
        updateEntity.setStatus(STATUS_RETURNED);
        updateEntity.setCurrentApprovalNode(returnTarget);
        updateEntity.setCurrentHandlerUserId(resolveHandlerUserId(returnTarget, reporter, approvalChain));
        updateEntity.setCurrentHandlerUserName(resolveHandlerUserName(returnTarget, reporter, approvalChain));
        updateEntity.setCurrentFlowOrder(resolveFlowOrderForNodeCode(returnTarget, reporter, approvalChain));
        updateEntity.setFinalApproverUserId(resolveFinalApproverUserId(approvalChain));
        updateEntity.setLastReturnTarget(returnTarget);
        updateEntity.setLastReturnComment(comment);
        updateEntity.setApprovedTime(null);
    }

    private WeeklyWorkApprovalLogEntity buildApprovalLog(Long weeklyWorkId,
                                                         LoginUser loginUser,
                                                         String currentNode,
                                                         WeeklyWorkEntity updateEntity,
                                                         ReviewWeeklyWorkRequest request) {
        WeeklyWorkApprovalLogEntity logEntity = new WeeklyWorkApprovalLogEntity();
        logEntity.setWeeklyWorkId(weeklyWorkId);
        logEntity.setAction(request.getAction().trim().toUpperCase());
        logEntity.setFromNode(currentNode);
        logEntity.setToNode(updateEntity.getCurrentApprovalNode() == null ? updateEntity.getStatus() : updateEntity.getCurrentApprovalNode());
        logEntity.setReviewerUserId(loginUser.getUserId());
        logEntity.setReviewerName(resolveDisplayName(loginUser));
        logEntity.setComment(normalizeText(request.getComment()));
        logEntity.setCreateTime(LocalDateTime.now());
        return logEntity;
    }

    private WeeklyWorkDetailVO toDetailVO(WeeklyWorkEntity entity) {
        WeeklyWorkDetailVO detail = new WeeklyWorkDetailVO();
        detail.setId(entity.getId());
        detail.setUnitId(entity.getUnitId());
        detail.setUserId(entity.getUserId());
        detail.setWeekNo(entity.getWeekNo());
        detail.setStatus(entity.getStatus());
        detail.setWorkPlan(entity.getWorkPlan());
        detail.setWorkContent(entity.getWorkContent());
        detail.setRemark(entity.getRemark());
        detail.setCurrentApprovalNode(entity.getCurrentApprovalNode());
        detail.setCurrentHandlerUserId(entity.getCurrentHandlerUserId());
        detail.setCurrentHandlerUserName(entity.getCurrentHandlerUserName());
        detail.setCurrentFlowOrder(entity.getCurrentFlowOrder());
        detail.setFinalApproverUserId(entity.getFinalApproverUserId());
        detail.setLastReturnTarget(entity.getLastReturnTarget());
        detail.setLastReturnComment(entity.getLastReturnComment());
        detail.setLastReviewBy(entity.getLastReviewBy());
        detail.setLastReviewByName(entity.getLastReviewByName());
        detail.setLastReviewTime(entity.getLastReviewTime());
        detail.setSubmitTime(entity.getSubmitTime());
        detail.setApprovedTime(entity.getApprovedTime());
        detail.setCreateTime(entity.getCreateTime());
        return detail;
    }

    private void enrichListItem(WeeklyWorkListItemVO item, Long currentUserId) {
        UserEntity reporter = requireCurrentUser(item.getUserId());
        List<UserEntity> approvalChain = resolveApprovalChain(reporter);
        item.setCurrentApprovalNode(resolveCurrentApprovalNode(item.getStatus(), item.getCurrentApprovalNode(), item.getLastReturnTarget(), approvalChain));
        item.setFlowNodes(buildFlowNodes(reporter, approvalChain));
        item.setReviewedByCurrentUser(weeklyWorkMapper.queryApprovalLogs(item.getId()).stream()
                .anyMatch(log -> currentUserId != null && currentUserId.equals(log.getReviewerUserId())));
    }

    private ApprovalSnapshot buildSubmitSnapshot(UserEntity reporter, List<UserEntity> approvalChain) {
        if (approvalChain.isEmpty()) {
            return new ApprovalSnapshot(null, null, null, null, LocalDateTime.now());
        }
        UserEntity currentHandler = approvalChain.get(0);
        return new ApprovalSnapshot(
                currentHandler.getId(),
                buildFlowNodeLabel(currentHandler),
                resolveFlowOrderForNodeCode(toApprovalNodeCode(currentHandler), reporter, approvalChain),
                resolveFinalApproverUserId(approvalChain),
                null
        );
    }

    private Long resolveFinalApproverUserId(List<UserEntity> approvalChain) {
        return approvalChain.isEmpty() ? null : approvalChain.get(approvalChain.size() - 1).getId();
    }

    private Long resolveHandlerUserId(String nodeCode, UserEntity reporter, List<UserEntity> approvalChain) {
        if (NODE_STAFF.equals(nodeCode)) {
            return reporter == null ? null : reporter.getId();
        }
        int approvalIndex = findApprovalNodeIndex(approvalChain, nodeCode);
        return approvalIndex < 0 ? null : approvalChain.get(approvalIndex).getId();
    }

    private String resolveHandlerUserName(String nodeCode, UserEntity reporter, List<UserEntity> approvalChain) {
        if (NODE_STAFF.equals(nodeCode)) {
            return reporter == null ? null : buildFlowNodeLabel(reporter);
        }
        int approvalIndex = findApprovalNodeIndex(approvalChain, nodeCode);
        return approvalIndex < 0 ? null : buildFlowNodeLabel(approvalChain.get(approvalIndex));
    }

    private Integer resolveFlowOrderForNodeCode(String nodeCode, UserEntity reporter, List<UserEntity> approvalChain) {
        if (nodeCode == null || STATUS_APPROVED.equals(nodeCode)) {
            return approvalChain.isEmpty() ? null : approvalChain.size() + 1;
        }
        if (NODE_STAFF.equals(nodeCode)) {
            return reporter == null ? null : 1;
        }
        int approvalIndex = findApprovalNodeIndex(approvalChain, nodeCode);
        return approvalIndex < 0 ? null : approvalIndex + 2;
    }

    private void logSubmitPayload(WeeklyWorkEntity entity, String status, String currentApprovalNode) {
        SubmitDbContext context = resolveSubmitDbContext();
        log.info(
                "weekly submit payload -> weeklyWorkId={}, weekNo='{}', weekNoLength={}, status='{}', statusLength={}, currentApprovalNode='{}', currentApprovalNodeLength={}, dbUrl='{}', dbName='{}', schema='{}'",
                entity.getId(),
                entity.getWeekNo(),
                lengthOf(entity.getWeekNo()),
                status,
                lengthOf(status),
                currentApprovalNode,
                lengthOf(currentApprovalNode),
                context.url,
                context.databaseName,
                context.schema
        );
    }

    private void logApprovalLogPayload(WeeklyWorkApprovalLogEntity approvalLog, String targetStatus) {
        log.info(
                "weekly approval log payload -> weeklyWorkId={}, action='{}', actionLength={}, fromNode='{}', fromNodeLength={}, toNode='{}', toNodeLength={}, targetStatus='{}', targetStatusLength={}, commentLength={}",
                approvalLog.getWeeklyWorkId(),
                approvalLog.getAction(),
                lengthOf(approvalLog.getAction()),
                approvalLog.getFromNode(),
                lengthOf(approvalLog.getFromNode()),
                approvalLog.getToNode(),
                lengthOf(approvalLog.getToNode()),
                targetStatus,
                lengthOf(targetStatus),
                lengthOf(approvalLog.getComment())
        );
    }

    private SubmitDbContext resolveSubmitDbContext() {
        if (dataSource == null) {
            return SubmitDbContext.empty();
        }
        try (Connection connection = dataSource.getConnection()) {
            String databaseName = connection.getCatalog();
            String schema = connection.getSchema();
            if (databaseName != null && schema != null) {
                return new SubmitDbContext(connection.getMetaData().getURL(), databaseName, schema);
            }
            try (PreparedStatement statement = connection.prepareStatement("SELECT current_database(), current_schema()");
                 ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return new SubmitDbContext(
                            connection.getMetaData().getURL(),
                            resultSet.getString(1),
                            resultSet.getString(2)
                    );
                }
            }
            return new SubmitDbContext(connection.getMetaData().getURL(), databaseName, schema);
        } catch (SQLException exception) {
            log.warn("failed to resolve weekly submit database context", exception);
            return SubmitDbContext.empty();
        }
    }

    private int lengthOf(String value) {
        return value == null ? 0 : value.length();
    }

    private static final class SubmitDbContext {
        private final String url;
        private final String databaseName;
        private final String schema;

        private SubmitDbContext(String url, String databaseName, String schema) {
            this.url = url;
            this.databaseName = databaseName;
            this.schema = schema;
        }

        private static SubmitDbContext empty() {
            return new SubmitDbContext("unavailable", "unavailable", "unavailable");
        }
    }

    private record ApprovalSnapshot(Long currentHandlerUserId,
                                    String currentHandlerUserName,
                                    Integer currentFlowOrder,
                                    Long finalApproverUserId,
                                    LocalDateTime approvedTime) {
    }

    private boolean isReviewableStatus(WeeklyWorkEntity entity) {
        if (List.of(
                STATUS_SUBMITTED,
                STATUS_PENDING_SECTION_CHIEF,
                STATUS_PENDING_DEPUTY_LEADER,
                STATUS_PENDING_LEGION_LEADER
        ).contains(entity.getStatus())) {
            return true;
        }
        return STATUS_RETURNED.equals(entity.getStatus()) && !NODE_STAFF.equals(resolveCurrentApprovalNode(entity));
    }

    private String resolveCurrentApprovalNode(WeeklyWorkEntity entity) {
        return resolveCurrentApprovalNode(entity, resolveApprovalChain(requireCurrentUser(entity.getUserId())));
    }

    private String resolveCurrentApprovalNode(WeeklyWorkEntity entity, List<UserEntity> approvalChain) {
        return resolveCurrentApprovalNode(entity.getStatus(), entity.getCurrentApprovalNode(), entity.getLastReturnTarget(), approvalChain);
    }

    private String resolveCurrentApprovalNode(String status,
                                              String currentApprovalNode,
                                              String lastReturnTarget,
                                              List<UserEntity> approvalChain) {
        String currentNode = normalizeCode(currentApprovalNode);
        if (currentNode != null) {
            return currentNode;
        }
        if (approvalChain.isEmpty()) {
            return switch (status) {
                case STATUS_DRAFT -> NODE_STAFF;
                case STATUS_RETURNED -> normalizeCode(lastReturnTarget) == null ? NODE_STAFF : normalizeCode(lastReturnTarget);
                case STATUS_APPROVED -> STATUS_APPROVED;
                default -> null;
            };
        }
        return switch (status) {
            case STATUS_SUBMITTED, STATUS_PENDING_SECTION_CHIEF -> toApprovalNodeCode(approvalChain.get(0));
            case STATUS_PENDING_DEPUTY_LEADER -> toApprovalNodeCode(approvalChain.get(Math.min(1, approvalChain.size() - 1)));
            case STATUS_PENDING_LEGION_LEADER -> toApprovalNodeCode(approvalChain.get(approvalChain.size() - 1));
            case STATUS_DRAFT -> NODE_STAFF;
            case STATUS_RETURNED -> normalizeCode(lastReturnTarget) == null ? NODE_STAFF : normalizeCode(lastReturnTarget);
            case STATUS_APPROVED -> STATUS_APPROVED;
            default -> null;
        };
    }

    private List<String> resolveReturnTargets(WeeklyWorkEntity entity, List<UserEntity> approvalChain) {
        String currentNode = resolveCurrentApprovalNode(entity, approvalChain);
        int currentIndex = findApprovalNodeIndex(approvalChain, currentNode);
        if (currentIndex < 0) {
            return List.of();
        }
        List<String> returnTargets = new ArrayList<>();
        for (int index = currentIndex - 1; index >= 0; index -= 1) {
            returnTargets.add(toApprovalNodeCode(approvalChain.get(index)));
        }
        returnTargets.add(NODE_STAFF);
        return returnTargets;
    }

    private List<WeeklyWorkFlowNodeVO> buildFlowNodes(UserEntity reporter, List<UserEntity> approvalChain) {
        List<WeeklyWorkFlowNodeVO> flowNodes = new ArrayList<>();
        flowNodes.add(buildFlowNode(reporter, 1, reporter.getId() == null ? NODE_STAFF : "USER_" + reporter.getId(), NODE_STAFF));
        for (int index = 0; index < approvalChain.size(); index += 1) {
            UserEntity approver = approvalChain.get(index);
            flowNodes.add(buildFlowNode(
                    approver,
                    index + 2,
                    toApprovalNodeCode(approver),
                    resolveLegacyApprovalAlias(index, approvalChain.size())
            ));
        }
        return flowNodes;
    }

    private WeeklyWorkFlowNodeVO buildFlowNode(UserEntity user, int order, String key, String legacyAlias) {
        WeeklyWorkFlowNodeVO flowNode = new WeeklyWorkFlowNodeVO();
        flowNode.setKey(key);
        flowNode.setLabel(buildFlowNodeLabel(user));
        flowNode.setUserId(user.getId());
        flowNode.setUsername(user.getUsername());
        flowNode.setRealName(user.getRealName());
        flowNode.setJobTitle(user.getJobTitle());
        flowNode.setOrder(order);
        flowNode.setRoleCode(legacyAlias);
        List<String> aliases = new ArrayList<>();
        aliases.add(key);
        if (legacyAlias != null && !legacyAlias.isBlank() && !legacyAlias.equals(key)) {
            aliases.add(legacyAlias);
        }
        flowNode.setAliases(aliases);
        return flowNode;
    }

    private String buildFlowNodeLabel(UserEntity user) {
        String displayName = user.getRealName() == null || user.getRealName().isBlank() ? user.getUsername() : user.getRealName();
        if (user.getJobTitle() != null && !user.getJobTitle().isBlank()) {
            return displayName + "（" + user.getJobTitle() + "）";
        }
        return displayName;
    }

    private String resolveLegacyApprovalAlias(int approverIndex, int approverCount) {
        if (approverIndex == 0) {
            return NODE_SECTION_CHIEF;
        }
        if (approverIndex >= approverCount - 1) {
            return approverCount == 1 ? NODE_SECTION_CHIEF : NODE_LEGION_LEADER;
        }
        return NODE_DEPUTY_LEADER;
    }

    private String resolveDisplayName(LoginUser loginUser) {
        return loginUser.getRealName() == null || loginUser.getRealName().isBlank()
                ? loginUser.getUsername()
                : loginUser.getRealName();
    }

    private String normalizeCode(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized.toUpperCase();
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private List<UserEntity> resolveApprovalChain(UserEntity reporter) {
        List<UserEntity> chain = new ArrayList<>();
        Set<Long> visitedUserIds = new HashSet<>();
        Long currentParentUserId = reporter == null ? null : reporter.getParentUserId();
        while (currentParentUserId != null && visitedUserIds.add(currentParentUserId)) {
            UserEntity parentUser = requireCurrentUser(currentParentUserId);
            if (!permissionService.isSuperAdmin(parentUser.getId())) {
                chain.add(parentUser);
            }
            currentParentUserId = parentUser.getParentUserId();
        }
        return chain;
    }

    private String toApprovalNodeCode(UserEntity user) {
        return user == null || user.getId() == null ? null : "USER_" + user.getId();
    }

    private int findApprovalNodeIndex(List<UserEntity> approvalChain, String currentNode) {
        if (currentNode == null) {
            return -1;
        }
        for (int index = 0; index < approvalChain.size(); index += 1) {
            if (currentNode.equals(toApprovalNodeCode(approvalChain.get(index)))) {
                return index;
            }
        }
        if (NODE_SECTION_CHIEF.equals(currentNode) && !approvalChain.isEmpty()) {
            return 0;
        }
        if (NODE_DEPUTY_LEADER.equals(currentNode) && approvalChain.size() >= 2) {
            return Math.min(1, approvalChain.size() - 1);
        }
        if (NODE_LEGION_LEADER.equals(currentNode) && !approvalChain.isEmpty()) {
            return approvalChain.size() - 1;
        }
        return -1;
    }

    private String resolvePendingStatus(int approverIndex, int approverCount) {
        if (approverCount <= 0) {
            return STATUS_APPROVED;
        }
        if (approverIndex <= 0) {
            return STATUS_PENDING_SECTION_CHIEF;
        }
        if (approverIndex >= approverCount - 1) {
            return approverCount == 1 ? STATUS_PENDING_SECTION_CHIEF : STATUS_PENDING_LEGION_LEADER;
        }
        return STATUS_PENDING_DEPUTY_LEADER;
    }

    private boolean matchesApprovalNode(Long userId, String nodeCode) {
        return nodeCode != null && nodeCode.equals("USER_" + userId);
    }

    private WeeklyWorkEntity requireWeeklyWork(Long id) {
        WeeklyWorkEntity entity = weeklyWorkMapper.findById(id);
        if (entity == null) {
            throw new IllegalArgumentException("周工作不存在");
        }
        return entity;
    }

    private UserEntity requireCurrentUser(Long userId) {
        UserEntity user = userMapper.findById(userId);
        if (user == null) {
            throw new IllegalArgumentException("当前用户不存在");
        }
        return user;
    }

    private void validateReadable(LoginUser loginUser, WeeklyWorkEntity entity) {
        if (permissionService.isSuperAdmin(loginUser.getUserId())) {
            return;
        }
        UserEntity currentUser = requireCurrentUser(loginUser.getUserId());
        UserEntity targetUser = requireCurrentUser(entity.getUserId());
        dataScopeService.validateReadableUser(currentUser, targetUser, "无权查看该周报");
    }

    private LoginUser currentLoginUser() {
        if (currentUserFacade != null) {
            return currentUserFacade.currentLoginUser();
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof LoginUser loginUser)) {
            throw new IllegalArgumentException("当前未登录");
        }
        return loginUser;
    }

}
