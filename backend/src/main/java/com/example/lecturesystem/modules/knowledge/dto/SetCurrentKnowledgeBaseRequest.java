package com.example.lecturesystem.modules.knowledge.dto;

import jakarta.validation.constraints.NotNull;

public class SetCurrentKnowledgeBaseRequest {
    @NotNull(message = "知识库ID不能为空")
    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
