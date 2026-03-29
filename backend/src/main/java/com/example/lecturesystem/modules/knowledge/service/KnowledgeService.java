package com.example.lecturesystem.modules.knowledge.service;

import com.example.lecturesystem.modules.knowledge.dto.ImportKnowledgeDocxRequest;
import com.example.lecturesystem.modules.knowledge.dto.KnowledgeBaseQueryRequest;
import com.example.lecturesystem.modules.knowledge.dto.KnowledgeChunkQueryRequest;
import com.example.lecturesystem.modules.knowledge.dto.KnowledgeDocumentQueryRequest;
import com.example.lecturesystem.modules.knowledge.dto.KnowledgeSearchRequest;
import com.example.lecturesystem.modules.knowledge.dto.SaveKnowledgeBaseRequest;
import com.example.lecturesystem.modules.knowledge.dto.ToggleKnowledgeBaseStatusRequest;
import com.example.lecturesystem.modules.knowledge.vo.KnowledgeImportResultVO;
import org.springframework.web.multipart.MultipartFile;

public interface KnowledgeService {
    Object listBases(KnowledgeBaseQueryRequest request);
    Long saveBase(SaveKnowledgeBaseRequest request);
    void toggleBaseStatus(ToggleKnowledgeBaseStatusRequest request);
    Object listDocuments(KnowledgeDocumentQueryRequest request);
    Object getDocumentDetail(Long id);
    Object listChunks(KnowledgeChunkQueryRequest request);
    KnowledgeImportResultVO importDocx(ImportKnowledgeDocxRequest request, MultipartFile file);
    Object search(KnowledgeSearchRequest request);
}
