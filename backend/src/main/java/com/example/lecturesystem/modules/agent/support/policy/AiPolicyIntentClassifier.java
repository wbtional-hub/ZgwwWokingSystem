package com.example.lecturesystem.modules.agent.support.policy;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AiPolicyIntentClassifier {
    private static final List<String> COMPARE_TERMS = List.of("区别", "差异", "对比", "不同", "比较");
    private static final List<String> LIST_TERMS = List.of("有哪些", "包括哪些", "包含哪些", "清单", "名单", "目录");
    private static final List<String> PROCESS_TERMS = List.of("怎么申请", "如何申请", "怎么申报", "如何申报", "申请流程", "申报流程", "流程", "步骤", "材料", "入口");
    private static final List<String> CONDITION_TERMS = List.of("条件", "要求", "资格", "对象", "认定", "适用");
    private static final List<String> BENEFIT_TERMS = List.of("补助", "补贴", "奖励", "待遇", "多少", "资助", "拨付");
    private static final List<String> RISK_TERMS = List.of("退出", "追回", "撤销", "终止", "管理期", "风险", "考核");
    private static final List<String> SERVICE_TERMS = List.of("服务保障", "子女教育", "医疗保障", "平台申报", "落户", "住房");

    public IntentMatch classify(AiPolicyQuestionNormalizer.NormalizedQuestion question,
                                AiPolicyRegionResolver.RegionMatch regionMatch,
                                AiPolicyResolver.PolicyMatch policyMatch) {
        if (question == null) {
            return new IntentMatch(IntentType.UNKNOWN, false);
        }
        String compact = question.compact();
        if (regionMatch != null && regionMatch.compareQuestion() && containsAny(compact, COMPARE_TERMS)) {
            return new IntentMatch(IntentType.COMPARE, true);
        }
        if (containsAny(compact, PROCESS_TERMS)) {
            return new IntentMatch(IntentType.PROCESS, true);
        }
        if (containsAny(compact, CONDITION_TERMS)) {
            return new IntentMatch(IntentType.CONDITION, true);
        }
        if (containsAny(compact, BENEFIT_TERMS)) {
            return new IntentMatch(IntentType.BENEFIT, true);
        }
        if (containsAny(compact, SERVICE_TERMS)) {
            return new IntentMatch(IntentType.SERVICE, true);
        }
        if (containsAny(compact, RISK_TERMS)) {
            return new IntentMatch(IntentType.RISK, true);
        }
        if (containsAny(compact, LIST_TERMS)) {
            return new IntentMatch(IntentType.LIST, true);
        }
        if (policyMatch != null && policyMatch.matched()) {
            return new IntentMatch(IntentType.TOPIC, true);
        }
        return new IntentMatch(IntentType.TOPIC, false);
    }

    private boolean containsAny(String text, List<String> terms) {
        if (text == null || terms == null) {
            return false;
        }
        for (String term : terms) {
            if (term != null && !term.isBlank() && text.contains(term)) {
                return true;
            }
        }
        return false;
    }

    public enum IntentType {
        LIST,
        PROCESS,
        CONDITION,
        BENEFIT,
        TOPIC,
        RISK,
        SERVICE,
        COMPARE,
        UNKNOWN
    }

    public record IntentMatch(IntentType intentType, boolean explicit) {
    }
}
