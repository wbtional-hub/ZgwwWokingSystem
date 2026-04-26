package com.example.lecturesystem.modules.agent.support.policy;

import com.example.lecturesystem.modules.agent.entity.AiPolicyFaqEntity;
import com.example.lecturesystem.modules.agent.mapper.AiPolicyFaqMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AiPolicyFaqService {
    private static final int DIRECT_HIT_THRESHOLD = 70;

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

        String answer = "结论：" + best.getStandardAnswer() + "\n"
                + "当前依据：" + valueOrDefault(best.getEvidenceSource(), "已命中政策 FAQ。") + "\n"
                + "当前边界：如需进一步确认流程细项、材料清单、时间节点或条文原文，请继续命中对应专题或年度通知。";

        return new FaqMatch(true, best, answer);
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