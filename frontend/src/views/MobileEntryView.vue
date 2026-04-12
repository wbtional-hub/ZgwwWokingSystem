<template>
  <section class="mobile-entry-page">
    <div class="mobile-entry-card">
      <div class="mobile-entry-badge">{{ badgeText }}</div>
      <h1 class="mobile-entry-title">{{ titleText }}</h1>
      <p class="mobile-entry-message">{{ messageText }}</p>

      <div v-if="showLoading" class="mobile-entry-loading" aria-hidden="true">
        <span class="mobile-entry-dot"></span>
        <span class="mobile-entry-dot"></span>
        <span class="mobile-entry-dot"></span>
      </div>

      <button
        v-else
        type="button"
        class="mobile-entry-button"
        @click="goLogin"
      >
        去登录页
      </button>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { MOBILE_WORKSPACE_PATH } from '@/constants/mobile-workspace'
import request from '@/utils/request'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const state = reactive({
  status: 'loading',
  errorMessage: ''
})

const enforceWechatBrowser = computed(() => route.meta?.enforceWechatBrowser !== false)
const redirectPath = computed(() => {
  const target = route.meta?.entryRedirectPath
  return typeof target === 'string' && target.startsWith('/')
    ? target
    : MOBILE_WORKSPACE_PATH
})

const badgeText = computed(() => enforceWechatBrowser.value ? 'WeChat Entry' : 'Mobile Debug Entry')
const showLoading = computed(() => ['loading', 'browser_redirecting'].includes(state.status))

const titleText = computed(() => {
  switch (state.status) {
    case 'browser_redirecting':
      return '正在进入调试入口'
    case 'not_wechat':
      return '请在微信中打开此页面'
    case 'disabled':
      return '微信公众号登录未启用，请联系管理员'
    case 'error':
      return enforceWechatBrowser.value ? '获取微信授权地址失败' : '进入调试入口失败'
    default:
      return enforceWechatBrowser.value ? '正在发起微信授权，请稍候...' : '正在进入调试入口，请稍候...'
  }
})

const messageText = computed(() => {
  switch (state.status) {
    case 'browser_redirecting':
      return '当前为调试入口，非微信浏览器会复用现有登录链路进入移动工作台。'
    case 'not_wechat':
      return '当前环境不是微信浏览器，请在微信中打开正式入口页面。'
    case 'disabled':
      return '系统当前未开启公众号登录，请联系管理员检查相关配置。'
    case 'error':
      return state.errorMessage || '未知错误'
    default:
      return enforceWechatBrowser.value
        ? '系统正在请求微信公众号授权地址，成功后会自动跳转。'
        : '系统正在为调试入口选择可用链路，成功后会自动跳转。'
  }
})

onMounted(() => {
  void startEntryFlow()
})

async function startEntryFlow() {
  if (!enforceWechatBrowser.value && !isWechatBrowser()) {
    state.status = 'browser_redirecting'
    await redirectToDebugEntry()
    return
  }

  await startWechatAuthorize()
}

async function redirectToDebugEntry() {
  state.errorMessage = ''
  try {
    if (userStore.token) {
      await router.replace(redirectPath.value)
      return
    }
    await router.replace({
      path: '/login',
      query: { redirect: redirectPath.value }
    })
  } catch (error) {
    state.status = 'error'
    state.errorMessage = error?.message || '未知错误'
  }
}

async function startWechatAuthorize() {
  if (enforceWechatBrowser.value && !isWechatBrowser()) {
    state.status = 'not_wechat'
    return
  }

  state.status = 'loading'
  state.errorMessage = ''

  try {
    const response = await request.get('/auth/wechat-mp-authorize-url', {
      params: {
        returnUrl: redirectPath.value
      }
    })
    if (!response || response.code !== 0) {
      throw new Error(response?.message || '请求失败')
    }
    const data = response.data || {}
    if (data.enabled !== true) {
      state.status = 'disabled'
      return
    }
    if (data.authorizeUrl) {
      window.location.href = data.authorizeUrl
      return
    }
    throw new Error('授权地址为空')
  } catch (error) {
    state.status = 'error'
    state.errorMessage = error?.response?.data?.message || error?.message || '未知错误'
  }
}

function isWechatBrowser() {
  if (typeof navigator === 'undefined') {
    return false
  }
  return /micromessenger/i.test(navigator.userAgent || '')
}

function goLogin() {
  router.push({
    path: '/login',
    query: { redirect: redirectPath.value }
  })
}
</script>

<style scoped>
.mobile-entry-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px 16px;
  background: #f5f7fb;
}

.mobile-entry-card {
  width: min(100%, 420px);
  padding: 28px 22px;
  border-radius: 24px;
  background: #ffffff;
  border: 1px solid rgba(148, 163, 184, 0.18);
  box-shadow: 0 18px 48px rgba(15, 23, 42, 0.08);
  text-align: center;
}

.mobile-entry-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 30px;
  padding: 0 12px;
  border-radius: 999px;
  background: rgba(15, 118, 110, 0.08);
  color: #0f766e;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.mobile-entry-title {
  margin: 18px 0 10px;
  color: #111827;
  font-size: 24px;
  line-height: 1.3;
}

.mobile-entry-message {
  margin: 0;
  color: #4b5563;
  font-size: 14px;
  line-height: 1.8;
}

.mobile-entry-loading {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-top: 24px;
}

.mobile-entry-dot {
  width: 10px;
  height: 10px;
  border-radius: 999px;
  background: #10b981;
  animation: mobile-entry-bounce 0.9s ease-in-out infinite;
}

.mobile-entry-dot:nth-child(2) {
  animation-delay: 0.12s;
}

.mobile-entry-dot:nth-child(3) {
  animation-delay: 0.24s;
}

.mobile-entry-button {
  width: 100%;
  margin-top: 24px;
  min-height: 46px;
  border: none;
  border-radius: 14px;
  background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%);
  color: #ffffff;
  font-size: 15px;
  font-weight: 600;
}

@keyframes mobile-entry-bounce {
  0%,
  80%,
  100% {
    transform: translateY(0);
    opacity: 0.45;
  }

  40% {
    transform: translateY(-5px);
    opacity: 1;
  }
}
</style>
