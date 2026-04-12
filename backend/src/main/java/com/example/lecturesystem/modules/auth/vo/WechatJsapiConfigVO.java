package com.example.lecturesystem.modules.auth.vo;

import java.util.ArrayList;
import java.util.List;

public class WechatJsapiConfigVO {
    private boolean enabled;
    private boolean locationEnabled;
    private String appId;
    private Long timestamp;
    private String nonceStr;
    private String signature;
    private List<String> jsApiList = new ArrayList<>();
    private String locationType;
    private boolean debug;
    private String priority;
    private String fallback;
    private Integer accuracyThreshold;
    private Integer timeoutMs;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isLocationEnabled() {
        return locationEnabled;
    }

    public void setLocationEnabled(boolean locationEnabled) {
        this.locationEnabled = locationEnabled;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public String getNonceStr() {
        return nonceStr;
    }

    public void setNonceStr(String nonceStr) {
        this.nonceStr = nonceStr;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public List<String> getJsApiList() {
        return jsApiList;
    }

    public void setJsApiList(List<String> jsApiList) {
        this.jsApiList = jsApiList == null ? new ArrayList<>() : new ArrayList<>(jsApiList);
    }

    public String getLocationType() {
        return locationType;
    }

    public void setLocationType(String locationType) {
        this.locationType = locationType;
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getFallback() {
        return fallback;
    }

    public void setFallback(String fallback) {
        this.fallback = fallback;
    }

    public Integer getAccuracyThreshold() {
        return accuracyThreshold;
    }

    public void setAccuracyThreshold(Integer accuracyThreshold) {
        this.accuracyThreshold = accuracyThreshold;
    }

    public Integer getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(Integer timeoutMs) {
        this.timeoutMs = timeoutMs;
    }
}
