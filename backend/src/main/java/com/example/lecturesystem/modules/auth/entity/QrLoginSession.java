package com.example.lecturesystem.modules.auth.entity;

import java.time.LocalDateTime;

public class QrLoginSession {
    private Long id;
    private String qrToken;
    private Long userId;
    private String status;
    private LocalDateTime expireTime;
    private LocalDateTime scannedTime;
    private LocalDateTime confirmedTime;
    private LocalDateTime consumedTime;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getQrToken() { return qrToken; }
    public void setQrToken(String qrToken) { this.qrToken = qrToken; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getExpireTime() { return expireTime; }
    public void setExpireTime(LocalDateTime expireTime) { this.expireTime = expireTime; }
    public LocalDateTime getScannedTime() { return scannedTime; }
    public void setScannedTime(LocalDateTime scannedTime) { this.scannedTime = scannedTime; }
    public LocalDateTime getConfirmedTime() { return confirmedTime; }
    public void setConfirmedTime(LocalDateTime confirmedTime) { this.confirmedTime = confirmedTime; }
    public LocalDateTime getConsumedTime() { return consumedTime; }
    public void setConsumedTime(LocalDateTime consumedTime) { this.consumedTime = consumedTime; }
    public LocalDateTime getCreatedTime() { return createdTime; }
    public void setCreatedTime(LocalDateTime createdTime) { this.createdTime = createdTime; }
    public LocalDateTime getUpdatedTime() { return updatedTime; }
    public void setUpdatedTime(LocalDateTime updatedTime) { this.updatedTime = updatedTime; }
}
