package com.example.lecturesystem.modules.agent.dto;

import java.time.LocalDate;

public class AgentSessionQueryRequest {
    private Long userId;
    private Long skillId;
    private String status;
    private String keywords;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer year;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getSkillId() { return skillId; }
    public void setSkillId(Long skillId) { this.skillId = skillId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getKeywords() { return keywords; }
    public void setKeywords(String keywords) { this.keywords = keywords; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }
}