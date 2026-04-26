package com.example.lecturesystem.modules.knowledge.service;

import com.example.lecturesystem.modules.agent.support.PolicyRouteIntent;
import com.example.lecturesystem.modules.knowledge.entity.AiPolicyCatalogEntity;
import com.example.lecturesystem.modules.knowledge.mapper.AiPolicyCatalogMapper;
import com.example.lecturesystem.modules.knowledge.mapper.KnowledgeChunkMapper;
import com.example.lecturesystem.modules.knowledge.support.PolicyCatalogBuilder;
import com.example.lecturesystem.modules.knowledge.support.PolicyKnowledgeSupport;
import com.example.lecturesystem.modules.knowledge.vo.KnowledgeSearchResultVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

@Service
public class PolicyCatalogService {
    private static final String SOURCE_SCENE_MOBILE_POLICY_CONSULTANT = "MOBILE_POLICY_CONSULTANT";
    private static final List<String> APPLY_FLOW_HINTS = List.of(
        "申请流程",
        "申报流程",
        "遴选与申请流程",
        "五步流程",
        "组织申报",
        "资格核查",
        "部门联审",
        "综合评审",
        "研究确认"
);

    private final AiPolicyCatalogMapper aiPolicyCatalogMapper;
    private final KnowledgeChunkMapper knowledgeChunkMapper;
    private final PolicyCatalogBuilder policyCatalogBuilder;

    public PolicyCatalogService(AiPolicyCatalogMapper aiPolicyCatalogMapper,
                                KnowledgeChunkMapper knowledgeChunkMapper,
                                PolicyCatalogBuilder policyCatalogBuilder) {
        this.aiPolicyCatalogMapper = aiPolicyCatalogMapper;
        this.knowledgeChunkMapper = knowledgeChunkMapper;
        this.policyCatalogBuilder = policyCatalogBuilder;
    }

    @Transactional
    public void rebuildBaseCatalog(Long baseId) {
        if (baseId == null) {
            return;
        }
        List<KnowledgeSearchResultVO> sourceChunks = knowledgeChunkMapper.queryCatalogSourceByBaseId(baseId);
        List<AiPolicyCatalogEntity> catalog = policyCatalogBuilder.build(baseId, sourceChunks);
        aiPolicyCatalogMapper.deleteByBaseId(baseId);
        if (!catalog.isEmpty()) {
            aiPolicyCatalogMapper.batchInsert(catalog);
        }
    }

    public CatalogAnswer buildCatalogAnswer(Long baseId, PolicyRouteIntent intent, String sourceScene) {
        if (baseId == null || intent == null || !SOURCE_SCENE_MOBILE_POLICY_CONSULTANT.equals(sourceScene)) {
            return CatalogAnswer.empty();
        }
        if (intent.getRouteSkills().contains("XM_FJ_BOUNDARY_COMPARE")) {
            return buildBoundaryAnswer(baseId, true);
        }
        if (intent.getQuestionType() == PolicyKnowledgeSupport.PolicyQuestionType.BOUNDARY) {
            return buildBoundaryAnswer(baseId, false);
        }
        if (intent.getQuestionType() == PolicyKnowledgeSupport.PolicyQuestionType.LIST) {
            return buildListAnswer(baseId, intent);
        }
        return CatalogAnswer.empty();
    }

    public List<String> resolveCandidateSearchTerms(Long baseId, PolicyRouteIntent intent, int limit) {
        if (baseId == null || intent == null) {
            return List.of();
        }
        LinkedHashSet<String> terms = new LinkedHashSet<>();
        List<AiPolicyCatalogEntity> candidates = findMatchedCatalog(baseId, intent);
        for (AiPolicyCatalogEntity item : candidates) {
            terms.add(item.getPolicyName());
            terms.addAll(PolicyKnowledgeSupport.splitCsv(item.getPolicyAliases()));
            if (terms.size() >= Math.max(limit, 1) * 3) {
                break;
            }
        }
        if (terms.isEmpty()) {
    terms.addAll(intent.getFormalPolicyNames());
    terms.addAll(intent.getTopicTags());
}

if (isDoubleHundredApplyFlowIntent(intent)) {
    terms.addAll(APPLY_FLOW_HINTS);
}

return terms.stream()
        .filter(this::hasText)
        .distinct()
        .limit(Math.max(limit, 1) * 3L)
        .toList();
    }

    public boolean hasCatalogData(Long baseId) {
        return baseId != null && !aiPolicyCatalogMapper.queryByBaseId(baseId, null, Boolean.TRUE).isEmpty();
    }

    private CatalogAnswer buildListAnswer(Long baseId, PolicyRouteIntent intent) {
        List<AiPolicyCatalogEntity> matched = findMatchedCatalog(baseId, intent);
        if (matched.isEmpty()) {
            if (isStrictFjList(intent)) {
                return buildInsufficientCatalogAnswer("FJ");
            }
            return CatalogAnswer.empty();
        }
        LinkedHashMap<String, List<AiPolicyCatalogEntity>> grouped = groupCatalogItems(matched, intent);
        if (grouped.isEmpty()) {
            if (isStrictFjList(intent)) {
                return buildInsufficientCatalogAnswer("FJ");
            }
            return CatalogAnswer.empty();
        }
        String regionLabel = resolveRegionLabel(resolveCatalogRegionScope(intent));
        StringBuilder builder = new StringBuilder();
        builder.append("结论：");
        if (intent.isTopicList() && !intent.getTopicTags().isEmpty()) {
            builder.append(regionLabel == null ? "当前知识库目录层命中的专题政策如下。" : regionLabel + "当前知识库目录层命中的专题政策如下。");
        } else {
            builder.append(regionLabel == null ? "当前知识库目录层命中的主要人才政策/项目如下。" : regionLabel + "当前知识库目录层命中的主要人才政策/项目如下。");
        }
        for (Map.Entry<String, List<AiPolicyCatalogEntity>> entry : grouped.entrySet()) {
            builder.append("\n").append(entry.getKey()).append("：\n");
            List<AiPolicyCatalogEntity> items = entry.getValue();
            for (int i = 0; i < items.size(); i++) {
                AiPolicyCatalogEntity item = items.get(i);
                builder.append(i + 1)
                        .append(". ")
                        .append(item.getPolicyName())
                        .append("：")
                        .append(valueOrDefault(item.getShortSummary(), "属于当前知识库已沉淀的目录项。"))
                        .append("\n");
            }
        }
        builder.append("以下为当前知识库已命中的主要政策/项目，不代表完整官方清单");
        return new CatalogAnswer(builder.toString().trim(), collectCatalogCitations(matched));
    }

    private CatalogAnswer buildBoundaryAnswer(Long baseId, boolean forceCompareAnswer) {
        List<AiPolicyCatalogEntity> xm = sortCatalogItems(aiPolicyCatalogMapper.queryByBaseId(baseId, "XM", Boolean.TRUE));
        List<AiPolicyCatalogEntity> fj = sortCatalogItems(aiPolicyCatalogMapper.queryByBaseId(baseId, "FJ", Boolean.TRUE));
        if (!forceCompareAnswer && xm.isEmpty() && fj.isEmpty()) {
            return CatalogAnswer.empty();
        }
        StringBuilder builder = new StringBuilder();
        builder.append("结论：厦门市级人才政策和福建省级人才政策属于不同层级，应分别检索后再做对比。\n");
        appendComparisonBlock(builder, "厦门市级代表政策", xm, "当前厦门目录项不足，建议补充厦门市级目录文档。");
        appendComparisonBlock(builder, "福建省级代表政策", fj, "当前福建目录项不足，建议补充福建省级目录文档。");
        builder.append("\n对比要点：\n");
        builder.append("1. 政策层级：厦门侧重市级落地项目和配套措施，福建侧重省级统筹政策、专项计划和申报安排。\n");
        builder.append("2. 适用对象：厦门更偏向本市重点产业、用人单位和落地人才，福建更偏向全省范围的认定对象和省级项目申报主体。\n");
        builder.append("3. 支持方向：厦门常见于产业项目、住房和服务保障等落地支持，福建更多覆盖省级认定、专项计划和全省统筹支持。\n");
        builder.append("4. 办理层级：厦门问题优先查看市级主管部门和市级项目规则，福建问题优先查看省级目录、申报通知和省级认定要求。");
        List<String> citations = new ArrayList<>(collectCatalogCitations(xm));
        citations.addAll(collectCatalogCitations(fj));
        return new CatalogAnswer(builder.toString().trim(), citations.stream().distinct().toList());
    }

    private void appendComparisonBlock(StringBuilder builder, String title, List<AiPolicyCatalogEntity> items, String emptyMessage) {
        builder.append("\n").append(title).append("：\n");
        if (items == null || items.isEmpty()) {
            builder.append(emptyMessage).append("\n");
            return;
        }
        appendRepresentativeItems(builder, items, 4);
    }

    private void appendRepresentativeItems(StringBuilder builder, List<AiPolicyCatalogEntity> items, int limit) {
        List<AiPolicyCatalogEntity> selected = sortCatalogItems(items).stream().limit(limit).toList();
        for (int i = 0; i < selected.size(); i++) {
            AiPolicyCatalogEntity item = selected.get(i);
            builder.append(i + 1)
                    .append(". ")
                    .append(item.getPolicyName())
                    .append("：")
                    .append(valueOrDefault(item.getShortSummary(), "属于当前知识库已沉淀的目录项。"))
                    .append("\n");
        }
    }

    private List<AiPolicyCatalogEntity> findMatchedCatalog(Long baseId, PolicyRouteIntent intent) {
        String regionScope = resolveCatalogRegionScope(intent);
        List<AiPolicyCatalogEntity> catalog = aiPolicyCatalogMapper.queryByBaseId(baseId, regionScope, Boolean.TRUE);
        if (catalog.isEmpty()) {
            return List.of();
        }
        List<AiPolicyCatalogEntity> matched = new ArrayList<>();
        for (AiPolicyCatalogEntity item : catalog) {
            if (!matchesRoute(item, intent)) {
                continue;
            }
            if (hasText(regionScope) && !matchesRegion(item, regionScope)) {
                continue;
            }
            matched.add(item);
        }
        if (matched.isEmpty() && !hasText(regionScope) && intent.getQuestionType() == PolicyKnowledgeSupport.PolicyQuestionType.LIST) {
            for (AiPolicyCatalogEntity item : catalog) {
                if (matchesRoute(item, intent)) {
                    matched.add(item);
                }
            }
        }
        return sortCatalogItems(matched);
    }

    private boolean matchesRoute(AiPolicyCatalogEntity item, PolicyRouteIntent intent) {
        String strictRegion = resolveCatalogRegionScope(intent);
        if (hasText(strictRegion) && !matchesRegion(item, strictRegion)) {
            return false;
        }
        if (!intent.getFormalPolicyNames().isEmpty()) {
            for (String policyName : intent.getFormalPolicyNames()) {
                String canonical = PolicyKnowledgeSupport.canonicalizePolicyName(policyName);
                if (canonical != null && canonical.equalsIgnoreCase(PolicyKnowledgeSupport.canonicalizePolicyName(item.getPolicyName()))) {
                    return true;
                }
                for (String alias : PolicyKnowledgeSupport.splitCsv(item.getPolicyAliases())) {
                    if (PolicyKnowledgeSupport.containsKeyword(alias, policyName) || PolicyKnowledgeSupport.containsKeyword(policyName, alias)) {
                        return true;
                    }
                }
            }
            return false;
        }
        if (!intent.getTopicTags().isEmpty()) {
            List<String> itemTags = PolicyKnowledgeSupport.splitCsv(item.getTopicTags());
            for (String tag : intent.getTopicTags()) {
                if (itemTags.stream().anyMatch(itemTag -> itemTag.equalsIgnoreCase(tag))) {
                    return true;
                }
            }
            return false;
        }
        return true;
    }

    private String resolveCatalogRegionScope(PolicyRouteIntent intent) {
        if (intent == null) {
            return null;
        }
        if (intent.getRouteSkills().contains("XM_FJ_BOUNDARY_COMPARE")) {
            return null;
        }
        if (intent.getRouteSkills().contains("FJ_LIST")) {
            return "FJ";
        }
        if (intent.getRouteSkills().contains("XM_LIST")) {
            return "XM";
        }
        return intent.getQuestionType() == PolicyKnowledgeSupport.PolicyQuestionType.BOUNDARY ? null : intent.getRegionScope();
    }

    private boolean isStrictFjList(PolicyRouteIntent intent) {
        return intent != null
                && intent.getQuestionType() == PolicyKnowledgeSupport.PolicyQuestionType.LIST
                && "FJ".equalsIgnoreCase(resolveCatalogRegionScope(intent));
    }

    private CatalogAnswer buildInsufficientCatalogAnswer(String regionScope) {
        if ("FJ".equalsIgnoreCase(regionScope)) {
            return new CatalogAnswer("当前福建目录项不足，建议补充福建省级目录文档。", List.of());
        }
        if ("XM".equalsIgnoreCase(regionScope)) {
            return new CatalogAnswer("当前厦门目录项不足，建议补充厦门市级目录文档。", List.of());
        }
        return CatalogAnswer.empty();
    }

    private boolean matchesRegion(AiPolicyCatalogEntity item, String regionScope) {
        if (!hasText(regionScope)) {
            return true;
        }
        return hasText(item.getRegionScope()) && item.getRegionScope().equalsIgnoreCase(regionScope);
    }

    private LinkedHashMap<String, List<AiPolicyCatalogEntity>> groupCatalogItems(List<AiPolicyCatalogEntity> items, PolicyRouteIntent intent) {
        LinkedHashMap<String, List<AiPolicyCatalogEntity>> grouped = new LinkedHashMap<>();
        for (String group : resolveGroupOrder(intent)) {
            List<AiPolicyCatalogEntity> matched = items.stream()
                    .filter(item -> group.equals(item.getPolicyGroup()))
                    .limit(intent.isTopicList() ? 8 : 6)
                    .toList();
            if (!matched.isEmpty()) {
                grouped.put(group, matched);
            }
        }
        if (grouped.isEmpty() && !items.isEmpty()) {
            grouped.put(intent.isTopicList() ? "专题政策" : "政策清单", items.stream().limit(8).toList());
        }
        return grouped;
    }

    private List<String> resolveGroupOrder(PolicyRouteIntent intent) {
        if ("FJ".equalsIgnoreCase(resolveCatalogRegionScope(intent))) {
            return List.of("省级主干政策", "近期申报", "专项支持", "四大经济", "博士后", "台湾人才", "百人计划");
        }
        if (intent.isTopicList() && intent.getTopicTags().contains("housing")) {
            return List.of("住房", "服务保障", "公共专项");
        }
        return List.of("统领政策", "重点产业项目", "公共专项", "住房", "博士后", "AI", "服务保障", "台湾人才");
    }

    private List<String> collectCatalogCitations(List<AiPolicyCatalogEntity> items) {
        LinkedHashSet<String> citations = new LinkedHashSet<>();
        for (AiPolicyCatalogEntity item : items) {
            if (hasText(item.getSourceDocName())) {
                citations.add(item.getSourceDocName());
            }
        }
        return new ArrayList<>(citations);
    }

    private List<AiPolicyCatalogEntity> sortCatalogItems(List<AiPolicyCatalogEntity> items) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        return items.stream()
                .sorted(Comparator.comparing(AiPolicyCatalogEntity::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(AiPolicyCatalogEntity::getPolicyName))
                .toList();
    }

    private String resolveRegionLabel(String regionScope) {
        if ("XM".equalsIgnoreCase(regionScope)) {
            return "厦门市";
        }
        if ("FJ".equalsIgnoreCase(regionScope)) {
            return "福建省";
        }
        return null;
    }

    private boolean hasText(String text) {
        return text != null && !text.trim().isEmpty();
    }

    private boolean isDoubleHundredApplyFlowIntent(PolicyRouteIntent intent) {
    if (intent == null || intent.getQuestionType() != PolicyKnowledgeSupport.PolicyQuestionType.PROCESS) {
        return false;
    }
    return containsAnyText(intent.getFormalPolicyNames(),
            "双百计划",
            "厦门市引进高层次创新创业人才“双百计划”实施意见",
            "创新团队",
            "创业人才");
}

private boolean containsAnyText(List<String> values, String... targets) {
    if (values == null || values.isEmpty() || targets == null || targets.length == 0) {
        return false;
    }
    for (String value : values) {
        if (!hasText(value)) {
            continue;
        }
        for (String target : targets) {
            if (!hasText(target)) {
                continue;
            }
            if (value.contains(target) || target.contains(value)) {
                return true;
            }
        }
    }
    return false;
}

    private String valueOrDefault(String value, String defaultValue) {
        return hasText(value) ? value : defaultValue;
    }

    public record CatalogAnswer(String answer, List<String> citations) {
        public static CatalogAnswer empty() {
            return new CatalogAnswer(null, List.of());
        }

        public boolean hasAnswer() {
            return answer != null && !answer.isBlank();
        }
    }
}
