package com.example.lecturesystem.modules.knowledge.vo;

public class KnowledgeImportResultVO {
    private Long jobId;
    private Long documentId;
    private String fileName;
    private String jobStatus;
    private Integer totalChunks;
    private Integer successChunks;
    private String errorMessage;

    public Long getJobId() { return jobId; }
    public void setJobId(Long jobId) { this.jobId = jobId; }
    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getJobStatus() { return jobStatus; }
    public void setJobStatus(String jobStatus) { this.jobStatus = jobStatus; }
    public Integer getTotalChunks() { return totalChunks; }
    public void setTotalChunks(Integer totalChunks) { this.totalChunks = totalChunks; }
    public Integer getSuccessChunks() { return successChunks; }
    public void setSuccessChunks(Integer successChunks) { this.successChunks = successChunks; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
