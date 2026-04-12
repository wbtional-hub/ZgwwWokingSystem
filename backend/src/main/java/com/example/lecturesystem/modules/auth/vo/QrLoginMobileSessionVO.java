package com.example.lecturesystem.modules.auth.vo;

import java.time.LocalDateTime;

public class QrLoginMobileSessionVO {
    private String qrToken;
    private String status;
    private LocalDateTime expireAt;

    public String getQrToken() { return qrToken; }
    public void setQrToken(String qrToken) { this.qrToken = qrToken; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getExpireAt() { return expireAt; }
    public void setExpireAt(LocalDateTime expireAt) { this.expireAt = expireAt; }
}
