package com.example.lecturesystem.modules.agent.support.policy;

import com.example.lecturesystem.modules.agent.dto.AiPolicyExactPolicyChunk;
import com.example.lecturesystem.modules.agent.mapper.AiPolicyExactPolicyMatchMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AiPolicyExactPolicyMatchService {

    private final AiPolicyExactPolicyMatchMapper exactPolicyMatchMapper;

    public AiPolicyExactPolicyMatchService(AiPolicyExactPolicyMatchMapper exactPolicyMatchMapper) {
        this.exactPolicyMatchMapper = exactPolicyMatchMapper;
    }

    public List<AiPolicyExactPolicyChunk> match(Long baseId, String question) {
        String keyword = normalizeKeyword(question);
        if (baseId == null || keyword == null || keyword.isBlank()) {
            return List.of();
        }

        if (!isLikelyExactPolicyName(keyword)) {
            return List.of();
        }

        return exactPolicyMatchMapper.selectExactPolicyChunks(baseId, keyword);
    }

    private boolean isLikelyExactPolicyName(String keyword) {
    if (keyword == null || keyword.isBlank()) {
        return false;
    }

    /*
     * 这些是问答意图，不是精确政策名。
     * 例如“厦门有哪些住房类人才政策？”不能走 policy_name 精确命中，
     * 应该走条件反查 / 专题路由。
     */
    if (containsAny(keyword, List.of(
            "可以申请",
            "能申请",
            "申请什么",
            "有什么政策",
            "有哪些政策",
            "哪些政策",
            "有哪些",
            "有什么",
            "怎么申请",
            "如何申请",
            "申请条件",
            "补助多少",
            "补贴多少",
            "可以享受",
            "能享受"
    ))) {
        return false;
    }

    /*
     * “住房”本身不是精确政策名，不能因为含有“住房”就走精确命中。
     * 精确命中应更偏向正式政策标题。
     */
    return keyword.contains("实施办法")
            || keyword.contains("实施意见")
            || keyword.contains("若干措施")
            || keyword.contains("管理细则")
            || keyword.contains("工作方案")
            || keyword.contains("专项资金")
            || keyword.contains("双百计划")
            || keyword.contains("特聘岗位")
            || keyword.contains("专题");
}

    private String normalizeKeyword(String question) {
        if (question == null) {
            return "";
        }

        String value = question
                .replaceAll("\\s+", "")
                .replaceAll("[？?。！!，,、；;：:]", "")
                .trim();

        return value.isBlank() ? "" : value;
    }

    private boolean containsAny(String text, List<String> keywords) {
        if (text == null || keywords == null) {
            return false;
        }

        for (String keyword : keywords) {
            if (keyword != null && !keyword.isBlank() && text.contains(keyword)) {
                return true;
            }
        }

        return false;
    }
}