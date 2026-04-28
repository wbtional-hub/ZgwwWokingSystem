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
FaqMatch exactPolicyMatch = tryMatchExactPolicy(baseId, question);
if (exactPolicyMatch.matched()) {
    return exactPolicyMatch;
}

FaqMatch serviceBenefitMatch = tryMatchServiceBenefitTopic(question);
if (serviceBenefitMatch.matched()) {
    return serviceBenefitMatch;
}
        /*
         * 第一优先级：条件反查索引
         * 例如：
         * - 有 CFA 可以申请什么政策？
         * - 软件工程专业可以申请什么人才政策？
         * - 本科生可以申请什么政策？
         *
         * 这类问题不能只靠 FAQ 文本相似度，否则容易被“双百计划”“特聘岗位”等高频 FAQ 抢答。
         */
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

        extracted |= putExtractedField(fieldMap, "主管部门", content, "主管部门");
        extracted |= putExtractedField(fieldMap, "适用对象", content, "项目对象", "适用对象", "支持对象");
        extracted |= putExtractedField(fieldMap, "分层分类", content, "分层或分类", "分类", "层级");
        extracted |= putExtractedField(fieldMap, "申报方式", content, "申报方式", "谁来推荐申报");
        extracted |= putExtractedField(fieldMap, "支持标准", content, "支持标准", "基本支持标准");
        extracted |= putExtractedField(fieldMap, "安家补贴", content, "是否有安家补贴", "安家补贴");

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

private FaqMatch tryMatchServiceBenefitTopic(AiPolicyQuestionNormalizer.NormalizedQuestion question) {
    String questionText = buildQuestionText(question);
    if (questionText == null || questionText.isBlank()) {
        return FaqMatch.notMatched();
    }

    String compact = valueOrBlank(compactText(questionText));
    String matchText = questionText + " " + compact;

    if (containsAnyText(matchText, CHILD_EDUCATION_TOPIC_KEYWORDS)) {
        return new FaqMatch(true, null, buildChildEducationServiceAnswer(matchText));
    }

    if (containsAnyText(matchText, MEDICAL_SERVICE_TOPIC_KEYWORDS)) {
        return new FaqMatch(true, null, buildMedicalServiceAnswer(matchText));
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

        if ("CHILD".equals(analysis.getTargetSubject())
                || "OTHER_PERSON".equals(analysis.getTargetSubject())) {
            return "本轮问题是代他人咨询，系统只使用本轮问题明确提供的信息，不引用历史上下文中的用户本人学历、专业、行业或单位信息作为判断条件。";
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