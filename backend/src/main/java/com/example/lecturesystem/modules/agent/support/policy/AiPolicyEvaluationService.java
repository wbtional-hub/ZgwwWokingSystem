package com.example.lecturesystem.modules.agent.support.policy;

import com.example.lecturesystem.modules.agent.entity.AiPolicyEvalCaseEntity;
import com.example.lecturesystem.modules.agent.mapper.AiPolicyCandidateAnswerMapper;
import com.example.lecturesystem.modules.agent.mapper.AiPolicyCandidatePhraseMapper;
import com.example.lecturesystem.modules.agent.mapper.AiPolicyEvalCaseMapper;
import com.example.lecturesystem.modules.agent.mapper.AiPolicyIntentSuggestLogMapper;
import com.example.lecturesystem.modules.agent.mapper.AiPolicyUserFavoriteMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AiPolicyEvaluationService {
    private static final String POLICY_SOURCE_SCENE = "MOBILE_POLICY_CONSULTANT";

    private final AiPolicyProperties properties;
    private final AiPolicyEvalCaseMapper aiPolicyEvalCaseMapper;
    private final AiPolicyConsultService aiPolicyConsultService;
    private final AiPolicyIntentSuggestLogMapper aiPolicyIntentSuggestLogMapper;
    private final AiPolicyUserFavoriteMapper aiPolicyUserFavoriteMapper;
    private final AiPolicyCandidatePhraseMapper aiPolicyCandidatePhraseMapper;
    private final AiPolicyCandidateAnswerMapper aiPolicyCandidateAnswerMapper;

    public AiPolicyEvaluationService(AiPolicyProperties properties,
                                     AiPolicyEvalCaseMapper aiPolicyEvalCaseMapper,
                                     AiPolicyConsultService aiPolicyConsultService,
                                     AiPolicyIntentSuggestLogMapper aiPolicyIntentSuggestLogMapper,
                                     AiPolicyUserFavoriteMapper aiPolicyUserFavoriteMapper,
                                     AiPolicyCandidatePhraseMapper aiPolicyCandidatePhraseMapper,
                                     AiPolicyCandidateAnswerMapper aiPolicyCandidateAnswerMapper) {
        this.properties = properties;
        this.aiPolicyEvalCaseMapper = aiPolicyEvalCaseMapper;
        this.aiPolicyConsultService = aiPolicyConsultService;
        this.aiPolicyIntentSuggestLogMapper = aiPolicyIntentSuggestLogMapper;
        this.aiPolicyUserFavoriteMapper = aiPolicyUserFavoriteMapper;
        this.aiPolicyCandidatePhraseMapper = aiPolicyCandidatePhraseMapper;
        this.aiPolicyCandidateAnswerMapper = aiPolicyCandidateAnswerMapper;
    }

    public EvaluationReport evaluate(Long baseId) {
        if (!properties.getEval().isEnabled()) {
            return new EvaluationReport(0, 0, 0, 0, 0, 0D, 0D, 0D, 0D, 0D, 0D, 0D, 0D, 0D, 0D, List.of());
        }
        List<AiPolicyEvalCaseEntity> cases = aiPolicyEvalCaseMapper.queryEnabledByBaseId(baseId);
        List<EvaluationItem> items = new ArrayList<>();
        int regionMatched = 0;
        int policyMatched = 0;
        int questionTypeMatched = 0;
        int faqHits = 0;
        int intentHits = 0;
        int appliedCount = 0;
        int crossRegionFailures = 0;
        int mainDocumentOverrideFailures = 0;
        int topicMisrecallFailures = 0;
        int housingAiServiceMisrecallFailures = 0;

        for (AiPolicyEvalCaseEntity evalCase : cases) {
            AiPolicyConsultService.ConsultResult result =
                    aiPolicyConsultService.consult(null, null, baseId, evalCase.getQuestion(), POLICY_SOURCE_SCENE, false);
            boolean regionOk = evalCase.getRegionScope() == null
                    || evalCase.getRegionScope().equalsIgnoreCase(result.trace().regionScope());
            boolean policyOk = evalCase.getExpectedPolicyKey() == null
                    || evalCase.getExpectedPolicyKey().equalsIgnoreCase(result.trace().policyKey());
            boolean questionTypeOk = evalCase.getExpectedQuestionType() == null
                    || evalCase.getExpectedQuestionType().equalsIgnoreCase(result.trace().questionType());
            boolean containsOk = evalCase.getExpectedAnswerContains() == null
                    || containsAll(result.answer(), evalCase.getExpectedAnswerContains());
            boolean forbiddenOk = evalCase.getExpectedForbiddenKeywords() == null
                    || !containsAny(result.answer(), evalCase.getExpectedForbiddenKeywords());
            boolean passed = result.applied() && regionOk && policyOk && questionTypeOk && containsOk && forbiddenOk;

            if (result.applied()) {
                appliedCount++;
            }
            if (regionOk) {
                regionMatched++;
            } else {
                crossRegionFailures++;
            }
            if (policyOk) {
                policyMatched++;
            } else {
                topicMisrecallFailures++;
            }
            if (questionTypeOk) {
                questionTypeMatched++;
            }
            if (result.trace().faqHit()) {
                faqHits++;
            }
            if (result.trace().matchedIntentId() != null) {
                intentHits++;
            }
            if (contains(result.trace().validationSummary(), "主文档首答")) {
                mainDocumentOverrideFailures++;
            }
            if (isSpecialTopic(evalCase.getExpectedPolicyKey()) && !forbiddenOk) {
                housingAiServiceMisrecallFailures++;
            }

            items.add(new EvaluationItem(
                    evalCase.getQuestion(),
                    passed,
                    result.applied(),
                    result.trace().regionScope(),
                    result.trace().policyKey(),
                    result.trace().questionType(),
                    result.trace().faqHit(),
                    result.trace().matchedIntentId(),
                    result.trace().matchedIntentCode(),
                    result.trace().routePlan(),
                    result.trace().answerMode(),
                    result.trace().validationSummary(),
                    result.answer()
            ));
        }

        int total = cases.size();
        int suggestCount = defaultInt(aiPolicyIntentSuggestLogMapper.countByBaseId(baseId));
        int suggestSelectedCount = defaultInt(aiPolicyIntentSuggestLogMapper.countSelectedByBaseId(baseId));
        int favoriteCount = defaultInt(aiPolicyUserFavoriteMapper.countByBaseId(baseId));
        int favoriteIntentCount = defaultInt(aiPolicyUserFavoriteMapper.countByBaseIdWithIntent(baseId));
        int candidatePhraseTotal = countCandidatePhrases(baseId);
        int candidateAnswerTotal = countCandidateAnswers(baseId);
        int candidatePhraseMerged = defaultInt(aiPolicyCandidatePhraseMapper.countByReviewStatus(baseId, "merged"));
        int candidateAnswerMerged = defaultInt(aiPolicyCandidateAnswerMapper.countByReviewStatus(baseId, "merged"));

        return new EvaluationReport(
                total,
                appliedCount,
                regionMatched,
                policyMatched,
                questionTypeMatched,
                ratio(faqHits, total),
                ratio(intentHits, total),
                ratio(suggestSelectedCount, suggestCount),
                ratio(favoriteIntentCount, favoriteCount),
                ratio(candidatePhraseMerged, candidatePhraseTotal),
                ratio(candidateAnswerMerged, candidateAnswerTotal),
                ratio(crossRegionFailures, total),
                ratio(mainDocumentOverrideFailures, total),
                ratio(topicMisrecallFailures, total),
                ratio(housingAiServiceMisrecallFailures, total),
                items
        );
    }

    private int countCandidatePhrases(Long baseId) {
        return defaultInt(aiPolicyCandidatePhraseMapper.countByReviewStatus(baseId, "pending"))
                + defaultInt(aiPolicyCandidatePhraseMapper.countByReviewStatus(baseId, "rejected"))
                + defaultInt(aiPolicyCandidatePhraseMapper.countByReviewStatus(baseId, "merged"));
    }

    private int countCandidateAnswers(Long baseId) {
        return defaultInt(aiPolicyCandidateAnswerMapper.countByReviewStatus(baseId, "pending"))
                + defaultInt(aiPolicyCandidateAnswerMapper.countByReviewStatus(baseId, "rejected"))
                + defaultInt(aiPolicyCandidateAnswerMapper.countByReviewStatus(baseId, "merged"));
    }

    private boolean containsAll(String text, String expression) {
        if (text == null || expression == null) {
            return false;
        }
        String[] parts = expression.split("&&");
        for (String part : parts) {
            String keyword = part == null ? "" : part.trim();
            if (!keyword.isEmpty() && !text.contains(keyword)) {
                return false;
            }
        }
        return true;
    }

    private boolean contains(String text, String keyword) {
        return containsAny(text, keyword);
    }

    private boolean containsAny(String text, String expression) {
        if (text == null || expression == null) {
            return false;
        }
        String[] parts = expression.split("\\|");
        for (String part : parts) {
            String keyword = part == null ? "" : part.trim();
            if (!keyword.isEmpty() && text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private boolean isSpecialTopic(String policyKey) {
        return "housing".equalsIgnoreCase(policyKey)
                || "ai_talent".equalsIgnoreCase(policyKey)
                || "service_support".equalsIgnoreCase(policyKey);
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    private double ratio(int numerator, int denominator) {
        return denominator == 0 ? 0D : numerator * 1.0D / denominator;
    }

    public record EvaluationReport(int totalCount,
                                   int appliedNewChainCount,
                                   int matchedRegionCount,
                                   int matchedPolicyCount,
                                   int matchedQuestionTypeCount,
                                   double faqHitRate,
                                   double intentHitRate,
                                   double intentSuggestAdoptionRate,
                                   double favoriteReaskHitRate,
                                   double candidatePhraseApprovalRate,
                                   double candidateAnswerApprovalRate,
                                   double crossRegionAnswerRate,
                                   double mainDocOverrideRate,
                                   double topicMisrecallRate,
                                   double housingAiServiceMisrecallRate,
                                   List<EvaluationItem> items) {
    }

    public record EvaluationItem(String question,
                                 boolean passed,
                                 boolean appliedNewChain,
                                 String actualRegionScope,
                                 String actualPolicyKey,
                                 String actualQuestionType,
                                 boolean faqHit,
                                 Long matchedIntentId,
                                 String matchedIntentCode,
                                 String routePlan,
                                 String answerMode,
                                 String validationSummary,
                                 String finalAnswer) {
    }
}
