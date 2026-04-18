package com.example.lecturesystem.modules.agent.support.policy;

import java.util.ArrayList;
import java.util.List;

public class PolicyEligibilityMatchTool {
    public Result match(PolicyQuestionClassifier.Analysis analysis,
                        PolicyKnowledgeRetrievalTool.Result retrievalResult) {
        if (analysis == null) {
            return new Result(false, List.of(), List.of(), List.of());
        }
        List<String> satisfied = new ArrayList<>();
        if (analysis.getRegion() != null) {
            satisfied.add("地区=" + analysis.getRegion());
        }
        if (analysis.getEducation() != null) {
            satisfied.add("学历=" + analysis.getEducation());
        }
        if (analysis.getSalaryYearly() != null) {
            satisfied.add("年薪=" + analysis.getSalaryYearly());
        }
        if (analysis.getIndustry() != null) {
            satisfied.add("行业=" + analysis.getIndustry());
        }
        if (analysis.getWorkStatus() != null) {
            satisfied.add("工作状态=" + analysis.getWorkStatus());
        }
        if (analysis.getConsultTheme() != null) {
            satisfied.add("咨询主题=" + analysis.getConsultTheme());
        }

        List<String> missing = new ArrayList<>();
        for (String field : analysis.getMissingFields()) {
            if ("region".equals(field)) {
                missing.add("政策地区");
            } else if ("workStatus".equals(field)) {
                missing.add("在厦状态");
            } else if ("industryOrTheme".equals(field)) {
                missing.add("行业或咨询主题");
            } else {
                missing.add(field);
            }
        }

        List<String> referenceOnly = new ArrayList<>();
        if (retrievalResult == null || retrievalResult.isMainlyExplanation()) {
            referenceOnly.add("当前主要命中的是说明型或整理型资料，不能直接作为资格判断条款。");
        }
        if (retrievalResult != null && !retrievalResult.hasRawPolicy()) {
            referenceOnly.add("当前未直接命中资格判断正文条款，结论只能作为初步参考。");
        }

        boolean canDirectJudge = missing.isEmpty()
                && retrievalResult != null
                && retrievalResult.hasRawPolicy()
                && !retrievalResult.isMainlyExplanation();
        return new Result(canDirectJudge, satisfied, missing, referenceOnly);
    }

    public static final class Result {
        private final boolean canDirectJudge;
        private final List<String> satisfiedConditions;
        private final List<String> missingConditions;
        private final List<String> referenceOnlyNotes;

        public Result(boolean canDirectJudge,
                      List<String> satisfiedConditions,
                      List<String> missingConditions,
                      List<String> referenceOnlyNotes) {
            this.canDirectJudge = canDirectJudge;
            this.satisfiedConditions = satisfiedConditions == null ? List.of() : List.copyOf(satisfiedConditions);
            this.missingConditions = missingConditions == null ? List.of() : List.copyOf(missingConditions);
            this.referenceOnlyNotes = referenceOnlyNotes == null ? List.of() : List.copyOf(referenceOnlyNotes);
        }

        public boolean isCanDirectJudge() {
            return canDirectJudge;
        }

        public List<String> getSatisfiedConditions() {
            return satisfiedConditions;
        }

        public List<String> getMissingConditions() {
            return missingConditions;
        }

        public List<String> getReferenceOnlyNotes() {
            return referenceOnlyNotes;
        }
    }
}
