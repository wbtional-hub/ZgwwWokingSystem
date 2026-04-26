package com.example.lecturesystem.modules.agent.support.policy;

import com.example.lecturesystem.modules.agent.entity.AiPolicyCandidateAnswerEntity;
import com.example.lecturesystem.modules.agent.entity.AiPolicyCandidatePhraseEntity;
import com.example.lecturesystem.modules.agent.entity.AiPolicyFeedbackEntity;
import com.example.lecturesystem.modules.agent.entity.AiPolicyIntentAnswerEntity;
import com.example.lecturesystem.modules.agent.entity.AiPolicyIntentEntity;
import com.example.lecturesystem.modules.agent.entity.AiPolicyIntentPhraseEntity;
import com.example.lecturesystem.modules.agent.entity.AiPolicyUserFavoriteEntity;
import com.example.lecturesystem.modules.agent.mapper.AiPolicyCandidateAnswerMapper;
import com.example.lecturesystem.modules.agent.mapper.AiPolicyCandidatePhraseMapper;
import com.example.lecturesystem.modules.agent.mapper.AiPolicyFeedbackMapper;
import com.example.lecturesystem.modules.agent.mapper.AiPolicyIntentAnswerMapper;
import com.example.lecturesystem.modules.agent.mapper.AiPolicyIntentPhraseMapper;
import com.example.lecturesystem.modules.agent.mapper.AiPolicyUserFavoriteMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class AiPolicyReviewService {
    private final AiPolicyCandidatePhraseMapper aiPolicyCandidatePhraseMapper;
    private final AiPolicyCandidateAnswerMapper aiPolicyCandidateAnswerMapper;
    private final AiPolicyIntentPhraseMapper aiPolicyIntentPhraseMapper;
    private final AiPolicyIntentAnswerMapper aiPolicyIntentAnswerMapper;
    private final AiPolicyIntentService aiPolicyIntentService;
    private final AiPolicyUserFavoriteMapper aiPolicyUserFavoriteMapper;
    private final AiPolicyFeedbackMapper aiPolicyFeedbackMapper;

    public AiPolicyReviewService(AiPolicyCandidatePhraseMapper aiPolicyCandidatePhraseMapper,
                                 AiPolicyCandidateAnswerMapper aiPolicyCandidateAnswerMapper,
                                 AiPolicyIntentPhraseMapper aiPolicyIntentPhraseMapper,
                                 AiPolicyIntentAnswerMapper aiPolicyIntentAnswerMapper,
                                 AiPolicyIntentService aiPolicyIntentService,
                                 AiPolicyUserFavoriteMapper aiPolicyUserFavoriteMapper,
                                 AiPolicyFeedbackMapper aiPolicyFeedbackMapper) {
        this.aiPolicyCandidatePhraseMapper = aiPolicyCandidatePhraseMapper;
        this.aiPolicyCandidateAnswerMapper = aiPolicyCandidateAnswerMapper;
        this.aiPolicyIntentPhraseMapper = aiPolicyIntentPhraseMapper;
        this.aiPolicyIntentAnswerMapper = aiPolicyIntentAnswerMapper;
        this.aiPolicyIntentService = aiPolicyIntentService;
        this.aiPolicyUserFavoriteMapper = aiPolicyUserFavoriteMapper;
        this.aiPolicyFeedbackMapper = aiPolicyFeedbackMapper;
    }

    public List<AiPolicyCandidatePhraseEntity> queryCandidatePhrases(Long baseId, String reviewStatus) {
        return aiPolicyCandidatePhraseMapper.queryByReviewStatus(baseId, defaultStatus(reviewStatus));
    }

    public List<AiPolicyCandidateAnswerEntity> queryCandidateAnswers(Long baseId, String reviewStatus) {
        return aiPolicyCandidateAnswerMapper.queryByReviewStatus(baseId, defaultStatus(reviewStatus));
    }

    @Transactional
    public String reviewCandidatePhrase(Long candidateId, String action, Long targetIntentId) {
        AiPolicyCandidatePhraseEntity candidate = aiPolicyCandidatePhraseMapper.findById(candidateId);
        if (candidate == null) {
            throw new IllegalArgumentException("未找到待审核问法候选");
        }
        if ("rejected".equalsIgnoreCase(action)) {
            aiPolicyCandidatePhraseMapper.updateReviewStatus(candidateId, "rejected");
            return "rejected";
        }
        Long intentId = targetIntentId != null ? targetIntentId : candidate.getSuggestedIntentId();
        if (intentId == null) {
            throw new IllegalArgumentException("审核通过时必须指定 intentId");
        }
        Integer exists = aiPolicyIntentPhraseMapper.countExisting(candidate.getBaseId(), intentId, candidate.getNormalizedQuestion());
        if (exists == null || exists == 0) {
            AiPolicyIntentPhraseEntity phrase = new AiPolicyIntentPhraseEntity();
            phrase.setBaseId(candidate.getBaseId());
            phrase.setIntentId(intentId);
            phrase.setPhrase(candidate.getNormalizedQuestion());
            phrase.setPhraseType("user_phrase");
            phrase.setHitWeight(60);
            phrase.setEnabled(Boolean.TRUE);
        phrase.setCreatedAt(OffsetDateTime.now());
        phrase.setUpdatedAt(OffsetDateTime.now());
            aiPolicyIntentPhraseMapper.insert(phrase);
        }
        aiPolicyCandidatePhraseMapper.updateReviewStatus(candidateId, "merged");
        return "merged";
    }

    @Transactional
    public String reviewCandidateAnswer(Long candidateId, String action, Long targetIntentId) {
        AiPolicyCandidateAnswerEntity candidate = aiPolicyCandidateAnswerMapper.findById(candidateId);
        if (candidate == null) {
            throw new IllegalArgumentException("未找到待审核答案候选");
        }
        if ("rejected".equalsIgnoreCase(action)) {
            aiPolicyCandidateAnswerMapper.updateReviewStatus(candidateId, "rejected");
            return "rejected";
        }
        Long intentId = targetIntentId != null ? targetIntentId : candidate.getIntentId();
        if (intentId == null) {
            throw new IllegalArgumentException("审核通过时必须指定 intentId");
        }
        AiPolicyIntentEntity intent = aiPolicyIntentService.findById(intentId);
        AiPolicyIntentAnswerEntity answer = new AiPolicyIntentAnswerEntity();
        answer.setBaseId(candidate.getBaseId());
        answer.setIntentId(intentId);
        answer.setAnswerMode("direct_confirmed");
        answer.setAnswerTitle(intent == null ? "审核升级答案" : intent.getIntentName());
        answer.setAnswerTemplate(candidate.getFinalAnswer());
        answer.setEvidenceRule("manual_review");
        answer.setFollowupSuggestion("如需更细条款，可继续命中对应专题或原文。");
        answer.setPriority(50);
        answer.setEnabled(Boolean.TRUE);
        answer.setCreatedAt(OffsetDateTime.now());
        answer.setUpdatedAt(OffsetDateTime.now());
        aiPolicyIntentAnswerMapper.insert(answer);
        aiPolicyCandidateAnswerMapper.updateReviewStatus(candidateId, "merged");
        return "merged";
    }

    public ReviewDashboard queryDashboard(Long baseId) {
        List<AiPolicyUserFavoriteEntity> favorites = aiPolicyUserFavoriteMapper.queryRecentByBaseId(baseId);
        List<AiPolicyFeedbackEntity> feedbacks = aiPolicyFeedbackMapper.queryRecentByBaseId(baseId);
        List<AiPolicyIntentEntity> intents = aiPolicyIntentService.queryEnabledByBaseId(baseId);

        List<HotIntentItem> hotIntents = new ArrayList<>();
        for (AiPolicyIntentEntity intent : intents) {
            int favoriteCount = defaultInt(aiPolicyUserFavoriteMapper.countByIntentId(baseId, intent.getId()));
            int likeCount = defaultInt(aiPolicyFeedbackMapper.countByIntentAndType(baseId, intent.getId(), "like"));
            int dislikeCount = defaultInt(aiPolicyFeedbackMapper.countByIntentAndType(baseId, intent.getId(), "dislike"));
            hotIntents.add(new HotIntentItem(intent.getIntentCode(), intent.getStandardQuestion(), favoriteCount, likeCount, dislikeCount));
        }
        hotIntents.sort(Comparator.comparingInt(HotIntentItem::heatScore).reversed());

        List<AiPolicyFeedbackEntity> lowSatisfaction = feedbacks.stream()
                .filter(item -> "dislike".equalsIgnoreCase(item.getFeedbackType()) || "correct".equalsIgnoreCase(item.getFeedbackType()))
                .limit(20)
                .toList();

        return new ReviewDashboard(
                favorites.size(),
                feedbacks.size(),
                defaultInt(aiPolicyCandidatePhraseMapper.countByReviewStatus(baseId, "pending")),
                defaultInt(aiPolicyCandidateAnswerMapper.countByReviewStatus(baseId, "pending")),
                hotIntents.stream().limit(10).toList(),
                lowSatisfaction
        );
    }

    private String defaultStatus(String status) {
        return status == null || status.isBlank() ? "pending" : status;
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    public record ReviewDashboard(int favoriteCount,
                                  int feedbackCount,
                                  int pendingPhraseCount,
                                  int pendingAnswerCount,
                                  List<HotIntentItem> hotIntents,
                                  List<AiPolicyFeedbackEntity> lowSatisfactionFeedbacks) {
    }

    public record HotIntentItem(String intentCode,
                                String standardQuestion,
                                int favoriteCount,
                                int likeCount,
                                int dislikeCount) {
        public int heatScore() {
            return favoriteCount * 2 + likeCount * 3 - dislikeCount;
        }
    }
}
