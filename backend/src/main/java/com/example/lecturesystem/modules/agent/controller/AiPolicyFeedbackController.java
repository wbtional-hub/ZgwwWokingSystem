package com.example.lecturesystem.modules.agent.controller;

import com.example.lecturesystem.common.ApiResponse;
import com.example.lecturesystem.modules.agent.support.policy.AiPolicyLearningService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agent/policy-feedback")
public class AiPolicyFeedbackController {
    private final AiPolicyLearningService aiPolicyLearningService;

    public AiPolicyFeedbackController(AiPolicyLearningService aiPolicyLearningService) {
        this.aiPolicyLearningService = aiPolicyLearningService;
    }

    @PostMapping("/favorite")
    public ApiResponse<?> favorite(@RequestBody FeedbackRequest request) {
        return ApiResponse.success(aiPolicyLearningService.favorite(
                request.baseId(),
                request.userId(),
                request.sessionId(),
                request.regionScope(),
                request.policyKey(),
                request.intentId(),
                request.rawQuestion(),
                request.normalizedQuestion(),
                request.finalAnswer()
        ));
    }

    @PostMapping("/like")
    public ApiResponse<?> like(@RequestBody FeedbackRequest request) {
        return ApiResponse.success(writeFeedback(request, "like"));
    }

    @PostMapping("/dislike")
    public ApiResponse<?> dislike(@RequestBody FeedbackRequest request) {
        return ApiResponse.success(writeFeedback(request, "dislike"));
    }

    @PostMapping("/correct")
    public ApiResponse<?> correct(@RequestBody FeedbackRequest request) {
        return ApiResponse.success(writeFeedback(request, "correct"));
    }

    @PostMapping("/submit")
    public ApiResponse<?> submit(@RequestBody FeedbackSubmitRequest request) {
        return ApiResponse.success(aiPolicyLearningService.submitFeedbackOnly(
                request.baseId(),
                request.userId(),
                request.sessionId(),
                request.messageId(),
                request.traceId(),
                request.intentId(),
                request.question(),
                request.question(),
                request.answer(),
                request.feedbackType(),
                request.feedbackContent(),
                request.policyKey(),
                request.topicType(),
                request.questionType(),
                request.evidenceIds(),
                request.evidenceSource()
        ));
    }

    private AiPolicyLearningService.LearningResult writeFeedback(FeedbackRequest request, String feedbackType) {
        return aiPolicyLearningService.feedback(
                request.baseId(),
                request.userId(),
                request.sessionId(),
                request.regionScope(),
                request.policyKey(),
                request.intentId(),
                request.rawQuestion(),
                request.normalizedQuestion(),
                request.finalAnswer(),
                feedbackType,
                request.feedbackText()
        );
    }

    public record FeedbackRequest(Long baseId,
                                  Long userId,
                                  Long sessionId,
                                  String regionScope,
                                  String policyKey,
                                  Long intentId,
                                  String rawQuestion,
                                  String normalizedQuestion,
                                  String finalAnswer,
                                  String feedbackText) {
    }

    public record FeedbackSubmitRequest(Long baseId,
                                        Long userId,
                                        Long sessionId,
                                        Long messageId,
                                        String traceId,
                                        String question,
                                        String answer,
                                        String feedbackType,
                                        String feedbackContent,
                                        Boolean submitContext,
                                        String policyKey,
                                        String topicType,
                                        String questionType,
                                        String evidenceIds,
                                        String evidenceSource,
                                        Long intentId) {
    }
}
