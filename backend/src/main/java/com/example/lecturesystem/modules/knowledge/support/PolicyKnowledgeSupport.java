package com.example.lecturesystem.modules.knowledge.support;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PolicyKnowledgeSupport {
    private static final Pattern POLICY_NO_PATTERN = Pattern.compile("([\\u4e00-\\u9fa5A-Za-z]{1,20}[〔\\(（]?\\d{4}[〕\\)）]?\\d{0,4}号)");
    private static final Pattern CATALOG_SERIAL_PREFIX_PATTERN = Pattern.compile("^(?:第[一二三四五六七八九十百]+[章节部分]\\s*|[（(]?[一二三四五六七八九十]+[)）]?[、.．]\\s*|\\d+[、.．]\\s*)");
    private static final List<String> LIST_KEYWORDS = List.of("有哪些", "包括哪些", "包含哪些", "列举", "清单", "名单");
    private static final List<String> PROCESS_KEYWORDS = List.of("怎么申请", "如何申请", "如何申报", "怎么申报", "流程", "材料", "审核", "公示", "兑现", "拨付");
    private static final List<String> CONDITION_KEYWORDS = List.of("条件", "要求", "资格", "适用对象", "能不能申请");
    private static final List<String> BENEFIT_KEYWORDS = List.of(
        "补助多少",
        "奖励多少",
        "安家补贴",
        "支持多少",
        "待遇",
        "补贴多少",
        "保障待遇",
        "人才待遇",
        "待遇保障"
);

private static final List<String> SERVICE_BENEFIT_KEYWORDS = List.of(
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
        "服务保障",
        "人才服务",
        "人才服务卡",
        "绿色通道",
        "一站式服务",
        "配偶就业",
        "落户服务",
        "居留服务",
        "人才待遇",
        "待遇保障"
);
    private static final List<String> FAQ_KEYWORDS = List.of("常见问题", "高频问答", "faq");
    private static final List<String> BOUNDARY_KEYWORDS = List.of("厦门和福建", "厦门 vs 福建", "省级", "市级", "省市", "边界", "有什么区别");
    private static final List<String> PROCESS_SECTION_KEYWORDS = List.of("申报", "申请", "流程", "遴选", "评审", "审核", "公示", "确认", "拨付", "兑现");
    private static final List<String> CONDITION_SECTION_KEYWORDS = List.of("条件", "要求", "资格", "对象", "适用对象", "学历", "年龄", "合同", "缴税", "服务期");
    private static final List<String> BENEFIT_SECTION_KEYWORDS = List.of("补助", "补贴", "奖励", "安家", "资金", "金额", "标准", "待遇", "资助");
    private static final List<String> FAQ_SECTION_KEYWORDS = List.of("高频问答", "常见问题", "问答");
    private static final List<String> ROUTING_SECTION_KEYWORDS = List.of("检索建议", "导入目录");
    private static final List<String> LIST_SECTION_KEYWORDS = List.of("总览", "目录", "政策正文", "政策清单", "概览");
    private static final List<String> INVALID_CATALOG_PREFIXES = List.of(
            "以下为当前知识库已整理",
            "当前知识库已命中的主要政策",
            "以下为",
            "如问到具体材料清单",
            "若问到具体材料清单",
            "本章用于回答",
            "本专题优先服务",
            "建议继续命中年度公告"
    );
    private static final List<String> INVALID_CATALOG_PHRASES = List.of(
            "不代表所有历史政策全文清单",
            "建议继续命中年度公告",
            "如问到具体材料清单/申报入口",
            "若问到具体材料清单/申报入口",
            "专题问法路由建议",
            "专题补充文档",
            "政策依据",
            "引用：",
            "引用:",
            "当前知识库已命中的主要政策/项目",
            "知识库专题补充文档",
            "路由建议",
            "问答增强版",
            "清洗导入版",
            "目录说明",
            "清单说明",
            "本章用于回答",
            "如用户继续追问",
            "应跳转对应专题",
            "优先服务省级上位政策问答",
            "只作补充说明",
            "专题类政策导航",
            "政策导航"
    );
    private static final List<String> META_CATALOG_TITLE_KEYWORDS = List.of(
            "专题补充文档",
            "知识库专题补充文档",
            "路由建议",
            "问答增强版",
            "清洗导入版",
            "专题说明",
            "目录说明",
            "清单说明",
            "章节标题",
            "导语",
            "政策导航",
            "专题类政策导航"
    );
    private static final List<String> SECTION_HEADING_KEYWORDS = List.of(
            "专题定位与适用范围",
            "专题定位与适用问题",
            "适用范围",
            "适用问题",
            "申报条件",
            "申请条件",
            "支持标准",
            "申报流程",
            "办理流程",
            "申请材料",
            "材料清单",
            "政策依据"
    );
    private static final List<String> FORMAL_POLICY_TITLE_KEYWORDS = List.of(
            "实施意见",
            "实施方案",
            "管理办法",
            "若干措施",
            "申报通知",
            "通知",
            "公告",
            "通告",
            "细则",
            "意见"
    );
    private static final List<String> TOPIC_TITLE_HINT_KEYWORDS = List.of(
            "人才",
            "申报",
            "支持",
            "认定",
            "引才",
            "专项",
            "产业",
            "住房",
            "博士后",
            "台湾",
            "人工智能",
            "项目",
            "计划",
            "工程师"
    );
    private static final List<String> AI_SUMMARY_KEYWORDS = List.of("人工智能", "AI", "ai");
    private static final List<String> XM_SPECIAL_TOPICS = List.of(
        "双百计划",
        "特聘岗位",
        "专项资金",
        "群鹭兴厦",
        "博士后",
        "人工智能",
        "住房",
        "台湾人才",
        "台湾特聘专家",
        "服务保障",
        "人才服务",
        "人才服务卡",
        "绿色通道",
        "子女教育",
        "子女入学",
        "子女就学",
        "医疗保障",
        "医疗服务",
        "健康体检",
        "配偶就业",
        "落户服务"
);
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
if (containsAny(normalized, SERVICE_BENEFIT_KEYWORDS)) {
    return PolicyQuestionType.BENEFIT;
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
            return "route_help";
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
        if (containsAny(merged, List.of(
        "服务保障",
        "人才服务",
        "人才服务卡",
        "绿色通道",
        "一站式服务",
        "子女教育",
        "子女入学",
        "子女就学",
        "随迁子女",
        "医疗保障",
        "医疗服务",
        "健康体检",
        "就医",
        "配偶就业",
        "落户服务",
        "住房保障",
        "平台申报"
))) {
    return "service";
}
        if (containsAny(merged, List.of("退出机制", "追回", "撤销", "终止", "考核"))) {
            return "risk";
        }
        if (containsAny(merged, List.of("管理期", "服务期", "管理周期"))) {
            return "management_period";
        }
        if (containsAny(merged, List.of("资金拨付", "兑现申请", "分批拨付", "拨付", "兑现"))) {
            return "payment";
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
        if (List.of("process", "condition", "benefit", "service", "risk", "management_period", "payment", "faq", "routing", "route_help", "list").contains(normalized)) {
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

    public static String canonicalizePolicyName(String text) {
        String candidate = normalizeCatalogText(text);
        if (candidate == null) {
            return null;
        }
        for (PolicyAliasMapping mapping : POLICY_ALIAS_MAPPINGS) {
            if (containsKeyword(candidate, mapping.canonicalName())) {
                return mapping.canonicalName();
            }
            for (String alias : mapping.aliases()) {
                if (containsKeyword(candidate, alias) || containsKeyword(alias, candidate)) {
                    return mapping.canonicalName();
                }
            }
        }
        return candidate;
    }

    public static List<String> resolvePolicySearchTerms(String policyName) {
        LinkedHashSet<String> terms = new LinkedHashSet<>();
        String canonical = canonicalizePolicyName(policyName);
        if (canonical != null) {
            terms.add(canonical);
            String compact = canonical.replace("厦门市", "").replace("福建省", "").trim();
            if (!compact.isBlank()) {
                terms.add(compact);
            }
        }
        String normalized = normalize(policyName);
        if (normalized != null) {
            terms.add(normalized);
        }
        for (PolicyAliasMapping mapping : POLICY_ALIAS_MAPPINGS) {
            if (canonical == null || !mapping.canonicalName().equalsIgnoreCase(canonical)) {
                continue;
            }
            terms.addAll(mapping.aliases());
            break;
        }
        return new ArrayList<>(terms);
    }

    public static List<String> detectTopicTags(String... texts) {
        LinkedHashSet<String> tags = new LinkedHashSet<>();
        String merged = mergeText(texts);
        if (merged == null) {
            return List.of();
        }
        if (containsAny(merged, List.of("住房", "住房补贴", "安居", "租房", "购房"))) {
            tags.add("housing");
        }
        if (containsAny(merged, List.of("博士后"))) {
            tags.add("postdoc");
        }
        if (containsAny(merged, List.of("人工智能", "AI人才", "ai人才"))) {
            tags.add("ai");
        }
        if (containsAny(merged, List.of("台湾人才", "台湾特聘专家", "台湾专才"))) {
            tags.add("taiwan");
        }
        if (containsAny(merged, List.of("专项资金", "资金"))) {
            tags.add("funding");
        }
        if (containsAny(merged, List.of("双百计划"))) {
            tags.add("double-hundred");
        }
        if (containsAny(merged, List.of("百人计划"))) {
            tags.add("bai-ren");
        }
        if (containsAny(merged, List.of("四大经济"))) {
            tags.add("four-economies");
        }
        if (containsAny(merged, List.of(
        "服务保障",
        "人才服务",
        "人才服务卡",
        "绿色通道",
        "一站式服务",
        "子女教育",
        "子女入学",
        "子女就学",
        "随迁子女",
        "医疗保障",
        "医疗服务",
        "健康体检",
        "就医",
        "配偶就业",
        "落户服务"
))) {
    tags.add("service");
}
        if (containsAny(merged, List.of("产业人才项目", "骨干人才项目", "产业"))) {
            tags.add("industry");
        }
        return new ArrayList<>(tags);
    }

    public static boolean isInvalidCatalogName(String text) {
        String normalized = normalizeCatalogText(text);
        if (normalized == null) {
            return true;
        }
        if (isObviousNoiseCatalogTitle(normalized)) {
            return true;
        }
        return !looksLikeValidPolicyOrTopicTitle(normalized);
    }

    public static List<String> extractCatalogPolicyNames(String... texts) {
        LinkedHashSet<String> names = new LinkedHashSet<>();
        for (String text : texts) {
            names.addAll(extractFormalPolicyNames(text));
            String candidate = canonicalizePolicyName(extractPolicyName(text));
            if (candidate != null && !isInvalidCatalogName(candidate)) {
                names.add(candidate);
            }
        }
        return new ArrayList<>(names);
    }

    public static String detectCatalogPolicyGroup(String regionScope, String policyName, String headingText, String topicTags) {
        String canonical = canonicalizePolicyName(policyName);
        String merged = mergeText(canonical, headingText, topicTags);
        String region = normalize(regionScope);
        if (containsAny(canonical, List.of("住房"))) {
            return "住房";
        }
        if (containsAny(canonical, List.of("博士后"))) {
            return "博士后";
        }
        if (containsAny(canonical, List.of("台湾"))) {
            return "台湾人才";
        }
        if (containsAny(canonical, AI_SUMMARY_KEYWORDS)) {
            return "AI";
        }
        if ("FJ".equalsIgnoreCase(region) && containsAny(merged, List.of("百人计划"))) {
            return "百人计划";
        }
        if ("FJ".equalsIgnoreCase(region) && containsAny(merged, List.of("四大经济"))) {
            return "四大经济";
        }
        if ("FJ".equalsIgnoreCase(region) && containsAny(merged, List.of("申报", "通知", "近期"))) {
            return "近期申报";
        }
        if (containsAny(merged, List.of("双百计划", "特聘岗位", "专项资金", "意见", "统领"))) {
            return "XM".equalsIgnoreCase(region) ? "统领政策" : "省级主干政策";
        }
        if (containsAny(merged, List.of("教育人才", "卫生健康人才", "社会工作人才", "公共", "服务保障"))) {
            return "公共专项";
        }
        if (containsAny(merged, List.of("产业人才项目", "骨干人才项目", "产业项目"))) {
            return "重点产业项目";
        }
        return "FJ".equalsIgnoreCase(region) ? "专项支持" : "统领政策";
    }

    public static String buildCatalogShortSummary(String policyName, String topicTags, String fallbackText) {
        return buildCatalogShortSummary(policyName, topicTags, fallbackText, (String[]) null);
    }

    public static String buildCatalogShortSummary(String policyName, String topicTags, String fallbackText, String... contextTexts) {
        String canonical = canonicalizePolicyName(policyName);
        String nameDrivenContext = canonical;
        if (containsAny(nameDrivenContext, List.of("双百计划"))) {
            return "聚焦高层次创新创业人才引进、评审和支持安排。";
        }
        if (containsAny(nameDrivenContext, List.of("特聘岗位"))) {
            return "聚焦高层次人才特聘岗位设置、引进、管理和支持规则。";
        }
        if (containsAny(nameDrivenContext, List.of("专项资金"))) {
            return "聚焦高层次人才专项资金的拨付、使用和管理规则。";
        }
        if (containsAny(nameDrivenContext, List.of("住房", "住房补贴", "安居", "租房", "购房"))) {
            return "聚焦住房保障相关支持对象、条件和兑现安排。";
        }
        if (containsAny(nameDrivenContext, List.of("博士后"))) {
            return "聚焦博士后招收、培养、资助和平台建设安排。";
        }
        if (containsAny(canonical, AI_SUMMARY_KEYWORDS)) {
            return "聚焦人工智能领域人才引进、培养和专项支持措施。";
        }
        if (containsAny(nameDrivenContext, List.of("台湾"))) {
            return "聚焦台湾人才引进、支持和服务保障安排。";
        }
        if (containsAny(nameDrivenContext, List.of("产业人才项目", "骨干人才项目"))) {
            return "聚焦对应重点产业人才项目的申报、评审和资金支持。";
        }
        String normalizedFallback = sanitizeCatalogSummary(fallbackText);
        if (normalizedFallback != null) {
            return normalizedFallback;
        }
        return "属于当前知识库已沉淀的主要政策/项目目录项。";
    }

    public static boolean isCatalogRegionConsistent(String regionScope, String policyName, String aliasesCsv) {
        String normalizedRegion = normalize(regionScope);
        if (!"XM".equalsIgnoreCase(normalizedRegion) && !"FJ".equalsIgnoreCase(normalizedRegion)) {
            return true;
        }
        String primaryRegion = resolveCatalogPolicyNameRegion(policyName);
        if (primaryRegion != null && !normalizedRegion.equalsIgnoreCase(primaryRegion)) {
            return false;
        }
        for (String alias : splitCsv(aliasesCsv)) {
            String aliasRegion = resolveExplicitRegionPrefix(alias);
            if (aliasRegion != null && !normalizedRegion.equalsIgnoreCase(aliasRegion)) {
                return false;
            }
        }
        return true;
    }

    public static String sanitizeCatalogSummary(String text) {
        String normalized = normalizeCatalogText(text);
        if (normalized == null || isInvalidCatalogName(normalized)) {
            return null;
        }
        String cleaned = normalized.replace('\n', ' ').replace('\r', ' ').replaceAll("\\s+", " ").trim();
        if (cleaned.length() <= 48) {
            return cleaned;
        }
        String truncated = cleaned.substring(0, 48).trim();
        return truncated.endsWith("。") ? truncated : truncated + "。";
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
        String candidate = normalizeCatalogText(text);
        if (candidate != null && looksLikeValidPolicyOrTopicTitle(candidate) && !isInvalidCatalogName(candidate)) {
            return candidate.length() <= 120 ? candidate : candidate.substring(0, 120);
        }
        return null;
    }

    private static boolean startsWithAny(String text, List<String> prefixes) {
        String normalized = normalize(text);
        if (normalized == null || prefixes == null || prefixes.isEmpty()) {
            return false;
        }
        for (String prefix : prefixes) {
            String normalizedPrefix = normalize(prefix);
            if (normalizedPrefix != null && normalized.startsWith(normalizedPrefix)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isObviousNoiseCatalogTitle(String text) {
        String normalized = normalizeCatalogText(text);
        if (normalized == null) {
            return true;
        }
        if (containsAny(normalized, List.of(
                "问答增强版", "使用边界", "检索建议", "导入说明", "导入目录", "文档定位", "正文", "高频问答", "Chunk",
                "A类人才项目", "B类人才项目", "C类人才项目"
        ))) {
            return true;
        }
        if (startsWithAny(normalized, INVALID_CATALOG_PREFIXES) || containsAny(normalized, INVALID_CATALOG_PHRASES)) {
            return true;
        }
        if (looksLikeMetaCatalogTitle(normalized) || looksLikeObviousSectionHeading(normalized)) {
            return true;
        }
        return containsAny(normalized, List.of("。", "？", "?", "！", "!", "；", ";"))
                || normalized.contains("：")
                || normalized.contains(":")
                || normalized.contains("如需回答")
                || normalized.contains("适合作为")
                || normalized.contains("建议作为");
    }

    public static boolean looksLikeValidPolicyOrTopicTitle(String text) {
        String normalized = normalizeCatalogText(text);
        if (normalized == null) {
            return false;
        }
        if (normalized.length() > 60 || isObviousNoiseCatalogTitle(normalized)) {
            return false;
        }
        if (!extractFormalPolicyNames(normalized).isEmpty()) {
            return true;
        }
        if (containsAny(normalized, FORMAL_POLICY_TITLE_KEYWORDS)
                && (normalized.startsWith("关于")
                || normalized.startsWith("厦门市")
                || normalized.startsWith("福建省")
                || containsAny(normalized, List.of("人才", "引进", "支持", "集聚")))) {
            return true;
        }
        return normalized.endsWith("专题")
                && (normalized.startsWith("福建省")
                || normalized.startsWith("厦门市")
                || containsAny(normalized, XM_SPECIAL_TOPICS)
                || containsAny(normalized, FJ_SPECIAL_TOPICS)
                || containsAny(normalized, TOPIC_TITLE_HINT_KEYWORDS));
    }

    private static boolean looksLikeMetaCatalogTitle(String text) {
        String normalized = normalizeCatalogText(text);
        if (normalized == null) {
            return false;
        }
        return containsAny(normalized, META_CATALOG_TITLE_KEYWORDS)
                || "政策依据".equals(normalized)
                || "专题补充".equals(normalized)
                || "路由建议".equals(normalized)
                || "引用".equals(normalized);
    }

    private static boolean looksLikeObviousSectionHeading(String text) {
        String normalized = normalize(text);
        if (normalized == null) {
            return false;
        }
        String stripped = stripCatalogSerialPrefix(normalized);
        boolean numbered = !normalized.equals(stripped);
        if (!numbered) {
            return false;
        }
        return containsAny(stripped, SECTION_HEADING_KEYWORDS)
                || (stripped.endsWith("条件") && stripped.length() <= 8)
                || (stripped.endsWith("标准") && stripped.length() <= 8)
                || (stripped.endsWith("范围") && stripped.length() <= 8)
                || (stripped.endsWith("问题") && stripped.length() <= 8)
                || (stripped.endsWith("流程") && stripped.length() <= 8);
    }

    private static String resolveCatalogPolicyNameRegion(String text) {
        String normalized = canonicalizePolicyName(text);
        if (normalized == null) {
            return null;
        }
        return resolveExplicitRegionPrefix(normalized);
    }

    private static String resolveExplicitRegionPrefix(String text) {
        String normalized = normalizeCatalogText(text);
        if (normalized == null) {
            return null;
        }
        if (normalized.startsWith("厦门市")) {
            return "XM";
        }
        if (normalized.startsWith("福建省")) {
            return "FJ";
        }
        return null;
    }

    private static String stripCatalogSerialPrefix(String text) {
        String normalized = normalize(text);
        if (normalized == null) {
            return null;
        }
        String stripped = normalized;
        while (true) {
            Matcher matcher = CATALOG_SERIAL_PREFIX_PATTERN.matcher(stripped);
            if (!matcher.find()) {
                return stripped.trim();
            }
            String next = stripped.substring(matcher.end()).trim();
            if (next.equals(stripped) || next.isEmpty()) {
                return stripped.trim();
            }
            stripped = next;
        }
    }

    private static String normalizeCatalogText(String text) {
        String normalized = normalize(text);
        if (normalized == null) {
            return null;
        }
        return stripCatalogSerialPrefix(normalized.replace('\u3000', ' ')
                .replace("“", "")
                .replace("”", "")
                .replace('\n', ' ')
                .replace('\r', ' ')
                .trim()).replaceAll("\\s+", " ").trim();
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
