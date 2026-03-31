package com.example.lecturesystem.modules.knowledge.controller;

import com.example.lecturesystem.common.ApiResponse;
import com.example.lecturesystem.modules.knowledge.dto.ImportKnowledgeDocxRequest;
import com.example.lecturesystem.modules.knowledge.dto.KnowledgeBaseQueryRequest;
import com.example.lecturesystem.modules.knowledge.dto.KnowledgeChunkQueryRequest;
import com.example.lecturesystem.modules.knowledge.dto.KnowledgeDocumentQueryRequest;
import com.example.lecturesystem.modules.knowledge.dto.KnowledgeSearchRequest;
import com.example.lecturesystem.modules.knowledge.dto.SaveKnowledgeBaseRequest;
import com.example.lecturesystem.modules.knowledge.dto.SetCurrentKnowledgeBaseRequest;
import com.example.lecturesystem.modules.knowledge.dto.ToggleKnowledgeBaseStatusRequest;
import com.example.lecturesystem.modules.knowledge.dto.WebKnowledgeImportRequest;
import com.example.lecturesystem.modules.knowledge.dto.WebKnowledgePreviewRequest;
import com.example.lecturesystem.modules.knowledge.service.KnowledgeService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeController {
    private final KnowledgeService knowledgeService;

    public KnowledgeController(KnowledgeService knowledgeService) {
        this.knowledgeService = knowledgeService;
    }

    @PostMapping("/base/list")
    public ApiResponse<?> listBases(@RequestBody(required = false) KnowledgeBaseQueryRequest request) {
        return ApiResponse.success(knowledgeService.listBases(request == null ? new KnowledgeBaseQueryRequest() : request));
    }

    @PostMapping("/base/save")
    public ApiResponse<?> saveBase(@Validated @RequestBody SaveKnowledgeBaseRequest request) {
        return ApiResponse.success(knowledgeService.saveBase(request));
    }

    @PostMapping("/base/toggle-status")
    public ApiResponse<?> toggleBaseStatus(@Validated @RequestBody ToggleKnowledgeBaseStatusRequest request) {
        knowledgeService.toggleBaseStatus(request);
        return ApiResponse.success("ok");
    }

    @PostMapping("/base/set-current")
    public ApiResponse<?> setCurrentBase(@Validated @RequestBody SetCurrentKnowledgeBaseRequest request) {
        knowledgeService.setCurrentBase(request);
        return ApiResponse.success("ok");
    }

    @DeleteMapping("/base/{id}")
    public ApiResponse<?> deleteBase(@PathVariable("id") Long id) {
        knowledgeService.deleteBase(id);
        return ApiResponse.success("ok");
    }

    @PostMapping("/web-preview")
    public ApiResponse<?> previewWebContent(@Validated @RequestBody WebKnowledgePreviewRequest request) {
        return ApiResponse.success(knowledgeService.previewWebContent(request));
    }

    @PostMapping("/web-import")
    public ApiResponse<?> importWebContent(@Validated @RequestBody WebKnowledgeImportRequest request) {
        return ApiResponse.success(knowledgeService.importWebContent(request));
    }

    @PostMapping("/snapshot-preview")
    public ApiResponse<?> previewSnapshotContent(@RequestParam("baseId") Long baseId,
                                                 @RequestParam("file") MultipartFile file) {
        return ApiResponse.success(knowledgeService.previewSnapshotContent(baseId, file));
    }

    @PostMapping("/snapshot-import")
    public ApiResponse<?> importSnapshotContent(@RequestParam("baseId") Long baseId,
                                                @RequestParam("status") Integer status,
                                                @RequestParam("title") String title,
                                                @RequestParam("summary") String summary,
                                                @RequestParam("content") String content,
                                                @RequestParam(value = "sourceUrl", required = false) String sourceUrl,
                                                @RequestParam(value = "fileName", required = false) String fileName,
                                                @RequestParam(value = "file", required = false) MultipartFile file) {
        return ApiResponse.success(knowledgeService.importSnapshotContent(baseId, status, title, summary, content, sourceUrl, fileName, file));
    }

    @PostMapping("/document/list")
    public ApiResponse<?> listDocuments(@RequestBody(required = false) KnowledgeDocumentQueryRequest request) {
        return ApiResponse.success(knowledgeService.listDocuments(request == null ? new KnowledgeDocumentQueryRequest() : request));
    }

    @GetMapping("/document/{id}")
    public ApiResponse<?> getDocumentDetail(@PathVariable("id") Long id) {
        return ApiResponse.success(knowledgeService.getDocumentDetail(id));
    }

    @PostMapping("/chunk/list")
    public ApiResponse<?> listChunks(@Validated @RequestBody KnowledgeChunkQueryRequest request) {
        return ApiResponse.success(knowledgeService.listChunks(request));
    }

    @PostMapping("/document/import-docx")
    public ApiResponse<?> importDocx(@ModelAttribute ImportKnowledgeDocxRequest request,
                                     @RequestParam("file") MultipartFile file) {
        return ApiResponse.success(knowledgeService.importDocx(request, file));
    }

    @PostMapping("/search")
    public ApiResponse<?> search(@Validated @RequestBody KnowledgeSearchRequest request) {
        return ApiResponse.success(knowledgeService.search(request));
    }
}
