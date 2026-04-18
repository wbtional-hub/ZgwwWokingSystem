package com.example.lecturesystem.modules.agent.support.policy;

import java.util.List;

public class PolicySkillStrategy {
    private static final String SOURCE_SCENE_MOBILE_POLICY_CONSULTANT = "MOBILE_POLICY_CONSULTANT";

    public Decision decide(String sourceScene,
                           PolicyQuestionClassifier.Analysis analysis,
                           int defaultTopN) {
        String normalizedScene = normalize(sourceScene);
        boolean mobilePolicyScene = SOURCE_SCENE_MOBILE_POLICY_CONSULTANT.equalsIgnoreCase(normalizedScene);
        if (!mobilePolicyScene || analysis == null) {
            return new Decision("DEFAULT_AGENT_CHAIN", defaultTopN, false, false, false, List.of());
        }
        String questionType = analysis.getQuestionType();
        return switch (questionType) {
            case PolicyQuestionClassifier.QUESTION_TYPE_OVERVIEW ->
                    new Decision("MOBILE_POLICY_CONSULTANT/OVERVIEW/OVERVIEW_FIRST", Math.max(defaultTopN, 4), false, true, true, List.of());
            case PolicyQuestionClassifier.QUESTION_TYPE_ELIGIBILITY ->
                    new Decision("MOBILE_POLICY_CONSULTANT/ELIGIBILITY/RAW_POLICY_FIRST", defaultTopN, analysis.isNeedFollowup(), false, true, List.of());
            case PolicyQuestionClassifier.QUESTION_TYPE_COMPARE ->
                    new Decision("MOBILE_POLICY_CONSULTANT/COMPARE/DUAL_REGION", Math.max(defaultTopN, 4), false, false, true, buildCompareRegions(analysis.getRegion()));
            default ->
                    new Decision("MOBILE_POLICY_CONSULTANT/DETAIL/RAW_POLICY_FIRST", defaultTopN, false, false, true, List.of());
        };
    }

    private List<String> buildCompareRegions(String region) {
        if ("福建省".equals(region)) {
            return List.of("福建省", "厦门市");
        }
        return List.of("厦门市", "福建省");
    }

    private String normalize(String text) {
        if (text == null) {
            return null;
        }
        String value = text.trim();
        return value.isEmpty() ? null : value;
    }

    public static final class Decision {
        private final String retrievalStrategy;
        private final int topN;
        private final boolean needFollowup;
        private final boolean preferOverview;
        private final boolean preferRawPolicy;
        private final List<String> compareRegions;

        public Decision(String retrievalStrategy,
                        int topN,
                        boolean needFollowup,
                        boolean preferOverview,
                        boolean preferRawPolicy,
                        List<String> compareRegions) {
            this.retrievalStrategy = retrievalStrategy;
            this.topN = topN;
            this.needFollowup = needFollowup;
            this.preferOverview = preferOverview;
            this.preferRawPolicy = preferRawPolicy;
            this.compareRegions = compareRegions == null ? List.of() : List.copyOf(compareRegions);
        }

        public String getRetrievalStrategy() {
            return retrievalStrategy;
        }

        public int getTopN() {
            return topN;
        }

        public boolean isNeedFollowup() {
            return needFollowup;
        }

        public boolean isPreferOverview() {
            return preferOverview;
        }

        public boolean isPreferRawPolicy() {
            return preferRawPolicy;
        }

        public List<String> getCompareRegions() {
            return compareRegions;
        }
    }
}
