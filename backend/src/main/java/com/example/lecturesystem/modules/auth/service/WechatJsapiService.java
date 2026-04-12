package com.example.lecturesystem.modules.auth.service;

import com.example.lecturesystem.modules.auth.vo.WechatJsapiConfigVO;

public interface WechatJsapiService {
    WechatJsapiConfigVO queryJsapiConfig(String url);
}
