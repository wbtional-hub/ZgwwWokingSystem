package com.example.lecturesystem.modules.auth.service;

import com.example.lecturesystem.modules.auth.vo.LoginVO;
import com.example.lecturesystem.modules.auth.vo.WechatMpAuthorizeUrlVO;

public interface WechatMpAuthService {
    String CALLBACK_ERROR_CODE_UNBOUND = "WECHAT_MP_UNBOUND";
    String CALLBACK_ERROR_CODE_NOT_ENABLED = "WECHAT_MP_NOT_ENABLED";
    String CALLBACK_ERROR_CODE_CONFIG_MISSING = "WECHAT_MP_CONFIG_MISSING";
    String CALLBACK_ERROR_CODE_CODE_INVALID = "WECHAT_MP_CODE_INVALID";
    String CALLBACK_ERROR_CODE_CODE_MISSING = "WECHAT_MP_CODE_MISSING";
    String CALLBACK_ERROR_CODE_OAUTH_DENIED = "WECHAT_MP_OAUTH_DENIED";
    String CALLBACK_ERROR_CODE_BINDING_CONFLICT = "WECHAT_MP_BINDING_CONFLICT";
    String CALLBACK_ERROR_CODE_USER_DISABLED = "WECHAT_MP_USER_DISABLED";
    String CALLBACK_ERROR_CODE_UNKNOWN = "WECHAT_MP_LOGIN_FAILED";

    WechatMpAuthorizeUrlVO buildAuthorizeUrl(String returnUrl, String state);

    WechatMpIdentity exchangeCode(String code);

    WechatMpCallbackState parseCallbackState(String state);

    String buildSuccessCallbackRedirect(LoginVO loginVO, WechatMpCallbackState callbackState);

    String buildFailureCallbackRedirect(WechatMpCallbackState callbackState, String message);

    default String buildFailureCallbackRedirect(WechatMpCallbackState callbackState, String code, String message) {
        return buildFailureCallbackRedirect(callbackState, message);
    }

    record WechatMpIdentity(String openId, String unionId) {
    }

    record WechatMpCallbackState(String returnUrl, String requestState) {
    }
}
