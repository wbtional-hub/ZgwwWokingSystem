package com.example.lecturesystem.modules.agent.support.policy;

import com.example.lecturesystem.modules.agent.entity.AiPolicyAliasEntity;
import com.example.lecturesystem.modules.agent.entity.AiPolicyChunkEntity;
import com.example.lecturesystem.modules.agent.entity.AiPolicyDocumentEntity;
import com.example.lecturesystem.modules.agent.entity.AiPolicyEvalCaseEntity;
import com.example.lecturesystem.modules.agent.entity.AiPolicyFaqEntity;
import com.example.lecturesystem.modules.agent.entity.AiPolicyIntentAnswerEntity;
import com.example.lecturesystem.modules.agent.entity.AiPolicyIntentEntity;
import com.example.lecturesystem.modules.agent.entity.AiPolicyIntentPhraseEntity;
import com.example.lecturesystem.modules.agent.entity.AiPolicyRouteRuleEntity;
import com.example.lecturesystem.modules.agent.mapper.AiPolicyAliasMapper;
import com.example.lecturesystem.modules.agent.mapper.AiPolicyAnswerLogMapper;
import com.example.lecturesystem.modules.agent.mapper.AiPolicyCandidateAnswerMapper;
import com.example.lecturesystem.modules.agent.mapper.AiPolicyCandidatePhraseMapper;
import com.example.lecturesystem.modules.agent.mapper.AiPolicyChunkMapper;
import com.example.lecturesystem.modules.agent.mapper.AiPolicyDocumentMapper;
import com.example.lecturesystem.modules.agent.mapper.AiPolicyEvalCaseMapper;
import com.example.lecturesystem.modules.agent.mapper.AiPolicyFeedbackMapper;
import com.example.lecturesystem.modules.agent.mapper.AiPolicyFaqMapper;
import com.example.lecturesystem.modules.agent.mapper.AiPolicyIntentAnswerMapper;
import com.example.lecturesystem.modules.agent.mapper.AiPolicyIntentMapper;
import com.example.lecturesystem.modules.agent.mapper.AiPolicyIntentPhraseMapper;
import com.example.lecturesystem.modules.agent.mapper.AiPolicyIntentSuggestLogMapper;
import com.example.lecturesystem.modules.agent.mapper.AiPolicyRetrievalLogMapper;
import com.example.lecturesystem.modules.agent.mapper.AiPolicyRouteRuleMapper;
import com.example.lecturesystem.modules.agent.mapper.AiPolicyUserFavoriteMapper;
import com.example.lecturesystem.modules.knowledge.entity.KnowledgeBaseEntity;
import com.example.lecturesystem.modules.knowledge.mapper.KnowledgeBaseMapper;
import com.example.lecturesystem.modules.knowledge.support.DocxPolicyParser;
import com.example.lecturesystem.modules.knowledge.support.ParsedDocResult;
import com.example.lecturesystem.modules.knowledge.support.ParsedDocSection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Service
public class AiPolicyKnowledgeImportService {
    private static final String SOURCE_SCENE = "MOBILE_POLICY_CONSULTANT";
    private static final Logger log = LoggerFactory.getLogger(AiPolicyKnowledgeImportService.class);

    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final DocxPolicyParser docxPolicyParser;
    private final AiPolicyDocumentMapper aiPolicyDocumentMapper;
    private final AiPolicyChunkMapper aiPolicyChunkMapper;
    private final AiPolicyAliasMapper aiPolicyAliasMapper;
    private final AiPolicyFaqMapper aiPolicyFaqMapper;
    private final AiPolicyRouteRuleMapper aiPolicyRouteRuleMapper;
    private final AiPolicyEvalCaseMapper aiPolicyEvalCaseMapper;
    private final AiPolicyIntentMapper aiPolicyIntentMapper;
    private final AiPolicyIntentPhraseMapper aiPolicyIntentPhraseMapper;
    private final AiPolicyIntentAnswerMapper aiPolicyIntentAnswerMapper;
    private final AiPolicyUserFavoriteMapper aiPolicyUserFavoriteMapper;
    private final AiPolicyFeedbackMapper aiPolicyFeedbackMapper;
    private final AiPolicyCandidatePhraseMapper aiPolicyCandidatePhraseMapper;
    private final AiPolicyCandidateAnswerMapper aiPolicyCandidateAnswerMapper;
    private final AiPolicyIntentSuggestLogMapper aiPolicyIntentSuggestLogMapper;
    private final AiPolicyAnswerLogMapper aiPolicyAnswerLogMapper;
    private final AiPolicyRetrievalLogMapper aiPolicyRetrievalLogMapper;

    public AiPolicyKnowledgeImportService(KnowledgeBaseMapper knowledgeBaseMapper,
                                          DocxPolicyParser docxPolicyParser,
                                          AiPolicyDocumentMapper aiPolicyDocumentMapper,
                                          AiPolicyChunkMapper aiPolicyChunkMapper,
                                          AiPolicyAliasMapper aiPolicyAliasMapper,
                                          AiPolicyFaqMapper aiPolicyFaqMapper,
                                          AiPolicyRouteRuleMapper aiPolicyRouteRuleMapper,
                                          AiPolicyEvalCaseMapper aiPolicyEvalCaseMapper,
                                          AiPolicyIntentMapper aiPolicyIntentMapper,
                                          AiPolicyIntentPhraseMapper aiPolicyIntentPhraseMapper,
                                          AiPolicyIntentAnswerMapper aiPolicyIntentAnswerMapper,
                                          AiPolicyUserFavoriteMapper aiPolicyUserFavoriteMapper,
                                          AiPolicyFeedbackMapper aiPolicyFeedbackMapper,
                                          AiPolicyCandidatePhraseMapper aiPolicyCandidatePhraseMapper,
                                          AiPolicyCandidateAnswerMapper aiPolicyCandidateAnswerMapper,
                                          AiPolicyIntentSuggestLogMapper aiPolicyIntentSuggestLogMapper,
                                          AiPolicyAnswerLogMapper aiPolicyAnswerLogMapper,
                                          AiPolicyRetrievalLogMapper aiPolicyRetrievalLogMapper) {
        this.knowledgeBaseMapper = knowledgeBaseMapper;
        this.docxPolicyParser = docxPolicyParser;
        this.aiPolicyDocumentMapper = aiPolicyDocumentMapper;
        this.aiPolicyChunkMapper = aiPolicyChunkMapper;
        this.aiPolicyAliasMapper = aiPolicyAliasMapper;
        this.aiPolicyFaqMapper = aiPolicyFaqMapper;
        this.aiPolicyRouteRuleMapper = aiPolicyRouteRuleMapper;
        this.aiPolicyEvalCaseMapper = aiPolicyEvalCaseMapper;
        this.aiPolicyIntentMapper = aiPolicyIntentMapper;
        this.aiPolicyIntentPhraseMapper = aiPolicyIntentPhraseMapper;
        this.aiPolicyIntentAnswerMapper = aiPolicyIntentAnswerMapper;
        this.aiPolicyUserFavoriteMapper = aiPolicyUserFavoriteMapper;
        this.aiPolicyFeedbackMapper = aiPolicyFeedbackMapper;
        this.aiPolicyCandidatePhraseMapper = aiPolicyCandidatePhraseMapper;
        this.aiPolicyCandidateAnswerMapper = aiPolicyCandidateAnswerMapper;
        this.aiPolicyIntentSuggestLogMapper = aiPolicyIntentSuggestLogMapper;
        this.aiPolicyAnswerLogMapper = aiPolicyAnswerLogMapper;
        this.aiPolicyRetrievalLogMapper = aiPolicyRetrievalLogMapper;
    }

    @Transactional
    public ResetResult resetBase(Long baseId) {
        requireBase(baseId);
        int answerLogs = safeDelete(() -> aiPolicyAnswerLogMapper.deleteByBaseId(baseId));
        int retrievalLogs = safeDelete(() -> aiPolicyRetrievalLogMapper.deleteByBaseId(baseId));
        int suggestLogs = safeDelete(() -> aiPolicyIntentSuggestLogMapper.deleteByBaseId(baseId));
        int candidateAnswers = safeDelete(() -> aiPolicyCandidateAnswerMapper.deleteByBaseId(baseId));
        int candidatePhrases = safeDelete(() -> aiPolicyCandidatePhraseMapper.deleteByBaseId(baseId));
        int feedbackRows = safeDelete(() -> aiPolicyFeedbackMapper.deleteByBaseId(baseId));
        int favoriteRows = safeDelete(() -> aiPolicyUserFavoriteMapper.deleteByBaseId(baseId));
        int intentAnswers = safeDelete(() -> aiPolicyIntentAnswerMapper.deleteByBaseId(baseId));
        int intentPhrases = safeDelete(() -> aiPolicyIntentPhraseMapper.deleteByBaseId(baseId));
        int intents = safeDelete(() -> aiPolicyIntentMapper.deleteByBaseId(baseId));
        int evalCases = safeDelete(() -> aiPolicyEvalCaseMapper.deleteByBaseId(baseId));
        int routeRules = safeDelete(() -> aiPolicyRouteRuleMapper.deleteByBaseId(baseId));
        int faqs = safeDelete(() -> aiPolicyFaqMapper.deleteByBaseId(baseId));
        int aliases = safeDelete(() -> aiPolicyAliasMapper.deleteByBaseId(baseId));
        int chunks = safeDelete(() -> aiPolicyChunkMapper.deleteByBaseId(baseId));
        int documents = safeDelete(() -> aiPolicyDocumentMapper.deleteByBaseId(baseId));
        return new ResetResult(baseId, documents, chunks, aliases, faqs, routeRules, evalCases,
                intents, intentPhrases, intentAnswers, favoriteRows, feedbackRows, candidatePhrases,
                candidateAnswers, suggestLogs, answerLogs, retrievalLogs);
    }

    @Transactional
    public BootstrapResult bootstrapBase(Long baseId) {
        requireBase(baseId);
        log.info("ai-policy bootstrap start: baseId={}, counts={}", baseId, queryDebugSnapshot(baseId));
        aiPolicyIntentSuggestLogMapper.deleteByBaseId(baseId);
        aiPolicyCandidateAnswerMapper.deleteByBaseId(baseId);
        aiPolicyCandidatePhraseMapper.deleteByBaseId(baseId);
        aiPolicyFeedbackMapper.deleteByBaseId(baseId);
        aiPolicyUserFavoriteMapper.deleteByBaseId(baseId);
        aiPolicyIntentAnswerMapper.deleteByBaseId(baseId);
        aiPolicyIntentPhraseMapper.deleteByBaseId(baseId);
        aiPolicyIntentMapper.deleteByBaseId(baseId);
        aiPolicyEvalCaseMapper.deleteByBaseId(baseId);
        aiPolicyRouteRuleMapper.deleteByBaseId(baseId);
        aiPolicyFaqMapper.deleteByBaseId(baseId);
        aiPolicyAliasMapper.deleteByBaseId(baseId);

        List<AiPolicyIntentEntity> intentSeeds = AiPolicyBootstrapData.buildIntents(baseId);
        if (intentSeeds.isEmpty()) {
            throw new IllegalStateException("ai_policy_intent seed is empty for baseId=" + baseId);
        }
        intentSeeds.forEach(aiPolicyIntentMapper::insert);
        log.info("ai-policy bootstrap after intent insert: baseId={}, intentSeedSize={}, dbIntentCount={}",
                baseId, intentSeeds.size(), defaultInt(aiPolicyIntentMapper.countByBaseId(baseId)));
        List<AiPolicyIntentEntity> persistedIntents = aiPolicyIntentMapper.queryEnabledByBaseId(baseId);
        if (persistedIntents.isEmpty()) {
            throw new IllegalStateException("ai_policy_intent bootstrap inserted 0 rows for baseId=" + baseId);
        }
        List<AiPolicyIntentPhraseEntity> intentPhrases = AiPolicyBootstrapData.buildIntentPhrases(baseId, persistedIntents);
        if (intentPhrases.isEmpty()) {
            throw new IllegalStateException("ai_policy_intent_phrase seed is empty for baseId=" + baseId);
        }
        List<AiPolicyIntentAnswerEntity> intentAnswers = AiPolicyBootstrapData.buildIntentAnswers(baseId, persistedIntents);
        if (intentAnswers.isEmpty()) {
            throw new IllegalStateException("ai_policy_intent_answer seed is empty for baseId=" + baseId);
        }
        List<AiPolicyAliasEntity> aliases = AiPolicyBootstrapData.buildAliases(baseId);
        List<AiPolicyRouteRuleEntity> routeRules = AiPolicyBootstrapData.buildRouteRules(baseId);
        List<AiPolicyFaqEntity> faqs = AiPolicyBootstrapData.buildFaqs(baseId);
        List<AiPolicyEvalCaseEntity> evalCases = AiPolicyBootstrapData.buildEvalCases(baseId);
        intentPhrases.forEach(aiPolicyIntentPhraseMapper::insert);
        log.info("ai-policy bootstrap after phrase insert: baseId={}, intentPhraseSize={}, dbIntentPhraseCount={}",
                baseId, intentPhrases.size(), defaultInt(aiPolicyIntentPhraseMapper.countByBaseId(baseId)));
        intentAnswers.forEach(aiPolicyIntentAnswerMapper::insert);
        log.info("ai-policy bootstrap after answer insert: baseId={}, intentAnswerSize={}, dbIntentAnswerCount={}",
                baseId, intentAnswers.size(), defaultInt(aiPolicyIntentAnswerMapper.countByBaseId(baseId)));
        aliases.forEach(aiPolicyAliasMapper::insert);
        routeRules.forEach(aiPolicyRouteRuleMapper::insert);
        faqs.forEach(aiPolicyFaqMapper::insert);
        evalCases.forEach(aiPolicyEvalCaseMapper::insert);

        BootstrapResult result = new BootstrapResult(
                baseId,
                defaultInt(aiPolicyIntentMapper.countByBaseId(baseId)),
                defaultInt(aiPolicyIntentPhraseMapper.countByBaseId(baseId)),
                defaultInt(aiPolicyIntentAnswerMapper.countByBaseId(baseId)),
                defaultInt(aiPolicyAliasMapper.countByBaseId(baseId)),
                defaultInt(aiPolicyRouteRuleMapper.countByBaseId(baseId)),
                defaultInt(aiPolicyFaqMapper.countByBaseId(baseId)),
                defaultInt(aiPolicyEvalCaseMapper.countByBaseId(baseId))
        );
        log.info("ai-policy bootstrap done: baseId={}, result={}, counts={}", baseId, result, queryDebugSnapshot(baseId));
        return result;
    }

    @Transactional
    public ImportResult importDocument(ImportCommand command, MultipartFile file) throws IOException {
        requireBase(command.baseId());
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("导入文件不能为空");
        }
        log.info("ai-policy import start: baseId={}, sourceFileName={}, counts={}",
                command.baseId(), file.getOriginalFilename(), queryDebugSnapshot(command.baseId()));
        ParsedDocResult parsedDoc = parseFile(file);
        String regionScope = normalizeRegionScope(firstNonBlank(command.regionScope(), parsedDoc.getRegionScope(), inferRegionScope(parsedDoc)));
        String docType = normalizeDocType(command.docType());
        String sourceType = normalizeSourceType(command.sourceType());
        String policyKey = firstNonBlank(command.policyKey(), inferPolicyKey(parsedDoc));
        String policyName = firstNonBlank(command.policyName(), resolveCanonicalPolicyName(policyKey), parsedDoc.getTitle());
        String documentName = firstNonBlank(parsedDoc.getTitle(), file.getOriginalFilename(), "未命名政策文档");
        String aliases = joinAliases(policyKey);

        AiPolicyDocumentEntity document = new AiPolicyDocumentEntity();
        OffsetDateTime now = OffsetDateTime.now();
        document.setBaseId(command.baseId());
        document.setName(documentName);
        document.setRegionScope(regionScope);
        document.setPolicyKey(policyKey);
        document.setPolicyName(policyName);
        document.setDocType(docType);
        document.setSourceType(sourceType);
        document.setSourceFileName(file.getOriginalFilename());
        document.setEnabled(Boolean.TRUE);
        document.setCreatedAt(now);
        document.setUpdatedAt(now);
        aiPolicyDocumentMapper.insert(document);
        if (document.getId() == null) {
            throw new IllegalStateException("ai_policy_document insert did not return generated id for baseId=" + command.baseId());
        }
        log.info("ai-policy import after document insert: baseId={}, sourceFileName={}, documentId={}, documentCount={}",
                command.baseId(), file.getOriginalFilename(), document.getId(), defaultInt(aiPolicyDocumentMapper.countByBaseId(command.baseId())));

        List<AiPolicyChunkEntity> chunks = buildChunks(command.baseId(), document.getId(), regionScope, docType, policyKey, policyName, aliases, parsedDoc, now);
        if (chunks.isEmpty()) {
            throw new IllegalStateException("parsed policy document generated 0 chunks for baseId=" + command.baseId()
                    + ", file=" + firstNonBlank(file.getOriginalFilename(), documentName));
        }
        for (AiPolicyChunkEntity chunk : chunks) {
            aiPolicyChunkMapper.insert(chunk);
        }
        int persistedDocumentCount = defaultInt(aiPolicyDocumentMapper.countByBaseId(command.baseId()));
        int persistedChunkCount = defaultInt(aiPolicyChunkMapper.countByBaseId(command.baseId()));
        log.info("ai-policy import after chunk insert: baseId={}, sourceFileName={}, documentId={}, chunkSize={}, documentCount={}, chunkCount={}",
                command.baseId(), file.getOriginalFilename(), document.getId(), chunks.size(), persistedDocumentCount, persistedChunkCount);
        if (persistedDocumentCount <= 0 || persistedChunkCount <= 0) {
            throw new IllegalStateException("policy import persisted no evidence rows for baseId=" + command.baseId()
                    + ", documents=" + persistedDocumentCount + ", chunks=" + persistedChunkCount);
        }
        ImportResult result = new ImportResult(command.baseId(), document.getId(), documentName, regionScope, docType, policyKey, chunks.size());
        log.info("ai-policy import done: baseId={}, sourceFileName={}, result={}, counts={}",
                command.baseId(), file.getOriginalFilename(), result, queryDebugSnapshot(command.baseId()));
        return result;
    }

    private ParsedDocResult parseFile(MultipartFile file) throws IOException {
        String fileName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        if (fileName.endsWith(".docx")) {
            return docxPolicyParser.parse(file.getInputStream());
        }
        String text = new String(file.getBytes(), StandardCharsets.UTF_8);
        return parsePlainText(firstNonBlank(file.getOriginalFilename(), "导入文本"), text);
    }

    private ParsedDocResult parsePlainText(String title, String content) {
        ParsedDocResult result = new ParsedDocResult();
        result.setTitle(title);
        result.setSummary(abbreviate(content, 120));
        List<ParsedDocSection> sections = new ArrayList<>();
        String[] blocks = content.split("\\r?\\n\\s*\\r?\\n");
        int sectionNo = 1;
        for (String block : blocks) {
            String text = trimToNull(block);
            if (text == null) {
                continue;
            }
            ParsedDocSection section = new ParsedDocSection();
            section.setSectionNo(sectionNo++);
            section.setHeadingPath(title);
            section.setChapterTitle(title);
            section.setSectionTitle(null);
            section.setChunkType("PARAGRAPH");
            section.setContentText(text);
            sections.add(section);
        }
        result.setSections(sections);
        return result;
    }

    private List<AiPolicyChunkEntity> buildChunks(Long baseId,
                                                  Long documentId,
                                                  String regionScope,
                                                  String docType,
                                                  String policyKey,
                                                  String policyName,
                                                  String policyAliases,
                                                  ParsedDocResult parsedDoc,
                                                  OffsetDateTime now) {
        List<AiPolicyChunkEntity> chunks = new ArrayList<>();
        int sortNo = 1;
        for (ParsedDocSection section : parsedDoc.getSections()) {
            String content = trimToNull(section.getContentText());
            if (content == null) {
                continue;
            }
            String title = firstNonBlank(section.getHeadingPath(), section.getSectionTitle(), section.getChapterTitle(), parsedDoc.getTitle());
            String effectiveDocType = resolveChunkDocType(docType, title, content);
            String topicType = detectTopicType(effectiveDocType, title, content);
            String questionType = detectQuestionType(topicType, title, content);
            String answerLevel = detectAnswerLevel(effectiveDocType, topicType);
            int priority = detectPriority(effectiveDocType, topicType);
            SplitRule rule = resolveSplitRule(effectiveDocType);
            for (String part : splitContent(content, rule.minSize(), rule.maxSize())) {
                AiPolicyChunkEntity entity = new AiPolicyChunkEntity();
                entity.setBaseId(baseId);
                entity.setDocumentId(documentId);
                entity.setRegionScope(regionScope);
                entity.setPolicyKey(policyKey);
                entity.setPolicyName(policyName);
                entity.setPolicyAliases(policyAliases);
                entity.setDocType(effectiveDocType);
                entity.setTopicType(topicType);
                entity.setQuestionType(questionType);
                entity.setTitle(title);
                entity.setContent(part);
                entity.setAnswerLevel(answerLevel);
                entity.setPriority(priority);
                entity.setSourceScene(SOURCE_SCENE);
                entity.setSortNo(sortNo++);
                entity.setEnabled(Boolean.TRUE);
                entity.setCreatedAt(now);
                entity.setUpdatedAt(now);
                chunks.add(entity);
            }
        }
        return chunks;
    }

    private String resolveChunkDocType(String documentDocType, String title, String content) {
        if ("route_help".equalsIgnoreCase(documentDocType)) {
            return "route_help";
        }
        return isRouteHelp(title, content) ? "route_help" : documentDocType;
    }

    private boolean isRouteHelp(String title, String content) {
        String text = safeText(title) + " " + safeText(content);
        return containsAny(text, List.of("使用边界", "检索建议", "问答建议", "专题路由建议", "路由建议", "边界说明"));
    }

    private String detectTopicType(String docType, String title, String content) {
        if ("route_help".equalsIgnoreCase(docType)) {
            return "route_help";
        }
        String text = safeText(title) + " " + safeText(content);
        if (containsAny(text, List.of("流程", "步骤", "申报", "申请", "遴选", "组织申报", "资格核查", "联审", "评审", "研究确认"))) {
            return "process";
        }
        if (containsAny(text, List.of("条件", "要求", "对象", "资格", "认定", "适用范围"))) {
            return "condition";
        }
        if (containsAny(text, List.of("拨付", "兑付", "兑现", "分期", "发放"))) {
            return "payment";
        }
        if (containsAny(text, List.of("管理期", "考核周期"))) {
            return "management_period";
        }
        if (containsAny(text, List.of("退出", "追回", "终止", "违约", "考核"))) {
            return "risk";
        }
        if (containsAny(text, List.of("服务保障", "子女教育", "医疗保障", "平台申报", "落户", "住房"))) {
            return "service";
        }
        if (containsAny(text, List.of("补助", "补贴", "奖励", "资助", "标准", "经费", "资金支持"))) {
            return "benefit";
        }
        return "overview";
    }

    private String detectQuestionType(String topicType, String title, String content) {
        return switch (topicType) {
            case "process" -> "process";
            case "condition" -> "condition";
            case "benefit", "payment" -> "benefit";
            case "management_period", "risk" -> "risk";
            case "service" -> "service";
            case "route_help" -> "topic";
            default -> {
                String text = safeText(title) + " " + safeText(content);
                if (containsAny(text, List.of("有哪些", "清单", "目录", "项目列表"))) {
                    yield "list";
                }
                yield "topic";
            }
        };
    }

    private String detectAnswerLevel(String docType, String topicType) {
        if ("route_help".equalsIgnoreCase(docType) || "route_help".equalsIgnoreCase(topicType)) {
            return "support_only";
        }
        if ("main".equalsIgnoreCase(docType)) {
            return "partial";
        }
        if ("source".equalsIgnoreCase(docType)) {
            return "support_only";
        }
        return "direct";
    }

    private int detectPriority(String docType, String topicType) {
        if ("route_help".equalsIgnoreCase(docType)) {
            return 20;
        }
        if ("main".equalsIgnoreCase(docType)) {
            return 60;
        }
        if ("source".equalsIgnoreCase(docType)) {
            return 50;
        }
        return switch (topicType) {
            case "process", "condition", "benefit", "service", "risk" -> 95;
            case "payment", "management_period" -> 90;
            default -> 75;
        };
    }

    private SplitRule resolveSplitRule(String docType) {
        return switch (docType) {
            case "main" -> new SplitRule(300, 500);
            case "topic", "route_help" -> new SplitRule(150, 300);
            case "source" -> new SplitRule(240, 420);
            default -> new SplitRule(180, 320);
        };
    }

    private List<String> splitContent(String content, int minSize, int maxSize) {
        List<String> units = new ArrayList<>();
        for (String paragraph : content.split("\\r?\\n")) {
            String normalized = trimToNull(paragraph);
            if (normalized == null) {
                continue;
            }
            String[] sentences = normalized.split("(?<=[。！？；])");
            for (String sentence : sentences) {
                String value = trimToNull(sentence);
                if (value != null) {
                    units.add(value);
                }
            }
        }
        if (units.isEmpty()) {
            return List.of(content);
        }
        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String unit : units) {
            if (current.length() > 0 && current.length() + unit.length() + 1 > maxSize && current.length() >= minSize) {
                chunks.add(current.toString().trim());
                current = new StringBuilder();
            }
            if (current.length() > 0) {
                current.append('\n');
            }
            current.append(unit);
        }
        if (current.length() > 0) {
            String last = current.toString().trim();
            if (!chunks.isEmpty() && last.length() < minSize) {
                String merged = chunks.get(chunks.size() - 1) + "\n" + last;
                if (merged.length() <= maxSize + minSize / 2) {
                    chunks.set(chunks.size() - 1, merged);
                } else {
                    chunks.add(last);
                }
            } else {
                chunks.add(last);
            }
        }
        return chunks;
    }

    private String inferRegionScope(ParsedDocResult parsedDoc) {
        String text = safeText(parsedDoc.getTitle()) + " " + safeText(parsedDoc.getSummary());
        return normalizeRegionScope(text.contains("厦门") ? "xiamen_city" : text.contains("福建") ? "fujian_province" : null);
    }

    private String inferPolicyKey(ParsedDocResult parsedDoc) {
        String text = safeText(parsedDoc.getTitle()) + " " + safeText(parsedDoc.getSummary());
        if (containsAny(text, List.of("双百计划", "双百人才", "创新团队", "创业人才"))) {
            return "double_hundred";
        }
        if (containsAny(text, List.of("特聘岗位", "高层次人才特聘岗位"))) {
            return "special_post";
        }
        if (containsAny(text, List.of("专项资金", "创业扶持资金"))) {
            return "special_fund";
        }
        if (containsAny(text, List.of("人工智能", "AI 人才", "AI人才"))) {
            return "ai_talent";
        }
        if (containsAny(text, List.of("住房", "安居", "住房补贴"))) {
            return "housing";
        }
        if (containsAny(text, List.of("博士后", "工作站"))) {
            return "postdoc";
        }
        if (containsAny(text, List.of("服务保障", "子女教育", "医疗保障", "平台申报"))) {
            return "service_support";
        }
        if (containsAny(text, List.of("高层次人才认定"))) {
            return "fujian_high_level";
        }
        if (containsAny(text, List.of("近期申报", "专项支持"))) {
            return "fujian_recent_apply";
        }
        if (containsAny(text, List.of("百人计划", "引才百人计划"))) {
            return "fujian_bairen";
        }
        return null;
    }

    private String resolveCanonicalPolicyName(String policyKey) {
        if (policyKey == null) {
            return null;
        }
        return switch (policyKey) {
            case "double_hundred" -> "厦门市引进高层次创新创业人才“双百计划”实施意见";
            case "special_post" -> "厦门市高层次人才特聘岗位实施方案";
            case "special_fund" -> "厦门市高层次人才专项资金管理办法";
            case "ai_talent" -> "厦门市支持人工智能领域人才发展的若干措施";
            case "housing" -> "厦门市引进高层次人才住房补贴实施意见";
            case "postdoc" -> "关于进一步加强博士后工作的若干措施";
            case "service_support" -> "服务保障专题";
            case "fujian_high_level" -> "福建省高层次人才认定与支持专题";
            case "fujian_recent_apply" -> "福建省近期申报与专项支持专题";
            case "fujian_bairen" -> "福建省引才“百人计划”专题";
            default -> null;
        };
    }

    private String joinAliases(String policyKey) {
        if (policyKey == null) {
            return null;
        }
        LinkedHashSet<String> aliases = new LinkedHashSet<>();
        switch (policyKey) {
            case "double_hundred" -> aliases.addAll(List.of("双百计划", "双百人才", "厦门双百", "创新团队", "创业人才"));
            case "special_post" -> aliases.addAll(List.of("特聘岗位", "高层次人才特聘岗位"));
            case "special_fund" -> aliases.addAll(List.of("专项资金", "创业扶持资金"));
            case "housing" -> aliases.addAll(List.of("住房", "安居", "住房补贴"));
            case "ai_talent" -> aliases.addAll(List.of("AI", "人工智能", "AI人才专项"));
            case "postdoc" -> aliases.addAll(List.of("博士后", "工作站", "在站补贴"));
            case "service_support" -> aliases.addAll(List.of("服务保障", "子女教育", "医疗保障", "平台申报"));
            case "fujian_high_level" -> aliases.add("高层次人才认定");
            case "fujian_recent_apply" -> aliases.addAll(List.of("近期申报", "专项支持"));
            case "fujian_bairen" -> aliases.addAll(List.of("福建省百人计划", "引才百人计划", "创业创新人才项目"));
            default -> {
            }
        }
        return aliases.isEmpty() ? null : String.join("|", aliases);
    }

    private String normalizeDocType(String docType) {
        String normalized = trimToNull(docType);
        if (normalized == null) {
            return "topic";
        }
        normalized = normalized.toLowerCase();
        return switch (normalized) {
            case "main", "topic", "source", "route_help" -> normalized;
            default -> "topic";
        };
    }

    private String normalizeSourceType(String sourceType) {
        String normalized = trimToNull(sourceType);
        return normalized == null ? "upload_docx" : normalized;
    }

    private String normalizeRegionScope(String regionScope) {
        String normalized = trimToNull(regionScope);
        if (normalized == null) {
            return "unknown";
        }
        if (normalized.contains("厦门") || "xm".equalsIgnoreCase(normalized) || "xiamen_city".equalsIgnoreCase(normalized)) {
            return "xiamen_city";
        }
        if (normalized.contains("福建") || "fj".equalsIgnoreCase(normalized) || "fujian_province".equalsIgnoreCase(normalized)) {
            return "fujian_province";
        }
        return normalized;
    }

    private boolean containsAny(String text, List<String> keywords) {
        if (text == null || keywords == null) {
            return false;
        }
        for (String keyword : keywords) {
            if (keyword != null && !keyword.isBlank() && text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String safeText(String text) {
        return text == null ? "" : text;
    }

    private String abbreviate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        String normalized = text.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength) + "...";
    }

    private String trimToNull(String text) {
        if (text == null) {
            return null;
        }
        String value = text.trim();
        return value.isEmpty() ? null : value;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private KnowledgeBaseEntity requireBase(Long baseId) {
        if (baseId == null) {
            throw new IllegalArgumentException("baseId 不能为空");
        }
        KnowledgeBaseEntity base = knowledgeBaseMapper.findById(baseId);
        if (base == null) {
            throw new IllegalArgumentException("未找到对应知识库 baseId=" + baseId);
        }
        return base;
    }

    private int safeDelete(DeleteAction action) {
        try {
            return action.execute();
        } catch (Exception ex) {
            return 0;
        }
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    public DebugSnapshot queryDebugSnapshot(Long baseId) {
        return new DebugSnapshot(
                baseId,
                defaultInt(aiPolicyAliasMapper.countByBaseId(baseId)),
                defaultInt(aiPolicyFaqMapper.countByBaseId(baseId)),
                defaultInt(aiPolicyRouteRuleMapper.countByBaseId(baseId)),
                defaultInt(aiPolicyEvalCaseMapper.countByBaseId(baseId)),
                defaultInt(aiPolicyIntentMapper.countByBaseId(baseId)),
                defaultInt(aiPolicyIntentPhraseMapper.countByBaseId(baseId)),
                defaultInt(aiPolicyIntentAnswerMapper.countByBaseId(baseId)),
                defaultInt(aiPolicyDocumentMapper.countByBaseId(baseId)),
                defaultInt(aiPolicyChunkMapper.countByBaseId(baseId))
        );
    }

    @FunctionalInterface
    private interface DeleteAction {
        int execute();
    }

    private record SplitRule(int minSize, int maxSize) {
    }

    public record ImportCommand(Long baseId,
                                String regionScope,
                                String docType,
                                String sourceType,
                                String policyKey,
                                String policyName) {
    }

    public record ResetResult(Long baseId,
                              int documentRows,
                              int chunkRows,
                              int aliasRows,
                              int faqRows,
                              int routeRuleRows,
                              int evalCaseRows,
                              int intentRows,
                              int intentPhraseRows,
                              int intentAnswerRows,
                              int favoriteRows,
                              int feedbackRows,
                              int candidatePhraseRows,
                              int candidateAnswerRows,
                              int suggestLogRows,
                              int answerLogRows,
                              int retrievalLogRows) {
    }

    public record BootstrapResult(Long baseId,
                                  int intentCount,
                                  int intentPhraseCount,
                                  int intentAnswerCount,
                                  int aliasCount,
                                  int routeRuleCount,
                                  int faqCount,
                                  int evalCaseCount) {
    }

    public record ImportResult(Long baseId,
                               Long documentId,
                               String documentName,
                               String regionScope,
                               String docType,
                               String policyKey,
                               int chunkCount) {
    }

    public record DebugSnapshot(Long baseId,
                                int aliasCount,
                                int faqCount,
                                int routeRuleCount,
                                int evalCaseCount,
                                int intentCount,
                                int intentPhraseCount,
                                int intentAnswerCount,
                                int documentCount,
                                int chunkCount) {
    }
}
