package com.example.lecturesystem.modules.agent.support.policy;

import com.example.lecturesystem.modules.agent.support.policy.AiPolicyIntentClassifier.IntentType;
import com.example.lecturesystem.modules.knowledge.vo.KnowledgeSearchResultVO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

@Service
public class AiPolicyEvidenceValidator {
    public ValidationResult validate(AiPolicyIntentClassifier.IntentMatch intentMatch,
                                     AiPolicyRegionResolver.RegionMatch regionMatch,
                                     AiPolicyResolver.PolicyMatch policyMatch,
                                     AiPolicyRouterService.RoutePlan routePlan,
                                     AiPolicyHybridRetrievalService.RetrievalResult retrievalResult) {
        List<KnowledgeSearchResultVO> hits = retrievalResult == null ? List.of() : retrievalResult.hits();
        if (hits.isEmpty()) {
            return new ValidationResult(
                    AnswerMode.INSUFFICIENT_EVIDENCE,
                    List.of(),
                    "当前未命中足够的 AI 政策专题证据。",
                    true,
                    false,
                    false,
                    false
            );
        }

        List<KnowledgeSearchResultVO> regionMatched = filterByRegion(regionMatch, routePlan, hits);
        boolean crossRegionBlocked = !regionMatched.isEmpty() && regionMatched.size() < hits.size();
        if (regionMatched.isEmpty() && regionMatch != null && regionMatch.scope() != AiPolicyRegionResolver.RegionScope.UNKNOWN) {
            return new ValidationResult(
                    AnswerMode.INSUFFICIENT_EVIDENCE,
                    List.of(),
                    "已识别到明确地域，但当前命中结果与该地域不一致，已拦截跨地区替代回答。",
                    true,
                    false,
                    true,
                    false
            );
        }

        List<KnowledgeSearchResultVO> policyMatched = filterByPolicy(policyMatch, regionMatched.isEmpty() ? hits : regionMatched);
        boolean policyMiss = policyMatch != null && policyMatch.matched() && policyMatched.isEmpty();
        if (policyMiss) {
            return new ValidationResult(
                    AnswerMode.INSUFFICIENT_EVIDENCE,
                    List.of(),
                    "已识别到明确政策主题，但当前未命中对应专题证据，已避免跨政策混答。",
                    true,
                    false,
                    crossRegionBlocked,
                    true
            );
        }

        List<KnowledgeSearchResultVO> primary = policyMatched.isEmpty() ? (regionMatched.isEmpty() ? hits : regionMatched) : policyMatched;
        List<KnowledgeSearchResultVO> topicFirst = preferTopicEvidence(routePlan, primary);
        boolean mainDocumentOnly = topicFirst.stream().allMatch(this::isMainDocument);
        boolean routeHelpOnly = topicFirst.stream().allMatch(this::isRouteHelp);

        AnswerMode answerMode;
        String summary;
        boolean suggestDive;
        if (intentMatch.intentType() == IntentType.COMPARE) {
            answerMode = topicFirst.size() >= 2 ? AnswerMode.DIRECT_CONFIRMED : AnswerMode.PARTIAL_CONFIRMED;
            summary = "已按厦门/福建两个地域分别筛选并组织对比证据。";
            suggestDive = topicFirst.size() < 4;
        } else if (routeHelpOnly) {
            answerMode = AnswerMode.PARTIAL_CONFIRMED;
            summary = "当前主要命中路由提示或使用边界信息，仍缺少可直接展开的事实条款。";
            suggestDive = true;
        } else if (routePlan.detailIntent() && mainDocumentOnly) {
            answerMode = AnswerMode.PARTIAL_CONFIRMED;
            summary = "当前仅命中主文档首答证据，细节题仍需继续命中专题或原文条款。";
            suggestDive = true;
        } else if (topicFirst.size() == 1) {
            answerMode = routePlan.detailIntent() ? AnswerMode.PARTIAL_CONFIRMED : AnswerMode.DIRECT_CONFIRMED;
            summary = routePlan.detailIntent()
                    ? "当前已命中单条核心专题证据，可先回答已确认部分。"
                    : "当前已命中单条核心证据，可直接给出首答。";
            suggestDive = routePlan.detailIntent();
        } else {
            answerMode = AnswerMode.DIRECT_CONFIRMED;
            summary = "当前已命中同地域、同政策的多条专题证据，可直接组织回答。";
            suggestDive = false;
        }

        return new ValidationResult(
                answerMode,
                topicFirst.stream().limit(8).toList(),
                summary,
                suggestDive,
                mainDocumentOnly,
                crossRegionBlocked,
                false
        );
    }

    private List<KnowledgeSearchResultVO> filterByRegion(AiPolicyRegionResolver.RegionMatch regionMatch,
                                                         AiPolicyRouterService.RoutePlan routePlan,
                                                         List<KnowledgeSearchResultVO> hits) {
        if (routePlan.compareIntent()) {
            return hits.stream()
                    .filter(hit -> "xiamen_city".equalsIgnoreCase(hit.getRegionScope())
                            || "fujian_province".equalsIgnoreCase(hit.getRegionScope()))
                    .toList();
        }
        if (regionMatch == null || regionMatch.scope() == AiPolicyRegionResolver.RegionScope.UNKNOWN) {
            return new ArrayList<>(hits);
        }
        return hits.stream()
                .filter(hit -> regionMatch.scope().getCode().equalsIgnoreCase(hit.getRegionScope()))
                .toList();
    }

    private List<KnowledgeSearchResultVO> filterByPolicy(AiPolicyResolver.PolicyMatch policyMatch,
                                                         List<KnowledgeSearchResultVO> hits) {
        if (policyMatch == null || !policyMatch.matched()) {
            return new ArrayList<>(hits);
        }
        List<KnowledgeSearchResultVO> matched = new ArrayList<>();
        for (KnowledgeSearchResultVO hit : hits) {
            String text = buildText(hit);
            if (policyMatch.canonicalPolicyName() != null && text.contains(policyMatch.canonicalPolicyName())) {
                matched.add(hit);
                continue;
            }
            for (String alias : policyMatch.aliases()) {
                if (text.contains(alias)) {
                    matched.add(hit);
                    break;
                }
            }
        }
        return matched;
    }

    private List<KnowledgeSearchResultVO> preferTopicEvidence(AiPolicyRouterService.RoutePlan routePlan,
                                                              List<KnowledgeSearchResultVO> hits) {
        if (hits.isEmpty()) {
            return hits;
        }
        if (!routePlan.detailIntent()) {
            return deduplicateByChunkId(hits);
        }
        List<KnowledgeSearchResultVO> topicOrSource = hits.stream()
                .filter(hit -> !isMainDocument(hit))
                .toList();
        if (!topicOrSource.isEmpty()) {
            return deduplicateByChunkId(topicOrSource);
        }
        return deduplicateByChunkId(hits);
    }

    private List<KnowledgeSearchResultVO> deduplicateByChunkId(List<KnowledgeSearchResultVO> hits) {
        Map<Long, KnowledgeSearchResultVO> unique = new LinkedHashMap<>();
        for (KnowledgeSearchResultVO hit : hits) {
            if (hit.getChunkId() != null) {
                unique.putIfAbsent(hit.getChunkId(), hit);
            }
        }
        return new ArrayList<>(unique.values());
    }

    private boolean isMainDocument(KnowledgeSearchResultVO hit) {
        return "main".equalsIgnoreCase(hit.getDocType());
    }

    private boolean isRouteHelp(KnowledgeSearchResultVO hit) {
        return "route_help".equalsIgnoreCase(hit.getDocType()) || "route_help".equalsIgnoreCase(hit.getTopicType());
    }

    private String buildText(KnowledgeSearchResultVO hit) {
        return String.join(" ",
                valueOrEmpty(hit.getDocTitle()),
                valueOrEmpty(hit.getHeadingPath()),
                valueOrEmpty(hit.getPolicyName()),
                valueOrEmpty(hit.getPolicyAliases()),
                valueOrEmpty(hit.getSnippet()));
    }

    private String valueOrEmpty(String text) {
        return text == null ? "" : text;
    }

    public enum AnswerMode {
        DIRECT_CONFIRMED,
        PARTIAL_CONFIRMED,
        INSUFFICIENT_EVIDENCE
    }

    public record ValidationResult(AnswerMode answerMode,
                                   List<KnowledgeSearchResultVO> primaryHits,
                                   String validationSummary,
                                   boolean shouldSuggestTopicDive,
                                   boolean mainDocumentOnly,
                                   boolean crossRegionBlocked,
                                   boolean policyMissed) {
        public Map<String, List<KnowledgeSearchResultVO>> groupByRegion() {
            Map<String, List<KnowledgeSearchResultVO>> grouped = new LinkedHashMap<>();
            for (KnowledgeSearchResultVO hit : primaryHits) {
                grouped.computeIfAbsent(hit.getRegionScope(), key -> new ArrayList<>()).add(hit);
            }
            return grouped;
        }

        public LinkedHashSet<String> chunkIds() {
            LinkedHashSet<String> ids = new LinkedHashSet<>();
            for (KnowledgeSearchResultVO hit : primaryHits) {
                if (hit.getChunkId() != null) {
                    ids.add(String.valueOf(hit.getChunkId()));
                }
            }
            return ids;
        }
    }
}
