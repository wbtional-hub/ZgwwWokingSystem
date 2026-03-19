package com.example.lecturesystem.modules.attendance.vo;

import java.util.ArrayList;
import java.util.List;

public class AttendanceAbnormalMonitorVO {
    private List<AttendanceAbnormalUserRankVO> topUsers = new ArrayList<>();
    private List<AttendanceStatusCountVO> statusCounts = new ArrayList<>();
    private List<AttendanceAbnormalReasonDistributionVO> reasonDistributions = new ArrayList<>();
    private Long highRiskCount;
    private Long alertCount;

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

    public List<AttendanceAbnormalReasonDistributionVO> getReasonDistributions() {
        return reasonDistributions;
    }

    public void setReasonDistributions(List<AttendanceAbnormalReasonDistributionVO> reasonDistributions) {
        this.reasonDistributions = reasonDistributions;
    }

    public Long getHighRiskCount() {
        return highRiskCount;
    }

    public void setHighRiskCount(Long highRiskCount) {
        this.highRiskCount = highRiskCount;
    }

    public Long getAlertCount() {
        return alertCount;
    }

    public void setAlertCount(Long alertCount) {
        this.alertCount = alertCount;
    }
}
