package com.example.lecturesystem.modules.attendance.vo;

import java.util.ArrayList;
import java.util.List;

public class AttendanceAbnormalMonitorVO {
    private List<AttendanceAbnormalUserRankVO> topUsers = new ArrayList<>();
    private List<AttendanceStatusCountVO> statusCounts = new ArrayList<>();

    public List<AttendanceAbnormalUserRankVO> getTopUsers() {
        return topUsers;
    }

    public void setTopUsers(List<AttendanceAbnormalUserRankVO> topUsers) {
        this.topUsers = topUsers;
    }

    public List<AttendanceStatusCountVO> getStatusCounts() {
        return statusCounts;
    }

    public void setStatusCounts(List<AttendanceStatusCountVO> statusCounts) {
        this.statusCounts = statusCounts;
    }
}
