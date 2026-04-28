package com.example.lecturesystem.modules.agent.support.policy;

import com.example.lecturesystem.modules.agent.entity.AiPolicyConditionIndex;
import com.example.lecturesystem.modules.agent.mapper.AiPolicyConditionIndexMapper;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class AiPolicyConditionRetrievalService {

    private final AiPolicyConditionIndexMapper conditionIndexMapper;

    public AiPolicyConditionRetrievalService(AiPolicyConditionIndexMapper conditionIndexMapper) {
        this.conditionIndexMapper = conditionIndexMapper;
    }

    public List<AiPolicyConditionIndex> retrieveByConditions(Long baseId, List<String> conditionCodes) {
        if (baseId == null || conditionCodes == null || conditionCodes.isEmpty()) {
            return Collections.emptyList();
        }

        return conditionIndexMapper.selectByConditionCodes(baseId, conditionCodes);
    }
}