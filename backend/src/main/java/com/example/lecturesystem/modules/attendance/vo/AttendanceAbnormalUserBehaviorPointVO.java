package com.example.lecturesystem.modules.attendance.vo;

import java.time.LocalDateTime;

public class AttendanceAbnormalUserBehaviorPointVO {
    private String checkInAddress;
    private LocalDateTime checkInTime;

    public String getCheckInAddress() {
        return checkInAddress;
    }

    public void setCheckInAddress(String checkInAddress) {
        this.checkInAddress = checkInAddress;
    }

    public LocalDateTime getCheckInTime() {
        return checkInTime;
    }

    public void setCheckInTime(LocalDateTime checkInTime) {
        this.checkInTime = checkInTime;
    }
}
