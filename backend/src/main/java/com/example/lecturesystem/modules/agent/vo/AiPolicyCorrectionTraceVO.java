package com.example.lecturesystem.modules.agent.vo;

import com.example.lecturesystem.modules.agent.entity.AiPolicyAnswerLogEntity;
import com.example.lecturesystem.modules.agent.entity.AiPolicyFeedbackTaskEntity;

import java.util.List;

public record AiPolicyCorrectionTraceVO(AiPolicyFeedbackTaskEntity task,
                                        AiPolicyAnswerLogEntity answerLog,
                                        List<AiPolicyMessageContextVO> messages,
                                        String answerMode,
                                        String routePlan,
                                        Boolean faqHit,
                                        Boolean fallbackFlag,
                                        String matchedIntentCode,
                                        Boolean candidateGenerated,
                                        String validationSummary,
                                        String hitChunkIds,
                                        String finalAnswer) {
}
