package com.example.lecturesystem.modules.agent.support.policy;

import com.example.lecturesystem.modules.agent.entity.AiPolicyCandidateAnswerEntity;
import com.example.lecturesystem.modules.agent.entity.AiPolicyCandidatePhraseEntity;
import com.example.lecturesystem.modules.agent.entity.AiPolicyAnswerLogEntity;
import com.example.lecturesystem.modules.agent.entity.AiPolicyFeedbackEntity;
import com.example.lecturesystem.modules.agent.entity.AiPolicyFeedbackTaskEntity;
import com.example.lecturesystem.modules.agent.entity.AiPolicyUserFavoriteEntity;
import com.example.lecturesystem.modules.agent.mapper.AiPolicyAnswerLogMapper;
import com.example.lecturesystem.modules.agent.mapper.AiPolicyCandidateAnswerMapper;
import com.example.lecturesystem.modules.agent.mapper.AiPolicyCandidatePhraseMapper;
import com.example.lecturesystem.modules.agent.mapper.AiPolicyFeedbackMapper;
import com.example.lecturesystem.modules.agent.mapper.AiPolicyFeedbackTaskMapper;
import com.example.lecturesystem.modules.agent.mapper.AiPolicyUserFavoriteMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class AiPolicyLearningService {
    private final AiPolicyUserFavoriteMapper aiPolicyUserFavoriteMapper;
    private final AiPolicyFeedbackMapper aiPolicyFeedbackMapper;
    private final AiPolicyCandidatePhraseMapper aiPolicyCandidatePhraseMapper;
    private final AiPolicyCandidateAnswerMapper aiPolicyCandidateAnswerMapper;
    private final AiPolicyFeedbackTaskMapper aiPolicyFeedbackTaskMapper;
    private final AiPolicyAnswerLogMapper aiPolicyAnswerLogMapper;

    public AiPolicyLearningService(AiPolicyUserFavoriteMapper aiPolicyUserFavoriteMapper,
                                   AiPolicyFeedbackMapper aiPolicyFeedbackMapper,
                                   AiPolicyCandidatePhraseMapper aiPolicyCandidatePhraseMapper,
                                   AiPolicyCandidateAnswerMapper aiPolicyCandidateAnswerMapper,
                                   AiPolicyFeedbackTaskMapper aiPolicyFeedbackTaskMapper,
                                   AiPolicyAnswerLogMapper aiPolicyAnswerLogMapper) {
        this.aiPolicyUserFavoriteMapper = aiPolicyUserFavoriteMapper;
        this.aiPolicyFeedbackMapper = aiPolicyFeedbackMapper;
        this.aiPolicyCandidatePhraseMapper = aiPolicyCandidatePhraseMapper;
        this.aiPolicyCandidateAnswerMapper = aiPolicyCandidateAnswerMapper;
        this.aiPolicyFeedbackTaskMapper = aiPolicyFeedbackTaskMapper;
        this.aiPolicyAnswerLogMapper = aiPolicyAnswerLogMapper;
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

    @Transactional
    public LearningResult submitFeedbackOnly(Long baseId,
                                             Long userId,
                                             Long sessionId,
                                             Long messageId,
                                             String traceId,
                                             Long intentId,
                                             String rawQuestion,
                                             String normalizedQuestion,
                                             String finalAnswer,
                                             String feedbackType,
                                             String feedbackText,
                                             String policyKey,
                                             String topicType,
                                             String questionType,
                                             String evidenceIds,
                                             String evidenceSource) {
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
        writeFeedbackTask(
                feedback,
                messageId,
                traceId,
                policyKey,
                topicType,
                questionType,
                evidenceIds,
                evidenceSource,
                now
        );
        return new LearningResult(true, false);
    }

    private void writeFeedbackTask(AiPolicyFeedbackEntity feedback,
                                   Long messageId,
                                   String traceId,
                                   String policyKey,
                                   String topicType,
                                   String questionType,
                                   String evidenceIds,
                                   String evidenceSource,
                                   OffsetDateTime now) {
        AiPolicyAnswerLogEntity answerLog = findLatestAnswerLog(feedback.getSessionId(), feedback.getRawQuestion());
        AiPolicyFeedbackTaskEntity task = new AiPolicyFeedbackTaskEntity();
        task.setFeedbackId(feedback.getId());
        task.setBaseId(feedback.getBaseId());
        task.setUserId(feedback.getUserId());
        task.setSessionId(feedback.getSessionId());
        task.setMessageId(messageId);
        task.setTraceId(traceId);
        task.setAnswerLogId(answerLog == null ? null : answerLog.getId());
        task.setQuestion(feedback.getRawQuestion());
        task.setAnswer(feedback.getFinalAnswer());
        task.setFeedbackType(feedback.getFeedbackType());
        task.setFeedbackContent(feedback.getFeedbackText());
        task.setPolicyKey(firstNonBlank(policyKey, answerLog == null ? null : answerLog.getPolicyKey()));
        task.setTopicType(topicType);
        task.setQuestionType(firstNonBlank(questionType, answerLog == null ? null : answerLog.getQuestionType()));
        task.setEvidenceIds(firstNonBlank(evidenceIds, answerLog == null ? null : answerLog.getHitChunkIds()));
        task.setEvidenceSource(evidenceSource);
        task.setRoutePlan(answerLog == null ? null : answerLog.getRoutePlan());
        task.setValidationSummary(answerLog == null ? null : answerLog.getValidationSummary());
        task.setStatus("PENDING");
        task.setPriority(100);
        task.setCreatedAt(now);
        task.setUpdatedAt(now);
        aiPolicyFeedbackTaskMapper.insert(task);
    }

    private AiPolicyAnswerLogEntity findLatestAnswerLog(Long sessionId, String rawQuestion) {
        if (sessionId == null) {
            return null;
        }
        try {
            return aiPolicyAnswerLogMapper.findLatestForFeedback(sessionId, rawQuestion);
        } catch (Exception ex) {
            return null;
        }
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
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
