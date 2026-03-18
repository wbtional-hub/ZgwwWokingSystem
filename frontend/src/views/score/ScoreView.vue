<template>
  <AppPageShell title="工作评分" description="当前页已接入最小评分体系：基于考勤和周报按周生成评分结果。">
    <template #actions>
      <div class="action-row">
        <van-button v-if="isAdmin" type="primary" :loading="state.calculating" :disabled="pageBusy" @click="handleCalculate">计算评分</van-button>
        <van-button v-if="isAdmin" type="success" :loading="state.exporting" :disabled="pageBusy" @click="handleExportReport">导出通报</van-button>
        <van-button plain type="primary" :loading="state.loading" :disabled="pageBusy" @click="fetchList">刷新列表</van-button>
      </div>
    </template>

    <section class="panel">
      <div class="panel-title">查询区</div>
      <div class="query-grid">
        <van-field v-model.trim="state.queryForm.weekNo" label="周次" placeholder="如：2026-W12" :disabled="pageBusy" />
        <van-field v-model.trim="state.queryForm.unitName" label="组织" placeholder="请输入组织名称" :disabled="pageBusy" />
        <div class="select-field">
          <span class="select-label">状态</span>
          <select v-model="state.queryForm.status" :disabled="pageBusy">
            <option value="">全部</option>
            <option value="NORMAL">正常</option>
            <option value="YELLOW">黄牌</option>
            <option value="RED">红牌</option>
          </select>
        </div>
      </div>
      <div class="panel-actions">
        <van-button size="small" type="primary" :loading="state.loading" :disabled="pageBusy" @click="handleSearch">查询</van-button>
        <van-button size="small" plain :disabled="pageBusy" @click="handleReset">重置</van-button>
        <van-button size="small" plain :type="state.queryForm.sortByTotalDesc ? 'success' : 'default'" :disabled="pageBusy" @click="toggleSort">
          {{ state.queryForm.sortByTotalDesc ? '当前按总分排序' : '切换按总分排序' }}
        </van-button>
        <van-button size="small" plain type="danger" :disabled="pageBusy" @click="showRedList">红牌名单</van-button>
      </div>
    </section>

    <section class="panel">
      <div class="panel-title">Top10 优秀榜</div>
      <van-empty v-if="!topTenList.length" description="暂无优秀榜数据" />
      <div v-else class="mini-list">
        <div v-for="item in topTenList" :key="item.id" class="mini-item">
          <span>#{{ item.level || '-' }} {{ item.realName || item.username }}</span>
          <span>{{ scoreText(item.totalScore) }} 分</span>
        </div>
      </div>
    </section>

    <section class="panel">
      <div class="panel-title">组织平均分</div>
      <van-empty v-if="!orgAverageList.length" description="暂无组织平均分数据" />
      <div v-else class="mini-list">
        <div v-for="item in orgAverageList" :key="item.unitName" class="mini-item">
          <span>{{ item.unitName }}</span>
          <span>{{ scoreText(item.averageScore) }} 分 / {{ item.count }} 人</span>
        </div>
      </div>
    </section>

    <section class="panel">
      <div class="panel-title">评分列表</div>
      <div class="panel-hint">共 {{ state.list.length }} 条评分结果</div>

      <van-loading v-if="state.loading" class="state-block" size="24px" vertical>加载中...</van-loading>
      <van-empty v-else-if="!state.list.length" description="暂无评分结果">
        <template #default>
          <div class="panel-hint">请先选择周次并点击“计算评分”。</div>
        </template>
      </van-empty>

      <div v-else class="list-wrap">
        <van-card v-for="item in state.list" :key="item.id" class="score-card">
          <template #title>
            <div class="score-title">
              <span>#{{ item.level || '-' }} {{ item.realName || item.username }} · {{ item.weekNo }}</span>
              <div class="tag-row">
                <van-tag :type="statusTagType(item.status)">{{ statusLabel(item.status) }}</van-tag>
                <van-tag type="primary">总分 {{ scoreText(item.totalScore) }}</van-tag>
              </div>
            </div>
          </template>
          <template #desc>
            <div class="score-meta">组织：{{ item.unitName || '-' }}</div>
            <div class="score-meta">账号：{{ item.username || '-' }}</div>
            <div class="score-meta">考勤：{{ scoreText(item.attendanceScore) }}（{{ item.attendanceDays || 0 }} 天）</div>
            <div class="score-meta">周报：{{ scoreText(item.weeklyWorkScore) }}（{{ weeklyStatusLabel(item.weeklyWorkStatus) }}）</div>
            <div class="score-meta">纪律：{{ scoreText(item.disciplineScore) }}</div>
            <div class="score-meta">计算时间：{{ formatDateTime(item.calculateTime) }}</div>
          </template>
          <template #footer>
            <div class="action-row">
              <van-button size="small" plain type="primary" :disabled="pageBusy" @click="openDetail(item)">查看详情</van-button>
            </div>
          </template>
        </van-card>
      </div>
    </section>

    <van-popup v-model:show="state.detailVisible" position="bottom" round :close-on-click-overlay="!state.detailLoading">
      <div class="detail-panel">
        <div class="panel-title">评分详情</div>
        <van-loading v-if="state.detailLoading" class="state-block" size="24px" vertical>加载中...</van-loading>
        <template v-else-if="state.detailRecord">
          <div class="score-meta">周次：{{ state.detailRecord.weekNo }}</div>
          <div class="score-meta">姓名：{{ state.detailRecord.realName || state.detailRecord.username || '-' }}</div>
          <div class="score-meta">组织：{{ state.detailRecord.unitName || '-' }}</div>
          <div class="score-meta">排名：{{ state.detailRecord.level || '-' }}</div>
          <div class="score-meta">状态：{{ statusLabel(state.detailRecord.status) }}</div>
          <div class="score-meta">考勤得分：{{ scoreText(state.detailRecord.attendanceScore) }}</div>
          <div class="score-meta">考勤天数：{{ state.detailRecord.attendanceDays || 0 }}</div>
          <div class="score-meta">周报得分：{{ scoreText(state.detailRecord.weeklyWorkScore) }}</div>
          <div class="score-meta">周报状态：{{ weeklyStatusLabel(state.detailRecord.weeklyWorkStatus) }}</div>
          <div class="score-meta">纪律得分：{{ scoreText(state.detailRecord.disciplineScore) }}</div>
          <div class="score-meta">纪律说明：{{ state.detailRecord.disciplineRemark || '-' }}</div>
          <div class="score-meta">总分：{{ scoreText(state.detailRecord.totalScore) }}</div>
        </template>
      </div>
    </van-popup>
  </AppPageShell>
</template>

<script setup>
import { computed, onMounted, reactive } from 'vue'
import { showToast } from 'vant'
import AppPageShell from '@/components/layout/AppPageShell.vue'
import { calculateWorkScore, exportWorkScoreReport, queryWorkScoreDetail, queryWorkScoreList } from '@/api/score'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

const state = reactive({
  loading: false,
  calculating: false,
  exporting: false,
  detailLoading: false,
  detailVisible: false,
  detailRecord: null,
  list: [],
  queryForm: {
    weekNo: getCurrentWeekNo(),
    unitName: '',
    status: '',
    sortByTotalDesc: true
  }
})

const pageBusy = computed(() => state.loading || state.calculating || state.exporting || state.detailLoading)
const isAdmin = computed(() => {
  const role = userStore.userInfo?.role || (userStore.userInfo?.superAdmin ? 'ADMIN' : 'USER')
  return role === 'ADMIN'
})
const topTenList = computed(() => {
  return [...state.list]
    .filter((item) => item.status === 'NORMAL')
    .sort((a, b) => Number(a.level || 9999) - Number(b.level || 9999))
    .slice(0, 10)
})
const orgAverageList = computed(() => {
  const map = new Map()
  state.list.forEach((item) => {
    const key = item.unitName || '未分组'
    const current = map.get(key) || { unitName: key, total: 0, count: 0 }
    current.total += Number(item.totalScore || 0)
    current.count += 1
    map.set(key, current)
  })
  return Array.from(map.values())
    .map((item) => ({
      unitName: item.unitName,
      count: item.count,
      averageScore: item.count ? item.total / item.count : 0
    }))
    .sort((a, b) => b.averageScore - a.averageScore)
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

function statusLabel(status) {
  return {
    NORMAL: '正常',
    YELLOW: '黄牌',
    RED: '红牌'
  }[status] || '未标记'
}

function statusTagType(status) {
  return {
    NORMAL: 'success',
    YELLOW: 'warning',
    RED: 'danger'
  }[status] || 'default'
}

function weeklyStatusLabel(status) {
  return {
    DRAFT: '草稿',
    SUBMITTED: '已提交',
    APPROVED: '已通过',
    RETURNED: '已退回'
  }[status] || '未提交'
}

function formatDateTime(value) {
  if (!value) {
    return '-'
  }
  return String(value).replace('T', ' ')
}

async function fetchList() {
  state.loading = true
  try {
    const response = await queryWorkScoreList({
      weekNo: state.queryForm.weekNo || undefined,
      unitName: state.queryForm.unitName || undefined,
      status: state.queryForm.status || undefined,
      sortByTotalDesc: state.queryForm.sortByTotalDesc
    })
    state.list = Array.isArray(response.data) ? response.data : []
  } catch (error) {
    showToast(error.message || '评分列表加载失败')
  } finally {
    state.loading = false
  }
}

function handleSearch() {
  fetchList()
}

function handleReset() {
  state.queryForm.weekNo = getCurrentWeekNo()
  state.queryForm.unitName = ''
  state.queryForm.status = ''
  state.queryForm.sortByTotalDesc = true
  fetchList()
}

function toggleSort() {
  state.queryForm.sortByTotalDesc = !state.queryForm.sortByTotalDesc
  fetchList()
}

function showRedList() {
  state.queryForm.status = 'RED'
  fetchList()
}

async function handleCalculate() {
  if (!state.queryForm.weekNo) {
    showToast('请先填写周次')
    return
  }
  state.calculating = true
  try {
    const response = await calculateWorkScore({ weekNo: state.queryForm.weekNo })
    showToast(`评分计算完成，共 ${response.data?.calculatedCount || 0} 人`)
    await fetchList()
  } catch (error) {
    showToast(error.message || '评分计算失败')
  } finally {
    state.calculating = false
  }
}

async function handleExportReport() {
  if (!state.queryForm.weekNo) {
    showToast('请先填写周次')
    return
  }
  state.exporting = true
  try {
    const blob = await exportWorkScoreReport(state.queryForm.weekNo)
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `weekly-score-report-${state.queryForm.weekNo}.docx`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    showToast('通报导出成功')
  } catch (error) {
    showToast(error.message || '通报导出失败')
  } finally {
    state.exporting = false
  }
}

async function openDetail(item) {
  state.detailVisible = true
  state.detailLoading = true
  state.detailRecord = null
  try {
    const response = await queryWorkScoreDetail(item.id)
    state.detailRecord = response.data || null
  } catch (error) {
    showToast(error.message || '评分详情加载失败')
    state.detailVisible = false
  } finally {
    state.detailLoading = false
  }
}

onMounted(() => {
  fetchList()
})
</script>

<style scoped>
.action-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.query-grid {
  display: grid;
  gap: 8px;
}

.panel {
  margin-bottom: 16px;
  padding: 12px;
  background: #fff;
  border-radius: 12px;
}

.panel-title {
  margin-bottom: 8px;
  font-size: 15px;
  font-weight: 600;
  color: #1f2937;
}

.panel-hint,
.score-meta {
  font-size: 13px;
  color: #4b5563;
  line-height: 1.6;
}

.panel-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
}

.list-wrap {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.score-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.select-field {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 10px 0;
  padding: 10px 12px;
  border: 1px solid #ebedf0;
  border-radius: 8px;
}

.select-label {
  flex: 0 0 56px;
  font-size: 14px;
  color: #323233;
}

.select-field select {
  flex: 1;
  border: 0;
  background: transparent;
  font-size: 14px;
}

.mini-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.mini-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  color: #374151;
  font-size: 13px;
}

.detail-panel {
  padding: 16px;
  background: #fff;
  border-radius: 16px 16px 0 0;
}

.state-block {
  padding: 20px 0;
}
</style>
