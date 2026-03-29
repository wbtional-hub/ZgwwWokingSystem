package com.example.lecturesystem.modules.knowledge.dto;

import jakarta.validation.constraints.NotNull;

public class KnowledgeChunkQueryRequest {
    @NotNull(message = "鏂囨。ID涓嶈兘涓虹┖")
    private Long documentId;

    public Long getDocumentId() {
        return documentId;
    }

    public void setDocumentId(Long documentId) {
        this.documentId = documentId;
    }
}
