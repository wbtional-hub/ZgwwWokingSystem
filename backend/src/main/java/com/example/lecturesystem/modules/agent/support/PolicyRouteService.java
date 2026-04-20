package com.example.lecturesystem.modules.agent.support;

import com.example.lecturesystem.modules.knowledge.support.PolicyKnowledgeSupport;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Service
public class PolicyRouteService {
    private static final List<String> XM_REGION_TERMS = List.of("\u53a6\u95e8", "\u53a6\u95e8\u5e02");
    private static final List<String> FJ_REGION_TERMS = List.of("\u798f\u5efa", "\u798f\u5efa\u7701");
    private static final List<String> COMPARE_TERMS = List.of(
            "\u533a\u522b",
            "\u5dee\u5f02",
            "\u5bf9\u6bd4",
            "\u4e0d\u540c",
            "\u4e0d\u4e00\u6837",
            "\u6709\u4ec0\u4e48\u533a\u522b"
    );
    private static final List<String> LIST_TERMS = List.of(
            "\u6709\u54ea\u4e9b",
            "\u5305\u542b\u54ea\u4e9b",
            "\u5305\u62ec\u54ea\u4e9b",
            "\u6e05\u5355",
            "\u540d\u5355",
            "\u5217\u4e3e"
    );

    public PolicyRouteIntent route(String question) {
        PolicyRouteIntent intent = new PolicyRouteIntent();
        intent.setQuestion(question);
        intent.setQuestionType(resolveQuestionType(question));
        intent.setFormalPolicyNames(PolicyKnowledgeSupport.extractFormalPolicyNames(question));
        intent.setTopicTags(PolicyKnowledgeSupport.detectTopicTags(question));
        intent.setRegionScope(resolveRegionScope(question, intent.getQuestionType()));
        intent.setRouteSkills(resolveRouteSkills(intent));
        intent.setOverviewList(intent.getQuestionType() == PolicyKnowledgeSupport.PolicyQuestionType.LIST
                && intent.getFormalPolicyNames().isEmpty()
                && intent.getTopicTags().isEmpty());
        intent.setTopicList(intent.getQuestionType() == PolicyKnowledgeSupport.PolicyQuestionType.LIST
                && !intent.getTopicTags().isEmpty());
        return intent;
    }

    private PolicyKnowledgeSupport.PolicyQuestionType resolveQuestionType(String question) {
        PolicyKnowledgeSupport.PolicyQuestionType detected = PolicyKnowledgeSupport.detectQuestionType(question);
        String normalized = normalize(question);
        if (normalized == null) {
            return detected;
        }
        if (isXmFjBoundaryCompareQuestion(normalized)) {
            return PolicyKnowledgeSupport.PolicyQuestionType.BOUNDARY;
        }
        if (isFjOverviewListQuestion(normalized)) {
            return PolicyKnowledgeSupport.PolicyQuestionType.LIST;
        }
        return detected;
    }

    private String resolveRegionScope(String question, PolicyKnowledgeSupport.PolicyQuestionType questionType) {
        String normalized = normalize(question);
        if (normalized == null) {
            return null;
        }
        if (isXmFjBoundaryCompareQuestion(normalized)) {
            return null;
        }
        if (isFjOverviewListQuestion(normalized)) {
            return "FJ";
        }
        boolean xm = containsAny(normalized, XM_REGION_TERMS);
        boolean fj = containsAny(normalized, FJ_REGION_TERMS);
        if (questionType == PolicyKnowledgeSupport.PolicyQuestionType.BOUNDARY && xm && fj) {
            return null;
        }
        if (xm && !fj) {
            return "XM";
        }
        if (fj && !xm) {
            return "FJ";
        }
        String helperScope = PolicyKnowledgeSupport.resolveRegionScope(question);
        return "UNKNOWN".equalsIgnoreCase(helperScope) ? null : helperScope;
    }

    private List<String> resolveRouteSkills(PolicyRouteIntent intent) {
        LinkedHashSet<String> skills = new LinkedHashSet<>();
        skills.add("POLICY_ROUTE_SKILL");
        if (isXmFjBoundaryCompareQuestion(intent.getQuestion())) {
            skills.add("XM_FJ_BOUNDARY_COMPARE");
            skills.add("TALENT_BOUNDARY_SKILL");
            return new ArrayList<>(skills);
        }
        if (isFjOverviewListQuestion(intent.getQuestion())) {
            skills.add("FJ_LIST");
        } else if (isXmOverviewListQuestion(intent.getQuestion())) {
            skills.add("XM_LIST");
        }
        if ("XM".equalsIgnoreCase(intent.getRegionScope())) {
            skills.add("XM_POLICY_SKILL");
        } else if ("FJ".equalsIgnoreCase(intent.getRegionScope())) {
            skills.add("FJ_POLICY_SKILL");
        }
        if (intent.getQuestionType() == PolicyKnowledgeSupport.PolicyQuestionType.BOUNDARY) {
            skills.add("TALENT_BOUNDARY_SKILL");
        }
        return new ArrayList<>(skills);
    }

    private boolean isXmFjBoundaryCompareQuestion(String question) {
        String normalized = normalize(question);
        if (normalized == null) {
            return false;
        }
        return containsAny(normalized, XM_REGION_TERMS)
                && containsAny(normalized, FJ_REGION_TERMS)
                && containsAny(normalized, COMPARE_TERMS);
    }

    private boolean isFjOverviewListQuestion(String question) {
        String normalized = normalize(question);
        if (normalized == null) {
            return false;
        }
        boolean hasFj = containsAny(normalized, FJ_REGION_TERMS);
        boolean isList = containsAny(normalized, LIST_TERMS);
        boolean talentPolicy = normalized.contains("\u4eba\u624d")
                && (normalized.contains("\u653f\u7b56") || normalized.contains("\u9879\u76ee"));
        return hasFj && isList && talentPolicy;
    }

    private boolean isXmOverviewListQuestion(String question) {
        String normalized = normalize(question);
        if (normalized == null) {
            return false;
        }
        boolean hasXm = containsAny(normalized, XM_REGION_TERMS);
        boolean isList = containsAny(normalized, LIST_TERMS);
        boolean talentPolicy = normalized.contains("\u4eba\u624d")
                && (normalized.contains("\u653f\u7b56") || normalized.contains("\u9879\u76ee"));
        return hasXm && isList && talentPolicy;
    }

    private boolean containsAny(String text, List<String> keywords) {
        if (text == null || keywords == null || keywords.isEmpty()) {
            return false;
        }
        for (String keyword : keywords) {
            if (keyword != null && !keyword.isBlank() && text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String text) {
        if (text == null) {
            return null;
        }
        String normalized = text.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
