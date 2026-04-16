package com.example.lecturesystem.modules.auth.exception;

public class WechatMpPendingBindException extends RuntimeException {
    private final String openId;
    private final String unionId;
    private final String bindCode;

    public WechatMpPendingBindException(String message, String openId, String unionId, String bindCode) {
        super(message);
        this.openId = openId;
        this.unionId = unionId;
        this.bindCode = bindCode;
    }

    public String getOpenId() {
        return openId;
    }

    public String getUnionId() {
        return unionId;
    }

    public String getBindCode() {
        return bindCode;
    }
}