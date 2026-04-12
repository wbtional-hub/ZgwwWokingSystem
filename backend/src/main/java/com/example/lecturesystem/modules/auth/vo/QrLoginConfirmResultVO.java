package com.example.lecturesystem.modules.auth.vo;

public class QrLoginConfirmResultVO {
    private boolean success;
    private String status;

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
