<template>
  <AppPageShell title="考勤统计" description="查看当前数据范围内的应到、异常、非工作日加班情况，以及本周出勤概览。" help-key="attendance">
    <template #actions>
      <van-button plain type="primary" :disabled="loading" @click="fetchStats">刷新统计</van-button>
    </template>

    <AttendanceWorkspaceTabs />

    <section class="panel">
      <div class="panel-title">统计日期</div>
      <div class="panel-actions">
        <input v-model="queryDate" class="field-input field-input--date" type="date" :disabled="loading">
        <van-button type="primary" :loading="loading" @click="fetchStats">更新统计</van-button>
      </div>
      <div class="panel-meta">统计范围：{{ stats.scopeDescription || '-' }}</div>
      <div v-if="stats.nonWorkdayNotice" class="notice-card">{{ stats.nonWorkdayNotice }}</div>
    </section>

    <section class="panel">
      <div class="panel-title">今日概览</div>
      <div class="summary-grid">
        <div class="summary-card">
          <div class="summary-card__label">今日应到</div>
          <div class="summary-card__value">{{ stats.shouldAttendCount ?? 0 }}</div>
        </div>
        <div class="summary-card">
          <div class="summary-card__label">已打卡</div>
          <div class="summary-card__value">{{ stats.checkedInCount ?? 0 }}</div>
        </div>
        <div class="summary-card">
          <div class="summary-card__label">未打卡</div>
          <div class="summary-card__value">{{ stats.missingCount ?? 0 }}</div>
        </div>
        <div class="summary-card summary-card--warning">
          <div class="summary-card__label">迟到人数</div>
          <div class="summary-card__value">{{ stats.lateCount ?? 0 }}</div>
        </div>
        <div class="summary-card summary-card--warning">
          <div class="summary-card__label">早退人数</div>
          <div class="summary-card__value">{{ stats.earlyLeaveCount ?? 0 }}</div>
        </div>
        <div class="summary-card summary-card--accent">
          <div class="summary-card__label">非工作日打卡</div>
          <div class="summary-card__value">{{ stats.overtimeCount ?? 0 }}</div>
        </div>
      </div>
    </section>

    <section class="panel">
      <div class="panel-title">本周概览</div>
      <div class="week-list">
        <div v-for="item in stats.weeklyOverview || []" :key="item.date" class="week-item">
          <div>
            <div class="week-item__title">{{ item.date }}</div>
            <div class="week-item__meta">{{ item.workday ? '工作日' : '非工作日' }}</div>
          </div>
          <div class="week-item__stats">
            <span>应到 {{ item.shouldAttendCount ?? 0 }}</span>
            <span>已打卡 {{ item.checkedInCount ?? 0 }}</span>
            <span>未打卡 {{ item.missingCount ?? 0 }}</span>
            <span>迟到 {{ item.lateCount ?? 0 }}</span>
            <span>早退 {{ item.earlyLeaveCount ?? 0 }}</span>
            <span>加班 {{ item.overtimeCount ?? 0 }}</span>
          </div>
        </div>
      </div>
    </section>

    <section class="panel">
      <div class="panel-title">未打卡人员</div>
      <van-empty v-if="!stats.missingUsers?.length" description="当前没有未打卡人员" />
      <div v-else class="member-list">
        <article v-for="item in stats.missingUsers" :key="`missing-${item.userId}`" class="member-card">
          <div class="member-card__title">{{ item.realName || item.username || '-' }}</div>
          <div class="member-card__meta">{{ item.unitName || '-' }}</div>
          <div class="member-card__meta">{{ item.abnormalReason || '工作日应到未打卡' }}</div>
        </article>
      </div>
    </section>

    <section class="panel">
      <div class="panel-title">异常打卡人员</div>
      <van-empty v-if="!stats.abnormalUsers?.length" description="当前没有迟到或早退人员" />
      <div v-else class="member-list">
        <article v-for="item in stats.abnormalUsers" :key="`abnormal-${item.userId}`" class="member-card member-card--warning">
          <div class="member-card__title">{{ item.realName || item.username || '-' }} · {{ item.statusLabel || '-' }}</div>
          <div class="member-card__meta">{{ item.unitName || '-' }}</div>
          <div class="member-card__meta">上班：{{ formatDateTime(item.checkInTime) }} / 下班：{{ formatDateTime(item.checkOutTime) }}</div>
          <div class="member-card__meta">{{ item.abnormalReason || '-' }}</div>
        </article>
      </div>
    </section>

    <section class="panel">
      <div class="panel-title">非工作日打卡摘要</div>
      <van-empty v-if="!stats.overtimeUsers?.length" description="当前没有非工作日打卡记录" />
      <div v-else class="member-list">
        <article v-for="item in stats.overtimeUsers" :key="`overtime-${item.userId}`" class="member-card member-card--accent">
          <div class="member-card__title">{{ item.realName || item.username || '-' }}</div>
          <div class="member-card__meta">{{ item.unitName || '-' }}</div>
          <div class="member-card__meta">上班：{{ formatDateTime(item.checkInTime) }} / 下班：{{ formatDateTime(item.checkOutTime) }}</div>
        </article>
      </div>
    </section>

    <section class="panel">
      <div class="panel-title">按人员查看最近状态</div>
      <van-empty v-if="!stats.recentMembers?.length" description="暂无人员状态数据" />
      <div v-else class="member-list">
        <article v-for="item in stats.recentMembers" :key="`recent-${item.userId}`" class="member-card">
          <div class="member-card__title">{{ item.realName || item.username || '-' }} · {{ item.statusLabel || '-' }}</div>
          <div class="member-card__meta">{{ item.unitName || '-' }}</div>
          <div class="member-card__meta">上班：{{ formatDateTime(item.checkInTime) }} / 下班：{{ formatDateTime(item.checkOutTime) }}</div>
          <div v-if="item.abnormalReason" class="member-card__meta">{{ item.abnormalReason }}</div>
        </article>
      </div>
    </section>
  </AppPageShell>
</template>

<script setup>
import { onMounted, ref, reactive } from 'vue'
import AppPageShell from '@/components/layout/AppPageShell.vue'
import AttendanceWorkspaceTabs from '@/components/attendance/AttendanceWorkspaceTabs.vue'
import { queryAttendanceTeamStatisticsApi } from '@/api/attendance'

const loading = ref(false)
const queryDate = ref(todayDate())
const stats = reactive({
  scopeDescription: '',
  shouldAttendCount: 0,
  checkedInCount: 0,
  missingCount: 0,
  lateCount: 0,
  earlyLeaveCount: 0,
  overtimeCount: 0,
  nonWorkdayNotice: '',
  missingUsers: [],
  abnormalUsers: [],
  overtimeUsers: [],
  recentMembers: [],
  weeklyOverview: []
})

onMounted(() => {
  fetchStats()
})

async function fetchStats() {
  loading.value = true
  try {
    const response = await queryAttendanceTeamStatisticsApi({
      date: queryDate.value
    })
    const data = ensureSuccess(response) || {}
    stats.scopeDescription = data.scopeDescription || ''
    stats.shouldAttendCount = Number(data.shouldAttendCount || 0)
    stats.checkedInCount = Number(data.checkedInCount || 0)
    stats.missingCount = Number(data.missingCount || 0)
    stats.lateCount = Number(data.lateCount || 0)
    stats.earlyLeaveCount = Number(data.earlyLeaveCount || 0)
    stats.overtimeCount = Number(data.overtimeCount || 0)
    stats.nonWorkdayNotice = data.nonWorkdayNotice || ''
    stats.missingUsers = Array.isArray(data.missingUsers) ? data.missingUsers : []
    stats.abnormalUsers = Array.isArray(data.abnormalUsers) ? data.abnormalUsers : []
    stats.overtimeUsers = Array.isArray(data.overtimeUsers) ? data.overtimeUsers : []
    stats.recentMembers = Array.isArray(data.recentMembers) ? data.recentMembers : []
    stats.weeklyOverview = Array.isArray(data.weeklyOverview) ? data.weeklyOverview : []
  } finally {
    loading.value = false
  }
}

function ensureSuccess(response) {
  if (!response || response.code !== 0) {
    throw new Error(response?.message || '请求失败')
  }
  return response.data
}

function formatDateTime(value) {
  if (!value) {
    return '-'
  }
  return String(value).replace('T', ' ').slice(0, 19)
}

function todayDate() {
  const now = new Date()
  const year = now.getFullYear()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}
</script>

<style scoped>
.panel {
  margin-bottom: 16px;
  padding: 14px;
  border-radius: 18px;
  background: linear-gradient(180deg, #ffffff, #f8fafc);
  border: 1px solid #e2e8f0;
}

.panel-title {
  font-size: 15px;
  font-weight: 700;
  color: #0f172a;
}

.panel-meta {
  margin-top: 10px;
  font-size: 13px;
  color: #64748b;
}

.panel-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.field-input {
  padding: 10px 12px;
  border: 1px solid #dbe3ef;
  border-radius: 12px;
  background: #fff;
  color: #0f172a;
}

.field-input--date {
  min-width: 180px;
}

.notice-card {
  margin-top: 12px;
  padding: 12px;
  border-radius: 14px;
  background: #eff6ff;
  color: #1d4ed8;
  font-size: 13px;
  line-height: 1.7;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 12px;
  margin-top: 12px;
}

.summary-card {
  padding: 14px;
  border-radius: 16px;
  background: #fff;
  border: 1px solid #dbe3ef;
}

.summary-card--warning {
  background: #fff7ed;
  border-color: #fdba74;
}

.summary-card--accent {
  background: #eff6ff;
  border-color: #93c5fd;
}

.summary-card__label {
  font-size: 12px;
  color: #64748b;
}

.summary-card__value {
  margin-top: 8px;
  font-size: 26px;
  font-weight: 700;
  color: #0f172a;
}

.week-list,
.member-list {
  display: grid;
  gap: 12px;
  margin-top: 12px;
}

.week-item,
.member-card {
  padding: 14px;
  border-radius: 16px;
  border: 1px solid #dbe3ef;
  background: #fff;
}

.member-card--warning {
  background: #fff7ed;
}

.member-card--accent {
  background: #eff6ff;
}

.week-item__title,
.member-card__title {
  font-size: 15px;
  font-weight: 700;
  color: #0f172a;
}

.week-item__meta,
.member-card__meta {
  margin-top: 6px;
  font-size: 13px;
  line-height: 1.6;
  color: #64748b;
}

.week-item__stats {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 12px;
  margin-top: 10px;
  font-size: 13px;
  color: #334155;
}
</style>
