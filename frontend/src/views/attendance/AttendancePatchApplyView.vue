<template>
  <AppPageShell title="补打卡申请" description="员工可在 PC 端提交补上班卡、补下班卡申请，并查看自己的审批进度。" help-key="attendance">
    <template #actions>
      <van-button plain type="primary" :disabled="pageBusy" @click="fetchList">刷新列表</van-button>
    </template>

    <AttendanceWorkspaceTabs />

    <section class="panel">
      <div class="panel-title">提交申请</div>
      <div class="panel-grid">
        <label class="field">
          <span class="field-label">考勤日期</span>
          <input v-model="form.attendanceDate" class="field-input" type="date" :disabled="pageBusy">
        </label>
        <label class="field">
          <span class="field-label">补卡类型</span>
          <select v-model="form.patchType" class="field-input" :disabled="pageBusy">
            <option value="CHECK_IN">补上班卡</option>
            <option value="CHECK_OUT">补下班卡</option>
          </select>
        </label>
        <label class="field">
          <span class="field-label">补卡时间</span>
          <input v-model="form.patchTime" class="field-input" type="datetime-local" :disabled="pageBusy">
        </label>
      </div>
      <label class="field">
        <span class="field-label">补卡原因</span>
        <textarea v-model.trim="form.reason" class="field-textarea" rows="4" maxlength="500" :disabled="pageBusy" placeholder="请填写忘打卡、设备异常、外出等具体原因"></textarea>
      </label>
      <div class="panel-actions">
        <van-button type="primary" :loading="submitting" :disabled="pageBusy" @click="handleSubmit">提交补卡申请</van-button>
      </div>
    </section>

    <section class="panel">
      <div class="panel-title">我的补卡申请</div>
      <div class="panel-grid">
        <label class="field">
          <span class="field-label">状态</span>
          <select v-model="filters.status" class="field-input" :disabled="pageBusy" @change="handleSearch">
            <option value="">全部状态</option>
            <option value="PENDING">待审批</option>
            <option value="APPROVED">已通过</option>
            <option value="REJECTED">已拒绝</option>
          </select>
        </label>
        <label class="field">
          <span class="field-label">开始日期</span>
          <input v-model="filters.dateFrom" class="field-input" type="date" :disabled="pageBusy">
        </label>
        <label class="field">
          <span class="field-label">结束日期</span>
          <input v-model="filters.dateTo" class="field-input" type="date" :disabled="pageBusy">
        </label>
      </div>
      <div class="panel-actions">
        <van-button plain type="primary" :disabled="pageBusy" @click="handleSearch">查询</van-button>
        <van-button plain :disabled="pageBusy" @click="handleReset">重置</van-button>
      </div>

      <van-loading v-if="loading" class="state-block" vertical size="24px">加载中...</van-loading>
      <van-empty v-else-if="!list.length" description="暂无补卡申请记录" />
      <div v-else class="card-list">
        <article v-for="item in list" :key="item.id" class="record-card">
          <div class="record-card__top">
            <div>
              <div class="record-card__title">{{ patchTypeLabel(item.patchType) }} · {{ item.attendanceDate }}</div>
              <div class="record-card__meta">申请时间：{{ formatDateTime(item.patchTime) }}</div>
            </div>
            <van-tag :type="statusTagType(item.status)">{{ statusLabel(item.status) }}</van-tag>
          </div>
          <div class="record-card__reason">{{ item.reason || '-' }}</div>
          <div class="record-card__meta">审批人：{{ item.approveRealName || item.approveUsername || '-' }}</div>
          <div class="record-card__meta">审批时间：{{ formatDateTime(item.approveTime) }}</div>
          <div class="record-card__meta">审批意见：{{ item.approveComment || '-' }}</div>
          <div class="record-card__meta">创建时间：{{ formatDateTime(item.createTime) }}</div>
        </article>
      </div>

      <div class="pager">
        <van-button size="small" plain :disabled="pageBusy || pagination.pageNo <= 1" @click="changePage(-1)">上一页</van-button>
        <span class="pager__text">第 {{ pagination.pageNo }} / {{ totalPages }} 页，共 {{ pagination.total }} 条</span>
        <van-button size="small" plain :disabled="pageBusy || pagination.pageNo >= totalPages" @click="changePage(1)">下一页</van-button>
      </div>
    </section>
  </AppPageShell>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { showToast } from 'vant'
import AppPageShell from '@/components/layout/AppPageShell.vue'
import AttendanceWorkspaceTabs from '@/components/attendance/AttendanceWorkspaceTabs.vue'
import { queryMyAttendancePatchApplyPageApi, submitAttendancePatchApplyApi } from '@/api/attendance'

const list = ref([])
const loading = ref(false)
const submitting = ref(false)

const form = reactive({
  attendanceDate: todayDate(),
  patchType: 'CHECK_IN',
  patchTime: nowDateTimeLocal(),
  reason: ''
})

const filters = reactive({
  status: '',
  dateFrom: '',
  dateTo: ''
})

const pagination = reactive({
  pageNo: 1,
  pageSize: 10,
  total: 0
})

const pageBusy = computed(() => loading.value || submitting.value)
const totalPages = computed(() => Math.max(1, Math.ceil((pagination.total || 0) / pagination.pageSize)))

onMounted(() => {
  fetchList()
})

async function fetchList() {
  loading.value = true
  try {
    const response = await queryMyAttendancePatchApplyPageApi({
      pageNo: pagination.pageNo,
      pageSize: pagination.pageSize,
      status: filters.status || undefined,
      dateFrom: filters.dateFrom || undefined,
      dateTo: filters.dateTo || undefined
    })
    const data = ensureSuccess(response) || {}
    pagination.total = Number(data.total || 0)
    list.value = Array.isArray(data.list) ? data.list : []
    pagination.pageNo = Number(data.pageNo || pagination.pageNo || 1)
  } finally {
    loading.value = false
  }
}

async function handleSubmit() {
  if (!form.attendanceDate) {
    showToast('请选择考勤日期')
    return
  }
  if (!form.patchTime) {
    showToast('请选择补卡时间')
    return
  }
  if (!form.reason) {
    showToast('请填写补卡原因')
    return
  }
  submitting.value = true
  try {
    const response = await submitAttendancePatchApplyApi({
      attendanceDate: form.attendanceDate,
      patchType: form.patchType,
      patchTime: toBackendDateTime(form.patchTime),
      reason: form.reason
    })
    ensureSuccess(response)
    showToast('补卡申请已提交')
    form.reason = ''
    pagination.pageNo = 1
    await fetchList()
  } finally {
    submitting.value = false
  }
}

function handleSearch() {
  pagination.pageNo = 1
  fetchList()
}

function handleReset() {
  filters.status = ''
  filters.dateFrom = ''
  filters.dateTo = ''
  pagination.pageNo = 1
  fetchList()
}

function changePage(offset) {
  const nextPage = pagination.pageNo + offset
  if (nextPage < 1 || nextPage > totalPages.value) {
    return
  }
  pagination.pageNo = nextPage
  fetchList()
}

function patchTypeLabel(value) {
  return value === 'CHECK_OUT' ? '补下班卡' : '补上班卡'
}

function statusLabel(value) {
  if (value === 'APPROVED') return '已通过'
  if (value === 'REJECTED') return '已拒绝'
  return '待审批'
}

function statusTagType(value) {
  if (value === 'APPROVED') return 'success'
  if (value === 'REJECTED') return 'danger'
  return 'primary'
}

function formatDateTime(value) {
  if (!value) {
    return '-'
  }
  return String(value).replace('T', ' ').slice(0, 19)
}

function ensureSuccess(response) {
  if (!response || response.code !== 0) {
    throw new Error(response?.message || '请求失败')
  }
  return response.data
}

function todayDate() {
  const now = new Date()
  const year = now.getFullYear()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function nowDateTimeLocal() {
  const now = new Date()
  const offset = now.getTimezoneOffset() * 60000
  return new Date(now.getTime() - offset).toISOString().slice(0, 16)
}

function toBackendDateTime(value) {
  return value.replace('T', ' ') + (value.length === 16 ? ':00' : '')
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

.panel-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 12px;
  margin-top: 12px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-top: 12px;
}

.field-label {
  font-size: 12px;
  font-weight: 600;
  color: #64748b;
}

.field-input,
.field-textarea {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #dbe3ef;
  border-radius: 12px;
  background: #fff;
  color: #0f172a;
  box-sizing: border-box;
}

.panel-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 14px;
}

.card-list {
  display: grid;
  gap: 12px;
  margin-top: 14px;
}

.record-card {
  padding: 14px;
  border-radius: 16px;
  border: 1px solid #dbe3ef;
  background: #fff;
}

.record-card__top {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.record-card__title {
  font-size: 15px;
  font-weight: 700;
  color: #0f172a;
}

.record-card__meta {
  margin-top: 6px;
  font-size: 13px;
  color: #64748b;
  line-height: 1.6;
}

.record-card__reason {
  margin-top: 10px;
  font-size: 14px;
  line-height: 1.7;
  color: #334155;
  white-space: pre-wrap;
}

.pager {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 16px;
}

.pager__text {
  font-size: 13px;
  color: #64748b;
}

.state-block {
  padding: 24px 0;
}
</style>
