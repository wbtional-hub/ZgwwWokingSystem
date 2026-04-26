package com.example.lecturesystem.modules.agent.support.policy;

import com.example.lecturesystem.modules.agent.entity.AiPolicyIntentEntity;
import com.example.lecturesystem.modules.agent.entity.AiPolicyRouteRuleEntity;
import com.example.lecturesystem.modules.agent.mapper.AiPolicyRouteRuleMapper;
import com.example.lecturesystem.modules.agent.support.policy.AiPolicyIntentClassifier.IntentType;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Service
public class AiPolicyRouterService {
    private final AiPolicyRouteRuleMapper aiPolicyRouteRuleMapper;

    public AiPolicyRouterService(AiPolicyRouteRuleMapper aiPolicyRouteRuleMapper) {
        this.aiPolicyRouteRuleMapper = aiPolicyRouteRuleMapper;
    }

    public RoutePlan plan(Long baseId,
                          AiPolicyQuestionNormalizer.NormalizedQuestion question,
                          AiPolicyRegionResolver.RegionMatch regionMatch,
                          AiPolicyIntentClassifier.IntentMatch intentMatch,
                          AiPolicyResolver.PolicyMatch policyMatch) {
        LinkedHashSet<String> regionScopes = new LinkedHashSet<>();
        LinkedHashSet<String> docTypes = new LinkedHashSet<>();
        LinkedHashSet<String> topicTypes = new LinkedHashSet<>();
        LinkedHashSet<String> questionTypes = new LinkedHashSet<>();
        LinkedHashSet<String> queryKeywords = new LinkedHashSet<>();
        LinkedHashSet<String> routeLabels = new LinkedHashSet<>();
        LinkedHashSet<String> policyKeys = new LinkedHashSet<>();

        if (intentMatch.intentType() == IntentType.COMPARE) {
            regionScopes.add("xiamen_city");
            regionScopes.add("fujian_province");
            routeLabels.add("dual-region-compare");
        } else if (regionMatch != null && regionMatch.scope() != AiPolicyRegionResolver.RegionScope.UNKNOWN) {
            regionScopes.add(regionMatch.scope().getCode());
            routeLabels.add("strict-region");
        } else if (policyMatch != null && policyMatch.preferredRegionScope() != null) {
            regionScopes.add(policyMatch.preferredRegionScope());
        }

        if (policyMatch != null && policyMatch.matched()) {
            policyKeys.add(policyMatch.policyKey());
            routeLabels.add("policy:" + policyMatch.policyKey());
            queryKeywords.addAll(policyMatch.aliases());
            if (policyMatch.canonicalPolicyName() != null) {
                queryKeywords.add(policyMatch.canonicalPolicyName());
            }
        }

        switch (intentMatch.intentType()) {
            case LIST -> {
                docTypes.add("main");
                docTypes.add("topic");
                topicTypes.add("overview");
                topicTypes.add("topic");
                questionTypes.add("list");
            }
            case PROCESS -> {
                docTypes.add("topic");
                docTypes.add("source");
                docTypes.add("main");
                topicTypes.add("process");
                topicTypes.add("route_help");
                questionTypes.add("process");
                queryKeywords.addAll(List.of("申请流程", "申报流程", "组织申报", "资格核查", "部门联审", "综合评审", "研究确认"));
            }
            case CONDITION -> {
                docTypes.add("topic");
                docTypes.add("source");
                docTypes.add("main");
                topicTypes.add("condition");
                questionTypes.add("condition");
            }
            case BENEFIT -> {
                docTypes.add("topic");
                docTypes.add("source");
                docTypes.add("main");
                topicTypes.add("benefit");
                topicTypes.add("payment");
                topicTypes.add("management_period");
                questionTypes.add("benefit");
            }
            case SERVICE -> {
                docTypes.add("topic");
                docTypes.add("main");
                topicTypes.add("service");
                questionTypes.add("service");
            }
            case RISK -> {
                docTypes.add("topic");
                docTypes.add("source");
                docTypes.add("main");
                topicTypes.add("risk");
                topicTypes.add("management_period");
                questionTypes.add("risk");
            }
            case COMPARE -> {
                docTypes.add("main");
                docTypes.add("topic");
                topicTypes.add("overview");
                topicTypes.add("topic");
                questionTypes.add("compare");
            }
            default -> {
                docTypes.add("topic");
                docTypes.add("main");
                topicTypes.add("overview");
                topicTypes.add("topic");
                questionTypes.add("topic");
            }
        }

        applyHardRules(policyMatch, docTypes, topicTypes, routeLabels, queryKeywords);
        applyRouteRules(baseId, regionScopes, intentMatch.intentType(), question, docTypes, topicTypes, routeLabels);

        if (question != null) {
            queryKeywords.add(question.compact());
            queryKeywords.addAll(question.terms());
        }

        return new RoutePlan(
                new ArrayList<>(regionScopes),
                new ArrayList<>(policyKeys),
                new ArrayList<>(docTypes),
                new ArrayList<>(topicTypes),
                new ArrayList<>(questionTypes),
                queryKeywords.stream().filter(item -> item != null && !item.isBlank()).limit(16).toList(),
                new ArrayList<>(routeLabels),
                isDetailIntent(intentMatch.intentType()),
                intentMatch.intentType() == IntentType.COMPARE
        );
    }

    public RoutePlan plan(Long baseId,
                          AiPolicyQuestionNormalizer.NormalizedQuestion question,
                          AiPolicyRegionResolver.RegionMatch regionMatch,
                          AiPolicyIntentClassifier.IntentMatch intentMatch,
                          AiPolicyResolver.PolicyMatch policyMatch,
                          AiPolicyIntentEntity matchedIntent) {
        RoutePlan basePlan = plan(baseId, question, regionMatch, intentMatch, policyMatch);
        if (matchedIntent == null) {
            return basePlan;
        }
        LinkedHashSet<String> regionScopes = new LinkedHashSet<>(basePlan.regionScopes());
        LinkedHashSet<String> policyKeys = new LinkedHashSet<>(basePlan.policyKeys());
        LinkedHashSet<String> docTypes = new LinkedHashSet<>(basePlan.docTypes());
        LinkedHashSet<String> topicTypes = new LinkedHashSet<>(basePlan.topicTypes());
        LinkedHashSet<String> questionTypes = new LinkedHashSet<>(basePlan.questionTypes());
        LinkedHashSet<String> queryKeywords = new LinkedHashSet<>(basePlan.queryKeywords());
        LinkedHashSet<String> routeLabels = new LinkedHashSet<>(basePlan.routeLabels());

        if (matchedIntent.getRegionScope() != null && !matchedIntent.getRegionScope().isBlank()) {
            if (!basePlan.compareIntent()) {
                regionScopes.clear();
            }
            regionScopes.add(matchedIntent.getRegionScope());
        }
        if (matchedIntent.getPolicyKey() != null && !matchedIntent.getPolicyKey().isBlank()) {
            policyKeys.add(matchedIntent.getPolicyKey());
        }
        if (matchedIntent.getQuestionType() != null && !matchedIntent.getQuestionType().isBlank()) {
            questionTypes.clear();
            questionTypes.add(matchedIntent.getQuestionType());
        }
        if (matchedIntent.getTopicType() != null && !matchedIntent.getTopicType().isBlank()) {
            topicTypes.add(matchedIntent.getTopicType());
        }
        if (matchedIntent.getStandardQuestion() != null && !matchedIntent.getStandardQuestion().isBlank()) {
            queryKeywords.add(matchedIntent.getStandardQuestion());
        }
        if (matchedIntent.getIntentName() != null && !matchedIntent.getIntentName().isBlank()) {
            queryKeywords.add(matchedIntent.getIntentName());
        }
        routeLabels.add("intent:" + matchedIntent.getIntentCode());

        return new RoutePlan(
                new ArrayList<>(regionScopes),
                new ArrayList<>(policyKeys),
                new ArrayList<>(docTypes),
                new ArrayList<>(topicTypes),
                new ArrayList<>(questionTypes),
                queryKeywords.stream().filter(item -> item != null && !item.isBlank()).limit(20).toList(),
                new ArrayList<>(routeLabels),
                basePlan.detailIntent() || isDetailQuestionType(matchedIntent.getQuestionType()),
                basePlan.compareIntent()
        );
    }

    private void applyHardRules(AiPolicyResolver.PolicyMatch policyMatch,
                                LinkedHashSet<String> docTypes,
                                LinkedHashSet<String> topicTypes,
                                LinkedHashSet<String> routeLabels,
                                LinkedHashSet<String> queryKeywords) {
        if (policyMatch == null || !policyMatch.matched()) {
            return;
        }
        switch (policyMatch.policyKey()) {
            case "double_hundred" -> {
                routeLabels.add("double-hundred-topic-first");
                topicTypes.add("process");
                topicTypes.add("condition");
                topicTypes.add("benefit");
                queryKeywords.add("双百计划");
            }
            case "special_post" -> routeLabels.add("special-post-topic-first");
            case "special_fund" -> routeLabels.add("special-fund-topic-first");
            case "housing" -> {
                routeLabels.add("housing-topic-first");
                topicTypes.add("service");
                queryKeywords.add("住房");
            }
            case "ai_talent" -> routeLabels.add("ai-topic-first");
            case "postdoc" -> routeLabels.add("postdoc-topic-first");
            case "service_support" -> routeLabels.add("service-topic-first");
            default -> {
            }
        }
        docTypes.remove("main");
        docTypes.add("topic");
        docTypes.add("main");
    }

    private void applyRouteRules(Long baseId,
                                 LinkedHashSet<String> regionScopes,
                                 IntentType intentType,
                                 AiPolicyQuestionNormalizer.NormalizedQuestion question,
                                 LinkedHashSet<String> docTypes,
                                 LinkedHashSet<String> topicTypes,
                                 LinkedHashSet<String> routeLabels) {
        if (baseId == null || question == null) {
            return;
        }
        List<AiPolicyRouteRuleEntity> rules;
        try {
            rules = aiPolicyRouteRuleMapper.queryEnabledByBaseId(baseId);
        } catch (Exception ex) {
            return;
        }
        for (AiPolicyRouteRuleEntity rule : rules) {
            if (rule.getQuestionType() != null && !rule.getQuestionType().equalsIgnoreCase(intentType.name().toLowerCase())) {
                continue;
            }
            if (rule.getRegionScope() != null && !rule.getRegionScope().isBlank()
                    && !regionScopes.isEmpty() && !regionScopes.contains(rule.getRegionScope())) {
                continue;
            }
            if (matchesRule(question.compact(), rule.getKeywordPattern())) {
                docTypes.add(rule.getTargetDocType());
                topicTypes.add(rule.getTargetTopicType());
                routeLabels.add("rule:" + rule.getKeywordPattern());
            }
        }
    }

    private boolean matchesRule(String text, String pattern) {
        if (text == null || pattern == null) {
            return false;
        }
        for (String term : pattern.split("\\|")) {
            if (!term.isBlank() && text.contains(term)) {
                return true;
            }
        }
        return false;
    }

    private boolean isDetailIntent(IntentType intentType) {
        return intentType == IntentType.PROCESS
                || intentType == IntentType.CONDITION
                || intentType == IntentType.BENEFIT
                || intentType == IntentType.SERVICE
                || intentType == IntentType.RISK;
    }

    private boolean isDetailQuestionType(String questionType) {
        if (questionType == null) {
            return false;
        }
        return switch (questionType.toLowerCase()) {
            case "process", "condition", "benefit", "service", "risk" -> true;
            default -> false;
        };
    }

    public record RoutePlan(List<String> regionScopes,
                            List<String> policyKeys,
                            List<String> docTypes,
                            List<String> topicTypes,
                            List<String> questionTypes,
                            List<String> queryKeywords,
                            List<String> routeLabels,
                            boolean detailIntent,
                            boolean compareIntent) {
    }
}
