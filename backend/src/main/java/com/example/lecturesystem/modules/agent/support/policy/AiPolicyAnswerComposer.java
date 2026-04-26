package com.example.lecturesystem.modules.agent.support.policy;

import com.example.lecturesystem.modules.agent.support.policy.AiPolicyIntentClassifier.IntentType;
import com.example.lecturesystem.modules.knowledge.vo.KnowledgeSearchResultVO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

@Service
public class AiPolicyAnswerComposer {
    public String compose(AiPolicyQuestionNormalizer.NormalizedQuestion question,
                          AiPolicyRegionResolver.RegionMatch regionMatch,
                          AiPolicyIntentClassifier.IntentMatch intentMatch,
                          AiPolicyResolver.PolicyMatch policyMatch,
                          AiPolicyRouterService.RoutePlan routePlan,
                          AiPolicyEvidenceValidator.ValidationResult validationResult) {
        if (validationResult.answerMode() == AiPolicyEvidenceValidator.AnswerMode.INSUFFICIENT_EVIDENCE) {
            return buildInsufficientAnswer(regionMatch, intentMatch, policyMatch, routePlan, validationResult);
        }
        return switch (intentMatch.intentType()) {
            case LIST -> buildListAnswer(regionMatch, validationResult);
            case PROCESS -> buildProcessAnswer(policyMatch, validationResult);
            case CONDITION -> buildPointAnswer("结论：当前知识库已命中可确认的申报条件或适用对象。", validationResult, List.of("条件", "对象", "资格", "认定", "要求"));
            case BENEFIT -> buildPointAnswer("结论：当前知识库已命中可确认的支持待遇或资金安排。", validationResult, List.of("补助", "补贴", "奖励", "资助", "拨付", "兑付"));
            case SERVICE -> buildPointAnswer("结论：当前知识库已命中可确认的服务保障内容。", validationResult, List.of("服务保障", "子女教育", "医疗保障", "平台申报", "落户", "住房"));
            case RISK -> buildPointAnswer("结论：当前知识库已命中可确认的管理期或风险约束。", validationResult, List.of("管理期", "退出", "追回", "终止", "考核"));
            case COMPARE -> buildCompareAnswer(validationResult);
            default -> buildTopicAnswer(policyMatch, validationResult);
        };
    }

    private String buildListAnswer(AiPolicyRegionResolver.RegionMatch regionMatch,
                                   AiPolicyEvidenceValidator.ValidationResult validationResult) {
        StringBuilder builder = new StringBuilder();
        builder.append("结论：");
        if (regionMatch != null && regionMatch.scope() != AiPolicyRegionResolver.RegionScope.UNKNOWN) {
            builder.append(regionMatch.scope().getLabel()).append("当前命中的主要政策/专题如下。\n");
        } else {
            builder.append("当前命中的主要政策/专题如下。\n");
        }
        appendPolicyList(builder, validationResult.primaryHits());
        appendBoundary(builder, validationResult.validationSummary());
        appendAdvice(builder, validationResult.shouldSuggestTopicDive());
        appendCitations(builder, validationResult.primaryHits());
        return builder.toString().trim();
    }

    private String buildProcessAnswer(AiPolicyResolver.PolicyMatch policyMatch,
                                      AiPolicyEvidenceValidator.ValidationResult validationResult) {
        StringBuilder builder = new StringBuilder();
        builder.append("结论：");
        if (policyMatch != null && "double_hundred".equals(policyMatch.policyKey())) {
            builder.append("“双百计划”申请前应先区分申报类别。当前库内已确认创新团队、创业人才的一般流程，可先按已命中的步骤执行判断。\n");
        } else {
            builder.append("当前知识库已命中可确认的申请/申报流程依据。\n");
        }
        builder.append("当前依据：\n");
        List<String> steps = extractProcessSteps(validationResult.primaryHits());
        if (steps.isEmpty()) {
            builder.append("1. 当前仅命中到部分流程线索，尚缺少足以展开完整步骤的专题证据。\n");
        } else {
            for (int i = 0; i < steps.size(); i++) {
                builder.append(i + 1).append(". ").append(steps.get(i)).append("\n");
            }
        }
        builder.append("当前边界：具体材料清单、申报入口、年度时间节点，仍以当年申报公告和遴选系统要求为准。\n");
        appendAdvice(builder, validationResult.shouldSuggestTopicDive());
        appendCitations(builder, validationResult.primaryHits());
        return builder.toString().trim();
    }

    private String buildCompareAnswer(AiPolicyEvidenceValidator.ValidationResult validationResult) {
        StringBuilder builder = new StringBuilder();
        builder.append("结论：厦门市级与福建省级人才政策属于不同层级，回答时应分别以各自地域专题为准，再进行并列比较。\n");
        Map<String, List<KnowledgeSearchResultVO>> grouped = validationResult.groupByRegion();
        appendRegionBlock(builder, "厦门市级", grouped.get("xiamen_city"));
        appendRegionBlock(builder, "福建省级", grouped.get("fujian_province"));
        builder.append("当前边界：若要继续比较申报条件、补助标准或办理流程，建议分别继续命中对应地区的专题文档。\n");
        appendCitations(builder, validationResult.primaryHits());
        return builder.toString().trim();
    }

    private String buildTopicAnswer(AiPolicyResolver.PolicyMatch policyMatch,
                                    AiPolicyEvidenceValidator.ValidationResult validationResult) {
        String title = policyMatch != null && policyMatch.canonicalPolicyName() != null
                ? policyMatch.canonicalPolicyName()
                : "相关政策专题";
        StringBuilder builder = new StringBuilder();
        builder.append("结论：当前知识库已命中 ").append(title).append(" 的可确认内容。\n");
        appendPointBlock(builder, "当前依据", extractPoints(validationResult.primaryHits(), List.of("支持", "对象", "措施", "专题", "政策")));
        appendBoundary(builder, validationResult.validationSummary());
        appendAdvice(builder, validationResult.shouldSuggestTopicDive());
        appendCitations(builder, validationResult.primaryHits());
        return builder.toString().trim();
    }

    private String buildPointAnswer(String conclusion,
                                    AiPolicyEvidenceValidator.ValidationResult validationResult,
                                    List<String> keywords) {
        StringBuilder builder = new StringBuilder();
        builder.append(conclusion).append("\n");
        appendPointBlock(builder, "当前依据", extractPoints(validationResult.primaryHits(), keywords));
        appendBoundary(builder, validationResult.validationSummary());
        appendAdvice(builder, validationResult.shouldSuggestTopicDive());
        appendCitations(builder, validationResult.primaryHits());
        return builder.toString().trim();
    }

    private String buildInsufficientAnswer(AiPolicyRegionResolver.RegionMatch regionMatch,
                                           AiPolicyIntentClassifier.IntentMatch intentMatch,
                                           AiPolicyResolver.PolicyMatch policyMatch,
                                           AiPolicyRouterService.RoutePlan routePlan,
                                           AiPolicyEvidenceValidator.ValidationResult validationResult) {
        StringBuilder builder = new StringBuilder();
        builder.append("结论：当前知识库暂未命中足够的");
        if (policyMatch != null && policyMatch.matched()) {
            builder.append(policyMatch.canonicalPolicyName() == null ? "目标政策" : policyMatch.canonicalPolicyName());
        } else if (regionMatch != null && regionMatch.scope() != AiPolicyRegionResolver.RegionScope.UNKNOWN) {
            builder.append(regionMatch.scope().getLabel()).append("政策");
        } else {
            builder.append("政策专题");
        }
        builder.append("证据，暂不能把未命中的细节当作已确认事实。\n");
        builder.append("当前依据：");
        builder.append(resolveIntentHint(intentMatch.intentType(), routePlan.detailIntent())).append("\n");
        builder.append("当前边界：").append(validationResult.validationSummary()).append("\n");
        builder.append("是否建议继续命中专题/原文：建议继续命中对应专题文档、年度申报公告或原始条款，再确认细项内容。\n");
        return builder.toString().trim();
    }

    private String resolveIntentHint(IntentType intentType, boolean detailIntent) {
        return switch (intentType) {
            case LIST -> "已识别为清单型问题，建议优先补充主文档总览或目录型专题。";
            case PROCESS -> "已识别为流程型问题，建议优先补充流程专题、年度通知或申报步骤条款。";
            case CONDITION -> "已识别为条件型问题，建议优先补充适用对象、资格要求、认定条件专题。";
            case BENEFIT -> "已识别为待遇型问题，建议优先补充补助标准、拨付安排、兑现条款。";
            case SERVICE -> "已识别为服务保障型问题，建议优先补充服务保障专题。";
            case RISK -> "已识别为管理期/风险型问题，建议优先补充管理期、退出或追回条款。";
            case COMPARE -> "已识别为对比型问题，建议分别补充厦门与福建两个地域的对应专题。";
            default -> detailIntent ? "已识别为细节型专题问题，建议优先补充对应专题文档。" : "已识别为专题型问题，建议补充该政策的专题证据。";
        };
    }

    private void appendPolicyList(StringBuilder builder, List<KnowledgeSearchResultVO> hits) {
        LinkedHashSet<String> policies = new LinkedHashSet<>();
        for (KnowledgeSearchResultVO hit : hits) {
            String policyName = firstNonBlank(hit.getPolicyName(), hit.getDocTitle());
            if (policyName != null) {
                policies.add(policyName);
            }
            if (policies.size() >= 8) {
                break;
            }
        }
        if (policies.isEmpty()) {
            builder.append("1. 当前尚未抽取出足够稳定的目录项。\n");
            return;
        }
        int index = 1;
        for (String policy : policies) {
            builder.append(index++).append(". ").append(policy).append("\n");
        }
    }

    private void appendRegionBlock(StringBuilder builder, String label, List<KnowledgeSearchResultVO> hits) {
        builder.append(label).append("：\n");
        if (hits == null || hits.isEmpty()) {
            builder.append("1. 当前未命中足够的该地域专题证据。\n");
            return;
        }
        appendPolicyList(builder, hits);
    }

    private void appendPointBlock(StringBuilder builder, String title, List<String> points) {
        builder.append(title).append("：\n");
        if (points.isEmpty()) {
            builder.append("1. 当前仅命中到部分专题线索，仍缺少可直接展开的细项条款。\n");
            return;
        }
        for (int i = 0; i < points.size(); i++) {
            builder.append(i + 1).append(". ").append(points.get(i)).append("\n");
        }
    }

    private void appendBoundary(StringBuilder builder, String boundary) {
        builder.append("当前边界：").append(boundary).append("\n");
    }

    private void appendAdvice(StringBuilder builder, boolean suggestTopicDive) {
        builder.append("是否建议继续命中专题/原文：");
        if (suggestTopicDive) {
            builder.append("建议继续命中对应专题或原始条款，补齐流程细项、材料清单、金额标准或年度要求。");
        } else {
            builder.append("当前可先依据已命中证据作答，如需更细条款仍可继续下钻专题/原文。");
        }
        builder.append("\n");
    }

    private void appendCitations(StringBuilder builder, List<KnowledgeSearchResultVO> hits) {
        builder.append("政策依据：\n");
        LinkedHashSet<String> citations = new LinkedHashSet<>();
        for (KnowledgeSearchResultVO hit : hits) {
            citations.add(firstNonBlank(hit.getDocTitle(), "未命名文档")
                    + " / "
                    + firstNonBlank(hit.getHeadingPath(), hit.getSectionTitle(), "未标注标题"));
        }
        if (citations.isEmpty()) {
            builder.append("- 当前未形成可引用的专题证据。\n");
            return;
        }
        for (String citation : citations) {
            builder.append("- ").append(citation).append("\n");
        }
    }

    private List<String> extractProcessSteps(List<KnowledgeSearchResultVO> hits) {
        List<String> orderedKeywords = List.of("组织申报", "资格核查", "部门联审", "综合评审", "研究确认", "公示", "拨付");
        LinkedHashSet<String> steps = new LinkedHashSet<>();
        for (String keyword : orderedKeywords) {
            for (KnowledgeSearchResultVO hit : hits) {
                String text = buildText(hit);
                if (text.contains(keyword)) {
                    steps.add(keyword + "： " + abbreviate(extractAround(text, keyword), 80));
                    break;
                }
            }
        }
        if (steps.isEmpty()) {
            for (KnowledgeSearchResultVO hit : hits) {
                if (steps.size() >= 5) {
                    break;
                }
                String text = abbreviate(buildText(hit), 80);
                if (!text.isBlank()) {
                    steps.add(text);
                }
            }
        }
        return new ArrayList<>(steps);
    }

    private List<String> extractPoints(List<KnowledgeSearchResultVO> hits, List<String> keywords) {
        LinkedHashSet<String> points = new LinkedHashSet<>();
        for (KnowledgeSearchResultVO hit : hits) {
            String text = buildText(hit);
            for (String keyword : keywords) {
                if (text.contains(keyword)) {
                    points.add(abbreviate(extractAround(text, keyword), 90));
                    break;
                }
            }
            if (points.size() >= 4) {
                break;
            }
        }
        return new ArrayList<>(points);
    }

    private String extractAround(String text, String anchor) {
        if (text == null || text.isBlank()) {
            return "";
        }
        int index = anchor == null ? -1 : text.indexOf(anchor);
        if (index < 0) {
            return text.replaceAll("\\s+", " ").trim();
        }
        int start = Math.max(0, index - 12);
        int end = Math.min(text.length(), index + 72);
        return text.substring(start, end).replaceAll("\\s+", " ").trim();
    }

    private String buildText(KnowledgeSearchResultVO hit) {
        return String.join(" ",
                firstNonBlank(hit.getHeadingPath(), ""),
                firstNonBlank(hit.getPolicyName(), ""),
                firstNonBlank(hit.getSnippet(), ""));
    }

    private String abbreviate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }
}
