package com.example.lecturesystem.modules.knowledge.vo;

public class KnowledgeSearchResultVO {
    private Long documentId;
    private Long chunkId;
    private String docTitle;
    private String policyRegion;
    private String headingPath;
    private String snippet;
    private String keywordText;
    private Integer chunkNo;

    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }
    public Long getChunkId() { return chunkId; }
    public void setChunkId(Long chunkId) { this.chunkId = chunkId; }
    public String getDocTitle() { return docTitle; }
    public void setDocTitle(String docTitle) { this.docTitle = docTitle; }
    public String getPolicyRegion() { return policyRegion; }
    public void setPolicyRegion(String policyRegion) { this.policyRegion = policyRegion; }
    public String getHeadingPath() { return headingPath; }
    public void setHeadingPath(String headingPath) { this.headingPath = headingPath; }
    public String getSnippet() { return snippet; }
    public void setSnippet(String snippet) { this.snippet = snippet; }
    public String getKeywordText() { return keywordText; }
    public void setKeywordText(String keywordText) { this.keywordText = keywordText; }
    public Integer getChunkNo() { return chunkNo; }
    public void setChunkNo(Integer chunkNo) { this.chunkNo = chunkNo; }
}
