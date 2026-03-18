package com.example.lecturesystem.modules.workscore.vo;

public class WorkScoreCandidateVO {
    private Long unitId;
    private Long userId;
    private Integer attendanceDays;
    private String weeklyWorkStatus;

    public Long getUnitId() { return unitId; }
    public void setUnitId(Long unitId) { this.unitId = unitId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Integer getAttendanceDays() { return attendanceDays; }
    public void setAttendanceDays(Integer attendanceDays) { this.attendanceDays = attendanceDays; }
    public String getWeeklyWorkStatus() { return weeklyWorkStatus; }
    public void setWeeklyWorkStatus(String weeklyWorkStatus) { this.weeklyWorkStatus = weeklyWorkStatus; }
}
