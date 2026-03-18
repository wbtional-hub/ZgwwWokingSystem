package com.example.lecturesystem.modules.workscore.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class WorkScoreListItemVO {
    private Long id;
    private String weekNo;
    private Long unitId;
    private Long userId;
    private String unitName;
    private String username;
    private String realName;
    private Integer attendanceDays;
    private BigDecimal attendanceScore;
    private String weeklyWorkStatus;
    private BigDecimal weeklyWorkScore;
    private BigDecimal disciplineScore;
    private String disciplineRemark;
    private BigDecimal totalScore;
    private Integer level;
    private String status;
    private LocalDateTime calculateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getWeekNo() { return weekNo; }
    public void setWeekNo(String weekNo) { this.weekNo = weekNo; }
    public Long getUnitId() { return unitId; }
    public void setUnitId(Long unitId) { this.unitId = unitId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUnitName() { return unitName; }
    public void setUnitName(String unitName) { this.unitName = unitName; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getRealName() { return realName; }
    public void setRealName(String realName) { this.realName = realName; }
    public Integer getAttendanceDays() { return attendanceDays; }
    public void setAttendanceDays(Integer attendanceDays) { this.attendanceDays = attendanceDays; }
    public BigDecimal getAttendanceScore() { return attendanceScore; }
    public void setAttendanceScore(BigDecimal attendanceScore) { this.attendanceScore = attendanceScore; }
    public String getWeeklyWorkStatus() { return weeklyWorkStatus; }
    public void setWeeklyWorkStatus(String weeklyWorkStatus) { this.weeklyWorkStatus = weeklyWorkStatus; }
    public BigDecimal getWeeklyWorkScore() { return weeklyWorkScore; }
    public void setWeeklyWorkScore(BigDecimal weeklyWorkScore) { this.weeklyWorkScore = weeklyWorkScore; }
    public BigDecimal getDisciplineScore() { return disciplineScore; }
    public void setDisciplineScore(BigDecimal disciplineScore) { this.disciplineScore = disciplineScore; }
    public String getDisciplineRemark() { return disciplineRemark; }
    public void setDisciplineRemark(String disciplineRemark) { this.disciplineRemark = disciplineRemark; }
    public BigDecimal getTotalScore() { return totalScore; }
    public void setTotalScore(BigDecimal totalScore) { this.totalScore = totalScore; }
    public Integer getLevel() { return level; }
    public void setLevel(Integer level) { this.level = level; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCalculateTime() { return calculateTime; }
    public void setCalculateTime(LocalDateTime calculateTime) { this.calculateTime = calculateTime; }
}
