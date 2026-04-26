package com.example.lecturesystem.modules.agent.support.policy;

import com.example.lecturesystem.modules.agent.entity.AiPolicyIntentSuggestLogEntity;
import com.example.lecturesystem.modules.agent.mapper.AiPolicyIntentSuggestLogMapper;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class AiPolicyIntentSuggestService {
    private final AiPolicyQuestionNormalizer normalizer;
    private final AiPolicyRegionResolver regionResolver;
    private final AiPolicyResolver policyResolver;
    private final AiPolicyIntentClassifier intentClassifier;
    private final AiPolicyIntentPhraseService aiPolicyIntentPhraseService;
    private final AiPolicyIntentSuggestLogMapper aiPolicyIntentSuggestLogMapper;

    public AiPolicyIntentSuggestService(AiPolicyQuestionNormalizer normalizer,
                                        AiPolicyRegionResolver regionResolver,
                                        AiPolicyResolver policyResolver,
                                        AiPolicyIntentClassifier intentClassifier,
                                        AiPolicyIntentPhraseService aiPolicyIntentPhraseService,
                                        AiPolicyIntentSuggestLogMapper aiPolicyIntentSuggestLogMapper) {
        this.normalizer = normalizer;
        this.regionResolver = regionResolver;
        this.policyResolver = policyResolver;
        this.intentClassifier = intentClassifier;
        this.aiPolicyIntentPhraseService = aiPolicyIntentPhraseService;
        this.aiPolicyIntentSuggestLogMapper = aiPolicyIntentSuggestLogMapper;
    }

    public SuggestResult suggest(Long baseId, Long userId, String input) {
        AiPolicyQuestionNormalizer.NormalizedQuestion normalizedQuestion = normalizer.normalize(input);
        if (baseId == null || normalizedQuestion == null) {
            return new SuggestResult(null, List.of());
        }
        AiPolicyRegionResolver.RegionMatch regionMatch = regionResolver.resolve(normalizedQuestion);
        AiPolicyResolver.PolicyMatch policyMatch = policyResolver.resolve(baseId, normalizedQuestion, regionMatch);
        AiPolicyIntentClassifier.IntentMatch intentMatch = intentClassifier.classify(normalizedQuestion, regionMatch, policyMatch);
        List<AiPolicyIntentPhraseService.SuggestedIntent> suggestions =
                aiPolicyIntentPhraseService.suggest(baseId, userId, normalizedQuestion, regionMatch, policyMatch, intentMatch, 8);

        AiPolicyIntentSuggestLogEntity entity = new AiPolicyIntentSuggestLogEntity();
        entity.setBaseId(baseId);
        entity.setUserId(userId);
        entity.setRawInput(input);
        entity.setNormalizedInput(normalizedQuestion.normalized());
        entity.setSuggestedItems(aiPolicyIntentPhraseService.serializeSuggestions(suggestions));
        entity.setSelectedIntentId(null);
        entity.setCreatedAt(OffsetDateTime.now());
        aiPolicyIntentSuggestLogMapper.insert(entity);
        return new SuggestResult(entity.getId(), suggestions);
    }

    public void selectSuggestion(Long suggestLogId, Long selectedIntentId) {
        if (suggestLogId == null || selectedIntentId == null) {
            return;
        }
        aiPolicyIntentSuggestLogMapper.updateSelectedIntent(suggestLogId, selectedIntentId);
    }

    public record SuggestResult(Long suggestLogId,
                                List<AiPolicyIntentPhraseService.SuggestedIntent> items) {
    }
}
