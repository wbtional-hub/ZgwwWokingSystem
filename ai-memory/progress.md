# 2026-04-06

## 公众号 H5 授权登录最小闭环
- 继续复用现有 `POST /api/auth/wechat-mp-login` 作为 `code -> JWT` 的核心入口。
- 新增 `GET /api/auth/wechat-mp-authorize-url` 和 `GET /api/auth/wechat-mp-callback`，由后端统一生成授权地址并处理回调跳转。
- 新增统一配置 `wechat.mp`、`wechat.mini`，未配置真实参数时系统仍可正常启动。
- 前端登录页只负责微信浏览器检测、调用后端授权地址接口、消费回调结果，不再持有公众号参数。
- 小程序继续只保留配置位、服务骨架和未启用提示，不在本轮启用真实调用。

## 真机测试收口
- 本轮只收口“部署后可直接用手机微信打开 `https://www.xmzgww.com/login` 测试公众号 H5 登录”，不改账号密码登录主链路。
- 后端配置已改为仅后端读取并支持环境变量覆盖：`WECHAT_MP_ENABLED`、`WECHAT_MP_APP_ID`、`WECHAT_MP_APP_SECRET`、`WECHAT_MP_OAUTH_REDIRECT_URI`、`WECHAT_MP_AUTHORIZE_SCOPE`。
- 新增 `application-prod.yml` 生产配置示例，生产默认可开启公众号 H5 授权；本地默认仍保持关闭，避免破坏 localhost / 127.0.0.1 开发。
- 回调失败时统一回落到 `/login`，并带上 `wechatAuthFailed`、`wechatAuthCode`、`wechatAuthMessage`，便于区分“未绑定系统账号”“配置缺失”“code 无效”等分支。
- 绑定逻辑继续复用 `sys_user.wechat_open_id / wechat_union_id`，已绑定用户直登，未绑定用户给出明确提示，不白屏、不 500。
## 2026-04-06 Change Password Minimal Loop
- Confirmed first that the project had no self-service change-password flow for the current logged-in user.
- Reused the existing `/profile` page as the smallest safe entry instead of changing the header/menu system.
- Confirmed storage and auth consistency: table `sys_user`, password field `password_hash`, password algorithm `BCryptPasswordEncoder`.
- Added backend endpoint `POST /api/auth/change-password` with current-user-only validation, old-password check, confirm-password check, and same-password rejection.
- Added frontend popup form in `frontend/src/views/profile/ProfileView.vue` and success handling that clears local login state and redirects to `/login`.
- Verified with `mvn -q -DskipTests compile`, `mvn -q -Dtest=AuthServiceImplTest test`, and `npm.cmd run build`.
- Final closeout: added frontend trim-on-submit, explicit duplicate-submit disable, popup close guard during submit, and frontend same-password prompt.
- Final closeout: supplemented backend test coverage for wrong old password and same old/new password, then reran compile, `AuthServiceImplTest`, and frontend build successfully.

## 2026-04-06 Force Password Change After Admin Reset
- Added the database script `database/20260406_force_password_change.sql` to persist `sys_user.force_password_change` with default `false`.
- Completed the backend flag loop with the smallest possible scope: admin reset now writes `force_password_change=true`, self-service password change writes `force_password_change=false`, and login responses now carry `forcePasswordChange` in `LoginVO`.
- Kept the existing JWT generation, BCrypt/SM3 compatibility, and WeChat login chain intact; only the login response wrapper now returns message `FORCE_PASSWORD_CHANGE` when the flag is set.
- Added frontend forced-change handling without touching the auth chain: login stores the temporary login state, routes are restricted to `/profile`, and the password popup auto-opens until the user changes the password.
- Re-verified with `mvn -q -DskipTests compile`, `mvn -q "-Dtest=AuthServiceImplTest,UserServiceImplTest" test`, `mvn -q "-Dtest=SecurityConfigTest,WechatMpAuthServiceImplTest,WechatMiniAuthServiceImplTest,AuthServiceImplTest" test`, and `npm.cmd run build`.

## 2026-04-06 QR Login Phase 1
- Added the first-stage QR login session migration at `backend/src/main/resources/db/migration/20260406_create_qr_login_session.sql`.
- Added new QR session entity, mapper, service, controller, and MyBatis XML without touching the existing password login flow, JWT generation, JWT filter, or WeChat login flow.
- Implemented anonymous `POST /api/auth/qr-login/session` to create a 2-minute QR session and `GET /api/auth/qr-login/status` to query session status with automatic expired-session fallback.
- Added frontend API wrappers only in `frontend/src/api/auth.js`; this round does not include PC QR UI or mobile confirmation logic.
- Verified with `mvn -q -DskipTests compile`, `mvn -q "-Dmaven.repo.local=D:\\20.develop64\\ZgwwWokingSystem\\backend\\.m2" "-Dtest=QrLoginServiceTest,AuthServiceImplTest,SecurityConfigTest,WechatMpAuthServiceImplTest,WechatMiniAuthServiceImplTest" test`, and `npm.cmd run build`.

## 2026-04-06 QR Login Phase 2
- Extended the existing QR login session service/controller with mobile-side confirmation only, without touching `AuthServiceImpl`, JWT generation/filtering, or WeChat login flow.
- Added authenticated `GET /api/auth/qr-login/mobile-session` to load session info and promote `PENDING -> SCANNED` on first mobile access.
- Added authenticated `POST /api/auth/qr-login/confirm` so a logged-in mobile user can confirm or reject the QR session, with strict status validation and expired-session protection.
- Added the minimal mobile confirmation page at `frontend/src/views/mobile/MobileQrConfirmView.vue` and a public route `/mobile/qr-confirm`; if the phone is not logged in, the page only prompts the user to log in first and does not attempt a complex return chain.
- Re-verified with `mvn -q -DskipTests compile`, `mvn -q "-Dmaven.repo.local=D:\\20.develop64\\ZgwwWokingSystem\\backend\\.m2" "-Dtest=QrLoginServiceTest,AuthServiceImplTest,SecurityConfigTest,WechatMpAuthServiceImplTest,WechatMiniAuthServiceImplTest" test`, and `npm.cmd run build`.

## 2026-04-06 QR Login Phase 3
- Extended `GET /api/auth/qr-login/status` so a confirmed QR session can be consumed exactly once on the PC side, returning `LoginVO` plus an existing JWT without touching `AuthServiceImpl`, `JwtTokenService`, or the password login chain.
- Added the missing `frontend/src/views/auth/LoginView.vue` and kept the existing password-login and WeChat-entry behavior while introducing a desktop-only QR login area.
- The new QR area can create a session, render the QR image, poll status every 2 seconds, stop on `EXPIRED/CANCELED/CONSUMED`, and automatically apply the returned login result when the session reaches `CONFIRMED`.
- Verified this round with backend compile, targeted QR/auth/security/wechat tests, and frontend production build.
