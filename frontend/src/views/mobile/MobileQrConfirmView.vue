<template>
  <section class="qr-confirm-page">
    <div class="qr-confirm-card">
      <div class="qr-confirm-tag">QR Login</div>
      <h1>扫码登录确认</h1>
      <p class="qr-confirm-desc">
        {{ descriptionText }}
      </p>

      <div class="qr-confirm-meta">
        <div class="meta-row">
          <span>二维码状态</span>
          <strong>{{ statusText }}</strong>
        </div>
        <div class="meta-row">
          <span>会话标识</span>
          <strong>{{ maskedQrToken }}</strong>
        </div>
        <div class="meta-row">
          <span>过期时间</span>
          <strong>{{ expireText }}</strong>
        </div>
      </div>

      <div v-if="state.message" class="qr-confirm-message">{{ state.message }}</div>

      <div v-if="!userStore.token" class="qr-confirm-actions">
        <van-button block type="primary" @click="goLogin">请先登录手机端</van-button>
      </div>
      <div v-else class="qr-confirm-actions">
        <van-button
          block
          type="primary"
          :loading="state.submitting"
          :disabled="!canConfirm"
          @click="submitDecision(true)"
        >
          确认登录
        </van-button>
        <van-button
          block
          plain
          type="danger"
          :loading="state.submitting"
          :disabled="!canConfirm"
          @click="submitDecision(false)"
        >
          拒绝登录
        </van-button>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast } from 'vant'
import { confirmQrLogin, getQrMobileSession } from '@/api/auth'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const state = reactive({
  loading: false,
  submitting: false,
  qrToken: '',
  status: '',
  expireAt: '',
  message: ''
})

const canConfirm = computed(() => {
  return Boolean(userStore.token) && ['PENDING', 'SCANNED'].includes(state.status) && !state.submitting
})

const descriptionText = computed(() => {
  if (!state.qrToken) {
    return '请通过有效二维码打开当前页面。'
  }
  if (!userStore.token) {
    return '当前手机端未登录。请先登录后，再返回此页面确认二维码登录。'
  }
  if (state.status === 'CONFIRMED') {
    return '该二维码已经确认，无需重复操作。'
  }
  if (state.status === 'CANCELED') {
    return '该二维码已被拒绝。'
  }
  if (state.status === 'EXPIRED') {
    return '该二维码已过期，请在 PC 端重新生成。'
  }
  return '确认后，PC 端即可继续完成扫码登录。'
})

const statusText = computed(() => state.status || '未加载')
const maskedQrToken = computed(() => {
  if (!state.qrToken) {
    return '-'
  }
  if (state.qrToken.length <= 12) {
    return state.qrToken
  }
  return `${state.qrToken.slice(0, 6)}...${state.qrToken.slice(-6)}`
})

const expireText = computed(() => state.expireAt ? state.expireAt.replace('T', ' ') : '-')

onMounted(async () => {
  state.qrToken = typeof route.query.qrToken === 'string' ? route.query.qrToken.trim() : ''
  if (!state.qrToken) {
    state.message = '缺少 qrToken，无法确认二维码登录。'
    return
  }
  if (!userStore.token) {
    state.message = '请先登录手机端后再扫码确认。'
    return
  }
  await loadMobileSession()
})

async function loadMobileSession() {
  state.loading = true
  try {
    const response = await getQrMobileSession(state.qrToken)
    if (response.code !== 0 || !response.data) {
      throw new Error(response.message || '二维码会话加载失败')
    }
    state.status = response.data.status || ''
    state.expireAt = response.data.expireAt || ''
    state.message = ''
  } catch (error) {
    state.message = error.response?.data?.message || error.message || '二维码会话加载失败'
  } finally {
    state.loading = false
  }
}

async function submitDecision(approve) {
  if (!canConfirm.value) {
    return
  }
  state.submitting = true
  try {
    const response = await confirmQrLogin({
      qrToken: state.qrToken,
      approve
    })
    if (response.code !== 0 || !response.data) {
      throw new Error(response.message || '二维码确认失败')
    }
    state.status = response.data.status || ''
    state.message = approve ? '已确认登录请求。' : '已拒绝本次登录请求。'
    showToast(state.message)
  } catch (error) {
    state.message = error.response?.data?.message || error.message || '二维码确认失败'
  } finally {
    state.submitting = false
  }
}

function goLogin() {
  router.push({
    path: '/login',
    query: { redirect: route.fullPath }
  })
}
</script>

<style scoped>
.qr-confirm-page {
  min-height: 100vh;
  padding: 24px 16px;
  background: #f5f7fb;
}

.qr-confirm-card {
  max-width: 520px;
  margin: 0 auto;
  border-radius: 20px;
  padding: 24px 18px;
  background: #fff;
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.08);
}

.qr-confirm-tag {
  display: inline-flex;
  padding: 4px 10px;
  border-radius: 999px;
  background: #eff6ff;
  color: #1d4ed8;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.qr-confirm-card h1 {
  margin: 16px 0 10px;
  font-size: 28px;
  color: #111827;
}

.qr-confirm-desc {
  margin: 0;
  color: #4b5563;
  line-height: 1.7;
}

.qr-confirm-meta {
  margin-top: 20px;
  display: grid;
  gap: 10px;
}

.meta-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 14px;
  border-radius: 12px;
  background: #f9fafb;
  border: 1px solid #e5e7eb;
}

.meta-row span {
  color: #6b7280;
  font-size: 13px;
}

.meta-row strong {
  color: #111827;
  font-size: 13px;
  text-align: right;
}

.qr-confirm-message {
  margin-top: 18px;
  padding: 12px 14px;
  border-radius: 12px;
  background: #fff7ed;
  border: 1px solid #fdba74;
  color: #9a3412;
  line-height: 1.6;
}

.qr-confirm-actions {
  margin-top: 20px;
  display: grid;
  gap: 12px;
}
</style>
