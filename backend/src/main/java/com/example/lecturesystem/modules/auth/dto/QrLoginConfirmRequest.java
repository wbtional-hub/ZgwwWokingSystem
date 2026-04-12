package com.example.lecturesystem.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class QrLoginConfirmRequest {
    @NotBlank(message = "qrToken不能为空")
    private String qrToken;

    @NotNull(message = "approve不能为空")
    private Boolean approve;

    public String getQrToken() { return qrToken; }
    public void setQrToken(String qrToken) { this.qrToken = qrToken; }
    public Boolean getApprove() { return approve; }
    public void setApprove(Boolean approve) { this.approve = approve; }
}
