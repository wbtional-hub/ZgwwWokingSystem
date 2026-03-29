package com.example.lecturesystem.modules.knowledge.vo;

public class KnowledgeChunkListItemVO {
    private Long id;
    private Long documentId;
    private Integer chunkNo;
    private String chunkType;
    private String headingPath;
    private String contentText;
    private String keywordText;
    private Integer contentLength;
    private Integer sortNo;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }
    public Integer getChunkNo() { return chunkNo; }
    public void setChunkNo(Integer chunkNo) { this.chunkNo = chunkNo; }
    public String getChunkType() { return chunkType; }
    public void setChunkType(String chunkType) { this.chunkType = chunkType; }
    public String getHeadingPath() { return headingPath; }
    public void setHeadingPath(String headingPath) { this.headingPath = headingPath; }
    public String getContentText() { return contentText; }
    public void setContentText(String contentText) { this.contentText = contentText; }
    public String getKeywordText() { return keywordText; }
    public void setKeywordText(String keywordText) { this.keywordText = keywordText; }
    public Integer getContentLength() { return contentLength; }
    public void setContentLength(Integer contentLength) { this.contentLength = contentLength; }
    public Integer getSortNo() { return sortNo; }
    public void setSortNo(Integer sortNo) { this.sortNo = sortNo; }
}
