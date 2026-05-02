package com.example.lecturesystem.modules.agent.support.policy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.lecturesystem.modules.agent.dto.AiPolicyCorrectionTaskQueryRequest;
import com.example.lecturesystem.modules.agent.dto.PolicyDraftRequest;
import com.example.lecturesystem.modules.agent.entity.AgentMessageEntity;
import com.example.lecturesystem.modules.agent.entity.AiPolicyAnswerLogEntity;
import com.example.lecturesystem.modules.agent.entity.AiPolicyChunkEntity;
import com.example.lecturesystem.modules.agent.entity.AiPolicyFeedbackActionEntity;
import com.example.lecturesystem.modules.agent.entity.AiPolicyFeedbackTaskEntity;
import com.example.lecturesystem.modules.agent.mapper.AgentMessageMapper;
import com.example.lecturesystem.modules.agent.mapper.AiPolicyAnswerLogMapper;
import com.example.lecturesystem.modules.agent.mapper.AiPolicyChunkMapper;
import com.example.lecturesystem.modules.agent.mapper.AiPolicyFeedbackActionMapper;
import com.example.lecturesystem.modules.agent.mapper.AiPolicyFeedbackTaskMapper;
import com.example.lecturesystem.modules.agent.vo.AiPolicyCorrectionTraceVO;
import com.example.lecturesystem.modules.agent.vo.AiPolicyEvidenceItemVO;
import com.example.lecturesystem.modules.agent.vo.AiPolicyMessageContextVO;
import com.example.lecturesystem.modules.auth.security.LoginUser;
import com.example.lecturesystem.modules.knowledge.mapper.KnowledgeChunkMapper;
import com.example.lecturesystem.modules.knowledge.vo.KnowledgeChunkListItemVO;
import com.example.lecturesystem.modules.permission.support.CurrentUserFacade;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AiPolicyCorrectionService {
    private static final Set<String> VALID_STATUSES = Set.of(
            "PENDING",
            "PROCESSING",
            "NEED_KNOWLEDGE_FIX",
            "NEED_CODE_FIX",
            "NO_ACTION",
            "RESOLVED",
            "CLOSED"
    );

    private final AiPolicyFeedbackTaskMapper taskMapper;
    private final AiPolicyFeedbackActionMapper actionMapper;
    private final AiPolicyAnswerLogMapper answerLogMapper;
    private final AiPolicyChunkMapper policyChunkMapper;
    private final KnowledgeChunkMapper knowledgeChunkMapper;
    private final AgentMessageMapper agentMessageMapper;
    private final CurrentUserFacade currentUserFacade;
    private final ObjectMapper objectMapper;

    public AiPolicyCorrectionService(AiPolicyFeedbackTaskMapper taskMapper,
                                     AiPolicyFeedbackActionMapper actionMapper,
                                     AiPolicyAnswerLogMapper answerLogMapper,
                                     AiPolicyChunkMapper policyChunkMapper,
                                     KnowledgeChunkMapper knowledgeChunkMapper,
                                     AgentMessageMapper agentMessageMapper,
                                     CurrentUserFacade currentUserFacade,
                                     ObjectMapper objectMapper) {
        this.taskMapper = taskMapper;
        this.actionMapper = actionMapper;
        this.answerLogMapper = answerLogMapper;
        this.policyChunkMapper = policyChunkMapper;
        this.knowledgeChunkMapper = knowledgeChunkMapper;
        this.agentMessageMapper = agentMessageMapper;
        this.currentUserFacade = currentUserFacade;
        this.objectMapper = objectMapper;
    }

    public PageResult<AiPolicyFeedbackTaskEntity> queryTasks(AiPolicyCorrectionTaskQueryRequest request) {
        AiPolicyCorrectionTaskQueryRequest safeRequest = request == null ? new AiPolicyCorrectionTaskQueryRequest() : request;
        int pageNo = normalizePageNo(safeRequest.getPageNo());
        int pageSize = normalizePageSize(safeRequest.getPageSize());
        int total = taskMapper.countPage(safeRequest);
        List<AiPolicyFeedbackTaskEntity> list = taskMapper.queryPage(safeRequest, pageSize, (pageNo - 1) * pageSize);
        return new PageResult<>(list, total, pageNo, pageSize);
    }

    public TaskDetail getDetail(Long id) {
        AiPolicyFeedbackTaskEntity task = requireTask(id);
        return new TaskDetail(task, actionMapper.queryByTaskId(id));
    }

    public AiPolicyCorrectionTraceVO getTrace(Long id) {
        AiPolicyFeedbackTaskEntity task = requireTask(id);
        AiPolicyAnswerLogEntity answerLog = resolveAnswerLog(task);
        List<AiPolicyMessageContextVO> messages = queryMessageContext(task.getSessionId());
        return new AiPolicyCorrectionTraceVO(
                task,
                answerLog,
                messages,
                firstNonBlank(answerLog == null ? null : answerLog.getAnswerMode(), null),
                firstNonBlank(answerLog == null ? null : answerLog.getRoutePlan(), task.getRoutePlan()),
                answerLog == null ? null : answerLog.getFaqHit(),
                answerLog == null ? null : answerLog.getFallbackFlag(),
                answerLog == null ? null : answerLog.getMatchedIntentCode(),
                answerLog == null ? null : answerLog.getCandidateGenerated(),
                firstNonBlank(answerLog == null ? null : answerLog.getValidationSummary(), task.getValidationSummary()),
                firstNonBlank(answerLog == null ? null : answerLog.getHitChunkIds(), task.getEvidenceIds()),
                firstNonBlank(answerLog == null ? null : answerLog.getFinalAnswer(), task.getAnswer())
        );
    }

    public List<AiPolicyEvidenceItemVO> getEvidence(Long id) {
        AiPolicyFeedbackTaskEntity task = requireTask(id);
        AiPolicyAnswerLogEntity answerLog = resolveAnswerLog(task);
        List<Long> chunkIds = parseChunkIds(firstNonBlank(
                answerLog == null ? null : answerLog.getHitChunkIds(),
                task.getEvidenceIds()
        ));
        List<AiPolicyEvidenceItemVO> evidence = new ArrayList<>();
        if (!chunkIds.isEmpty()) {
            Map<Long, AiPolicyChunkEntity> policyChunks = policyChunkMapper.findByIds(chunkIds).stream()
                    .collect(Collectors.toMap(AiPolicyChunkEntity::getId, Function.identity(), (a, b) -> a));
            Map<Long, KnowledgeChunkListItemVO> knowledgeChunks = knowledgeChunkMapper.findByIds(chunkIds).stream()
                    .collect(Collectors.toMap(KnowledgeChunkListItemVO::getId, Function.identity(), (a, b) -> a));
            for (Long chunkId : chunkIds) {
                AiPolicyChunkEntity policyChunk = policyChunks.get(chunkId);
                if (policyChunk != null) {
                    evidence.add(toPolicyChunkEvidence(policyChunk));
                }
                KnowledgeChunkListItemVO knowledgeChunk = knowledgeChunks.get(chunkId);
                if (knowledgeChunk != null) {
                    evidence.add(toKnowledgeChunkEvidence(knowledgeChunk));
                }
            }
        }
        AgentMessageEntity message = task.getMessageId() == null ? null : agentMessageMapper.findById(task.getMessageId());
        if (message != null) {
            evidence.add(toMessageEvidence(message));
        }
        return evidence;
    }

    @Transactional
    public AiPolicyFeedbackTaskEntity updateStatus(Long id, String status, String handleResult) {
        String normalizedStatus = normalizeStatus(status);
        Long operatorId = currentOperatorId();
        boolean markHandled = isTerminalStatus(normalizedStatus);
        int updated = taskMapper.updateStatus(id, normalizedStatus, operatorId, safeText(handleResult), markHandled);
        if (updated == 0) {
            throw new IllegalArgumentException("未找到纠错任务");
        }
        writeAction(id, "STATUS_CHANGE", "状态更新为 " + normalizedStatus + appendHandleResult(handleResult), operatorId);
        return requireTask(id);
    }

    @Transactional
    public AiPolicyFeedbackActionEntity comment(Long id, String actionContent) {
        requireTask(id);
        return writeAction(id, "COMMENT", safeText(actionContent), currentOperatorId());
    }

    @Transactional
    public AiPolicyFeedbackActionEntity saveFaqDraft(Long id, PolicyDraftRequest request) {
        requireTask(id);
        String payload = validateDraftPayload(request);
        return writeDraftAction(
                id,
                "FAQ_DRAFT",
                "ai_policy_faq",
                firstNonBlank(request == null ? null : request.getActionContent(), "生成知识补强草稿，仅供人工审核，不写入正式知识库"),
                payload,
                currentOperatorId()
        );
    }

    @Transactional
    public AiPolicyFeedbackActionEntity savePolicyChunkDraft(Long id, PolicyDraftRequest request) {
        requireTask(id);
        String payload = validateDraftPayload(request);
        return writeDraftAction(
                id,
                "POLICY_CHUNK_DRAFT",
                "ai_policy_chunk",
                firstNonBlank(request == null ? null : request.getActionContent(), "生成知识补强草稿，仅供人工审核，不写入正式知识库"),
                payload,
                currentOperatorId()
        );
    }

    @Transactional
    public AiPolicyFeedbackTaskEntity close(Long id, String handleResult) {
        return updateStatus(id, "CLOSED", firstNonBlank(handleResult, "人工关闭反馈任务"));
    }

    private AiPolicyFeedbackTaskEntity requireTask(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("任务 ID 不能为空");
        }
        AiPolicyFeedbackTaskEntity task = taskMapper.findById(id);
        if (task == null) {
            throw new IllegalArgumentException("未找到纠错任务");
        }
        return task;
    }

    private AiPolicyAnswerLogEntity resolveAnswerLog(AiPolicyFeedbackTaskEntity task) {
        if (task == null) {
            return null;
        }
        if (task.getAnswerLogId() != null) {
            AiPolicyAnswerLogEntity answerLog = answerLogMapper.findById(task.getAnswerLogId());
            if (answerLog != null) {
                return answerLog;
            }
        }
        if (task.getSessionId() != null) {
            AiPolicyAnswerLogEntity exact = answerLogMapper.findLatestForFeedback(task.getSessionId(), task.getQuestion());
            if (exact != null) {
                return exact;
            }
            return answerLogMapper.findLatestForFeedback(task.getSessionId(), null);
        }
        return null;
    }

    private List<AiPolicyMessageContextVO> queryMessageContext(Long sessionId) {
        if (sessionId == null) {
            return List.of();
        }
        List<AgentMessageEntity> messages = agentMessageMapper.queryRecentEntityBySessionId(sessionId, 6);
        if (messages == null || messages.isEmpty()) {
            return List.of();
        }
        List<AgentMessageEntity> ordered = new ArrayList<>(messages);
        ordered.sort(Comparator
                .comparing(AgentMessageEntity::getCreateTime, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(AgentMessageEntity::getId, Comparator.nullsLast(Comparator.naturalOrder())));
        return ordered.stream()
                .map(item -> new AiPolicyMessageContextVO(
                        item.getId(),
                        item.getMessageRole(),
                        item.getMessageText(),
                        item.getCitedChunkIds(),
                        item.getCreateTime()
                ))
                .toList();
    }

    private List<Long> parseChunkIds(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        LinkedHashSet<Long> ids = new LinkedHashSet<>();
        for (String item : raw.split(",")) {
            String value = item == null ? "" : item.trim();
            if (value.isBlank()) {
                continue;
            }
            try {
                ids.add(Long.parseLong(value));
            } catch (NumberFormatException ignored) {
                // Keep trace lookup read-only and tolerant of legacy non-numeric evidence ids.
            }
        }
        return ids.isEmpty() ? List.of() : new ArrayList<>(ids);
    }

    private AiPolicyEvidenceItemVO toPolicyChunkEvidence(AiPolicyChunkEntity chunk) {
        String content = chunk.getContent();
        return new AiPolicyEvidenceItemVO(
                "POLICY_CHUNK",
                chunk.getId(),
                chunk.getTitle(),
                chunk.getPolicyKey(),
                chunk.getPolicyName(),
                chunk.getTopicType(),
                chunk.getSourceScene(),
                preview(content),
                content
        );
    }

    private AiPolicyEvidenceItemVO toKnowledgeChunkEvidence(KnowledgeChunkListItemVO chunk) {
        String title = firstNonBlank(chunk.getHeadingPath(), chunk.getChapterTitle(), chunk.getSectionTitle(), "原始知识切片");
        String content = chunk.getContentText();
        return new AiPolicyEvidenceItemVO(
                "KNOWLEDGE_CHUNK",
                chunk.getId(),
                title,
                null,
                chunk.getPolicyName(),
                chunk.getTopicType(),
                chunk.getScenePriority(),
                preview(content),
                content
        );
    }

    private AiPolicyEvidenceItemVO toMessageEvidence(AgentMessageEntity message) {
        String title = firstNonBlank(message.getMessageRole(), "MESSAGE") + " #" + message.getId();
        String content = message.getMessageText();
        return new AiPolicyEvidenceItemVO(
                "MESSAGE",
                message.getId(),
                title,
                null,
                null,
                null,
                message.getCitedChunkIds(),
                preview(content),
                content
        );
    }

    private String preview(String content) {
        String text = safeText(content);
        if (text.length() <= 240) {
            return text;
        }
        return text.substring(0, 240) + "...";
    }

    private AiPolicyFeedbackActionEntity writeAction(Long taskId, String actionType, String actionContent, Long operatorId) {
        AiPolicyFeedbackActionEntity action = new AiPolicyFeedbackActionEntity();
        action.setTaskId(taskId);
        action.setActionType(actionType);
        action.setActionContent(actionContent);
        action.setOperatorId(operatorId);
        action.setCreatedAt(OffsetDateTime.now());
        actionMapper.insert(action);
        return action;
    }

    private AiPolicyFeedbackActionEntity writeDraftAction(Long taskId,
                                                          String draftType,
                                                          String targetTable,
                                                          String actionContent,
                                                          String draftPayload,
                                                          Long operatorId) {
        AiPolicyFeedbackActionEntity action = new AiPolicyFeedbackActionEntity();
        action.setTaskId(taskId);
        action.setActionType("DRAFT_CREATED");
        action.setActionContent(actionContent);
        action.setDraftType(draftType);
        action.setDraftPayload(draftPayload);
        action.setTargetTable(targetTable);
        action.setTargetId(null);
        action.setOperatorId(operatorId);
        action.setCreatedAt(OffsetDateTime.now());
        actionMapper.insert(action);
        return action;
    }

    private String validateDraftPayload(PolicyDraftRequest request) {
        String payload = safeText(request == null ? null : request.getDraftPayload());
        if (payload.isBlank()) {
            throw new IllegalArgumentException("草稿内容不能为空");
        }
        try {
            objectMapper.readTree(payload);
        } catch (Exception ex) {
            throw new IllegalArgumentException("草稿内容必须是合法 JSON");
        }
        return payload;
    }

    private String normalizeStatus(String status) {
        String normalized = status == null ? "" : status.trim().toUpperCase();
        if (!VALID_STATUSES.contains(normalized)) {
            throw new IllegalArgumentException("不支持的纠错任务状态：" + status);
        }
        return normalized;
    }

    private boolean isTerminalStatus(String status) {
        return "NO_ACTION".equals(status) || "RESOLVED".equals(status) || "CLOSED".equals(status);
    }

    private Long currentOperatorId() {
        try {
            LoginUser loginUser = currentUserFacade.currentLoginUser();
            return loginUser == null ? null : loginUser.getUserId();
        } catch (Exception ex) {
            return null;
        }
    }

    private int normalizePageNo(Integer pageNo) {
        return pageNo == null || pageNo < 1 ? 1 : pageNo;
    }

    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return 20;
        }
        return Math.min(pageSize, 100);
    }

    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }

    private String appendHandleResult(String handleResult) {
        String value = safeText(handleResult);
        return value.isBlank() ? "" : "；处理意见：" + value;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    public record PageResult<T>(List<T> list, int total, int pageNo, int pageSize) {
    }

    public record TaskDetail(AiPolicyFeedbackTaskEntity task,
                             List<AiPolicyFeedbackActionEntity> actions) {
    }
}
