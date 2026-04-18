package com.example.lecturesystem.modules.agent.support.policy;

import com.example.lecturesystem.modules.agent.support.KnowledgeCitationContext;
import com.example.lecturesystem.modules.knowledge.dto.KnowledgeSearchRequest;
import com.example.lecturesystem.modules.knowledge.mapper.KnowledgeChunkMapper;
import com.example.lecturesystem.modules.knowledge.vo.KnowledgeSearchResultVO;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class PolicyKnowledgeRetrievalTool {
    private static final String HIT_TYPE_OVERVIEW = "OVERVIEW";
    private static final String HIT_TYPE_RAW_POLICY = "RAW_POLICY";
    private static final String HIT_TYPE_CLEAN_SUMMARY = "CLEAN_SUMMARY";
    private static final String HIT_TYPE_CLEAN_EXPLANATION = "CLEAN_EXPLANATION";

    private final KnowledgeChunkMapper knowledgeChunkMapper;

    public PolicyKnowledgeRetrievalTool(KnowledgeChunkMapper knowledgeChunkMapper) {
        this.knowledgeChunkMapper = knowledgeChunkMapper;
    }

    public Result retrieve(Long baseId,
                           String question,
                           PolicyQuestionClassifier.Analysis analysis,
                           PolicySkillStrategy.Decision decision,
                           List<String> searchCandidates) {
        KnowledgeCitationContext context = new KnowledgeCitationContext();
        if (baseId == null || decision == null || decision.getTopN() <= 0) {
            return new Result(context, decision == null ? null : decision.getRetrievalStrategy(), List.of(), false, false);
        }
        List<KnowledgeSearchResultVO> rawHits = new ArrayList<>();
        if (analysis != null
                && PolicyQuestionClassifier.QUESTION_TYPE_COMPARE.equals(analysis.getQuestionType())
                && !decision.getCompareRegions().isEmpty()) {
            int perRegion = Math.max(2, decision.getTopN() / Math.max(1, decision.getCompareRegions().size()));
            for (String region : decision.getCompareRegions()) {
                rawHits.addAll(search(baseId, analysis, question, searchCandidates, region, perRegion));
            }
        } else {
            rawHits.addAll(search(baseId, analysis, question, searchCandidates, analysis == null ? null : analysis.getRegion(), Math.max(decision.getTopN(), 3)));
        }

        List<ScoredHit> scoredHits = new ArrayList<>();
        for (KnowledgeSearchResultVO item : deduplicate(rawHits)) {
            String hitType = resolveHitType(item);
            scoredHits.add(new ScoredHit(item, hitType, score(item, analysis, decision, hitType)));
        }
        scoredHits.sort(Comparator.comparingInt(ScoredHit::score)
                .thenComparing(hit -> hit.item().getChunkNo() == null ? Integer.MAX_VALUE : hit.item().getChunkNo()));

        List<String> topChunkTypes = new ArrayList<>();
        for (ScoredHit hit : scoredHits) {
            context.getChunks().add(hit.item());
            topChunkTypes.add(hit.hitType());
            if (context.getChunks().size() >= decision.getTopN()) {
                break;
            }
        }
        boolean mainlyExplanation = !topChunkTypes.isEmpty()
                && topChunkTypes.stream()
                .limit(Math.min(3, topChunkTypes.size()))
                .allMatch(type -> HIT_TYPE_CLEAN_EXPLANATION.equals(type) || HIT_TYPE_CLEAN_SUMMARY.equals(type));
        boolean hasRawPolicy = topChunkTypes.stream().anyMatch(HIT_TYPE_RAW_POLICY::equals);
        return new Result(context, decision.getRetrievalStrategy(), topChunkTypes, mainlyExplanation, hasRawPolicy);
    }

    private List<KnowledgeSearchResultVO> search(Long baseId,
                                                 PolicyQuestionClassifier.Analysis analysis,
                                                 String question,
                                                 List<String> candidates,
                                                 String regionPriority,
                                                 int topN) {
        Set<Long> seenChunkIds = new LinkedHashSet<>();
        List<KnowledgeSearchResultVO> hits = new ArrayList<>();
        int fetchLimit = Math.max(topN * 3, 9);
        for (String candidate : candidates == null ? List.<String>of() : candidates) {
            KnowledgeSearchRequest request = new KnowledgeSearchRequest();
            request.setBaseId(baseId);
            request.setKeywords(candidate);
            request.setQuestionType(analysis == null ? null : analysis.getQuestionType());
            request.setRegionPriority(normalize(regionPriority));
            request.setEffectiveOnly(Boolean.TRUE);
            request.setTopN(fetchLimit);
            List<KnowledgeSearchResultVO> results = knowledgeChunkMapper.search(request);
            if (results == null || results.isEmpty()) {
                continue;
            }
            for (KnowledgeSearchResultVO item : results) {
                Long chunkId = item.getChunkId();
                if (chunkId != null && !seenChunkIds.add(chunkId)) {
                    continue;
                }
                hits.add(item);
            }
            if (hits.size() >= fetchLimit) {
                break;
            }
        }
        if (hits.isEmpty() && normalize(question) != null) {
            KnowledgeSearchRequest request = new KnowledgeSearchRequest();
            request.setBaseId(baseId);
            request.setKeywords(question);
            request.setQuestionType(analysis == null ? null : analysis.getQuestionType());
            request.setRegionPriority(normalize(regionPriority));
            request.setEffectiveOnly(Boolean.TRUE);
            request.setTopN(fetchLimit);
            List<KnowledgeSearchResultVO> results = knowledgeChunkMapper.search(request);
            if (results != null) {
                hits.addAll(deduplicate(results));
            }
        }
        return hits;
    }

    private int score(KnowledgeSearchResultVO item,
                      PolicyQuestionClassifier.Analysis analysis,
                      PolicySkillStrategy.Decision decision,
                      String hitType) {
        int score = 200;
        String region = analysis == null ? null : analysis.getRegion();
        if (region != null) {
            if (containsRegion(item.getHeadingPath(), region)) {
                score -= 60;
            } else if (containsRegion(item.getSnippet(), region)) {
                score -= 45;
            } else if (containsRegion(item.getDocTitle(), region)) {
                score -= 30;
            }
        }
        if (decision.isPreferOverview()) {
            score += switch (hitType) {
                case HIT_TYPE_OVERVIEW -> -50;
                case HIT_TYPE_RAW_POLICY -> -20;
                case HIT_TYPE_CLEAN_SUMMARY -> 0;
                default -> 30;
            };
        } else if (decision.isPreferRawPolicy()) {
            score += switch (hitType) {
                case HIT_TYPE_RAW_POLICY -> -50;
                case HIT_TYPE_OVERVIEW -> -10;
                case HIT_TYPE_CLEAN_SUMMARY -> 10;
                default -> 40;
            };
        }
        if (analysis != null
                && PolicyQuestionClassifier.QUESTION_TYPE_COMPARE.equals(analysis.getQuestionType())
                && containsAny(joinText(item).toLowerCase(Locale.ROOT), "区别", "差异", "比较")) {
            score -= 10;
        }
        return score;
    }

    private String resolveHitType(KnowledgeSearchResultVO item) {
        String text = joinText(item).toLowerCase(Locale.ROOT);
        if (containsAny(text, "导入说明", "清洗导入", "整理原则", "来源说明", "结构特点", "解析说明")) {
            return HIT_TYPE_CLEAN_EXPLANATION;
        }
        if (containsAny(text, "总览", "总清单", "清单", "汇编", "概览")) {
            return HIT_TYPE_OVERVIEW;
        }
        if (containsAny(text, "办法", "实施意见", "通知", "方案", "认定条件", "申报条件", "支持措施", "第", "条")) {
            return HIT_TYPE_RAW_POLICY;
        }
        if (containsAny(text, "摘要", "总结", "汇总", "解读")) {
            return HIT_TYPE_CLEAN_SUMMARY;
        }
        return HIT_TYPE_RAW_POLICY;
    }

    private List<KnowledgeSearchResultVO> deduplicate(List<KnowledgeSearchResultVO> items) {
        Map<Long, KnowledgeSearchResultVO> unique = new LinkedHashMap<>();
        for (KnowledgeSearchResultVO item : items) {
            if (item == null) {
                continue;
            }
            Long chunkId = item.getChunkId();
            if (chunkId == null || !unique.containsKey(chunkId)) {
                unique.put(chunkId == null ? -System.nanoTime() : chunkId, item);
            }
        }
        return new ArrayList<>(unique.values());
    }

    private boolean containsRegion(String text, String region) {
        String normalized = normalize(text);
        return normalized != null && region != null && normalized.contains(region);
    }

    private boolean containsAny(String text, String... values) {
        if (text == null || values == null) {
            return false;
        }
        for (String value : values) {
            if (value != null && text.contains(value.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private String joinText(KnowledgeSearchResultVO item) {
        return String.join(
                " ",
                normalize(item.getDocTitle()) == null ? "" : item.getDocTitle(),
                normalize(item.getHeadingPath()) == null ? "" : item.getHeadingPath(),
                normalize(item.getSnippet()) == null ? "" : item.getSnippet()
        );
    }

    private String normalize(String text) {
        if (text == null) {
            return null;
        }
        String value = text.trim();
        return value.isEmpty() ? null : value;
    }

    private record ScoredHit(KnowledgeSearchResultVO item, String hitType, int score) {
    }

    public static final class Result {
        private final KnowledgeCitationContext context;
        private final String retrievalStrategy;
        private final List<String> topChunkTypes;
        private final boolean mainlyExplanation;
        private final boolean hasRawPolicy;

        public Result(KnowledgeCitationContext context,
                      String retrievalStrategy,
                      List<String> topChunkTypes,
                      boolean mainlyExplanation,
                      boolean hasRawPolicy) {
            this.context = context;
            this.retrievalStrategy = retrievalStrategy;
            this.topChunkTypes = topChunkTypes == null ? List.of() : List.copyOf(topChunkTypes);
            this.mainlyExplanation = mainlyExplanation;
            this.hasRawPolicy = hasRawPolicy;
        }

        public KnowledgeCitationContext getContext() {
            return context;
        }

        public String getRetrievalStrategy() {
            return retrievalStrategy;
        }

        public List<String> getTopChunkTypes() {
            return topChunkTypes;
        }

        public boolean isMainlyExplanation() {
            return mainlyExplanation;
        }

        public boolean hasRawPolicy() {
            return hasRawPolicy;
        }
    }
}
