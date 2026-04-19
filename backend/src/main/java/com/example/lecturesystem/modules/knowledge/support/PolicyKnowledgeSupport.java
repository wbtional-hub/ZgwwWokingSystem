package com.example.lecturesystem.modules.knowledge.support;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PolicyKnowledgeSupport {
    private static final Pattern POLICY_NO_PATTERN = Pattern.compile("([\\u4e00-\\u9fa5A-Za-z]{1,20}[〔\\(（]?\\d{4}[〕\\)）]?\\d{0,4}号)");
    private static final List<String> LIST_KEYWORDS = List.of("有哪些", "包括哪些", "包含哪些", "列举", "清单", "名单");
    private static final List<String> PROCESS_KEYWORDS = List.of("怎么申请", "如何申请", "如何申报", "怎么申报", "流程", "材料", "审核", "公示", "兑现", "拨付");
    private static final List<String> CONDITION_KEYWORDS = List.of("条件", "要求", "资格", "适用对象", "能不能申请");
    private static final List<String> BENEFIT_KEYWORDS = List.of("补助多少", "奖励多少", "安家补贴", "支持多少", "待遇", "补贴多少");
    private static final List<String> FAQ_KEYWORDS = List.of("常见问题", "高频问答", "faq");
    private static final List<String> BOUNDARY_KEYWORDS = List.of("厦门和福建", "厦门 vs 福建", "省级", "市级", "省市", "边界", "有什么区别");
    private static final List<String> PROCESS_SECTION_KEYWORDS = List.of("申报", "申请", "流程", "遴选", "评审", "审核", "公示", "确认", "拨付", "兑现");
    private static final List<String> CONDITION_SECTION_KEYWORDS = List.of("条件", "要求", "资格", "对象", "适用对象", "学历", "年龄", "合同", "缴税", "服务期");
    private static final List<String> BENEFIT_SECTION_KEYWORDS = List.of("补助", "补贴", "奖励", "安家", "资金", "金额", "标准", "待遇", "资助");
    private static final List<String> FAQ_SECTION_KEYWORDS = List.of("高频问答", "常见问题", "问答");
    private static final List<String> ROUTING_SECTION_KEYWORDS = List.of("检索建议", "导入目录");
    private static final List<String> LIST_SECTION_KEYWORDS = List.of("总览", "目录", "政策正文", "政策清单", "概览");
    private static final List<String> XM_SPECIAL_TOPICS = List.of("双百计划", "特聘岗位", "专项资金", "群鹭兴厦", "博士后", "人工智能", "住房", "台湾人才", "台湾特聘专家", "服务保障", "子女教育", "医疗保障");
    private static final List<String> FJ_SPECIAL_TOPICS = List.of("福建省高层次人才认定", "省引才百人计划", "百人计划", "四大经济", "四大经济专项认定", "博士后", "工程师队伍建设", "台湾人才", "国际化引才");
    private static final List<PolicyAliasMapping> POLICY_ALIAS_MAPPINGS = List.of(
            new PolicyAliasMapping("厦门市引进高层次创新创业人才“双百计划”实施意见", "XM", List.of("双百计划")),
            new PolicyAliasMapping("厦门市高层次人才特聘岗位实施方案", "XM", List.of("特聘岗位")),
            new PolicyAliasMapping("厦门市高层次人才专项资金管理办法", "XM", List.of("专项资金")),
            new PolicyAliasMapping("关于更加精准有效集聚人才加快推进高质量发展的意见", "XM", List.of("群鹭兴厦", "厦门人才总纲", "厦门人才意见")),
            new PolicyAliasMapping("厦门市电子信息产业人才项目实施办法", "XM", List.of("电子信息人才", "电子信息产业人才")),
            new PolicyAliasMapping("厦门市机械装备产业人才项目实施办法", "XM", List.of("机械装备人才", "机械装备产业人才")),
            new PolicyAliasMapping("厦门市商贸物流产业人才项目实施办法", "XM", List.of("商贸物流人才", "商贸物流产业人才")),
            new PolicyAliasMapping("厦门市金融服务产业人才项目实施办法", "XM", List.of("金融人才", "金融服务产业人才")),
            new PolicyAliasMapping("厦门市生物医药产业人才项目实施办法", "XM", List.of("生物医药人才", "生物医药产业人才")),
            new PolicyAliasMapping("厦门市新能源和新材料产业人才项目实施办法", "XM", List.of("新能源和新材料人才", "新能源新材料人才")),
            new PolicyAliasMapping("厦门市文旅创意产业人才项目实施办法", "XM", List.of("文旅创意人才")),
            new PolicyAliasMapping("厦门市海洋经济人才项目实施办法", "XM", List.of("海洋经济人才")),
            new PolicyAliasMapping("厦门市重点产业骨干人才项目实施办法", "XM", List.of("重点产业骨干人才")),
            new PolicyAliasMapping("厦门市教育人才项目实施办法", "XM", List.of("教育人才")),
            new PolicyAliasMapping("厦门市卫生健康人才项目实施办法", "XM", List.of("卫生健康人才")),
            new PolicyAliasMapping("厦门市社会工作人才项目实施办法", "XM", List.of("社会工作人才")),
            new PolicyAliasMapping("厦门市台湾特聘专家（专才）项目实施办法", "XM", List.of("台湾特聘专家", "台湾专才", "台湾人才")),
            new PolicyAliasMapping("厦门市支持人工智能领域人才发展的若干措施", "XM", List.of("人工智能人才", "AI人才", "ai人才")),
            new PolicyAliasMapping("关于进一步加强博士后工作的若干措施", "XM", List.of("博士后")),
            new PolicyAliasMapping("厦门市引进高层次人才住房补贴实施意见", "XM", List.of("住房", "住房补贴", "住房类人才政策")),
            new PolicyAliasMapping("福建省高层次人才认定与支持专题", "FJ", List.of("福建省高层次人才认定", "高层次人才认定", "福建省高层次人才认定与支持")),
            new PolicyAliasMapping("福建省第十批引进高层次创业创新人才申报通知", "FJ", List.of("省引才百人计划", "福建省引才百人计划", "百人计划")),
            new PolicyAliasMapping("福建省近期申报与专项支持专题", "FJ", List.of("近期申报", "专项支持")),
            new PolicyAliasMapping("福建省四大经济专项认定专题", "FJ", List.of("四大经济", "四大经济专项认定")),
            new PolicyAliasMapping("福建省博士后与工程师队伍建设专题", "FJ", List.of("福建省博士后", "工程师队伍建设")),
            new PolicyAliasMapping("福建省台湾人才与国际化引才专题", "FJ", List.of("福建省台湾人才", "国际化引才", "台湾人才"))
    );

    private PolicyKnowledgeSupport() {
    }

    public static void enrichParsedDocResult(ParsedDocResult result, String fileName) {
        if (result == null) {
            return;
        }
        StructuredDocType structuredDocType = detectStructuredDocType(fileName, result.getTitle(), result.getSummary());
        result.setStructuredDocType(structuredDocType.name());
        result.setRegionScope(resolveRegionScope(fileName, result.getTitle(), result.getSummary(), structuredDocType));
        result.setSearchable(structuredDocType != StructuredDocType.IMPORT_GUIDE);
        String defaultPolicyName = extractPolicyName(result.getTitle(), result.getSummary());
        String defaultPolicyAliases = joinAliases(defaultPolicyName, extractMatchedAliases(result.getTitle()));
        String defaultPolicyNo = extractPolicyNo(result.getTitle(), result.getSummary());
        for (ParsedDocSection section : result.getSections()) {
            String chapterTitle = normalize(section.getChapterTitle());
            String sectionTitle = normalize(section.getSectionTitle());
            String headingPath = normalize(section.getHeadingPath());
            String contentText = normalize(section.getContentText());
            String policyName = firstNonBlank(
                    extractPolicyName(sectionTitle, chapterTitle, headingPath),
                    extractPolicyName(contentText),
                    defaultPolicyName
            );
            section.setRegionScope(result.getRegionScope());
            section.setDocType(toChunkDocType(structuredDocType));
            section.setTopicType(detectTopicType(chapterTitle, sectionTitle, contentText, structuredDocType));
            section.setPolicyName(policyName);
            section.setPolicyAliases(joinAliases(policyName, extractMatchedAliases(headingPath), extractMatchedAliases(contentText), splitCsv(defaultPolicyAliases)));
            section.setPolicyNo(firstNonBlank(extractPolicyNo(headingPath, contentText), defaultPolicyNo));
            section.setScenePriority(detectScenePriority(section.getTopicType()));
            section.setSearchable(Boolean.TRUE.equals(result.getSearchable()));
        }
    }

    public static StructuredDocType detectStructuredDocType(String fileName, String title, String summary) {
        String merged = mergeText(fileName, title, summary);
        if (containsAny(merged, List.of("导入清单", "总导入清单", "总打包清单"))) {
            return StructuredDocType.IMPORT_GUIDE;
        }
        boolean fj = containsAny(merged, List.of("福建省", "福建"));
        boolean xm = containsAny(merged, List.of("厦门市", "厦门"));
        boolean main = containsAny(merged, List.of("清洗导入版 v2", "清洗导入版", "主文档"));
        if (xm && main) {
            return StructuredDocType.MAIN_DOC_XM;
        }
        if (fj && main) {
            return StructuredDocType.MAIN_DOC_FJ;
        }
        if (xm) {
            return StructuredDocType.TOPIC_DOC_XM;
        }
        if (fj) {
            return StructuredDocType.TOPIC_DOC_FJ;
        }
        return StructuredDocType.GENERIC;
    }

    public static String resolveRegionScope(String... texts) {
        return resolveRegionScope(null, null, mergeText(texts), StructuredDocType.GENERIC);
    }

    public static String resolveRegionScope(String fileName, String title, String content, StructuredDocType structuredDocType) {
        if (structuredDocType == StructuredDocType.MAIN_DOC_XM || structuredDocType == StructuredDocType.TOPIC_DOC_XM) {
            return "XM";
        }
        if (structuredDocType == StructuredDocType.MAIN_DOC_FJ || structuredDocType == StructuredDocType.TOPIC_DOC_FJ) {
            return "FJ";
        }
        String merged = mergeText(fileName, title, content);
        if (containsAny(merged, List.of("厦门市", "厦门"))) {
            return "XM";
        }
        if (containsAny(merged, List.of("福建省", "福建"))) {
            return "FJ";
        }
        return "UNKNOWN";
    }

    public static String toChunkDocType(StructuredDocType structuredDocType) {
        if (structuredDocType == StructuredDocType.MAIN_DOC_XM || structuredDocType == StructuredDocType.MAIN_DOC_FJ) {
            return "main";
        }
        if (structuredDocType == StructuredDocType.TOPIC_DOC_XM || structuredDocType == StructuredDocType.TOPIC_DOC_FJ) {
            return "topic";
        }
        if (structuredDocType == StructuredDocType.IMPORT_GUIDE) {
            return "guide";
        }
        return "generic";
    }

    public static PolicyQuestionType detectQuestionType(String question) {
        String normalized = normalize(question);
        if (normalized == null) {
            return PolicyQuestionType.GENERAL;
        }
        if (containsAny(normalized, BOUNDARY_KEYWORDS) || (containsAny(normalized, List.of("厦门")) && containsAny(normalized, List.of("福建")))) {
            return PolicyQuestionType.BOUNDARY;
        }
        if (containsAny(normalized, FAQ_KEYWORDS)) {
            return PolicyQuestionType.FAQ;
        }
        if (containsAny(normalized, LIST_KEYWORDS)) {
            return PolicyQuestionType.LIST;
        }
        if (containsAny(normalized, PROCESS_KEYWORDS)) {
            return PolicyQuestionType.PROCESS;
        }
        if (containsAny(normalized, BENEFIT_KEYWORDS)) {
            return PolicyQuestionType.BENEFIT;
        }
        if (containsAny(normalized, CONDITION_KEYWORDS)) {
            return PolicyQuestionType.CONDITION;
        }
        if (containsAny(normalized, XM_SPECIAL_TOPICS) || containsAny(normalized, FJ_SPECIAL_TOPICS) || !extractFormalPolicyNames(normalized).isEmpty()) {
            return PolicyQuestionType.SPECIAL_TOPIC;
        }
        return PolicyQuestionType.GENERAL;
    }

    public static String detectTopicType(String chapterTitle, String sectionTitle, String contentText, StructuredDocType structuredDocType) {
        String merged = mergeText(chapterTitle, sectionTitle, contentText);
        if (containsAny(merged, ROUTING_SECTION_KEYWORDS)) {
            return "routing";
        }
        if (containsAny(merged, FAQ_SECTION_KEYWORDS)) {
            return "faq";
        }
        if (containsAny(merged, PROCESS_SECTION_KEYWORDS)) {
            return "process";
        }
        if (containsAny(merged, CONDITION_SECTION_KEYWORDS)) {
            return "condition";
        }
        if (containsAny(merged, BENEFIT_SECTION_KEYWORDS)) {
            return "benefit";
        }
        if (containsAny(merged, LIST_SECTION_KEYWORDS) || structuredDocType == StructuredDocType.MAIN_DOC_XM || structuredDocType == StructuredDocType.MAIN_DOC_FJ) {
            return "list";
        }
        return "special_topic";
    }

    public static String detectScenePriority(String topicType) {
        String normalized = normalize(topicType);
        if (normalized == null) {
            return "GENERAL";
        }
        if (List.of("process", "condition", "benefit", "faq", "routing", "list").contains(normalized)) {
            return "MOBILE_POLICY_CONSULTANT";
        }
        return "AI_WORKBENCH";
    }

    public static List<String> extractFormalPolicyNames(String text) {
        LinkedHashSet<String> names = new LinkedHashSet<>();
        String normalized = normalize(text);
        if (normalized == null) {
            return List.of();
        }
        for (PolicyAliasMapping mapping : POLICY_ALIAS_MAPPINGS) {
            if (containsAny(normalized, mapping.aliases()) || containsKeyword(normalized, mapping.canonicalName())) {
                names.add(mapping.canonicalName());
            }
        }
        return new ArrayList<>(names);
    }

    public static List<String> extractMatchedAliases(String text) {
        LinkedHashSet<String> aliases = new LinkedHashSet<>();
        String normalized = normalize(text);
        if (normalized == null) {
            return List.of();
        }
        for (PolicyAliasMapping mapping : POLICY_ALIAS_MAPPINGS) {
            for (String alias : mapping.aliases()) {
                if (containsKeyword(normalized, alias)) {
                    aliases.add(alias);
                }
            }
        }
        return new ArrayList<>(aliases);
    }

    public static String extractPolicyName(String... texts) {
        for (String text : texts) {
            List<String> formalNames = extractFormalPolicyNames(text);
            if (!formalNames.isEmpty()) {
                return formalNames.get(0);
            }
        }
        for (String text : texts) {
            String normalized = normalize(text);
            if (normalized == null) {
                continue;
            }
            String candidate = firstPolicyLikeSentence(normalized);
            if (candidate != null) {
                return candidate;
            }
        }
        return null;
    }

    public static String extractPolicyNo(String... texts) {
        for (String text : texts) {
            String normalized = normalize(text);
            if (normalized == null) {
                continue;
            }
            Matcher matcher = POLICY_NO_PATTERN.matcher(normalized);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return null;
    }

    public static String joinAliases(String policyName, List<String>... aliasGroups) {
        LinkedHashSet<String> merged = new LinkedHashSet<>();
        String normalizedPolicyName = normalize(policyName);
        if (normalizedPolicyName != null) {
            for (PolicyAliasMapping mapping : POLICY_ALIAS_MAPPINGS) {
                if (mapping.canonicalName().equalsIgnoreCase(normalizedPolicyName)) {
                    merged.addAll(mapping.aliases());
                }
            }
        }
        if (aliasGroups != null) {
            for (List<String> aliasGroup : aliasGroups) {
                if (aliasGroup != null) {
                    merged.addAll(aliasGroup);
                }
            }
        }
        return merged.isEmpty() ? null : String.join(",", merged);
    }

    public static String normalizeRegionLabel(String regionScope) {
        if ("XM".equalsIgnoreCase(normalize(regionScope))) {
            return "厦门市";
        }
        if ("FJ".equalsIgnoreCase(normalize(regionScope))) {
            return "福建省";
        }
        return normalize(regionScope);
    }

    public static boolean containsAny(String text, List<String> keywords) {
        String normalized = normalize(text);
        if (normalized == null || keywords == null || keywords.isEmpty()) {
            return false;
        }
        for (String keyword : keywords) {
            if (containsKeyword(normalized, keyword)) {
                return true;
            }
        }
        return false;
    }

    public static boolean containsKeyword(String text, String keyword) {
        String normalizedText = normalize(text);
        String normalizedKeyword = normalize(keyword);
        if (normalizedText == null || normalizedKeyword == null) {
            return false;
        }
        return normalizedText.contains(normalizedKeyword)
                || normalizedText.toLowerCase(Locale.ROOT).contains(normalizedKeyword.toLowerCase(Locale.ROOT));
    }

    public static List<String> splitCsv(String csv) {
        String normalized = normalize(csv);
        if (normalized == null) {
            return List.of();
        }
        String[] parts = normalized.split("[,，、;；]");
        List<String> result = new ArrayList<>();
        for (String part : parts) {
            String value = normalize(part);
            if (value != null) {
                result.add(value);
            }
        }
        return result;
    }

    private static String mergeText(String... texts) {
        List<String> parts = new ArrayList<>();
        if (texts != null) {
            for (String text : texts) {
                String normalized = normalize(text);
                if (normalized != null) {
                    parts.add(normalized);
                }
            }
        }
        return parts.isEmpty() ? null : String.join(" ", parts);
    }

    private static String firstPolicyLikeSentence(String text) {
        if (containsAny(text, List.of("实施意见", "实施方案", "管理办法", "若干措施", "申报通知", "专题"))) {
            String compact = text.replace('\n', ' ').trim();
            return compact.length() <= 120 ? compact : compact.substring(0, 120);
        }
        return null;
    }

    private static String normalize(String text) {
        if (text == null) {
            return null;
        }
        String value = text.trim();
        return value.isEmpty() ? null : value;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            String normalized = normalize(value);
            if (normalized != null) {
                return normalized;
            }
        }
        return null;
    }

    public enum StructuredDocType {
        MAIN_DOC_XM,
        TOPIC_DOC_XM,
        MAIN_DOC_FJ,
        TOPIC_DOC_FJ,
        IMPORT_GUIDE,
        GENERIC
    }

    public enum PolicyQuestionType {
        GENERAL,
        LIST,
        PROCESS,
        CONDITION,
        BENEFIT,
        SPECIAL_TOPIC,
        FAQ,
        BOUNDARY
    }

    public record PolicyAliasMapping(String canonicalName, String regionScope, List<String> aliases) {
    }
}
