package com.example.lecturesystem.modules.auth.service.impl;

import com.example.lecturesystem.modules.auth.config.WechatProperties;
import com.example.lecturesystem.modules.auth.service.WechatTemplateMessageService;
import com.example.lecturesystem.modules.param.service.ParamService;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
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
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class WechatTemplateMessageServiceImpl implements WechatTemplateMessageService {
    private static final Logger log = LoggerFactory.getLogger(WechatTemplateMessageServiceImpl.class);

    private static final String ACCESS_TOKEN_URL = "https://api.weixin.qq.com/cgi-bin/token";
    private static final String TEMPLATE_SEND_URL = "https://api.weixin.qq.com/cgi-bin/message/template/send";

    private static final String PARAM_WECHAT_MP_ENABLED = "WECHAT_MP_ENABLED";
    private static final String PARAM_WECHAT_MP_APP_ID = "WECHAT_MP_APP_ID";
    private static final String PARAM_WECHAT_MP_APP_SECRET = "WECHAT_MP_APP_SECRET";

    private final WechatProperties wechatProperties;
    private final ParamService paramService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private volatile String cachedAccessToken;
    private volatile Instant accessTokenExpireAt = Instant.EPOCH;

    public WechatTemplateMessageServiceImpl(WechatProperties wechatProperties, ParamService paramService) {
        this.wechatProperties = wechatProperties;
        this.paramService = paramService;
    }

    @Override
    public SendResult sendTemplateMessage(SendRequest request) {
        String openId = trimToNull(request == null ? null : request.getOpenId());
        String templateId = trimToNull(request == null ? null : request.getTemplateId());
        if (openId == null) {
            throw new IllegalArgumentException("openid 不能为空");
        }
        if (templateId == null) {
            throw new IllegalArgumentException("模板消息 templateId 不能为空");
        }

        WechatMpConfig config = resolveConfig();
        if (!config.enabled()) {
            throw new IllegalStateException("微信公众号模板消息未启用");
        }
        if (config.appId() == null || config.appSecret() == null) {
            throw new IllegalStateException("微信公众号 appId/appSecret 未配置完整");
        }

        try {
            String accessToken = resolveAccessToken(config);
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("touser", openId);
            payload.put("template_id", templateId);
            if (trimToNull(request.getUrl()) != null) {
                payload.put("url", request.getUrl().trim());
            }
            payload.put("data", request.getData() == null ? Map.of() : request.getData());

            String requestBody = objectMapper.writeValueAsString(payload);
            String requestUrl = TEMPLATE_SEND_URL + "?access_token=" + encode(accessToken);
            HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(requestUrl))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            TemplateSendResponse sendResponse = objectMapper.readValue(response.body(), TemplateSendResponse.class);

            SendResult result = new SendResult();
            result.setRequestPayload(requestBody);
            result.setResponseBody(response.body());
            String errCode = sendResponse.getErrcode() == null ? "0" : String.valueOf(sendResponse.getErrcode());
            result.setErrCode(errCode);
            result.setErrMsg(trimToNull(sendResponse.getErrmsg()));
            result.setSuccess(sendResponse.getErrcode() == null || sendResponse.getErrcode() == 0);
            return result;
        } catch (Exception ex) {
            log.error("wechat template message send failed: openId={}, templateId={}", openId, templateId, ex);
            SendResult result = new SendResult();
            result.setSuccess(false);
            result.setErrCode("SEND_EXCEPTION");
            result.setErrMsg(ex.getMessage());
            return result;
        }
    }

    private synchronized String resolveAccessToken(WechatMpConfig config) throws Exception {
        Instant now = Instant.now();
        if (cachedAccessToken != null && now.isBefore(accessTokenExpireAt.minusSeconds(60))) {
            return cachedAccessToken;
        }

        String requestUrl = ACCESS_TOKEN_URL
                + "?grant_type=client_credential"
                + "&appid=" + encode(config.appId())
                + "&secret=" + encode(config.appSecret());
        String maskedRequestUrl = maskSecretInUrl(requestUrl);
        log.info("wechat template access_token request: url={}", maskedRequestUrl);
        HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(requestUrl))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        String responseBody = response.body();
        String maskedResponseBody = maskAccessTokenInBody(responseBody);
        log.info("wechat template access_token response: httpStatus={}, body={}", response.statusCode(), maskedResponseBody);
        if (response.statusCode() != 200) {
            throw new IllegalStateException("微信 access_token 获取失败: HTTP 状态码=" + response.statusCode()
                    + ", requestUrl=" + maskedRequestUrl + ", body=" + maskedResponseBody);
        }
        if (trimToNull(responseBody) == null) {
            throw new IllegalStateException("微信 access_token 获取失败: 微信返回 body 为空, requestUrl=" + maskedRequestUrl);
        }

        AccessTokenResponse tokenResponse;
        try {
            tokenResponse = objectMapper.readValue(responseBody, AccessTokenResponse.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("微信 access_token 获取失败: JSON 解析失败, requestUrl="
                    + maskedRequestUrl + ", body=" + maskedResponseBody + ", error=" + ex.getOriginalMessage(), ex);
        }
        log.info("wechat template access_token parsed: errcode={}, errmsg={}, expiresIn={}, accessToken={}",
                tokenResponse.getErrcode(), tokenResponse.getErrmsg(), tokenResponse.getExpiresIn(),
                maskSensitive(tokenResponse.getAccessToken()));
        if (tokenResponse.getErrcode() != null && tokenResponse.getErrcode() != 0) {
            throw new IllegalStateException("微信 access_token 获取失败: " + tokenResponse.getErrcode() + " " + tokenResponse.getErrmsg());
        }
        if (trimToNull(tokenResponse.getAccessToken()) == null) {
            throw new IllegalStateException("微信 access_token 获取失败: access_token 字段缺失或为空, errcode="
                    + tokenResponse.getErrcode() + ", errmsg=" + tokenResponse.getErrmsg()
                    + ", body=" + maskedResponseBody);
        }
        if (trimToNull(tokenResponse.getAccessToken()) == null) {
            throw new IllegalStateException("微信 access_token 返回为空");
        }
        cachedAccessToken = tokenResponse.getAccessToken().trim();
        accessTokenExpireAt = now.plusSeconds(Math.max(300, tokenResponse.getExpiresIn() == null ? 7200 : tokenResponse.getExpiresIn()));
        return cachedAccessToken;
    }

    private WechatMpConfig resolveConfig() {
        boolean enabled = parseBoolean(resolveParamOrProperty(PARAM_WECHAT_MP_ENABLED, String.valueOf(wechatProperties.getMp().isEnabled())));
        String appId = resolveParamOrProperty(PARAM_WECHAT_MP_APP_ID, wechatProperties.getMp().getAppId());
        String appSecret = resolveParamOrProperty(PARAM_WECHAT_MP_APP_SECRET, wechatProperties.getMp().getAppSecret());
        return new WechatMpConfig(enabled, trimToNull(appId), trimToNull(appSecret));
    }

    private String resolveParamOrProperty(String code, String fallback) {
        try {
            String value = trimToNull(paramService.getByCode(code));
            return value != null ? value : fallback;
        } catch (Exception ex) {
            log.warn("resolve wechat template config from sys_param failed: code={}", code, ex);
            return fallback;
        }
    }

    private boolean parseBoolean(String value) {
        return "1".equals(value) || "true".equalsIgnoreCase(trimToNull(value));
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String maskSecretInUrl(String requestUrl) {
        return maskQueryValue(requestUrl, "secret");
    }

    private String maskQueryValue(String url, String key) {
        if (url == null || key == null) {
            return url;
        }
        String marker = key + "=";
        int markerIndex = url.indexOf(marker);
        if (markerIndex < 0) {
            return url;
        }
        int valueStart = markerIndex + marker.length();
        int valueEnd = url.indexOf('&', valueStart);
        if (valueEnd < 0) {
            valueEnd = url.length();
        }
        return url.substring(0, valueStart) + "***" + url.substring(valueEnd);
    }

    private String maskAccessTokenInBody(String body) {
        if (body == null) {
            return null;
        }
        String key = "\"access_token\"";
        int keyIndex = body.indexOf(key);
        if (keyIndex < 0) {
            return body;
        }
        int colonIndex = body.indexOf(':', keyIndex + key.length());
        if (colonIndex < 0) {
            return body;
        }
        int valueStart = body.indexOf('"', colonIndex + 1);
        if (valueStart < 0) {
            return body;
        }
        int valueEnd = body.indexOf('"', valueStart + 1);
        if (valueEnd < 0) {
            return body;
        }
        String accessToken = body.substring(valueStart + 1, valueEnd);
        return body.substring(0, valueStart + 1) + maskSensitive(accessToken) + body.substring(valueEnd);
    }

    private String maskSensitive(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return null;
        }
        if (normalized.length() <= 8) {
            return "***";
        }
        return normalized.substring(0, 4) + "***" + normalized.substring(normalized.length() - 4);
    }

    private record WechatMpConfig(boolean enabled, String appId, String appSecret) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class AccessTokenResponse {
        @JsonProperty("access_token")
        private String accessToken;
        @JsonProperty("expires_in")
        private Integer expiresIn;
        @JsonProperty("errcode")
        private Integer errcode;
        @JsonProperty("errmsg")
        private String errmsg;

        public String getAccessToken() {
            return accessToken;
        }

        public void setAccessToken(String accessToken) {
            this.accessToken = accessToken;
        }

        public Integer getExpiresIn() {
            return expiresIn;
        }

        public void setExpiresIn(Integer expiresIn) {
            this.expiresIn = expiresIn;
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

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class TemplateSendResponse {
        private Integer errcode;
        private String errmsg;

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
