package com.example.lecturesystem.modules.attendance.vo;

public class AttendanceStatusCountVO {
    private String checkInStatus;
    private Long count;

    public String getCheckInStatus() {
        return checkInStatus;
    }

    public void setCheckInStatus(String checkInStatus) {
        this.checkInStatus = checkInStatus;
    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }
}
