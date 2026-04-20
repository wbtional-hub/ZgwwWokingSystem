package com.example.lecturesystem.modules.knowledge.support;

import com.example.lecturesystem.modules.knowledge.entity.AiPolicyCatalogEntity;
import com.example.lecturesystem.modules.knowledge.vo.KnowledgeSearchResultVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class PolicyCatalogBuilder {
    private static final Logger log = LoggerFactory.getLogger(PolicyCatalogBuilder.class);

    public List<AiPolicyCatalogEntity> build(Long baseId, List<KnowledgeSearchResultVO> sourceChunks) {
        if (baseId == null || sourceChunks == null || sourceChunks.isEmpty()) {
            return List.of();
        }
        LinkedHashMap<String, CatalogAccumulator> accumulators = new LinkedHashMap<>();
        int eligibleCount = 0;
        for (KnowledgeSearchResultVO item : sourceChunks) {
            if (!isEligibleSource(item)) {
                continue;
            }
            eligibleCount++;
            String regionScope = normalizeRegion(item.getRegionScope());
            if (regionScope == null) {
                continue;
            }
            for (String policyName : extractCatalogPolicyNames(item)) {
                String canonicalName = PolicyKnowledgeSupport.canonicalizePolicyName(policyName);
                if (canonicalName == null || PolicyKnowledgeSupport.isInvalidCatalogName(canonicalName)) {
                    continue;
                }
                if (!PolicyKnowledgeSupport.isCatalogRegionConsistent(regionScope, canonicalName, item.getPolicyAliases())) {
                    continue;
                }
                String key = baseId + "|" + regionScope + "|" + canonicalName.toLowerCase(Locale.ROOT);
                CatalogAccumulator accumulator = accumulators.computeIfAbsent(key,
                        ignored -> new CatalogAccumulator(baseId, regionScope, canonicalName));
                accumulator.absorb(item);
            }
        }
        List<AiPolicyCatalogEntity> result = new ArrayList<>();
        int fallbackPolicyGroupCount = 0;
        for (CatalogAccumulator accumulator : accumulators.values()) {
            AiPolicyCatalogEntity entity = accumulator.toEntity();
            if (entity != null) {
                result.add(entity);
                if (accumulator.usedFallbackPolicyGroup()) {
                    fallbackPolicyGroupCount++;
                }
            }
        }
        result.sort(Comparator.comparing(AiPolicyCatalogEntity::getRegionScope)
                .thenComparing(item -> item.getSortOrder() == null ? Integer.MAX_VALUE : item.getSortOrder())
                .thenComparing(AiPolicyCatalogEntity::getPolicyName));
        if (log.isDebugEnabled()) {
            log.debug("Policy catalog build baseId={}, sourceChunks={}, eligible={}, accumulators={}, finalCatalog={}, fallbackPolicyGroup={}",
                    baseId, sourceChunks.size(), eligibleCount, accumulators.size(), result.size(), fallbackPolicyGroupCount);
        }
        return result;
    }

    private boolean isEligibleSource(KnowledgeSearchResultVO item) {
        if (item == null || Boolean.FALSE.equals(item.getSearchable())) {
            return false;
        }
        String docType = normalize(item.getDocType());
        if (!"main".equalsIgnoreCase(docType) && !"topic".equalsIgnoreCase(docType)) {
            return false;
        }
        return !extractCatalogPolicyNames(item).isEmpty();
    }

    private List<String> extractCatalogPolicyNames(KnowledgeSearchResultVO item) {
        LinkedHashSet<String> names = new LinkedHashSet<>();
        names.addAll(PolicyKnowledgeSupport.extractCatalogPolicyNames(
                item.getPolicyName(),
                item.getPolicyAliases(),
                item.getHeadingPath(),
                item.getChapterTitle(),
                item.getSectionTitle(),
                item.getSnippet(),
                item.getDocTitle()
        ));
        String direct = PolicyKnowledgeSupport.canonicalizePolicyName(item.getPolicyName());
        if (direct != null && !PolicyKnowledgeSupport.isInvalidCatalogName(direct)) {
            names.add(direct);
        }
        return new ArrayList<>(names);
    }

    private String normalizeRegion(String regionScope) {
        String normalized = normalize(regionScope);
        if ("XM".equalsIgnoreCase(normalized) || "FJ".equalsIgnoreCase(normalized)) {
            return normalized.toUpperCase(Locale.ROOT);
        }
        return null;
    }

    private String normalize(String text) {
        if (text == null) {
            return null;
        }
        String value = text.trim();
        return value.isEmpty() ? null : value;
    }

    private String value(String text) {
        return text == null ? "" : text;
    }

    private final class CatalogAccumulator {
        private final Long baseId;
        private final String regionScope;
        private final String policyName;
        private final LinkedHashSet<String> aliases = new LinkedHashSet<>();
        private final LinkedHashSet<String> topicTags = new LinkedHashSet<>();
        private final LinkedHashSet<Long> chunkIds = new LinkedHashSet<>();
        private final LinkedHashSet<String> sourceDocNames = new LinkedHashSet<>();
        private String policyNo;
        private String policyGroup;
        private String policyType;
        private String shortSummary;
        private String sourceDocType;
        private boolean searchable = true;
        private boolean fallbackPolicyGroupApplied = false;
        private int sortOrder = Integer.MAX_VALUE;

        private CatalogAccumulator(Long baseId, String regionScope, String policyName) {
            this.baseId = baseId;
            this.regionScope = regionScope;
            this.policyName = policyName;
            aliases.addAll(PolicyKnowledgeSupport.resolvePolicySearchTerms(policyName));
        }

        private void absorb(KnowledgeSearchResultVO item) {
            aliases.addAll(PolicyKnowledgeSupport.splitCsv(item.getPolicyAliases()));
            aliases.addAll(PolicyKnowledgeSupport.resolvePolicySearchTerms(policyName));
            List<String> currentTopicTags = PolicyKnowledgeSupport.detectTopicTags(
                    policyName,
                    item.getHeadingPath(),
                    item.getChapterTitle(),
                    item.getSectionTitle(),
                    item.getSnippet(),
                    item.getDocTitle()
            );
            topicTags.addAll(currentTopicTags);
            List<String> summaryTopicTags = PolicyKnowledgeSupport.detectTopicTags(
                    policyName,
                    item.getHeadingPath(),
                    item.getChapterTitle(),
                    item.getSectionTitle(),
                    item.getSnippet()
            );
            if (normalize(item.getPolicyNo()) != null && policyNo == null) {
                policyNo = item.getPolicyNo().trim();
            }
            if (item.getChunkId() != null) {
                chunkIds.add(item.getChunkId());
            }
            if (normalize(item.getDocTitle()) != null) {
                sourceDocNames.add(item.getDocTitle().trim());
            }
            String nextGroup = PolicyKnowledgeSupport.detectCatalogPolicyGroup(
                    regionScope,
                    policyName,
                    String.join(" ", value(item.getHeadingPath()), value(item.getChapterTitle()), value(item.getSectionTitle())),
                    String.join(",", summaryTopicTags)
            );
            if (policyGroup == null || preferGroup(nextGroup, policyGroup)) {
                policyGroup = nextGroup;
            }
            String nextPolicyType = detectPolicyType(item);
            if (policyType == null || preferPolicyType(nextPolicyType, policyType)) {
                policyType = nextPolicyType;
            }
            String fallbackText = cleanSummary(item.getSnippet());
            String nextSummary = PolicyKnowledgeSupport.buildCatalogShortSummary(
                    policyName,
                    String.join(",", summaryTopicTags),
                    fallbackText,
                    item.getDocTitle(),
                    item.getHeadingPath(),
                    item.getChapterTitle(),
                    item.getSectionTitle(),
                    item.getSnippet()
            );
            if (shortSummary == null || preferSummary(nextSummary, shortSummary)) {
                shortSummary = nextSummary;
            }
            if (sourceDocType == null || preferDocType(item.getDocType(), sourceDocType)) {
                sourceDocType = normalize(item.getDocType());
            }
            searchable = searchable && !Boolean.FALSE.equals(item.getSearchable());
            sortOrder = Math.min(sortOrder, resolveSortOrder(item, nextGroup));
        }

        private AiPolicyCatalogEntity toEntity() {
            if (chunkIds.isEmpty()) {
                return null;
            }
            String finalPolicyGroup = normalize(policyGroup);
            if (finalPolicyGroup == null) {
                finalPolicyGroup = defaultPolicyGroup();
                fallbackPolicyGroupApplied = true;
            }
            AiPolicyCatalogEntity entity = new AiPolicyCatalogEntity();
            entity.setBaseId(baseId);
            entity.setRegionScope(regionScope);
            entity.setPolicyGroup(finalPolicyGroup);
            entity.setPolicyName(policyName);
            entity.setPolicyAliases(aliases.stream()
                    .filter(alias -> normalize(alias) != null && !alias.equalsIgnoreCase(policyName))
                    .filter(alias -> !PolicyKnowledgeSupport.isInvalidCatalogName(alias))
                    .filter(alias -> PolicyKnowledgeSupport.isCatalogRegionConsistent(regionScope, alias, null))
                    .collect(Collectors.joining(",")));
            entity.setPolicyNo(policyNo);
            entity.setPolicyType(policyType == null ? "topic" : policyType);
            entity.setTopicTags(String.join(",", topicTags));
            entity.setShortSummary(shortSummary == null ? "属于当前知识库已沉淀的主要政策/项目目录项。" : shortSummary);
            entity.setSourceDocType(sourceDocType == null ? "topic" : sourceDocType);
            entity.setSourceDocName(sourceDocNames.isEmpty() ? null : sourceDocNames.iterator().next());
            entity.setSourceChunkIds(chunkIds.stream().map(String::valueOf).collect(Collectors.joining(",")));
            entity.setSearchable(searchable);
            entity.setSortOrder(sortOrder == Integer.MAX_VALUE ? 9999 : sortOrder);
            entity.setCreateTime(LocalDateTime.now());
            entity.setUpdateTime(LocalDateTime.now());
            return entity;
        }

        private boolean usedFallbackPolicyGroup() {
            return fallbackPolicyGroupApplied;
        }

        private String defaultPolicyGroup() {
            if ("FJ".equalsIgnoreCase(regionScope)) {
                return "省级主干政策";
            }
            if ("XM".equalsIgnoreCase(regionScope)) {
                return "统领政策";
            }
            return "政策清单";
        }

        private String detectPolicyType(KnowledgeSearchResultVO item) {
            String topicType = normalize(item.getTopicType());
            if ("list".equalsIgnoreCase(topicType) || "main".equalsIgnoreCase(normalize(item.getDocType()))) {
                return "overview";
            }
            if ("process".equalsIgnoreCase(topicType)) {
                return "process";
            }
            if ("condition".equalsIgnoreCase(topicType)) {
                return "condition";
            }
            if ("benefit".equalsIgnoreCase(topicType)) {
                return "benefit";
            }
            if ("faq".equalsIgnoreCase(topicType)) {
                return "faq";
            }
            if ("routing".equalsIgnoreCase(topicType)) {
                return "boundary";
            }
            return "topic";
        }

        private int resolveSortOrder(KnowledgeSearchResultVO item, String group) {
            int groupScore = switch (normalize(group)) {
                case "统领政策", "省级主干政策" -> 100;
                case "重点产业项目", "近期申报", "专项支持" -> 200;
                case "公共专项", "住房", "博士后", "AI", "台湾人才", "四大经济", "百人计划" -> 300;
                default -> 500;
            };
            int docScore = "main".equalsIgnoreCase(normalize(item.getDocType())) ? 0 : 50;
            int chunkScore = item.getChunkNo() == null ? 999 : item.getChunkNo();
            return groupScore + docScore + chunkScore;
        }

        private boolean preferGroup(String left, String right) {
            return groupRank(left) < groupRank(right);
        }

        private int groupRank(String group) {
            return switch (normalize(group)) {
                case "统领政策", "省级主干政策" -> 1;
                case "重点产业项目", "近期申报", "专项支持" -> 2;
                case "公共专项", "住房", "博士后", "AI", "台湾人才", "四大经济", "百人计划" -> 3;
                default -> 9;
            };
        }

        private boolean preferPolicyType(String left, String right) {
            return policyTypeRank(left) < policyTypeRank(right);
        }

        private int policyTypeRank(String value) {
            return switch (normalize(value)) {
                case "overview" -> 1;
                case "topic" -> 2;
                case "process", "condition", "benefit" -> 3;
                case "faq", "boundary" -> 4;
                default -> 9;
            };
        }

        private boolean preferDocType(String left, String right) {
            return "main".equalsIgnoreCase(normalize(left)) && !"main".equalsIgnoreCase(normalize(right));
        }

        private boolean preferSummary(String left, String right) {
            return normalize(left) != null && (normalize(right) == null || left.length() < right.length());
        }

        private String cleanSummary(String text) {
            return PolicyKnowledgeSupport.sanitizeCatalogSummary(text);
        }
    }
}
