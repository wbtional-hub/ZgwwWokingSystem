<template>
  <AppPageShell title="月度报表" description="按来源场景、技能、用户查看 AI 咨询月报，确认手机端政策咨询是否已进入统计闭环。">
    <template #actions>
      <div class="action-row">
        <label class="compact-field">
          <span>统计年份</span>
          <select v-model="state.query.year">
            <option v-for="item in yearOptions" :key="item" :value="item">{{ item }}</option>
          </select>
        </label>
        <van-button size="small" type="success" plain :loading="state.exporting" :disabled="state.loading" @click="handleExport">
          导出月报
        </van-button>
        <van-button size="small" type="primary" :loading="state.loading" @click="fetchReport">刷新</van-button>
      </div>
    </template>

    <section class="panel">
      <div class="panel-title">筛选条件</div>
      <div class="filter-grid">
        <label v-if="canManage" class="filter-field">
          <span>用户</span>
          <select v-model="state.query.userId">
            <option value="">全部</option>
            <option v-for="user in state.userOptions" :key="user.id" :value="String(user.id)">
              {{ user.realName || user.username }}
            </option>
          </select>
        </label>
        <label class="filter-field">
          <span>Skill</span>
          <select v-model="state.query.skillId">
            <option value="">全部</option>
            <option v-for="item in state.skillOptions" :key="item.id" :value="String(item.id)">
              {{ item.skillName }}
            </option>
          </select>
        </label>
        <label class="filter-field">
          <span>来源场景</span>
          <select v-model="state.query.sourceScene">
            <option value="">全部</option>
            <option value="AI_WORKBENCH">AI工作台</option>
            <option value="MOBILE_POLICY_CONSULTANT">手机端政策咨询</option>
          </select>
        </label>
        <label class="filter-field filter-field--wide">
          <span>关键词</span>
          <input v-model.trim="state.query.keywords" type="text" placeholder="按 Skill、知识库、会话标题或用户搜索" />
        </label>
      </div>
      <div class="action-row">
        <van-button size="small" type="primary" :loading="state.loading" @click="fetchReport">查询</van-button>
        <van-button size="small" plain :disabled="state.loading" @click="resetQuery">重置</van-button>
      </div>
    </section>

    <section class="stats-grid">
      <article class="stats-card">
        <div class="stats-label">会话总数</div>
        <div class="stats-value">{{ report.totalSessionCount || 0 }}</div>
      </article>
      <article class="stats-card">
        <div class="stats-label">消息总数</div>
        <div class="stats-value">{{ report.totalMessageCount || 0 }}</div>
      </article>
      <article class="stats-card">
        <div class="stats-label">AI 回复数</div>
        <div class="stats-value">{{ report.assistantMessageCount || 0 }}</div>
      </article>
      <article class="stats-card">
        <div class="stats-label">知识命中率</div>
        <div class="stats-value stats-value--small">{{ formatPercent(report.citationHitRate) }}</div>
      </article>
      <article class="stats-card">
        <div class="stats-label">环比</div>
        <div class="stats-value stats-value--small">{{ formatDelta(report.monthOverMonthRate) }}</div>
      </article>
      <article class="stats-card">
        <div class="stats-label">同比</div>
        <div class="stats-value stats-value--small">{{ formatDelta(report.yearOverYearRate) }}</div>
      </article>
    </section>

    <section class="trend-layout">
      <section class="panel">
        <div class="panel-title">本年趋势</div>
        <div class="panel-hint">{{ state.query.year }} 年 1 月到 12 月会话量</div>
        <div v-if="state.loading" class="state-block">加载中...</div>
        <div v-else class="trend-list">
          <div v-for="item in report.monthlySessions || []" :key="item.label" class="trend-item">
            <span class="trend-label">{{ monthLabel(item.label) }}</span>
            <div class="trend-bar-wrap">
              <div class="trend-bar" :style="{ width: `${barWidth(item.value, report.monthlySessions)}%` }"></div>
            </div>
            <strong>{{ item.value || 0 }}</strong>
          </div>
        </div>
      </section>

      <section class="panel">
        <div class="panel-title">去年对比</div>
        <div class="panel-hint">{{ state.query.year }} 年与上一年同月对比</div>
        <div v-if="state.loading" class="state-block">加载中...</div>
        <div v-else class="compare-list">
          <div v-for="item in compareRows" :key="item.month" class="compare-item">
            <strong>{{ item.month }}</strong>
            <span>今年 {{ item.current }}</span>
            <span>去年 {{ item.previous }}</span>
            <span>同比 {{ formatDelta(item.yoy) }}</span>
          </div>
        </div>
      </section>
    </section>

    <section class="rank-layout">
      <section class="panel">
        <div class="panel-title">Skill 排行</div>
        <div v-if="!(report.skillRanking || []).length" class="state-block">暂无数据</div>
        <div v-else class="rank-list">
          <div v-for="item in report.skillRanking" :key="item.label" class="rank-item">
            <strong>{{ item.label }}</strong>
            <span>会话 {{ item.sessionCount || 0 }} / 消息 {{ item.messageCount || 0 }}</span>
          </div>
        </div>
      </section>

      <section class="panel">
        <div class="panel-title">知识库排行</div>
        <div v-if="!(report.baseRanking || []).length" class="state-block">暂无数据</div>
        <div v-else class="rank-list">
          <div v-for="item in report.baseRanking" :key="item.label" class="rank-item">
            <strong>{{ item.label }}</strong>
            <span>会话 {{ item.sessionCount || 0 }} / 消息 {{ item.messageCount || 0 }}</span>
          </div>
        </div>
      </section>

      <section class="panel">
        <div class="panel-title">用户排行</div>
        <div v-if="!(report.userRanking || []).length" class="state-block">暂无数据</div>
        <div v-else class="rank-list">
          <div v-for="item in report.userRanking" :key="item.label" class="rank-item">
            <strong>{{ item.label }}</strong>
            <span>会话 {{ item.sessionCount || 0 }} / 消息 {{ item.messageCount || 0 }}</span>
          </div>
        </div>
      </section>
    </section>

    <section class="panel">
      <div class="panel-title">专家维度</div>
      <div class="panel-hint">用于确认高频专家、命中率与手机端来源统计是否正常承接。</div>
      <div v-if="!(report.expertRanking || []).length" class="state-block">暂无数据</div>
      <div v-else class="expert-table">
        <div class="expert-row expert-row--head">
          <span>专家</span>
          <span>等级</span>
          <span>会话</span>
          <span>AI 回复</span>
          <span>引用回复</span>
          <span>命中率</span>
        </div>
        <div v-for="item in report.expertRanking" :key="`${item.label}-${item.expertLevel}`" class="expert-row">
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
    assistantMessageCount: 0,
    citedMessageCount: 0,
    citationHitRate: 0,
    activeMonthCount: 0,
    averageMonthlySessions: 0,
    currentMonthSessionCount: 0,
    monthOverMonthRate: 0,
    yearOverYearRate: 0,
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
    sourceScene: '',
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
    sourceScene: state.query.sourceScene || undefined,
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
  state.permissions = ensureSuccess(await queryCurrentAiPermission(), '加载权限失败')
}

async function fetchOptions() {
  state.skillOptions = ensureSuccess(await querySkillList({ publishStatus: 'PUBLISHED', status: 1 }), '加载 Skill 列表失败') || []
  if (canManage.value) {
    const userData = ensureSuccess(await queryUserPageApi({ pageNo: 1, pageSize: 200 }), '加载用户列表失败')
    state.userOptions = Array.isArray(userData?.list) ? userData.list : []
  } else {
    state.userOptions = []
  }
}

async function fetchReport() {
  state.loading = true
  try {
    state.report = ensureSuccess(await queryAgentMonthlyReport(buildPayload()), '加载月度报表失败') || createReport()
  } catch (error) {
    showToast(error.message || '加载月度报表失败')
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
    showToast('导出成功')
  } catch (error) {
    showToast(error.message || '导出失败')
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
    showToast(error.message || '初始化月报失败')
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

.compact-field,
.filter-field {
  display: grid;
  gap: 8px;
}

.compact-field {
  grid-template-columns: auto auto;
  align-items: center;
  padding: 10px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  background: #fff;
}

.compact-field select,
.filter-field select,
.filter-field input {
  border: 0;
  outline: none;
  background: transparent;
  font: inherit;
}

.panel {
  padding: 16px;
  margin-bottom: 16px;
  border: 1px solid #e5e7eb;
  border-radius: 14px;
  background: #fff;
}

.panel-title {
  color: #111827;
  font-size: 16px;
  font-weight: 700;
}

.panel-hint,
.stats-label,
.rank-item span,
.compare-item span,
.trend-label {
  color: #6b7280;
  font-size: 13px;
}

.filter-grid,
.stats-grid,
.trend-layout,
.rank-layout {
  display: grid;
  gap: 16px;
  margin-top: 12px;
}

.filter-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.filter-field {
  min-width: 0;
  padding: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  background: #f8fafc;
}

.filter-field--wide {
  grid-column: span 2;
}

.stats-grid {
  grid-template-columns: repeat(6, minmax(0, 1fr));
}

.stats-card {
  padding: 16px;
  border: 1px solid #e5e7eb;
  border-radius: 14px;
  background: #fff;
}

.stats-value {
  margin-top: 10px;
  color: #111827;
  font-size: 28px;
  font-weight: 700;
}

.stats-value--small {
  font-size: 18px;
}

.trend-layout,
.rank-layout {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.rank-layout {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.trend-list,
.compare-list,
.rank-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 12px;
}

.trend-item {
  display: grid;
  grid-template-columns: 56px 1fr auto;
  gap: 10px;
  align-items: center;
}

.trend-bar-wrap {
  height: 10px;
  border-radius: 999px;
  background: #e5e7eb;
  overflow: hidden;
}

.trend-bar {
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #0ea5e9, #2563eb);
}

.compare-item,
.rank-item,
.expert-row {
  display: grid;
  gap: 8px;
  padding: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  background: #f8fafc;
}

.expert-table {
  display: grid;
  gap: 10px;
  margin-top: 12px;
}

.expert-row {
  grid-template-columns: 2fr repeat(5, minmax(0, 1fr));
  align-items: center;
}

.expert-row--head {
  background: #eff6ff;
  color: #1d4ed8;
  font-weight: 700;
}

.state-block {
  margin-top: 12px;
  color: #6b7280;
}

@media (max-width: 1100px) {
  .stats-grid,
  .rank-layout {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .filter-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .filter-grid,
  .stats-grid,
  .trend-layout,
  .rank-layout {
    grid-template-columns: 1fr;
  }

  .filter-field--wide {
    grid-column: span 1;
  }

  .expert-row {
    grid-template-columns: 1fr;
  }
}
</style>
