<template>
  <AppPageShell title="AI结果回流" description="当前阶段先做服务闭环，不强制先上重页面。">
    <section class="result-flow-grid">
      <article class="result-card">
        <div class="result-card__label">手机端来源会话</div>
        <div class="result-card__value">{{ mobileStats.totalSessionCount || 0 }}</div>
        <div class="result-card__hint">来源场景：手机端政策咨询</div>
      </article>
      <article class="result-card">
        <div class="result-card__label">手机端消息数</div>
        <div class="result-card__value">{{ mobileReport.totalMessageCount || 0 }}</div>
        <div class="result-card__hint">已被台账和月报承接</div>
      </article>
      <article class="result-card">
        <div class="result-card__label">本年手机端会话</div>
        <div class="result-card__value">{{ mobileReport.totalSessionCount || 0 }}</div>
        <div class="result-card__hint">月报统计口径</div>
      </article>
    </section>

    <section class="result-panel">
      <div class="result-panel__title">当前回流闭环</div>
      <div class="result-links">
        <router-link class="result-link" to="/policy-consultant">1. 手机端政策咨询提问</router-link>
        <router-link class="result-link" to="/ai-ledger">2. 咨询台账查看来源数据</router-link>
        <router-link class="result-link" to="/ai-monthly-report">3. 月度报表查看来源统计</router-link>
        <router-link class="result-link" to="/log-center">4. 日志中台排查链路断点</router-link>
      </div>
    </section>

    <section class="result-panel">
      <div class="result-panel__title">最近手机端会话</div>
      <div v-if="sessionList.length" class="session-list">
        <article v-for="item in sessionList" :key="item.id" class="session-card">
          <div class="session-card__title">{{ item.sessionTitle || `会话 #${item.id}` }}</div>
          <div class="session-card__meta">Skill：{{ item.skillName || '-' }}</div>
          <div class="session-card__meta">消息数：{{ item.messageCount || 0 }}</div>
          <div class="session-card__meta">状态：{{ item.status || '-' }}</div>
          <div class="session-card__meta">时间：{{ formatDateTime(item.lastMessageTime || item.createTime) }}</div>
        </article>
      </div>
      <div v-else class="result-empty">当前还没有手机端政策咨询会话，建议先去手机端页面提一个真实问题。</div>
    </section>
  </AppPageShell>
</template>

<script setup>
import { onMounted, reactive, computed } from 'vue'
import AppPageShell from '@/components/layout/AppPageShell.vue'
import { queryAgentMonthlyReport, queryAgentSessionStats, queryAgentSessions } from '@/api/agent'

const state = reactive({
  mobileStats: {},
  mobileReport: {},
  sessionList: []
})

const mobileStats = computed(() => state.mobileStats || {})
const mobileReport = computed(() => state.mobileReport || {})
const sessionList = computed(() => state.sessionList || [])

function ensureSuccess(response, fallback = '请求失败') {
  if (!response || response.code !== 0) {
    throw new Error(response?.message || fallback)
  }
  return response.data
}

function formatDateTime(value) {
  return value ? String(value).replace('T', ' ') : '-'
}

onMounted(async () => {
  const year = new Date().getFullYear()
  const sourceScene = 'MOBILE_POLICY_CONSULTANT'
  const [statsResponse, reportResponse, listResponse] = await Promise.all([
    queryAgentSessionStats({ sourceScene }).catch(() => null),
    queryAgentMonthlyReport({ year, sourceScene }).catch(() => null),
    queryAgentSessions({ sourceScene, status: 'ACTIVE' }).catch(() => null)
  ])

  state.mobileStats = statsResponse ? (ensureSuccess(statsResponse, '加载手机端统计失败') || {}) : {}
  state.mobileReport = reportResponse ? (ensureSuccess(reportResponse, '加载手机端月报失败') || {}) : {}
  state.sessionList = listResponse ? (ensureSuccess(listResponse, '加载手机端会话失败') || []).slice(0, 6) : []
})
</script>

<style scoped>
.result-flow-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 16px;
}

.result-card,
.result-panel,
.session-card {
  padding: 18px;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  background: #fff;
}

.result-card__label,
.result-card__hint,
.session-card__meta,
.result-empty {
  color: #64748b;
  font-size: 13px;
}

.result-card__value {
  margin-top: 10px;
  color: #0f172a;
  font-size: 30px;
  font-weight: 700;
}

.result-panel {
  margin-bottom: 16px;
}

.result-panel__title,
.session-card__title {
  color: #111827;
  font-size: 16px;
  font-weight: 700;
}

.result-links,
.session-list {
  display: grid;
  gap: 12px;
  margin-top: 14px;
}

.result-link {
  display: block;
  padding: 14px 16px;
  border-radius: 12px;
  background: #f8fafc;
  color: #2563eb;
  text-decoration: none;
  font-weight: 600;
}

.session-card__meta + .session-card__meta {
  margin-top: 6px;
}

@media (max-width: 1024px) {
  .result-flow-grid {
    grid-template-columns: 1fr;
  }
}
</style>
