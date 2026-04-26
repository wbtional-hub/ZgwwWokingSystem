package com.example.lecturesystem.modules.agent.controller;

import com.example.lecturesystem.common.ApiResponse;
import com.example.lecturesystem.modules.agent.support.policy.AiPolicyIntentSuggestService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/agent/policy-intent")
public class AiPolicyIntentController {
    private final AiPolicyIntentSuggestService aiPolicyIntentSuggestService;

    public AiPolicyIntentController(AiPolicyIntentSuggestService aiPolicyIntentSuggestService) {
        this.aiPolicyIntentSuggestService = aiPolicyIntentSuggestService;
    }

    @GetMapping("/suggest")
    public ApiResponse<?> suggest(@RequestParam("baseId") Long baseId,
                                  @RequestParam("q") String input,
                                  @RequestParam(value = "userId", required = false) Long userId) {
        AiPolicyIntentSuggestService.SuggestResult result = aiPolicyIntentSuggestService.suggest(baseId, userId, input);
        List<SuggestItem> items = result.items().stream()
                .map(item -> new SuggestItem(
        item.intent().getId(),
        item.intent().getIntentCode(),
        item.intent().getIntentName(),
        item.intent().getStandardQuestion(),
        item.intent().getPolicyKey(),
        item.intent().getRegionScope(),
        item.intent().getQuestionType(),
        item.intent().getTopicType(),
        item.hotScore()
))
                .toList();
        return ApiResponse.success(new SuggestResponse(result.suggestLogId(), items));
    }

    @PostMapping("/select")
    public ApiResponse<?> select(@RequestBody SelectRequest request) {
        aiPolicyIntentSuggestService.selectSuggestion(request.suggestLogId(), request.selectedIntentId());
        return ApiResponse.success("ok");
    }

    public record SuggestResponse(Long suggestLogId,
                                  List<SuggestItem> items) {
    }

    public record SuggestItem(Long intentId,
                          String intentCode,
                          String intentName,
                          String standardQuestion,
                          String policyKey,
                          String regionScope,
                          String questionType,
                          String topicType,
                          int hotScore) {
}

    public record SelectRequest(Long suggestLogId,
                                Long selectedIntentId) {
    }
}
