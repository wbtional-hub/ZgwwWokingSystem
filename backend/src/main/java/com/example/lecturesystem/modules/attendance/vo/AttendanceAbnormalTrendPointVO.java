package com.example.lecturesystem.modules.attendance.vo;

import java.math.BigDecimal;
import java.time.LocalDate;

public class AttendanceAbnormalTrendPointVO {
    private LocalDate attendanceDate;
    private Long totalCount;
    private Long abnormalCount;
    private BigDecimal abnormalRate;

    public LocalDate getAttendanceDate() {
        return attendanceDate;
    }

    public void setAttendanceDate(LocalDate attendanceDate) {
        this.attendanceDate = attendanceDate;
    }

    public Long getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Long totalCount) {
        this.totalCount = totalCount;
    }

    public Long getAbnormalCount() {
        return abnormalCount;
    }

    public void setAbnormalCount(Long abnormalCount) {
        this.abnormalCount = abnormalCount;
    }

    public BigDecimal getAbnormalRate() {
        return abnormalRate;
    }

    public void setAbnormalRate(BigDecimal abnormalRate) {
        this.abnormalRate = abnormalRate;
    }
}
