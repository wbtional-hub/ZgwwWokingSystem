<template>
  <AppPageShell title="咨询台账" description="沉淀 AI 工作台会话记录，支持统计、趋势查看、归档管理和多格式导出。">
    <template #actions>
      <div class="action-row">
        <van-button type="success" plain :loading="state.exportingExcel" :disabled="pageBusy" @click="handleExportExcel">
          导出 Excel
        </van-button>
        <van-button type="success" plain :loading="state.exportingCsv" :disabled="pageBusy" @click="handleExportCsv">
          导出 CSV
        </van-button>
        <van-button plain type="primary" :loading="state.loadingList || state.loadingStats || state.loadingTrend" :disabled="pageBusy" @click="reloadAll">
          刷新台账
        </van-button>
      </div>
    </template>

    <section class="stats-grid" data-guide="ai-ledger-stats">
      <div class="stats-card">
        <div class="stats-label">会话总数</div>
        <div class="stats-value">{{ state.stats.totalSessionCount || 0 }}</div>
      </div>
      <div class="stats-card">
        <div class="stats-label">活跃会话</div>
        <div class="stats-value">{{ state.stats.activeSessionCount || 0 }}</div>
      </div>
      <div class="stats-card">
        <div class="stats-label">今日新增</div>
        <div class="stats-value">{{ state.stats.todaySessionCount || 0 }}</div>
      </div>
      <div class="stats-card">
        <div class="stats-label">消息总数</div>
        <div class="stats-value">{{ state.stats.totalMessageCount || 0 }}</div>
      </div>
      <div class="stats-card">
        <div class="stats-label">涉及技能</div>
        <div class="stats-value">{{ state.stats.distinctSkillCount || 0 }}</div>
      </div>
    </section>

    <section class="panel" data-guide="ai-ledger-filter">
      <div class="panel-title">筛选条件</div>
      <div class="filter-grid filter-grid--wide">
        <div v-if="canManage" class="select-field">
          <span class="select-label">用户</span>
          <select v-model="state.query.userId">
            <option value="">全部</option>
            <option v-for="user in state.userOptions" :key="user.id" :value="String(user.id)">
              {{ user.realName || user.username }}
            </option>
          </select>
        </div>
        <div class="select-field">
          <span class="select-label">技能</span>
          <select v-model="state.query.skillId">
            <option value="">全部</option>
            <option v-for="item in state.skillOptions" :key="item.id" :value="String(item.id)">
              {{ item.skillName }}
            </option>
          </select>
        </div>
        <div class="select-field">
          <span class="select-label">状态</span>
          <select v-model="state.query.status">
            <option value="">全部</option>
            <option value="ACTIVE">ACTIVE</option>
            <option value="ARCHIVED">ARCHIVED</option>
          </select>
        </div>
        <div class="select-field">
          <span class="select-label">开始日期</span>
          <input v-model="state.query.startDate" type="date" />
        </div>
        <div class="select-field">
          <span class="select-label">结束日期</span>
          <input v-model="state.query.endDate" type="date" />
        </div>
        <van-field v-model.trim="state.query.keywords" label="关键字" placeholder="按标题、技能、知识库或用户搜索" />
      </div>
      <div class="action-row">
        <van-button size="small" type="primary" :loading="state.loadingList || state.loadingStats || state.loadingTrend" @click="reloadAll">
          查询
        </van-button>
        <van-button size="small" plain :disabled="pageBusy" @click="resetQuery">
          重置
        </van-button>
      </div>
    </section>

    <section class="trend-grid">
      <section class="panel">
        <div class="panel-title">咨询趋势</div>
        <div class="panel-hint">
          {{ state.query.startDate || '最近 7 天默认起始' }} 至 {{ state.query.endDate || '最近 7 天默认结束' }}
        </div>
        <van-loading v-if="state.loadingTrend" class="state-block" size="24px" vertical>加载中...</van-loading>
        <div v-else class="trend-list">
          <div v-for="item in state.trend.dailySessions || []" :key="item.label" class="trend-item">
            <div class="trend-label">{{ item.label }}</div>
            <div class="trend-bar-wrap">
              <div class="trend-bar" :style="{ width: `${barWidth(item.value)}%` }"></div>
            </div>
            <div class="trend-value">{{ item.value }}</div>
          </div>
        </div>
      </section>

      <section class="panel">
        <div class="panel-title">技能排行</div>
        <van-empty v-if="!(state.trend.skillRanking || []).length" description="暂无排行数据" />
        <div v-else class="rank-list">
          <div v-for="item in state.trend.skillRanking" :key="item.label" class="rank-item">
            <div class="rank-title">{{ item.label }}</div>
            <div class="meta-line">会话 {{ item.sessionCount || 0 }} / 消息 {{ item.messageCount || 0 }}</div>
          </div>
        </div>
      </section>

      <section class="panel">
        <div class="panel-title">用户排行</div>
        <van-empty v-if="!(state.trend.userRanking || []).length" description="暂无排行数据" />
        <div v-else class="rank-list">
          <div v-for="item in state.trend.userRanking" :key="item.label" class="rank-item">
            <div class="rank-title">{{ item.label }}</div>
            <div class="meta-line">会话 {{ item.sessionCount || 0 }} / 消息 {{ item.messageCount || 0 }}</div>
          </div>
        </div>
      </section>
    </section>

    <section class="panel" data-guide="ai-ledger-list">
      <div class="panel-title">会话列表</div>
      <div class="panel-hint">当前共 {{ state.list.length }} 条咨询记录</div>
      <van-loading v-if="state.loadingList" class="state-block" size="24px" vertical>加载中...</van-loading>
      <van-empty v-else-if="!state.list.length" description="暂无咨询记录" />
      <div v-else class="ledger-list">
        <div v-for="item in state.list" :key="item.id" class="ledger-item">
          <div class="ledger-title-row">
            <div class="ledger-title">{{ item.sessionTitle || '未命名会话' }}</div>
            <div class="ledger-tag-row">
              <van-tag type="primary">{{ item.status || 'ACTIVE' }}</van-tag>
              <van-button size="mini" plain type="warning" :disabled="pageBusy" @click="handleToggleArchive(item)">
                {{ item.status === 'ARCHIVED' ? '恢复' : '归档' }}
              </van-button>
            </div>
          </div>
          <div class="meta-line">用户：{{ canManage ? (item.realName || item.username || '-') : '当前用户' }}</div>
          <div class="meta-line">技能：{{ item.skillName || '-' }}</div>
          <div class="meta-line">知识库：{{ item.baseName || '-' }}</div>
          <div class="meta-line">模型：{{ item.modelCode || '-' }}</div>
          <div class="meta-line">消息数：{{ item.messageCount || 0 }}</div>
          <div class="meta-line">最后消息时间：{{ formatDateTime(item.lastMessageTime) }}</div>
          <div class="meta-line">创建时间：{{ formatDateTime(item.createTime) }}</div>
        </div>
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
const pageBusy = computed(() => {
  return state.loadingList || state.loadingStats || state.loadingTrend || state.exportingCsv || state.exportingExcel
})

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
    keywords: state.query.keywords || undefined,
    startDate: state.query.startDate || undefined,
    endDate: state.query.endDate || undefined
  }
}

function formatDateTime(value) {
  if (!value) {
    return '-'
  }
  return String(value).replace('T', ' ')
}

function getTodayText() {
  const today = new Date()
  const year = today.getFullYear()
  const month = String(today.getMonth() + 1).padStart(2, '0')
  const day = String(today.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function resetQuery() {
  Object.assign(state.query, createDefaultQuery())
  reloadAll()
}

function barWidth(value) {
  const list = state.trend.dailySessions || []
  const maxValue = Math.max(...list.map((item) => item.value || 0), 1)
  return Math.max(12, Math.round(((value || 0) / maxValue) * 100))
}

async function fetchPermissions() {
  state.permissions = ensureSuccess(await queryCurrentAiPermission(), '权限信息加载失败')
}

async function fetchOptions() {
  const skills = await querySkillList({})
  state.skillOptions = ensureSuccess(skills, '技能列表加载失败') || []
  if (canManage.value) {
    const users = await queryUserPageApi({ pageNo: 1, pageSize: 200 })
    const userData = ensureSuccess(users, '用户列表加载失败')
    state.userOptions = Array.isArray(userData?.list) ? userData.list : []
  } else {
    state.userOptions = []
  }
}

async function fetchList() {
  state.loadingList = true
  try {
    const data = ensureSuccess(await queryAgentSessions(buildQueryPayload()), '会话台账加载失败')
    state.list = Array.isArray(data) ? data : []
  } catch (error) {
    showToast(error.message || '会话台账加载失败')
  } finally {
    state.loadingList = false
  }
}

async function fetchStats() {
  state.loadingStats = true
  try {
    state.stats = ensureSuccess(await queryAgentSessionStats(buildQueryPayload()), '会话统计加载失败') || {}
  } catch (error) {
    showToast(error.message || '会话统计加载失败')
  } finally {
    state.loadingStats = false
  }
}

async function fetchTrend() {
  state.loadingTrend = true
  try {
    state.trend = ensureSuccess(await queryAgentSessionTrend(buildQueryPayload()), '趋势数据加载失败') || createEmptyTrend()
  } catch (error) {
    state.trend = createEmptyTrend()
    showToast(error.message || '趋势数据加载失败')
  } finally {
    state.loadingTrend = false
  }
}

async function handleToggleArchive(item) {
  const nextStatus = item.status === 'ARCHIVED' ? 'ACTIVE' : 'ARCHIVED'
  const actionText = nextStatus === 'ARCHIVED' ? '归档' : '恢复'
  try {
    await showConfirmDialog({
      title: `${actionText}确认`,
      message: `${actionText}当前会话后将更新台账状态，是否继续？`
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
    await downloadReport(
      exportAgentSessions,
      `ai-consultation-ledger-${getTodayText()}.csv`,
      '咨询台账 CSV 导出成功',
      '咨询台账 CSV 导出失败'
    )
  } finally {
    state.exportingCsv = false
  }
}

async function handleExportExcel() {
  state.exportingExcel = true
  try {
    await downloadReport(
      exportAgentSessionsExcel,
      `ai-consultation-ledger-${getTodayText()}.xlsx`,
      '咨询台账 Excel 导出成功',
      '咨询台账 Excel 导出失败'
    )
  } finally {
    state.exportingExcel = false
  }
}

async function reloadAll() {
  try {
    await Promise.all([fetchList(), fetchStats(), fetchTrend()])
  } catch (error) {
    showToast(error.message || '咨询台账加载失败')
  }
}

onMounted(async () => {
  try {
    await fetchPermissions()
    await fetchOptions()
    await reloadAll()
  } catch (error) {
    showToast(error.message || '咨询台账初始化失败')
  }
})
</script>

<style scoped>
.action-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 16px;
}

.trend-grid {
  display: grid;
  grid-template-columns: 1.4fr 1fr 1fr;
  gap: 16px;
  margin-bottom: 16px;
}

.stats-card,
.panel,
.ledger-item,
.rank-item,
.trend-item {
  padding: 16px;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  background: #fff;
}

.stats-label,
.meta-line,
.panel-hint,
.trend-label,
.trend-value {
  color: #6b7280;
  font-size: 13px;
}

.stats-value,
.panel-title,
.ledger-title,
.rank-title {
  color: #111827;
  font-weight: 600;
}

.stats-value {
  margin-top: 10px;
  font-size: 28px;
}

.panel {
  margin-bottom: 16px;
}

.filter-grid {
  display: grid;
  gap: 12px;
  margin: 12px 0;
}

.filter-grid--wide {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.select-field {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border: 1px solid #ebedf0;
  border-radius: 8px;
}

.select-label {
  flex: 0 0 72px;
}

.select-field select,
.select-field input {
  flex: 1;
  border: 0;
  background: transparent;
  outline: none;
}

.ledger-list,
.rank-list,
.trend-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 12px;
}

.ledger-title-row,
.ledger-tag-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.ledger-title-row {
  justify-content: space-between;
  margin-bottom: 8px;
}

.trend-item {
  display: grid;
  grid-template-columns: 92px 1fr 36px;
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
  background: linear-gradient(90deg, #1677ff, #36cfc9);
}

.state-block {
  padding: 20px 0;
}

@media (max-width: 1200px) {
  .stats-grid {
    grid-template-columns: 1fr 1fr;
  }

  .trend-grid,
  .filter-grid--wide {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 900px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }

  .trend-item {
    grid-template-columns: 1fr;
  }

  .ledger-title-row,
  .select-field {
    flex-direction: column;
    align-items: flex-start;
  }

  .select-field select,
  .select-field input {
    width: 100%;
  }
}
</style>
