<template>
  <section class="login-page">
    <div class="login-shell">
      <div class="brand-panel">
        <div class="brand-chip">组工万维</div>
        <p class="brand-eyebrow">Smart Office Automation</p>
        <h1>智慧OA系统</h1>
        <p class="brand-description">
          统一办公入口，支持账号登录、微信登录与扫码登录，
          提供安全、便捷、高效的日常协同体验。
        </p>

        <div class="brand-points">
          <div class="brand-point">
            <strong>统一入口</strong>
            <span>账号密码、微信授权与扫码确认在同一入口完成接入。</span>
          </div>
          <div class="brand-point">
            <strong>协同高效</strong>
            <span>面向单位内部日常办公场景，减少重复操作与切换成本。</span>
          </div>
          <div class="brand-point">
            <strong>安全稳妥</strong>
            <span>沿用现有认证能力，兼顾日常使用效率与登录安全边界。</span>
          </div>
        </div>
      </div>

      <div class="login-column">
        <div class="login-card">
          <div class="card-header">
            <div class="card-kicker">Secure Access</div>
            <h2>欢迎登录</h2>
            <p>请输入账号密码，或使用已登录的手机端扫码确认登录。</p>
          </div>

          <div v-if="state.wechatError" class="info-banner warning">{{ state.wechatError }}</div>
          <div v-if="state.qrError" class="info-banner danger">{{ state.qrError }}</div>
          <div v-if="showMobilePasswordDisabledNotice" class="info-banner warning">
            当前环境手机端未开启账号密码登录，请在微信内打开后使用微信登录。
          </div>

          <van-form v-if="showPasswordLoginForm" class="login-form" @submit="handleSubmit">
            <van-field
              v-model="state.username"
              class="login-field"
              name="username"
              label="用户名"
              placeholder="请输入用户名"
              autocomplete="username"
            />
            <van-field
              v-model="state.password"
              class="login-field"
              name="password"
              type="password"
              label="密码"
              placeholder="请输入密码"
              autocomplete="current-password"
            />
            <div class="form-actions">
              <van-button
                block
                class="login-button primary-login-button"
                type="primary"
                native-type="submit"
                :loading="state.submitting"
                :disabled="state.submitting || state.processingLogin"
              >
                账号密码登录
              </van-button>
            </div>
          </van-form>

          <div v-if="showWechatEntry" class="wechat-entry">
            <van-button
              block
              plain
              class="login-button wechat-login-button"
              type="success"
              :loading="state.wechatLoading"
              :disabled="state.wechatLoading || state.processingLogin"
              @click="startWechatOauth"
            >
              微信内打开时，使用公众号授权登录
            </van-button>
          </div>

          <div v-if="showQrEntry" class="qr-section">
            <div class="section-divider">
              <span>扫码登录</span>
            </div>

            <div class="qr-panel">
              <div class="qr-panel-head">
                <div class="qr-title-group">
                  <h3>企业级扫码确认</h3>
                  <p>生成二维码后，使用手机端扫码确认，PC 将自动完成登录。</p>
                </div>
                <van-button
                  size="small"
                  plain
                  class="refresh-button"
                  type="primary"
                  :loading="state.qrLoading"
                  :disabled="state.qrLoading || state.processingLogin"
                  @click="openQrLogin"
                >
                  {{ state.qrToken ? '刷新二维码' : '生成二维码' }}
                </van-button>
              </div>

              <div v-if="state.qrToken" class="qr-body">
                <div class="qr-visual">
                  <div class="qr-frame">
                    <canvas ref="qrCanvas" class="qr-canvas" />
                  </div>
                  <div class="qr-caption">请使用已登录的手机端扫一扫</div>
                </div>

                <div class="qr-meta">
                  <div class="meta-row">
                    <span>当前状态</span>
                    <strong>{{ qrStatusText }}</strong>
                  </div>
                  <div class="meta-row">
                    <span>剩余时间</span>
                    <strong>{{ qrCountdownText }}</strong>
                  </div>
                  <div class="meta-row">
                    <span>确认入口</span>
                    <a :href="qrFullUrl" target="_blank" rel="noopener noreferrer">打开手机确认页</a>
                  </div>
                </div>
              </div>

              <p v-else class="qr-placeholder">
                点击“生成二维码”后，将创建一个 2 分钟有效的扫码登录会话。
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import QRCode from 'qrcode'
import { useRoute, useRouter } from 'vue-router'
import { showFailToast, showLoadingToast, showSuccessToast } from 'vant'
import {
  createQrSession,
  getQrStatus,
  loginApi,
  queryMobileLoginOptionsApi,
  queryWechatMpAuthorizeUrlApi
} from '@/api/auth'
import { useUserStore } from '@/stores/user'
import { isMobileClient } from '@/utils/device'
import { isTestIpLoginEnv } from '@/utils/runtime-origin'

const WECHAT_HASH_KEY = 'wechatLogin'
const WECHAT_FAILURE_FLAG = 'wechatAuthFailed'
// FIX: wechat login recovery protection
const WECHAT_LOGIN_RECOVERING_KEY = 'wechat_login_recovering'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const qrCanvas = ref(null)

const state = reactive({
  username: '',
  password: '',
  mobilePasswordLoginEnabled: false,
  submitting: false,
  processingLogin: false,
  wechatLoading: false,
  wechatError: '',
  qrLoading: false,
  qrToken: '',
  qrUrl: '',
  qrStatus: '',
  qrExpireAt: '',
  qrCountdownSeconds: 0,
  qrError: ''
})

let qrPollTimer = null
let qrCountdownTimer = null
let qrLoginLock = false

const mobileClient = computed(() => isMobileClient())
const showQrEntry = computed(() => !mobileClient.value)
const showWechatEntry = computed(() => /MicroMessenger/i.test(resolveUserAgent()))
const showPasswordLoginForm = computed(() => !mobileClient.value || state.mobilePasswordLoginEnabled)
const showMobilePasswordDisabledNotice = computed(() => mobileClient.value && !state.mobilePasswordLoginEnabled)

const qrFullUrl = computed(() => {
  if (!state.qrUrl) {
    return ''
  }
  if (/^https?:\/\//i.test(state.qrUrl)) {
    return state.qrUrl
  }
  if (typeof window === 'undefined') {
    return state.qrUrl
  }
  return `${window.location.origin}${state.qrUrl}`
})

const qrCountdownText = computed(() => {
  if (!state.qrToken || !state.qrExpireAt) {
    return '-'
  }
  if (state.qrCountdownSeconds <= 0) {
    return '已过期'
  }
  const minutes = Math.floor(state.qrCountdownSeconds / 60)
  const seconds = state.qrCountdownSeconds % 60
  return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
})

const qrStatusText = computed(() => {
  const statusMap = {
    PENDING: '等待扫码',
    SCANNED: '已扫码，等待手机确认',
    CONFIRMED: '已确认，正在登录',
    CONSUMED: '已被消费',
    CANCELED: '已拒绝',
    EXPIRED: '已过期'
  }
  return statusMap[state.qrStatus] || '待生成'
})

onMounted(async () => {
  await loadMobileLoginOptions()
  await tryConsumeWechatLoginCallback()
  consumeWechatFailureNotice()
})

onBeforeUnmount(() => {
  stopQrPolling()
})

async function handleSubmit() {
  if (!showPasswordLoginForm.value) {
    showFailToast('当前环境手机端未开启账号密码登录，请使用微信登录')
    return
  }
  const username = state.username.trim()
  const password = state.password.trim()
  if (!username) {
    showFailToast('请输入用户名')
    return
  }
  if (!password) {
    showFailToast('请输入密码')
    return
  }

  state.submitting = true
  try {
    const response = await loginApi({ username, password })
    const loginInfo = ensureSuccess(response, '登录失败')
    await applyLoginResult(loginInfo, response?.message, resolvePostLoginPath())
  } catch (error) {
    showFailToast(resolveErrorMessage(error, '登录失败，请稍后重试'))
  } finally {
    state.submitting = false
  }
}

async function loadMobileLoginOptions() {
  if (isTestIpLoginEnv()) {
    state.mobilePasswordLoginEnabled = true
    return
  }
  if (!mobileClient.value) {
    state.mobilePasswordLoginEnabled = true
    return
  }
  try {
    const response = await queryMobileLoginOptionsApi()
    const data = ensureSuccess(response, '获取手机端登录配置失败')
    state.mobilePasswordLoginEnabled = data?.passwordLoginEnabled === true
  } catch (error) {
    state.mobilePasswordLoginEnabled = false
  }
}

async function startWechatOauth() {
  state.wechatLoading = true
  try {
    const response = await queryWechatMpAuthorizeUrlApi({
      returnUrl: resolvePostLoginPath()
    })
    const data = ensureSuccess(response, '获取微信授权地址失败')
    if (!data?.authorizeUrl) {
      throw new Error('微信授权地址为空')
    }
    if (typeof window !== 'undefined') {
      const loading = showLoadingToast({
        message: '正在跳转微信授权...',
        forbidClick: true,
        duration: 0
      })
      setTimeout(() => loading.close(), 300)
      window.location.href = data.authorizeUrl
    }
  } catch (error) {
    const message = resolveErrorMessage(error, '微信登录暂时不可用')
    state.wechatError = message
    showFailToast(message)
  } finally {
    state.wechatLoading = false
  }
}

async function openQrLogin() {
  state.qrError = ''
  await createAndStartQrSession()
}

async function createAndStartQrSession() {
  state.qrLoading = true
  stopQrPolling()
  try {
    const response = await createQrSession()
    const data = ensureSuccess(response, '生成二维码失败')
    state.qrToken = data.qrToken || ''
    state.qrUrl = data.qrUrl || ''
    state.qrExpireAt = data.expireAt || ''
    state.qrStatus = 'PENDING'
    await nextTick()
    await renderQrCode()
    updateQrCountdown()
    startQrPolling()
  } catch (error) {
    state.qrError = resolveErrorMessage(error, '生成二维码失败')
    showFailToast(state.qrError)
  } finally {
    state.qrLoading = false
  }
}

async function renderQrCode() {
  if (!qrCanvas.value || !qrFullUrl.value) {
    return
  }
  try {
    await QRCode.toCanvas(qrCanvas.value, qrFullUrl.value, {
      width: 220,
      margin: 2
    })
  } catch (err) {
    console.error('二维码生成失败', err)
    state.qrError = '二维码生成失败，请刷新重试。'
    showFailToast(state.qrError)
  }
}

function startQrPolling() {
  stopQrPolling()
  qrCountdownTimer = window.setInterval(updateQrCountdown, 1000)
  qrPollTimer = window.setInterval(() => {
    void pollQrStatus()
  }, 2000)
}

function stopQrPolling() {
  if (qrPollTimer) {
    window.clearInterval(qrPollTimer)
    qrPollTimer = null
  }
  if (qrCountdownTimer) {
    window.clearInterval(qrCountdownTimer)
    qrCountdownTimer = null
  }
}

async function pollQrStatus() {
  if (!state.qrToken || qrLoginLock) {
    return
  }
  try {
    const response = await getQrStatus(state.qrToken)
    const data = ensureSuccess(response, '二维码状态查询失败')
    const status = data?.status || ''
    state.qrStatus = status

    if (status === 'CONFIRMED' && data?.login) {
      stopQrPolling()
      qrLoginLock = true
      await consumeQrLogin(data.login)
      return
    }

    if (status === 'EXPIRED') {
      handleQrExpired()
      return
    }

    if (status === 'CONSUMED') {
      stopQrPolling()
      state.qrError = '二维码已被使用，请重新生成。'
      showFailToast(state.qrError)
      return
    }

    if (status === 'CANCELED') {
      stopQrPolling()
      state.qrError = '本次扫码登录已被拒绝。'
      showFailToast(state.qrError)
    }
  } catch (error) {
    stopQrPolling()
    state.qrError = resolveErrorMessage(error, '二维码状态查询失败')
    showFailToast(state.qrError)
  }
}

async function consumeQrLogin(loginInfo) {
  try {
    await applyLoginResult(
      loginInfo,
      loginInfo.forcePasswordChange ? 'FORCE_PASSWORD_CHANGE' : 'success',
      resolvePostLoginPath()
    )
    showSuccessToast('扫码登录成功')
  } catch (error) {
    state.qrError = resolveErrorMessage(error, '扫码登录失败')
    showFailToast(state.qrError)
  } finally {
    qrLoginLock = false
  }
}

function handleQrExpired() {
  stopQrPolling()
  state.qrStatus = 'EXPIRED'
  state.qrCountdownSeconds = 0
  state.qrError = '二维码已过期，请重新生成。'
  showFailToast(state.qrError)
}

function updateQrCountdown() {
  if (!state.qrExpireAt) {
    state.qrCountdownSeconds = 0
    return
  }
  const expireAt = new Date(state.qrExpireAt).getTime()
  const diff = Math.max(0, Math.floor((expireAt - Date.now()) / 1000))
  state.qrCountdownSeconds = diff
  if (diff <= 0 && state.qrToken && state.qrStatus !== 'EXPIRED') {
    handleQrExpired()
  }
}

async function tryConsumeWechatLoginCallback() {
  const encodedPayload = extractWechatPayload()
  if (!encodedPayload) {
    return
  }
  state.wechatLoading = true
  try {
    const payload = decodeWechatPayload(encodedPayload)
    if (!payload?.loginInfo?.token) {
      throw new Error('微信登录结果无效')
    }
    clearWechatHash()
    await applyLoginResult(
      payload.loginInfo,
      payload.loginInfo.forcePasswordChange ? 'FORCE_PASSWORD_CHANGE' : 'success',
      payload.returnUrl || resolvePostLoginPath(),
      { wechatRecoveryProtection: true }
    )
    showSuccessToast('微信登录成功')
  } catch (error) {
    const message = resolveErrorMessage(error, '微信登录结果处理失败')
    state.wechatError = message
    showFailToast(message)
  } finally {
    state.wechatLoading = false
  }
}

function consumeWechatFailureNotice() {
  if (route.query[WECHAT_FAILURE_FLAG] !== '1') {
    return
  }
  const message = typeof route.query.wechatAuthMessage === 'string'
    ? route.query.wechatAuthMessage
    : '微信登录失败，请稍后重试'
  state.wechatError = message
  showFailToast(message)
}

async function applyLoginResult(loginInfo, message, targetPath, options = {}) {
  if (!loginInfo?.token) {
    throw new Error('登录结果缺少 token')
  }
  if (state.processingLogin) {
    return
  }
  const wechatRecoveryProtection = Boolean(options.wechatRecoveryProtection)
  state.processingLogin = true
  try {
    clearExistingLoginState()
    userStore.setLogin(loginInfo)
    // FIX: wechat login recovery protection
    if (wechatRecoveryProtection) {
      setWechatLoginRecoveryFlag()
    }
    if (message === 'FORCE_PASSWORD_CHANGE' || loginInfo.forcePasswordChange) {
      await router.replace('/profile?forcePasswordChange=1')
      // FIX: wechat login recovery protection
      if (wechatRecoveryProtection) {
        await router.isReady()
        clearWechatLoginRecoveryFlag()
      }
      return
    }
    await router.replace(targetPath || '/')
    // FIX: wechat login recovery protection
    if (wechatRecoveryProtection) {
      await router.isReady()
      clearWechatLoginRecoveryFlag()
    }
  } catch (error) {
    // FIX: wechat login recovery protection
    if (wechatRecoveryProtection) {
      clearWechatLoginRecoveryFlag()
    }
    throw error
  } finally {
    state.processingLogin = false
  }
}

function clearExistingLoginState() {
  stopQrPolling()
  userStore.clearLogin()
}

// FIX: wechat login recovery protection
function setWechatLoginRecoveryFlag() {
  if (typeof window === 'undefined') {
    return
  }
  try {
    window.sessionStorage.setItem(WECHAT_LOGIN_RECOVERING_KEY, '1')
  } catch (error) {
    // ignore sessionStorage access errors
  }
}

// FIX: wechat login recovery protection
function clearWechatLoginRecoveryFlag() {
  if (typeof window === 'undefined') {
    return
  }
  try {
    window.sessionStorage.removeItem(WECHAT_LOGIN_RECOVERING_KEY)
  } catch (error) {
    // ignore sessionStorage access errors
  }
}

function resolvePostLoginPath() {
  const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : ''
  if (redirect && redirect !== '/login' && redirect.startsWith('/')) {
    return redirect
  }
  return '/'
}

function extractWechatPayload() {
  if (typeof window === 'undefined' || !window.location.hash) {
    return ''
  }
  const hash = window.location.hash.startsWith('#')
    ? window.location.hash.slice(1)
    : window.location.hash
  return new URLSearchParams(hash).get(WECHAT_HASH_KEY) || ''
}

function clearWechatHash() {
  if (typeof window === 'undefined') {
    return
  }
  const url = `${window.location.pathname}${window.location.search}`
  window.history.replaceState({}, document.title, url)
}

function decodeWechatPayload(encodedPayload) {
  const normalized = encodedPayload.replace(/-/g, '+').replace(/_/g, '/')
  const padding = normalized.length % 4 === 0 ? '' : '='.repeat(4 - (normalized.length % 4))
  const decoded = atob(normalized + padding)
  return JSON.parse(decoded)
}

function ensureSuccess(response, fallbackMessage) {
  if (!response || response.code !== 0) {
    throw new Error(response?.message || fallbackMessage)
  }
  return response.data
}

function resolveErrorMessage(error, fallbackMessage) {
  return error?.response?.data?.message || error?.message || fallbackMessage
}

function resolveUserAgent() {
  if (typeof navigator === 'undefined') {
    return ''
  }
  return navigator.userAgent || ''
}
</script>

<style scoped>
.login-page {
  position: relative;
  min-height: 100vh;
  overflow: hidden;
  padding: 36px 22px;
  background:
    radial-gradient(circle at 14% 20%, rgba(91, 141, 239, 0.2), transparent 28%),
    radial-gradient(circle at 88% 18%, rgba(255, 255, 255, 0.78), transparent 22%),
    radial-gradient(circle at 72% 82%, rgba(73, 124, 232, 0.12), transparent 26%),
    linear-gradient(135deg, #f7fbff 0%, #eef3fb 48%, #e8eef7 100%);
}

.login-page::before,
.login-page::after {
  content: '';
  position: absolute;
  border-radius: 999px;
  pointer-events: none;
  filter: blur(18px);
}

.login-page::before {
  top: 76px;
  left: 12%;
  width: 220px;
  height: 220px;
  background: rgba(141, 189, 255, 0.26);
}

.login-page::after {
  right: 10%;
  bottom: 64px;
  width: 260px;
  height: 260px;
  background: rgba(216, 233, 255, 0.56);
}

.login-shell {
  position: relative;
  z-index: 1;
  max-width: 1260px;
  min-height: calc(100vh - 72px);
  margin: 0 auto;
  display: grid;
  grid-template-columns: minmax(0, 1.08fr) minmax(420px, 0.92fr);
  gap: 64px;
  align-items: center;
}

.brand-panel {
  max-width: 560px;
  padding: 24px 8px 24px 4px;
}

.brand-chip {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 7px 14px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.66);
  border: 1px solid rgba(151, 174, 212, 0.34);
  box-shadow: 0 10px 28px rgba(77, 102, 146, 0.08);
  color: #4d648c;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.12em;
}

.brand-eyebrow {
  margin: 26px 0 10px;
  color: #6f85ab;
  font-size: 13px;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.brand-panel h1 {
  margin: 0;
  color: #163250;
  font-size: clamp(40px, 4.8vw, 64px);
  line-height: 1.02;
  font-weight: 700;
  letter-spacing: -0.03em;
}

.brand-description {
  max-width: 500px;
  margin: 22px 0 0;
  color: #536985;
  font-size: 16px;
  line-height: 1.9;
}

.brand-points {
  margin-top: 34px;
  display: grid;
  gap: 16px;
}

.brand-point {
  padding-left: 18px;
  border-left: 1px solid rgba(112, 140, 191, 0.22);
}

.brand-point strong {
  display: block;
  margin-bottom: 6px;
  color: #1c3856;
  font-size: 15px;
  font-weight: 700;
}

.brand-point span {
  color: #6b7f98;
  font-size: 14px;
  line-height: 1.8;
}

.login-column {
  display: flex;
  justify-content: flex-end;
}

.login-card {
  width: min(100%, 488px);
  padding: 28px 28px 26px;
  border-radius: 30px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(177, 194, 221, 0.34);
  box-shadow:
    0 24px 70px rgba(57, 79, 116, 0.14),
    inset 0 1px 0 rgba(255, 255, 255, 0.78);
  backdrop-filter: blur(18px);
}

.card-header {
  margin-bottom: 18px;
}

.card-kicker {
  display: inline-flex;
  align-items: center;
  padding: 5px 11px;
  border-radius: 999px;
  background: rgba(36, 84, 196, 0.08);
  color: #43608f;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.card-header h2 {
  margin: 16px 0 8px;
  color: #163250;
  font-size: 30px;
  font-weight: 700;
  letter-spacing: -0.02em;
}

.card-header p {
  margin: 0;
  color: #6b7f98;
  line-height: 1.75;
  font-size: 14px;
}

.info-banner {
  margin-top: 14px;
  padding: 12px 14px;
  border-radius: 16px;
  font-size: 13px;
  line-height: 1.7;
}

.info-banner.warning {
  background: rgba(255, 245, 236, 0.88);
  border: 1px solid rgba(247, 194, 133, 0.62);
  color: #a75417;
}

.info-banner.danger {
  background: rgba(254, 241, 241, 0.92);
  border: 1px solid rgba(244, 167, 167, 0.68);
  color: #b53a3a;
}

.login-form {
  margin-top: 18px;
}

.login-field {
  margin-bottom: 14px;
  border-radius: 18px;
  background: rgba(247, 250, 254, 0.9);
  border: 1px solid rgba(194, 208, 230, 0.72);
  transition: border-color 0.22s ease, box-shadow 0.22s ease, transform 0.22s ease;
}

.login-field:hover {
  border-color: rgba(142, 167, 208, 0.95);
}

.login-field:focus-within {
  border-color: rgba(73, 118, 216, 0.78);
  box-shadow: 0 0 0 4px rgba(79, 131, 234, 0.08);
  transform: translateY(-1px);
}

.login-field :deep(.van-cell) {
  background: transparent;
}

.login-field :deep(.van-field__label) {
  width: 58px;
  color: #49607e;
  font-size: 13px;
  font-weight: 600;
}

.login-field :deep(.van-field__control) {
  color: #1b3352;
  font-size: 14px;
}

.login-field :deep(.van-field__control::placeholder) {
  color: #9fb0c7;
}

.form-actions {
  margin-top: 16px;
}

.login-button {
  height: 46px;
  border-radius: 16px;
  font-size: 15px;
  font-weight: 600;
  transition: transform 0.22s ease, box-shadow 0.22s ease, filter 0.22s ease;
}

.login-button:hover {
  transform: translateY(-1px);
}

.login-button:active {
  transform: translateY(0);
}

.primary-login-button {
  border: none;
  background: linear-gradient(135deg, #2f63d6 0%, #254eb2 100%);
  box-shadow: 0 14px 32px rgba(44, 88, 183, 0.24);
}

.wechat-entry {
  margin-top: 14px;
}

.wechat-login-button {
  border-radius: 16px;
  border-color: rgba(139, 196, 159, 0.7);
  color: #2b8a57;
  background: rgba(247, 253, 249, 0.9);
}

.section-divider {
  position: relative;
  margin: 22px 0 18px;
  text-align: center;
}

.section-divider::before {
  content: '';
  position: absolute;
  top: 50%;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(90deg, rgba(214, 224, 240, 0), rgba(214, 224, 240, 0.95), rgba(214, 224, 240, 0));
}

.section-divider span {
  position: relative;
  z-index: 1;
  display: inline-flex;
  padding: 0 14px;
  background: rgba(255, 255, 255, 0.72);
  color: #7b8ca7;
  font-size: 12px;
  letter-spacing: 0.12em;
}

.qr-panel {
  padding: 18px;
  border-radius: 24px;
  background:
    linear-gradient(180deg, rgba(250, 252, 255, 0.92) 0%, rgba(243, 247, 252, 0.92) 100%);
  border: 1px solid rgba(197, 211, 233, 0.76);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.8);
}

.qr-panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
}

.qr-title-group h3 {
  margin: 0;
  color: #173252;
  font-size: 20px;
  font-weight: 700;
}

.qr-title-group p {
  margin: 8px 0 0;
  color: #6d809a;
  font-size: 14px;
  line-height: 1.75;
}

.refresh-button {
  border-radius: 14px;
  border-color: rgba(126, 155, 206, 0.72);
  color: #325db5;
  background: rgba(255, 255, 255, 0.74);
}

.qr-body {
  margin-top: 18px;
  display: grid;
  grid-template-columns: minmax(0, 218px) minmax(0, 1fr);
  gap: 18px;
  align-items: center;
}

.qr-visual {
  display: grid;
  gap: 12px;
  justify-items: center;
}

.qr-frame {
  width: 218px;
  height: 218px;
  padding: 10px;
  border-radius: 26px;
  background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
  border: 1px solid rgba(208, 219, 236, 0.86);
  box-shadow:
    0 16px 36px rgba(73, 99, 142, 0.1),
    inset 0 1px 0 rgba(255, 255, 255, 0.9);
}

.qr-canvas {
  display: block;
  width: 100%;
  height: 100%;
  border-radius: 18px;
}

.qr-caption {
  color: #8393aa;
  font-size: 12px;
}

.qr-meta {
  display: grid;
  gap: 12px;
}

.meta-row {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 13px 14px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.88);
  border: 1px solid rgba(212, 223, 239, 0.74);
}

.meta-row span {
  color: #72849c;
  font-size: 13px;
}

.meta-row strong,
.meta-row a {
  color: #193554;
  font-size: 13px;
  font-weight: 600;
  text-align: right;
}

.meta-row a {
  color: #2f63d6;
  text-decoration: none;
  word-break: break-all;
}

.meta-row a:hover {
  text-decoration: underline;
}

.qr-placeholder {
  margin: 18px 0 2px;
  color: #6f8199;
  line-height: 1.8;
  font-size: 14px;
}

@media (max-width: 1120px) {
  .login-shell {
    gap: 40px;
    grid-template-columns: minmax(0, 1fr) minmax(390px, 0.94fr);
  }
}

@media (max-width: 900px) {
  .login-page {
    padding: 20px 14px;
  }

  .login-shell {
    min-height: auto;
    grid-template-columns: 1fr;
    gap: 24px;
  }

  .brand-panel {
    max-width: none;
    padding: 12px 0 0;
  }

  .login-column {
    justify-content: stretch;
  }

  .login-card {
    width: 100%;
  }
}

@media (max-width: 640px) {
  .login-page {
    padding: 14px 12px 24px;
  }

  .brand-chip {
    padding: 6px 12px;
  }

  .brand-panel h1 {
    font-size: 36px;
  }

  .brand-description {
    font-size: 15px;
  }

  .login-card {
    padding: 22px 16px 18px;
    border-radius: 24px;
  }

  .card-header h2 {
    font-size: 26px;
  }

  .qr-panel {
    padding: 16px;
  }

  .qr-panel-head {
    flex-direction: column;
  }

  .qr-body {
    grid-template-columns: 1fr;
  }

  .qr-frame {
    width: min(218px, 100%);
    height: auto;
    aspect-ratio: 1 / 1;
  }

  .meta-row {
    flex-direction: column;
    align-items: flex-start;
  }

  .meta-row strong,
  .meta-row a {
    text-align: left;
  }
}
</style>
