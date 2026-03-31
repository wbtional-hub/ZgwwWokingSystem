package com.example.lecturesystem.modules.knowledge.service;

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
import com.example.lecturesystem.modules.knowledge.vo.KnowledgeImportResultVO;
import com.example.lecturesystem.modules.knowledge.vo.WebKnowledgeImportResultVO;
import com.example.lecturesystem.modules.knowledge.vo.WebKnowledgePreviewVO;
import org.springframework.web.multipart.MultipartFile;

public interface KnowledgeService {
    Object listBases(KnowledgeBaseQueryRequest request);
    Long saveBase(SaveKnowledgeBaseRequest request);
    void toggleBaseStatus(ToggleKnowledgeBaseStatusRequest request);
    void setCurrentBase(SetCurrentKnowledgeBaseRequest request);
    void deleteBase(Long id);
    WebKnowledgePreviewVO previewWebContent(WebKnowledgePreviewRequest request);
    WebKnowledgeImportResultVO importWebContent(WebKnowledgeImportRequest request);
    WebKnowledgePreviewVO previewSnapshotContent(Long baseId, MultipartFile file);
    WebKnowledgeImportResultVO importSnapshotContent(Long baseId, Integer status, String title, String summary, String content,
                                                    String sourceUrl, String fileName, MultipartFile file);
    Object listDocuments(KnowledgeDocumentQueryRequest request);
    Object getDocumentDetail(Long id);
    Object listChunks(KnowledgeChunkQueryRequest request);
    KnowledgeImportResultVO importDocx(ImportKnowledgeDocxRequest request, MultipartFile file);
    Object search(KnowledgeSearchRequest request);
}
