package com.example.lecturesystem.modules.auth.service.impl;

import com.example.lecturesystem.modules.auth.entity.WechatMpPendingBindEntity;
import com.example.lecturesystem.modules.auth.mapper.WechatMpPendingBindMapper;
import com.example.lecturesystem.modules.auth.service.WechatMpPendingBindService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class WechatMpPendingBindServiceImpl implements WechatMpPendingBindService {

    private final WechatMpPendingBindMapper wechatMpPendingBindMapper;

    public WechatMpPendingBindServiceImpl(WechatMpPendingBindMapper wechatMpPendingBindMapper) {
        this.wechatMpPendingBindMapper = wechatMpPendingBindMapper;
    }

    @Override
    public WechatMpPendingBindEntity saveIfAbsent(String openId, String unionId, String requestIp, String userAgent, String remark) {
        boolean noOpenId = openId == null || openId.isBlank();
        boolean noUnionId = unionId == null || unionId.isBlank();
        if (noOpenId && noUnionId) {
            return null;
        }

        WechatMpPendingBindEntity existing = wechatMpPendingBindMapper.findPendingByIdentity(openId, unionId);
        if (existing != null) {
            wechatMpPendingBindMapper.touchPendingBind(existing.getId(), requestIp, userAgent, remark);
            return wechatMpPendingBindMapper.findById(existing.getId());
        }

        WechatMpPendingBindEntity entity = new WechatMpPendingBindEntity();
        entity.setOpenId(openId);
        entity.setUnionId(unionId);
        entity.setSourceType("MP");
        entity.setBindStatus("PENDING");
        entity.setRequestIp(requestIp);
        entity.setUserAgent(userAgent);
        entity.setRemark(remark);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        wechatMpPendingBindMapper.insertPendingBind(entity);

        if (entity.getId() != null) {
            return entity;
        }
        return wechatMpPendingBindMapper.findPendingByIdentity(openId, unionId);
    }
}