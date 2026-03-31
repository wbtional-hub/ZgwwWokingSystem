package com.example.lecturesystem.modules.knowledge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class WebKnowledgeImportRequest {
    @NotNull(message = "知识库ID不能为空")
    private Long baseId;

    @NotBlank(message = "网页地址不能为空")
    private String url;

    @NotBlank(message = "网页标题不能为空")
    private String title;

    @NotBlank(message = "网页摘要不能为空")
    private String summary;

    @NotBlank(message = "网页正文不能为空")
    private String content;

    @NotNull(message = "状态不能为空")
    private Integer status;

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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
