package com.example.lecturesystem.modules.agent.support.policy;

import com.example.lecturesystem.modules.agent.mapper.AiPolicyChunkMapper;
import com.example.lecturesystem.modules.knowledge.vo.KnowledgeSearchResultVO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiPolicyHybridRetrievalService {
    private final AiPolicyChunkMapper aiPolicyChunkMapper;

    public AiPolicyHybridRetrievalService(AiPolicyChunkMapper aiPolicyChunkMapper) {
        this.aiPolicyChunkMapper = aiPolicyChunkMapper;
    }

    public RetrievalResult retrieve(Long baseId,
                                    AiPolicyQuestionNormalizer.NormalizedQuestion question,
                                    AiPolicyRegionResolver.RegionMatch regionMatch,
                                    AiPolicyIntentClassifier.IntentMatch intentMatch,
                                    AiPolicyResolver.PolicyMatch policyMatch,
                                    AiPolicyRouterService.RoutePlan routePlan) {
        if (baseId == null || question == null || routePlan == null) {
            return new RetrievalResult(List.of(), List.of(), "missing_base_or_question");
        }
        Map<Long, RankedHit> rankedHits = new LinkedHashMap<>();
        List<String> searchQueries = new ArrayList<>();
        for (String keyword : routePlan.queryKeywords()) {
            if (keyword == null || keyword.isBlank()) {
                continue;
            }
            AiPolicyChunkSearchQuery query = new AiPolicyChunkSearchQuery();
            query.setBaseId(baseId);
            query.setKeywords(keyword);
            query.setRegionScopes(routePlan.regionScopes());
            query.setPolicyKeys(routePlan.policyKeys());
            query.setDocTypes(routePlan.docTypes());
            query.setTopicTypes(routePlan.topicTypes());
            query.setQuestionTypes(normalizeQuestionTypes(routePlan.questionTypes()));
            query.setEnabled(Boolean.TRUE);
            query.setTopN(12);

            List<KnowledgeSearchResultVO> hits;
            try {
                hits = aiPolicyChunkMapper.search(query);
            } catch (Exception ex) {
                return new RetrievalResult(List.of(), searchQueries, "search_error:" + ex.getClass().getSimpleName());
            }
            searchQueries.add(keyword);
            for (KnowledgeSearchResultVO hit : hits) {
                if (hit.getChunkId() == null) {
                    continue;
                }
                int score = scoreHit(hit, keyword, regionMatch, intentMatch, policyMatch, routePlan);
                RankedHit existing = rankedHits.get(hit.getChunkId());
                if (existing == null || score > existing.score()) {
                    rankedHits.put(hit.getChunkId(), new RankedHit(hit, score));
                }
            }
        }
        List<KnowledgeSearchResultVO> ordered = rankedHits.values().stream()
                .sorted(Comparator.comparingInt(RankedHit::score).reversed()
                        .thenComparing(item -> item.hit().getChunkNo() == null ? Integer.MAX_VALUE : item.hit().getChunkNo()))
                .map(RankedHit::hit)
                .limit(10)
                .toList();
        return new RetrievalResult(ordered, searchQueries, ordered.isEmpty() ? "no_hits" : "ok");
    }

    private List<String> normalizeQuestionTypes(List<String> questionTypes) {
        if (questionTypes == null || questionTypes.isEmpty()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (String value : questionTypes) {
            if (value == null || value.isBlank() || "compare".equalsIgnoreCase(value)) {
                continue;
            }
            result.add(value.toLowerCase());
        }
        return result;
    }

    private int scoreHit(KnowledgeSearchResultVO hit,
                         String keyword,
                         AiPolicyRegionResolver.RegionMatch regionMatch,
                         AiPolicyIntentClassifier.IntentMatch intentMatch,
                         AiPolicyResolver.PolicyMatch policyMatch,
                         AiPolicyRouterService.RoutePlan routePlan) {
        int score = 0;
        String text = buildText(hit);
        if (text.contains(keyword)) {
            score += 30;
        }
        if (!routePlan.regionScopes().isEmpty() && routePlan.regionScopes().contains(hit.getRegionScope())) {
            score += 36;
        } else if (regionMatch != null
                && regionMatch.scope() != AiPolicyRegionResolver.RegionScope.UNKNOWN
                && !routePlan.compareIntent()) {
            score -= 42;
        }
        if (routePlan.docTypes().contains(hit.getDocType())) {
            score += "topic".equalsIgnoreCase(hit.getDocType()) ? 24 : 10;
        }
        if (routePlan.topicTypes().contains(hit.getTopicType())) {
            score += 28;
        }
        if (policyMatch != null && policyMatch.matched()) {
            if (policyMatch.canonicalPolicyName() != null && text.contains(policyMatch.canonicalPolicyName())) {
                score += 20;
            }
            for (String alias : policyMatch.aliases()) {
                if (text.contains(alias)) {
                    score += 18;
                }
            }
        }
        if (routePlan.detailIntent() && "main".equalsIgnoreCase(hit.getDocType())) {
            score -= 14;
        }
        if ("route_help".equalsIgnoreCase(hit.getTopicType()) || "route_help".equalsIgnoreCase(hit.getDocType())) {
            score -= 10;
        }
        if (intentMatch.intentType() == AiPolicyIntentClassifier.IntentType.COMPARE
                && ("xiamen_city".equalsIgnoreCase(hit.getRegionScope()) || "fujian_province".equalsIgnoreCase(hit.getRegionScope()))) {
            score += 16;
        }
        return score;
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

    private record RankedHit(KnowledgeSearchResultVO hit, int score) {
    }

    public record RetrievalResult(List<KnowledgeSearchResultVO> hits,
                                  List<String> searchQueries,
                                  String retrievalSummary) {
    }
}
