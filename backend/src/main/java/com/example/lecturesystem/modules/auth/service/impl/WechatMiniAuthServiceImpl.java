package com.example.lecturesystem.modules.auth.service.impl;

import com.example.lecturesystem.modules.auth.config.WechatProperties;
import com.example.lecturesystem.modules.auth.service.AuthService;
import com.example.lecturesystem.modules.auth.service.WechatMiniAuthService;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Service
public class WechatMiniAuthServiceImpl implements WechatMiniAuthService {
    private static final Logger log = LoggerFactory.getLogger(WechatMiniAuthServiceImpl.class);
    private static final String WECHAT_MINI_SESSION_URL = "https://api.weixin.qq.com/sns/jscode2session";

    private final WechatProperties wechatProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public WechatMiniAuthServiceImpl(WechatProperties wechatProperties) {
        this.wechatProperties = wechatProperties;
    }

    @Override
    public AuthService.WechatMiniIdentity exchangeCode(String code) {
        WechatProperties.Mini mini = requireEnabledMini();
        String normalizedCode = requireText(code, "微信授权 code 不能为空");
        String appId = requireText(mini.getAppId(), "小程序登录配置缺少 app-id");
        String appSecret = requireText(mini.getAppSecret(), "小程序登录配置缺少 app-secret");

        try {
            String url = WECHAT_MINI_SESSION_URL
                    + "?appid=" + encode(appId)
                    + "&secret=" + encode(appSecret)
                    + "&js_code=" + encode(normalizedCode)
                    + "&grant_type=authorization_code";
            HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            WechatMiniSessionResponse session = objectMapper.readValue(response.body(), WechatMiniSessionResponse.class);
            if (session.getErrcode() != null && session.getErrcode() != 0) {
                throw new IllegalArgumentException("微信小程序授权失败：" + defaultIfBlank(session.getErrmsg(), "errcode=" + session.getErrcode()));
            }
            String openId = trimToNull(session.getOpenid());
            String unionId = trimToNull(session.getUnionid());
            if (openId == null && unionId == null) {
                throw new IllegalArgumentException("微信小程序授权结果未返回可识别身份");
            }
            return new AuthService.WechatMiniIdentity(openId, unionId);
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("微信小程序 jscode2session 调用失败", ex);
            throw new IllegalStateException("微信小程序登录调用失败，请检查网络或小程序配置", ex);
        }
    }

    private WechatProperties.Mini requireEnabledMini() {
        WechatProperties.Mini mini = wechatProperties.getMini();
        if (mini == null || !mini.isEnabled()) {
            throw new IllegalArgumentException("未启用小程序登录");
        }
        return mini;
    }

    private String requireText(String value, String message) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new IllegalArgumentException(message);
        }
        return normalized;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String defaultIfBlank(String value, String fallback) {
        String normalized = trimToNull(value);
        return normalized == null ? fallback : normalized;
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class WechatMiniSessionResponse {
        private String openid;
        private String unionid;
        private Integer errcode;
        private String errmsg;

        public String getOpenid() {
            return openid;
        }

        public void setOpenid(String openid) {
            this.openid = openid;
        }

        public String getUnionid() {
            return unionid;
        }

        public void setUnionid(String unionid) {
            this.unionid = unionid;
        }

        public Integer getErrcode() {
            return errcode;
        }

        public void setErrcode(Integer errcode) {
            this.errcode = errcode;
        }

        public String getErrmsg() {
            return errmsg;
        }

        public void setErrmsg(String errmsg) {
            this.errmsg = errmsg;
        }
    }
}
