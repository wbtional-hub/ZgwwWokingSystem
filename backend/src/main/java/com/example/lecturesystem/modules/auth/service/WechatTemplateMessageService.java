package com.example.lecturesystem.modules.auth.service;

import java.util.Map;

public interface WechatTemplateMessageService {
    SendResult sendTemplateMessage(SendRequest request);

    class SendRequest {
        private String openId;
        private String templateId;
        private String url;
        private Map<String, TemplateDataItem> data;

        public String getOpenId() {
            return openId;
        }

        public void setOpenId(String openId) {
            this.openId = openId;
        }

        public String getTemplateId() {
            return templateId;
        }

        public void setTemplateId(String templateId) {
            this.templateId = templateId;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public Map<String, TemplateDataItem> getData() {
            return data;
        }

        public void setData(Map<String, TemplateDataItem> data) {
            this.data = data;
        }
    }

    class TemplateDataItem {
        private String value;
        private String color;

        public TemplateDataItem() {
        }

        public TemplateDataItem(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getColor() {
            return color;
        }

        public void setColor(String color) {
            this.color = color;
        }
    }

    class SendResult {
        private boolean success;
        private String errCode;
        private String errMsg;
        private String requestPayload;
        private String responseBody;

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getErrCode() {
            return errCode;
        }

        public void setErrCode(String errCode) {
            this.errCode = errCode;
        }

        public String getErrMsg() {
            return errMsg;
        }

        public void setErrMsg(String errMsg) {
            this.errMsg = errMsg;
        }

        public String getRequestPayload() {
            return requestPayload;
        }

        public void setRequestPayload(String requestPayload) {
            this.requestPayload = requestPayload;
        }

        public String getResponseBody() {
            return responseBody;
        }

        public void setResponseBody(String responseBody) {
            this.responseBody = responseBody;
        }
    }
}
