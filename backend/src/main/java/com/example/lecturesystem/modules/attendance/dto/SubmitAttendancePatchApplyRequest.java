package com.example.lecturesystem.modules.attendance.dto;

import jakarta.validation.constraints.NotBlank;

public class SubmitAttendancePatchApplyRequest {
    @NotBlank(message = "考勤日期不能为空")
    private String attendanceDate;

    @NotBlank(message = "补卡类型不能为空")
    private String patchType;

    @NotBlank(message = "补卡时间不能为空")
    private String patchTime;

    @NotBlank(message = "补卡原因不能为空")
    private String reason;

    public String getAttendanceDate() { return attendanceDate; }
    public void setAttendanceDate(String attendanceDate) { this.attendanceDate = attendanceDate; }
    public String getPatchType() { return patchType; }
    public void setPatchType(String patchType) { this.patchType = patchType; }
    public String getPatchTime() { return patchTime; }
    public void setPatchTime(String patchTime) { this.patchTime = patchTime; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
