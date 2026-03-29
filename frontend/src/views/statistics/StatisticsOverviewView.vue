<template>
  <AppPageShell title="统计分析" description="当前页已接入领导判断所需的最小统计视图：概览、排名、红黄牌和近4周趋势。">
    <template #title-extra>
      <PageHelp page-key="statistics" />
    </template>
    <template #actions>
      <div class="action-row">
        <van-button type="primary" :loading="state.loading" :disabled="state.loading" @click="handleRefresh">刷新统计</van-button>
      </div>
    </template>

    <section class="panel" data-guide="statistics-query">
      <div class="panel-title">查询区</div>
      <div class="query-grid">
        <van-field v-model.trim="state.queryForm.weekNo" label="周次" placeholder="如：2026-W12" :disabled="state.loading" />
        <van-field v-model.trim="state.queryForm.unitName" label="组织" placeholder="请输入组织名称" :disabled="state.loading" />
      </div>
      <div class="panel-actions">
        <van-button size="small" type="primary" :loading="state.loading" :disabled="state.loading" @click="handleSearch">查询</van-button>
        <van-button size="small" plain :disabled="state.loading" @click="handleReset">重置</van-button>
      </div>
    </section>

    <section class="panel">
      <div class="panel-title">当前查询工作面</div>
      <div class="scope-card" :class="{ 'scope-card--filtered': hasActiveFilters }">
        <div class="scope-status-row">
          <span class="scope-status-badge" :class="hasActiveFilters ? 'scope-status-badge--filtered' : 'scope-status-badge--default'">
            {{ currentViewModeText }}
          </span>
          <span class="scope-status-note">{{ filterPresenceText }}</span>
        </div>
        <div class="scope-summary">{{ currentScopeText }}</div>
        <div class="scope-tag-list">
          <span class="scope-tag">周次：{{ activeWeekLabel }}</span>
          <span class="scope-tag">组织：{{ activeUnitLabel }}</span>
          <span class="scope-tag">趋势：{{ trendScopeLabel }}</span>
          <span class="scope-tag">{{ filterTagText }}</span>
        </div>
      </div>
      <div class="panel-actions">
        <van-button
          size="small"
          plain
          :disabled="state.loading || !hasActiveFilters"
          :class="{ 'panel-action-button--muted': !hasActiveFilters }"
          @click="clearCurrentFilters"
        >
          清空当前查询条件
        </van-button>
      </div>
    </section>

    <section class="panel">
      <div class="panel-title">本次查询结果说明</div>
      <div class="result-hint-card" :class="{ 'result-hint-card--loading': state.loading, 'result-hint-card--filtered': hasActiveFilters }">
        <div class="result-hint-title">{{ currentResultHintText }}</div>
        <div class="result-hint-desc">{{ currentResultHintDescription }}</div>
      </div>
    </section>

    <section class="panel" data-guide="statistics-overview">
      <div class="panel-title">总体概览</div>
      <div class="panel-hint">{{ currentScopeText }}</div>
      <div v-if="state.loading" class="panel-feedback panel-feedback--loading">总体概览加载中，请稍候...</div>
      <van-empty v-else-if="isOverviewEmpty" :description="overviewEmptyText" />
      <div v-else class="summary-grid">
        <div class="summary-card">
          <div class="summary-label">平均分</div>
          <div class="summary-value">{{ scoreText(state.overview.averageScore) }}</div>
        </div>
        <div class="summary-card">
          <div class="summary-label">红牌人数</div>
          <div class="summary-value">{{ state.overview.redCount || 0 }}</div>
        </div>
        <div class="summary-card">
          <div class="summary-label">黄牌人数</div>
          <div class="summary-value">{{ state.overview.yellowCount || 0 }}</div>
        </div>
        <div class="summary-card">
          <div class="summary-label">正常人数</div>
          <div class="summary-value">{{ state.overview.normalCount || 0 }}</div>
        </div>
        <div class="summary-card">
          <div class="summary-label">覆盖率</div>
          <div class="summary-value">{{ percentText(state.overview.coverageRate) }}</div>
        </div>
      </div>
    </section>

    <section class="panel">
      <div class="panel-title">组织排名</div>
      <div class="panel-hint">{{ currentScopeText }}</div>
      <div v-if="state.loading" class="panel-feedback panel-feedback--loading">组织排名加载中，请稍候...</div>
      <van-empty v-else-if="isRankingEmpty" :description="rankingEmptyText" />
      <div v-else class="table-wrap">
        <table class="simple-table">
          <thead>
            <tr>
              <th>排名</th>
              <th>组织</th>
              <th>平均分</th>
              <th>红牌人数</th>
              <th>覆盖人数</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(item, index) in state.orgRank" :key="`${item.unitId || 'none'}-${index}`">
              <td>{{ index + 1 }}</td>
              <td>{{ item.unitName || '未分组' }}</td>
              <td>{{ scoreText(item.averageScore) }}</td>
              <td>{{ item.redCount || 0 }}</td>
              <td>{{ item.scoreCount || 0 }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <section class="panel">
      <div class="panel-title">红牌名单</div>
      <div class="panel-hint">{{ currentScopeText }}</div>
      <div v-if="state.loading" class="panel-feedback panel-feedback--loading">红牌名单加载中，请稍候...</div>
      <van-empty v-else-if="isRedListEmpty" :description="redListEmptyText" />
      <div v-else class="table-wrap">
        <table class="simple-table">
          <thead>
            <tr>
              <th>姓名</th>
              <th>组织</th>
              <th>总分</th>
              <th>排名</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in state.redList" :key="item.id">
              <td>{{ item.realName || item.username || '-' }}</td>
              <td>{{ item.unitName || '-' }}</td>
              <td>{{ scoreText(item.totalScore) }}</td>
              <td>{{ item.level || '-' }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <section class="panel">
      <div class="panel-title">黄牌名单</div>
      <div class="panel-hint">{{ currentScopeText }}</div>
      <div v-if="state.loading" class="panel-feedback panel-feedback--loading">黄牌名单加载中，请稍候...</div>
      <van-empty v-else-if="isYellowListEmpty" :description="yellowListEmptyText" />
      <div v-else class="table-wrap">
        <table class="simple-table">
          <thead>
            <tr>
              <th>姓名</th>
              <th>组织</th>
              <th>总分</th>
              <th>排名</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in state.yellowList" :key="item.id">
              <td>{{ item.realName || item.username || '-' }}</td>
              <td>{{ item.unitName || '-' }}</td>
              <td>{{ scoreText(item.totalScore) }}</td>
              <td>{{ item.level || '-' }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <section class="panel" data-guide="statistics-trend">
      <div class="panel-title">最近4周趋势</div>
      <div class="panel-hint">{{ trendPanelHint }}</div>
      <div v-if="state.loading" class="panel-feedback panel-feedback--loading">趋势数据加载中，请稍候...</div>
      <van-empty v-else-if="isTrendEmpty" :description="trendEmptyText" />
      <div v-else class="table-wrap">
        <table class="simple-table">
          <thead>
            <tr>
              <th>周次</th>
              <th>平均分</th>
              <th>红牌人数</th>
              <th>黄牌人数</th>
              <th>覆盖人数</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in orderedTrend" :key="item.weekNo">
              <td>{{ item.weekNo }}</td>
              <td>{{ scoreText(item.averageScore) }}</td>
              <td>{{ item.redCount || 0 }}</td>
              <td>{{ item.yellowCount || 0 }}</td>
              <td>{{ item.scoreCount || 0 }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </AppPageShell>
</template>

<script setup>
import { computed, onMounted, reactive } from 'vue'
import { showToast } from 'vant'
import AppPageShell from '@/components/layout/AppPageShell.vue'
import PageHelp from '@/components/PageHelp.vue'
import {
  queryStatisticsOrgRank,
  queryStatisticsOverview,
  queryStatisticsRedList,
  queryStatisticsTrend,
  queryStatisticsYellowList
} from '@/api/statistics'

const state = reactive({
  loading: false,
  lastActionType: 'init',
  queryForm: {
    weekNo: getCurrentWeekNo(),
    unitName: ''
  },
  overview: null,
  orgRank: [],
  redList: [],
  yellowList: [],
  trend: []
})

const activeWeekLabel = computed(() => state.queryForm.weekNo || getCurrentWeekNo())
const activeUnitLabel = computed(() => state.queryForm.unitName?.trim() || '全部组织')
const hasActiveFilters = computed(() => Boolean(state.queryForm.weekNo?.trim() || state.queryForm.unitName?.trim()))
const currentViewModeText = computed(() => (hasActiveFilters.value ? '当前为筛选结果视图' : '当前为默认全量视图'))
const filterPresenceText = computed(() =>
  hasActiveFilters.value ? '当前数据基于已填写查询条件生成，请按筛选结果理解统计内容。' : '当前数据未附加额外筛选条件，展示的是默认全量统计视图。'
)
const filterTagText = computed(() => (hasActiveFilters.value ? '筛选状态：存在有效筛选条件' : '筛选状态：未设置额外筛选条件'))
const currentResultHintText = computed(() => {
  if (state.loading) {
    return '正在按当前条件加载统计数据'
  }
  if (state.lastActionType === 'query') {
    return hasActiveFilters.value ? '已按当前筛选条件加载统计结果' : '已按当前条件重新加载默认全量统计结果'
  }
  if (state.lastActionType === 'reset') {
    return '已恢复默认全量统计视图'
  }
  if (state.lastActionType === 'refresh') {
    return hasActiveFilters.value ? '已重新加载当前条件下的统计结果' : '已重新加载默认全量统计结果'
  }
  return '当前展示默认全量统计结果'
})
const currentResultHintDescription = computed(() => {
  if (state.loading) {
    return hasActiveFilters.value
      ? `正在按周次 ${activeWeekLabel.value}、组织 ${activeUnitLabel.value} 重新加载统计数据，请稍候。`
      : `正在按默认全量视图重新加载统计数据，请稍候。`
  }
  if (state.lastActionType === 'query') {
    return hasActiveFilters.value
      ? `页面各区块已按周次 ${activeWeekLabel.value}、组织 ${activeUnitLabel.value} 同步刷新。`
      : '页面各区块已回到默认全量统计口径，并按当前默认条件同步刷新。'
  }
  if (state.lastActionType === 'reset') {
    return `页面各区块已恢复为默认全量统计视图，当前按周次 ${activeWeekLabel.value} 展示全部组织范围数据。`
  }
  if (state.lastActionType === 'refresh') {
    return hasActiveFilters.value
      ? `页面各区块已按周次 ${activeWeekLabel.value}、组织 ${activeUnitLabel.value} 重新加载完成。`
      : `页面各区块已按默认全量视图重新加载完成，当前展示周次 ${activeWeekLabel.value} 的全部组织范围数据。`
  }
  return `页面当前按默认全量视图展示周次 ${activeWeekLabel.value} 的全部组织范围统计结果。`
})
const isOverviewEmpty = computed(() => !state.overview)
const isRankingEmpty = computed(() => !state.orgRank.length)
const isRedListEmpty = computed(() => !state.redList.length)
const isYellowListEmpty = computed(() => !state.yellowList.length)
const isTrendEmpty = computed(() => !state.trend.length)
const overviewEmptyText = computed(() =>
  hasActiveFilters.value ? '当前筛选条件下暂无概览统计结果，可调整条件后重试。' : '当前暂无统计数据，请稍后刷新查看。'
)
const rankingEmptyText = computed(() =>
  hasActiveFilters.value ? '当前筛选条件下暂无组织排名数据，可调整条件后重试。' : '当前暂无可展示的组织排名数据。'
)
const redListEmptyText = computed(() =>
  hasActiveFilters.value ? '当前筛选条件下暂无红牌名单记录，可调整条件后重试。' : '当前暂无红牌名单记录。'
)
const yellowListEmptyText = computed(() =>
  hasActiveFilters.value ? '当前筛选条件下暂无黄牌名单记录，可调整条件后重试。' : '当前暂无黄牌名单记录。'
)
const trendEmptyText = computed(() =>
  hasActiveFilters.value ? '当前筛选条件下暂无趋势数据，可调整条件后重试。' : '当前暂无趋势统计数据。'
)

const currentScopeText = computed(() => {
  const weekNo = activeWeekLabel.value
  const unitName = state.queryForm.unitName?.trim()
  return unitName
    ? `当前统计口径：周次 ${weekNo}，组织筛选“${unitName}”。`
    : `当前统计口径：周次 ${weekNo}，组织范围为当前数据权限内全部组织。`
})

const trendPanelHint = computed(() => {
  if (!state.trend.length) {
    return `${currentScopeText.value} 趋势按当前周次作为截止周，向前展示最近 4 周。`
  }
  const latestWeek = state.trend[0]?.weekNo || state.queryForm.weekNo || getCurrentWeekNo()
  const oldestWeek = state.trend[state.trend.length - 1]?.weekNo || latestWeek
  return `${currentScopeText.value} 趋势实际展示范围：${oldestWeek} 至 ${latestWeek}，表格按时间正序展示。`
})

const orderedTrend = computed(() => [...state.trend].reverse())
const trendScopeLabel = computed(() => {
  if (!state.trend.length) {
    return '当前周向前最近 4 周'
  }
  const latestWeek = state.trend[0]?.weekNo || activeWeekLabel.value
  const oldestWeek = state.trend[state.trend.length - 1]?.weekNo || latestWeek
  return `${oldestWeek} 至 ${latestWeek}`
})

function getCurrentWeekNo() {
  const now = new Date()
  const target = new Date(now.valueOf())
  const dayNr = (now.getDay() + 6) % 7
  target.setDate(target.getDate() - dayNr + 3)
  const firstThursday = new Date(target.getFullYear(), 0, 4)
  const firstDayNr = (firstThursday.getDay() + 6) % 7
  firstThursday.setDate(firstThursday.getDate() - firstDayNr + 3)
  const week = 1 + Math.round((target - firstThursday) / 604800000)
  return `${target.getFullYear()}-W${String(week).padStart(2, '0')}`
}

function scoreText(value) {
  return value == null ? '0.00' : Number(value).toFixed(2)
}

function percentText(value) {
  return `${Number(value || 0).toFixed(2)}%`
}

async function fetchAll(actionType = 'refresh') {
  state.lastActionType = actionType
  state.loading = true
  try {
    const params = {
      weekNo: state.queryForm.weekNo || undefined,
      unitName: state.queryForm.unitName || undefined
    }
    const [overviewRes, orgRankRes, redListRes, yellowListRes, trendRes] = await Promise.all([
      queryStatisticsOverview(params),
      queryStatisticsOrgRank(params),
      queryStatisticsRedList(params),
      queryStatisticsYellowList(params),
      queryStatisticsTrend({ weekNo: params.weekNo, unitName: params.unitName })
    ])
    state.overview = overviewRes.data || null
    state.orgRank = Array.isArray(orgRankRes.data) ? orgRankRes.data : []
    state.redList = Array.isArray(redListRes.data) ? redListRes.data : []
    state.yellowList = Array.isArray(yellowListRes.data) ? yellowListRes.data : []
    state.trend = Array.isArray(trendRes.data) ? trendRes.data : []
  } catch (error) {
    showToast(error.message || '统计数据加载失败')
  } finally {
    state.loading = false
  }
}

function handleSearch() {
  fetchAll('query')
}

function handleRefresh() {
  fetchAll('refresh')
}

function handleReset() {
  state.queryForm.weekNo = getCurrentWeekNo()
  state.queryForm.unitName = ''
  fetchAll('reset')
}

function clearCurrentFilters() {
  handleReset()
}

onMounted(() => {
  fetchAll('init')
})
</script>

<style scoped>
.action-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.panel {
  padding: 16px;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.06);
}

.panel + .panel {
  margin-top: 16px;
}

.panel-title {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
}

.query-grid {
  display: grid;
  gap: 12px;
  margin-top: 12px;
}

.panel-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.panel-hint {
  margin-top: 8px;
  font-size: 13px;
  line-height: 1.6;
  color: #64748b;
}

.panel-feedback {
  margin-top: 12px;
  padding: 16px;
  border-radius: 10px;
  font-size: 14px;
  line-height: 1.7;
}

.panel-feedback--loading {
  border: 1px dashed #cbd5e1;
  background: #f8fafc;
  color: #475569;
}

.result-hint-card {
  margin-top: 12px;
  padding: 14px 16px;
  border-radius: 10px;
  border: 1px solid #e5e7eb;
  background: #f8fafc;
}

.result-hint-card--filtered {
  border-color: #bfdbfe;
  background: #f8fbff;
}

.result-hint-card--loading {
  border-style: dashed;
}

.result-hint-title {
  color: #111827;
  font-size: 14px;
  font-weight: 600;
  line-height: 1.7;
}

.result-hint-desc {
  margin-top: 6px;
  color: #475569;
  font-size: 13px;
  line-height: 1.7;
}

.scope-card {
  margin-top: 12px;
  padding: 14px 16px;
  border-radius: 10px;
  background: #f8fafc;
  border: 1px solid #e5e7eb;
}

.scope-card--filtered {
  border-color: #bfdbfe;
  background: #f8fbff;
}

.scope-status-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.scope-status-badge {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
}

.scope-status-badge--default {
  background: #e5e7eb;
  color: #374151;
}

.scope-status-badge--filtered {
  background: #dbeafe;
  color: #1d4ed8;
}

.scope-status-note {
  color: #475569;
  font-size: 13px;
  line-height: 1.6;
}

.scope-summary {
  margin-top: 10px;
  color: #111827;
  font-size: 14px;
  line-height: 1.7;
}

.scope-tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
}

.scope-tag {
  display: inline-flex;
  align-items: center;
  min-height: 30px;
  padding: 0 10px;
  border-radius: 999px;
  background: #e2e8f0;
  color: #334155;
  font-size: 12px;
}

:deep(.panel-action-button--muted.van-button--plain) {
  opacity: 0.55;
}

.summary-grid {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  margin-top: 12px;
}

.summary-card {
  padding: 16px;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  background: #f8fafc;
}

.summary-label {
  font-size: 13px;
  color: #64748b;
}

.summary-value {
  margin-top: 8px;
  font-size: 22px;
  font-weight: 700;
  color: #111827;
}

.table-wrap {
  margin-top: 12px;
  overflow-x: auto;
}

.simple-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}

.simple-table th,
.simple-table td {
  padding: 10px 12px;
  border: 1px solid #e5e7eb;
  text-align: left;
  white-space: nowrap;
}

.simple-table th {
  background: #f8fafc;
  color: #374151;
}
</style>
