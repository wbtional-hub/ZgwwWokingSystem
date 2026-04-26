package com.example.lecturesystem.modules.agent.support.policy;

import com.example.lecturesystem.modules.agent.entity.AiPolicyCandidateAnswerEntity;
import com.example.lecturesystem.modules.agent.entity.AiPolicyCandidatePhraseEntity;
import com.example.lecturesystem.modules.agent.entity.AiPolicyFeedbackEntity;
import com.example.lecturesystem.modules.agent.entity.AiPolicyUserFavoriteEntity;
import com.example.lecturesystem.modules.agent.mapper.AiPolicyCandidateAnswerMapper;
import com.example.lecturesystem.modules.agent.mapper.AiPolicyCandidatePhraseMapper;
import com.example.lecturesystem.modules.agent.mapper.AiPolicyFeedbackMapper;
import com.example.lecturesystem.modules.agent.mapper.AiPolicyUserFavoriteMapper;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
public class AiPolicyLearningService {
    private final AiPolicyUserFavoriteMapper aiPolicyUserFavoriteMapper;
    private final AiPolicyFeedbackMapper aiPolicyFeedbackMapper;
    private final AiPolicyCandidatePhraseMapper aiPolicyCandidatePhraseMapper;
    private final AiPolicyCandidateAnswerMapper aiPolicyCandidateAnswerMapper;

    public AiPolicyLearningService(AiPolicyUserFavoriteMapper aiPolicyUserFavoriteMapper,
                                   AiPolicyFeedbackMapper aiPolicyFeedbackMapper,
                                   AiPolicyCandidatePhraseMapper aiPolicyCandidatePhraseMapper,
                                   AiPolicyCandidateAnswerMapper aiPolicyCandidateAnswerMapper) {
        this.aiPolicyUserFavoriteMapper = aiPolicyUserFavoriteMapper;
        this.aiPolicyFeedbackMapper = aiPolicyFeedbackMapper;
        this.aiPolicyCandidatePhraseMapper = aiPolicyCandidatePhraseMapper;
        this.aiPolicyCandidateAnswerMapper = aiPolicyCandidateAnswerMapper;
    }

    public LearningResult favorite(Long baseId,
                                   Long userId,
                                   Long sessionId,
                                   String regionScope,
                                   String policyKey,
                                   Long intentId,
                                   String rawQuestion,
                                   String normalizedQuestion,
                                   String finalAnswer) {
        OffsetDateTime now = OffsetDateTime.now();
        AiPolicyUserFavoriteEntity favorite = new AiPolicyUserFavoriteEntity();
        favorite.setBaseId(baseId);
        favorite.setUserId(userId);
        favorite.setSessionId(sessionId);
        favorite.setRawQuestion(rawQuestion);
        favorite.setNormalizedQuestion(normalizedQuestion);
        favorite.setPolicyKey(policyKey);
        favorite.setIntentId(intentId);
        favorite.setFinalAnswer(finalAnswer);
        favorite.setCreatedAt(now);
        aiPolicyUserFavoriteMapper.insert(favorite);
        boolean candidateGenerated = writeCandidates(baseId, regionScope, policyKey, intentId, rawQuestion, normalizedQuestion, finalAnswer, "favorite", now);
        return new LearningResult(true, candidateGenerated);
    }

    public LearningResult feedback(Long baseId,
                                   Long userId,
                                   Long sessionId,
                                   String regionScope,
                                   String policyKey,
                                   Long intentId,
                                   String rawQuestion,
                                   String normalizedQuestion,
                                   String finalAnswer,
                                   String feedbackType,
                                   String feedbackText) {
        OffsetDateTime now = OffsetDateTime.now();
        AiPolicyFeedbackEntity feedback = new AiPolicyFeedbackEntity();
        feedback.setBaseId(baseId);
        feedback.setUserId(userId);
        feedback.setSessionId(sessionId);
        feedback.setRawQuestion(rawQuestion);
        feedback.setNormalizedQuestion(normalizedQuestion);
        feedback.setIntentId(intentId);
        feedback.setFeedbackType(feedbackType);
        feedback.setFeedbackText(feedbackText);
        feedback.setFinalAnswer(finalAnswer);
        feedback.setCreatedAt(now);
        aiPolicyFeedbackMapper.insert(feedback);
        boolean candidateGenerated = writeCandidates(baseId, regionScope, policyKey, intentId, rawQuestion, normalizedQuestion, finalAnswer, feedbackType, now);
        return new LearningResult(true, candidateGenerated);
    }

    private boolean writeCandidates(Long baseId,
                                    String regionScope,
                                    String policyKey,
                                    Long intentId,
                                    String rawQuestion,
                                    String normalizedQuestion,
                                    String finalAnswer,
                                    String sourceType,
                                                   OffsetDateTime now) {
        boolean generated = false;
        if (normalizedQuestion != null && !normalizedQuestion.isBlank()) {
            AiPolicyCandidatePhraseEntity candidatePhrase = new AiPolicyCandidatePhraseEntity();
            candidatePhrase.setBaseId(baseId);
            candidatePhrase.setRegionScope(regionScope);
            candidatePhrase.setPolicyKey(policyKey);
            candidatePhrase.setRawQuestion(rawQuestion);
            candidatePhrase.setNormalizedQuestion(normalizedQuestion);
            candidatePhrase.setSuggestedIntentId(intentId);
            candidatePhrase.setSourceType(sourceType);
            candidatePhrase.setReviewStatus("pending");
            candidatePhrase.setCreatedAt(now);
            candidatePhrase.setUpdatedAt(now);
            aiPolicyCandidatePhraseMapper.insert(candidatePhrase);
            generated = true;
        }
        if (finalAnswer != null && !finalAnswer.isBlank()) {
            AiPolicyCandidateAnswerEntity candidateAnswer = new AiPolicyCandidateAnswerEntity();
            candidateAnswer.setBaseId(baseId);
            candidateAnswer.setRegionScope(regionScope);
            candidateAnswer.setPolicyKey(policyKey);
            candidateAnswer.setIntentId(intentId);
            candidateAnswer.setRawQuestion(rawQuestion);
            candidateAnswer.setFinalAnswer(finalAnswer);
            candidateAnswer.setSourceType(sourceType);
            candidateAnswer.setReviewStatus("pending");
            candidateAnswer.setCreatedAt(now);
            candidateAnswer.setUpdatedAt(now);
            aiPolicyCandidateAnswerMapper.insert(candidateAnswer);
            generated = true;
        }
        return generated;
    }

    public record LearningResult(boolean learningSignalWritten,
                                 boolean candidateGenerated) {
    }
}
