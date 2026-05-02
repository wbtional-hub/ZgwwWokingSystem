package com.example.lecturesystem.modules.agent.vo;

public record AiPolicyEvidenceItemVO(String evidenceType,
                                     Long id,
                                     String title,
                                     String policyKey,
                                     String policyName,
                                     String topicType,
                                     String sourceScene,
                                     String contentPreview,
                                     String fullContent) {
}
