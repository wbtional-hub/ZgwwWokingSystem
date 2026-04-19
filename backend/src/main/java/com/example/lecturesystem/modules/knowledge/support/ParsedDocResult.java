package com.example.lecturesystem.modules.knowledge.support;

import java.util.ArrayList;
import java.util.List;

public class ParsedDocResult {
    private String title;
    private String summary;
    private String structuredDocType;
    private String regionScope;
    private Boolean searchable;
    private List<ParsedDocSection> sections = new ArrayList<>();

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getStructuredDocType() { return structuredDocType; }
    public void setStructuredDocType(String structuredDocType) { this.structuredDocType = structuredDocType; }
    public String getRegionScope() { return regionScope; }
    public void setRegionScope(String regionScope) { this.regionScope = regionScope; }
    public Boolean getSearchable() { return searchable; }
    public void setSearchable(Boolean searchable) { this.searchable = searchable; }
    public List<ParsedDocSection> getSections() { return sections; }
    public void setSections(List<ParsedDocSection> sections) { this.sections = sections; }
}
