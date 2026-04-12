package com.example.lecturesystem.modules.auth.service.impl;

import com.example.lecturesystem.modules.auth.config.WechatProperties;
import com.example.lecturesystem.modules.auth.service.WechatMpAuthService;
import com.example.lecturesystem.modules.auth.vo.LoginVO;
import com.example.lecturesystem.modules.auth.vo.WechatMpAuthorizeUrlVO;
import com.example.lecturesystem.modules.param.service.ParamService;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Service
public class WechatMpAuthServiceImpl implements WechatMpAuthService {
    private static final Logger log = LoggerFactory.getLogger(WechatMpAuthServiceImpl.class);

    private static final String WECHAT_OAUTH_AUTHORIZE_URL = "https://open.weixin.qq.com/connect/oauth2/authorize";
    private static final String WECHAT_OAUTH_ACCESS_TOKEN_URL = "https://api.weixin.qq.com/sns/oauth2/access_token";

    private static final String DEFAULT_AUTHORIZE_SCOPE = "snsapi_base";
    private static final String DEFAULT_RETURN_URL = "/mobile-workspace";
    private static final String LOGIN_PATH = "/login";
    private static final String HASH_LOGIN_KEY = "wechatLogin";
    private static final String FAILURE_FLAG_QUERY = "wechatAuthFailed";
    private static final String FAILURE_MESSAGE_QUERY = "wechatAuthMessage";
    private static final String FAILURE_CODE_QUERY = "wechatAuthCode";

    private static final String PARAM_WECHAT_MP_ENABLED = "WECHAT_MP_ENABLED";
    private static final String PARAM_WECHAT_MP_APP_ID = "WECHAT_MP_APP_ID";
    private static final String PARAM_WECHAT_MP_APP_SECRET = "WECHAT_MP_APP_SECRET";
    private static final String PARAM_WECHAT_MP_AUTHORIZE_SCOPE = "WECHAT_MP_AUTHORIZE_SCOPE";
    private static final String PARAM_WECHAT_MP_SCOPE = "WECHAT_MP_SCOPE";
    private static final String PARAM_WECHAT_MP_OAUTH_REDIRECT_URI = "WECHAT_MP_OAUTH_REDIRECT_URI";
    private static final String PARAM_WECHAT_MP_CALLBACK_URL = "WECHAT_MP_CALLBACK_URL";

    private static final String INTERFACE_AUTHORIZE_URL = "wechat-mp-authorize-url";
    private static final String INTERFACE_EXCHANGE_CODE = "wechat-mp-exchange-code";

    private final WechatProperties wechatProperties;
    private final ParamService paramService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public WechatMpAuthServiceImpl(WechatProperties wechatProperties, ParamService paramService) {
        this.wechatProperties = wechatProperties;
        this.paramService = paramService;
    }

    @Override
    public WechatMpAuthorizeUrlVO buildAuthorizeUrl(String returnUrl, String state) {
        WechatMpConfigSnapshot config = resolveWechatMpConfig();
        List<String> missing = validateConfigForAuthorize(config);
        if (!missing.isEmpty()) {
            log.warn("[WeChatMP] config invalid: interface={}, source={}, missing={}, enabled={}, appIdEmpty={}, appSecretEmpty={}, redirectUriEmpty={}, scope={}",
                    INTERFACE_AUTHORIZE_URL,
                    config.getSource(),
                    missing,
                    config.getEnabled(),
                    isBlank(config.getAppId()),
                    isBlank(config.getAppSecret()),
                    isBlank(config.getOauthRedirectUri()),
                    config.getAuthorizeScope());
            throw new IllegalArgumentException(buildConfigErrorMessage(INTERFACE_AUTHORIZE_URL, config, missing));
        }

        try {
            WechatMpCallbackState callbackState = new WechatMpCallbackState(normalizeReturnUrl(returnUrl), trimToNull(state));
            String encodedState = encodeJson(callbackState);
            String authorizeUrl = UriComponentsBuilder.fromUriString(WECHAT_OAUTH_AUTHORIZE_URL)
                    .queryParam("appid", config.getAppId())
                    .queryParam("redirect_uri", config.getOauthRedirectUri())
                    .queryParam("response_type", "code")
                    .queryParam("scope", config.getAuthorizeScope())
                    .queryParam("state", encodedState)
                    .build()
                    .encode()
                    .toUriString() + "#wechat_redirect";

            WechatMpAuthorizeUrlVO result = new WechatMpAuthorizeUrlVO();
            result.setEnabled(Boolean.TRUE.equals(config.getEnabled()));
            result.setAuthorizeScope(config.getAuthorizeScope());
            result.setAuthorizeUrl(authorizeUrl);
            return result;
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("[WeChatMP] authorize url generation failed: interface={}, source={}", INTERFACE_AUTHORIZE_URL, config.getSource(), ex);
            throw new IllegalStateException(buildRuntimeErrorMessage("微信公众号授权地址生成失败", INTERFACE_AUTHORIZE_URL, config, ex), ex);
        }
    }

    @Override
    public WechatMpIdentity exchangeCode(String code) {
        WechatMpConfigSnapshot config = resolveWechatMpConfig();
        List<String> missing = validateConfigForExchange(config);
        if (!missing.isEmpty()) {
            log.warn("[WeChatMP] config invalid: interface={}, source={}, missing={}, enabled={}, appIdEmpty={}, appSecretEmpty={}, redirectUriEmpty={}, scope={}",
                    INTERFACE_EXCHANGE_CODE,
                    config.getSource(),
                    missing,
                    config.getEnabled(),
                    isBlank(config.getAppId()),
                    isBlank(config.getAppSecret()),
                    isBlank(config.getOauthRedirectUri()),
                    config.getAuthorizeScope());
            throw new IllegalArgumentException(buildConfigErrorMessage(INTERFACE_EXCHANGE_CODE, config, missing));
        }

        String normalizedCode = requireText(code, "微信公众号授权 code 不能为空");
        try {
            String requestUrl = WECHAT_OAUTH_ACCESS_TOKEN_URL
                    + "?appid=" + encode(config.getAppId())
                    + "&secret=" + encode(config.getAppSecret())
                    + "&code=" + encode(normalizedCode)
                    + "&grant_type=authorization_code";
            HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(requestUrl))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            WechatMpAccessTokenResponse accessTokenResponse = objectMapper.readValue(response.body(), WechatMpAccessTokenResponse.class);
            if (accessTokenResponse.getErrcode() != null && accessTokenResponse.getErrcode() != 0) {
                throw buildWechatMpExchangeException(accessTokenResponse);
            }
            String openId = trimToNull(accessTokenResponse.getOpenid());
            String unionId = trimToNull(accessTokenResponse.getUnionid());
            if (openId == null) {
                throw new IllegalArgumentException("微信公众号授权结果缺少 openid，无法完成登录");
            }
            return new WechatMpIdentity(openId, unionId);
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("[WeChatMP] exchange code failed: interface={}, source={}", INTERFACE_EXCHANGE_CODE, config.getSource(), ex);
            throw new IllegalStateException(buildRuntimeErrorMessage("微信公众号授权换取身份失败", INTERFACE_EXCHANGE_CODE, config, ex), ex);
        }
    }

    @Override
    public WechatMpCallbackState parseCallbackState(String state) {
        if (trimToNull(state) == null) {
            return new WechatMpCallbackState(DEFAULT_RETURN_URL, null);
        }
        try {
            WechatMpCallbackState callbackState = objectMapper.readValue(decodeJson(state), WechatMpCallbackState.class);
            return new WechatMpCallbackState(
                    normalizeReturnUrl(callbackState.returnUrl()),
                    trimToNull(callbackState.requestState())
            );
        } catch (Exception ex) {
            log.warn("微信公众号 callback state 解析失败，将使用默认回落页", ex);
            return new WechatMpCallbackState(DEFAULT_RETURN_URL, null);
        }
    }

    @Override
    public String buildSuccessCallbackRedirect(LoginVO loginVO, WechatMpCallbackState callbackState) {
        WechatLoginCallbackPayload payload = new WechatLoginCallbackPayload(loginVO, callbackState.returnUrl());
        return buildLoginRedirect(callbackState.returnUrl(), null, null, encodeJson(payload));
    }

    @Override
    public String buildFailureCallbackRedirect(WechatMpCallbackState callbackState, String message) {
        return buildFailureCallbackRedirect(callbackState, CALLBACK_ERROR_CODE_UNKNOWN, message);
    }

    @Override
    public String buildFailureCallbackRedirect(WechatMpCallbackState callbackState, String code, String message) {
        return buildLoginRedirect(
                callbackState.returnUrl(),
                defaultIfBlank(trimToNull(code), CALLBACK_ERROR_CODE_UNKNOWN),
                defaultIfBlank(trimToNull(message), "微信公众号登录失败，请稍后重试"),
                null
        );
    }

    private String buildLoginRedirect(String returnUrl, String errorCode, String errorMessage, String successPayload) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(resolveFrontendBaseUrl() + LOGIN_PATH)
                .queryParam("redirect", normalizeReturnUrl(returnUrl));
        if (errorMessage != null) {
            builder.queryParam(FAILURE_FLAG_QUERY, "1");
            builder.queryParam(FAILURE_MESSAGE_QUERY, errorMessage);
            builder.queryParam(FAILURE_CODE_QUERY, defaultIfBlank(trimToNull(errorCode), CALLBACK_ERROR_CODE_UNKNOWN));
        }
        if (successPayload != null) {
            builder.fragment(HASH_LOGIN_KEY + "=" + successPayload);
        }
        return builder.build().encode().toUriString();
    }

    private String resolveFrontendBaseUrl() {
        WechatMpConfigSnapshot config = resolveWechatMpConfig(false);
        String redirectUri = trimToNull(config.getOauthRedirectUri());
        if (redirectUri == null) {
            return "";
        }
        try {
            URI uri = URI.create(redirectUri);
            if (uri.getScheme() == null || uri.getHost() == null) {
                return "";
            }
            int port = uri.getPort();
            if (port < 0) {
                return uri.getScheme() + "://" + uri.getHost();
            }
            return uri.getScheme() + "://" + uri.getHost() + ":" + port;
        } catch (Exception ex) {
            log.warn("[WeChatMP] oauth redirect uri parse failed: source={}, redirectUri={}", config.getSource(), redirectUri, ex);
            return "";
        }
    }

    private String getSysParamValue(String code) {
        try {
            return trimToNull(paramService.getByCode(code));
        } catch (Exception ex) {
            log.warn("[WeChatMP] failed to read sys_param: code={}", code, ex);
            return null;
        }
    }

    private WechatMpConfigSnapshot resolveWechatMpConfig() {
        return resolveWechatMpConfig(true);
    }

    private WechatMpConfigSnapshot resolveWechatMpConfig(boolean logResolved) {
        WechatProperties.Mp mp = wechatProperties != null ? wechatProperties.getMp() : null;
        boolean sysParamUsed = false;
        boolean propertiesUsed = false;
        boolean defaultUsed = false;

        Boolean enabled = null;
        String enabledFromParam = getSysParamValue(PARAM_WECHAT_MP_ENABLED);
        if (enabledFromParam != null) {
            enabled = parseBoolean(enabledFromParam, PARAM_WECHAT_MP_ENABLED);
            if (enabled != null) {
                sysParamUsed = true;
            }
        }
        if (enabled == null) {
            enabled = mp != null && mp.isEnabled();
            propertiesUsed = true;
        }

        String appIdFromParam = getSysParamValue(PARAM_WECHAT_MP_APP_ID);
        String appId;
        if (appIdFromParam != null) {
            appId = appIdFromParam;
            sysParamUsed = true;
        } else {
            appId = trimToNull(mp != null ? mp.getAppId() : null);
            if (appId != null) {
                propertiesUsed = true;
            }
        }

        String appSecretFromParam = getSysParamValue(PARAM_WECHAT_MP_APP_SECRET);
        String appSecret;
        if (appSecretFromParam != null) {
            appSecret = appSecretFromParam;
            sysParamUsed = true;
        } else {
            appSecret = trimToNull(mp != null ? mp.getAppSecret() : null);
            if (appSecret != null) {
                propertiesUsed = true;
            }
        }

        String authorizeScope = getSysParamValue(PARAM_WECHAT_MP_AUTHORIZE_SCOPE);
        if (authorizeScope != null) {
            sysParamUsed = true;
        } else {
            authorizeScope = getSysParamValue(PARAM_WECHAT_MP_SCOPE);
            if (authorizeScope != null) {
                sysParamUsed = true;
            } else {
                authorizeScope = trimToNull(mp != null ? mp.getAuthorizeScope() : null);
                if (authorizeScope != null) {
                    propertiesUsed = true;
                } else {
                    authorizeScope = DEFAULT_AUTHORIZE_SCOPE;
                    defaultUsed = true;
                }
            }
        }

        String oauthRedirectUri = getSysParamValue(PARAM_WECHAT_MP_OAUTH_REDIRECT_URI);
        if (oauthRedirectUri != null) {
            sysParamUsed = true;
        } else {
            oauthRedirectUri = getSysParamValue(PARAM_WECHAT_MP_CALLBACK_URL);
            if (oauthRedirectUri != null) {
                sysParamUsed = true;
            } else {
                oauthRedirectUri = trimToNull(mp != null ? mp.getOauthRedirectUri() : null);
                if (oauthRedirectUri != null) {
                    propertiesUsed = true;
                }
            }
        }

        WechatMpConfigSnapshot snapshot = new WechatMpConfigSnapshot();
        snapshot.setEnabled(enabled);
        snapshot.setAppId(appId);
        snapshot.setAppSecret(appSecret);
        snapshot.setAuthorizeScope(authorizeScope);
        snapshot.setOauthRedirectUri(oauthRedirectUri);
        snapshot.setSource(determineSource(sysParamUsed, propertiesUsed, defaultUsed));

        if (logResolved) {
            log.info("[WeChatMP] resolved config: source={}, enabled={}, appId={}, appSecret={}, redirectUri={}, scope={}",
                    snapshot.getSource(),
                    snapshot.getEnabled(),
                    mask(snapshot.getAppId()),
                    mask(snapshot.getAppSecret()),
                    defaultIfBlank(snapshot.getOauthRedirectUri(), "<empty>"),
                    defaultIfBlank(snapshot.getAuthorizeScope(), DEFAULT_AUTHORIZE_SCOPE));
        }
        return snapshot;
    }

    private List<String> validateConfigForAuthorize(WechatMpConfigSnapshot config) {
        return validateBaseConfig(config);
    }

    private List<String> validateConfigForExchange(WechatMpConfigSnapshot config) {
        return validateBaseConfig(config);
    }

    private List<String> validateBaseConfig(WechatMpConfigSnapshot config) {
        List<String> missing = new ArrayList<>();
        if (!Boolean.TRUE.equals(config.getEnabled())) {
            missing.add("enabled");
        }
        if (isBlank(config.getAppId())) {
            missing.add("appId");
        }
        if (isBlank(config.getAppSecret())) {
            missing.add("appSecret");
        }
        if (isBlank(config.getOauthRedirectUri())) {
            missing.add("redirectUri");
        }
        return missing;
    }

    private String buildConfigErrorMessage(String interfaceName, WechatMpConfigSnapshot config, List<String> missing) {
        return resolveActionLabel(interfaceName)
                + "："
                + (Boolean.TRUE.equals(config.getEnabled()) ? "配置不完整（配置缺少必要项）。" : "公众号登录暂未启用或配置不完整。")
                + "interface=" + interfaceName
                + ", source=" + config.getSource()
                + ", enabled=" + config.getEnabled()
                + ", appIdEmpty=" + isBlank(config.getAppId())
                + ", appSecretEmpty=" + isBlank(config.getAppSecret())
                + ", redirectUriEmpty=" + isBlank(config.getOauthRedirectUri())
                + ", scope=" + defaultIfBlank(config.getAuthorizeScope(), DEFAULT_AUTHORIZE_SCOPE)
                + ", missing=" + missing;
    }

    private String buildRuntimeErrorMessage(String actionLabel, String interfaceName, WechatMpConfigSnapshot config, Exception ex) {
        String exceptionMessage = trimToNull(ex.getMessage());
        String detail = ex.getClass().getSimpleName();
        if (exceptionMessage != null) {
            detail = detail + ": " + exceptionMessage;
        }
        return actionLabel
                + "："
                + detail
                + "。interface=" + interfaceName
                + ", source=" + config.getSource();
    }

    private String resolveActionLabel(String interfaceName) {
        if (INTERFACE_AUTHORIZE_URL.equals(interfaceName)) {
            return "微信公众号授权地址生成失败";
        }
        if (INTERFACE_EXCHANGE_CODE.equals(interfaceName)) {
            return "微信公众号授权换取身份失败";
        }
        return "微信公众号授权失败";
    }

    private IllegalArgumentException buildWechatMpExchangeException(WechatMpAccessTokenResponse response) {
        Integer errcode = response.getErrcode();
        if (Integer.valueOf(40029).equals(errcode) || Integer.valueOf(40163).equals(errcode)) {
            return new IllegalArgumentException("微信公众号授权 code 无效或已过期，请从微信内重新进入登录页");
        }
        return new IllegalArgumentException("微信公众号授权失败：" + defaultIfBlank(response.getErrmsg(), "errcode=" + errcode));
    }

    private String normalizeReturnUrl(String returnUrl) {
        String normalized = trimToNull(returnUrl);
        if (normalized == null) {
            return DEFAULT_RETURN_URL;
        }
        if (!normalized.startsWith("/") || normalized.startsWith("//")) {
            return DEFAULT_RETURN_URL;
        }
        return normalized;
    }

    private String encodeJson(Object value) {
        try {
            String json = objectMapper.writeValueAsString(value);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            throw new IllegalStateException("微信公众号登录状态编码失败", ex);
        }
    }

    private String decodeJson(String encoded) {
        try {
            byte[] bytes = Base64.getUrlDecoder().decode(addBase64Padding(encoded));
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalArgumentException("微信公众号登录状态无效", ex);
        }
    }

    private String addBase64Padding(String value) {
        int mod = value.length() % 4;
        if (mod == 0) {
            return value;
        }
        return value + "=".repeat(4 - mod);
    }

    private String requireText(String value, String message) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new IllegalArgumentException(message);
        }
        return normalized;
    }

    private String defaultIfBlank(String value, String fallback) {
        String normalized = trimToNull(value);
        return normalized == null ? fallback : normalized;
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

    private String determineSource(boolean sysParamUsed, boolean propertiesUsed, boolean defaultUsed) {
        if (sysParamUsed && propertiesUsed) {
            return "mixed";
        }
        if (sysParamUsed) {
            return "sys_param";
        }
        if (propertiesUsed) {
            return "properties";
        }
        if (defaultUsed) {
            return "default";
        }
        return "default";
    }

    private Boolean parseBoolean(String value, String code) {
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
        log.warn("[WeChatMP] invalid boolean sys_param value: code={}, value={}", code, mask(normalized));
        return null;
    }

    private boolean isBlank(String value) {
        return trimToNull(value) == null;
    }

    private String mask(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return "<empty>";
        }
        if (normalized.length() <= 4) {
            return "****";
        }
        return normalized.substring(0, 2) + "****" + normalized.substring(normalized.length() - 2);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class WechatMpAccessTokenResponse {
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

    private static class WechatLoginCallbackPayload {
        private final LoginVO loginInfo;
        private final String returnUrl;

        private WechatLoginCallbackPayload(LoginVO loginInfo, String returnUrl) {
            this.loginInfo = loginInfo;
            this.returnUrl = returnUrl;
        }

        public LoginVO getLoginInfo() {
            return loginInfo;
        }

        public String getReturnUrl() {
            return returnUrl;
        }
    }

    private static class WechatMpConfigSnapshot {
        private Boolean enabled;
        private String appId;
        private String appSecret;
        private String authorizeScope;
        private String oauthRedirectUri;
        private String source;

        public Boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
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

        public String getAuthorizeScope() {
            return authorizeScope;
        }

        public void setAuthorizeScope(String authorizeScope) {
            this.authorizeScope = authorizeScope;
        }

        public String getOauthRedirectUri() {
            return oauthRedirectUri;
        }

        public void setOauthRedirectUri(String oauthRedirectUri) {
            this.oauthRedirectUri = oauthRedirectUri;
        }

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }
    }
}
