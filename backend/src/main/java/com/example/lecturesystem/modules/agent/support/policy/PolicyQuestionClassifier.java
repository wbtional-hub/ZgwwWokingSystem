package com.example.lecturesystem.modules.agent.support.policy;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PolicyQuestionClassifier {
    public static final String QUESTION_TYPE_OVERVIEW = "OVERVIEW";
    public static final String QUESTION_TYPE_ELIGIBILITY = "ELIGIBILITY";
    public static final String QUESTION_TYPE_DETAIL = "DETAIL";
    public static final String QUESTION_TYPE_COMPARE = "COMPARE";

    private static final Pattern SALARY_PATTERN = Pattern.compile("(?:税前)?年薪\\s*([0-9]+(?:\\.[0-9]+)?)\\s*(万|万元)?");

    public Analysis analyze(String question) {
        String normalized = normalize(question);
        if (normalized == null) {
            return new Analysis(QUESTION_TYPE_DETAIL, null, null, null, null, null, null, false, List.of());
        }
        String questionType = resolveQuestionType(normalized);
        String region = resolveRegion(normalized);
        String education = resolveEducation(normalized);
        Integer salaryYearly = resolveSalaryYearly(normalized);
        String industry = resolveIndustry(normalized);
        String workStatus = resolveWorkStatus(normalized);
        String consultTheme = resolveConsultTheme(normalized);
        List<String> missingFields = resolveMissingFields(questionType, region, industry, workStatus, consultTheme);
        boolean needFollowup = QUESTION_TYPE_ELIGIBILITY.equals(questionType) && !missingFields.isEmpty();
        return new Analysis(
                questionType,
                region,
                education,
                salaryYearly,
                industry,
                workStatus,
                consultTheme,
                needFollowup,
                missingFields
        );
    }

    private String resolveQuestionType(String question) {
        if (containsAny(question, "区别", "差别", "差异", "不同", "对比", "比较")) {
            return QUESTION_TYPE_COMPARE;
        }
        if (looksLikeEligibility(question)) {
            return QUESTION_TYPE_ELIGIBILITY;
        }
        if (containsAny(question, "有哪些", "有什么", "总览", "总清单", "清单", "梳理", "总结", "汇总")) {
            return QUESTION_TYPE_OVERVIEW;
        }
        return QUESTION_TYPE_DETAIL;
    }

    private boolean looksLikeEligibility(String question) {
        boolean hasCondition = containsAny(
                question,
                "我是",
                "本人",
                "我们公司",
                "本科学历",
                "本科生",
                "硕士",
                "博士",
                "博士后",
                "年薪",
                "税前年薪",
                "在厦",
                "来厦",
                "工作"
        );
        boolean asksJudgement = containsAny(
                question,
                "可以申请哪些",
                "能申请哪些",
                "申请哪些人才",
                "可以享受哪些",
                "能享受哪些",
                "适合哪些",
                "符合什么条件",
                "能认定为什么",
                "可以认定为什么"
        );
        return hasCondition && asksJudgement;
    }

    private String resolveRegion(String question) {
        if (containsAny(question, "厦门市", "厦门")) {
            return "厦门市";
        }
        if (containsAny(question, "福建省", "福建")) {
            return "福建省";
        }
        return null;
    }

    private String resolveEducation(String question) {
        if (containsAny(question, "博士后")) {
            return "博士后";
        }
        if (containsAny(question, "博士")) {
            return "博士";
        }
        if (containsAny(question, "硕士")) {
            return "硕士";
        }
        if (containsAny(question, "本科")) {
            return "本科";
        }
        return null;
    }

    private Integer resolveSalaryYearly(String question) {
        Matcher matcher = SALARY_PATTERN.matcher(question);
        if (!matcher.find()) {
            return null;
        }
        double value = Double.parseDouble(matcher.group(1));
        String unit = matcher.group(2);
        if (unit != null && unit.contains("万")) {
            value = value * 10000D;
        }
        return (int) Math.round(value);
    }

    private String resolveIndustry(String question) {
        if (containsAny(question, "电子信息")) {
            return "电子信息";
        }
        if (containsAny(question, "机械装备")) {
            return "机械装备";
        }
        if (containsAny(question, "商贸物流")) {
            return "商贸物流";
        }
        if (containsAny(question, "金融")) {
            return "金融";
        }
        if (containsAny(question, "生物医药")) {
            return "生物医药";
        }
        if (containsAny(question, "新能源新材料")) {
            return "新能源新材料";
        }
        if (containsAny(question, "文旅", "文化旅游", "旅游")) {
            return "文旅";
        }
        if (containsAny(question, "海洋")) {
            return "海洋";
        }
        return null;
    }

    private String resolveWorkStatus(String question) {
        if (containsAny(question, "已在厦", "在厦门工作", "在厦工作", "从外地来厦", "已来厦")) {
            return "已在厦";
        }
        if (containsAny(question, "准备来厦", "计划来厦", "来厦发展", "拟来厦")) {
            return "准备来厦";
        }
        return null;
    }

    private String resolveConsultTheme(String question) {
        if (containsAny(question, "高层次人才")) {
            return "高层次人才";
        }
        if (containsAny(question, "产业人才")) {
            return "产业人才";
        }
        if (containsAny(question, "骨干人才")) {
            return "骨干人才";
        }
        if (containsAny(question, "住房", "安家", "公寓")) {
            return "住房";
        }
        if (containsAny(question, "创业", "创业支持")) {
            return "创业";
        }
        if (containsAny(question, "博士后")) {
            return "博士后";
        }
        if (containsAny(question, "人工智能", "AI")) {
            return "人工智能";
        }
        if (containsAny(question, "台湾人才", "台胞")) {
            return "台湾人才";
        }
        return null;
    }

    private List<String> resolveMissingFields(String questionType,
                                              String region,
                                              String industry,
                                              String workStatus,
                                              String consultTheme) {
        if (!QUESTION_TYPE_ELIGIBILITY.equals(questionType)) {
            return List.of();
        }
        List<String> missing = new ArrayList<>();
        if (region == null) {
            missing.add("region");
        }
        if (workStatus == null) {
            missing.add("workStatus");
        }
        if (industry == null && consultTheme == null) {
            missing.add("industryOrTheme");
        }
        return missing;
    }

    private boolean containsAny(String text, String... terms) {
        if (text == null || terms == null) {
            return false;
        }
        for (String term : terms) {
            if (term != null && text.contains(term)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String text) {
        if (text == null) {
            return null;
        }
        String value = text.trim();
        return value.isEmpty() ? null : value;
    }

    public static final class Analysis {
        private final String questionType;
        private final String region;
        private final String education;
        private final Integer salaryYearly;
        private final String industry;
        private final String workStatus;
        private final String consultTheme;
        private final boolean needFollowup;
        private final List<String> missingFields;

        public Analysis(String questionType,
                        String region,
                        String education,
                        Integer salaryYearly,
                        String industry,
                        String workStatus,
                        String consultTheme,
                        boolean needFollowup,
                        List<String> missingFields) {
            this.questionType = questionType;
            this.region = region;
            this.education = education;
            this.salaryYearly = salaryYearly;
            this.industry = industry;
            this.workStatus = workStatus;
            this.consultTheme = consultTheme;
            this.needFollowup = needFollowup;
            this.missingFields = missingFields == null ? List.of() : List.copyOf(missingFields);
        }

        public String getQuestionType() {
            return questionType;
        }

        public String getRegion() {
            return region;
        }

        public String getEducation() {
            return education;
        }

        public Integer getSalaryYearly() {
            return salaryYearly;
        }

        public String getIndustry() {
            return industry;
        }

        public String getWorkStatus() {
            return workStatus;
        }

        public String getConsultTheme() {
            return consultTheme;
        }

        public boolean isNeedFollowup() {
            return needFollowup;
        }

        public List<String> getMissingFields() {
            return missingFields;
        }

        public Map<String, Object> toFieldMap() {
            Map<String, Object> fields = new LinkedHashMap<>();
            fields.put("questionType", questionType);
            fields.put("region", region);
            fields.put("education", education);
            fields.put("salaryYearly", salaryYearly);
            fields.put("industry", industry);
            fields.put("workStatus", workStatus);
            fields.put("consultTheme", consultTheme);
            return fields;
        }
    }
}
