<template>
  <AppPageShell title="统计分析" description="当前页已接入领导判断所需的最小统计视图：概览、排名、红黄牌和近4周趋势。">
    <template #actions>
      <div class="action-row">
        <van-button type="primary" :loading="state.loading" :disabled="state.loading" @click="fetchAll">刷新统计</van-button>
      </div>
    </template>

    <section class="panel">
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
      <div class="panel-title">总体概览</div>
      <van-empty v-if="!state.overview" description="暂无统计概览" />
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
          <div class="summary-label">优秀人数</div>
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
      <van-empty v-if="!state.orgRank.length" description="暂无组织排名数据" />
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
      <van-empty v-if="!state.redList.length" description="暂无红牌名单" />
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
      <van-empty v-if="!state.yellowList.length" description="暂无黄牌名单" />
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

    <section class="panel">
      <div class="panel-title">最近4周趋势</div>
      <van-empty v-if="!state.trend.length" description="暂无趋势数据" />
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
            <tr v-for="item in state.trend" :key="item.weekNo">
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
import { onMounted, reactive } from 'vue'
import { showToast } from 'vant'
import AppPageShell from '@/components/layout/AppPageShell.vue'
import {
  queryStatisticsOrgRank,
  queryStatisticsOverview,
  queryStatisticsRedList,
  queryStatisticsTrend,
  queryStatisticsYellowList
} from '@/api/statistics'

const state = reactive({
  loading: false,
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

async function fetchAll() {
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
      queryStatisticsTrend({ unitName: params.unitName })
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
  fetchAll()
}

function handleReset() {
  state.queryForm.weekNo = getCurrentWeekNo()
  state.queryForm.unitName = ''
  fetchAll()
}

onMounted(() => {
  fetchAll()
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
