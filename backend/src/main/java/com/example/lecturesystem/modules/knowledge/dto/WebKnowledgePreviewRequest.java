package com.example.lecturesystem.modules.knowledge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class WebKnowledgePreviewRequest {
    @NotNull(message = "知识库ID不能为空")
    private Long baseId;

    @NotBlank(message = "网页地址不能为空")
    private String url;

    public Long getBaseId() {
        return baseId;
    }

    public void setBaseId(Long baseId) {
        this.baseId = baseId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
