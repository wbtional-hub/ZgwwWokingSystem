package com.example.lecturesystem.modules.agent.controller;

import com.example.lecturesystem.common.ApiResponse;
import com.example.lecturesystem.modules.agent.support.policy.AiPolicyEvaluationService;
import com.example.lecturesystem.modules.agent.support.policy.AiPolicyKnowledgeImportService;
import com.example.lecturesystem.modules.agent.support.policy.AiPolicyReviewService;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.time.OffsetDateTime;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/agent/policy-admin")
public class AiPolicyAdminController {
    private static final String BOOTSTRAP_DEBUG_MARKER = "bootstrap-v2-live-check-20260421";
    private static final String IMPORT_DEBUG_MARKER = "import-v2-live-check-20260421";
    private static final String INSTANCE_STARTED_AT = OffsetDateTime.now().toString();

    private final AiPolicyKnowledgeImportService aiPolicyKnowledgeImportService;
    private final AiPolicyEvaluationService aiPolicyEvaluationService;
    private final AiPolicyReviewService aiPolicyReviewService;
    private final DataSource dataSource;

    public AiPolicyAdminController(AiPolicyKnowledgeImportService aiPolicyKnowledgeImportService,
                                   AiPolicyEvaluationService aiPolicyEvaluationService,
                                   AiPolicyReviewService aiPolicyReviewService,
                                   DataSource dataSource) {
        this.aiPolicyKnowledgeImportService = aiPolicyKnowledgeImportService;
        this.aiPolicyEvaluationService = aiPolicyEvaluationService;
        this.aiPolicyReviewService = aiPolicyReviewService;
        this.dataSource = dataSource;
    }

    @PostMapping("/reset")
    public ApiResponse<?> reset(@RequestParam("baseId") Long baseId) {
        return ApiResponse.success(aiPolicyKnowledgeImportService.resetBase(baseId));
    }

    @PostMapping("/bootstrap")
    public ApiResponse<?> bootstrap(@RequestParam("baseId") Long baseId) {
        AiPolicyKnowledgeImportService.BootstrapResult result = aiPolicyKnowledgeImportService.bootstrapBase(baseId);
        return ApiResponse.success(new BootstrapDebugResponse(
                BOOTSTRAP_DEBUG_MARKER,
                INSTANCE_STARTED_AT,
                buildDatabaseDebugInfo(),
                result,
                aiPolicyKnowledgeImportService.queryDebugSnapshot(baseId)
        ));
    }

    @PostMapping("/import-docx")
    public ApiResponse<?> importDocx(@RequestParam("baseId") Long baseId,
                                     @RequestParam("regionScope") String regionScope,
                                     @RequestParam("docType") String docType,
                                     @RequestParam(value = "sourceType", required = false) String sourceType,
                                     @RequestParam(value = "policyKey", required = false) String policyKey,
                                     @RequestParam(value = "policyName", required = false) String policyName,
                                     @RequestParam("file") MultipartFile file) throws Exception {
        AiPolicyKnowledgeImportService.ImportResult result = aiPolicyKnowledgeImportService.importDocument(
                new AiPolicyKnowledgeImportService.ImportCommand(baseId, regionScope, docType, sourceType, policyKey, policyName),
                file
        );
        AiPolicyKnowledgeImportService.DebugSnapshot snapshot = aiPolicyKnowledgeImportService.queryDebugSnapshot(baseId);
        return ApiResponse.success(new ImportDebugResponse(
                IMPORT_DEBUG_MARKER,
                INSTANCE_STARTED_AT,
                buildDatabaseDebugInfo(),
                new ImportResultView(
                        result.baseId(),
                        result.documentId(),
                        result.documentName(),
                        result.regionScope(),
                        result.docType(),
                        result.policyKey(),
                        snapshot.documentCount(),
                        snapshot.chunkCount()
                ),
                snapshot
        ));
    }

    @PostMapping("/evaluate")
    public ApiResponse<?> evaluate(@RequestParam("baseId") Long baseId) {
        return ApiResponse.success(aiPolicyEvaluationService.evaluate(baseId));
    }

    @GetMapping("/candidate-phrases")
    public ApiResponse<?> candidatePhrases(@RequestParam("baseId") Long baseId,
                                           @RequestParam(value = "reviewStatus", required = false) String reviewStatus) {
        return ApiResponse.success(aiPolicyReviewService.queryCandidatePhrases(baseId, reviewStatus));
    }

    @PostMapping("/candidate-phrases/review")
    public ApiResponse<?> reviewCandidatePhrase(@RequestBody ReviewRequest request) {
        return ApiResponse.success(aiPolicyReviewService.reviewCandidatePhrase(
                request.candidateId(),
                request.action(),
                request.targetIntentId()
        ));
    }

    @GetMapping("/candidate-answers")
    public ApiResponse<?> candidateAnswers(@RequestParam("baseId") Long baseId,
                                           @RequestParam(value = "reviewStatus", required = false) String reviewStatus) {
        return ApiResponse.success(aiPolicyReviewService.queryCandidateAnswers(baseId, reviewStatus));
    }

    @PostMapping("/candidate-answers/review")
    public ApiResponse<?> reviewCandidateAnswer(@RequestBody ReviewRequest request) {
        return ApiResponse.success(aiPolicyReviewService.reviewCandidateAnswer(
                request.candidateId(),
                request.action(),
                request.targetIntentId()
        ));
    }

    @GetMapping("/dashboard")
    public ApiResponse<?> dashboard(@RequestParam("baseId") Long baseId) {
        return ApiResponse.success(aiPolicyReviewService.queryDashboard(baseId));
    }

    public record ReviewRequest(Long candidateId,
                                String action,
                                Long targetIntentId) {
    }

    private DatabaseDebugInfo buildDatabaseDebugInfo() {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            return new DatabaseDebugInfo(
                    metaData.getDatabaseProductName(),
                    metaData.getDatabaseProductVersion(),
                    metaData.getURL(),
                    metaData.getUserName()
            );
        } catch (Exception ex) {
            return new DatabaseDebugInfo("unknown", ex.getClass().getSimpleName(), "unavailable", "unavailable");
        }
    }

    public record BootstrapDebugResponse(String debugMarker,
                                         String instanceStartedAt,
                                         DatabaseDebugInfo database,
                                         AiPolicyKnowledgeImportService.BootstrapResult result,
                                         AiPolicyKnowledgeImportService.DebugSnapshot databaseCounts) {
    }

    public record ImportDebugResponse(String debugMarker,
                                      String instanceStartedAt,
                                      DatabaseDebugInfo database,
                                      ImportResultView result,
                                      AiPolicyKnowledgeImportService.DebugSnapshot databaseCounts) {
    }

    public record ImportResultView(Long baseId,
                                   Long documentId,
                                   String documentName,
                                   String regionScope,
                                   String docType,
                                   String policyKey,
                                   int documentCount,
                                   int chunkCount) {
    }

    public record DatabaseDebugInfo(String productName,
                                    String productVersion,
                                    String jdbcUrl,
                                    String userName) {
    }
}
