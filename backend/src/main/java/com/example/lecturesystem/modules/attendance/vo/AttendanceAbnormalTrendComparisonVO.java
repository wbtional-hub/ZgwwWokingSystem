package com.example.lecturesystem.modules.attendance.vo;

public class AttendanceAbnormalTrendComparisonVO {
    private Long userId;
    private Long recent7DayAbnormalCount;
    private Long previous7DayAbnormalCount;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getRecent7DayAbnormalCount() {
        return recent7DayAbnormalCount;
    }

    public void setRecent7DayAbnormalCount(Long recent7DayAbnormalCount) {
        this.recent7DayAbnormalCount = recent7DayAbnormalCount;
    }

    public Long getPrevious7DayAbnormalCount() {
        return previous7DayAbnormalCount;
    }

    public void setPrevious7DayAbnormalCount(Long previous7DayAbnormalCount) {
        this.previous7DayAbnormalCount = previous7DayAbnormalCount;
    }
}
