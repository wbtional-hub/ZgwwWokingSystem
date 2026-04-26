package com.example.lecturesystem.modules.agent.support.policy;

import com.example.lecturesystem.modules.agent.entity.AiPolicyAliasEntity;
import com.example.lecturesystem.modules.agent.mapper.AiPolicyAliasMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

@Service
public class AiPolicyResolver {
    private final AiPolicyAliasMapper aiPolicyAliasMapper;

    public AiPolicyResolver(AiPolicyAliasMapper aiPolicyAliasMapper) {
        this.aiPolicyAliasMapper = aiPolicyAliasMapper;
    }

    public PolicyMatch resolve(Long baseId,
                               AiPolicyQuestionNormalizer.NormalizedQuestion question,
                               AiPolicyRegionResolver.RegionMatch regionMatch) {
        if (baseId == null || question == null) {
            return PolicyMatch.unmatched();
        }
        List<AiPolicyAliasEntity> aliases;
        try {
            aliases = aiPolicyAliasMapper.queryEnabledByBaseId(baseId);
        } catch (Exception ex) {
            return PolicyMatch.unmatched();
        }
        if (aliases == null || aliases.isEmpty()) {
            return PolicyMatch.unmatched();
        }
        Map<String, Integer> scoreMap = new LinkedHashMap<>();
        Map<String, String> regionMap = new LinkedHashMap<>();
        Map<String, LinkedHashSet<String>> matchedAliasMap = new LinkedHashMap<>();
        for (AiPolicyAliasEntity alias : aliases) {
            if (alias.getAlias() == null || alias.getPolicyKey() == null) {
                continue;
            }
            if (question.compact().contains(alias.getAlias())) {
                int score = Math.max(alias.getPriority() == null ? 0 : alias.getPriority(), alias.getAlias().length() * 10);
                scoreMap.merge(alias.getPolicyKey(), score, Integer::sum);
                regionMap.putIfAbsent(alias.getPolicyKey(), alias.getRegionScope());
                matchedAliasMap.computeIfAbsent(alias.getPolicyKey(), key -> new LinkedHashSet<>()).add(alias.getAlias());
            }
        }
        String bestPolicyKey = scoreMap.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
        if (bestPolicyKey == null) {
            return PolicyMatch.unmatched();
        }
        List<String> matchedAliases = new ArrayList<>(matchedAliasMap.getOrDefault(bestPolicyKey, new LinkedHashSet<>()));
        List<String> formalNames = matchedAliases.stream()
                .sorted(Comparator.comparingInt(String::length).reversed())
                .toList();
        String preferredRegion = regionMap.get(bestPolicyKey);
        if (regionMatch != null && regionMatch.scope() != AiPolicyRegionResolver.RegionScope.UNKNOWN) {
            preferredRegion = regionMatch.scope().getCode();
        }
        return new PolicyMatch(true, bestPolicyKey, resolveCanonicalPolicyName(bestPolicyKey), formalNames, matchedAliases, preferredRegion);
    }

    private String resolveCanonicalPolicyName(String policyKey) {
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
            default -> policyKey;
        };
    }

    public record PolicyMatch(boolean matched,
                              String policyKey,
                              String canonicalPolicyName,
                              List<String> formalPolicyNames,
                              List<String> aliases,
                              String preferredRegionScope) {
        public static PolicyMatch unmatched() {
            return new PolicyMatch(false, null, null, List.of(), List.of(), null);
        }
    }
}
