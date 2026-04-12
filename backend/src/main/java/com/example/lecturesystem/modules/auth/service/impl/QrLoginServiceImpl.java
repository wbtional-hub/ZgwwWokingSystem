package com.example.lecturesystem.modules.auth.service.impl;

import com.example.lecturesystem.modules.auth.entity.QrLoginSession;
import com.example.lecturesystem.modules.auth.mapper.QrLoginSessionMapper;
import com.example.lecturesystem.modules.auth.security.JwtTokenService;
import com.example.lecturesystem.modules.auth.security.LoginUser;
import com.example.lecturesystem.modules.auth.service.QrLoginService;
import com.example.lecturesystem.modules.auth.vo.LoginVO;
import com.example.lecturesystem.modules.auth.vo.QrLoginConfirmResultVO;
import com.example.lecturesystem.modules.auth.vo.QrLoginMobileSessionVO;
import com.example.lecturesystem.modules.auth.vo.QrLoginSessionCreateVO;
import com.example.lecturesystem.modules.auth.vo.QrLoginSessionStatusVO;
import com.example.lecturesystem.modules.user.entity.UserEntity;
import com.example.lecturesystem.modules.user.mapper.UserMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class QrLoginServiceImpl implements QrLoginService {
    static final String STATUS_PENDING = "PENDING";
    static final String STATUS_SCANNED = "SCANNED";
    static final String STATUS_CONFIRMED = "CONFIRMED";
    static final String STATUS_CONSUMED = "CONSUMED";
    static final String STATUS_EXPIRED = "EXPIRED";
    static final String STATUS_CANCELED = "CANCELED";

    private static final int EXPIRE_MINUTES = 2;

    private final QrLoginSessionMapper qrLoginSessionMapper;
    private final UserMapper userMapper;
    private final JwtTokenService jwtTokenService;

    public QrLoginServiceImpl(QrLoginSessionMapper qrLoginSessionMapper,
                              UserMapper userMapper,
                              JwtTokenService jwtTokenService) {
        this.qrLoginSessionMapper = qrLoginSessionMapper;
        this.userMapper = userMapper;
        this.jwtTokenService = jwtTokenService;
    }

    @Override
    public QrLoginSessionCreateVO createSession() {
        LocalDateTime expireTime = LocalDateTime.now().plusMinutes(EXPIRE_MINUTES);
        QrLoginSession session = new QrLoginSession();
        session.setQrToken(generateQrToken());
        session.setExpireTime(expireTime);
        qrLoginSessionMapper.insertSession(session);

        QrLoginSessionCreateVO result = new QrLoginSessionCreateVO();
        result.setQrToken(session.getQrToken());
        result.setExpireAt(expireTime);
        result.setQrUrl("/mobile/qr-confirm?qrToken=" + session.getQrToken());
        return result;
    }

    @Override
    public QrLoginSessionStatusVO getSessionStatus(String qrToken) {
        String normalizedToken = normalizeQrToken(qrToken);
        QrLoginSession session = requireSession(normalizedToken);
        if (isExpired(session)) {
            markExpired(normalizedToken, session);
            return buildStatusResult(STATUS_EXPIRED, null);
        }

        String currentStatus = normalizeStatus(session.getStatus());
        if (STATUS_CONFIRMED.equals(currentStatus)) {
            if (session.getConsumedTime() != null || STATUS_CONSUMED.equals(session.getStatus())) {
                return buildStatusResult(STATUS_CONSUMED, null);
            }

            LoginVO loginVO = buildLoginVO(session.getUserId());
            int updated = qrLoginSessionMapper.consumeConfirmedSession(normalizedToken, LocalDateTime.now());
            if (updated > 0) {
                return buildStatusResult(STATUS_CONFIRMED, loginVO);
            }
            return buildStatusResult(STATUS_CONSUMED, null);
        }

        if (STATUS_CONSUMED.equals(session.getStatus())) {
            return buildStatusResult(STATUS_CONSUMED, null);
        }
        return buildStatusResult(currentStatus, null);
    }

    @Override
    public QrLoginMobileSessionVO getMobileSession(String qrToken) {
        String normalizedToken = normalizeQrToken(qrToken);
        QrLoginSession session = requireSession(normalizedToken);
        if (isExpired(session)) {
            markExpired(normalizedToken, session);
            return buildMobileSessionVO(session.getQrToken(), STATUS_EXPIRED, session.getExpireTime());
        }

        String status = normalizeStatus(session.getStatus());
        if (STATUS_PENDING.equals(status)) {
            qrLoginSessionMapper.markScanned(normalizedToken, LocalDateTime.now());
            status = STATUS_SCANNED;
        }
        return buildMobileSessionVO(session.getQrToken(), status, session.getExpireTime());
    }

    @Override
    public QrLoginConfirmResultVO confirmSession(String qrToken, Long userId, boolean approve) {
        if (userId == null) {
            throw new IllegalArgumentException("当前未登录");
        }
        String normalizedToken = normalizeQrToken(qrToken);
        QrLoginSession session = requireSession(normalizedToken);
        if (isExpired(session)) {
            markExpired(normalizedToken, session);
            throw new IllegalArgumentException("二维码已过期，不能再确认");
        }

        String currentStatus = normalizeStatus(session.getStatus());
        if (STATUS_CONFIRMED.equals(currentStatus)) {
            throw new IllegalArgumentException("该二维码已确认，不能重复操作");
        }
        if (STATUS_CANCELED.equals(currentStatus)) {
            throw new IllegalArgumentException("该二维码已取消，不能重复操作");
        }
        if (STATUS_CONSUMED.equals(session.getStatus())) {
            throw new IllegalArgumentException("该二维码已完成登录，不能重复确认");
        }
        if (!STATUS_PENDING.equals(currentStatus) && !STATUS_SCANNED.equals(currentStatus)) {
            throw new IllegalArgumentException("当前二维码状态不允许确认");
        }

        if (approve) {
            if (session.getUserId() != null && !session.getUserId().equals(userId)) {
                throw new IllegalArgumentException("该二维码会话已绑定其他用户，不能覆盖确认");
            }
            qrLoginSessionMapper.confirmSession(normalizedToken, userId, STATUS_CONFIRMED, LocalDateTime.now());
            return buildConfirmResult(STATUS_CONFIRMED);
        }

        qrLoginSessionMapper.updateStatus(normalizedToken, STATUS_CANCELED);
        return buildConfirmResult(STATUS_CANCELED);
    }

    private String generateQrToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private String normalizeQrToken(String qrToken) {
        if (qrToken == null || qrToken.trim().isEmpty()) {
            throw new IllegalArgumentException("qrToken不能为空");
        }
        return qrToken.trim();
    }

    private QrLoginSession requireSession(String qrToken) {
        QrLoginSession session = qrLoginSessionMapper.selectByQrToken(qrToken);
        if (session == null) {
            throw new IllegalArgumentException("二维码会话不存在");
        }
        return session;
    }

    private LoginVO buildLoginVO(Long userId) {
        if (userId == null) {
            throw new IllegalStateException("二维码会话缺少确认用户");
        }
        UserEntity user = userMapper.findById(userId);
        if (user == null) {
            throw new IllegalStateException("二维码对应用户不存在");
        }
        LoginUser loginUser = new LoginUser(user.getId(), user.getUsername(), user.getRealName(), user.getRole());
        LoginVO loginVO = LoginVO.fromLoginUser(loginUser);
        loginVO.setToken(jwtTokenService.generateToken(loginUser));
        loginVO.setForcePasswordChange(Boolean.TRUE.equals(user.getForcePasswordChange()));
        return loginVO;
    }

    private boolean isExpired(QrLoginSession session) {
        return session.getExpireTime() != null && session.getExpireTime().isBefore(LocalDateTime.now());
    }

    private void markExpired(String qrToken, QrLoginSession session) {
        if (!STATUS_EXPIRED.equals(session.getStatus())) {
            qrLoginSessionMapper.updateStatus(qrToken, STATUS_EXPIRED);
        }
    }

    private String normalizeStatus(String status) {
        if (STATUS_CONSUMED.equals(status)) {
            return STATUS_CONFIRMED;
        }
        return status == null ? STATUS_PENDING : status;
    }

    private QrLoginMobileSessionVO buildMobileSessionVO(String qrToken, String status, LocalDateTime expireTime) {
        QrLoginMobileSessionVO result = new QrLoginMobileSessionVO();
        result.setQrToken(qrToken);
        result.setStatus(status);
        result.setExpireAt(expireTime);
        return result;
    }

    private QrLoginConfirmResultVO buildConfirmResult(String status) {
        QrLoginConfirmResultVO result = new QrLoginConfirmResultVO();
        result.setSuccess(true);
        result.setStatus(status);
        return result;
    }

    private QrLoginSessionStatusVO buildStatusResult(String status, LoginVO loginVO) {
        QrLoginSessionStatusVO result = new QrLoginSessionStatusVO(status);
        result.setLogin(loginVO);
        return result;
    }
}
