package com.example.lecturesystem.modules.attendance.vo;

import java.time.LocalDate;

public class AttendanceWeeklyOverviewPointVO {
    private LocalDate date;
    private Boolean workday;
    private Integer shouldAttendCount;
    private Integer checkedInCount;
    private Integer missingCount;
    private Integer lateCount;
    private Integer earlyLeaveCount;
    private Integer overtimeCount;

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public Boolean getWorkday() { return workday; }
    public void setWorkday(Boolean workday) { this.workday = workday; }
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
}
