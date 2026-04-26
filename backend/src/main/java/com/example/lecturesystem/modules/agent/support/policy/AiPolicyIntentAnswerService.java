package com.example.lecturesystem.modules.agent.support.policy;

import com.example.lecturesystem.modules.agent.entity.AiPolicyIntentAnswerEntity;
import com.example.lecturesystem.modules.agent.entity.AiPolicyIntentEntity;
import com.example.lecturesystem.modules.agent.mapper.AiPolicyIntentAnswerMapper;
import com.example.lecturesystem.modules.knowledge.vo.KnowledgeSearchResultVO;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;

@Service
public class AiPolicyIntentAnswerService {
    private final AiPolicyIntentAnswerMapper aiPolicyIntentAnswerMapper;

    public AiPolicyIntentAnswerService(AiPolicyIntentAnswerMapper aiPolicyIntentAnswerMapper) {
        this.aiPolicyIntentAnswerMapper = aiPolicyIntentAnswerMapper;
    }

    public AiPolicyIntentAnswerEntity findBestAnswer(Long intentId, String answerMode) {
        if (intentId == null) {
            return null;
        }
        List<AiPolicyIntentAnswerEntity> answers = aiPolicyIntentAnswerMapper.queryByIntentId(intentId);
        if (answers == null || answers.isEmpty()) {
            return null;
        }
        for (AiPolicyIntentAnswerEntity answer : answers) {
            if (answer.getAnswerMode() != null && answer.getAnswerMode().equalsIgnoreCase(answerMode)) {
                return answer;
            }
        }
        return answers.get(0);
    }

    public String render(AiPolicyIntentEntity intent,
                         String answerMode,
                         AiPolicyIntentAnswerEntity answerEntity,
                         AiPolicyEvidenceValidator.ValidationResult validationResult,
                         String fallbackAnswer) {
        if (answerEntity == null || answerEntity.getAnswerTemplate() == null || answerEntity.getAnswerTemplate().isBlank()) {
            return fallbackAnswer;
        }
        String evidenceItems = buildEvidenceItems(validationResult.primaryHits());
        String rendered = answerEntity.getAnswerTemplate()
                .replace("{intent_name}", valueOrDefault(intent == null ? null : intent.getIntentName(), "标准意图"))
                .replace("{standard_question}", valueOrDefault(intent == null ? null : intent.getStandardQuestion(), "标准问题"))
                .replace("{answer_mode}", valueOrDefault(answerMode, "partial_confirmed"))
                .replace("{validation_summary}", valueOrDefault(validationResult.validationSummary(), "当前需继续核验证据边界。"))
                .replace("{evidence_items}", evidenceItems)
                .replace("{followup_suggestion}", valueOrDefault(answerEntity.getFollowupSuggestion(), "如需细项，请继续命中对应专题或原始条款。"));
        return rendered.trim();
    }

    private String buildEvidenceItems(List<KnowledgeSearchResultVO> hits) {
        if (hits == null || hits.isEmpty()) {
            return "当前尚未命中足够的政策证据。";
        }
        LinkedHashSet<String> citations = new LinkedHashSet<>();
        for (KnowledgeSearchResultVO hit : hits) {
            citations.add(valueOrDefault(hit.getDocTitle(), "未命名文档")
                    + " / "
                    + valueOrDefault(hit.getHeadingPath(), valueOrDefault(hit.getSectionTitle(), "未标注标题")));
        }
        return String.join("；", citations);
    }

    private String valueOrDefault(String text, String defaultValue) {
        return text == null || text.isBlank() ? defaultValue : text;
    }
}
