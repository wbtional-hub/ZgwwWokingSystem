package com.example.lecturesystem.modules.aiuserprofile.service;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Propagation;

/**
 * AI 用户画像服务 - 第一阶段最小闭环版
 *
 * 目标：
 * 1. 从用户消息中规则抽取画像
 * 2. 写入 ai_user_profile_item / ai_user_profile_habit
 * 3. 重建 ai_user_profile_summary
 * 4. 为 Agent Prompt 提供 prompt_summary
 *
 * 注意：
 * 1. 本类不修改考勤、周报、菜单、路由、登录等任何功能
 * 2. 本类不调用外部 AI
 * 3. 本类使用 JdbcTemplate，减少 Mapper/XML 文件数量，便于人工复制
 */
@Service
public class AiUserProfileService {

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_INACTIVE = "INACTIVE";
    private static final String BUSINESS_DOMAIN_POLICY = "POLICY_CONSULT";
    private static final String SOURCE_TYPE_RULE = "RULE";

    private final JdbcTemplate jdbcTemplate;

    public AiUserProfileService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 从用户消息中抽取画像并更新数据库。
     *
     * 接入位置建议：
     * AgentServiceImpl 中 userMessage 入库之后调用。
     */
@Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
public void extractAndUpdateFromMessage(Long userId,
                                            Long baseId,
                                            String sourceScene,
                                            Long sessionId,
                                            Long messageId,
                                            String messageText) {
        if (userId == null || isBlank(messageText)) {
            return;
        }

        String text = normalize(messageText);
        if (isBlank(text)) {
            return;
        }

        boolean changed = false;
ProfileSubjectContext subjectContext = analyzeProfileSubjectContext(text);
        // 手机端来源偏好，属于习惯，不依赖用户自述。
        if ("MOBILE_POLICY_CONSULTANT".equalsIgnoreCase(nullToEmpty(sourceScene))) {
            changed |= upsertHabit(
                    userId,
                    "device_preference",
                    "mobile",
                    "手机端使用",
                    "用户经常使用手机端咨询",
                    sourceScene,
                    text
            );
        }

  
        // 回答偏好可以直接抽取。
changed |= extractAnswerStyleHabit(userId, sourceScene, text);

// 同类问题频率：不管是否用户自述，都可以作为“习惯画像”统计。
changed |= extractQuestionFrequencyHabits(userId, sourceScene, text);

// 非用户自述问题，不写入“个人身份画像”。
// 例如：本科生可以申请什么政策？博士后补助多少？
// 是否允许写入“用户本人长期画像”。
// 规则：必须是用户本人明确自述，并且不是帮别人问、不是假设、不是企业主体泛问。
boolean selfStatement = isUserSelfStatement(text);
boolean allowPersistSelfProfile = selfStatement && subjectContext.allowPersistSelfProfile();

if (allowPersistSelfProfile) {
    changed |= extractEducation(userId, baseId, sourceScene, sessionId, messageId, text);
    changed |= extractGender(userId, baseId, sourceScene, sessionId, messageId, text);
    changed |= extractCity(userId, baseId, sourceScene, sessionId, messageId, text);
    changed |= extractEmploymentAndStartup(userId, baseId, sourceScene, sessionId, messageId, text);
    changed |= extractIndustry(userId, baseId, sourceScene, sessionId, messageId, text);
    changed |= extractAgeAndExperience(userId, baseId, sourceScene, sessionId, messageId, text);
    changed |= extractPolicyInterest(userId, baseId, sourceScene, sessionId, messageId, text);
    changed |= extractBenefitInterest(userId, baseId, sourceScene, sessionId, messageId, text);
} else {
    // 不是用户本人长期画像时，不写入身份事实。
    // 但仍然可以沉淀“问题类型、关注主题、沟通习惯”等低风险习惯画像。
    changed |= extractFrequentTopicHabit(userId, sourceScene, text);
}

        if (changed) {
            rebuildSummary(userId, baseId, sourceScene);
        }
    }

    /**
     * 构建注入 Prompt 的用户画像摘要。
     *
     * 接入位置建议：
     * 调用外部模型前调用。
     */
    public String buildPromptSummary(Long userId, Long baseId, String sourceScene) {
        if (userId == null) {
            return "";
        }

        Map<String, Object> summary = findSummary(userId, baseId, sourceScene);
        if (summary == null) {
            return "";
        }

        Object promptSummary = summary.get("prompt_summary");
        if (promptSummary == null || isBlank(String.valueOf(promptSummary))) {
            return "";
        }

        return String.valueOf(promptSummary).trim();
    }

public String buildPromptSummaryForQuestion(Long userId,
                                            Long baseId,
                                            String sourceScene,
                                            String question) {
    String baseSummary = buildPromptSummary(userId, baseId, sourceScene);
    String text = normalize(question);
    ProfileSubjectContext subjectContext = analyzeProfileSubjectContext(text);
    String directive = buildQuestionAwareProfileDirective(subjectContext);

    if (isBlank(baseSummary)) {
        return directive;
    }

    if (isBlank(directive)) {
        return baseSummary;
    }

    return baseSummary + "\n\n" + directive;
}
private String buildQuestionAwareProfileDirective(ProfileSubjectContext context) {
    if (context == null) {
        return "";
    }

    StringBuilder builder = new StringBuilder();
    builder.append("【本轮画像使用边界】\n");

    if ("SELF".equals(context.subjectScope())) {
        builder.append("本轮问题判断为用户本人咨询。可以结合用户长期画像辅助政策匹配，但仍应以本轮明确提供的信息和知识库依据为准。\n");
        builder.append("回答时不要机械说“根据你的画像”，应自然表达为“结合你前面提到的情况”。\n");
        return builder.toString();
    }

    if ("OTHER_PERSON".equals(context.subjectScope())) {
        builder.append("本轮问题判断为用户帮他人咨询。咨询对象关系：")
                .append(nullToEmpty(context.relationToUser()))
                .append("。\n");
        builder.append("不要把咨询对象的信息写成用户本人情况。\n");
        builder.append("不要用用户本人的长期事实画像替代咨询对象条件。\n");
        builder.append("用户长期画像只可用于沟通风格，例如回答长短、是否结论先行，不可用于政策资格判断。\n");
        builder.append("回答时应使用“如果是帮他/她咨询，可以先按咨询对象情况判断”这样的自然表达。\n");
        return builder.toString();
    }

    if ("HYPOTHETICAL".equals(context.subjectScope())) {
        builder.append("本轮问题判断为假设场景。\n");
        builder.append("不要把假设条件写成用户本人画像。\n");
        builder.append("不要把用户长期事实画像强行套入假设对象。\n");
        builder.append("回答时应围绕假设条件判断，例如“如果按博士后身份来看……”。\n");
        builder.append("用户长期画像只可用于沟通风格，不可用于政策资格判断。\n");
        return builder.toString();
    }

    if ("ORGANIZATION".equals(context.subjectScope())) {
        builder.append("本轮问题判断为企业或组织主体咨询。\n");
        builder.append("不要把企业情况写成用户个人画像。\n");
        builder.append("回答时应围绕单位性质、企业资质、行业方向、纳税社保、项目成果等组织条件展开。\n");
        builder.append("用户个人长期画像只可用于沟通风格，不可用于企业政策资格判断。\n");
        return builder.toString();
    }

    if ("GENERIC".equals(context.subjectScope())) {
        builder.append("本轮问题判断为泛问某类人群或某类政策。\n");
        builder.append("不要推断用户本人属于该人群。\n");
        builder.append("回答应按通用政策条件说明，并提示如果要判断本人是否符合，需要补充个人条件。\n");
        builder.append("用户长期画像只可用于沟通风格，不可直接套入政策资格判断。\n");
        return builder.toString();
    }

    builder.append("本轮咨询主体不明确。\n");
    builder.append("回答时不要过度使用长期画像做资格判断，应优先基于本轮问题和知识库依据。\n");
    builder.append("如需判断是否符合政策，应温和追问关键条件。\n");

    return builder.toString();
}
    /**
     * 查询当前用户画像，后续给前端“我的画像”页面使用。
     */
    public Map<String, Object> queryCurrentProfile(Long userId) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (userId == null) {
            result.put("items", Collections.emptyList());
            result.put("habits", Collections.emptyList());
            result.put("summary", null);
            return result;
        }

        List<Map<String, Object>> items = jdbcTemplate.queryForList(
        "SELECT id, profile_key, profile_domain, profile_cardinality, " +
                "value_code, value_text, display_label, confidence, " +
                "source_text, status, effective_at AS first_seen_at, last_seen_at, created_at, updated_at " +
                "FROM ai_user_profile_item " +
                "WHERE user_id = ? AND status = ? " +
                "ORDER BY profile_key, updated_at DESC",
        userId, STATUS_ACTIVE
);

        List<Map<String, Object>> habits = jdbcTemplate.queryForList(
        "SELECT id, habit_key, habit_domain, habit_value_code, habit_value_text, display_label, " +
                "hit_count, habit_score, source_text, status, first_seen_at, last_seen_at, created_at, updated_at " +
                "FROM ai_user_profile_habit " +
                "WHERE user_id = ? AND status = ? " +
                "ORDER BY habit_score DESC, last_seen_at DESC",
        userId, STATUS_ACTIVE
);

        Map<String, Object> summary = queryFirstMap(
                "SELECT id, user_id, base_id, business_domain, source_scene, " +
                        "profile_summary, habit_summary, prompt_summary, stale AS is_stale, created_at, updated_at " +
                        "FROM ai_user_profile_summary " +
                        "WHERE user_id = ? " +
                        "ORDER BY updated_at DESC LIMIT 1",
                userId
        );

        result.put("items", items);
        result.put("habits", habits);
        result.put("summary", summary);
        return result;
    }

    /**
     * 清空当前用户画像和习惯。
     */
    @Transactional(rollbackFor = Exception.class)
    public void clearUserProfile(Long userId) {
        if (userId == null) {
            return;
        }

        jdbcTemplate.update(
                "UPDATE ai_user_profile_item SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE user_id = ? AND status = ?",
                STATUS_INACTIVE, userId, STATUS_ACTIVE
        );

        jdbcTemplate.update(
                "UPDATE ai_user_profile_habit SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE user_id = ? AND status = ?",
                STATUS_INACTIVE, userId, STATUS_ACTIVE
        );

        jdbcTemplate.update(
        "UPDATE ai_user_profile_summary " +
                "SET profile_summary = '', " +
                "habit_summary = '', " +
                "prompt_summary = '', " +
                "stale = TRUE, " +
                "updated_at = CURRENT_TIMESTAMP " +
                "WHERE user_id = ?",
        userId
);

        writeChangeLog(userId, null, null, null, "CLEAR", null, null, "用户清空画像");
    }

    /**
     * 删除某条画像项，只允许删除自己的。
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteProfileItem(Long userId, Long itemId) {
        if (userId == null || itemId == null) {
            return;
        }

        Map<String, Object> old = queryFirstMap(
        "SELECT id, base_id, source_scene, profile_key, value_code, display_label " +
                "FROM ai_user_profile_item WHERE id = ? AND user_id = ?",
        itemId, userId
);

        jdbcTemplate.update(
                "UPDATE ai_user_profile_item SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ? AND user_id = ?",
                STATUS_INACTIVE, itemId, userId
        );

        writeChangeLog(
                userId,
                null,
                itemId,
                old == null ? null : stringValue(old.get("profile_key")),
                "DELETE",
                old == null ? null : stringValue(old.get("display_label")),
                null,
                "用户删除画像项"
        );
        if (old != null) {
    rebuildSummary(
            userId,
            longValue(old.get("base_id")),
            stringValue(old.get("source_scene"))
    );
}
    }

    private boolean extractEducation(Long userId,
                                     Long baseId,
                                     String sourceScene,
                                     Long sessionId,
                                     Long messageId,
                                     String text) {
        // 注意顺序：博士后必须放在博士前面。
        if (containsAny(text, "博士后", "在站博士后", "出站博士后")) {
            return upsertSingleProfile(userId, baseId, sourceScene, sessionId, messageId,
                    "education_level", "postdoc", "博士后", "博士后", text, new BigDecimal("0.90"));
        }

        if (containsAny(text, "博士研究生", "我是博士", "本人是博士", "博士毕业")) {
            return upsertSingleProfile(userId, baseId, sourceScene, sessionId, messageId,
                    "education_level", "doctor", "博士", "博士", text, new BigDecimal("0.90"));
        }

        if (containsAny(text, "硕士研究生", "我是研究生", "我是硕士", "研究生学历", "硕士毕业")) {
            return upsertSingleProfile(userId, baseId, sourceScene, sessionId, messageId,
                    "education_level", "master", "研究生", "研究生", text, new BigDecimal("0.90"));
        }

        if (containsAny(text,
        "我是本科生",
        "本科学历",
        "本科毕业",
        "我是本科",
        "本人本科",
        "学士学位",
        "学士",
        "大学本科",
        "本科",
        "大学毕业")) {
    return upsertSingleProfile(userId, baseId, sourceScene, sessionId, messageId,
            "education_level", "bachelor", "本科/学士", "本科/学士", text, new BigDecimal("0.90"));
}

        return false;
    }

    private boolean extractGender(Long userId,
                              Long baseId,
                              String sourceScene,
                              Long sessionId,
                              Long messageId,
                              String text) {
    if (isBlank(text)) {
        return false;
    }

    // 注意：性别只允许来自用户明确自述，不根据姓名、头像、语气、行业推断。
    if (containsAny(text,
            "我是男性",
            "我是男的",
            "本人男性",
            "本人男",
            "性别男",
            "性别:男",
            "性别：男",
            "男士")) {
        return upsertSingleProfile(userId, baseId, sourceScene, sessionId, messageId,
                "gender", "male", "男", "男", text, new BigDecimal("1.00"));
    }

    if (containsAny(text,
            "我是女性",
            "我是女的",
            "本人女性",
            "本人女",
            "性别女",
            "性别:女",
            "性别：女",
            "女士")) {
        return upsertSingleProfile(userId, baseId, sourceScene, sessionId, messageId,
                "gender", "female", "女", "女", text, new BigDecimal("1.00"));
    }

    return false;
}

    private boolean extractCity(Long userId,
                                Long baseId,
                                String sourceScene,
                                Long sessionId,
                                Long messageId,
                                String text) {
        boolean changed = false;

        if (containsAny(text, "我在厦门工作", "目前在厦门工作", "现在在厦门工作")) {
            changed |= upsertSingleProfile(userId, baseId, sourceScene, sessionId, messageId,
                    "work_city", "xiamen", "厦门", "厦门", text, new BigDecimal("0.90"));
            changed |= upsertSingleProfile(userId, baseId, sourceScene, sessionId, messageId,
                    "employment_status", "employed", "在职", "在职", text, new BigDecimal("0.85"));
        } else if (containsAny(text, "我在厦门", "本人在厦门", "目前在厦门", "现在在厦门")) {
            changed |= upsertSingleProfile(userId, baseId, sourceScene, sessionId, messageId,
                    "current_city", "xiamen", "厦门", "厦门", text, new BigDecimal("0.85"));
        }

        if (containsAny(text, "我想去厦门发展", "准备去厦门发展", "计划去厦门发展")) {
            changed |= upsertSingleProfile(userId, baseId, sourceScene, sessionId, messageId,
                    "target_city", "xiamen", "厦门", "厦门", text, new BigDecimal("0.85"));
        }

        return changed;
    }

    private boolean extractEmploymentAndStartup(Long userId,
                                                Long baseId,
                                                String sourceScene,
                                                Long sessionId,
                                                Long messageId,
                                                String text) {
        boolean changed = false;

        if (containsAny(text, "我在工作", "我已经就业", "我已就业", "目前在职", "现在在职")) {
            changed |= upsertSingleProfile(userId, baseId, sourceScene, sessionId, messageId,
                    "employment_status", "employed", "在职", "在职", text, new BigDecimal("0.85"));
        }

        if (containsAny(text, "我还是学生", "我是在校生", "目前在校", "现在是学生")) {
            changed |= upsertSingleProfile(userId, baseId, sourceScene, sessionId, messageId,
                    "employment_status", "student", "在校生", "在校生", text, new BigDecimal("0.85"));
        }

        if (containsAny(text, "我准备创业", "我想创业", "计划创业", "准备创办企业")) {
            changed |= upsertSingleProfile(userId, baseId, sourceScene, sessionId, messageId,
                    "startup_status", "planning_startup", "准备创业", "准备创业", text, new BigDecimal("0.85"));
        }

        if (containsAny(text, "我已经创业", "我已创业", "我创办企业", "我开了公司", "我成立了公司")) {
            changed |= upsertSingleProfile(userId, baseId, sourceScene, sessionId, messageId,
                    "startup_status", "startup", "已创业", "已创业", text, new BigDecimal("0.85"));
        }

        return changed;
    }

    private boolean extractIndustry(Long userId,
                                    Long baseId,
                                    String sourceScene,
                                    Long sessionId,
                                    Long messageId,
                                    String text) {
        boolean changed = false;

        changed |= extractMultiByKeywords(userId, baseId, sourceScene, sessionId, messageId, text,
                "industry_direction", "ai", "人工智能", "人工智能", "AI", "人工智能");

        changed |= extractMultiByKeywords(userId, baseId, sourceScene, sessionId, messageId, text,
                "industry_direction", "biomedicine", "生物医药", "生物医药", "生物医药");

        changed |= extractMultiByKeywords(userId, baseId, sourceScene, sessionId, messageId, text,
        "industry_direction", "software_information", "软件信息技术", "软件信息技术",
        "电子信息", "软件信息", "软件工程", "软件开发", "软件", "信息技术", "计算机", "互联网", "集成电路");

        changed |= extractMultiByKeywords(userId, baseId, sourceScene, sessionId, messageId, text,
                "industry_direction", "new_energy_material", "新能源新材料", "新能源新材料", "新能源", "新材料");

        changed |= extractMultiByKeywords(userId, baseId, sourceScene, sessionId, messageId, text,
                "industry_direction", "machinery", "机械装备", "机械装备", "机械装备");

        changed |= extractMultiByKeywords(userId, baseId, sourceScene, sessionId, messageId, text,
                "industry_direction", "marine", "海洋经济", "海洋经济", "海洋经济");

        changed |= extractMultiByKeywords(userId, baseId, sourceScene, sessionId, messageId, text,
                "industry_direction", "finance", "金融", "金融", "金融");

        changed |= extractMultiByKeywords(userId, baseId, sourceScene, sessionId, messageId, text,
                "industry_direction", "trade_logistics", "商贸物流", "商贸物流", "商贸物流");

        changed |= extractMultiByKeywords(userId, baseId, sourceScene, sessionId, messageId, text,
                "industry_direction", "culture_tourism", "文旅创意", "文旅创意", "文旅", "文旅创意");

        changed |= extractMultiByKeywords(userId, baseId, sourceScene, sessionId, messageId, text,
                "industry_direction", "education", "教育", "教育", "教育");

        changed |= extractMultiByKeywords(userId, baseId, sourceScene, sessionId, messageId, text,
                "industry_direction", "health", "卫生健康", "卫生健康", "医疗", "卫生健康");

        changed |= extractMultiByKeywords(userId, baseId, sourceScene, sessionId, messageId, text,
                "industry_direction", "social_work", "社会工作", "社会工作", "社会工作", "社工");

        return changed;
    }
private boolean extractAgeAndExperience(Long userId,
                                        Long baseId,
                                        String sourceScene,
                                        Long sessionId,
                                        Long messageId,
                                        String text) {
    boolean changed = false;

    Integer age = extractFirstIntegerBeforeKeyword(text, "岁");
    if (age != null && age >= 16 && age <= 80) {
        changed |= upsertSingleProfile(userId, baseId, sourceScene, sessionId, messageId,
                "age_range",
                resolveAgeRangeCode(age),
                resolveAgeRangeText(age),
                resolveAgeRangeText(age),
                text,
                new BigDecimal("0.80"));
    }

    Integer years = extractFirstIntegerBeforeKeyword(text, "年");
    if (years != null && years >= 1 && years <= 60 && containsAny(text, "从业", "工作", "经验", "软件")) {
        changed |= upsertSingleProfile(userId, baseId, sourceScene, sessionId, messageId,
                "work_experience",
                resolveWorkExperienceCode(years),
                resolveWorkExperienceText(years),
                resolveWorkExperienceText(years),
                text,
                new BigDecimal("0.82"));
    }

    return changed;
}
    private boolean extractPolicyInterest(Long userId,
                                          Long baseId,
                                          String sourceScene,
                                          Long sessionId,
                                          Long messageId,
                                          String text) {
        boolean changed = false;

        changed |= extractMultiByKeywords(userId, baseId, sourceScene, sessionId, messageId, text,
                "policy_interest", "double_hundred", "双百计划", "双百计划", "双百计划");

        changed |= extractMultiByKeywords(userId, baseId, sourceScene, sessionId, messageId, text,
                "policy_interest", "special_post", "特聘岗位", "特聘岗位", "特聘岗位");

        changed |= extractMultiByKeywords(userId, baseId, sourceScene, sessionId, messageId, text,
                "policy_interest", "special_fund", "专项资金", "专项资金", "专项资金", "创业资金");

        changed |= extractMultiByKeywords(userId, baseId, sourceScene, sessionId, messageId, text,
                "policy_interest", "housing", "住房补贴", "住房补贴", "住房补贴", "住房", "安居");

        changed |= extractMultiByKeywords(userId, baseId, sourceScene, sessionId, messageId, text,
                "policy_interest", "postdoc", "博士后", "博士后", "博士后");

        changed |= extractMultiByKeywords(userId, baseId, sourceScene, sessionId, messageId, text,
                "policy_interest", "ai_talent", "AI人才专项", "AI人才专项", "AI人才", "人工智能人才");

        changed |= extractMultiByKeywords(userId, baseId, sourceScene, sessionId, messageId, text,
                "policy_interest", "taiwan_talent", "台湾人才", "台湾人才", "台湾特聘", "台湾人才");

        changed |= extractMultiByKeywords(userId, baseId, sourceScene, sessionId, messageId, text,
                "policy_interest", "fujian_bairen", "福建省百人计划", "福建省百人计划", "福建省百人计划", "百人计划");

        return changed;
    }

    private boolean extractBenefitInterest(Long userId,
                                           Long baseId,
                                           String sourceScene,
                                           Long sessionId,
                                           Long messageId,
                                           String text) {
        boolean changed = false;

        changed |= extractMultiByKeywords(userId, baseId, sourceScene, sessionId, messageId, text,
                "benefit_interest", "subsidy", "补助奖励", "补助奖励", "补助", "补贴", "奖励");

        changed |= extractMultiByKeywords(userId, baseId, sourceScene, sessionId, messageId, text,
                "benefit_interest", "housing", "住房保障", "住房保障", "住房", "安居", "租房", "购房");

        changed |= extractMultiByKeywords(userId, baseId, sourceScene, sessionId, messageId, text,
                "benefit_interest", "children_education", "子女教育", "子女教育", "子女教育");

        changed |= extractMultiByKeywords(userId, baseId, sourceScene, sessionId, messageId, text,
                "benefit_interest", "medical", "医疗保障", "医疗保障", "医疗保障", "医疗");

        changed |= extractMultiByKeywords(userId, baseId, sourceScene, sessionId, messageId, text,
                "benefit_interest", "startup_fund", "创业资金", "创业资金", "创业资金", "创业扶持");

        changed |= extractMultiByKeywords(userId, baseId, sourceScene, sessionId, messageId, text,
                "benefit_interest", "material_prepare", "材料准备", "材料准备", "材料", "清单");

        changed |= extractMultiByKeywords(userId, baseId, sourceScene, sessionId, messageId, text,
                "benefit_interest", "process", "申报流程", "申报流程", "流程", "怎么申请", "如何申请");

        changed |= extractMultiByKeywords(userId, baseId, sourceScene, sessionId, messageId, text,
                "benefit_interest", "condition", "申报条件", "申报条件", "条件", "能不能申请", "是否符合");

        return changed;
    }

    private boolean extractAnswerStyleHabit(Long userId, String sourceScene, String text) {
        boolean changed = false;

        if (containsAny(text, "详细一点", "全面一点", "深入分析", "讲详细", "说详细")) {
            changed |= upsertHabit(userId, "answer_style", "detailed", "详细回答", "希望回答详细全面", sourceScene, text);
        }

        if (containsAny(text, "简单一点", "简洁一点", "直接告诉我", "简单说", "简要说明")) {
            changed |= upsertHabit(userId, "answer_style", "brief", "简洁回答", "希望回答简洁直接", sourceScene, text);
        }

        if (containsAny(text, "一步一步", "按步骤", "慢慢来", "分步骤", "一步步")) {
            changed |= upsertHabit(userId, "answer_style", "step_by_step", "分步骤回答", "希望按步骤说明", sourceScene, text);
        }

        if (containsAny(text, "用表格", "列表对比", "做个表", "表格对比")) {
            changed |= upsertHabit(userId, "answer_style", "table", "表格化回答", "希望用表格或列表对比", sourceScene, text);
        }

        return changed;
    }

    private boolean extractFrequentTopicHabit(Long userId, String sourceScene, String text) {
        boolean changed = false;

        if (containsAny(text, "双百计划")) {
            changed |= upsertHabit(userId, "frequent_topic", "double_hundred", "双百计划", "频繁咨询双百计划", sourceScene, text);
        }

        if (containsAny(text, "博士后")) {
            changed |= upsertHabit(userId, "frequent_topic", "postdoc", "博士后", "频繁咨询博士后政策", sourceScene, text);
        }

        if (containsAny(text, "住房补贴", "住房", "安居")) {
            changed |= upsertHabit(userId, "frequent_topic", "housing", "住房政策", "频繁咨询住房政策", sourceScene, text);
        }

        if (containsAny(text, "AI人才", "人工智能人才")) {
            changed |= upsertHabit(userId, "frequent_topic", "ai_talent", "AI人才专项", "频繁咨询AI人才政策", sourceScene, text);
        }

        return changed;
    }

    private boolean extractQuestionFrequencyHabits(Long userId,
                                               String sourceScene,
                                               String text) {
    boolean changed = false;

    // 高频政策/权益主题
    if (containsAny(text, "双百计划")) {
        changed |= upsertHabit(userId,
                "repeated_policy_interest",
                "double_hundred",
                "双百计划",
                "用户多次关注双百计划",
                sourceScene,
                text);
    }

    if (containsAny(text, "住房补贴", "住房保障", "安居", "租房补贴", "购房补贴")) {
        changed |= upsertHabit(userId,
                "repeated_policy_interest",
                "housing",
                "住房补贴/安居政策",
                "用户多次关注住房补贴或安居政策",
                sourceScene,
                text);
    }

    if (containsAny(text, "AI人才", "人工智能人才", "AI人才专项", "人工智能")) {
        changed |= upsertHabit(userId,
                "repeated_policy_interest",
                "ai_talent",
                "AI人才专项",
                "用户多次关注AI人才或人工智能相关政策",
                sourceScene,
                text);
    }

    if (containsAny(text, "博士后")) {
        changed |= upsertHabit(userId,
                "repeated_policy_interest",
                "postdoc",
                "博士后政策",
                "用户多次关注博士后政策",
                sourceScene,
                text);
    }

    if (containsAny(text, "创业", "创业扶持", "创业资金", "创办企业")) {
        changed |= upsertHabit(userId,
                "repeated_policy_interest",
                "startup",
                "创业扶持",
                "用户多次关注创业扶持政策",
                sourceScene,
                text);
    }

    // 高频问题类型
    if (containsAny(text,
            "可以申请",
            "能申请",
            "能不能申请",
            "能否申请",
            "是否符合",
            "符合条件",
            "有没有资格",
            "可以申报")) {
        changed |= upsertHabit(userId,
                "frequent_question_type",
                "qualification",
                "资格判断",
                "用户经常咨询自己是否符合申报条件",
                sourceScene,
                text);
    }

    if (containsAny(text,
            "怎么申请",
            "如何申请",
            "申请流程",
            "申报流程",
            "流程是什么",
            "办理流程",
            "步骤")) {
        changed |= upsertHabit(userId,
                "frequent_question_type",
                "process",
                "申报流程",
                "用户经常咨询政策申报流程",
                sourceScene,
                text);
    }

    if (containsAny(text,
            "材料",
            "材料清单",
            "需要准备什么",
            "准备哪些材料",
            "申报材料")) {
        changed |= upsertHabit(userId,
                "frequent_question_type",
                "materials",
                "材料准备",
                "用户经常咨询申报材料准备",
                sourceScene,
                text);
    }

    if (containsAny(text,
            "多少钱",
            "补贴多少",
            "补助多少",
            "奖励多少",
            "金额",
            "标准")) {
        changed |= upsertHabit(userId,
                "frequent_question_type",
                "amount",
                "补贴金额",
                "用户经常咨询补贴金额或奖励标准",
                sourceScene,
                text);
    }

    if (containsAny(text,
            "什么时候",
            "申报时间",
            "截止时间",
            "时间节点",
            "截止",
            "几月")) {
        changed |= upsertHabit(userId,
                "frequent_question_type",
                "time",
                "申报时间",
                "用户经常咨询申报时间节点",
                sourceScene,
                text);
    }
if (containsAny(text,
        "直接告诉我",
        "给结论",
        "结论是什么",
        "简单说",
        "简洁一点")) {
    changed |= upsertHabit(userId,
            "communication_style",
            "conclusion_first",
            "结论先行",
            "用户偏好先给结论、再解释原因",
            sourceScene,
            text);
}

if (containsAny(text,
        "详细一点",
        "展开说",
        "讲细一点",
        "全面一点",
        "深入分析")) {
    changed |= upsertHabit(userId,
            "communication_style",
            "detailed_explanation",
            "详细解释",
            "用户偏好较完整、较细致的解释",
            sourceScene,
            text);
}

if (containsAny(text,
        "一步一步",
        "按步骤",
        "流程",
        "步骤",
        "怎么操作")) {
    changed |= upsertHabit(userId,
            "communication_style",
            "step_by_step",
            "分步骤说明",
            "用户偏好按步骤说明操作路径",
            sourceScene,
            text);
}
    return changed;
}

    private boolean extractMultiByKeywords(Long userId,
                                           Long baseId,
                                           String sourceScene,
                                           Long sessionId,
                                           Long messageId,
                                           String text,
                                           String profileKey,
                                           String valueCode,
                                           String valueText,
                                           String displayLabel,
                                           String... keywords) {
        if (!containsAny(text, keywords)) {
            return false;
        }

        return upsertMultiProfile(
                userId,
                baseId,
                sourceScene,
                sessionId,
                messageId,
                profileKey,
                valueCode,
                valueText,
                displayLabel,
                text,
                new BigDecimal("0.80")
        );
    }

    private boolean upsertSingleProfile(Long userId,
                                        Long baseId,
                                        String sourceScene,
                                        Long sessionId,
                                        Long messageId,
                                        String profileKey,
                                        String valueCode,
                                        String valueText,
                                        String displayLabel,
                                        String sourceText,
                                        BigDecimal confidence) {
        Map<String, Object> existing = queryFirstMap(
                "SELECT id, value_code, display_label FROM ai_user_profile_item " +
                        "WHERE user_id = ? AND profile_key = ? AND status = ? " +
                        "ORDER BY updated_at DESC LIMIT 1",
                userId, profileKey, STATUS_ACTIVE
        );

        if (existing != null && valueCode.equals(stringValue(existing.get("value_code")))) {
            Long id = longValue(existing.get("id"));
            jdbcTemplate.update(
                    "UPDATE ai_user_profile_item " +
                            "SET confidence = ?, source_text = ?, source_scene = ?, source_session_id = ?, " +
                            "source_message_id = ?, last_seen_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP " +
                            "WHERE id = ?",
                    confidence, sourceText, sourceScene, sessionId, messageId, id
            );

            writeChangeLog(userId, null, id, profileKey, "UPDATE",
                    stringValue(existing.get("display_label")), displayLabel, sourceText);
            return true;
        }

        if (existing != null) {
            Long oldId = longValue(existing.get("id"));
            jdbcTemplate.update(
                    "UPDATE ai_user_profile_item SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                    STATUS_INACTIVE, oldId
            );

            writeChangeLog(userId, null, oldId, profileKey, "INACTIVE",
                    stringValue(existing.get("display_label")), null, sourceText);
        }

        Long newId = insertProfileItem(
                userId,
                baseId,
                sourceScene,
                sessionId,
                messageId,
                profileKey,
                valueCode,
                valueText,
                displayLabel,
                sourceText,
                confidence
        );

        writeChangeLog(userId, null, newId, profileKey, "CREATE",
                null, displayLabel, sourceText);
        return true;
    }

    private boolean upsertMultiProfile(Long userId,
                                       Long baseId,
                                       String sourceScene,
                                       Long sessionId,
                                       Long messageId,
                                       String profileKey,
                                       String valueCode,
                                       String valueText,
                                       String displayLabel,
                                       String sourceText,
                                       BigDecimal confidence) {
        Map<String, Object> existing = queryFirstMap(
                "SELECT id, display_label FROM ai_user_profile_item " +
                        "WHERE user_id = ? AND profile_key = ? AND value_code = ? AND status = ? " +
                        "ORDER BY updated_at DESC LIMIT 1",
                userId, profileKey, valueCode, STATUS_ACTIVE
        );

        if (existing != null) {
            Long id = longValue(existing.get("id"));
            jdbcTemplate.update(
                    "UPDATE ai_user_profile_item " +
                            "SET confidence = ?, source_text = ?, source_scene = ?, source_session_id = ?, " +
                            "source_message_id = ?, last_seen_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP " +
                            "WHERE id = ?",
                    confidence, sourceText, sourceScene, sessionId, messageId, id
            );

            writeChangeLog(userId, null, id, profileKey, "UPDATE",
                    stringValue(existing.get("display_label")), displayLabel, sourceText);
            return true;
        }

        Long newId = insertProfileItem(
                userId,
                baseId,
                sourceScene,
                sessionId,
                messageId,
                profileKey,
                valueCode,
                valueText,
                displayLabel,
                sourceText,
                confidence
        );

        writeChangeLog(userId, null, newId, profileKey, "CREATE",
                null, displayLabel, sourceText);
        return true;
    }

   private Long insertProfileItem(Long userId,
                               Long baseId,
                               String sourceScene,
                               Long sessionId,
                               Long messageId,
                               String profileKey,
                               String valueCode,
                               String valueText,
                               String displayLabel,
                               String sourceText,
                               BigDecimal confidence) {
    return jdbcTemplate.queryForObject(
            "INSERT INTO ai_user_profile_item " +
                    "(user_id, base_id, business_domain, source_scene, profile_key, profile_domain, profile_cardinality, " +
                    "value_code, value_text, display_label, confidence, extraction_method, source_text, " +
                    "source_session_id, source_message_id, status, effective_at, last_seen_at, created_at, updated_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, " +
                    "CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) " +
                    "RETURNING id",
            Long.class,
            userId,
            baseId,
            BUSINESS_DOMAIN_POLICY,
            sourceScene,
            profileKey,
            resolveProfileDomain(profileKey),
            resolveProfileCardinality(profileKey),
            valueCode,
            valueText,
            displayLabel,
            confidence,
            SOURCE_TYPE_RULE,
            sourceText,
            sessionId,
            messageId,
            STATUS_ACTIVE
    );
}
private String resolveProfileDomain(String profileKey) {
    String key = profileKey == null ? "" : profileKey.trim();

    return switch (key) {
        case "education_level" -> "education";
        case "gender", "age_range" -> "basic";
        case "current_city", "work_city", "target_city" -> "location";
        case "employment_status", "startup_status", "work_experience" -> "career";
        case "industry_direction" -> "industry";
        case "policy_interest" -> "policy";
        case "benefit_interest" -> "benefit";
        default -> "basic";
    };
}

private String resolveProfileCardinality(String profileKey) {
    String key = profileKey == null ? "" : profileKey.trim();

    return switch (key) {
        case "industry_direction", "policy_interest", "benefit_interest" -> "MULTI";
        default -> "SINGLE";
    };
}
    private boolean upsertHabit(Long userId,
                                String habitKey,
                                String habitValueCode,
                                String displayLabel,
                                String habitValueText,
                                String sourceScene,
                                String sourceText) {
        Map<String, Object> existing = queryFirstMap(
                "SELECT id, display_label FROM ai_user_profile_habit " +
                        "WHERE user_id = ? AND habit_key = ? AND habit_value_code = ? AND status = ? " +
                        "ORDER BY updated_at DESC LIMIT 1",
                userId, habitKey, habitValueCode, STATUS_ACTIVE
        );

        if (existing != null) {
            Long id = longValue(existing.get("id"));
            jdbcTemplate.update(
        "UPDATE ai_user_profile_habit " +
                "SET habit_domain = COALESCE(habit_domain, ?), " +
                "hit_count = COALESCE(hit_count, 0) + 1, " +
                "habit_score = COALESCE(habit_score, 0) + 1, " +
                "source_text = ?, source_scene = ?, last_seen_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP " +
                "WHERE id = ?",
        resolveHabitDomain(habitKey), sourceText, sourceScene, id
);

            writeChangeLog(userId, id, null, habitKey, "HABIT_UPDATE",
                    stringValue(existing.get("display_label")), displayLabel, sourceText);
            return true;
        }

        Long newId = jdbcTemplate.queryForObject(
        "INSERT INTO ai_user_profile_habit " +
                "(user_id, business_domain, source_scene, habit_key, habit_domain, habit_value_code, habit_value_text, display_label, " +
                "habit_score, hit_count, confidence, source_text, status, " +
                "first_seen_at, last_seen_at, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 1, 1, 1.00, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) " +
                "RETURNING id",
        Long.class,
        userId,
        BUSINESS_DOMAIN_POLICY,
        sourceScene,
        habitKey,
        resolveHabitDomain(habitKey),
        habitValueCode,
        habitValueText,
        displayLabel,
        sourceText,
        STATUS_ACTIVE
);

        writeChangeLog(userId, newId, null, habitKey, "HABIT_CREATE",
                null, displayLabel, sourceText);
        return true;
    }

    private void rebuildSummary(Long userId, Long baseId, String sourceScene) {
        List<Map<String, Object>> items = jdbcTemplate.queryForList(
                "SELECT profile_key, value_code, value_text, display_label " +
                        "FROM ai_user_profile_item " +
                        "WHERE user_id = ? AND status = ? " +
                        "ORDER BY profile_key, updated_at DESC",
                userId, STATUS_ACTIVE
        );

        List<Map<String, Object>> habits = jdbcTemplate.queryForList(
                "SELECT habit_key, habit_value_code, habit_value_text, display_label, hit_count, habit_score " +
                        "FROM ai_user_profile_habit " +
                        "WHERE user_id = ? AND status = ? " +
                        "ORDER BY habit_score DESC, last_seen_at DESC",
                userId, STATUS_ACTIVE
        );

        String profileSummary = buildProfileSummaryText(items);
        String habitSummary = buildHabitSummaryText(habits);
        String promptSummary = buildPromptSummaryText(profileSummary, habitSummary);

        Map<String, Object> existing = findSummary(userId, baseId, sourceScene);

        if (existing == null) {
            jdbcTemplate.update(
                    "INSERT INTO ai_user_profile_summary " +
        "(user_id, base_id, business_domain, source_scene, profile_summary, habit_summary, prompt_summary, " +
        "summary_version, stale, created_at, updated_at) " +
        "VALUES (?, ?, ?, ?, ?, ?, ?, 1, FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                    userId,
                    baseId,
                    BUSINESS_DOMAIN_POLICY,
                    sourceScene,
                    profileSummary,
                    habitSummary,
                    promptSummary
            );
        } else {
            Long id = longValue(existing.get("id"));
            jdbcTemplate.update(
                    "UPDATE ai_user_profile_summary " +
        "SET profile_summary = ?, habit_summary = ?, prompt_summary = ?, " +
        "summary_version = COALESCE(summary_version, 0) + 1, stale = FALSE, updated_at = CURRENT_TIMESTAMP " +
        "WHERE id = ?",
                    profileSummary,
                    habitSummary,
                    promptSummary,
                    id
            );
        }

        writeChangeLog(userId, null, null, "summary", "SUMMARY_REBUILD",
                null, "画像摘要已重建", promptSummary);
    }

    private Map<String, Object> findSummary(Long userId, Long baseId, String sourceScene) {
        Map<String, Object> byExact = queryFirstMap(
                "SELECT id, prompt_summary FROM ai_user_profile_summary " +
                        "WHERE user_id = ? AND (base_id = ? OR ? IS NULL) " +
                        "AND COALESCE(source_scene, '') = COALESCE(?, '') " +
                        "ORDER BY updated_at DESC LIMIT 1",
                userId, baseId, baseId, sourceScene
        );

        if (byExact != null) {
            return byExact;
        }

        return queryFirstMap(
                "SELECT id, prompt_summary FROM ai_user_profile_summary " +
                        "WHERE user_id = ? " +
                        "ORDER BY updated_at DESC LIMIT 1",
                userId
        );
    }

    private String buildProfileSummaryText(List<Map<String, Object>> items) {
        if (items == null || items.isEmpty()) {
            return "";
        }

        Map<String, List<String>> grouped = new LinkedHashMap<>();
        for (Map<String, Object> item : items) {
            String key = stringValue(item.get("profile_key"));
            String label = stringValue(item.get("display_label"));
            if (isBlank(key) || isBlank(label)) {
                continue;
            }
            grouped.computeIfAbsent(key, k -> new ArrayList<>());
            if (!grouped.get(key).contains(label)) {
                grouped.get(key).add(label);
            }
        }

        List<String> parts = new ArrayList<>();

        addProfileLine(parts, grouped, "education_level", "学历/身份");
        addProfileLine(parts, grouped, "gender", "性别");
addProfileLine(parts, grouped, "age_range", "年龄阶段");
addProfileLine(parts, grouped, "current_city", "当前所在地");
addProfileLine(parts, grouped, "work_city", "工作城市");
addProfileLine(parts, grouped, "target_city", "目标发展城市");
addProfileLine(parts, grouped, "employment_status", "工作状态");
addProfileLine(parts, grouped, "work_experience", "从业经验");
addProfileLine(parts, grouped, "startup_status", "创业状态");
addProfileLine(parts, grouped, "industry_direction", "行业方向");
addProfileLine(parts, grouped, "policy_interest", "关注政策");
addProfileLine(parts, grouped, "benefit_interest", "关注权益");

        return String.join("\n", parts);
    }

    private String buildHabitSummaryText(List<Map<String, Object>> habits) {
        if (habits == null || habits.isEmpty()) {
            return "";
        }

        Map<String, List<String>> grouped = new LinkedHashMap<>();
        for (Map<String, Object> habit : habits) {
            String key = stringValue(habit.get("habit_key"));
            String label = stringValue(habit.get("display_label"));
            if (isBlank(key) || isBlank(label)) {
                continue;
            }
            grouped.computeIfAbsent(key, k -> new ArrayList<>());
            if (!grouped.get(key).contains(label)) {
                grouped.get(key).add(label);
            }
        }

        List<String> parts = new ArrayList<>();

        addProfileLine(parts, grouped, "answer_style", "回答偏好");
addProfileLine(parts, grouped, "device_preference", "使用习惯");
addProfileLine(parts, grouped, "frequent_topic", "常问主题");
addProfileLine(parts, grouped, "repeated_policy_interest", "反复关注政策");
addProfileLine(parts, grouped, "frequent_question_type", "高频问题类型");
addProfileLine(parts, grouped, "communication_style", "沟通风格");

        return String.join("\n", parts);
    }

    private String buildPromptSummaryText(String profileSummary, String habitSummary) {
    if (isBlank(profileSummary) && isBlank(habitSummary)) {
        return "";
    }

    StringBuilder builder = new StringBuilder();

    builder.append("【用户画像上下文】\n");
    builder.append("以下信息来自用户历史咨询、明确自述或使用习惯，仅用于提升政策咨询的个性化理解，不能作为最终资格认定依据。\n\n");

    if (!isBlank(profileSummary)) {
        builder.append("一、已知用户基础画像：\n");
        builder.append(profileSummary).append("\n\n");
    }

    if (!isBlank(habitSummary)) {
        builder.append("二、已观察到的咨询习惯：\n");
        builder.append(habitSummary).append("\n\n");
    }

    builder.append("三、画像使用边界：\n");
    builder.append("1. 本轮用户明确提供的信息优先于历史画像。\n");
    builder.append("2. 如果用户是在帮别人咨询，例如女儿、朋友、客户、员工、同事，不要把咨询对象信息当作用户本人画像。\n");
    builder.append("3. 如果用户是在假设，例如“如果我是、假如、比如”，不要把假设条件当作用户本人情况。\n");
    builder.append("4. 如果用户只是泛问“本科生可以申请什么政策”，不要推断用户本人就是本科生。\n");
    builder.append("5. 使用画像时要自然表达，不要机械说“根据你的画像”。\n");
    builder.append("6. 对政策资格判断必须严谨，画像只能辅助初判，不能替代政策原文条件。\n");
    builder.append("7. 如果知识库没有明确依据，应直接说明未命中依据，不能编造。\n\n");

    builder.append("四、沟通与认知策略：\n");
    builder.append("1. 先理解用户真实需求：他可能是在确认能不能申请、需要哪些材料、下一步怎么做。\n");
    builder.append("2. 如果用户高频咨询“资格判断”，回答要先给初步判断，再说明依据和缺失条件。\n");
    builder.append("3. 如果用户长期关注某类政策，应优先围绕该方向给出更具体的匹配建议。\n");
    builder.append("4. 如果用户使用手机端，应尽量分段清楚、结论前置、避免过长政策堆砌。\n");
    builder.append("5. 如果用户信息不完整，应温和追问关键条件，不要直接否定用户。\n");
    builder.append("6. 回答要有温度：先给方向，再说明风险，再给下一步行动建议。\n\n");

    builder.append("五、推荐回答结构：\n");
    builder.append("1. 一句话结论：先告诉用户大致方向。\n");
    builder.append("2. 个性化判断：结合本轮信息和已知画像说明原因。\n");
    builder.append("3. 政策方向：列出可能适配的政策类别或项目。\n");
    builder.append("4. 缺失条件：说明还需要补充哪些信息才能进一步判断。\n");
    builder.append("5. 下一步建议：告诉用户应该先查什么、准备什么、或继续补充什么。\n");

    return builder.toString();
}

    private void addProfileLine(List<String> parts,
                                Map<String, List<String>> grouped,
                                String key,
                                String title) {
        List<String> values = grouped.get(key);
        if (values == null || values.isEmpty()) {
            return;
        }

        parts.add("- " + title + "：" + String.join("、", values));
    }

private ProfileSubjectContext analyzeProfileSubjectContext(String text) {
    if (isBlank(text)) {
        return new ProfileSubjectContext(
                "UNKNOWN",
                null,
                false,
                false,
                false,
                false,
                "空文本"
        );
    }

    boolean hypothetical = containsAny(text,
            "如果我是",
            "假如我是",
            "要是我是",
            "假设我是",
            "比如我是",
            "如果他是",
            "假如他是",
            "假设一个",
            "举个例子");

    if (hypothetical) {
        return new ProfileSubjectContext(
                "HYPOTHETICAL",
                null,
                true,
                false,
                false,
                false,
                "用户在提出假设场景"
        );
    }

    String relation = resolveProxyRelation(text);
    if (!isBlank(relation)) {
        return new ProfileSubjectContext(
                "OTHER_PERSON",
                relation,
                false,
                true,
                false,
                false,
                "用户在帮他人咨询"
        );
    }

    boolean organizationQuestion = containsAny(text,
            "我们公司",
            "我司",
            "我们单位",
            "本单位",
            "我们企业",
            "公司想申请",
            "单位想申请",
            "企业可以申请",
            "我们团队");

    if (organizationQuestion) {
        return new ProfileSubjectContext(
                "ORGANIZATION",
                null,
                false,
                false,
                true,
                false,
                "用户在咨询组织或企业情况"
        );
    }

    boolean genericQuestion = isGenericProfileQuestion(text);
    if (genericQuestion) {
        return new ProfileSubjectContext(
                "GENERIC",
                null,
                false,
                false,
                false,
                true,
                "用户在泛问某类人群政策"
        );
    }

    if (isUserSelfStatement(text)) {
        return new ProfileSubjectContext(
                "SELF",
                null,
                false,
                false,
                false,
                false,
                "用户明确自述本人情况"
        );
    }

    return new ProfileSubjectContext(
            "UNKNOWN",
            null,
            false,
            false,
            false,
            false,
            "无法明确判断咨询主体"
    );
}
private String resolveProxyRelation(String text) {
    if (isBlank(text)) {
        return null;
    }

    if (containsAny(text, "我女儿", "女儿", "孩子", "小孩", "我儿子", "儿子")) {
        return "child";
    }

    if (containsAny(text, "我朋友", "朋友", "同学")) {
        return "friend";
    }

    if (containsAny(text, "我同事", "同事", "员工", "下属")) {
        return "colleague_or_employee";
    }

    if (containsAny(text, "客户", "服务对象", "咨询对象")) {
        return "customer";
    }

    if (containsAny(text, "我爱人", "我老婆", "我老公", "配偶")) {
        return "spouse";
    }

    if (containsAny(text, "父亲", "母亲", "爸爸", "妈妈", "家人")) {
        return "family";
    }

    return null;
}
private boolean isGenericProfileQuestion(String text) {
    if (isBlank(text)) {
        return false;
    }

    // 已经明确说“我是/本人/我在”的，不算泛问。
    if (containsAny(text, "我是", "本人", "我目前", "我现在", "我在", "我的")) {
        return false;
    }

    // 典型泛问：某类人可以申请什么？
    return Pattern.compile(".*(本科生|研究生|硕士|博士|博士后|毕业生|人才|企业|创业者).*(可以|能否|能不能|是否|怎么|如何|哪些|什么|申请|申报).*")
            .matcher(text)
            .matches();
}
    private boolean isUserSelfStatement(String text) {
        if (isBlank(text)) {
            return false;
        }

        // 明确假设句，不作为用户本人画像。
        if (containsAny(text, "如果我是", "假如我是", "要是我是", "如果我现在是", "假设我是")) {
            return false;
        }

        // 明显泛问句，不作为用户本人画像。
        if (Pattern.compile(".*(本科生|研究生|硕士|博士|博士后|AI人才|人工智能人才).*(可以|能否|能不能|是否|怎么|如何|多少|条件|流程).*").matcher(text).matches()
                && !containsAny(text, "我是", "本人", "我现在", "我目前", "我在", "我关注", "我想", "我准备", "我已经", "我主要")) {
            return false;
        }

        return containsAny(
                text,
                "我是",
                "本人是",
                "我现在是",
                "我目前是",
                "我在",
                "目前在",
                "现在在",
                "我准备",
                "我想创业",
                "我已经",
                "我关注",
                "我主要想了解",
                "我想了解",
                "我想去"
        );
    }
private String resolveHabitDomain(String habitKey) {
    String key = habitKey == null ? "" : habitKey.trim();

    return switch (key) {
        case "answer_style" -> "answer";
        case "device_preference" -> "device";
        case "frequent_topic", "frequent_question_topic" -> "topic";
        case "frequent_question_type" -> "question";
        case "repeated_policy_interest" -> "policy";
        case "communication_style" -> "communication";
        default -> "interaction";
    };
}
private Integer extractFirstIntegerBeforeKeyword(String text, String keyword) {
    if (isBlank(text) || isBlank(keyword)) {
        return null;
    }

    Pattern pattern = Pattern.compile("(\\d{1,2})\\s*" + Pattern.quote(keyword));
    java.util.regex.Matcher matcher = pattern.matcher(text);
    if (!matcher.find()) {
        return null;
    }

    try {
        return Integer.parseInt(matcher.group(1));
    } catch (Exception e) {
        return null;
    }
}

private String resolveAgeRangeCode(Integer age) {
    if (age == null) {
        return "unknown";
    }
    if (age < 30) {
        return "under_30";
    }
    if (age < 40) {
        return "30_39";
    }
    if (age < 50) {
        return "40_49";
    }
    if (age < 60) {
        return "50_59";
    }
    return "60_plus";
}

private String resolveAgeRangeText(Integer age) {
    if (age == null) {
        return "年龄待确认";
    }
    if (age < 30) {
        return "30岁以下";
    }
    if (age < 40) {
        return "30-39岁";
    }
    if (age < 50) {
        return "40-49岁";
    }
    if (age < 60) {
        return "50-59岁";
    }
    return "60岁以上";
}

private String resolveWorkExperienceCode(Integer years) {
    if (years == null) {
        return "unknown";
    }
    if (years < 3) {
        return "under_3_years";
    }
    if (years < 5) {
        return "3_4_years";
    }
    if (years < 10) {
        return "5_9_years";
    }
    if (years < 20) {
        return "10_19_years";
    }
    return "20_plus_years";
}

private String resolveWorkExperienceText(Integer years) {
    if (years == null) {
        return "从业年限待确认";
    }
    if (years < 3) {
        return "3年以下经验";
    }
    if (years < 5) {
        return "3-4年经验";
    }
    if (years < 10) {
        return "5-9年经验";
    }
    if (years < 20) {
        return "10-19年经验";
    }
    return "20年以上经验";
}
    private boolean containsAny(String text, String... keywords) {
        if (isBlank(text) || keywords == null) {
            return false;
        }

        for (String keyword : keywords) {
            if (!isBlank(keyword) && text.contains(keyword)) {
                return true;
            }
        }

        return false;
    }

    private String normalize(String text) {
        if (text == null) {
            return "";
        }

        return text
                .replace("，", ",")
                .replace("。", ".")
                .replace("？", "?")
                .replace("！", "!")
                .replace("\r", " ")
                .replace("\n", " ")
                .trim();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private Long longValue(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Number number) {
            return number.longValue();
        }

        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, Object> queryFirstMap(String sql, Object... args) {
        try {
            List<Map<String, Object>> list = jdbcTemplate.queryForList(sql, args);
            if (list == null || list.isEmpty()) {
                return null;
            }
            return list.get(0);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    /**
     * 变更日志写入。
     *
     * 为了避免日志表字段与你当前库里略有差异导致主流程失败，
     * 这里做了 try-catch，日志失败不影响画像主流程。
     */
    private void writeChangeLog(Long userId,
                            Long habitId,
                            Long itemId,
                            String profileKey,
                            String changeType,
                            String oldValue,
                            String newValue,
                            String reason) {
    /*
     * 第一阶段先关闭画像变更日志写入。
     *
     * 原因：
     * PostgreSQL 事务内如果 ai_user_profile_change_log 插入失败，
     * 即使 catch 住异常，当前事务也会进入 aborted 状态，
     * 后续画像主表/习惯表/摘要表 SQL 会继续报：
     * “当前事务被终止，事务块结束之前的查询被忽略”。
     *
     * 等主画像链路跑通后，再按 ai_user_profile_change_log 真实表结构单独恢复日志。
     */
}
private record ProfileSubjectContext(
        String subjectScope,
        String relationToUser,
        boolean hypothetical,
        boolean proxyQuestion,
        boolean organizationQuestion,
        boolean genericQuestion,
        String reason
) {
    boolean allowPersistSelfProfile() {
        return "SELF".equals(subjectScope)
                && !hypothetical
                && !proxyQuestion
                && !organizationQuestion
                && !genericQuestion;
    }
}


}