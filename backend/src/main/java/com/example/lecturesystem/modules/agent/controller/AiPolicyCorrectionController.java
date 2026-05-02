package com.example.lecturesystem.modules.agent.controller;

import com.example.lecturesystem.common.ApiResponse;
import com.example.lecturesystem.modules.agent.dto.AiPolicyCorrectionTaskQueryRequest;
import com.example.lecturesystem.modules.agent.dto.PolicyDraftRequest;
import com.example.lecturesystem.modules.agent.support.policy.AiPolicyCorrectionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agent/policy-corrections")
public class AiPolicyCorrectionController {
    private final AiPolicyCorrectionService correctionService;

    public AiPolicyCorrectionController(AiPolicyCorrectionService correctionService) {
        this.correctionService = correctionService;
    }

    @PostMapping("/list")
    public ApiResponse<?> list(@RequestBody(required = false) AiPolicyCorrectionTaskQueryRequest request) {
        return ApiResponse.success(correctionService.queryTasks(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<?> detail(@PathVariable("id") Long id) {
        return ApiResponse.success(correctionService.getDetail(id));
    }

    @GetMapping("/{id}/trace")
    public ApiResponse<?> trace(@PathVariable("id") Long id) {
        return ApiResponse.success(correctionService.getTrace(id));
    }

    @GetMapping("/{id}/evidence")
    public ApiResponse<?> evidence(@PathVariable("id") Long id) {
        return ApiResponse.success(correctionService.getEvidence(id));
    }

    @PostMapping("/{id}/status")
    public ApiResponse<?> updateStatus(@PathVariable("id") Long id,
                                       @RequestBody StatusRequest request) {
        return ApiResponse.success(correctionService.updateStatus(id, request.status(), request.handleResult()));
    }

    @PostMapping("/{id}/comment")
    public ApiResponse<?> comment(@PathVariable("id") Long id,
                                  @RequestBody CommentRequest request) {
        return ApiResponse.success(correctionService.comment(id, request.actionContent()));
    }

    @PostMapping("/{id}/draft/faq")
    public ApiResponse<?> saveFaqDraft(@PathVariable("id") Long id,
                                       @RequestBody PolicyDraftRequest request) {
        return ApiResponse.success(correctionService.saveFaqDraft(id, request));
    }

    @PostMapping("/{id}/draft/policy-chunk")
    public ApiResponse<?> savePolicyChunkDraft(@PathVariable("id") Long id,
                                               @RequestBody PolicyDraftRequest request) {
        return ApiResponse.success(correctionService.savePolicyChunkDraft(id, request));
    }

    @PostMapping("/{id}/close")
    public ApiResponse<?> close(@PathVariable("id") Long id,
                                @RequestBody(required = false) StatusRequest request) {
        return ApiResponse.success(correctionService.close(id, request == null ? null : request.handleResult()));
    }

    public record StatusRequest(String status,
                                String handleResult) {
    }

    public record CommentRequest(String actionContent) {
    }
}
