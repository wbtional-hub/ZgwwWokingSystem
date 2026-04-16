package com.example.lecturesystem.modules.attendance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class SaveAttendanceRequest {
    private Long id;
    private Long userId;

    @NotBlank(message = "考勤日期不能为空")
    private String attendanceDate;

    /**
     * 上午上班
     */
    private String checkInTime;

    /**
     * 上午下班
     */
    private String amOffTime;

    /**
     * 下午上班
     */
    private String pmOnTime;

    /**
     * 下午下班
     */
    private String checkOutTime;

    private String checkInAddress;
    private String amOffAddress;
    private String pmOnAddress;
    private String checkOutAddress;

    @NotNull(message = "有效状态不能为空")
    private Integer validFlag;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getAttendanceDate() {
        return attendanceDate;
    }

    public void setAttendanceDate(String attendanceDate) {
        this.attendanceDate = attendanceDate;
    }

    public String getCheckInTime() {
        return checkInTime;
    }

    public void setCheckInTime(String checkInTime) {
        this.checkInTime = checkInTime;
    }

    public String getAmOffTime() {
        return amOffTime;
    }

    public void setAmOffTime(String amOffTime) {
        this.amOffTime = amOffTime;
    }

    public String getPmOnTime() {
        return pmOnTime;
    }

    public void setPmOnTime(String pmOnTime) {
        this.pmOnTime = pmOnTime;
    }

    public String getCheckOutTime() {
        return checkOutTime;
    }

    public void setCheckOutTime(String checkOutTime) {
        this.checkOutTime = checkOutTime;
    }

    public String getCheckInAddress() {
        return checkInAddress;
    }

    public void setCheckInAddress(String checkInAddress) {
        this.checkInAddress = checkInAddress;
    }

    public String getAmOffAddress() {
        return amOffAddress;
    }

    public void setAmOffAddress(String amOffAddress) {
        this.amOffAddress = amOffAddress;
    }

    public String getPmOnAddress() {
        return pmOnAddress;
    }

    public void setPmOnAddress(String pmOnAddress) {
        this.pmOnAddress = pmOnAddress;
    }

    public String getCheckOutAddress() {
        return checkOutAddress;
    }

    public void setCheckOutAddress(String checkOutAddress) {
        this.checkOutAddress = checkOutAddress;
    }

    public Integer getValidFlag() {
        return validFlag;
    }

    public void setValidFlag(Integer validFlag) {
        this.validFlag = validFlag;
    }
}