package com.example.lecturesystem.modules.agent.support.policy;

import com.example.lecturesystem.modules.agent.dto.PolicyQuestionAnalysis;
import com.example.lecturesystem.modules.agent.entity.AiPolicyConditionIndex;
import com.example.lecturesystem.modules.agent.entity.AiPolicyFaqEntity;
import com.example.lecturesystem.modules.agent.mapper.AiPolicyFaqMapper;
import org.springframework.stereotype.Service;
import com.example.lecturesystem.modules.agent.dto.AiPolicyExactPolicyChunk;


import java.util.Collections;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
@Service
public class AiPolicyFaqService {

    private static final int DIRECT_HIT_THRESHOLD = 70;
//第一步：新增对比关键词
    private static final List<String> CHILD_EDUCATION_TOPIC_KEYWORDS = List.of(
        "子女入学",
        "子女就学",
        "子女教育",
        "孩子上学",
        "小孩上学",
        "随迁子女",
        "入园",
        "读幼儿园",
        "读小学",
        "读初中",
        "读高中",
        "上学问题",
        "读书问题"
);

private static final List<String> MEDICAL_SERVICE_TOPIC_KEYWORDS = List.of(
        "医疗保障",
        "医疗服务",
        "就医",
        "看病",
        "健康体检",
        "体检",
        "保健",
        "诊疗",
        "专家诊疗",
        "就医绿色通道",
        "医疗绿色通道",
        "医疗待遇"
);

private static final List<String> TALENT_SERVICE_TOPIC_KEYWORDS = List.of(
        "服务保障",
        "人才服务",
        "人才服务卡",
        "人才绿色通道",
        "绿色通道",
        "一站式服务",
        "配偶就业",
        "落户服务",
        "居留服务",
        "安居服务",
        "保障待遇",
        "人才待遇",
        "待遇保障"
);
private static final List<String> SETTLEMENT_SERVICE_TOPIC_KEYWORDS = List.of(
        "落户",
        "人才落户",
        "落户服务",
        "户口",
        "迁户口",
        "户籍",
        "户籍迁入",
        "户口迁入",
        "随迁落户",
        "家属落户",
        "居留服务"
);

private static final List<String> SPOUSE_EMPLOYMENT_TOPIC_KEYWORDS = List.of(
        "配偶就业",
        "配偶工作",
        "家属就业",
        "家属工作",
        "爱人就业",
        "爱人工作",
        "夫妻就业",
        "随迁配偶",
        "随迁家属就业",
        "配偶安置",
        "就业服务",
        "就业协调"
);

private static final List<String> TALENT_SERVICE_CARD_TOPIC_KEYWORDS = List.of(
        "人才服务卡",
        "人才卡",
        "服务卡",
        "人才服务凭证",
        "人才服务码",
        "人才服务平台",
        "人才服务窗口",
        "一站式服务",
        "绿色通道"
);
private static final List<String> POLICY_PROCESS_TOPIC_KEYWORDS = List.of(
        "怎么申请",
        "如何申请",
        "怎么申报",
        "如何申报",
        "申请流程",
        "申报流程",
        "办理流程",
        "办理步骤",
        "申报步骤",
        "申请步骤",
        "申报程序",
        "遴选程序",
        "办理程序",
        "怎么办理",
        "如何办理"
);

private static final List<String> POLICY_MATERIAL_TOPIC_KEYWORDS = List.of(
        "材料清单",
        "申请材料",
        "申报材料",
        "办理材料",
        "需要什么材料",
        "需要哪些材料",
        "要什么材料",
        "要哪些材料",
        "证明材料",
        "提交材料",
        "需提交材料",
        "附件材料"
);

private static final List<String> POLICY_ENTRY_TOPIC_KEYWORDS = List.of(
        "办理入口",
        "申报入口",
        "申请入口",
        "申报平台",
        "办理平台",
        "申请平台",
        "申报系统",
        "办理系统",
        "在哪里申请",
        "在哪申请",
        "在哪里申报",
        "在哪申报",
        "哪里申请",
        "哪里申报",
        "线上申请",
        "网上申报"
);

private static final List<String> POLICY_AUTHORITY_TOPIC_KEYWORDS = List.of(
        "主管部门",
        "受理部门",
        "办理部门",
        "归口部门",
        "牵头部门",
        "责任部门",
        "审核部门",
        "哪个部门",
        "哪个单位负责",
        "谁负责",
        "找哪个部门",
        "咨询哪个部门"
);
private static final List<String> POLICY_COMPARE_INTENT_KEYWORDS = List.of(
        "区别",
        "有什么区别",
        "差别",
        "不同",
        "对比",
        "比较",
        "怎么区分",
        "如何区分",
        "边界",
        "一样吗",
        "是否一样",
        "是不是一样",
        "是不是",
        "是否属于",
        "算不算",
        "属于",
        "哪个更适合",
        "适合哪个",
        "分别适合",
        "分别对应",
        "对应哪个",
        "可以同时享受",
        "能否同时享受",
        "能不能同时享受",
        "vs",
        "VS",
        "和",
        "与",
        "跟"
);

private static final List<String> XM_POLICY_KEYWORDS = List.of(
        "厦门",
        "厦门市",
        "市级",
        "厦门政策",
        "厦门市政策",
        "厦门人才政策"
);

private static final List<String> FJ_POLICY_KEYWORDS = List.of(
        "福建",
        "福建省",
        "省级",
        "福建政策",
        "福建省政策",
        "省级政策"
);

private static final List<String> HIGH_LEVEL_TALENT_COMPARE_KEYWORDS = List.of(
        "高层次人才",
        "高层次人才认定",
        "福建省高层次人才",
        "厦门高层次人才",
        "高层次"
);

private static final List<String> DOUBLE_HUNDRED_COMPARE_KEYWORDS = List.of(
        "双百计划",
        "创新创业人才",
        "引进高层次创新创业人才"
);

private static final List<String> POSTDOC_SUBSIDY_COMPARE_KEYWORDS = List.of(
        "博士后补助",
        "博士后资助",
        "博士后",
        "在站补助",
        "出站补助"
);

private static final List<String> SETTLEMENT_SUBSIDY_COMPARE_KEYWORDS = List.of(
        "安家补贴",
        "安家补助",
        "生活补贴",
        "住房补贴",
        "落户补贴"
);

private static final List<String> FINANCE_TALENT_COMPARE_KEYWORDS = List.of(
        "金融服务产业人才",
        "金融产业人才",
        "金融人才",
        "金融服务产业人才项目",
        "金融服务",
        "金融机构",
        "CFA",
        "FRM",
        "ACCA",
        "CPA",
        "精算",
        "法律职业资格",
        "金融证书",
        "金融类资质"
);

private static final List<String> ELECTRONIC_INFO_TALENT_COMPARE_KEYWORDS = List.of(
        "电子信息产业人才",
        "电子信息人才",
        "电子信息产业人才项目",
        "电子信息",
        "软件信息",
        "软件工程",
        "软件开发",
        "计算机",
        "信息技术",
        "人工智能",
        "AI",
        "AI人才"
);
    private final AiPolicyFaqMapper aiPolicyFaqMapper;
    private final AiPolicyQuestionAnalyzer aiPolicyQuestionAnalyzer;
    private final AiPolicyConditionRetrievalService aiPolicyConditionRetrievalService;
    private final AiPolicyExactPolicyMatchService aiPolicyExactPolicyMatchService;

    public AiPolicyFaqService(AiPolicyFaqMapper aiPolicyFaqMapper,
                          AiPolicyQuestionAnalyzer aiPolicyQuestionAnalyzer,
                          AiPolicyConditionRetrievalService aiPolicyConditionRetrievalService,
                          AiPolicyExactPolicyMatchService aiPolicyExactPolicyMatchService) {
    this.aiPolicyFaqMapper = aiPolicyFaqMapper;
    this.aiPolicyQuestionAnalyzer = aiPolicyQuestionAnalyzer;
    this.aiPolicyConditionRetrievalService = aiPolicyConditionRetrievalService;
    this.aiPolicyExactPolicyMatchService = aiPolicyExactPolicyMatchService;
}

    public FaqMatch match(Long baseId,
                          AiPolicyQuestionNormalizer.NormalizedQuestion question,
                          AiPolicyRegionResolver.RegionMatch regionMatch,
                          AiPolicyResolver.PolicyMatch policyMatch) {
        if (baseId == null || question == null) {
            return FaqMatch.notMatched();
        }
        //第二步：在 match 里增加政策对比匹配
FaqMatch exactPolicyMatch = tryMatchExactPolicy(baseId, question);
if (exactPolicyMatch.matched()) {
    return exactPolicyMatch;
}

FaqMatch compareMatch = tryMatchPolicyCompareTopic(question);
if (compareMatch.matched()) {
    return compareMatch;
}

FaqMatch serviceBenefitMatch = tryMatchServiceBenefitTopic(question);
if (serviceBenefitMatch.matched()) {
    return serviceBenefitMatch;
}

FaqMatch handlingGuideMatch = tryMatchPolicyHandlingGuideTopic(question);
if (handlingGuideMatch.matched()) {
    return handlingGuideMatch;
}

FaqMatch conditionMatch = tryMatchConditionIndex(baseId, question);
if (conditionMatch.matched()) {
    return conditionMatch;
}
        /*
         * 第二优先级：原有 FAQ 直接命中逻辑
         * 保留你现有逻辑，避免影响“双百计划是什么”“博士后补助多少”等正常 FAQ。
         */
        List<AiPolicyFaqEntity> faqList;
        try {
            faqList = aiPolicyFaqMapper.queryEnabledByBaseId(baseId);
        } catch (Exception ex) {
            return FaqMatch.notMatched();
        }

        AiPolicyFaqEntity best = null;
        int bestScore = 0;
        int bestPriority = 0;

        for (AiPolicyFaqEntity faq : faqList) {
            int score = score(faq, question, regionMatch, policyMatch);
            int priority = faq.getPriority() == null ? 0 : faq.getPriority();

            if (score > bestScore || (score == bestScore && priority > bestPriority)) {
                bestScore = score;
                bestPriority = priority;
                best = faq;
            }
        }

        if (best == null || bestScore < DIRECT_HIT_THRESHOLD) {
            return FaqMatch.notMatched();
        }

        if (shouldRejectFaqForProfileOrEligibilityQuestion(question, best)) {
            return FaqMatch.notMatched();
        }

        String answer = "结论：" + best.getStandardAnswer() + "\n"
                + "当前依据：" + valueOrDefault(best.getEvidenceSource(), "已命中政策 FAQ。") + "\n"
                + "当前边界：如需进一步确认流程细项、材料清单、时间节点或条文原文，请继续命中对应专题或年度通知。";

        return new FaqMatch(true, best, answer);
    }
private FaqMatch tryMatchExactPolicy(Long baseId,
                                     AiPolicyQuestionNormalizer.NormalizedQuestion question) {
    String questionText = buildPrimaryQuestionText(question);
    if (questionText == null || questionText.isBlank()) {
        return FaqMatch.notMatched();
    }

    List<AiPolicyExactPolicyChunk> chunks;
    try {
        chunks = aiPolicyExactPolicyMatchService.match(baseId, questionText);
    } catch (Exception ex) {
        return FaqMatch.notMatched();
    }

    if (chunks == null || chunks.isEmpty()) {
        return FaqMatch.notMatched();
    }

    String answer = buildExactPolicyAnswer(chunks);
    return new FaqMatch(true, null, answer);
}

private String buildExactPolicyAnswer(List<AiPolicyExactPolicyChunk> chunks) {
    Map<String, AiPolicyExactPolicyChunk> policyMap = new LinkedHashMap<>();

    for (AiPolicyExactPolicyChunk chunk : chunks) {
        if (chunk == null || !notBlank(chunk.getPolicyName())) {
            continue;
        }

        AiPolicyExactPolicyChunk old = policyMap.get(chunk.getPolicyName());
        if (old == null || (!notBlank(old.getPolicyNo()) && notBlank(chunk.getPolicyNo()))) {
            policyMap.put(chunk.getPolicyName(), chunk);
        }
    }

    if (policyMap.isEmpty()) {
        return "结论：当前未命中明确的政策名称依据。\n"
                + "当前边界：建议继续命中对应专题文档、年度申报公告或原始条款后再确认。";
    }

    AiPolicyExactPolicyChunk main = policyMap.values().iterator().next();
    String policyName = main.getPolicyName();
    String policyNo = main.getPolicyNo();

    Map<String, String> fieldMap = new LinkedHashMap<>();
    Set<Long> evidenceChunkIds = new LinkedHashSet<>();

    for (AiPolicyExactPolicyChunk chunk : chunks) {
        if (chunk == null || !notBlank(chunk.getContentText())) {
            continue;
        }

        String content = chunk.getContentText();

        boolean extracted = false;

        extracted |= putExtractedField(fieldMap, "主管部门", content,
        "主管部门", "受理部门", "办理部门", "归口部门", "牵头部门", "责任部门");

extracted |= putExtractedField(fieldMap, "适用对象", content,
        "项目对象", "适用对象", "支持对象");

extracted |= putExtractedField(fieldMap, "分层分类", content,
        "分层或分类", "分类", "层级");

extracted |= putExtractedField(fieldMap, "申报方式", content,
        "申报方式", "谁来推荐申报", "推荐申报", "申报渠道");

extracted |= putExtractedField(fieldMap, "申报流程", content,
        "申报流程", "申请流程", "办理流程", "申报程序", "遴选程序", "办理程序");

extracted |= putExtractedField(fieldMap, "材料清单", content,
        "材料清单", "申请材料", "申报材料", "办理材料", "证明材料", "需提交材料");

extracted |= putExtractedField(fieldMap, "办理入口", content,
        "办理入口", "申报入口", "申请入口", "申报平台", "办理平台", "申报系统");

extracted |= putExtractedField(fieldMap, "支持标准", content,
        "支持标准", "基本支持标准");

extracted |= putExtractedField(fieldMap, "安家补贴", content,
        "是否有安家补贴", "安家补贴");

        if (extracted && chunk.getId() != null) {
            evidenceChunkIds.add(chunk.getId());
        }
    }

    StringBuilder sb = new StringBuilder();

    sb.append("结论：已命中《").append(policyName).append("》");
    if (notBlank(policyNo)) {
        sb.append("（").append(policyNo).append("）");
    }
    sb.append("。\n\n");

    if (!fieldMap.isEmpty()) {
        sb.append("政策摘要：\n");
        for (Map.Entry<String, String> entry : fieldMap.entrySet()) {
            if (notBlank(entry.getValue())) {
                sb.append(entry.getKey()).append("：").append(entry.getValue()).append("\n");
            }
        }
    } else {
        sb.append("政策要点：\n");

        int count = 1;
        for (AiPolicyExactPolicyChunk chunk : chunks) {
            if (chunk == null || !notBlank(chunk.getContentText())) {
                continue;
            }

            String text = compactDisplayText(chunk.getContentText());
            if (!notBlank(text) || isInternalPolicyChunk(text)) {
                continue;
            }

            sb.append(count++).append("、").append(text).append("\n");

            if (chunk.getId() != null) {
                evidenceChunkIds.add(chunk.getId());
            }

            if (count > 5) {
                break;
            }
        }
    }

    if (evidenceChunkIds.isEmpty()) {
        for (AiPolicyExactPolicyChunk chunk : chunks) {
            if (chunk != null && chunk.getId() != null) {
                evidenceChunkIds.add(chunk.getId());
            }
            if (evidenceChunkIds.size() >= 5) {
                break;
            }
        }
    }

    String chunkIds = evidenceChunkIds.stream()
            .limit(5)
            .map(id -> "Chunk " + id)
            .collect(Collectors.joining("、"));

    if (notBlank(chunkIds)) {
        sb.append("\n政策依据：").append(chunkIds).append("。");
    }

    sb.append("\n\n当前边界：以上内容来自当前知识库已命中的政策切片，具体申报时间、材料清单和最终资格认定仍应以年度申报通知及主管部门审核为准。");

    return sb.toString();
}
private boolean putExtractedField(Map<String, String> fieldMap,
                                  String displayName,
                                  String content,
                                  String... labels) {
    if (fieldMap == null || !notBlank(displayName) || !notBlank(content) || labels == null) {
        return false;
    }

    if (fieldMap.containsKey(displayName)) {
        return false;
    }

    for (String label : labels) {
        String value = extractLabelValue(content, label);
        if (notBlank(value)) {
            fieldMap.put(displayName, value);
            return true;
        }
    }

    return false;
}

private String extractLabelValue(String content, String label) {
    if (!notBlank(content) || !notBlank(label)) {
        return "";
    }

    String normalized = content
            .replace("\r\n", "\n")
            .replace("\r", "\n");

    String[] lines = normalized.split("\n");
    for (String rawLine : lines) {
        String line = rawLine == null ? "" : rawLine.trim();
        if (!line.startsWith(label)) {
            continue;
        }

        String value = "";

        int pipeIndex = line.indexOf("|");
        int colonIndex = line.indexOf("：");
        int englishColonIndex = line.indexOf(":");

        if (pipeIndex >= 0) {
            value = line.substring(pipeIndex + 1).trim();
        } else if (colonIndex >= 0) {
            value = line.substring(colonIndex + 1).trim();
        } else if (englishColonIndex >= 0) {
            value = line.substring(englishColonIndex + 1).trim();
        }

        value = cleanPolicyValue(value);
        if (notBlank(value)) {
            return value;
        }
    }

    return "";
}

private String cleanPolicyValue(String value) {
    if (value == null) {
        return "";
    }

    String cleaned = value.replaceAll("\\s+", " ").trim();

    List<String> stopWords = List.of(
            "高频问法",
            "详细问题跳转建议",
            "识别提示",
            "清洗版建议",
            "专题路由提示",
            "注意事项"
    );

    for (String stopWord : stopWords) {
        int index = cleaned.indexOf(stopWord);
        if (index >= 0) {
            cleaned = cleaned.substring(0, index).trim();
        }
    }

    if (cleaned.length() > 260) {
        cleaned = cleaned.substring(0, 260) + "...";
    }

    return cleaned;
}

private boolean isInternalPolicyChunk(String text) {
    if (!notBlank(text)) {
        return true;
    }

    return text.contains("高频问法")
            || text.contains("详细问题跳转建议")
            || text.contains("识别提示")
            || text.contains("清洗版建议")
            || text.contains("专题路由提示");
}
private String buildPrimaryQuestionText(AiPolicyQuestionNormalizer.NormalizedQuestion question) {
    if (question == null) {
        return "";
    }

    String original = valueOrBlank(question.original()).trim();
    if (!original.isBlank()) {
        return original;
    }

    String normalized = valueOrBlank(question.normalized()).trim();
    if (!normalized.isBlank()) {
        return normalized;
    }

    return valueOrBlank(question.compact()).trim();
}

private String compactDisplayText(String text) {
    if (text == null) {
        return "";
    }

    String value = text.replaceAll("\\s+", " ").trim();
    if (value.length() <= 180) {
        return value;
    }

    return value.substring(0, 180) + "...";
}

private boolean isHighLevelTalentQuestion(String text) {
    return containsAnyText(text, List.of(
            "高层次人才",
            "高层次",
            "特聘岗位",
            "双百计划",
            "群鹭兴厦",
            "领军人才",
            "拔尖人才"
    ));
}

private boolean isPostdoctoralQuestion(String text) {
    return containsAnyText(text, List.of(
            "博士后",
            "博士后工作站",
            "博士后科研工作站",
            "创新实践基地",
            "在站博士后",
            "出站博士后"
    ));
}

private boolean isIndustryTalentQuestion(String text) {
    return containsAnyText(text, List.of(
            "重点产业人才",
            "产业人才",
            "金融服务产业人才",
            "电子信息产业人才",
            "人工智能人才",
            "生物医药产业人才",
            "新能源和新材料产业人才",
            "文旅创意产业人才",
            "海洋经济人才",
            "骨干人才"
    ));
}
private FaqMatch tryMatchPolicyCompareTopic(AiPolicyQuestionNormalizer.NormalizedQuestion question) {
    String questionText = buildQuestionText(question);
    if (questionText == null || questionText.isBlank()) {
        return FaqMatch.notMatched();
    }

    String compact = valueOrBlank(compactText(questionText));
    String matchText = questionText + " " + compact;

    if (!looksLikeCompareQuestion(matchText)) {
        return FaqMatch.notMatched();
    }

    if (containsAnyText(matchText, XM_POLICY_KEYWORDS)
            && containsAnyText(matchText, FJ_POLICY_KEYWORDS)) {
        return new FaqMatch(true, null, buildXmFjPolicyCompareAnswer(matchText));
    }

    if (containsAnyText(matchText, HIGH_LEVEL_TALENT_COMPARE_KEYWORDS)
            && containsAnyText(matchText, DOUBLE_HUNDRED_COMPARE_KEYWORDS)) {
        return new FaqMatch(true, null, buildHighLevelVsDoubleHundredAnswer(matchText));
    }

    if (containsAnyText(matchText, POSTDOC_SUBSIDY_COMPARE_KEYWORDS)
            && containsAnyText(matchText, SETTLEMENT_SUBSIDY_COMPARE_KEYWORDS)) {
        return new FaqMatch(true, null, buildPostdocVsSettlementSubsidyAnswer(matchText));
    }

    if (containsAnyText(matchText, FINANCE_TALENT_COMPARE_KEYWORDS)
            && containsAnyText(matchText, ELECTRONIC_INFO_TALENT_COMPARE_KEYWORDS)) {
        return new FaqMatch(true, null, buildFinanceVsElectronicInfoTalentAnswer(matchText));
    }

    return FaqMatch.notMatched();
}

private boolean looksLikeCompareQuestion(String text) {
    if (!notBlank(text)) {
        return false;
    }

    /*
     * 明确比较型表达：
     * 例如：有什么区别、一样吗、是不是、算不算、是否属于、分别适合哪个。
     */
    if (containsAnyText(text, List.of(
            "区别",
            "有什么区别",
            "差别",
            "不同",
            "对比",
            "比较",
            "怎么区分",
            "如何区分",
            "边界",
            "一样吗",
            "是否一样",
            "是不是一样",
            "是不是",
            "是否属于",
            "算不算",
            "属于",
            "分别适合",
            "适合哪个",
            "分别对应",
            "对应哪个",
            "哪个更适合",
            "可以同时享受",
            "能否同时享受",
            "能不能同时享受"
    ))) {
        return true;
    }

    /*
     * 有连接词 + 比较意图，也视为比较问题。
     * 例如：A 和 B 哪个更适合、A 与 B 是否能同时享受。
     */
    return containsAnyText(text, List.of("和", "与", "跟", "及", "vs", "VS"))
            && containsAnyText(text, POLICY_COMPARE_INTENT_KEYWORDS);
}
private boolean looksLikePersonalCompositePolicyQuestion(String text) {
    if (!notBlank(text)) {
        return false;
    }

    boolean hasPersonalSubject = containsAnyText(text, List.of(
            "我是",
            "本人是",
            "我有",
            "我在",
            "我想",
            "我可以",
            "我能",
            "适合我",
            "帮我判断"
    ));

    boolean hasCompositeIntent = containsAnyText(text, List.of(
            "可以申请哪些",
            "申请哪些",
            "适合哪个",
            "适合哪些",
            "可以同时看",
            "同时看哪些",
            "可以同时享受",
            "能否同时享受",
            "可以吗",
            "哪些补助",
            "哪些政策",
            "什么政策"
    ));

    boolean hasPolicyCondition = containsAnyText(text, List.of(
            "博士",
            "博士后",
            "高级职称",
            "本科",
            "硕士",
            "CFA",
            "FRM",
            "CPA",
            "ACCA",
            "金融机构",
            "软件工程",
            "人工智能",
            "AI",
            "电子信息",
            "特聘岗位",
            "住房补贴",
            "服务保障",
            "留厦"
    ));

    return hasPersonalSubject && hasCompositeIntent && hasPolicyCondition;
}
private FaqMatch tryMatchServiceBenefitTopic(AiPolicyQuestionNormalizer.NormalizedQuestion question) {
    String questionText = buildQuestionText(question);
    if (questionText == null || questionText.isBlank()) {
        return FaqMatch.notMatched();
    }

    String compact = valueOrBlank(compactText(questionText));
    String matchText = questionText + " " + compact;

    if (looksLikePersonalCompositePolicyQuestion(matchText)) {
        return FaqMatch.notMatched();
    }


    if (containsAnyText(matchText, CHILD_EDUCATION_TOPIC_KEYWORDS)) {
        return new FaqMatch(true, null, buildChildEducationServiceAnswer(matchText));
    }

    if (containsAnyText(matchText, MEDICAL_SERVICE_TOPIC_KEYWORDS)) {
        return new FaqMatch(true, null, buildMedicalServiceAnswer(matchText));
    }

    if (containsAnyText(matchText, SETTLEMENT_SERVICE_TOPIC_KEYWORDS)) {
        return new FaqMatch(true, null, buildSettlementServiceAnswer(matchText));
    }

    if (containsAnyText(matchText, SPOUSE_EMPLOYMENT_TOPIC_KEYWORDS)) {
        return new FaqMatch(true, null, buildSpouseEmploymentServiceAnswer(matchText));
    }

    if (containsAnyText(matchText, TALENT_SERVICE_CARD_TOPIC_KEYWORDS)) {
        return new FaqMatch(true, null, buildTalentServiceCardAnswer(matchText));
    }

    if (containsAnyText(matchText, TALENT_SERVICE_TOPIC_KEYWORDS)) {
        return new FaqMatch(true, null, buildTalentServiceAnswer(matchText));
    }

    return FaqMatch.notMatched();
}

private String buildChildEducationServiceAnswer(String questionText) {
    String targetAdvice = buildChildEducationTargetAdvice(questionText);

    return "结论：子女入学属于人才服务保障类事项，不是单独的人才项目。根据当前知识库，应重点关注高层次人才、博士后、重点产业人才等政策中的“子女入学、子女就学、教育服务保障”相关条款。\n\n"
            + targetAdvice
            + "适配说明：\n"
            + "1、这类事项通常要先确认本人属于哪一类人才政策对象，例如高层次人才、博士后、重点产业人才或其他专项人才。\n"
            + "2、是否能够享受子女入学保障，还需要结合人才类别、认定层次、子女学段、户籍或居住情况、学校学位情况以及当年教育部门办理规则判断。\n"
            + "3、仅具备学历、职称、专业或资格证书，一般不能直接等同于已经可以享受子女入学保障。\n\n"
            + "办理建议：建议先确认本人已认定或拟申报的人才类别，再查询对应政策中的服务保障条款；涉及具体学校、学段和入学时间的，应以当年教育部门和人才服务窗口通知为准。\n\n"
            + "当前边界：以上为人才服务保障方向的初步判断，最终能否办理应以正式政策文件、年度通知和主管部门审核为准。";
}

private String buildChildEducationTargetAdvice(String questionText) {
    if (isPostdoctoralQuestion(questionText)) {
        return "重点方向：如果咨询对象是博士后，建议优先查看博士后相关政策中是否有子女就学、服务保障或属地协调条款，同时确认是在站博士后、出站留厦博士后还是依托设站单位申报。\n\n";
    }

    if (isHighLevelTalentQuestion(questionText)) {
        return "重点方向：如果咨询对象是高层次人才，建议优先查看高层次人才认定、特聘岗位、双百计划或综合性人才政策中的子女入学服务保障条款，并结合人才层次判断保障范围。\n\n";
    }

    if (isIndustryTalentQuestion(questionText)) {
        return "重点方向：如果咨询对象是重点产业人才，建议优先查看对应产业人才项目及服务保障条款，确认所在企业、岗位方向、申报类别和人才层次是否符合要求。\n\n";
    }

    return "";
}

private String buildMedicalServiceAnswer(String questionText) {
    String targetAdvice = buildMedicalTargetAdvice(questionText);

    return "结论：医疗保障属于人才服务保障类事项。根据当前知识库，应重点查看高层次人才、博士后、重点产业人才等政策中是否包含医疗服务、健康体检、就医便利、保健服务或就医绿色通道等内容。\n\n"
            + targetAdvice
            + "适配说明：\n"
            + "1、这里的医疗保障主要指人才政策中的配套服务保障，不等同于普通医保报销政策。\n"
            + "2、是否可以享受医疗服务保障，通常取决于人才类别、认定层次、所在单位、在厦工作情况以及政策具体条款。\n"
            + "3、具备申报资格不等于自动享受医疗保障，通常还需要完成相应人才认定或项目申报。\n\n"
            + "办理建议：建议先确认本人是否已经纳入相应人才政策或人才认定范围，再查询对应政策中的医疗服务、健康体检、就医绿色通道等条款。\n\n"
            + "当前边界：以上为人才政策服务保障方向的初步判断，不作为普通医保待遇或指定医院安排的承诺。";
}

private String buildMedicalTargetAdvice(String questionText) {
    if (isPostdoctoralQuestion(questionText)) {
        return "重点方向：如果咨询对象是博士后，应优先查看博士后政策及设站单位服务保障安排，确认是否有健康体检、医疗服务或属地人才服务支持。\n\n";
    }

    if (isHighLevelTalentQuestion(questionText)) {
        return "重点方向：如果咨询对象是高层次人才，应优先查看高层次人才认定支持、特聘岗位或综合性人才政策中关于医疗服务、健康体检、就医便利的条款。\n\n";
    }

    if (isIndustryTalentQuestion(questionText)) {
        return "重点方向：如果咨询对象是重点产业人才，应优先查看对应产业人才项目是否配套医疗服务或人才服务保障，并确认所在单位是否属于政策覆盖范围。\n\n";
    }

    return "";
}

private String buildTalentServiceAnswer(String questionText) {
    String targetAdvice = buildTalentServiceTargetAdvice(questionText);

    return "结论：服务保障属于人才政策中的配套待遇方向，通常包括安居保障、子女入学、医疗服务、配偶就业、落户服务、人才服务窗口或绿色通道等内容。\n\n"
            + targetAdvice
            + "适配说明：\n"
            + "1、服务保障不是单独判断“能不能申请一个项目”，而是要先确认本人属于哪一类人才政策对象。\n"
            + "2、不同政策、不同人才层次，对应的服务保障范围可能不同。\n"
            + "3、已经完成高层次人才认定、博士后进站或重点产业人才项目申报的人才，更适合进一步查询具体服务保障内容。\n"
            + "4、仅具备学历、职称、专业或证书条件时，通常只能作为申报入口线索，不能直接等同于已经享受服务保障。\n\n"
            + "办理建议：建议先明确人才身份或申报方向，再对应查询政策中的安居、子女入学、医疗、配偶就业、落户等服务保障条款。\n\n"
            + "当前边界：具体可享受哪些服务，应以政策原文、年度通知、人才认定结果和主管部门办理规则为准。";
}

private String buildTalentServiceTargetAdvice(String questionText) {
    if (isPostdoctoralQuestion(questionText)) {
        return "重点方向：如果咨询对象是博士后，服务保障应优先围绕进站、在站培养、出站留厦、设站单位支持等环节判断，并关注博士后政策中是否明确安居、科研、生活和服务保障内容。\n\n";
    }

    if (isHighLevelTalentQuestion(questionText)) {
        return "重点方向：如果咨询对象是高层次人才，服务保障应优先围绕人才认定层次判断，重点关注安居、子女入学、医疗服务、配偶就业、落户和人才服务绿色通道等内容。\n\n";
    }

    if (isIndustryTalentQuestion(questionText)) {
        return "重点方向：如果咨询对象是重点产业人才，服务保障应优先结合所在产业项目、企业资质、岗位方向、人才类别和单位推荐情况判断。\n\n";
    }

    return "";
}
private String buildSettlementServiceAnswer(String questionText) {
    String targetAdvice = buildSettlementTargetAdvice(questionText);

    return "结论：落户服务属于人才服务保障类事项，通常不是单独的人才项目。根据当前知识库，应重点查看高层次人才、博士后、重点产业人才等政策中是否包含落户、户籍迁入、家属随迁或人才服务窗口办理支持等条款。\n\n"
            + targetAdvice
            + "适配说明：\n"
            + "1、人才落户通常要先确认本人属于哪一类人才政策对象，例如高层次人才、博士后、重点产业人才或其他专项人才。\n"
            + "2、是否可以享受落户服务，还需要结合人才类别、认定层次、工作单位、社保或个税、居住情况、户籍政策和主管部门办理规则判断。\n"
            + "3、人才政策中出现落户服务，并不等于所有人员都可以直接落户，也不等于家属一定可以同步落户。\n\n"
            + "办理建议：建议先确认本人是否已经完成相应人才认定或项目申报，再查看对应政策中的落户服务、家属随迁、人才服务窗口或绿色通道条款。\n\n"
            + "当前边界：以上为人才服务保障方向的初步判断，具体落户条件、材料和办理口径应以公安、人社或人才服务窗口的正式规则为准。";
}

private String buildSettlementTargetAdvice(String questionText) {
    if (isPostdoctoralQuestion(questionText)) {
        return "重点方向：如果咨询对象是博士后，建议优先查看博士后进站、出站留厦、设站单位服务保障及属地人才服务政策中是否涉及落户或家属随迁支持。\n\n";
    }

    if (isHighLevelTalentQuestion(questionText)) {
        return "重点方向：如果咨询对象是高层次人才，建议优先查看高层次人才认定支持、特聘岗位、双百计划或综合性人才政策中的落户服务和绿色通道条款，并结合人才层次判断适用范围。\n\n";
    }

    if (isIndustryTalentQuestion(questionText)) {
        return "重点方向：如果咨询对象是重点产业人才，建议优先结合所在产业项目、企业资质、岗位方向、人才类别和单位推荐情况，判断是否配套落户服务或人才服务窗口支持。\n\n";
    }

    return "";
}
private String buildSpouseEmploymentServiceAnswer(String questionText) {
    String targetAdvice = buildSpouseEmploymentTargetAdvice(questionText);

    return "结论：配偶就业属于人才服务保障类事项，通常是人才政策中的配套服务内容，不是单独的人才项目。根据当前知识库，应重点查看高层次人才、博士后、重点产业人才等政策中是否包含配偶就业、家属就业、就业协调或人才服务窗口支持等条款。\n\n"
            + targetAdvice
            + "适配说明：\n"
            + "1、配偶就业服务通常以协调、推荐、服务对接或政策支持为主，不应理解为一定安排具体岗位。\n"
            + "2、是否可以享受配偶就业服务，通常取决于人才类别、认定层次、配偶自身学历专业、就业意愿、岗位需求、单位接收条件以及当年办理规则。\n"
            + "3、仅具备学历、职称、证书或专业条件，一般不能直接等同于已经可以享受配偶就业保障。\n\n"
            + "办理建议：建议先确认本人属于哪一类人才政策对象，再查看对应政策中的配偶就业、家属就业、就业协调或人才服务窗口条款；同时准备配偶学历、专业、工作经历和就业意向等材料。\n\n"
            + "当前边界：以上为人才服务保障方向的初步判断，不代表政府或单位承诺安排具体岗位，最终以政策原文、年度通知和主管部门办理规则为准。";
}

private String buildSpouseEmploymentTargetAdvice(String questionText) {
    if (isPostdoctoralQuestion(questionText)) {
        return "重点方向：如果咨询对象是博士后，建议优先查看博士后政策、设站单位服务保障和属地人才服务安排中是否涉及配偶就业或家属就业协调。\n\n";
    }

    if (isHighLevelTalentQuestion(questionText)) {
        return "重点方向：如果咨询对象是高层次人才，建议优先查看高层次人才认定支持、特聘岗位、双百计划或综合性人才政策中关于配偶就业、家属服务和绿色通道的条款。\n\n";
    }

    if (isIndustryTalentQuestion(questionText)) {
        return "重点方向：如果咨询对象是重点产业人才，建议优先结合所在产业项目、企业资质、岗位方向和单位推荐情况，判断是否配套配偶就业服务或人才服务窗口支持。\n\n";
    }

    return "";
}
private String buildTalentServiceCardAnswer(String questionText) {
    String targetAdvice = buildTalentServiceCardTargetAdvice(questionText);

    return "结论：人才服务卡通常属于人才服务保障的载体或凭证，用于承接部分人才服务事项，不是单独的人才项目。根据当前知识库，应重点查看高层次人才、博士后、重点产业人才等政策中是否包含人才服务卡、人才服务窗口、一站式服务或绿色通道等内容。\n\n"
            + targetAdvice
            + "适配说明：\n"
            + "1、人才服务卡一般需要先确认人才身份或认定层次，再判断是否具备申领或享受相关服务的资格。\n"
            + "2、人才服务卡可能承载安居、子女入学、医疗服务、配偶就业、落户服务、窗口办理等事项，但具体服务范围以政策细则和办理规则为准。\n"
            + "3、拥有学历、职称、专业或资格证书，不等于自动取得人才服务卡，也不等于自动享受全部服务事项。\n\n"
            + "办理建议：建议先确认本人是否已完成高层次人才认定、博士后身份确认或重点产业人才项目申报，再查询对应政策中的人才服务卡、人才服务窗口、一站式服务或绿色通道条款。\n\n"
            + "当前边界：以上为人才服务保障方向的初步判断，具体是否发卡、服务范围和办理方式，应以正式政策文件、年度通知和人才服务窗口规则为准。";
}

private String buildTalentServiceCardTargetAdvice(String questionText) {
    if (isPostdoctoralQuestion(questionText)) {
        return "重点方向：如果咨询对象是博士后，建议优先查看博士后政策及属地人才服务安排中是否明确人才服务卡、服务窗口或绿色通道支持。\n\n";
    }

    if (isHighLevelTalentQuestion(questionText)) {
        return "重点方向：如果咨询对象是高层次人才，建议优先查看高层次人才认定支持、特聘岗位、双百计划或综合性人才政策中关于人才服务卡、服务窗口和绿色通道的条款。\n\n";
    }

    if (isIndustryTalentQuestion(questionText)) {
        return "重点方向：如果咨询对象是重点产业人才，建议优先结合对应产业人才项目、企业资质、岗位方向、人才类别和单位推荐情况，判断是否配套人才服务卡或人才服务窗口支持。\n\n";
    }

    return "";
}
private FaqMatch tryMatchPolicyHandlingGuideTopic(AiPolicyQuestionNormalizer.NormalizedQuestion question) {
    String questionText = buildQuestionText(question);
    if (questionText == null || questionText.isBlank()) {
        return FaqMatch.notMatched();
    }

    String compact = valueOrBlank(compactText(questionText));
    String matchText = questionText + " " + compact;

    if (containsAnyText(matchText, POLICY_AUTHORITY_TOPIC_KEYWORDS)) {
        return new FaqMatch(true, null, buildPolicyAuthorityGuideAnswer(matchText));
    }

    if (containsAnyText(matchText, POLICY_MATERIAL_TOPIC_KEYWORDS)) {
        return new FaqMatch(true, null, buildPolicyMaterialGuideAnswer(matchText));
    }

    if (containsAnyText(matchText, POLICY_ENTRY_TOPIC_KEYWORDS)) {
        return new FaqMatch(true, null, buildPolicyEntryGuideAnswer(matchText));
    }

    if (containsAnyText(matchText, POLICY_PROCESS_TOPIC_KEYWORDS)) {
        return new FaqMatch(true, null, buildPolicyProcessGuideAnswer(matchText));
    }

    return FaqMatch.notMatched();
}
private String resolveHandlingGuidePolicyName(String questionText) {
    if (questionText == null || questionText.isBlank()) {
        return "";
    }

    if (containsAnyText(questionText, List.of(
            "金融服务产业人才项目",
            "金融服务产业人才",
            "金融人才"
    ))) {
        return "厦门市金融服务产业人才项目";
    }

    if (containsAnyText(questionText, List.of(
            "电子信息产业人才项目",
            "电子信息产业人才",
            "软件信息",
            "软件工程"
    ))) {
        return "厦门市电子信息产业人才项目";
    }

    if (containsAnyText(questionText, List.of(
            "博士后补助",
            "博士后"
    ))) {
        return "博士后相关政策";
    }

    if (containsAnyText(questionText, List.of(
            "双百计划",
            "创新创业人才"
    ))) {
        return "厦门市引进高层次创新创业人才“双百计划”";
    }

    if (containsAnyText(questionText, List.of(
            "特聘岗位"
    ))) {
        return "厦门市高层次人才特聘岗位";
    }

    if (containsAnyText(questionText, List.of(
            "住房补贴",
            "住房保障",
            "人才住房",
            "安居"
    ))) {
        return "人才住房保障相关政策";
    }

    if (containsAnyText(questionText, List.of(
            "重点产业人才",
            "产业人才",
            "骨干人才"
    ))) {
        return "重点产业人才相关政策";
    }

    if (containsAnyText(questionText, List.of(
            "高层次人才",
            "高层次"
    ))) {
        return "高层次人才相关政策";
    }

    return "";
}

private String buildHandlingGuidePolicyDirection(String questionText, String guideType) {
    String policyName = resolveHandlingGuidePolicyName(questionText);
    if (!notBlank(policyName)) {
        return "";
    }

    return "已识别政策方向：" + policyName + "。\n\n"
            + "重点方向：本轮应围绕该政策核对" + guideType
            + "，如果当前知识库没有命中完整年度通知或明细条款，应明确提示以年度申报通知、主管部门和申报系统要求为准，不能按泛问处理，也不能编造未命中的内容。\n\n";
}
private String buildPolicyProcessGuideAnswer(String questionText) {
    String policyDirection = buildHandlingGuidePolicyDirection(questionText, "申报流程、申报方式、办理步骤和组织申报要求");

    return "结论：这是政策申报流程类问题。是否能给出完整流程，取决于当前问题是否明确了具体政策名称，以及知识库中是否已导入该政策的年度申报通知、办理流程或材料清单。\n\n"
            + policyDirection
            + "适配说明：\n"
            + "1、如果已明确具体政策，应优先查询该政策对应的申报通知、申报流程、遴选程序、申报方式和主管部门要求。\n"
            + "2、如果只是泛问“人才政策怎么申请”，一般需要先确认申报方向，例如高层次人才、博士后、重点产业人才、住房保障或服务保障等。\n"
            + "3、不同政策的申报主体可能不同，有的由个人申请，有的需要单位推荐，有的需要主管部门组织申报。\n\n"
            + "办理建议：如已明确政策名称，建议继续核对该政策年度申报通知中的办理时间、申报主体、申报方式、申报入口、材料清单和主管部门；如当前知识库未命中完整流程，只能先给出办理判断方向。\n\n"
            + "当前边界：不能在未命中具体年度通知和流程条款的情况下编造流程，最终应以正式政策文件、年度申报通知和主管部门审核为准。";
}
private String buildPolicyMaterialGuideAnswer(String questionText) {
    String policyDirection = buildHandlingGuidePolicyDirection(questionText, "材料清单、证明材料、单位推荐材料和申报附件");

    return "结论：这是政策材料清单类问题。材料清单通常不能脱离具体政策单独判断，需要结合政策名称、申报类别、人才身份、单位推荐要求和年度申报通知确认。\n\n"
            + policyDirection
            + "适配说明：\n"
            + "1、不同政策需要的材料不同，常见材料可能包括身份证明、学历学位证明、职称或资格证书、劳动合同、社保或个税、单位推荐材料、成果证明、项目材料等。\n"
            + "2、如果是博士后、高层次人才、重点产业人才、住房补贴等方向，材料要求差异较大，不能用同一套材料清单替代。\n"
            + "3、如果知识库没有命中对应年度申报通知或材料清单，系统只能给出准备方向，不能确认最终材料。\n\n"
            + "办理建议：建议围绕具体政策继续核对年度申报通知中的“申报材料、证明材料、附件模板、单位推荐材料、线上填报要求”等内容。\n\n"
            + "当前边界：以上为材料准备方向，不代表正式材料清单；最终材料应以年度申报通知、主管部门或申报系统要求为准。";
}
private String buildPolicyEntryGuideAnswer(String questionText) {
    String policyDirection = buildHandlingGuidePolicyDirection(questionText, "申报入口、办理平台、线上系统和受理渠道");

    return "结论：这是政策办理入口类问题。办理入口通常需要结合具体政策、年度申报通知和主管部门发布的申报渠道确认，不能在未命中正式入口依据时直接指定平台或网址。\n\n"
            + policyDirection
            + "适配说明：\n"
            + "1、人才政策可能通过人才服务窗口、主管部门申报系统、单位推荐渠道、线上平台或年度通知指定入口办理。\n"
            + "2、有些政策不是个人直接申请，而是由单位推荐、行业主管部门组织申报或按年度集中受理。\n"
            + "3、如果当前知识库只命中政策正文，没有命中年度申报公告或入口说明，就不能直接判断具体入口。\n\n"
            + "办理建议：建议围绕具体政策继续核对当年度申报通知中的“申报入口、申报平台、受理单位、办理时间、联系人或咨询电话”等信息。\n\n"
            + "当前边界：以上为办理入口判断原则，不提供未经确认的网址或平台名称；具体入口以主管部门最新通知为准。";
}
private String buildPolicyAuthorityGuideAnswer(String questionText) {
    String policyDirection = buildHandlingGuidePolicyDirection(questionText, "主管部门、受理部门、责任部门和归口部门");

    return "结论：这是主管部门或受理部门类问题。主管部门需要结合具体政策名称判断，不同人才政策可能分别由组织、人社、科技、工信、金融、住建、教育、卫健等部门或行业主管部门负责。\n\n"
            + policyDirection
            + "适配说明：\n"
            + "1、如果问题中已经包含明确政策名称，应优先从该政策正文、实施办法或申报通知中提取主管部门、受理部门、责任部门或归口部门。\n"
            + "2、如果只是泛问“人才政策找哪个部门”，一般需要先明确政策方向，例如高层次人才、产业人才、博士后、住房补贴、子女入学、医疗服务等。\n"
            + "3、同一政策也可能存在主管部门、受理窗口、初审单位、行业推荐单位等不同角色，不能简单混为一个部门。\n\n"
            + "办理建议：建议围绕具体政策继续核对政策正文或年度通知中的“主管部门、受理部门、申报单位、行业主管部门、咨询窗口”等信息。\n\n"
            + "当前边界：如果当前知识库未命中明确主管部门字段，不能替代主管部门正式答复。";
}
private String buildXmFjPolicyCompareAnswer(String questionText) {
    return "结论：厦门市政策和福建省政策属于不同层级的人才政策体系，不能简单混为同一类政策。一般来说，厦门市政策更偏向本市产业发展、在厦就业创业、落地服务和市级财政支持；福建省政策更偏向省级人才认定、全省范围内的引才支持、专项申报和省级统筹。\n\n"
            + "对比说明：\n"
            + "1、政策层级不同：厦门市政策通常属于市级政策，福建省政策通常属于省级政策。\n"
            + "2、适用范围不同：厦门市政策一般要求在厦工作、创业、纳税、社保或由厦门单位推荐；福建省政策通常面向全省范围内符合条件的人才或单位。\n"
            + "3、主管体系不同：市级政策通常由厦门相关主管部门或人才服务窗口组织实施；省级政策通常由省级主管部门或省级专项渠道组织。\n"
            + "4、支持方向可能重叠：例如高层次人才、博士后、台湾人才、产业人才等方向，市级和省级政策都可能涉及，但申报条件、支持标准和办理渠道不一定相同。\n\n"
            + "办理建议：如果问题涉及“我能不能申请”，应先确认工作地、单位归属、申报身份和政策层级；如果同时符合省市政策，还要核对是否允许叠加享受或是否存在重复享受限制。\n\n"
            + "当前边界：以上是省市政策边界说明，具体能否申报、能否叠加、以哪个部门为准，应以对应政策原文和年度申报通知为准。";
}
private String buildHighLevelVsDoubleHundredAnswer(String questionText) {
    return "结论：“双百计划”不等同于高层次人才认定。高层次人才更偏向人才认定或人才层次评价；“双百计划”更偏向高层次创新创业人才引进项目，通常围绕创新创业能力、项目落地、评审遴选和资金支持展开。\n\n"
            + "对比说明：\n"
            + "1、高层次人才：重点看人才层次、认定条件、专业能力、贡献水平和对应服务保障，可能涉及省级或市级认定体系。\n"
            + "2、双百计划：重点看是否属于引进的高层次创新创业人才，通常更关注创业项目、创新成果、落地转化、团队和评审遴选。\n"
            + "3、申报逻辑不同：高层次人才更像“身份认定或层次认定”；双百计划更像“专项人才项目申报”。\n"
            + "4、支持内容不同：高层次人才可能更多关联安居、医疗、子女入学、人才服务等保障；双百计划可能更强调项目支持、创业扶持、资金支持和落地发展。\n\n"
            + "办理建议：如果用户是想确认身份层次，应优先查高层次人才认定；如果用户有创新创业项目、团队或落地计划，应重点看“双百计划”的申报条件和评审要求。\n\n"
            + "当前边界：两者可能存在交叉，但不能互相替代。最终应以具体政策条款、申报通知和主管部门审核为准。";
}
private String buildPostdocVsSettlementSubsidyAnswer(String questionText) {
    return "结论：博士后补助和安家补贴不是同一个概念。博士后补助通常围绕博士后进站、在站科研、平台建设或出站留厦等环节设置；安家补贴通常围绕人才落地、长期服务、住房或生活安置等事项设置。\n\n"
            + "对比说明：\n"
            + "1、博士后补助：重点看是否属于博士后身份，是否进站、在站、出站留厦，所在单位是否为博士后科研工作站或创新实践基地。\n"
            + "2、安家补贴：重点看人才是否符合引进、认定、落地就业、服务年限、住房或生活安置等条件。\n"
            + "3、支持阶段不同：博士后补助更关注博士后培养和科研阶段；安家补贴更关注人才留厦、就业创业和生活安置阶段。\n"
            + "4、材料和审核不同：博士后补助通常要看进站、设站单位、科研或就业材料；安家补贴通常要看人才认定、就业关系、服务期、住房或安置条件等。\n\n"
            + "办理建议：如果问题是博士后在站或出站留厦，应优先查博士后政策；如果问题是落地厦门后的生活安置或住房支持，应进一步查安家补贴、住房补贴或人才住房政策。\n\n"
            + "当前边界：两类支持可能在个别政策中同时出现，但不能默认重复享受或自动叠加，需以政策原文和年度通知为准。";
}
private String buildFinanceVsElectronicInfoTalentAnswer(String questionText) {
    return "结论：金融服务产业人才项目和电子信息产业人才项目都属于重点产业人才支持方向，但适用产业、岗位要求和申报条件不同，不能相互替代。\n\n"
            + "对比说明：\n"
            + "1、金融服务产业人才项目：重点面向金融机构、基金管理机构、地方金融组织、金融投资集团等金融服务领域人才，通常关注金融资质、岗位层级、在厦全职工作、单位推荐等条件。\n"
            + "2、电子信息产业人才项目：重点面向电子信息制造业、软件信息产业、人工智能等方向人才，通常关注所在企业产业属性、岗位方向、技术能力、项目成果和单位推荐等条件。\n"
            + "3、适配条件不同：CFA、FRM、ACCA、CPA 等金融类资质更容易与金融服务产业人才项目相关；软件工程、软件信息、人工智能等专业或岗位更容易与电子信息产业人才项目相关。\n"
            + "4、申报判断不同：不能只看学历或职称，还要看所在单位是否属于政策覆盖范围、岗位是否匹配、社保或个税是否在厦、是否由单位推荐。\n\n"
            + "办理建议：如果用户在金融机构或基金、金融投资相关单位工作，应优先核对金融服务产业人才项目；如果用户在软件、电子信息、人工智能相关企业工作，应优先核对电子信息产业人才项目。\n\n"
            + "当前边界：以上为项目边界说明，最终是否符合条件仍应以正式实施办法、年度申报通知和主管部门审核为准。";
}
    private FaqMatch tryMatchConditionIndex(Long baseId,
                                        AiPolicyQuestionNormalizer.NormalizedQuestion question) {
    String questionText = buildQuestionText(question);
    

    if (questionText == null || questionText.isBlank()) {
        return FaqMatch.notMatched();
    }

    PolicyQuestionAnalysis analysis = aiPolicyQuestionAnalyzer.analyze(questionText);


    if (analysis == null || !analysis.isConditionReverseQuery()) {
        return FaqMatch.notMatched();
    }

    List<AiPolicyConditionIndex> conditionHits;
    try {
        conditionHits = aiPolicyConditionRetrievalService.retrieveByConditions(
                baseId,
                analysis.getConditionCodes()
        );
    } catch (Exception ex) {
       
        return FaqMatch.notMatched();
    }

   

    if (conditionHits == null || conditionHits.isEmpty()) {
        return FaqMatch.notMatched();
    }

    String answer = buildConditionIndexAnswer(conditionHits, analysis);
    return new FaqMatch(true, null, answer);
}

    private String buildConditionIndexAnswer(List<AiPolicyConditionIndex> conditionHits,
                                         PolicyQuestionAnalysis analysis) {
    StringBuilder sb = new StringBuilder();

    Map<String, AiPolicyConditionIndex> policyMainHitMap = new LinkedHashMap<>();
    Map<String, Set<String>> policyConditionMap = new LinkedHashMap<>();
    Map<String, Set<String>> policyRequirementMap = new LinkedHashMap<>();
    Map<String, Set<Long>> policyChunkMap = new LinkedHashMap<>();
    Set<String> allConditionCodes = new LinkedHashSet<>();

    for (AiPolicyConditionIndex hit : conditionHits) {
        if (hit == null || !notBlank(hit.getPolicyName())) {
            continue;
        }

        String policyName = hit.getPolicyName();

        if (notBlank(hit.getConditionCode())) {
            allConditionCodes.add(hit.getConditionCode());
        }

        AiPolicyConditionIndex oldMainHit = policyMainHitMap.get(policyName);
        if (oldMainHit == null || betterMainHit(hit, oldMainHit)) {
            policyMainHitMap.put(policyName, hit);
        }

        policyConditionMap.computeIfAbsent(policyName, k -> new LinkedHashSet<>());
        if (notBlank(hit.getConditionText())) {
            policyConditionMap.get(policyName).add(hit.getConditionText());
        }

        policyRequirementMap.computeIfAbsent(policyName, k -> new LinkedHashSet<>());
        if (notBlank(hit.getRequirementText())) {
            policyRequirementMap.get(policyName).add(hit.getRequirementText());
        }

        policyChunkMap.computeIfAbsent(policyName, k -> new LinkedHashSet<>());
        if (hit.getMatchedChunkId() != null) {
            policyChunkMap.get(policyName).add(hit.getMatchedChunkId());
        }
    }

    if (policyMainHitMap.isEmpty()) {
    return "结论：当前未命中明确的条件反查政策依据。\n"
            + "当前边界：建议继续命中对应专题文档、年度申报公告或原始条款后再确认。";
}

if (isCompositeConditionAnalysis(analysis, allConditionCodes, policyConditionMap, policyMainHitMap)) {
    return buildCompositeConditionIndexAnswer(
            policyMainHitMap,
            policyConditionMap,
            policyRequirementMap,
            policyChunkMap,
            allConditionCodes,
            analysis
    );
}

sb.append("结论：根据当前知识库条件反查结果，您可以重点关注以下政策：\n");

    int index = 1;
    for (Map.Entry<String, AiPolicyConditionIndex> entry : policyMainHitMap.entrySet()) {
        String policyName = entry.getKey();
        AiPolicyConditionIndex mainHit = entry.getValue();

        sb.append(index++).append("、").append(policyName);

        if (notBlank(mainHit.getPolicyNo())) {
            sb.append("（").append(mainHit.getPolicyNo()).append("）");
        }

        sb.append("\n");

        Set<String> conditions = policyConditionMap.get(policyName);
        if (conditions != null && !conditions.isEmpty()) {
            sb.append("   命中条件：").append(String.join("、", conditions)).append("\n");
        }

        Set<String> requirements = policyRequirementMap.get(policyName);
        if (requirements != null && !requirements.isEmpty()) {
            int count = 0;
            for (String requirement : requirements) {
                if (count >= 2) {
                    break;
                }
                sb.append("   适配说明：").append(requirement).append("\n");
                count++;
            }
        }

        Set<Long> chunkIds = policyChunkMap.get(policyName);
        if (chunkIds != null && !chunkIds.isEmpty()) {
            String chunkText = chunkIds.stream()
                    .limit(5)
                    .map(id -> "Chunk " + id)
                    .collect(Collectors.joining("、"));
            sb.append("   依据切片：").append(chunkText).append("\n");
        }
    }

    sb.append("\n办理建议：").append(buildConditionAdviceText(allConditionCodes, policyMainHitMap.keySet()));

    String subjectBoundary = buildSubjectBoundaryText(analysis);
    if (notBlank(subjectBoundary)) {
        sb.append("\n\n咨询对象边界：").append(subjectBoundary);
    }

    sb.append("\n\n当前边界：以上为知识库条件反查结果，是否最终符合申报条件，应以当年度申报通知、正式政策文件和主管部门审核为准。");

    return sb.toString();
}
private String buildCompositeConditionIndexAnswer(
        Map<String, AiPolicyConditionIndex> policyMainHitMap,
        Map<String, Set<String>> policyConditionMap,
        Map<String, Set<String>> policyRequirementMap,
        Map<String, Set<Long>> policyChunkMap,
        Set<String> allConditionCodes,
        PolicyQuestionAnalysis analysis) {

    StringBuilder sb = new StringBuilder();

    sb.append("结论：根据您提供的多个条件，当前可以按“优先关注 / 可重点关注 / 补充关注”进行分层判断。");
    sb.append("多条件匹配只能说明政策适配方向，不能直接等同于已经符合申报资格。\n\n");

    List<Map.Entry<String, AiPolicyConditionIndex>> sortedPolicies = policyMainHitMap.entrySet()
            .stream()
            .sorted((a, b) -> {
                int aCount = safeSetSize(policyConditionMap.get(a.getKey()));
                int bCount = safeSetSize(policyConditionMap.get(b.getKey()));
                if (aCount != bCount) {
                    return Integer.compare(bCount, aCount);
                }

                int aPriority = a.getValue() == null || a.getValue().getPriority() == null
                        ? 9999
                        : a.getValue().getPriority();
                int bPriority = b.getValue() == null || b.getValue().getPriority() == null
                        ? 9999
                        : b.getValue().getPriority();

                return Integer.compare(aPriority, bPriority);
            })
            .collect(Collectors.toList());

    int maxMatchCount = 0;
    for (Map.Entry<String, AiPolicyConditionIndex> entry : sortedPolicies) {
        maxMatchCount = Math.max(maxMatchCount, safeSetSize(policyConditionMap.get(entry.getKey())));
    }

    if (maxMatchCount >= 2) {
        appendCompositePolicyGroup(
                sb,
                "一、优先关注",
                sortedPolicies,
                policyConditionMap,
                policyRequirementMap,
                policyChunkMap,
                maxMatchCount,
                maxMatchCount
        );

        if (maxMatchCount > 2) {
            appendCompositePolicyGroup(
                    sb,
                    "二、可重点关注",
                    sortedPolicies,
                    policyConditionMap,
                    policyRequirementMap,
                    policyChunkMap,
                    2,
                    maxMatchCount - 1
            );
        }

        appendCompositePolicyGroup(
                sb,
                maxMatchCount > 2 ? "三、补充关注" : "二、补充关注",
                sortedPolicies,
                policyConditionMap,
                policyRequirementMap,
                policyChunkMap,
                1,
                1
        );
    } else {
        appendCompositePolicyGroup(
                sb,
                "一、可重点关注",
                sortedPolicies,
                policyConditionMap,
                policyRequirementMap,
                policyChunkMap,
                1,
                1
        );
    }

    sb.append("\n综合办理建议：");
    sb.append(buildCompositeAdviceText(allConditionCodes, policyMainHitMap.keySet()));

    String subjectBoundary = buildSubjectBoundaryText(analysis);
    if (notBlank(subjectBoundary)) {
        sb.append("\n\n咨询对象边界：").append(subjectBoundary);
    }

    sb.append("\n\n当前边界：以上为知识库多条件匹配结果。最终是否符合申报条件，仍需结合单位性质、岗位方向、在厦工作情况、社保或个税、单位推荐、年度通知和主管部门审核判断。");

    return sb.toString();
}
private void appendCompositePolicyGroup(
        StringBuilder sb,
        String groupTitle,
        List<Map.Entry<String, AiPolicyConditionIndex>> sortedPolicies,
        Map<String, Set<String>> policyConditionMap,
        Map<String, Set<String>> policyRequirementMap,
        Map<String, Set<Long>> policyChunkMap,
        int minMatchCount,
        int maxMatchCount) {

    boolean hasGroup = false;
    int index = 1;

    for (Map.Entry<String, AiPolicyConditionIndex> entry : sortedPolicies) {
        String policyName = entry.getKey();
        AiPolicyConditionIndex mainHit = entry.getValue();

        int matchCount = safeSetSize(policyConditionMap.get(policyName));
        if (matchCount < minMatchCount || matchCount > maxMatchCount) {
            continue;
        }

        if (!hasGroup) {
            sb.append(groupTitle).append("：\n");
            hasGroup = true;
        }

        sb.append(index++).append("、").append(policyName);

        if (mainHit != null && notBlank(mainHit.getPolicyNo())) {
            sb.append("（").append(mainHit.getPolicyNo()).append("）");
        }

        sb.append("\n");

        Set<String> conditions = policyConditionMap.get(policyName);
        if (conditions != null && !conditions.isEmpty()) {
            sb.append("   命中条件：").append(String.join("、", conditions)).append("\n");
        }

        Set<String> requirements = policyRequirementMap.get(policyName);
        if (requirements != null && !requirements.isEmpty()) {
            int count = 0;
            for (String requirement : requirements) {
                if (count >= 2) {
                    break;
                }
                sb.append("   适配说明：").append(requirement).append("\n");
                count++;
            }
        }

        Set<Long> chunkIds = policyChunkMap.get(policyName);
        if (chunkIds != null && !chunkIds.isEmpty()) {
            String chunkText = chunkIds.stream()
                    .limit(5)
                    .map(id -> "Chunk " + id)
                    .collect(Collectors.joining("、"));
            sb.append("   依据切片：").append(chunkText).append("\n");
        }
    }

    if (hasGroup) {
        sb.append("\n");
    }
}

private int safeSetSize(Set<?> set) {
    return set == null ? 0 : set.size();
}
private String buildCompositeAdviceText(Set<String> conditionCodes,
                                        Set<String> policyNames) {
    String joinedPolicies = policyNames == null ? "" : String.join("、", policyNames);
    Set<String> codes = conditionCodes == null ? Collections.emptySet() : conditionCodes;

    boolean hasDoctor = codes.contains("doctor_degree")
            || codes.contains("phd")
            || codes.contains("doctor");

    boolean hasPostdoctoral = codes.contains("postdoctoral");

    boolean hasSeniorTitle = codes.contains("senior_title");

    boolean hasFinanceCondition = codes.contains("finance_org")
            || codes.contains("cfa")
            || codes.contains("frm")
            || codes.contains("cpa")
            || codes.contains("acca")
            || codes.contains("fsa")
            || codes.contains("actuary")
            || codes.contains("legal_profession");

    boolean hasFinancePolicyOnly = joinedPolicies.contains("金融服务产业人才");

    boolean hasSoftwareOrAi = codes.contains("software_engineering")
            || codes.contains("software_information")
            || codes.contains("ai")
            || codes.contains("integrated_circuit")
            || joinedPolicies.contains("电子信息产业人才")
            || joinedPolicies.contains("人工智能");

    boolean hasHousing = codes.contains("housing")
            || codes.contains("housing_subsidy")
            || codes.contains("rent_subsidy")
            || codes.contains("purchase_subsidy")
            || codes.contains("settlement_housing")
            || codes.contains("talent_housing")
            || codes.contains("youth_rental_housing")
            || codes.contains("affordable_housing")
            || joinedPolicies.contains("住房");

    boolean hasSpecialPost = codes.contains("special_post")
            || joinedPolicies.contains("特聘岗位");

    /*
     * 优先级说明：
     * 1. 博士后阶段问题优先；
     * 2. 明确的软件/AI/电子信息方向优先于“顺带命中的金融项目”；
     * 3. 博士 + 特聘岗位 + 住房补贴，要优先走特聘岗位/住房建议；
     * 4. 只有真实出现金融机构或金融资格证时，才优先给金融建议。
     */

    if (hasPostdoctoral) {
        return "建议优先区分进站博士后、在站博士后、出站留厦博士后等阶段，再同步核对博士后补助、安家补贴、住房保障和服务保障条款。";
    }

    if (hasDoctor && hasSpecialPost && hasHousing) {
        return "建议同时核对高层次人才特聘岗位和住房保障政策。特聘岗位重点看博士学位、海外经历、用人单位设岗资格等条件；住房补贴或人才住房还需结合人才类别、在厦就业、服务年限、是否已享受过同类住房政策和年度申报通知判断。";
    }

    if (hasSoftwareOrAi && hasSeniorTitle) {
        return "建议优先核对电子信息产业人才项目和高层次人才认定支持。电子信息方向重点看所在企业是否属于电子信息、软件信息或人工智能产业，高级职称还可同步关注省级高层次人才认定或相关人才支持政策。";
    }

    if (hasSoftwareOrAi) {
        return "建议优先核对电子信息产业人才项目和人工智能相关政策，重点确认所在企业是否属于电子信息、软件信息或人工智能方向，岗位、项目成果、申报类别和单位推荐条件是否匹配。";
    }

    if (hasFinanceCondition && hasSeniorTitle) {
        return "建议优先核对金融服务产业人才项目，同时确认所在机构是否属于金融机构、基金管理机构、地方金融组织或金融投资集团，高级职称是否与岗位方向匹配，在厦全职工作年限和单位推荐条件是否满足。";
    }

    if (hasFinanceCondition) {
        return "建议优先核对金融服务产业人才项目，重点确认金融机构、基金管理机构、地方金融组织或金融投资集团等单位属性，以及CFA、FRM、ACCA、CPA等资格证书、岗位层级、在厦全职工作和单位推荐要求。";
    }

    if (hasDoctor && hasSeniorTitle) {
        return "建议同时核对高层次人才认定、特聘岗位和重点产业人才项目，重点确认博士学位、高级职称、工作经历、所在单位和岗位方向是否同时匹配。";
    }

    if (hasHousing) {
        return "建议先确认本人是否属于新引进人才、高层次人才或重点产业人才，再结合住房补贴、人才住房、租房或购房支持政策分别判断。";
    }

    if (hasFinancePolicyOnly) {
        return "当前结果中命中了金融服务产业人才项目，但还需要进一步确认是否具备金融机构、金融岗位、金融资格证书、在厦全职工作和单位推荐等关键条件，不能仅凭学历或职称直接判断适配。";
    }

    return "建议先按照命中条件最多的政策方向优先核对，再补充确认单位性质、岗位方向、社保或个税、单位推荐和年度申报通知。";
}
private boolean isCompositeConditionAnalysis(PolicyQuestionAnalysis analysis,
                                             Set<String> allConditionCodes,
                                             Map<String, Set<String>> policyConditionMap,
                                             Map<String, AiPolicyConditionIndex> policyMainHitMap) {
    int allCodeCount = allConditionCodes == null ? 0 : allConditionCodes.size();

    int analyzerCodeCount = 0;
    if (analysis != null && analysis.getConditionCodes() != null) {
        analyzerCodeCount = analysis.getConditionCodes().size();
    }

    int maxPolicyConditionCount = 0;
    if (policyConditionMap != null) {
        for (Set<String> conditions : policyConditionMap.values()) {
            maxPolicyConditionCount = Math.max(maxPolicyConditionCount, safeSetSize(conditions));
        }
    }

    int policyCount = policyMainHitMap == null ? 0 : policyMainHitMap.size();

    return analyzerCodeCount >= 2
            || allCodeCount >= 2
            || maxPolicyConditionCount >= 2
            || (policyCount >= 2 && allCodeCount >= 2);
}
private boolean betterMainHit(AiPolicyConditionIndex current, AiPolicyConditionIndex old) {
    if (current == null) {
        return false;
    }
    if (old == null) {
        return true;
    }

    int currentPriority = current.getPriority() == null ? 9999 : current.getPriority();
    int oldPriority = old.getPriority() == null ? 9999 : old.getPriority();

    if (currentPriority != oldPriority) {
        return currentPriority < oldPriority;
    }

    boolean currentHasPolicyNo = notBlank(current.getPolicyNo());
    boolean oldHasPolicyNo = notBlank(old.getPolicyNo());

    if (currentHasPolicyNo != oldHasPolicyNo) {
        return currentHasPolicyNo;
    }

    Long currentChunkId = current.getMatchedChunkId();
    Long oldChunkId = old.getMatchedChunkId();

    if (currentChunkId == null) {
        return false;
    }
    if (oldChunkId == null) {
        return true;
    }

    return currentChunkId < oldChunkId;
}

private String buildConditionAdviceText(Set<String> conditionCodes, Set<String> policyNames) {
    String joinedPolicies = policyNames == null ? "" : String.join("、", policyNames);

    if (conditionCodes != null && conditionCodes.contains("postdoctoral")) {
        return "建议先确认是否属于进站博士后、所在单位是否为博士后科研工作站或创新实践基地、当前处于在站还是出站留厦阶段，并对照年度申报通知准备设站单位证明、进站材料、科研成果和就业材料。";
    }

    if (conditionCodes != null && (
            conditionCodes.contains("housing")
                    || conditionCodes.contains("housing_subsidy")
                    || conditionCodes.contains("rent_subsidy")
                    || conditionCodes.contains("purchase_subsidy")
                    || conditionCodes.contains("settlement_housing")
                    || conditionCodes.contains("talent_housing")
                    || conditionCodes.contains("youth_rental_housing")
                    || conditionCodes.contains("affordable_housing")
    )) {
        return "建议先确认本人是否属于政策覆盖的人才类别，是否为新引进人才或高层次人才，是否已享受过原有住房政策，并结合服务年限、住房保障方式、租房/购房/人才住房类型及年度申报通知判断。";
    }

    if (conditionCodes != null && conditionCodes.contains("doctor_degree")) {
        return "建议先区分申报方向：如关注特聘岗位，应重点确认用人单位是否具备设岗资格、本人是否符合海外博士学位或博士学位加2年以上海外学习工作经历等要求；如关注金融服务产业人才项目，还需结合所在金融机构、岗位层级、在厦全职工作年限和单位推荐要求判断。";
    }

    if (conditionCodes != null && conditionCodes.contains("senior_title")) {
        return "建议先确认高级职称的具体等级、专业方向和取得方式，再结合所在单位类型、岗位层级、人才类别、在厦或在闽工作情况及年度申报通知判断；涉及金融服务产业人才项目时，还需确认所在机构是否属于政策覆盖范围。";
    }

    if (joinedPolicies.contains("金融服务产业人才")) {
        return "建议先确认所在机构是否属于金融机构、基金管理机构、地方金融组织或金融投资集团，并准备资格证书、学历证明、在厦连续全职工作年限、社保或个税、单位推荐等材料。";
    }

    if (joinedPolicies.contains("电子信息产业人才")) {
        return "建议先确认所在企业是否属于电子信息制造业、软件信息产业或人工智能产业，岗位是否与申报方向匹配，并核对主要工作地、个税缴纳地、申报类别和单位推荐要求。";
    }

    if (conditionCodes != null && conditionCodes.contains("ai")) {
        return "建议先确认本人岗位是否属于人工智能产业链相关研发、应用、平台建设或产业化方向，再结合所在企业资质、项目成果和年度申报通知判断适配政策。";
    }

    return "建议先确认所在单位、工作地点、申报身份、资格证书、社保或个税、单位推荐资格等是否符合政策申报要求，再按主管部门或年度通知办理。";
}
    private String buildSubjectBoundaryText(PolicyQuestionAnalysis analysis) {
        if (analysis == null) {
            return "";
        }

        if ("CHILD".equals(analysis.getTargetSubject())) {
    return "本轮问题是代子女或他人咨询，系统只使用本轮问题明确提供的信息，不引用历史上下文中的用户本人学历、专业、行业或单位信息作为判断条件。";
}

        return "";
    }

    private String buildQuestionText(AiPolicyQuestionNormalizer.NormalizedQuestion question) {
        if (question == null) {
            return "";
        }

        String original = valueOrBlank(question.original());
        String normalized = valueOrBlank(question.normalized());
        String compact = valueOrBlank(question.compact());

        String merged = (original + " " + normalized + " " + compact).trim();
        return merged.isBlank() ? "" : merged;
    }

    private int score(AiPolicyFaqEntity faq,
                      AiPolicyQuestionNormalizer.NormalizedQuestion question,
                      AiPolicyRegionResolver.RegionMatch regionMatch,
                      AiPolicyResolver.PolicyMatch policyMatch) {
        if (faq == null || question == null) {
            return 0;
        }

        String compactQuestion = compactText(question.compact());
        if (compactQuestion == null) {
            compactQuestion = compactText(question.normalized());
        }
        if (compactQuestion == null) {
            compactQuestion = compactText(question.original());
        }
        if (compactQuestion == null) {
            return 0;
        }

        int textScore = scoreByQuestionText(faq, compactQuestion);

        /*
         * 关键规则：
         * 必须先有文本命中，才允许 region / policy / priority 加分。
         * 不能让 priority 自己决定命中，否则高优先级 FAQ 会误答所有问题。
         */
        if (textScore <= 0) {
            return 0;
        }

        int score = textScore;

        if (regionMatch != null
                && regionMatch.scope() != AiPolicyRegionResolver.RegionScope.UNKNOWN
                && notBlank(faq.getRegionScope())
                && regionMatch.scope().getCode().equalsIgnoreCase(faq.getRegionScope())) {
            score += 8;
        }

        if (policyMatch != null
                && policyMatch.matched()
                && notBlank(policyMatch.policyKey())
                && policyMatch.policyKey().equals(faq.getPolicyKey())) {
            score += 10;
        }

        /*
         * priority 只作为轻微加分和同分排序依据，不能作为命中依据。
         */
        int priority = faq.getPriority() == null ? 0 : faq.getPriority();
        score += Math.min(Math.max(priority, 0), 140) / 20;

        return score;
    }

    private boolean shouldRejectFaqForProfileOrEligibilityQuestion(AiPolicyQuestionNormalizer.NormalizedQuestion question,
                                                                   AiPolicyFaqEntity faq) {
        if (question == null || faq == null) {
            return false;
        }

        String text = compactText(
                valueOrBlank(question.original()) + " " +
                        valueOrBlank(question.normalized()) + " " +
                        valueOrBlank(question.compact())
        );

        if (text == null) {
            return false;
        }

        /*
         * 只拦截两类问题：
         * 1. 用户明显在描述自己：我是本科生、我在厦门、我从业21年
         * 2. 用户明显在问个人适配：我可以申请什么政策、我适合哪个政策
         *
         * 不要因为出现“本科、硕士、博士”就直接拦截，
         * 否则会误伤“博士后补助多少”“博士后怎么申请”等正常 FAQ。
         */
        boolean hasSelfDescription = containsAnyText(text, List.of(
                "我是",
                "本人是",
                "我现在是",
                "我目前是",
                "我在厦门",
                "我在福建",
                "我在外地",
                "我从业",
                "从业",
                "我的专业",
                "我专业",
                "我的学历",
                "我学历",
                "我毕业",
                "我关注",
                "我想了解"
        ));

        boolean hasPersonalEligibilityIntent = containsAnyText(text, List.of(
                "我可以申请",
                "我能申请",
                "我适合",
                "适合我",
                "适合申请",
                "帮我判断",
                "帮我分析",
                "给我推荐",
                "为我匹配",
                "我可以申报",
                "我能申报",
                "我能享受",
                "我可以享受"
        ));

        if (!hasSelfDescription && !hasPersonalEligibilityIntent) {
            return false;
        }

        String standardQuestion = valueOrBlank(faq.getStandardQuestion());

        if (containsAnyText(standardQuestion, List.of(
                "适合哪个人才政策",
                "怎么判断",
                "可以申请哪些政策",
                "我不知道自己适合哪个人才政策"
        ))) {
            return false;
        }

        /*
         * 其余普通 FAQ，如 housing benefit、postdoc benefit、overview 等，
         * 不要抢答用户画像/个人适配类问题。
         */
        return true;
    }

    private int scoreByQuestionText(AiPolicyFaqEntity faq, String compactQuestion) {
        int best = 0;

        String standardQuestion = compactText(faq.getStandardQuestion());
        if (standardQuestion != null) {
            if (compactQuestion.equals(standardQuestion)) {
                best = Math.max(best, 100);
            } else if (compactQuestion.contains(standardQuestion)) {
                best = Math.max(best, 95);
            } else if (standardQuestion.contains(compactQuestion) && compactQuestion.length() >= 6) {
                best = Math.max(best, 86);
            }
        }

        String pattern = faq.getQuestionPattern() == null ? "" : faq.getQuestionPattern();
        for (String rawTerm : pattern.split("\\|")) {
            String term = compactText(rawTerm);
            if (term == null) {
                continue;
            }

            if (compactQuestion.equals(term)) {
                best = Math.max(best, 100);
                continue;
            }

            if (compactQuestion.contains(term)) {
                if (term.length() >= 6) {
                    best = Math.max(best, 90);
                } else if (term.length() >= 4) {
                    best = Math.max(best, 76);
                } else {
                    best = Math.max(best, 45);
                }
                continue;
            }

            /*
             * 用户问题可能比 pattern 少几个字，例如：
             * 问：双百计划补助标准
             * pattern：双百计划补助标准是什么
             */
            if (term.contains(compactQuestion) && compactQuestion.length() >= 6) {
                best = Math.max(best, 82);
            }
        }

        return best;
    }

    private String compactText(String text) {
        if (text == null) {
            return null;
        }
        String value = text
                .replaceAll("\\s+", "")
                .replaceAll("[？?。！!，,、；;：:（）()【】\\[\\]《》\"“”'‘’]", "")
                .trim();
        return value.isBlank() ? null : value;
    }

    private String valueOrBlank(String text) {
        return text == null ? "" : text;
    }

    private boolean containsAnyText(String text, List<String> keywords) {
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

    private boolean notBlank(String text) {
        return text != null && !text.isBlank();
    }

    private String valueOrDefault(String text, String defaultValue) {
        return text == null || text.isBlank() ? defaultValue : text;
    }

    public record FaqMatch(boolean matched, AiPolicyFaqEntity faq, String answer) {
        public static FaqMatch notMatched() {
            return new FaqMatch(false, null, null);
        }
    }
}