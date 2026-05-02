package com.example.lecturesystem.modules.agent.support.policy;

import com.example.lecturesystem.modules.agent.entity.AiPolicyAnswerLogEntity;
import com.example.lecturesystem.modules.agent.entity.AiPolicyIntentAnswerEntity;
import com.example.lecturesystem.modules.agent.entity.AiPolicyIntentEntity;
import com.example.lecturesystem.modules.agent.entity.AiPolicyRetrievalLogEntity;
import com.example.lecturesystem.modules.agent.mapper.AiPolicyChunkMapper;
import com.example.lecturesystem.modules.agent.mapper.AiPolicyAnswerLogMapper;
import com.example.lecturesystem.modules.agent.mapper.AiPolicyRetrievalLogMapper;
import com.example.lecturesystem.modules.agent.support.KnowledgeCitationContext;
import com.example.lecturesystem.modules.knowledge.vo.KnowledgeSearchResultVO;
import com.example.lecturesystem.modules.logcenter.service.LogCenterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class AiPolicyConsultService {

    private static final Logger log = LoggerFactory.getLogger(AiPolicyConsultService.class);
    private static final String SOURCE_SCENE_MOBILE_POLICY_CONSULTANT = "MOBILE_POLICY_CONSULTANT";
    private static final String SOURCE_SCENE_POLICY_STANDARDIZATION_PHASE1 = "POLICY_STANDARDIZATION_PHASE1";
    private static final String TOPIC_TYPE_DEPARTMENT = "department";
    private static final String TOPIC_TYPE_BENEFIT = "benefit";

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
    private final AiPolicyChunkMapper aiPolicyChunkMapper;
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
                                  AiPolicyChunkMapper aiPolicyChunkMapper,
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
        this.aiPolicyChunkMapper = aiPolicyChunkMapper;
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
            AiPolicyIntentClassifier.IntentMatch intentMatch =
                    intentClassifier.classify(normalizedQuestion, regionMatch, policyMatch);

            AiPolicyIntentPhraseService.MatchResult phraseMatch =
                    intentPhraseService.matchIntent(baseId, userId, normalizedQuestion, regionMatch, policyMatch, intentMatch);

            AiPolicyIntentEntity matchedIntent =
                    resolveMatchedIntent(baseId, regionMatch, policyMatch, intentMatch, phraseMatch);

            List<AiPolicyIntentPhraseService.SuggestedIntent> suggestions =
                    resolveSuggestions(phraseMatch, matchedIntent);

            String suggestedIntents = intentPhraseService.serializeSuggestions(suggestions);
            AiPolicyIntentClassifier.IntentMatch effectiveIntentMatch =
                    resolveEffectiveIntentMatch(intentMatch, matchedIntent);

            Long matchedIntentId = matchedIntent == null ? null : matchedIntent.getId();
            String matchedIntentCode = matchedIntent == null ? null : matchedIntent.getIntentCode();
            Long selectedIntentId = matchedIntentId;

            ConsultResult directDepartmentTopicResult = tryAnswerDepartmentTopicCard(
                    baseId,
                    sessionId,
                    userId,
                    sourceScene,
                    normalizedQuestion,
                    regionMatch,
                    policyMatch,
                    effectiveIntentMatch,
                    matchedIntentId,
                    matchedIntentCode,
                    suggestedIntents,
                    selectedIntentId,
                    persistLogs
            );
            if (directDepartmentTopicResult != null) {
                return directDepartmentTopicResult;
            }

            ConsultResult directFinanceBenefitTopicResult = tryAnswerFinanceBenefitTopicCard(
                    baseId,
                    sessionId,
                    userId,
                    sourceScene,
                    normalizedQuestion,
                    regionMatch,
                    policyMatch,
                    matchedIntentId,
                    matchedIntentCode,
                    suggestedIntents,
                    selectedIntentId,
                    persistLogs
            );
            if (directFinanceBenefitTopicResult != null) {
                return directFinanceBenefitTopicResult;
            }

            if (properties.getFaq().isEnabled()) {
                AiPolicyFaqService.FaqMatch faqMatch =
                        faqService.match(baseId, normalizedQuestion, regionMatch, policyMatch);

                boolean conditionIndexDirectHit = isConditionIndexDirectHit(faqMatch);
                boolean normalFaqDirectHit = faqMatch != null
                        && faqMatch.matched()
                        && !conditionIndexDirectHit
                        && shouldAcceptFaqDirectHit(normalizedQuestion, faqMatch);

                

                if (faqMatch != null && faqMatch.matched() && (conditionIndexDirectHit || normalFaqDirectHit)) {
                    

                    DiagnosticTrace trace = new DiagnosticTrace(
                            true,
                            normalizedQuestion.original(),
                            normalizedQuestion.normalized(),
                            regionMatch.scope().getCode(),
                            policyMatch.policyKey(),
                            effectiveIntentMatch.intentType().name().toLowerCase(),
                            buildRoutePlanText(
                                    matchedIntentCode,
                                    conditionIndexDirectHit ? "condition-index-first" : "faq-first"
                            ),
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
                            conditionIndexDirectHit ? "Condition index direct hit" : "FAQ direct hit",
                            faqMatch.answer(),
                            conditionIndexDirectHit ? "condition_index_hit" : "faq_hit"
                    );

                    if (persistLogs) {
                        writeAnswerLog(baseId, sessionId, userId, sourceScene, trace);
                    }

                    return new ConsultResult(true, faqMatch.answer(), new KnowledgeCitationContext(), trace);
                }

                if (faqMatch != null && faqMatch.matched()) {
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

            String answer = answerComposer.compose(
                    normalizedQuestion,
                    regionMatch,
                    effectiveIntentMatch,
                    policyMatch,
                    routePlan,
                    validationResult
            );

            if (matchedIntent != null) {
                AiPolicyIntentAnswerEntity intentAnswer =
                        intentAnswerService.findBestAnswer(
                                matchedIntent.getId(),
                                validationResult.answerMode().name().toLowerCase()
                        );

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
                    validationResult.answerMode() != AiPolicyEvidenceValidator.AnswerMode.DIRECT_CONFIRMED
                            && matchedIntent == null,
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
                    "baseId=" + baseId
                            + ", error=" + ex.getClass().getSimpleName()
                            + ", message=" + valueOrBlank(ex.getMessage())
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

    private ConsultResult tryAnswerDepartmentTopicCard(Long baseId,
                                                       Long sessionId,
                                                       Long userId,
                                                       String sourceScene,
                                                       AiPolicyQuestionNormalizer.NormalizedQuestion normalizedQuestion,
                                                       AiPolicyRegionResolver.RegionMatch regionMatch,
                                                       AiPolicyResolver.PolicyMatch policyMatch,
                                                       AiPolicyIntentClassifier.IntentMatch effectiveIntentMatch,
                                                       Long matchedIntentId,
                                                       String matchedIntentCode,
                                                       String suggestedIntents,
                                                       Long selectedIntentId,
                                                       boolean persistLogs) {
        String questionText = valueOrBlank(normalizedQuestion == null ? null : normalizedQuestion.original())
                + " "
                + valueOrBlank(normalizedQuestion == null ? null : normalizedQuestion.normalized())
                + " "
                + valueOrBlank(normalizedQuestion == null ? null : normalizedQuestion.compact());
        boolean departmentQuestion = isDepartmentQuestion(questionText);
        String policyKey = resolveDepartmentPolicyKey(policyMatch, questionText);
        boolean matchedPolicy = policyKey != null && !policyKey.isBlank();
        if (!departmentQuestion) {
            return null;
        }
        if (!matchedPolicy) {
            log.info("AI_POLICY_DEPARTMENT_DIRECT_CHECK policyKey={} departmentQuestion={} exactTopic={} phase1DepartmentHit={} skipExactPolicy={}",
                    valueOrBlank(policyKey), departmentQuestion, TOPIC_TYPE_DEPARTMENT, false, false);
            return null;
        }

        KnowledgeSearchResultVO departmentCard = findPhaseOneDepartmentCard(baseId, policyKey);
        boolean phase1DepartmentHit = departmentCard != null;
        log.info("AI_POLICY_DEPARTMENT_DIRECT_CHECK policyKey={} departmentQuestion={} exactTopic={} phase1DepartmentHit={} skipExactPolicy={}",
                policyKey, true, TOPIC_TYPE_DEPARTMENT, phase1DepartmentHit, phase1DepartmentHit);
        if (!phase1DepartmentHit) {
            return null;
        }

        AiPolicyIntentClassifier.IntentMatch departmentIntent =
                new AiPolicyIntentClassifier.IntentMatch(AiPolicyIntentClassifier.IntentType.DEPARTMENT, true);
        AiPolicyRouterService.RoutePlan routePlan = new AiPolicyRouterService.RoutePlan(
                regionMatch == null || regionMatch.scope() == AiPolicyRegionResolver.RegionScope.UNKNOWN
                        ? List.of()
                        : List.of(regionMatch.scope().getCode()),
                List.of(policyKey),
                List.of("topic"),
                List.of(TOPIC_TYPE_DEPARTMENT),
                List.of(TOPIC_TYPE_DEPARTMENT),
                List.of(TOPIC_TYPE_DEPARTMENT),
                List.of("phase1-department-direct", "skipExactPolicy", "exactTopic=department"),
                true,
                false
        );
        AiPolicyEvidenceValidator.ValidationResult validationResult =
                new AiPolicyEvidenceValidator.ValidationResult(
                        AiPolicyEvidenceValidator.AnswerMode.DIRECT_CONFIRMED,
                        List.of(departmentCard),
                        "POLICY_STANDARDIZATION_PHASE1 department topic direct hit.",
                        false,
                        false,
                        false,
                        false
                );
        String answer = answerComposer.compose(
                normalizedQuestion,
                regionMatch,
                departmentIntent,
                policyMatch,
                routePlan,
                validationResult
        );
        KnowledgeCitationContext context = new KnowledgeCitationContext();
        context.getChunks().add(departmentCard);

        DiagnosticTrace trace = new DiagnosticTrace(
                true,
                normalizedQuestion.original(),
                normalizedQuestion.normalized(),
                regionMatch == null ? null : regionMatch.scope().getCode(),
                policyKey,
                TOPIC_TYPE_DEPARTMENT,
                "phase1-department-direct | policyKey=" + policyKey
                        + " | exactTopic=department"
                        + " | phase1DepartmentHit=true"
                        + " | skipExactPolicy=true",
                false,
                false,
                matchedIntentId,
                matchedIntentCode,
                suggestedIntents,
                selectedIntentId,
                false,
                false,
                departmentCard.getChunkId() == null ? "" : String.valueOf(departmentCard.getChunkId()),
                "direct_confirmed",
                "POLICY_STANDARDIZATION_PHASE1 department topic card direct hit.",
                answer,
                "phase1_department_direct_hit"
        );
        if (persistLogs) {
            writeAnswerLog(baseId, sessionId, userId, sourceScene, trace);
        }
        return new ConsultResult(true, answer, context, trace);
    }

    private KnowledgeSearchResultVO findPhaseOneDepartmentCard(Long baseId, String policyKey) {
        if (baseId == null || policyKey == null || policyKey.isBlank()) {
            return null;
        }
        AiPolicyChunkSearchQuery query = new AiPolicyChunkSearchQuery();
        query.setBaseId(baseId);
        query.setPolicyKey(policyKey);
        query.setTopicTypes(List.of(TOPIC_TYPE_DEPARTMENT));
        query.setDocTypes(List.of("topic"));
        query.setEnabled(Boolean.TRUE);
        query.setTopN(10);
        List<KnowledgeSearchResultVO> hits;
        try {
            hits = aiPolicyChunkMapper.search(query);
        } catch (Exception ex) {
            log.info("AI_POLICY_DEPARTMENT_DIRECT_SEARCH_FAILED policyKey={} error={}", policyKey, ex.getClass().getSimpleName());
            return null;
        }
        if (hits == null || hits.isEmpty()) {
            return null;
        }
        for (KnowledgeSearchResultVO hit : hits) {
            if (hit == null) {
                continue;
            }
            if (SOURCE_SCENE_POLICY_STANDARDIZATION_PHASE1.equalsIgnoreCase(hit.getScenePriority())
                    && TOPIC_TYPE_DEPARTMENT.equalsIgnoreCase(hit.getTopicType())) {
                return hit;
            }
        }
        return null;
    }

    private ConsultResult tryAnswerFinanceBenefitTopicCard(Long baseId,
                                                           Long sessionId,
                                                           Long userId,
                                                           String sourceScene,
                                                           AiPolicyQuestionNormalizer.NormalizedQuestion normalizedQuestion,
                                                           AiPolicyRegionResolver.RegionMatch regionMatch,
                                                           AiPolicyResolver.PolicyMatch policyMatch,
                                                           Long matchedIntentId,
                                                           String matchedIntentCode,
                                                           String suggestedIntents,
                                                           Long selectedIntentId,
                                                           boolean persistLogs) {
        String policyKey = policyMatch == null ? null : policyMatch.policyKey();
        if (!"xiamen_finance".equalsIgnoreCase(policyKey)) {
            return null;
        }
        String questionText = valueOrBlank(normalizedQuestion == null ? null : normalizedQuestion.original())
                + " "
                + valueOrBlank(normalizedQuestion == null ? null : normalizedQuestion.normalized())
                + " "
                + valueOrBlank(normalizedQuestion == null ? null : normalizedQuestion.compact());
        if (!isFinanceBenefitTopicQuestion(questionText)) {
            return null;
        }

        KnowledgeSearchResultVO benefitCard = findPhaseOneTopicCard(baseId, policyKey, TOPIC_TYPE_BENEFIT);
        if (benefitCard == null) {
            return null;
        }

        AiPolicyIntentClassifier.IntentMatch benefitIntent =
                new AiPolicyIntentClassifier.IntentMatch(AiPolicyIntentClassifier.IntentType.BENEFIT, true);
        AiPolicyRouterService.RoutePlan routePlan = new AiPolicyRouterService.RoutePlan(
                regionMatch == null || regionMatch.scope() == AiPolicyRegionResolver.RegionScope.UNKNOWN
                        ? List.of()
                        : List.of(regionMatch.scope().getCode()),
                List.of(policyKey),
                List.of("topic"),
                List.of(TOPIC_TYPE_BENEFIT),
                List.of(TOPIC_TYPE_BENEFIT),
                List.of(TOPIC_TYPE_BENEFIT),
                List.of("phase2-finance-benefit-direct", "skipExactPolicy", "exactTopic=benefit"),
                true,
                false
        );
        AiPolicyEvidenceValidator.ValidationResult validationResult =
                new AiPolicyEvidenceValidator.ValidationResult(
                        AiPolicyEvidenceValidator.AnswerMode.DIRECT_CONFIRMED,
                        List.of(benefitCard),
                        "POLICY_STANDARDIZATION_PHASE1 xiamen_finance benefit topic direct hit.",
                        false,
                        false,
                        false,
                        false
                );
        String answer = answerComposer.compose(
                normalizedQuestion,
                regionMatch,
                benefitIntent,
                policyMatch,
                routePlan,
                validationResult
        );
        KnowledgeCitationContext context = new KnowledgeCitationContext();
        context.getChunks().add(benefitCard);

        DiagnosticTrace trace = new DiagnosticTrace(
                true,
                normalizedQuestion.original(),
                normalizedQuestion.normalized(),
                regionMatch == null ? null : regionMatch.scope().getCode(),
                policyKey,
                TOPIC_TYPE_BENEFIT,
                "phase2-finance-benefit-direct | policyKey=" + policyKey
                        + " | exactTopic=benefit"
                        + " | phase1BenefitHit=true"
                        + " | skipExactPolicy=true",
                false,
                false,
                matchedIntentId,
                matchedIntentCode,
                suggestedIntents,
                selectedIntentId,
                false,
                false,
                benefitCard.getChunkId() == null ? "" : String.valueOf(benefitCard.getChunkId()),
                "direct_confirmed",
                "POLICY_STANDARDIZATION_PHASE1 xiamen_finance benefit topic card direct hit.",
                answer,
                "phase2_finance_benefit_direct_hit"
        );
        if (persistLogs) {
            writeAnswerLog(baseId, sessionId, userId, sourceScene, trace);
        }
        return new ConsultResult(true, answer, context, trace);
    }

    private KnowledgeSearchResultVO findPhaseOneTopicCard(Long baseId, String policyKey, String topicType) {
        if (baseId == null || policyKey == null || policyKey.isBlank() || topicType == null || topicType.isBlank()) {
            return null;
        }
        AiPolicyChunkSearchQuery query = new AiPolicyChunkSearchQuery();
        query.setBaseId(baseId);
        query.setPolicyKey(policyKey);
        query.setTopicTypes(List.of(topicType));
        query.setDocTypes(List.of("topic"));
        query.setEnabled(Boolean.TRUE);
        query.setTopN(10);
        List<KnowledgeSearchResultVO> hits;
        try {
            hits = aiPolicyChunkMapper.search(query);
        } catch (Exception ex) {
            log.info("AI_POLICY_TOPIC_DIRECT_SEARCH_FAILED policyKey={} topicType={} error={}",
                    policyKey, topicType, ex.getClass().getSimpleName());
            return null;
        }
        if (hits == null || hits.isEmpty()) {
            return null;
        }
        for (KnowledgeSearchResultVO hit : hits) {
            if (hit == null) {
                continue;
            }
            if (SOURCE_SCENE_POLICY_STANDARDIZATION_PHASE1.equalsIgnoreCase(hit.getScenePriority())
                    && topicType.equalsIgnoreCase(hit.getTopicType())) {
                return hit;
            }
        }
        return null;
    }

    private String resolveDepartmentPolicyKey(AiPolicyResolver.PolicyMatch policyMatch, String questionText) {
        if (policyMatch != null
                && policyMatch.matched()
                && policyMatch.policyKey() != null
                && !policyMatch.policyKey().isBlank()) {
            return policyMatch.policyKey();
        }
        String compact = compactPolicyText(questionText);
        if (compact != null && compact.contains("双百计划")) {
            return "double_hundred";
        }
        return null;
    }

    private boolean isDepartmentQuestion(String text) {
        String compact = compactPolicyText(text);
        return containsAnyText(compact, List.of(
                "主管部门",
                "受理部门",
                "责任部门",
                "归口部门",
                "办理部门",
                "哪个部门",
                "找谁办理",
                "谁负责",
                "由谁负责",
                "由谁执行",
                "谁执行",
                "执行部门",
                "谁审核",
                "由谁审核",
                "哪个部门审核",
                "审核部门",
                "谁牵头",
                "牵头部门",
                "谁组织",
                "谁组织申报",
                "办理主体",
                "责任主体",
                "执行主体",
                "审核主体"
        )) || (containsAnyText(compact, List.of(
                "创业人才",
                "创新人才",
                "创新个人",
                "创新团队",
                "团队",
                "创业",
                "创新"
        )) && containsAnyText(compact, List.of(
                "谁",
                "哪个部门",
                "执行",
                "审核",
                "负责",
                "牵头",
                "组织"
        )));
    }

    private boolean isFinanceBenefitTopicQuestion(String text) {
        String compact = compactPolicyText(text);
        if (compact == null || compact.isBlank()) {
            return false;
        }
        boolean certificateApplyQuestion = containsAnyText(compact.toLowerCase(), List.of("cfa", "frm", "acca", "fsa"))
                && containsAnyText(compact, List.of("可以申请", "能申请", "能不能申请", "是否可以申请", "能否申请"));
        if (certificateApplyQuestion) {
            return false;
        }
        return containsAnyText(compact.toLowerCase(), List.of("cfa", "frm", "acca", "fsa"))
                || containsAnyText(compact, List.of(
                "补贴多少",
                "多少钱",
                "有什么补贴",
                "有什么支持",
                "支持什么",
                "报考费",
                "考试费用",
                "资格证书",
                "专业资格证书",
                "5万元",
                "五万元",
                "累计不超过"
        ));
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
        /*
         * 只接受“短语/联想/标准问题”明确命中的 intent。
         * 不再通过 findBestByProfile 强行兜底，避免没有高置信命中时误套用高优先级 FAQ。
         */
        if (phraseMatch != null && phraseMatch.matched() && phraseMatch.intent() != null) {
            return phraseMatch.intent();
        }
        return null;
    }

    private List<AiPolicyIntentPhraseService.SuggestedIntent> resolveSuggestions(
            AiPolicyIntentPhraseService.MatchResult phraseMatch,
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

    private AiPolicyIntentClassifier.IntentMatch resolveEffectiveIntentMatch(
            AiPolicyIntentClassifier.IntentMatch original,
            AiPolicyIntentEntity matchedIntent) {
        if (matchedIntent == null
                || matchedIntent.getQuestionType() == null
                || matchedIntent.getQuestionType().isBlank()) {
            return original;
        }

        return new AiPolicyIntentClassifier.IntentMatch(
                parseIntentType(matchedIntent.getQuestionType()),
                true
        );
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
            case "department" -> AiPolicyIntentClassifier.IntentType.DEPARTMENT;
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
            case "department" -> "department";
            case "service" -> "service";
            case "risk" -> "risk";
            default -> "overview";
        };
    }

    private boolean isConditionIndexDirectHit(AiPolicyFaqService.FaqMatch faqMatch) {
        return faqMatch != null
                && faqMatch.matched()
                && faqMatch.faq() == null
                && faqMatch.answer() != null
                && !faqMatch.answer().isBlank();
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

        if (containsAnyText(question, List.of(
                "天气", "气温", "下雨", "空气质量", "几点", "现在时间", "今天几号",
                "你是谁", "你是什么模型", "语言模型", "回答错误", "回答错了", "不对", "错了"
        ))) {
            return false;
        }

        if (containsAnyText(question, List.of(
                "适合哪个", "适合什么", "怎么判断", "如何判断", "怎么匹配", "政策匹配", "怎么选择"
        ))) {
            return containsAnyText(answer, List.of("五个维度", "筛选", "适合", "判断", "补充"));
        }

        if (containsAnyText(question, List.of(
                "区别", "不同", "同时享受", "重复享受", "省级", "市级"
        ))) {
            return containsAnyText(answer, List.of(
                    "政策层级", "适用范围", "主管部门", "申报口径", "同时享受", "重复享受"
            ));
        }

        if (answer.contains("双百计划补助标准按申报类别区分")) {
            return question.contains("双百")
                    && containsAnyText(question, List.of(
                    "补助", "补贴", "资金", "多少", "多少钱", "待遇", "支持标准", "标准"
            ));
        }

        if (answer.contains("特聘岗位人选经研究确认")) {
            return question.contains("特聘岗位");
        }

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
                    new DiagnosticTrace(
                            false,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            false,
                            false,
                            null,
                            null,
                            null,
                            null,
                            false,
                            false,
                            null,
                            null,
                            reason,
                            null,
                            reason
                    )
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
