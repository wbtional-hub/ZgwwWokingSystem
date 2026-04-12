package com.example.lecturesystem.modules.attendance.vo;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class AttendanceTeamStatisticsVO {
    private LocalDate date;
    private Boolean workday;
    private Long scopeUserCount;
    private String scopeDescription;
    private Integer shouldAttendCount;
    private Integer checkedInCount;
    private Integer missingCount;
    private Integer lateCount;
    private Integer earlyLeaveCount;
    private Integer overtimeCount;
    private String nonWorkdayNotice;
    private List<AttendanceTeamMemberStatusVO> missingUsers = new ArrayList<>();
    private List<AttendanceTeamMemberStatusVO> abnormalUsers = new ArrayList<>();
    private List<AttendanceTeamMemberStatusVO> overtimeUsers = new ArrayList<>();
    private List<AttendanceTeamMemberStatusVO> recentMembers = new ArrayList<>();
    private List<AttendanceWeeklyOverviewPointVO> weeklyOverview = new ArrayList<>();

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public Boolean getWorkday() { return workday; }
    public void setWorkday(Boolean workday) { this.workday = workday; }
    public Long getScopeUserCount() { return scopeUserCount; }
    public void setScopeUserCount(Long scopeUserCount) { this.scopeUserCount = scopeUserCount; }
    public String getScopeDescription() { return scopeDescription; }
    public void setScopeDescription(String scopeDescription) { this.scopeDescription = scopeDescription; }
    public Integer getShouldAttendCount() { return shouldAttendCount; }
    public void setShouldAttendCount(Integer shouldAttendCount) { this.shouldAttendCount = shouldAttendCount; }
    public Integer getCheckedInCount() { return checkedInCount; }
    public void setCheckedInCount(Integer checkedInCount) { this.checkedInCount = checkedInCount; }
    public Integer getMissingCount() { return missingCount; }
    public void setMissingCount(Integer missingCount) { this.missingCount = missingCount; }
    public Integer getLateCount() { return lateCount; }
    public void setLateCount(Integer lateCount) { this.lateCount = lateCount; }
    public Integer getEarlyLeaveCount() { return earlyLeaveCount; }
    public void setEarlyLeaveCount(Integer earlyLeaveCount) { this.earlyLeaveCount = earlyLeaveCount; }
    public Integer getOvertimeCount() { return overtimeCount; }
    public void setOvertimeCount(Integer overtimeCount) { this.overtimeCount = overtimeCount; }
    public String getNonWorkdayNotice() { return nonWorkdayNotice; }
    public void setNonWorkdayNotice(String nonWorkdayNotice) { this.nonWorkdayNotice = nonWorkdayNotice; }
    public List<AttendanceTeamMemberStatusVO> getMissingUsers() { return missingUsers; }
    public void setMissingUsers(List<AttendanceTeamMemberStatusVO> missingUsers) { this.missingUsers = missingUsers; }
    public List<AttendanceTeamMemberStatusVO> getAbnormalUsers() { return abnormalUsers; }
    public void setAbnormalUsers(List<AttendanceTeamMemberStatusVO> abnormalUsers) { this.abnormalUsers = abnormalUsers; }
    public List<AttendanceTeamMemberStatusVO> getOvertimeUsers() { return overtimeUsers; }
    public void setOvertimeUsers(List<AttendanceTeamMemberStatusVO> overtimeUsers) { this.overtimeUsers = overtimeUsers; }
    public List<AttendanceTeamMemberStatusVO> getRecentMembers() { return recentMembers; }
    public void setRecentMembers(List<AttendanceTeamMemberStatusVO> recentMembers) { this.recentMembers = recentMembers; }
    public List<AttendanceWeeklyOverviewPointVO> getWeeklyOverview() { return weeklyOverview; }
    public void setWeeklyOverview(List<AttendanceWeeklyOverviewPointVO> weeklyOverview) { this.weeklyOverview = weeklyOverview; }
}
