package com.example.lecturesystem.modules.auth.vo;

import java.time.LocalDateTime;

public class QrLoginSessionCreateVO {
    private String qrToken;
    private LocalDateTime expireAt;
    private String qrUrl;

    public String getQrToken() { return qrToken; }
    public void setQrToken(String qrToken) { this.qrToken = qrToken; }
    public LocalDateTime getExpireAt() { return expireAt; }
    public void setExpireAt(LocalDateTime expireAt) { this.expireAt = expireAt; }
    public String getQrUrl() { return qrUrl; }
    public void setQrUrl(String qrUrl) { this.qrUrl = qrUrl; }
}
