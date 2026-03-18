package com.example.lecturesystem.modules.attendance.vo;

import java.util.ArrayList;
import java.util.List;

public class AttendanceSummaryVO {
    private Long totalCount;
    private Long successCount;
    private Long abnormalCount;
    private List<AttendanceStatusCountVO> statusCounts = new ArrayList<>();

    public Long getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Long totalCount) {
        this.totalCount = totalCount;
    }

    public Long getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(Long successCount) {
        this.successCount = successCount;
    }

    public Long getAbnormalCount() {
        return abnormalCount;
    }

    public void setAbnormalCount(Long abnormalCount) {
        this.abnormalCount = abnormalCount;
    }

    public List<AttendanceStatusCountVO> getStatusCounts() {
        return statusCounts;
    }

    public void setStatusCounts(List<AttendanceStatusCountVO> statusCounts) {
        this.statusCounts = statusCounts;
    }
}
