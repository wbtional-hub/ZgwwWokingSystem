package com.example.lecturesystem.modules.agent.support.policy;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AiPolicyIntentClassifier {
    private static final List<String> DEPARTMENT_TERMS = List.of(
            "主管部门", "受理部门", "办理部门", "责任部门", "归口部门", "牵头部门",
            "哪个部门", "找哪个部门", "找谁办理", "谁负责", "由谁负责",
            "由谁执行", "谁执行", "执行部门", "谁审核", "由谁审核", "哪个部门审核", "审核部门",
            "谁牵头", "谁组织", "谁组织申报", "办理主体", "责任主体", "执行主体", "审核主体"
    );
    private static final List<String> DEPARTMENT_SUBJECT_TERMS = List.of("创业人才", "创新人才", "创新个人", "创新团队", "团队", "创业", "创新");
    private static final List<String> DEPARTMENT_ACTION_TERMS = List.of("谁", "哪个部门", "执行", "审核", "负责", "牵头", "组织");
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
        if (containsAny(compact, DEPARTMENT_TERMS)) {
            return new IntentMatch(IntentType.DEPARTMENT, true);
        }
        if (containsAny(compact, DEPARTMENT_SUBJECT_TERMS) && containsAny(compact, DEPARTMENT_ACTION_TERMS)) {
            return new IntentMatch(IntentType.DEPARTMENT, true);
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
        DEPARTMENT,
        TOPIC,
        RISK,
        SERVICE,
        COMPARE,
        UNKNOWN
    }

    public record IntentMatch(IntentType intentType, boolean explicit) {
    }
}
