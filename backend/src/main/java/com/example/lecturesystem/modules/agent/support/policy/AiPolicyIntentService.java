package com.example.lecturesystem.modules.agent.support.policy;

import com.example.lecturesystem.modules.agent.entity.AiPolicyIntentEntity;
import com.example.lecturesystem.modules.agent.mapper.AiPolicyIntentMapper;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class AiPolicyIntentService {
    private final AiPolicyIntentMapper aiPolicyIntentMapper;

    public AiPolicyIntentService(AiPolicyIntentMapper aiPolicyIntentMapper) {
        this.aiPolicyIntentMapper = aiPolicyIntentMapper;
    }

    public List<AiPolicyIntentEntity> queryEnabledByBaseId(Long baseId) {
        if (baseId == null) {
            return List.of();
        }
        try {
            return aiPolicyIntentMapper.queryEnabledByBaseId(baseId);
        } catch (Exception ex) {
            return List.of();
        }
    }

    public AiPolicyIntentEntity findById(Long id) {
        if (id == null) {
            return null;
        }
        try {
            return aiPolicyIntentMapper.findById(id);
        } catch (Exception ex) {
            return null;
        }
    }

    public AiPolicyIntentEntity findByCode(Long baseId, String intentCode) {
        if (baseId == null || intentCode == null || intentCode.isBlank()) {
            return null;
        }
        try {
            return aiPolicyIntentMapper.findByCode(baseId, intentCode);
        } catch (Exception ex) {
            return null;
        }
    }

    public AiPolicyIntentEntity findBestByProfile(Long baseId,
                                                  String regionScope,
                                                  String policyKey,
                                                  String questionType,
                                                  String topicType) {
        return queryEnabledByBaseId(baseId).stream()
                .filter(intent -> regionScope == null || regionScope.isBlank()
                        || intent.getRegionScope() == null
                        || intent.getRegionScope().equalsIgnoreCase(regionScope))
                .filter(intent -> policyKey == null || policyKey.isBlank()
                        || intent.getPolicyKey() == null
                        || intent.getPolicyKey().equalsIgnoreCase(policyKey))
                .filter(intent -> questionType == null || questionType.isBlank()
                        || intent.getQuestionType() == null
                        || intent.getQuestionType().equalsIgnoreCase(questionType))
                .filter(intent -> topicType == null || topicType.isBlank()
                        || intent.getTopicType() == null
                        || intent.getTopicType().equalsIgnoreCase(topicType))
                .sorted(Comparator.comparingInt((AiPolicyIntentEntity item) ->
                        item.getPriority() == null ? 0 : item.getPriority()).reversed())
                .findFirst()
                .orElse(null);
    }
}
