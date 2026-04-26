package com.example.lecturesystem.modules.agent.support.policy;

import org.springframework.stereotype.Service;

@Service
public class AiPolicyRegionResolver {
    public RegionMatch resolve(AiPolicyQuestionNormalizer.NormalizedQuestion question) {
        if (question == null) {
            return new RegionMatch(RegionScope.UNKNOWN, false, false);
        }
        String compact = question.compact();
        boolean xm = compact.contains("厦门");
        boolean fj = compact.contains("福建");
        if (xm && fj) {
            return new RegionMatch(RegionScope.UNKNOWN, true, true);
        }
        if (xm) {
            return new RegionMatch(RegionScope.XIAMEN_CITY, true, false);
        }
        if (fj) {
            return new RegionMatch(RegionScope.FUJIAN_PROVINCE, true, false);
        }
        return new RegionMatch(RegionScope.UNKNOWN, false, false);
    }

    public enum RegionScope {
        XIAMEN_CITY("xiamen_city", "厦门市"),
        FUJIAN_PROVINCE("fujian_province", "福建省"),
        UNKNOWN("unknown", "未明确地区");

        private final String code;
        private final String label;

        RegionScope(String code, String label) {
            this.code = code;
            this.label = label;
        }

        public String getCode() {
            return code;
        }

        public String getLabel() {
            return label;
        }
    }

    public record RegionMatch(RegionScope scope, boolean explicit, boolean compareQuestion) {
    }
}
