package com.example.lecturesystem.modules.agent.support.policy;

import com.example.lecturesystem.modules.agent.entity.AiPolicyFaqEntity;
import com.example.lecturesystem.modules.agent.mapper.AiPolicyFaqMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AiPolicyFaqService {
    private final AiPolicyFaqMapper aiPolicyFaqMapper;

    public AiPolicyFaqService(AiPolicyFaqMapper aiPolicyFaqMapper) {
        this.aiPolicyFaqMapper = aiPolicyFaqMapper;
    }

    public FaqMatch match(Long baseId,
                          AiPolicyQuestionNormalizer.NormalizedQuestion question,
                          AiPolicyRegionResolver.RegionMatch regionMatch,
                          AiPolicyResolver.PolicyMatch policyMatch) {
        if (baseId == null || question == null) {
            return FaqMatch.notMatched();
        }
        List<AiPolicyFaqEntity> faqList;
        try {
            faqList = aiPolicyFaqMapper.queryEnabledByBaseId(baseId);
        } catch (Exception ex) {
            return FaqMatch.notMatched();
        }
        AiPolicyFaqEntity best = null;
        int bestScore = 0;
        for (AiPolicyFaqEntity faq : faqList) {
            int score = score(faq, question, regionMatch, policyMatch);
            if (score > bestScore) {
                bestScore = score;
                best = faq;
            }
        }
        if (best == null || bestScore < 70) {
            return FaqMatch.notMatched();
        }
        String answer = "结论：" + best.getStandardAnswer() + "\n"
                + "当前依据：" + valueOrDefault(best.getEvidenceSource(), "已命中政策 FAQ。") + "\n"
                + "当前边界：如需进一步确认流程细项、材料清单、时间节点或条文原文，请继续命中对应专题或年度通知。";
        return new FaqMatch(true, best, answer);
    }

    private int score(AiPolicyFaqEntity faq,
                      AiPolicyQuestionNormalizer.NormalizedQuestion question,
                      AiPolicyRegionResolver.RegionMatch regionMatch,
                      AiPolicyResolver.PolicyMatch policyMatch) {
        int score = faq.getPriority() == null ? 0 : faq.getPriority();
        if (regionMatch != null && regionMatch.scope() != AiPolicyRegionResolver.RegionScope.UNKNOWN
                && regionMatch.scope().getCode().equalsIgnoreCase(faq.getRegionScope())) {
            score += 20;
        }
        if (policyMatch != null && policyMatch.matched() && policyMatch.policyKey().equals(faq.getPolicyKey())) {
            score += 35;
        }
        String pattern = faq.getQuestionPattern() == null ? "" : faq.getQuestionPattern();
        for (String term : pattern.split("\\|")) {
            if (!term.isBlank() && question.compact().contains(term)) {
                score += 18;
            }
        }
        if (question.compact().contains(faq.getStandardQuestion().replace(" ", ""))) {
            score += 30;
        }
        return score;
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
