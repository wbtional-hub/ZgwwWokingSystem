package com.example.lecturesystem.modules.agent.support.policy;

import com.example.lecturesystem.modules.agent.entity.AiPolicyAliasEntity;
import com.example.lecturesystem.modules.agent.entity.AiPolicyIntentEntity;
import com.example.lecturesystem.modules.agent.entity.AiPolicyIntentPhraseEntity;
import com.example.lecturesystem.modules.agent.mapper.AiPolicyAliasMapper;
import com.example.lecturesystem.modules.agent.mapper.AiPolicyFeedbackMapper;
import com.example.lecturesystem.modules.agent.mapper.AiPolicyIntentPhraseMapper;
import com.example.lecturesystem.modules.agent.mapper.AiPolicyUserFavoriteMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

@Service
public class AiPolicyIntentPhraseService {
    private final AiPolicyIntentService aiPolicyIntentService;
    private final AiPolicyIntentPhraseMapper aiPolicyIntentPhraseMapper;
    private final AiPolicyAliasMapper aiPolicyAliasMapper;
    private final AiPolicyUserFavoriteMapper aiPolicyUserFavoriteMapper;
    private final AiPolicyFeedbackMapper aiPolicyFeedbackMapper;

    public AiPolicyIntentPhraseService(AiPolicyIntentService aiPolicyIntentService,
                                       AiPolicyIntentPhraseMapper aiPolicyIntentPhraseMapper,
                                       AiPolicyAliasMapper aiPolicyAliasMapper,
                                       AiPolicyUserFavoriteMapper aiPolicyUserFavoriteMapper,
                                       AiPolicyFeedbackMapper aiPolicyFeedbackMapper) {
        this.aiPolicyIntentService = aiPolicyIntentService;
        this.aiPolicyIntentPhraseMapper = aiPolicyIntentPhraseMapper;
        this.aiPolicyAliasMapper = aiPolicyAliasMapper;
        this.aiPolicyUserFavoriteMapper = aiPolicyUserFavoriteMapper;
        this.aiPolicyFeedbackMapper = aiPolicyFeedbackMapper;
    }

    public MatchResult matchIntent(Long baseId,
                                   Long userId,
                                   AiPolicyQuestionNormalizer.NormalizedQuestion question,
                                   AiPolicyRegionResolver.RegionMatch regionMatch,
                                   AiPolicyResolver.PolicyMatch policyMatch,
                                   AiPolicyIntentClassifier.IntentMatch intentMatch) {
        List<SuggestedIntent> suggestions = suggest(baseId, userId, question, regionMatch, policyMatch, intentMatch, 5);
        if (suggestions.isEmpty()) {
            return MatchResult.unmatched();
        }
        SuggestedIntent best = suggestions.get(0);
        boolean confident = best.hotScore() >= 90;
        return new MatchResult(confident, confident ? best.intent() : null, suggestions);
    }

    public List<SuggestedIntent> suggest(Long baseId,
                                         Long userId,
                                         AiPolicyQuestionNormalizer.NormalizedQuestion question,
                                         AiPolicyRegionResolver.RegionMatch regionMatch,
                                         AiPolicyResolver.PolicyMatch policyMatch,
                                         AiPolicyIntentClassifier.IntentMatch intentMatch,
                                         int limit) {
        if (baseId == null || question == null) {
            return List.of();
        }
        List<AiPolicyIntentEntity> intents = aiPolicyIntentService.queryEnabledByBaseId(baseId);
        List<AiPolicyIntentPhraseEntity> phrases = aiPolicyIntentPhraseMapper.queryEnabledByBaseId(baseId);
        List<AiPolicyAliasEntity> aliases = aiPolicyAliasMapper.queryEnabledByBaseId(baseId);
        if (intents.isEmpty()) {
            return List.of();
        }

        Map<Long, List<AiPolicyIntentPhraseEntity>> phraseMap = new LinkedHashMap<>();
        for (AiPolicyIntentPhraseEntity phrase : phrases) {
            phraseMap.computeIfAbsent(phrase.getIntentId(), key -> new ArrayList<>()).add(phrase);
        }
        Map<String, List<AiPolicyAliasEntity>> aliasMap = new LinkedHashMap<>();
        for (AiPolicyAliasEntity alias : aliases) {
            if (alias.getPolicyKey() == null) {
                continue;
            }
            aliasMap.computeIfAbsent(alias.getPolicyKey(), key -> new ArrayList<>()).add(alias);
        }

        List<SuggestedIntent> results = new ArrayList<>();
        String compact = question.compact();
        int compactLength = compact == null ? 0 : compact.length();
        AliasSignal aliasSignal = resolveAliasSignal(policyMatch, aliasMap, compact);
        String focusedPolicyKey = aliasSignal.strong() ? aliasSignal.policyKey() : null;
        for (AiPolicyIntentEntity intent : intents) {
            if (!matchesIntentScope(intent, regionMatch, policyMatch, intentMatch, focusedPolicyKey, compactLength)) {
                continue;
            }
            int evidenceScore = scoreByStandardQuestion(intent, compact)
                    + scoreByPhrases(phraseMap.get(intent.getId()), compact)
                    + scoreByAliases(aliasMap.get(intent.getPolicyKey()), compact);
            if (evidenceScore <= 0) {
                continue;
            }
            int score = evidenceScore;
            score += policyFocusBonus(intent, policyMatch, focusedPolicyKey, compactLength);
            score += questionTypeBonus(intent, intentMatch, focusedPolicyKey, compactLength);
            score += scoreByUserSignals(baseId, userId, intent.getId());
            score += Math.min(intent.getPriority() == null ? 0 : intent.getPriority(), 24);
            results.add(new SuggestedIntent(intent, score));
        }

        return results.stream()
                .sorted(Comparator.comparingInt(SuggestedIntent::hotScore).reversed()
                        .thenComparing(item -> item.intent().getPriority() == null ? 0 : item.intent().getPriority(), Comparator.reverseOrder()))
                .limit(limit)
                .toList();
    }

    public String serializeSuggestions(List<SuggestedIntent> suggestions) {
        if (suggestions == null || suggestions.isEmpty()) {
            return "";
        }
        LinkedHashSet<String> items = new LinkedHashSet<>();
        for (SuggestedIntent item : suggestions) {
            items.add(item.intent().getIntentCode() + ":" + item.intent().getStandardQuestion());
        }
        return String.join(" | ", items);
    }

    private boolean matchesIntentScope(AiPolicyIntentEntity intent,
                                       AiPolicyRegionResolver.RegionMatch regionMatch,
                                       AiPolicyResolver.PolicyMatch policyMatch,
                                       AiPolicyIntentClassifier.IntentMatch intentMatch,
                                       String focusedPolicyKey,
                                       int compactLength) {
        if (regionMatch != null && regionMatch.scope() != AiPolicyRegionResolver.RegionScope.UNKNOWN
                && intent.getRegionScope() != null && !intent.getRegionScope().equalsIgnoreCase(regionMatch.scope().getCode())) {
            return false;
        }
        String constrainedPolicyKey = policyMatch != null && policyMatch.matched()
                ? policyMatch.policyKey()
                : focusedPolicyKey;
        if (constrainedPolicyKey != null
                && intent.getPolicyKey() != null
                && !intent.getPolicyKey().equalsIgnoreCase(constrainedPolicyKey)
                && (policyMatch != null && policyMatch.matched() || compactLength <= 4)) {
            return false;
        }
        if (shouldRelaxQuestionType(intent, constrainedPolicyKey, compactLength)) {
            return true;
        }
        if (intentMatch != null
                && intent.getQuestionType() != null
                && !"topic".equalsIgnoreCase(intent.getQuestionType())
                && !intent.getQuestionType().equalsIgnoreCase(intentMatch.intentType().name().toLowerCase())) {
            return false;
        }
        return true;
    }

    private boolean shouldRelaxQuestionType(AiPolicyIntentEntity intent,
                                            String constrainedPolicyKey,
                                            int compactLength) {
        return constrainedPolicyKey != null
                && intent.getPolicyKey() != null
                && intent.getPolicyKey().equalsIgnoreCase(constrainedPolicyKey)
                && compactLength > 0
                && compactLength <= 4;
    }

    private int scoreByStandardQuestion(AiPolicyIntentEntity intent, String compact) {
        if (compact == null || compact.isBlank()) {
            return 0;
        }
        String standard = safeCompact(intent.getStandardQuestion());
        if (standard.isBlank()) {
            return 0;
        }
        if (standard.equals(compact)) {
            return 120;
        }
        if (standard.contains(compact) || compact.contains(standard)) {
            return 72;
        }
        if (intent.getIntentName() != null && safeCompact(intent.getIntentName()).contains(compact)) {
            return 36;
        }
        return 0;
    }

    private int scoreByAliases(List<AiPolicyAliasEntity> aliases, String compact) {
        if (aliases == null || aliases.isEmpty() || compact == null || compact.isBlank()) {
            return 0;
        }
        int best = 0;
        for (AiPolicyAliasEntity alias : aliases) {
            String normalizedAlias = safeCompact(alias.getAlias());
            if (normalizedAlias.isBlank()) {
                continue;
            }
            int score = alias.getPriority() == null ? 0 : alias.getPriority();
            if (normalizedAlias.equals(compact)) {
                score += 180;
            } else if (normalizedAlias.startsWith(compact) || compact.startsWith(normalizedAlias)) {
                score += 132;
            } else if (normalizedAlias.contains(compact) || compact.contains(normalizedAlias)) {
                score += 96;
            } else {
                continue;
            }
            best = Math.max(best, score);
        }
        return best;
    }

    private int scoreByPhrases(List<AiPolicyIntentPhraseEntity> phrases, String compact) {
        if (phrases == null || phrases.isEmpty() || compact == null || compact.isBlank()) {
            return 0;
        }
        int best = 0;
        for (AiPolicyIntentPhraseEntity phrase : phrases) {
            String normalizedPhrase = safeCompact(phrase.getPhrase());
            if (normalizedPhrase.isBlank()) {
                continue;
            }
            int score = phrase.getHitWeight() == null ? 0 : phrase.getHitWeight();
            score += phraseTypeBonus(phrase.getPhraseType());
            if (normalizedPhrase.equals(compact)) {
                score += 130;
            } else if (compact.contains(normalizedPhrase) || normalizedPhrase.contains(compact)) {
                score += 90;
            } else if (normalizedPhrase.startsWith(compact) || compact.startsWith(normalizedPhrase)) {
                score += 60;
            } else {
                continue;
            }
            best = Math.max(best, score);
        }
        return best;
    }

    private int scoreByUserSignals(Long baseId, Long userId, Long intentId) {
        int favoriteCount = defaultInt(aiPolicyUserFavoriteMapper.countByIntentId(baseId, intentId));
        int positiveCount = defaultInt(aiPolicyFeedbackMapper.countByIntentAndType(baseId, intentId, "like"));
        int userFavoriteCount = userId == null ? 0 : defaultInt(aiPolicyUserFavoriteMapper.countByUserAndIntentId(baseId, userId, intentId));
        return favoriteCount * 2 + positiveCount * 3 + userFavoriteCount * 5;
    }

    private int phraseTypeBonus(String phraseType) {
        if (phraseType == null) {
            return 0;
        }
        return switch (phraseType) {
            case "standard" -> 18;
            case "alias" -> 14;
            case "hot_phrase" -> 10;
            case "user_phrase" -> 8;
            default -> 0;
        };
    }

    private AliasSignal resolveAliasSignal(AiPolicyResolver.PolicyMatch policyMatch,
                                           Map<String, List<AiPolicyAliasEntity>> aliasMap,
                                           String compact) {
        if (policyMatch != null && policyMatch.matched()) {
            return new AliasSignal(policyMatch.policyKey(), 999, true);
        }
        if (aliasMap.isEmpty() || compact == null || compact.isBlank()) {
            return AliasSignal.empty();
        }
        String bestPolicyKey = null;
        int bestScore = 0;
        for (Map.Entry<String, List<AiPolicyAliasEntity>> entry : aliasMap.entrySet()) {
            int score = scoreByAliases(entry.getValue(), compact);
            if (score > bestScore) {
                bestScore = score;
                bestPolicyKey = entry.getKey();
            }
        }
        if (bestPolicyKey == null) {
            return AliasSignal.empty();
        }
        boolean strong = compact.length() <= 4 ? bestScore >= 120 : bestScore >= 90;
        return new AliasSignal(bestPolicyKey, bestScore, strong);
    }

    private int policyFocusBonus(AiPolicyIntentEntity intent,
                                 AiPolicyResolver.PolicyMatch policyMatch,
                                 String focusedPolicyKey,
                                 int compactLength) {
        String policyKey = policyMatch != null && policyMatch.matched()
                ? policyMatch.policyKey()
                : focusedPolicyKey;
        if (policyKey == null || intent.getPolicyKey() == null || !intent.getPolicyKey().equalsIgnoreCase(policyKey)) {
            return 0;
        }
        return compactLength <= 4 ? 80 : 32;
    }

    private int questionTypeBonus(AiPolicyIntentEntity intent,
                                  AiPolicyIntentClassifier.IntentMatch intentMatch,
                                  String focusedPolicyKey,
                                  int compactLength) {
        if (intentMatch == null || intentMatch.intentType() == null || intent.getQuestionType() == null) {
            return 0;
        }
        String questionType = intent.getQuestionType().toLowerCase();
        String intentType = intentMatch.intentType().name().toLowerCase();
        if (questionType.equals(intentType)) {
            return 26;
        }
        if ("topic".equals(questionType)) {
            return 12;
        }
        if (focusedPolicyKey != null && compactLength <= 4) {
            return 8;
        }
        return 0;
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    private String safeCompact(String text) {
        return text == null ? "" : text.replace(" ", "").trim();
    }

    public record SuggestedIntent(AiPolicyIntentEntity intent, int hotScore) {
    }

    private record AliasSignal(String policyKey, int score, boolean strong) {
        private static AliasSignal empty() {
            return new AliasSignal(null, 0, false);
        }
    }

    public record MatchResult(boolean matched,
                              AiPolicyIntentEntity intent,
                              List<SuggestedIntent> suggestions) {
        public static MatchResult unmatched() {
            return new MatchResult(false, null, List.of());
        }
    }
}
