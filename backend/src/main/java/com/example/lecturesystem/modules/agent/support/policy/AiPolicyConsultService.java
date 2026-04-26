package com.example.lecturesystem.modules.agent.support.policy;

import com.example.lecturesystem.modules.agent.entity.AiPolicyIntentAnswerEntity;
import com.example.lecturesystem.modules.agent.entity.AiPolicyIntentEntity;
import com.example.lecturesystem.modules.agent.entity.AiPolicyAnswerLogEntity;
import com.example.lecturesystem.modules.agent.entity.AiPolicyRetrievalLogEntity;
import com.example.lecturesystem.modules.agent.mapper.AiPolicyAnswerLogMapper;
import com.example.lecturesystem.modules.agent.mapper.AiPolicyRetrievalLogMapper;
import com.example.lecturesystem.modules.agent.support.KnowledgeCitationContext;
import com.example.lecturesystem.modules.logcenter.service.LogCenterService;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class AiPolicyConsultService {
    private static final String SOURCE_SCENE_MOBILE_POLICY_CONSULTANT = "MOBILE_POLICY_CONSULTANT";

    private final AiPolicyProperties properties;
    private final AiPolicyQuestionNormalizer normalizer;
    private final AiPolicyRegionResolver regionResolver;
    private final AiPolicyResolver policyResolver;
    private final AiPolicyIntentClassifier intentClassifier;
    private final AiPolicyIntentService intentService;
    private final AiPolicyIntentPhraseService intentPhraseService;
    private final AiPolicyIntentAnswerService intentAnswerService;
    private final AiPolicyFaqService faqService;
    private final AiPolicyRouterService routerService;
    private final AiPolicyHybridRetrievalService hybridRetrievalService;
    private final AiPolicyEvidenceValidator evidenceValidator;
    private final AiPolicyAnswerComposer answerComposer;
    private final AiPolicyAnswerLogMapper answerLogMapper;
    private final AiPolicyRetrievalLogMapper retrievalLogMapper;
    private final LogCenterService logCenterService;

    public AiPolicyConsultService(AiPolicyProperties properties,
                                  AiPolicyQuestionNormalizer normalizer,
                                  AiPolicyRegionResolver regionResolver,
                                  AiPolicyResolver policyResolver,
                                  AiPolicyIntentClassifier intentClassifier,
                                  AiPolicyIntentService intentService,
                                  AiPolicyIntentPhraseService intentPhraseService,
                                  AiPolicyIntentAnswerService intentAnswerService,
                                  AiPolicyFaqService faqService,
                                  AiPolicyRouterService routerService,
                                  AiPolicyHybridRetrievalService hybridRetrievalService,
                                  AiPolicyEvidenceValidator evidenceValidator,
                                  AiPolicyAnswerComposer answerComposer,
                                  AiPolicyAnswerLogMapper answerLogMapper,
                                  AiPolicyRetrievalLogMapper retrievalLogMapper,
                                  LogCenterService logCenterService) {
        this.properties = properties;
        this.normalizer = normalizer;
        this.regionResolver = regionResolver;
        this.policyResolver = policyResolver;
        this.intentClassifier = intentClassifier;
        this.intentService = intentService;
        this.intentPhraseService = intentPhraseService;
        this.intentAnswerService = intentAnswerService;
        this.faqService = faqService;
        this.routerService = routerService;
        this.hybridRetrievalService = hybridRetrievalService;
        this.evidenceValidator = evidenceValidator;
        this.answerComposer = answerComposer;
        this.answerLogMapper = answerLogMapper;
        this.retrievalLogMapper = retrievalLogMapper;
        this.logCenterService = logCenterService;
    }

    public ConsultResult consult(Long sessionId,
                                 Long userId,
                                 Long baseId,
                                 String question,
                                 String sourceScene) {
        return consult(sessionId, userId, baseId, question, sourceScene, true);
    }

    public ConsultResult consult(Long sessionId,
                                 Long userId,
                                 Long baseId,
                                 String question,
                                 String sourceScene,
                                 boolean persistLogs) {
        if (!properties.isEnabled()) {
            return ConsultResult.notApplied("new_chain_disabled");
        }
        if (baseId == null) {
            return ConsultResult.notApplied("base_id_missing");
        }
        if (!SOURCE_SCENE_MOBILE_POLICY_CONSULTANT.equalsIgnoreCase(sourceScene)) {
            return ConsultResult.notApplied("source_scene_not_policy");
        }
        AiPolicyQuestionNormalizer.NormalizedQuestion normalizedQuestion = normalizer.normalize(question);
        if (normalizedQuestion == null) {
            return ConsultResult.notApplied("question_empty");
        }
        try {
            AiPolicyRegionResolver.RegionMatch regionMatch = regionResolver.resolve(normalizedQuestion);
            AiPolicyResolver.PolicyMatch policyMatch = policyResolver.resolve(baseId, normalizedQuestion, regionMatch);
            AiPolicyIntentClassifier.IntentMatch intentMatch = intentClassifier.classify(normalizedQuestion, regionMatch, policyMatch);
            AiPolicyIntentPhraseService.MatchResult phraseMatch =
                    intentPhraseService.matchIntent(baseId, userId, normalizedQuestion, regionMatch, policyMatch, intentMatch);
            AiPolicyIntentEntity matchedIntent = resolveMatchedIntent(baseId, regionMatch, policyMatch, intentMatch, phraseMatch);
            List<AiPolicyIntentPhraseService.SuggestedIntent> suggestions =
                    resolveSuggestions(phraseMatch, matchedIntent);
            String suggestedIntents = intentPhraseService.serializeSuggestions(suggestions);
            AiPolicyIntentClassifier.IntentMatch effectiveIntentMatch = resolveEffectiveIntentMatch(intentMatch, matchedIntent);
            Long matchedIntentId = matchedIntent == null ? null : matchedIntent.getId();
            String matchedIntentCode = matchedIntent == null ? null : matchedIntent.getIntentCode();
            Long selectedIntentId = matchedIntentId;

            if (properties.getFaq().isEnabled()) {
    AiPolicyFaqService.FaqMatch faqMatch = faqService.match(baseId, normalizedQuestion, regionMatch, policyMatch);
    if (faqMatch.matched() && shouldAcceptFaqDirectHit(normalizedQuestion, faqMatch)) {
        DiagnosticTrace trace = new DiagnosticTrace(
                true,
                normalizedQuestion.original(),
                normalizedQuestion.normalized(),
                regionMatch.scope().getCode(),
                policyMatch.policyKey(),
                effectiveIntentMatch.intentType().name().toLowerCase(),
                buildRoutePlanText(matchedIntentCode, "faq-first"),
                true,
                false,
                matchedIntentId,
                matchedIntentCode,
                suggestedIntents,
                selectedIntentId,
                false,
                false,
                "",
                "direct_confirmed",
                "FAQ direct hit",
                faqMatch.answer(),
                "faq_hit"
        );
        if (persistLogs) {
            writeAnswerLog(baseId, sessionId, userId, sourceScene, trace);
        }
        return new ConsultResult(true, faqMatch.answer(), new KnowledgeCitationContext(), trace);
    }

    if (faqMatch.matched()) {
        logCenterService.recordAiChainFailed(
                "AI_POLICY_FAQ_REJECTED_LOW_CONFIDENCE",
                sessionId,
                userId,
                sourceScene,
                "question=" + valueOrBlank(normalizedQuestion.original())
                        + ", answerPreview=" + valueOrBlank(previewText(faqMatch.answer()))
        );
    }
}

            AiPolicyRouterService.RoutePlan routePlan =
                    routerService.plan(baseId, normalizedQuestion, regionMatch, effectiveIntentMatch, policyMatch, matchedIntent);
            AiPolicyHybridRetrievalService.RetrievalResult retrievalResult =
                    hybridRetrievalService.retrieve(baseId, normalizedQuestion, regionMatch, effectiveIntentMatch, policyMatch, routePlan);
            if (retrievalResult.retrievalSummary().startsWith("search_error")) {
                return ConsultResult.notApplied(retrievalResult.retrievalSummary());
            }

            AiPolicyEvidenceValidator.ValidationResult validationResult =
                    evidenceValidator.validate(effectiveIntentMatch, regionMatch, policyMatch, routePlan, retrievalResult);
            String answer = answerComposer.compose(normalizedQuestion, regionMatch, effectiveIntentMatch, policyMatch, routePlan, validationResult);
            if (matchedIntent != null) {
                AiPolicyIntentAnswerEntity intentAnswer =
                        intentAnswerService.findBestAnswer(matchedIntent.getId(), validationResult.answerMode().name().toLowerCase());
                answer = intentAnswerService.render(
                        matchedIntent,
                        validationResult.answerMode().name().toLowerCase(),
                        intentAnswer,
                        validationResult,
                        answer
                );
            }

            KnowledgeCitationContext context = new KnowledgeCitationContext();
            context.getChunks().addAll(validationResult.primaryHits());
            DiagnosticTrace trace = new DiagnosticTrace(
                    true,
                    normalizedQuestion.original(),
                    normalizedQuestion.normalized(),
                    regionMatch.scope().getCode(),
                    policyMatch.policyKey(),
                    effectiveIntentMatch.intentType().name().toLowerCase(),
                    buildRoutePlanText(matchedIntentCode, String.join(" | ", routePlan.routeLabels())),
                    false,
                    validationResult.answerMode() != AiPolicyEvidenceValidator.AnswerMode.DIRECT_CONFIRMED && matchedIntent == null,
                    matchedIntentId,
                    matchedIntentCode,
                    suggestedIntents,
                    selectedIntentId,
                    false,
                    false,
                    String.join(",", validationResult.chunkIds()),
                    validationResult.answerMode().name().toLowerCase(),
                    validationResult.validationSummary(),
                    answer,
                    retrievalResult.retrievalSummary()
            );
            if (persistLogs) {
                writeRetrievalLog(baseId, sessionId, userId, sourceScene, trace, retrievalResult);
                writeAnswerLog(baseId, sessionId, userId, sourceScene, trace);
            }
            return new ConsultResult(true, answer, context, trace);
        } catch (Exception ex) {
            logCenterService.recordAiChainFailed(
                    "AI_POLICY_NEW_CHAIN_FAILED",
                    sessionId,
                    userId,
                    sourceScene,
                    "baseId=" + baseId + ", error=" + ex.getClass().getSimpleName() + ", message=" + valueOrBlank(ex.getMessage())
            );
            return ConsultResult.notApplied("new_chain_exception:" + ex.getClass().getSimpleName());
        }
    }

    private void writeRetrievalLog(Long baseId,
                                   Long sessionId,
                                   Long userId,
                                   String sourceScene,
                                   DiagnosticTrace trace,
                                   AiPolicyHybridRetrievalService.RetrievalResult retrievalResult) {
        try {
            AiPolicyRetrievalLogEntity entity = new AiPolicyRetrievalLogEntity();
            entity.setBaseId(baseId);
            entity.setSessionId(sessionId);
            entity.setUserId(userId);
            entity.setSourceScene(sourceScene);
            entity.setRawQuestion(trace.rawQuestion());
            entity.setNormalizedQuestion(trace.normalizedQuestion());
            entity.setRegionScope(trace.regionScope());
            entity.setPolicyKey(trace.policyKey());
            entity.setQuestionType(trace.questionType());
            entity.setRoutePlan(trace.routePlan());
            entity.setMatchedIntentId(trace.matchedIntentId());
            entity.setMatchedIntentCode(trace.matchedIntentCode());
            entity.setSuggestedIntents(trace.suggestedIntents());
            entity.setSelectedIntentId(trace.selectedIntentId());
            entity.setLearningSignalWritten(trace.learningSignalWritten());
            entity.setCandidateGenerated(trace.candidateGenerated());
            entity.setSearchQuery(String.join(" | ", retrievalResult.searchQueries()));
            entity.setHitChunkIds(trace.hitChunkIds());
            entity.setRetrievalSummary(trace.retrievalSummary());
            entity.setCreatedAt(OffsetDateTime.now());
            retrievalLogMapper.insert(entity);
        } catch (Exception ignored) {
        }
    }

    private void writeAnswerLog(Long baseId,
                                Long sessionId,
                                Long userId,
                                String sourceScene,
                                DiagnosticTrace trace) {
        try {
            AiPolicyAnswerLogEntity entity = new AiPolicyAnswerLogEntity();
            entity.setBaseId(baseId);
            entity.setSessionId(sessionId);
            entity.setUserId(userId);
            entity.setSourceScene(sourceScene);
            entity.setAppliedNewChain(trace.appliedNewChain());
            entity.setRawQuestion(trace.rawQuestion());
            entity.setNormalizedQuestion(trace.normalizedQuestion());
            entity.setRegionScope(trace.regionScope());
            entity.setPolicyKey(trace.policyKey());
            entity.setQuestionType(trace.questionType());
            entity.setRoutePlan(trace.routePlan());
            entity.setFaqHit(trace.faqHit());
            entity.setFallbackFlag(trace.fallbackFlag());
            entity.setMatchedIntentId(trace.matchedIntentId());
            entity.setMatchedIntentCode(trace.matchedIntentCode());
            entity.setSuggestedIntents(trace.suggestedIntents());
            entity.setSelectedIntentId(trace.selectedIntentId());
            entity.setLearningSignalWritten(trace.learningSignalWritten());
            entity.setCandidateGenerated(trace.candidateGenerated());
            entity.setHitChunkIds(trace.hitChunkIds());
            entity.setAnswerMode(trace.answerMode());
            entity.setValidationSummary(trace.validationSummary());
            entity.setFinalAnswer(trace.finalAnswer());
            entity.setCreatedAt(OffsetDateTime.now());
            answerLogMapper.insert(entity);
        } catch (Exception ignored) {
        }
    }

    private AiPolicyIntentEntity resolveMatchedIntent(Long baseId,
                                                  AiPolicyRegionResolver.RegionMatch regionMatch,
                                                  AiPolicyResolver.PolicyMatch policyMatch,
                                                  AiPolicyIntentClassifier.IntentMatch intentMatch,
                                                  AiPolicyIntentPhraseService.MatchResult phraseMatch) {
    // 只接受“短语/联想/标准问题”明确命中的 intent。
    // 不再通过 findBestByProfile 强行兜底，避免没有高置信命中时误套用高优先级 FAQ。
    if (phraseMatch != null && phraseMatch.matched() && phraseMatch.intent() != null) {
        return phraseMatch.intent();
    }

    return null;
}

    private List<AiPolicyIntentPhraseService.SuggestedIntent> resolveSuggestions(AiPolicyIntentPhraseService.MatchResult phraseMatch,
                                                                                 AiPolicyIntentEntity matchedIntent) {
        if (phraseMatch != null && phraseMatch.suggestions() != null && !phraseMatch.suggestions().isEmpty()) {
            return phraseMatch.suggestions();
        }
        if (matchedIntent == null) {
            return List.of();
        }
        return List.of(new AiPolicyIntentPhraseService.SuggestedIntent(
                matchedIntent,
                matchedIntent.getPriority() == null ? 0 : matchedIntent.getPriority()
        ));
    }

    private AiPolicyIntentClassifier.IntentMatch resolveEffectiveIntentMatch(AiPolicyIntentClassifier.IntentMatch original,
                                                                             AiPolicyIntentEntity matchedIntent) {
        if (matchedIntent == null || matchedIntent.getQuestionType() == null || matchedIntent.getQuestionType().isBlank()) {
            return original;
        }
        return new AiPolicyIntentClassifier.IntentMatch(parseIntentType(matchedIntent.getQuestionType()), true);
    }

    private AiPolicyIntentClassifier.IntentType parseIntentType(String questionType) {
        if (questionType == null) {
            return AiPolicyIntentClassifier.IntentType.TOPIC;
        }
        return switch (questionType.toLowerCase()) {
            case "list" -> AiPolicyIntentClassifier.IntentType.LIST;
            case "process" -> AiPolicyIntentClassifier.IntentType.PROCESS;
            case "condition" -> AiPolicyIntentClassifier.IntentType.CONDITION;
            case "benefit" -> AiPolicyIntentClassifier.IntentType.BENEFIT;
            case "risk" -> AiPolicyIntentClassifier.IntentType.RISK;
            case "service" -> AiPolicyIntentClassifier.IntentType.SERVICE;
            case "compare" -> AiPolicyIntentClassifier.IntentType.COMPARE;
            default -> AiPolicyIntentClassifier.IntentType.TOPIC;
        };
    }

    private String defaultTopicType(String questionType) {
        if (questionType == null) {
            return "overview";
        }
        return switch (questionType.toLowerCase()) {
            case "list", "topic" -> "overview";
            case "process" -> "process";
            case "condition" -> "condition";
            case "benefit" -> "benefit";
            case "service" -> "service";
            case "risk" -> "risk";
            default -> "overview";
        };
    }
private boolean shouldAcceptFaqDirectHit(AiPolicyQuestionNormalizer.NormalizedQuestion normalizedQuestion,
                                         AiPolicyFaqService.FaqMatch faqMatch) {
    String question = compactPolicyText(
            valueOrBlank(normalizedQuestion.original()) + " " + valueOrBlank(normalizedQuestion.normalized())
    );
    String answer = compactPolicyText(faqMatch == null ? null : faqMatch.answer());

    if (question == null || answer == null) {
        return false;
    }

    // 明显非政策问题，不能进入 FAQ 快速回答
    if (containsAnyText(question, List.of(
            "天气", "气温", "下雨", "空气质量", "几点", "现在时间", "今天几号",
            "你是谁", "你是什么模型", "语言模型", "回答错误", "回答错了", "不对", "错了"
    ))) {
        return false;
    }

    // 用户问“适合哪个政策 / 怎么判断”，不能返回“省市政策区别”或“双百补助”
    if (containsAnyText(question, List.of("适合哪个", "适合什么", "怎么判断", "如何判断", "怎么匹配", "政策匹配", "怎么选择"))) {
        return containsAnyText(answer, List.of("五个维度", "筛选", "适合", "判断", "补充"));
    }

    // 用户问“区别 / 同时享受”，不能返回具体补助标准
    if (containsAnyText(question, List.of("区别", "不同", "同时享受", "重复享受", "省级", "市级"))) {
        return containsAnyText(answer, List.of("政策层级", "适用范围", "主管部门", "申报口径", "同时享受", "重复享受"));
    }

    // 防止最常见错误：不是问双百补助，却返回“双百计划补助标准”
    if (answer.contains("双百计划补助标准按申报类别区分")) {
        return question.contains("双百")
                && containsAnyText(question, List.of("补助", "补贴", "资金", "多少", "多少钱", "待遇", "支持标准", "标准"));
    }

    // 防止不是问特聘岗位，却返回特聘岗位答案
    if (answer.contains("特聘岗位人选经研究确认")) {
        return question.contains("特聘岗位");
    }

    // 防止不是问创业资金，却返回创业资金拨付答案
    if (answer.contains("创业人才创业扶持资金")) {
        return containsAnyText(question, List.of("创业资金", "创业扶持资金", "创业人才", "怎么拨", "拨付"));
    }

    return true;
}

private String compactPolicyText(String text) {
    if (text == null) {
        return null;
    }
    String value = text
            .replaceAll("\\s+", "")
            .replaceAll("[？?。！!，,、；;：:]", "");
    return value.isBlank() ? null : value;
}

private boolean containsAnyText(String text, List<String> keywords) {
    if (text == null || keywords == null || keywords.isEmpty()) {
        return false;
    }
    for (String keyword : keywords) {
        if (keyword != null && !keyword.isBlank() && text.contains(keyword)) {
            return true;
        }
    }
    return false;
}

private String previewText(String text) {
    if (text == null) {
        return "";
    }
    String value = text.replaceAll("\\s+", " ").trim();
    if (value.length() <= 120) {
        return value;
    }
    return value.substring(0, 120);
}
    private String buildRoutePlanText(String matchedIntentCode, String routePlan) {
        if (matchedIntentCode == null || matchedIntentCode.isBlank()) {
            return routePlan;
        }
        if (routePlan == null || routePlan.isBlank()) {
            return "intent:" + matchedIntentCode;
        }
        return "intent:" + matchedIntentCode + " | " + routePlan;
    }

    private String valueOrBlank(String text) {
        return text == null ? "" : text;
    }

    public record ConsultResult(boolean applied,
                                String answer,
                                KnowledgeCitationContext context,
                                DiagnosticTrace trace) {
        public static ConsultResult notApplied(String reason) {
            return new ConsultResult(
                    false,
                    null,
                    new KnowledgeCitationContext(),
                    new DiagnosticTrace(false, null, null, null, null, null, null, false, false,
                            null, null, null, null, false, false, null, null, reason, null, reason)
            );
        }
    }

    public record DiagnosticTrace(boolean appliedNewChain,
                                  String rawQuestion,
                                  String normalizedQuestion,
                                  String regionScope,
                                  String policyKey,
                                  String questionType,
                                  String routePlan,
                                  boolean faqHit,
                                  boolean fallbackFlag,
                                  Long matchedIntentId,
                                  String matchedIntentCode,
                                  String suggestedIntents,
                                  Long selectedIntentId,
                                  boolean learningSignalWritten,
                                  boolean candidateGenerated,
                                  String hitChunkIds,
                                  String answerMode,
                                  String validationSummary,
                                  String finalAnswer,
                                  String retrievalSummary) {
    }
}
