package com.example.lecturesystem.modules.auth.service;

import com.example.lecturesystem.modules.auth.entity.WechatMpPendingBindEntity;

public interface WechatMpPendingBindService {
    WechatMpPendingBindEntity saveIfAbsent(String openId, String unionId, String requestIp, String userAgent, String remark);
}