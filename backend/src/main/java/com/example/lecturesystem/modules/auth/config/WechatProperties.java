package com.example.lecturesystem.modules.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "wechat")
public class WechatProperties {
    private Mp mp = new Mp();
    private Mini mini = new Mini();

    public Mp getMp() {
        return mp;
    }

    public void setMp(Mp mp) {
        this.mp = mp;
    }

    public Mini getMini() {
        return mini;
    }

    public void setMini(Mini mini) {
        this.mini = mini;
    }

    public static class Mp {
        private boolean enabled = false;
        private String appId;
        private String appSecret;
        private String token;
        private String aesKey;
        private String oauthRedirectUri = "https://www.xmzgww.com/api/auth/wechat-mp-callback";
        private String authorizeScope = "snsapi_base";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getAppId() {
            return appId;
        }

        public void setAppId(String appId) {
            this.appId = appId;
        }

        public String getAppSecret() {
            return appSecret;
        }

        public void setAppSecret(String appSecret) {
            this.appSecret = appSecret;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public String getAesKey() {
            return aesKey;
        }

        public void setAesKey(String aesKey) {
            this.aesKey = aesKey;
        }

        public String getOauthRedirectUri() {
            return oauthRedirectUri;
        }

        public void setOauthRedirectUri(String oauthRedirectUri) {
            this.oauthRedirectUri = oauthRedirectUri;
        }

        public String getAuthorizeScope() {
            return authorizeScope;
        }

        public void setAuthorizeScope(String authorizeScope) {
            this.authorizeScope = authorizeScope;
        }
    }

    public static class Mini {
        private boolean enabled = false;
        private String appId;
        private String appSecret;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getAppId() {
            return appId;
        }

        public void setAppId(String appId) {
            this.appId = appId;
        }

        public String getAppSecret() {
            return appSecret;
        }

        public void setAppSecret(String appSecret) {
            this.appSecret = appSecret;
        }
    }
}
