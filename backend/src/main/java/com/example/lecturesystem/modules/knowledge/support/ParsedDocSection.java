package com.example.lecturesystem.modules.knowledge.support;

public class ParsedDocSection {
    private Integer sectionNo;
    private String headingPath;
    private String chunkType;
    private String contentText;

    public Integer getSectionNo() { return sectionNo; }
    public void setSectionNo(Integer sectionNo) { this.sectionNo = sectionNo; }
    public String getHeadingPath() { return headingPath; }
    public void setHeadingPath(String headingPath) { this.headingPath = headingPath; }
    public String getChunkType() { return chunkType; }
    public void setChunkType(String chunkType) { this.chunkType = chunkType; }
    public String getContentText() { return contentText; }
    public void setContentText(String contentText) { this.contentText = contentText; }
}
