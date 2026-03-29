<template>
  <AppPageShell title="月度报表" description="按年份查看 AI 咨询经营数据，包括趋势、同比环比、知识库命中率和专家维度表现。">
    <template #actions>
      <div class="action-row">
        <div class="year-select">
          <span>统计年份</span>
          <select v-model="state.query.year">
            <option v-for="item in yearOptions" :key="item" :value="item">{{ item }}</option>
          </select>
        </div>
        <van-button type="success" plain :loading="state.exporting" :disabled="state.loading" @click="handleExport">
          导出月报
        </van-button>
        <van-button size="small" type="primary" :loading="state.loading" @click="fetchReport">刷新报表</van-button>
      </div>
    </template>

    <section class="panel filter-panel" data-guide="ai-monthly-filter">
      <div class="panel-title">分析范围</div>
      <div class="filter-grid">
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
        <van-field v-model.trim="state.query.keywords" label="关键词" placeholder="按标题、技能、知识库或用户筛选" />
      </div>
      <div class="action-row">
        <van-button size="small" type="primary" :loading="state.loading" @click="fetchReport">查询</van-button>
        <van-button size="small" plain :disabled="state.loading || state.exporting" @click="resetQuery">重置</van-button>
      </div>
    </section>

    <section class="stats-grid" data-guide="ai-monthly-overview">
      <div class="stats-card">
        <div class="stats-label">年度会话数</div>
        <div class="stats-value">{{ report.totalSessionCount || 0 }}</div>
      </div>
      <div class="stats-card">
        <div class="stats-label">年度消息数</div>
        <div class="stats-value">{{ report.totalMessageCount || 0 }}</div>
      </div>
      <div class="stats-card">
        <div class="stats-label">活跃月份</div>
        <div class="stats-value">{{ report.activeMonthCount || 0 }}</div>
      </div>
      <div class="stats-card">
        <div class="stats-label">月均会话</div>
        <div class="stats-value">{{ report.averageMonthlySessions || 0 }}</div>
      </div>
      <div class="stats-card">
        <div class="stats-label">本月会话</div>
        <div class="stats-value">{{ report.currentMonthSessionCount || 0 }}</div>
      </div>
      <div class="stats-card">
        <div class="stats-label">知识命中率</div>
        <div class="stats-value stats-value--small">{{ formatPercent(report.citationHitRate) }}</div>
      </div>
      <div class="stats-card">
        <div class="stats-label">环比</div>
        <div class="stats-value stats-value--small">{{ formatDelta(report.monthOverMonthRate) }}</div>
      </div>
      <div class="stats-card">
        <div class="stats-label">同比</div>
        <div class="stats-value stats-value--small">{{ formatDelta(report.yearOverYearRate) }}</div>
      </div>
      <div class="stats-card">
        <div class="stats-label">AI 回复数</div>
        <div class="stats-value">{{ report.assistantMessageCount || 0 }}</div>
      </div>
      <div class="stats-card">
        <div class="stats-label">带引用回复数</div>
        <div class="stats-value">{{ report.citedMessageCount || 0 }}</div>
      </div>
      <div class="stats-card">
        <div class="stats-label">TOP 技能</div>
        <div class="stats-value stats-value--small">{{ report.topSkillLabel || '-' }}</div>
      </div>
      <div class="stats-card">
        <div class="stats-label">TOP 知识库</div>
        <div class="stats-value stats-value--small">{{ report.topBaseLabel || '-' }}</div>
      </div>
      <div class="stats-card stats-card--wide">
        <div class="stats-label">TOP 专家</div>
        <div class="stats-value stats-value--small">{{ report.topExpertLabel || '-' }}</div>
      </div>
    </section>

    <section class="trend-layout">
      <section class="panel panel--wide">
        <div class="panel-title">本年趋势</div>
        <div class="panel-hint">{{ state.query.year }} 年 1 月至 12 月咨询会话趋势</div>
        <van-loading v-if="state.loading" class="state-block" size="24px" vertical>加载中...</van-loading>
        <div v-else class="trend-list">
          <div v-for="item in report.monthlySessions || []" :key="item.label" class="trend-item">
            <div class="trend-label">{{ monthLabel(item.label) }}</div>
            <div class="trend-bar-wrap">
              <div class="trend-bar" :style="{ width: `${barWidth(item.value, report.monthlySessions)}%` }"></div>
            </div>
            <div class="trend-value">{{ item.value || 0 }}</div>
          </div>
        </div>
      </section>

      <section class="panel panel--wide">
        <div class="panel-title">去年对比</div>
        <div class="panel-hint">{{ state.query.year - 1 }} 年与 {{ state.query.year }} 年月度会话对比</div>
        <van-loading v-if="state.loading" class="state-block" size="24px" vertical>加载中...</van-loading>
        <div v-else class="compare-list">
          <div v-for="item in compareRows" :key="item.month" class="compare-item">
            <div class="compare-title">{{ item.month }}</div>
            <div class="compare-metrics">
              <span>本年 {{ item.current }}</span>
              <span>去年 {{ item.previous }}</span>
              <span>同比 {{ formatDelta(item.yoy) }}</span>
            </div>
          </div>
        </div>
      </section>
    </section>

    <section class="panel-grid panel-grid--triple">
      <section class="panel">
        <div class="panel-title">技能排行</div>
        <van-empty v-if="!(report.skillRanking || []).length" description="暂无技能排行" />
        <div v-else class="rank-list">
          <div v-for="item in report.skillRanking" :key="item.label" class="rank-item">
            <div class="rank-title">{{ item.label }}</div>
            <div class="meta-line">会话 {{ item.sessionCount || 0 }} / 消息 {{ item.messageCount || 0 }}</div>
          </div>
        </div>
      </section>

      <section class="panel">
        <div class="panel-title">知识库排行</div>
        <van-empty v-if="!(report.baseRanking || []).length" description="暂无知识库排行" />
        <div v-else class="rank-list">
          <div v-for="item in report.baseRanking" :key="item.label" class="rank-item">
            <div class="rank-title">{{ item.label }}</div>
            <div class="meta-line">会话 {{ item.sessionCount || 0 }} / 消息 {{ item.messageCount || 0 }}</div>
          </div>
        </div>
      </section>

      <section class="panel">
        <div class="panel-title">用户排行</div>
        <van-empty v-if="!(report.userRanking || []).length" description="暂无用户排行" />
        <div v-else class="rank-list">
          <div v-for="item in report.userRanking" :key="item.label" class="rank-item">
            <div class="rank-title">{{ item.label }}</div>
            <div class="meta-line">会话 {{ item.sessionCount || 0 }} / 消息 {{ item.messageCount || 0 }}</div>
          </div>
        </div>
      </section>
    </section>

    <section class="panel" data-guide="ai-monthly-expert">
      <div class="panel-title">专家命中率排行</div>
      <div class="panel-hint">按专家身份统计 AI 回复、带引用回复和知识命中率</div>
      <van-empty v-if="!(report.expertRanking || []).length" description="暂无专家指标" />
      <div v-else class="expert-table">
        <div class="expert-table__header">
          <span>专家</span>
          <span>等级</span>
          <span>会话</span>
          <span>回复</span>
          <span>带引用回复</span>
          <span>命中率</span>
        </div>
        <div v-for="item in report.expertRanking" :key="`${item.label}-${item.expertLevel}`" class="expert-table__row">
          <span>{{ item.label }}</span>
          <span>{{ item.expertLevel || '-' }}</span>
          <span>{{ item.sessionCount || 0 }}</span>
          <span>{{ item.assistantMessageCount || 0 }}</span>
          <span>{{ item.citedMessageCount || 0 }}</span>
          <span>{{ formatPercent(item.citationHitRate) }}</span>
        </div>
      </div>
    </section>
  </AppPageShell>
</template>

<script setup>
import { computed, onMounted, reactive } from 'vue'
import { showToast } from 'vant'
import AppPageShell from '@/components/layout/AppPageShell.vue'
import { exportAgentMonthlyReportExcel, queryAgentMonthlyReport } from '@/api/agent'
import { queryCurrentAiPermission } from '@/api/ai'
import { querySkillList } from '@/api/skill'
import { queryUserPageApi } from '@/api/user'

function createReport() {
  return {
    year: new Date().getFullYear(),
    totalSessionCount: 0,
    totalMessageCount: 0,
    activeMonthCount: 0,
    averageMonthlySessions: 0,
    currentMonthSessionCount: 0,
    monthOverMonthRate: 0,
    yearOverYearRate: 0,
    assistantMessageCount: 0,
    citedMessageCount: 0,
    citationHitRate: 0,
    topSkillLabel: '-',
    topBaseLabel: '-',
    topExpertLabel: '-',
    monthlySessions: [],
    previousYearMonthlySessions: [],
    skillRanking: [],
    baseRanking: [],
    userRanking: [],
    expertRanking: []
  }
}

function createQuery() {
  return {
    year: new Date().getFullYear(),
    userId: '',
    skillId: '',
    keywords: ''
  }
}

const state = reactive({
  permissions: null,
  loading: false,
  exporting: false,
  query: createQuery(),
  report: createReport(),
  skillOptions: [],
  userOptions: []
})

const canManage = computed(() => Boolean(state.permissions?.admin))
const report = computed(() => state.report)
const yearOptions = computed(() => {
  const currentYear = new Date().getFullYear()
  return Array.from({ length: 6 }, (_, index) => currentYear - index)
})
const compareRows = computed(() => {
  const currentMap = new Map((report.value.monthlySessions || []).map((item) => [monthLabel(item.label), Number(item.value || 0)]))
  const previousMap = new Map((report.value.previousYearMonthlySessions || []).map((item) => [monthLabel(item.label), Number(item.value || 0)]))
  return Array.from({ length: 12 }, (_, index) => {
    const month = `${String(index + 1).padStart(2, '0')}月`
    const current = currentMap.get(month) || 0
    const previous = previousMap.get(month) || 0
    return {
      month,
      current,
      previous,
      yoy: computeDelta(current, previous)
    }
  })
})

function ensureSuccess(response, fallback = '请求失败') {
  if (!response || response.code !== 0) {
    throw new Error(response?.message || fallback)
  }
  return response.data
}

function buildPayload() {
  return {
    year: Number(state.query.year),
    userId: state.query.userId ? Number(state.query.userId) : undefined,
    skillId: state.query.skillId ? Number(state.query.skillId) : undefined,
    keywords: state.query.keywords || undefined
  }
}

function getTodayText() {
  const today = new Date()
  return `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`
}

function formatPercent(value) {
  return `${Number(value || 0).toFixed(2)}%`
}

function formatDelta(value) {
  const number = Number(value || 0)
  const prefix = number > 0 ? '+' : ''
  return `${prefix}${number.toFixed(2)}%`
}

function computeDelta(current, previous) {
  if (!previous) {
    return current > 0 ? 100 : 0
  }
  return ((current - previous) / previous) * 100
}

function monthLabel(label) {
  if (!label) {
    return '-'
  }
  const parts = String(label).split('-')
  return parts.length >= 2 ? `${parts[1]}月` : String(label)
}

function barWidth(value, list = []) {
  const maxValue = Math.max(...list.map((item) => Number(item.value || 0)), 1)
  return Math.max(10, Math.round((Number(value || 0) / maxValue) * 100))
}

function resetQuery() {
  Object.assign(state.query, createQuery())
  fetchReport()
}

async function fetchPermissions() {
  state.permissions = ensureSuccess(await queryCurrentAiPermission(), '权限信息加载失败')
}

async function fetchOptions() {
  const skillResponse = await querySkillList({ publishStatus: 'PUBLISHED', status: 1 })
  state.skillOptions = ensureSuccess(skillResponse, '技能列表加载失败') || []
  if (canManage.value) {
    const userResponse = await queryUserPageApi({ pageNo: 1, pageSize: 200 })
    const userData = ensureSuccess(userResponse, '用户列表加载失败')
    state.userOptions = Array.isArray(userData?.list) ? userData.list : []
  } else {
    state.userOptions = []
  }
}

async function fetchReport() {
  state.loading = true
  try {
    state.report = ensureSuccess(await queryAgentMonthlyReport(buildPayload()), '月度报表加载失败') || createReport()
  } catch (error) {
    showToast(error.message || '月度报表加载失败')
  } finally {
    state.loading = false
  }
}

async function handleExport() {
  state.exporting = true
  try {
    const blob = await exportAgentMonthlyReportExcel(buildPayload())
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `ai-consultation-monthly-report-${state.query.year}-${getTodayText()}.xlsx`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    showToast('月度报表导出成功')
  } catch (error) {
    showToast(error.message || '月度报表导出失败')
  } finally {
    state.exporting = false
  }
}

onMounted(async () => {
  try {
    await fetchPermissions()
    await fetchOptions()
    await fetchReport()
  } catch (error) {
    showToast(error.message || '月度报表初始化失败')
  }
})
</script>

<style scoped>
.action-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.year-select {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  background: #fff;
}

.year-select select {
  border: 0;
  background: transparent;
  outline: none;
}

.panel,
.stats-card,
.rank-item,
.trend-item,
.compare-item {
  padding: 16px;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  background: #fff;
}

.panel {
  margin-bottom: 16px;
}

.panel-title,
.rank-title {
  color: #111827;
  font-weight: 600;
}

.panel-hint,
.meta-line,
.stats-label,
.trend-label,
.trend-value,
.compare-metrics {
  color: #6b7280;
  font-size: 13px;
}

.filter-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin: 12px 0;
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

.select-field select {
  flex: 1;
  border: 0;
  background: transparent;
  outline: none;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 16px;
}

.stats-card--wide {
  grid-column: span 2;
}

.stats-value {
  margin-top: 10px;
  font-size: 28px;
  font-weight: 700;
  color: #111827;
}

.stats-value--small {
  font-size: 18px;
}

.trend-layout {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-bottom: 16px;
}

.panel-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.panel-grid--triple {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.panel--wide {
  min-width: 0;
}

.rank-list,
.trend-list,
.compare-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 12px;
}

.trend-item {
  display: grid;
  grid-template-columns: 64px 1fr 36px;
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

.compare-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.compare-title {
  color: #111827;
  font-weight: 600;
}

.compare-metrics {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.expert-table {
  margin-top: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  overflow: hidden;
}

.expert-table__header,
.expert-table__row {
  display: grid;
  grid-template-columns: 1.4fr 0.8fr 0.7fr 0.8fr 1fr 0.8fr;
  gap: 12px;
  padding: 12px 16px;
}

.expert-table__header {
  background: #f8fafc;
  color: #374151;
  font-weight: 600;
}

.expert-table__row {
  border-top: 1px solid #eef2f7;
  color: #111827;
}

.state-block {
  padding: 20px 0;
}

@media (max-width: 1280px) {
  .stats-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .trend-layout,
  .panel-grid,
  .panel-grid--triple,
  .filter-grid {
    grid-template-columns: 1fr;
  }

  .stats-card--wide {
    grid-column: auto;
  }
}

@media (max-width: 900px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }

  .select-field,
  .year-select {
    flex-direction: column;
    align-items: flex-start;
  }

  .select-field select,
  .year-select select {
    width: 100%;
  }

  .trend-item,
  .expert-table__header,
  .expert-table__row {
    grid-template-columns: 1fr;
  }
}
</style>
