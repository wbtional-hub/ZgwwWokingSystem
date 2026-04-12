package com.example.lecturesystem.modules.auth.vo;

public class WechatMpAuthorizeUrlVO {
    private boolean enabled;
    private String authorizeScope;
    private String authorizeUrl;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getAuthorizeScope() {
        return authorizeScope;
    }

    public void setAuthorizeScope(String authorizeScope) {
        this.authorizeScope = authorizeScope;
    }

    public String getAuthorizeUrl() {
        return authorizeUrl;
    }

    public void setAuthorizeUrl(String authorizeUrl) {
        this.authorizeUrl = authorizeUrl;
    }
}
