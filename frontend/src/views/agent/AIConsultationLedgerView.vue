<template>
  <AppPageShell title="咨询台账" description="沉淀 AI 会话、来源场景、Skill、知识库和消息规模，支持导出与归档。">
    <template #actions>
      <div class="action-row">
        <van-button type="success" plain :loading="state.exportingExcel" :disabled="pageBusy" @click="handleExportExcel">导出 Excel</van-button>
        <van-button type="success" plain :loading="state.exportingCsv" :disabled="pageBusy" @click="handleExportCsv">导出 CSV</van-button>
        <van-button plain type="primary" :loading="pageBusy" @click="reloadAll">刷新</van-button>
      </div>
    </template>

    <section class="stats-grid">
      <article class="stats-card">
        <div class="stats-label">会话总数</div>
        <div class="stats-value">{{ state.stats.totalSessionCount || 0 }}</div>
      </article>
      <article class="stats-card">
        <div class="stats-label">活跃会话</div>
        <div class="stats-value">{{ state.stats.activeSessionCount || 0 }}</div>
      </article>
      <article class="stats-card">
        <div class="stats-label">今日新增</div>
        <div class="stats-value">{{ state.stats.todaySessionCount || 0 }}</div>
      </article>
      <article class="stats-card">
        <div class="stats-label">消息总数</div>
        <div class="stats-value">{{ state.stats.totalMessageCount || 0 }}</div>
      </article>
      <article class="stats-card">
        <div class="stats-label">涉及 Skill</div>
        <div class="stats-value">{{ state.stats.distinctSkillCount || 0 }}</div>
      </article>
    </section>

    <section class="panel">
      <div class="panel-title">筛选条件</div>
      <div class="filter-grid">
        <div v-if="canManage" class="field">
          <span>用户</span>
          <select v-model="state.query.userId">
            <option value="">全部</option>
            <option v-for="user in state.userOptions" :key="user.id" :value="String(user.id)">{{ user.realName || user.username }}</option>
          </select>
        </div>
        <div class="field">
          <span>Skill</span>
          <select v-model="state.query.skillId">
            <option value="">全部</option>
            <option v-for="item in state.skillOptions" :key="item.id" :value="String(item.id)">{{ item.skillName }}</option>
          </select>
        </div>
        <div class="field">
          <span>状态</span>
          <select v-model="state.query.status">
            <option value="">全部</option>
            <option value="ACTIVE">ACTIVE</option>
            <option value="ARCHIVED">ARCHIVED</option>
          </select>
        </div>
        <div class="field">
          <span>来源场景</span>
          <select v-model="state.query.sourceScene">
            <option value="">全部</option>
            <option value="AI_WORKBENCH">AI工作台</option>
            <option value="MOBILE_POLICY_CONSULTANT">手机端政策咨询</option>
          </select>
        </div>
        <div class="field">
          <span>开始日期</span>
          <input v-model="state.query.startDate" type="date" />
        </div>
        <div class="field">
          <span>结束日期</span>
          <input v-model="state.query.endDate" type="date" />
        </div>
        <div class="field field--wide">
          <span>关键词</span>
          <input v-model.trim="state.query.keywords" type="text" placeholder="按标题、Skill、知识库或用户搜索" />
        </div>
      </div>
      <div class="action-row">
        <van-button size="small" type="primary" :loading="pageBusy" @click="reloadAll">查询</van-button>
        <van-button size="small" plain :disabled="pageBusy" @click="resetQuery">重置</van-button>
      </div>
    </section>

    <section class="trend-grid">
      <section class="panel">
        <div class="panel-title">咨询趋势</div>
        <div v-if="state.loadingTrend" class="state-block">加载中...</div>
        <div v-else class="trend-list">
          <div v-for="item in state.trend.dailySessions || []" :key="item.label" class="trend-item">
            <span>{{ item.label }}</span>
            <div class="trend-bar-wrap">
              <div class="trend-bar" :style="{ width: `${barWidth(item.value)}%` }"></div>
            </div>
            <strong>{{ item.value || 0 }}</strong>
          </div>
        </div>
      </section>

      <section class="panel">
        <div class="panel-title">Skill 排名</div>
        <div v-if="!(state.trend.skillRanking || []).length" class="state-block">暂无数据</div>
        <div v-else class="rank-list">
          <div v-for="item in state.trend.skillRanking" :key="item.label" class="rank-item">
            <strong>{{ item.label }}</strong>
            <span>会话 {{ item.sessionCount || 0 }} / 消息 {{ item.messageCount || 0 }}</span>
          </div>
        </div>
      </section>

      <section class="panel">
        <div class="panel-title">用户排名</div>
        <div v-if="!(state.trend.userRanking || []).length" class="state-block">暂无数据</div>
        <div v-else class="rank-list">
          <div v-for="item in state.trend.userRanking" :key="item.label" class="rank-item">
            <strong>{{ item.label }}</strong>
            <span>会话 {{ item.sessionCount || 0 }} / 消息 {{ item.messageCount || 0 }}</span>
          </div>
        </div>
      </section>
    </section>

    <section class="panel">
      <div class="panel-title">会话列表</div>
      <div v-if="state.loadingList" class="state-block">加载中...</div>
      <div v-else-if="!state.list.length" class="state-block">暂无咨询记录</div>
      <div v-else class="ledger-list">
        <article v-for="item in state.list" :key="item.id" class="ledger-item">
          <div class="ledger-item__head">
            <div>
              <div class="ledger-item__title">{{ item.sessionTitle || `会话 #${item.id}` }}</div>
              <div class="ledger-item__meta">来源场景：{{ item.sourceScene || '-' }}</div>
            </div>
            <div class="action-row">
              <van-tag type="primary">{{ item.status || 'ACTIVE' }}</van-tag>
              <van-button size="mini" plain type="warning" :disabled="pageBusy" @click="handleToggleArchive(item)">
                {{ item.status === 'ARCHIVED' ? '恢复' : '归档' }}
              </van-button>
            </div>
          </div>
          <div class="ledger-item__meta">用户：{{ canManage ? (item.realName || item.username || '-') : '当前用户' }}</div>
          <div class="ledger-item__meta">Skill：{{ item.skillName || '-' }}</div>
          <div class="ledger-item__meta">知识库：{{ item.baseName || '-' }}</div>
          <div class="ledger-item__meta">模型：{{ item.modelCode || '-' }}</div>
          <div class="ledger-item__meta">消息数：{{ item.messageCount || 0 }}</div>
          <div class="ledger-item__meta">最后消息：{{ formatDateTime(item.lastMessageTime) }}</div>
          <div class="ledger-item__meta">创建时间：{{ formatDateTime(item.createTime) }}</div>
        </article>
      </div>
    </section>
  </AppPageShell>
</template>

<script setup>
import { computed, onMounted, reactive } from 'vue'
import { showConfirmDialog, showToast } from 'vant'
import AppPageShell from '@/components/layout/AppPageShell.vue'
import {
  exportAgentSessions,
  exportAgentSessionsExcel,
  queryAgentSessions,
  queryAgentSessionStats,
  queryAgentSessionTrend,
  updateAgentSessionStatus
} from '@/api/agent'
import { queryCurrentAiPermission } from '@/api/ai'
import { querySkillList } from '@/api/skill'
import { queryUserPageApi } from '@/api/user'

function createDefaultQuery() {
  return {
    userId: '',
    skillId: '',
    status: '',
    sourceScene: '',
    keywords: '',
    startDate: '',
    endDate: ''
  }
}

function createEmptyTrend() {
  return {
    dailySessions: [],
    skillRanking: [],
    userRanking: []
  }
}

const state = reactive({
  permissions: null,
  loadingList: false,
  loadingStats: false,
  loadingTrend: false,
  exportingCsv: false,
  exportingExcel: false,
  list: [],
  stats: {},
  trend: createEmptyTrend(),
  userOptions: [],
  skillOptions: [],
  query: createDefaultQuery()
})

const canManage = computed(() => Boolean(state.permissions?.admin))
const pageBusy = computed(() => state.loadingList || state.loadingStats || state.loadingTrend || state.exportingCsv || state.exportingExcel)

function ensureSuccess(response, fallback = '请求失败') {
  if (!response || response.code !== 0) {
    throw new Error(response?.message || fallback)
  }
  return response.data
}

function buildQueryPayload() {
  return {
    userId: state.query.userId ? Number(state.query.userId) : undefined,
    skillId: state.query.skillId ? Number(state.query.skillId) : undefined,
    status: state.query.status || undefined,
    sourceScene: state.query.sourceScene || undefined,
    keywords: state.query.keywords || undefined,
    startDate: state.query.startDate || undefined,
    endDate: state.query.endDate || undefined
  }
}

function formatDateTime(value) {
  return value ? String(value).replace('T', ' ') : '-'
}

function getTodayText() {
  const today = new Date()
  return `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`
}

function barWidth(value) {
  const list = state.trend.dailySessions || []
  const maxValue = Math.max(...list.map((item) => Number(item.value || 0)), 1)
  return Math.max(12, Math.round((Number(value || 0) / maxValue) * 100))
}

function resetQuery() {
  Object.assign(state.query, createDefaultQuery())
  reloadAll()
}

async function fetchPermissions() {
  state.permissions = ensureSuccess(await queryCurrentAiPermission(), '加载权限信息失败') || {}
}

async function fetchOptions() {
  state.skillOptions = ensureSuccess(await querySkillList({}), '加载 Skill 列表失败') || []
  if (canManage.value) {
    const userData = ensureSuccess(await queryUserPageApi({ pageNo: 1, pageSize: 200 }), '加载用户列表失败')
    state.userOptions = Array.isArray(userData?.list) ? userData.list : []
  } else {
    state.userOptions = []
  }
}

async function fetchList() {
  state.loadingList = true
  try {
    state.list = ensureSuccess(await queryAgentSessions(buildQueryPayload()), '加载会话台账失败') || []
  } catch (error) {
    showToast(error.message || '加载会话台账失败')
  } finally {
    state.loadingList = false
  }
}

async function fetchStats() {
  state.loadingStats = true
  try {
    state.stats = ensureSuccess(await queryAgentSessionStats(buildQueryPayload()), '加载会话统计失败') || {}
  } catch (error) {
    showToast(error.message || '加载会话统计失败')
  } finally {
    state.loadingStats = false
  }
}

async function fetchTrend() {
  state.loadingTrend = true
  try {
    state.trend = ensureSuccess(await queryAgentSessionTrend(buildQueryPayload()), '加载趋势失败') || createEmptyTrend()
  } catch (error) {
    state.trend = createEmptyTrend()
    showToast(error.message || '加载趋势失败')
  } finally {
    state.loadingTrend = false
  }
}

async function reloadAll() {
  await Promise.all([fetchList(), fetchStats(), fetchTrend()])
}

async function handleToggleArchive(item) {
  const nextStatus = item.status === 'ARCHIVED' ? 'ACTIVE' : 'ARCHIVED'
  const actionText = nextStatus === 'ARCHIVED' ? '归档' : '恢复'
  try {
    await showConfirmDialog({
      title: `${actionText}确认`,
      message: `${actionText}后会更新当前会话状态，是否继续？`
    })
    ensureSuccess(await updateAgentSessionStatus({ sessionId: item.id, status: nextStatus }), `${actionText}失败`)
    showToast(`${actionText}成功`)
    await reloadAll()
  } catch (error) {
    if (error?.message) {
      showToast(error.message || `${actionText}失败`)
    }
  }
}

async function downloadReport(fetcher, fileName, successMessage, failMessage) {
  try {
    const blob = await fetcher(buildQueryPayload())
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = fileName
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    showToast(successMessage)
  } catch (error) {
    showToast(error.message || failMessage)
  }
}

async function handleExportCsv() {
  state.exportingCsv = true
  try {
    await downloadReport(exportAgentSessions, `ai-consultation-ledger-${getTodayText()}.csv`, 'CSV 导出成功', 'CSV 导出失败')
  } finally {
    state.exportingCsv = false
  }
}

async function handleExportExcel() {
  state.exportingExcel = true
  try {
    await downloadReport(exportAgentSessionsExcel, `ai-consultation-ledger-${getTodayText()}.xlsx`, 'Excel 导出成功', 'Excel 导出失败')
  } finally {
    state.exportingExcel = false
  }
}

onMounted(async () => {
  try {
    await fetchPermissions()
    await fetchOptions()
    await reloadAll()
  } catch (error) {
    showToast(error.message || '初始化咨询台账失败')
  }
})
</script>

<style scoped>
.action-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.stats-grid,
.trend-grid {
  display: grid;
  gap: 16px;
  margin-bottom: 16px;
}

.stats-grid {
  grid-template-columns: repeat(5, minmax(0, 1fr));
}

.trend-grid {
  grid-template-columns: 1.4fr 1fr 1fr;
}

.stats-card,
.panel,
.ledger-item,
.rank-item,
.trend-item {
  padding: 16px;
  border: 1px solid #e5e7eb;
  border-radius: 14px;
  background: #fff;
}

.stats-label,
.ledger-item__meta,
.state-block,
.trend-item span,
.rank-item span {
  color: #64748b;
  font-size: 13px;
}

.stats-value,
.panel-title,
.ledger-item__title {
  color: #0f172a;
  font-weight: 700;
}

.stats-value {
  margin-top: 10px;
  font-size: 30px;
}

.panel {
  margin-bottom: 16px;
}

.filter-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin: 12px 0;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.field--wide {
  grid-column: span 2;
}

.field select,
.field input {
  min-height: 40px;
  padding: 0 12px;
  border: 1px solid #dbe4f0;
  border-radius: 10px;
  background: #fff;
}

.trend-list,
.rank-list,
.ledger-list {
  display: grid;
  gap: 12px;
  margin-top: 12px;
}

.trend-item {
  display: grid;
  grid-template-columns: 100px 1fr 40px;
  align-items: center;
  gap: 12px;
}

.trend-bar-wrap {
  height: 10px;
  background: #eef2ff;
  border-radius: 999px;
  overflow: hidden;
}

.trend-bar {
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, #2563eb, #0f766e);
}

.ledger-item__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
}

@media (max-width: 1280px) {
  .stats-grid,
  .trend-grid,
  .filter-grid {
    grid-template-columns: 1fr;
  }

  .field--wide {
    grid-column: auto;
  }
}
</style>
