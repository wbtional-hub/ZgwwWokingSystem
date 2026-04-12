package com.example.lecturesystem.modules.auth.service.impl;

import com.example.lecturesystem.modules.auth.entity.QrLoginSession;
import com.example.lecturesystem.modules.auth.mapper.QrLoginSessionMapper;
import com.example.lecturesystem.modules.auth.security.JwtTokenService;
import com.example.lecturesystem.modules.auth.vo.QrLoginConfirmResultVO;
import com.example.lecturesystem.modules.auth.vo.QrLoginMobileSessionVO;
import com.example.lecturesystem.modules.auth.vo.QrLoginSessionCreateVO;
import com.example.lecturesystem.modules.auth.vo.QrLoginSessionStatusVO;
import com.example.lecturesystem.modules.user.dto.UserQueryRequest;
import com.example.lecturesystem.modules.user.entity.UserEntity;
import com.example.lecturesystem.modules.user.mapper.UserMapper;
import com.example.lecturesystem.modules.user.vo.UserDetailVO;
import com.example.lecturesystem.modules.user.vo.UserListItemVO;
import org.junit.Assert;
import org.junit.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QrLoginServiceTest {
    @Test
    public void createSessionShouldGeneratePendingSession() {
        InMemoryQrLoginSessionMapper mapper = new InMemoryQrLoginSessionMapper();
        StubUserMapper userMapper = new StubUserMapper();
        QrLoginServiceImpl service = new QrLoginServiceImpl(mapper, userMapper, jwtTokenService());

        LocalDateTime before = LocalDateTime.now();
        QrLoginSessionCreateVO result = service.createSession();
        LocalDateTime after = LocalDateTime.now();

        Assert.assertNotNull(result.getQrToken());
        Assert.assertEquals(32, result.getQrToken().length());
        Assert.assertEquals("/mobile/qr-confirm?qrToken=" + result.getQrToken(), result.getQrUrl());

        QrLoginSession session = mapper.selectByQrToken(result.getQrToken());
        Assert.assertNotNull(session);
        Assert.assertEquals(QrLoginServiceImpl.STATUS_PENDING, session.getStatus());
        Assert.assertFalse(session.getExpireTime().isBefore(before.plusMinutes(2).minusSeconds(2)));
        Assert.assertFalse(session.getExpireTime().isAfter(after.plusMinutes(2).plusSeconds(2)));
    }

    @Test
    public void getSessionStatusShouldReturnCurrentStatus() {
        InMemoryQrLoginSessionMapper mapper = new InMemoryQrLoginSessionMapper();
        StubUserMapper userMapper = new StubUserMapper();
        QrLoginServiceImpl service = new QrLoginServiceImpl(mapper, userMapper, jwtTokenService());
        QrLoginSession session = new QrLoginSession();
        session.setQrToken("token-1");
        session.setStatus(QrLoginServiceImpl.STATUS_SCANNED);
        session.setExpireTime(LocalDateTime.now().plusMinutes(1));
        mapper.insertSeed(session);

        QrLoginSessionStatusVO result = service.getSessionStatus("token-1");

        Assert.assertEquals(QrLoginServiceImpl.STATUS_SCANNED, result.getStatus());
        Assert.assertNull(result.getLogin());
    }

    @Test
    public void getSessionStatusShouldReturnExpiredWhenSessionExpired() {
        InMemoryQrLoginSessionMapper mapper = new InMemoryQrLoginSessionMapper();
        StubUserMapper userMapper = new StubUserMapper();
        QrLoginServiceImpl service = new QrLoginServiceImpl(mapper, userMapper, jwtTokenService());
        QrLoginSession session = new QrLoginSession();
        session.setQrToken("token-2");
        session.setStatus(QrLoginServiceImpl.STATUS_PENDING);
        session.setExpireTime(LocalDateTime.now().minus(Duration.ofSeconds(5)));
        mapper.insertSeed(session);

        QrLoginSessionStatusVO result = service.getSessionStatus("token-2");

        Assert.assertEquals(QrLoginServiceImpl.STATUS_EXPIRED, result.getStatus());
        Assert.assertEquals(QrLoginServiceImpl.STATUS_EXPIRED, mapper.selectByQrToken("token-2").getStatus());
    }

    @Test
    public void getMobileSessionShouldMarkPendingAsScanned() {
        InMemoryQrLoginSessionMapper mapper = new InMemoryQrLoginSessionMapper();
        StubUserMapper userMapper = new StubUserMapper();
        QrLoginServiceImpl service = new QrLoginServiceImpl(mapper, userMapper, jwtTokenService());
        QrLoginSession session = new QrLoginSession();
        session.setQrToken("token-3");
        session.setStatus(QrLoginServiceImpl.STATUS_PENDING);
        session.setExpireTime(LocalDateTime.now().plusMinutes(1));
        mapper.insertSeed(session);

        QrLoginMobileSessionVO result = service.getMobileSession("token-3");

        Assert.assertEquals(QrLoginServiceImpl.STATUS_SCANNED, result.getStatus());
        Assert.assertEquals(QrLoginServiceImpl.STATUS_SCANNED, mapper.selectByQrToken("token-3").getStatus());
        Assert.assertNotNull(mapper.selectByQrToken("token-3").getScannedTime());
    }

    @Test
    public void confirmSessionShouldMarkConfirmedWhenApproved() {
        InMemoryQrLoginSessionMapper mapper = new InMemoryQrLoginSessionMapper();
        StubUserMapper userMapper = new StubUserMapper();
        userMapper.user = buildUser(99L, "scanner", "扫码人");
        QrLoginServiceImpl service = new QrLoginServiceImpl(mapper, userMapper, jwtTokenService());
        QrLoginSession session = new QrLoginSession();
        session.setQrToken("token-4");
        session.setStatus(QrLoginServiceImpl.STATUS_SCANNED);
        session.setExpireTime(LocalDateTime.now().plusMinutes(1));
        mapper.insertSeed(session);

        QrLoginConfirmResultVO result = service.confirmSession("token-4", 99L, true);

        Assert.assertTrue(result.isSuccess());
        Assert.assertEquals(QrLoginServiceImpl.STATUS_CONFIRMED, result.getStatus());
        Assert.assertEquals(QrLoginServiceImpl.STATUS_CONFIRMED, mapper.selectByQrToken("token-4").getStatus());
        Assert.assertEquals(Long.valueOf(99L), mapper.selectByQrToken("token-4").getUserId());
        Assert.assertNotNull(mapper.selectByQrToken("token-4").getConfirmedTime());
    }

    @Test
    public void confirmSessionShouldMarkCanceledWhenRejected() {
        InMemoryQrLoginSessionMapper mapper = new InMemoryQrLoginSessionMapper();
        StubUserMapper userMapper = new StubUserMapper();
        QrLoginServiceImpl service = new QrLoginServiceImpl(mapper, userMapper, jwtTokenService());
        QrLoginSession session = new QrLoginSession();
        session.setQrToken("token-5");
        session.setStatus(QrLoginServiceImpl.STATUS_PENDING);
        session.setExpireTime(LocalDateTime.now().plusMinutes(1));
        mapper.insertSeed(session);

        QrLoginConfirmResultVO result = service.confirmSession("token-5", 88L, false);

        Assert.assertTrue(result.isSuccess());
        Assert.assertEquals(QrLoginServiceImpl.STATUS_CANCELED, result.getStatus());
        Assert.assertEquals(QrLoginServiceImpl.STATUS_CANCELED, mapper.selectByQrToken("token-5").getStatus());
        Assert.assertNull(mapper.selectByQrToken("token-5").getUserId());
    }

    @Test
    public void confirmSessionShouldRejectExpiredSession() {
        InMemoryQrLoginSessionMapper mapper = new InMemoryQrLoginSessionMapper();
        StubUserMapper userMapper = new StubUserMapper();
        QrLoginServiceImpl service = new QrLoginServiceImpl(mapper, userMapper, jwtTokenService());
        QrLoginSession session = new QrLoginSession();
        session.setQrToken("token-6");
        session.setStatus(QrLoginServiceImpl.STATUS_SCANNED);
        session.setExpireTime(LocalDateTime.now().minusSeconds(5));
        mapper.insertSeed(session);

        try {
            service.confirmSession("token-6", 77L, true);
            Assert.fail("Expected expired session to be rejected");
        } catch (IllegalArgumentException ex) {
            Assert.assertEquals("二维码已过期，不能再确认", ex.getMessage());
            Assert.assertEquals(QrLoginServiceImpl.STATUS_EXPIRED, mapper.selectByQrToken("token-6").getStatus());
        }
    }

    @Test
    public void confirmSessionShouldRejectRepeatedConfirmation() {
        InMemoryQrLoginSessionMapper mapper = new InMemoryQrLoginSessionMapper();
        StubUserMapper userMapper = new StubUserMapper();
        QrLoginServiceImpl service = new QrLoginServiceImpl(mapper, userMapper, jwtTokenService());
        QrLoginSession session = new QrLoginSession();
        session.setQrToken("token-7");
        session.setStatus(QrLoginServiceImpl.STATUS_CONFIRMED);
        session.setUserId(66L);
        session.setExpireTime(LocalDateTime.now().plusMinutes(1));
        mapper.insertSeed(session);

        try {
            service.confirmSession("token-7", 66L, true);
            Assert.fail("Expected repeated confirmation to be rejected");
        } catch (IllegalArgumentException ex) {
            Assert.assertEquals("该二维码已确认，不能重复操作", ex.getMessage());
        }
    }

    @Test
    public void getSessionStatusShouldConsumeConfirmedSessionAndReturnLogin() {
        InMemoryQrLoginSessionMapper mapper = new InMemoryQrLoginSessionMapper();
        StubUserMapper userMapper = new StubUserMapper();
        UserEntity user = buildUser(12L, "qr-user", "二维码用户");
        user.setForcePasswordChange(Boolean.TRUE);
        userMapper.user = user;
        QrLoginServiceImpl service = new QrLoginServiceImpl(mapper, userMapper, jwtTokenService());
        QrLoginSession session = new QrLoginSession();
        session.setQrToken("token-8");
        session.setStatus(QrLoginServiceImpl.STATUS_CONFIRMED);
        session.setUserId(12L);
        session.setConfirmedTime(LocalDateTime.now());
        session.setExpireTime(LocalDateTime.now().plusMinutes(1));
        mapper.insertSeed(session);

        QrLoginSessionStatusVO result = service.getSessionStatus("token-8");

        Assert.assertEquals(QrLoginServiceImpl.STATUS_CONFIRMED, result.getStatus());
        Assert.assertNotNull(result.getLogin());
        Assert.assertEquals("qr-user", result.getLogin().getUsername());
        Assert.assertNotNull(result.getLogin().getToken());
        Assert.assertEquals(Boolean.TRUE, result.getLogin().getForcePasswordChange());
        Assert.assertEquals(QrLoginServiceImpl.STATUS_CONSUMED, mapper.selectByQrToken("token-8").getStatus());
        Assert.assertNotNull(mapper.selectByQrToken("token-8").getConsumedTime());
    }

    @Test
    public void getSessionStatusShouldNotReturnLoginAfterConsumed() {
        InMemoryQrLoginSessionMapper mapper = new InMemoryQrLoginSessionMapper();
        StubUserMapper userMapper = new StubUserMapper();
        userMapper.user = buildUser(12L, "qr-user", "二维码用户");
        QrLoginServiceImpl service = new QrLoginServiceImpl(mapper, userMapper, jwtTokenService());
        QrLoginSession session = new QrLoginSession();
        session.setQrToken("token-9");
        session.setStatus(QrLoginServiceImpl.STATUS_CONSUMED);
        session.setUserId(12L);
        session.setConsumedTime(LocalDateTime.now());
        session.setExpireTime(LocalDateTime.now().plusMinutes(1));
        mapper.insertSeed(session);

        QrLoginSessionStatusVO result = service.getSessionStatus("token-9");

        Assert.assertEquals(QrLoginServiceImpl.STATUS_CONSUMED, result.getStatus());
        Assert.assertNull(result.getLogin());
    }

    private static JwtTokenService jwtTokenService() {
        return new JwtTokenService("change-this-secret-in-production", 7200);
    }

    private static UserEntity buildUser(Long id, String username, String realName) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setUsername(username);
        user.setRealName(realName);
        user.setRole("USER");
        user.setStatus(1);
        return user;
    }

    private static class InMemoryQrLoginSessionMapper implements QrLoginSessionMapper {
        private final Map<String, QrLoginSession> sessions = new HashMap<>();
        private long sequence = 1L;

        @Override
        public int insertSession(QrLoginSession session) {
            session.setId(sequence++);
            session.setStatus(QrLoginServiceImpl.STATUS_PENDING);
            session.setCreatedTime(LocalDateTime.now());
            session.setUpdatedTime(LocalDateTime.now());
            sessions.put(session.getQrToken(), cloneSession(session));
            return 1;
        }

        @Override
        public QrLoginSession selectByQrToken(String qrToken) {
            QrLoginSession session = sessions.get(qrToken);
            return session == null ? null : cloneSession(session);
        }

        @Override
        public int updateStatus(String qrToken, String status) {
            QrLoginSession session = sessions.get(qrToken);
            if (session == null) {
                return 0;
            }
            session.setStatus(status);
            session.setUpdatedTime(LocalDateTime.now());
            return 1;
        }

        @Override
        public int markScanned(String qrToken, LocalDateTime scannedTime) {
            QrLoginSession session = sessions.get(qrToken);
            if (session == null) {
                return 0;
            }
            session.setStatus(QrLoginServiceImpl.STATUS_SCANNED);
            if (session.getScannedTime() == null) {
                session.setScannedTime(scannedTime);
            }
            session.setUpdatedTime(LocalDateTime.now());
            return 1;
        }

        @Override
        public int confirmSession(String qrToken, Long userId, String status, LocalDateTime confirmedTime) {
            QrLoginSession session = sessions.get(qrToken);
            if (session == null) {
                return 0;
            }
            session.setUserId(userId);
            session.setStatus(status);
            session.setConfirmedTime(confirmedTime);
            session.setUpdatedTime(LocalDateTime.now());
            return 1;
        }

        @Override
        public int consumeConfirmedSession(String qrToken, LocalDateTime consumedTime) {
            QrLoginSession session = sessions.get(qrToken);
            if (session == null || session.getConsumedTime() != null || !QrLoginServiceImpl.STATUS_CONFIRMED.equals(session.getStatus())) {
                return 0;
            }
            session.setStatus(QrLoginServiceImpl.STATUS_CONSUMED);
            session.setConsumedTime(consumedTime);
            session.setUpdatedTime(LocalDateTime.now());
            return 1;
        }

        @Override
        public int deleteExpiredSessions() {
            int before = sessions.size();
            sessions.entrySet().removeIf(entry -> entry.getValue().getExpireTime() != null && entry.getValue().getExpireTime().isBefore(LocalDateTime.now()));
            return before - sessions.size();
        }

        void insertSeed(QrLoginSession session) {
            if (session.getId() == null) {
                session.setId(sequence++);
            }
            sessions.put(session.getQrToken(), cloneSession(session));
        }

        private QrLoginSession cloneSession(QrLoginSession source) {
            QrLoginSession target = new QrLoginSession();
            target.setId(source.getId());
            target.setQrToken(source.getQrToken());
            target.setUserId(source.getUserId());
            target.setStatus(source.getStatus());
            target.setExpireTime(source.getExpireTime());
            target.setScannedTime(source.getScannedTime());
            target.setConfirmedTime(source.getConfirmedTime());
            target.setConsumedTime(source.getConsumedTime());
            target.setCreatedTime(source.getCreatedTime());
            target.setUpdatedTime(source.getUpdatedTime());
            return target;
        }
    }

    private static class StubUserMapper implements UserMapper {
        private UserEntity user;

        @Override
        public UserEntity findById(Long id) {
            return user != null && id.equals(user.getId()) ? user : null;
        }

        @Override
        public UserEntity findByUsername(String username) { return null; }
        @Override
        public UserEntity findByWechatOpenId(String wechatOpenId) { return null; }
        @Override
        public UserEntity findByWechatUnionId(String wechatUnionId) { return null; }
        @Override
        public long countPageByUserId(Long userId, UserQueryRequest request) { return 0; }
        @Override
        public List<UserListItemVO> queryPageByUserId(Long userId, UserQueryRequest request) { return List.of(); }
        @Override
        public long countPageByTreePath(String treePathPrefix, UserQueryRequest request) { return 0; }
        @Override
        public List<UserListItemVO> queryPageByTreePath(String treePathPrefix, UserQueryRequest request) { return List.of(); }
        @Override
        public int insertUser(UserEntity entity) { return 0; }
        @Override
        public long countPage(UserQueryRequest request) { return 0; }
        @Override
        public List<UserListItemVO> queryPage(UserQueryRequest request) { return List.of(); }
        @Override
        public UserDetailVO detailById(Long id) { return null; }
        @Override
        public UserDetailVO detailByIdAndTreePath(Long id, String treePathPrefix) { return null; }
        @Override
        public int updateUser(UserEntity entity) { return 0; }
        @Override
        public int updateWechatBinding(Long id, String wechatOpenId, String wechatUnionId, String updateUser, LocalDateTime updateTime) { return 0; }
        @Override
        public int logicalDelete(Long id, String updateUser, LocalDateTime updateTime) { return 0; }
        @Override
        public int updatePassword(Long id, String passwordHash, String passwordAlgo, String passwordSalt, Boolean forcePasswordChange, String updateUser, LocalDateTime updateTime) { return 0; }
        @Override
        public int updateLoginSecurityState(Long id, Integer loginFailCount, LocalDateTime lockUntil, String updateUser, LocalDateTime updateTime) { return 0; }
    }
}
