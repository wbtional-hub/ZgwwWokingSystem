package com.example.lecturesystem.modules.auth.service.impl;

import com.example.lecturesystem.modules.auth.config.WechatProperties;
import com.example.lecturesystem.modules.auth.service.WechatJsapiService;
import com.example.lecturesystem.modules.auth.vo.WechatJsapiConfigVO;
import com.example.lecturesystem.modules.param.service.ParamService;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
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
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class WechatJsapiServiceImpl implements WechatJsapiService {
    private static final Logger log = LoggerFactory.getLogger(WechatJsapiServiceImpl.class);

    private static final String WECHAT_ACCESS_TOKEN_URL = "https://api.weixin.qq.com/cgi-bin/token";
    private static final String WECHAT_JSAPI_TICKET_URL = "https://api.weixin.qq.com/cgi-bin/ticket/getticket";

    private static final String PARAM_WECHAT_MP_ENABLED = "WECHAT_MP_ENABLED";
    private static final String PARAM_WECHAT_MP_APP_ID = "WECHAT_MP_APP_ID";
    private static final String PARAM_WECHAT_MP_APP_SECRET = "WECHAT_MP_APP_SECRET";

    private static final String PARAM_WECHAT_JSAPI_ENABLED = "WECHAT_JSAPI_ENABLED";
    private static final String PARAM_WECHAT_JSAPI_SIGNATURE_ENABLED = "WECHAT_JSAPI_SIGNATURE_ENABLED";
    private static final String PARAM_WECHAT_JSAPI_LOCATION_ENABLED = "WECHAT_JSAPI_LOCATION_ENABLED";
    private static final String PARAM_WECHAT_JSAPI_ALLOWED_DOMAINS = "WECHAT_JSAPI_ALLOWED_DOMAINS";
    private static final String PARAM_WECHAT_JSAPI_LOCATION_PRIORITY = "WECHAT_JSAPI_LOCATION_PRIORITY";
    private static final String PARAM_WECHAT_JSAPI_LOCATION_TYPE = "WECHAT_JSAPI_LOCATION_TYPE";
    private static final String PARAM_WECHAT_JSAPI_DEBUG = "WECHAT_JSAPI_DEBUG";
    private static final String PARAM_WECHAT_JSAPI_NON_WECHAT_FALLBACK = "WECHAT_JSAPI_NON_WECHAT_FALLBACK";
    private static final String PARAM_WECHAT_JSAPI_ACCURACY_THRESHOLD = "WECHAT_JSAPI_ACCURACY_THRESHOLD";
    private static final String PARAM_WECHAT_JSAPI_TIMEOUT_MS = "WECHAT_JSAPI_TIMEOUT_MS";

    private static final String PRIORITY_WECHAT_FIRST = "WECHAT_FIRST";
    private static final String PRIORITY_BROWSER_FIRST = "BROWSER_FIRST";
    private static final String PRIORITY_WECHAT_ONLY = "WECHAT_ONLY";

    private static final String FALLBACK_NONE = "NONE";
    private static final String FALLBACK_BROWSER = "BROWSER";

    private static final String LOCATION_TYPE_GCJ02 = "gcj02";
    private static final String LOCATION_TYPE_WGS84 = "wgs84";

    private static final int DEFAULT_ACCURACY_THRESHOLD = 80;
    private static final int DEFAULT_TIMEOUT_MS = 8000;
    private static final long CACHE_REFRESH_BUFFER_SECONDS = 120L;

    private final WechatProperties wechatProperties;
    private final ParamService paramService;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private volatile CachedValue accessTokenCache;
    private volatile CachedValue jsapiTicketCache;

    public WechatJsapiServiceImpl(WechatProperties wechatProperties, ParamService paramService) {
        this.wechatProperties = wechatProperties;
        this.paramService = paramService;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public WechatJsapiConfigVO queryJsapiConfig(String url) {
        String normalizedUrl = normalizeSigningUrl(url);
        JsapiRuntimeConfig jsapiConfig = resolveJsapiRuntimeConfig();
        MpRuntimeConfig mpConfig = resolveMpRuntimeConfig();

        WechatJsapiConfigVO response = new WechatJsapiConfigVO();
        response.setEnabled(false);
        response.setLocationEnabled(false);
        response.setAppId(mpConfig.appId);
        response.setJsApiList(Collections.emptyList());
        response.setLocationType(jsapiConfig.locationType);
        response.setDebug(jsapiConfig.debug);
        response.setPriority(jsapiConfig.priority);
        response.setFallback(jsapiConfig.fallback);
        response.setAccuracyThreshold(jsapiConfig.accuracyThreshold);
        response.setTimeoutMs(jsapiConfig.timeoutMs);

        if (!jsapiConfig.enabled) {
            log.info("[WeChatJSAPI] jsapi disabled by sys_param: {}", PARAM_WECHAT_JSAPI_ENABLED);
            return response;
        }
        if (!jsapiConfig.signatureEnabled) {
            log.info("[WeChatJSAPI] jsapi signature disabled by sys_param: {}", PARAM_WECHAT_JSAPI_SIGNATURE_ENABLED);
            return response;
        }

        validateMpRuntimeConfig(mpConfig);
        validateAllowedDomain(normalizedUrl, jsapiConfig.allowedDomains);

        String ticket = getJsapiTicket(mpConfig);
        long timestamp = Instant.now().getEpochSecond();
        String nonceStr = UUID.randomUUID().toString().replace("-", "");
        String signature = buildSignature(ticket, nonceStr, timestamp, normalizedUrl);

        response.setEnabled(true);
        response.setLocationEnabled(jsapiConfig.locationEnabled);
        response.setTimestamp(timestamp);
        response.setNonceStr(nonceStr);
        response.setSignature(signature);
        response.setJsApiList(jsapiConfig.locationEnabled ? List.of("checkJsApi", "getLocation") : Collections.emptyList());
        return response;
    }

    protected WechatAccessTokenResponse requestAccessToken(String appId, String appSecret) {
        try {
            String requestUrl = WECHAT_ACCESS_TOKEN_URL
                    + "?grant_type=client_credential"
                    + "&appid=" + encode(appId)
                    + "&secret=" + encode(appSecret);
            HttpRequest request = HttpRequest.newBuilder(URI.create(requestUrl))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            WechatAccessTokenResponse body = objectMapper.readValue(response.body(), WechatAccessTokenResponse.class);
            if (body.getErrcode() != null && body.getErrcode() != 0) {
                log.warn("[WeChatJSAPI] access_token request failed: errcode={}, errmsg={}", body.getErrcode(), body.getErrmsg());
                throw new IllegalArgumentException("微信 access_token 获取失败: " + defaultIfBlank(body.getErrmsg(), "errcode=" + body.getErrcode()));
            }
            if (isBlank(body.getAccessToken())) {
                throw new IllegalArgumentException("微信 access_token 获取失败: 返回结果缺少 access_token");
            }
            return body;
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("[WeChatJSAPI] access_token request error", ex);
            throw new IllegalStateException("微信 access_token 获取失败: " + ex.getMessage(), ex);
        }
    }

    protected WechatJsapiTicketResponse requestJsapiTicket(String accessToken) {
        try {
            String requestUrl = WECHAT_JSAPI_TICKET_URL
                    + "?access_token=" + encode(accessToken)
                    + "&type=jsapi";
            HttpRequest request = HttpRequest.newBuilder(URI.create(requestUrl))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            WechatJsapiTicketResponse body = objectMapper.readValue(response.body(), WechatJsapiTicketResponse.class);
            if (body.getErrcode() != null && body.getErrcode() != 0) {
                log.warn("[WeChatJSAPI] jsapi_ticket request failed: errcode={}, errmsg={}", body.getErrcode(), body.getErrmsg());
                throw new IllegalArgumentException("微信 jsapi_ticket 获取失败: " + defaultIfBlank(body.getErrmsg(), "errcode=" + body.getErrcode()));
            }
            if (isBlank(body.getTicket())) {
                throw new IllegalArgumentException("微信 jsapi_ticket 获取失败: 返回结果缺少 ticket");
            }
            return body;
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("[WeChatJSAPI] jsapi_ticket request error", ex);
            throw new IllegalStateException("微信 jsapi_ticket 获取失败: " + ex.getMessage(), ex);
        }
    }

    private synchronized String getAccessToken(MpRuntimeConfig config) {
        String scopeKey = defaultIfBlank(config.appId, "") + "|" + defaultIfBlank(config.appSecret, "");
        if (isCacheValid(accessTokenCache, scopeKey)) {
            return accessTokenCache.value;
        }
        WechatAccessTokenResponse response = requestAccessToken(config.appId, config.appSecret);
        accessTokenCache = buildCacheValue(scopeKey, response.getAccessToken(), response.getExpiresIn());
        return accessTokenCache.value;
    }

    private synchronized String getJsapiTicket(MpRuntimeConfig config) {
        String scopeKey = defaultIfBlank(config.appId, "");
        if (isCacheValid(jsapiTicketCache, scopeKey)) {
            return jsapiTicketCache.value;
        }
        String accessToken = getAccessToken(config);
        WechatJsapiTicketResponse response = requestJsapiTicket(accessToken);
        jsapiTicketCache = buildCacheValue(scopeKey, response.getTicket(), response.getExpiresIn());
        return jsapiTicketCache.value;
    }

    private CachedValue buildCacheValue(String scopeKey, String value, Integer expiresIn) {
        long seconds = expiresIn == null ? 7200L : Math.max(expiresIn.longValue(), 300L);
        long safeSeconds = Math.max(seconds - CACHE_REFRESH_BUFFER_SECONDS, 60L);
        return new CachedValue(scopeKey, value, Instant.now().plusSeconds(safeSeconds));
    }

    private boolean isCacheValid(CachedValue cache, String scopeKey) {
        return cache != null
                && scopeKey.equals(cache.scopeKey)
                && cache.expiresAt != null
                && cache.expiresAt.isAfter(Instant.now());
    }

    private void validateMpRuntimeConfig(MpRuntimeConfig mpConfig) {
        if (!Boolean.TRUE.equals(mpConfig.enabled)) {
            throw new IllegalArgumentException("微信公众号未启用: 请检查 WECHAT_MP_ENABLED");
        }
        if (isBlank(mpConfig.appId) || isBlank(mpConfig.appSecret)) {
            throw new IllegalArgumentException("微信公众号配置缺失: 请检查 WECHAT_MP_APP_ID 和 WECHAT_MP_APP_SECRET");
        }
    }

    private void validateAllowedDomain(String url, List<String> allowedDomains) {
        if (allowedDomains == null || allowedDomains.isEmpty()) {
            throw new IllegalArgumentException("微信 JSAPI 域名白名单未配置: 请设置 WECHAT_JSAPI_ALLOWED_DOMAINS");
        }
        if (!isAllowedDomain(url, allowedDomains)) {
            String host = defaultIfBlank(URI.create(url).getHost(), url);
            throw new IllegalArgumentException("当前页面域名不在微信 JSAPI 白名单内: " + host);
        }
    }

    private MpRuntimeConfig resolveMpRuntimeConfig() {
        WechatProperties.Mp mp = wechatProperties == null ? null : wechatProperties.getMp();
        Boolean enabled = parseBoolean(getSysParamValue(PARAM_WECHAT_MP_ENABLED));
        if (enabled == null) {
            enabled = mp != null && mp.isEnabled();
        }
        String appId = getSysParamValue(PARAM_WECHAT_MP_APP_ID);
        if (appId == null) {
            appId = trimToNull(mp == null ? null : mp.getAppId());
        }
        String appSecret = getSysParamValue(PARAM_WECHAT_MP_APP_SECRET);
        if (appSecret == null) {
            appSecret = trimToNull(mp == null ? null : mp.getAppSecret());
        }
        return new MpRuntimeConfig(enabled, appId, appSecret);
    }

    private JsapiRuntimeConfig resolveJsapiRuntimeConfig() {
        return new JsapiRuntimeConfig(
                parseBooleanOrDefault(getSysParamValue(PARAM_WECHAT_JSAPI_ENABLED), false),
                parseBooleanOrDefault(getSysParamValue(PARAM_WECHAT_JSAPI_SIGNATURE_ENABLED), true),
                parseBooleanOrDefault(getSysParamValue(PARAM_WECHAT_JSAPI_LOCATION_ENABLED), true),
                parseAllowedDomains(getSysParamValue(PARAM_WECHAT_JSAPI_ALLOWED_DOMAINS)),
                normalizePriority(getSysParamValue(PARAM_WECHAT_JSAPI_LOCATION_PRIORITY)),
                normalizeLocationType(getSysParamValue(PARAM_WECHAT_JSAPI_LOCATION_TYPE)),
                parseBooleanOrDefault(getSysParamValue(PARAM_WECHAT_JSAPI_DEBUG), false),
                normalizeFallback(getSysParamValue(PARAM_WECHAT_JSAPI_NON_WECHAT_FALLBACK)),
                parseIntegerOrDefault(getSysParamValue(PARAM_WECHAT_JSAPI_ACCURACY_THRESHOLD), DEFAULT_ACCURACY_THRESHOLD, 0),
                parseIntegerOrDefault(getSysParamValue(PARAM_WECHAT_JSAPI_TIMEOUT_MS), DEFAULT_TIMEOUT_MS, 1000)
        );
    }

    private boolean isAllowedDomain(String url, List<String> allowedDomains) {
        if (allowedDomains == null || allowedDomains.isEmpty()) {
            return false;
        }
        URI uri = URI.create(url);
        String host = defaultIfBlank(uri.getHost(), "").toLowerCase(Locale.ROOT);
        int port = uri.getPort();
        for (String rule : allowedDomains) {
            if (matchesDomain(host, port, rule)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesDomain(String host, int port, String rawRule) {
        String normalized = trimToNull(rawRule);
        if (normalized == null) {
            return false;
        }
        String hostRule = normalized.toLowerCase(Locale.ROOT);
        Integer portRule = null;
        if (hostRule.contains("://")) {
            try {
                URI uri = URI.create(hostRule);
                hostRule = trimToNull(uri.getHost());
                portRule = uri.getPort() < 0 ? null : uri.getPort();
            } catch (Exception ex) {
                log.warn("[WeChatJSAPI] invalid domain rule: {}", rawRule, ex);
                return false;
            }
        } else if (!hostRule.startsWith("*.") && hostRule.indexOf(':') > 0 && hostRule.indexOf(':') == hostRule.lastIndexOf(':')) {
            int index = hostRule.lastIndexOf(':');
            String maybePort = hostRule.substring(index + 1);
            if (maybePort.chars().allMatch(Character::isDigit)) {
                portRule = Integer.parseInt(maybePort);
                hostRule = hostRule.substring(0, index);
            }
        }
        if (hostRule == null) {
            return false;
        }
        if (hostRule.startsWith("*.")) {
            hostRule = hostRule.substring(2);
        }
        if (portRule != null && portRule != port) {
            return false;
        }
        return host.equals(hostRule) || host.endsWith("." + hostRule);
    }

    private String normalizeSigningUrl(String url) {
        String normalized = trimToNull(url);
        if (normalized == null) {
            throw new IllegalArgumentException("微信 JS-SDK 配置读取失败: url 不能为空");
        }
        try {
            URI uri = URI.create(normalized);
            String scheme = trimToNull(uri.getScheme());
            String host = trimToNull(uri.getHost());
            if (scheme == null || host == null) {
                throw new IllegalArgumentException("微信 JS-SDK 配置读取失败: url 必须是完整的 http/https 地址");
            }
            if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
                throw new IllegalArgumentException("微信 JS-SDK 配置读取失败: 仅支持 http/https 地址");
            }
            StringBuilder builder = new StringBuilder();
            builder.append(scheme.toLowerCase(Locale.ROOT)).append("://").append(host.toLowerCase(Locale.ROOT));
            if (uri.getPort() >= 0) {
                builder.append(":").append(uri.getPort());
            }
            builder.append(isBlank(uri.getRawPath()) ? "/" : uri.getRawPath());
            if (!isBlank(uri.getRawQuery())) {
                builder.append("?").append(uri.getRawQuery());
            }
            return builder.toString();
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("微信 JS-SDK 配置读取失败: url 格式不正确");
        }
    }

    private String buildSignature(String ticket, String nonceStr, long timestamp, String url) {
        String payload = "jsapi_ticket=" + ticket
                + "&noncestr=" + nonceStr
                + "&timestamp=" + timestamp
                + "&url=" + url;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] bytes = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (byte value : bytes) {
                result.append(String.format("%02x", value));
            }
            return result.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("微信 JS-SDK 签名生成失败: " + ex.getMessage(), ex);
        }
    }

    private List<String> parseAllowedDomains(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return Collections.emptyList();
        }
        String[] segments = normalized.split("[,;\\r\\n]+");
        List<String> result = new ArrayList<>();
        for (String segment : segments) {
            String item = trimToNull(segment);
            if (item != null) {
                result.add(item);
            }
        }
        return result;
    }

    private String getSysParamValue(String code) {
        try {
            return trimToNull(paramService.getByCode(code));
        } catch (Exception ex) {
            log.warn("[WeChatJSAPI] failed to read sys_param: code={}", code, ex);
            return null;
        }
    }

    private Integer parseIntegerOrDefault(String value, int defaultValue, int minValue) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return defaultValue;
        }
        try {
            return Math.max(Integer.parseInt(normalized), minValue);
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private boolean parseBooleanOrDefault(String value, boolean defaultValue) {
        Boolean parsed = parseBoolean(value);
        return parsed == null ? defaultValue : parsed;
    }

    private Boolean parseBoolean(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return null;
        }
        if ("true".equalsIgnoreCase(normalized)
                || "1".equals(normalized)
                || "yes".equalsIgnoreCase(normalized)
                || "y".equalsIgnoreCase(normalized)
                || "on".equalsIgnoreCase(normalized)) {
            return Boolean.TRUE;
        }
        if ("false".equalsIgnoreCase(normalized)
                || "0".equals(normalized)
                || "no".equalsIgnoreCase(normalized)
                || "n".equalsIgnoreCase(normalized)
                || "off".equalsIgnoreCase(normalized)) {
            return Boolean.FALSE;
        }
        return null;
    }

    private String normalizePriority(String value) {
        String normalized = defaultIfBlank(value, PRIORITY_WECHAT_FIRST).toUpperCase(Locale.ROOT);
        if (PRIORITY_BROWSER_FIRST.equals(normalized) || PRIORITY_WECHAT_ONLY.equals(normalized)) {
            return normalized;
        }
        return PRIORITY_WECHAT_FIRST;
    }

    private String normalizeFallback(String value) {
        String normalized = defaultIfBlank(value, FALLBACK_BROWSER).toUpperCase(Locale.ROOT);
        if (FALLBACK_NONE.equals(normalized)) {
            return FALLBACK_NONE;
        }
        return FALLBACK_BROWSER;
    }

    private String normalizeLocationType(String value) {
        String normalized = defaultIfBlank(value, LOCATION_TYPE_GCJ02).toLowerCase(Locale.ROOT);
        if (LOCATION_TYPE_WGS84.equals(normalized)) {
            return LOCATION_TYPE_WGS84;
        }
        return LOCATION_TYPE_GCJ02;
    }

    private String defaultIfBlank(String value, String fallback) {
        String normalized = trimToNull(value);
        return normalized == null ? fallback : normalized;
    }

    private boolean isBlank(String value) {
        return trimToNull(value) == null;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private record MpRuntimeConfig(Boolean enabled, String appId, String appSecret) {
    }

    private record JsapiRuntimeConfig(boolean enabled,
                                      boolean signatureEnabled,
                                      boolean locationEnabled,
                                      List<String> allowedDomains,
                                      String priority,
                                      String locationType,
                                      boolean debug,
                                      String fallback,
                                      Integer accuracyThreshold,
                                      Integer timeoutMs) {
    }

    private record CachedValue(String scopeKey, String value, Instant expiresAt) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    protected static class WechatAccessTokenResponse {
        @JsonProperty("access_token")
        private String accessToken;
        @JsonProperty("expires_in")
        private Integer expiresIn;
        private Integer errcode;
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
    protected static class WechatJsapiTicketResponse {
        private String ticket;
        @JsonProperty("expires_in")
        private Integer expiresIn;
        private Integer errcode;
        private String errmsg;

        public String getTicket() {
            return ticket;
        }

        public void setTicket(String ticket) {
            this.ticket = ticket;
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
}
