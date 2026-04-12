<template>
  <AppPageShell title="补打卡审批" description="领导或管理员可在 PC 端查看待处理补卡申请，并审批通过或拒绝。" help-key="attendance">
    <template #actions>
      <van-button plain type="primary" :disabled="pageBusy" @click="fetchList">刷新列表</van-button>
    </template>

    <AttendanceWorkspaceTabs />

    <section class="panel">
      <div class="panel-title">筛选条件</div>
      <div class="panel-grid">
        <label class="field">
          <span class="field-label">关键词</span>
          <input v-model.trim="filters.keywords" class="field-input" type="text" :disabled="pageBusy" placeholder="姓名或账号">
        </label>
        <label class="field">
          <span class="field-label">状态</span>
          <select v-model="filters.status" class="field-input" :disabled="pageBusy">
            <option value="PENDING">待审批</option>
            <option value="APPROVED">已通过</option>
            <option value="REJECTED">已拒绝</option>
            <option value="">全部状态</option>
          </select>
        </label>
        <label class="field">
          <span class="field-label">补卡类型</span>
          <select v-model="filters.patchType" class="field-input" :disabled="pageBusy">
            <option value="">全部类型</option>
            <option value="CHECK_IN">补上班卡</option>
            <option value="CHECK_OUT">补下班卡</option>
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
    </section>

    <section class="panel">
      <div class="panel-title">审批列表</div>
      <van-loading v-if="loading" class="state-block" vertical size="24px">加载中...</van-loading>
      <van-empty v-else-if="!list.length" description="当前范围内暂无补卡申请" />
      <div v-else class="card-list">
        <article v-for="item in list" :key="item.id" class="record-card">
          <div class="record-card__top">
            <div>
              <div class="record-card__title">{{ item.realName || item.username || '未命名用户' }} · {{ patchTypeLabel(item.patchType) }}</div>
              <div class="record-card__meta">账号：{{ item.username || '-' }} / 单位：{{ item.unitName || '-' }}</div>
            </div>
            <van-tag :type="statusTagType(item.status)">{{ statusLabel(item.status) }}</van-tag>
          </div>
          <div class="record-card__meta">考勤日期：{{ item.attendanceDate }}，补卡时间：{{ formatDateTime(item.patchTime) }}</div>
          <div class="record-card__reason">{{ item.reason || '-' }}</div>
          <div class="record-card__meta">审批人：{{ item.approveRealName || item.approveUsername || '-' }}</div>
          <div class="record-card__meta">审批时间：{{ formatDateTime(item.approveTime) }}</div>
          <div class="record-card__meta">审批意见：{{ item.approveComment || '-' }}</div>
          <div v-if="item.status === 'PENDING'" class="panel-actions">
            <van-button size="small" type="success" :disabled="reviewBusy" @click="openReviewDialog(item, 'approve')">审批通过</van-button>
            <van-button size="small" plain type="danger" :disabled="reviewBusy" @click="openReviewDialog(item, 'reject')">审批拒绝</van-button>
          </div>
        </article>
      </div>

      <div class="pager">
        <van-button size="small" plain :disabled="pageBusy || pagination.pageNo <= 1" @click="changePage(-1)">上一页</van-button>
        <span class="pager__text">第 {{ pagination.pageNo }} / {{ totalPages }} 页，共 {{ pagination.total }} 条</span>
        <van-button size="small" plain :disabled="pageBusy || pagination.pageNo >= totalPages" @click="changePage(1)">下一页</van-button>
      </div>
    </section>

    <van-popup v-model:show="reviewDialog.show" round position="bottom">
      <div class="review-sheet">
        <div class="review-sheet__title">{{ reviewDialog.action === 'approve' ? '审批通过' : '审批拒绝' }}</div>
        <div class="review-sheet__desc">
          {{ reviewDialog.record?.realName || reviewDialog.record?.username || '-' }} · {{ patchTypeLabel(reviewDialog.record?.patchType) }}
        </div>
        <textarea
          v-model.trim="reviewDialog.comment"
          class="field-textarea"
          rows="4"
          maxlength="500"
          :placeholder="reviewDialog.action === 'approve' ? '可选填写审批备注' : '请填写拒绝原因'"
        />
        <div class="panel-actions">
          <van-button plain :disabled="reviewBusy" @click="closeReviewDialog">取消</van-button>
          <van-button :type="reviewDialog.action === 'approve' ? 'success' : 'danger'" :loading="reviewBusy" @click="submitReview">
            {{ reviewDialog.action === 'approve' ? '确认通过' : '确认拒绝' }}
          </van-button>
        </div>
      </div>
    </van-popup>
  </AppPageShell>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { showToast } from 'vant'
import AppPageShell from '@/components/layout/AppPageShell.vue'
import AttendanceWorkspaceTabs from '@/components/attendance/AttendanceWorkspaceTabs.vue'
import { approveAttendancePatchApplyApi, queryPendingAttendancePatchApplyPageApi, rejectAttendancePatchApplyApi } from '@/api/attendance'

const list = ref([])
const loading = ref(false)
const reviewBusy = ref(false)

const filters = reactive({
  keywords: '',
  status: 'PENDING',
  patchType: '',
  dateFrom: '',
  dateTo: ''
})

const pagination = reactive({
  pageNo: 1,
  pageSize: 10,
  total: 0
})

const reviewDialog = reactive({
  show: false,
  action: 'approve',
  record: null,
  comment: ''
})

const pageBusy = computed(() => loading.value || reviewBusy.value)
const totalPages = computed(() => Math.max(1, Math.ceil((pagination.total || 0) / pagination.pageSize)))

onMounted(() => {
  fetchList()
})

async function fetchList() {
  loading.value = true
  try {
    const response = await queryPendingAttendancePatchApplyPageApi({
      pageNo: pagination.pageNo,
      pageSize: pagination.pageSize,
      keywords: filters.keywords || undefined,
      status: filters.status || undefined,
      patchType: filters.patchType || undefined,
      dateFrom: filters.dateFrom || undefined,
      dateTo: filters.dateTo || undefined
    })
    const data = ensureSuccess(response) || {}
    pagination.total = Number(data.total || 0)
    pagination.pageNo = Number(data.pageNo || pagination.pageNo || 1)
    list.value = Array.isArray(data.list) ? data.list : []
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.pageNo = 1
  fetchList()
}

function handleReset() {
  filters.keywords = ''
  filters.status = 'PENDING'
  filters.patchType = ''
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

function openReviewDialog(record, action) {
  reviewDialog.show = true
  reviewDialog.record = record
  reviewDialog.action = action
  reviewDialog.comment = ''
}

function closeReviewDialog() {
  reviewDialog.show = false
  reviewDialog.record = null
  reviewDialog.comment = ''
}

async function submitReview() {
  if (!reviewDialog.record?.id) {
    return
  }
  if (reviewDialog.action === 'reject' && !reviewDialog.comment) {
    showToast('拒绝时请填写审批意见')
    return
  }
  reviewBusy.value = true
  try {
    const api = reviewDialog.action === 'approve' ? approveAttendancePatchApplyApi : rejectAttendancePatchApplyApi
    const response = await api(reviewDialog.record.id, {
      approveComment: reviewDialog.comment || undefined
    })
    ensureSuccess(response)
    showToast(reviewDialog.action === 'approve' ? '审批已通过' : '已拒绝该申请')
    closeReviewDialog()
    await fetchList()
  } finally {
    reviewBusy.value = false
  }
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
  align-items: flex-start;
  gap: 12px;
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

.review-sheet {
  padding: 18px 16px calc(18px + env(safe-area-inset-bottom));
}

.review-sheet__title {
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
}

.review-sheet__desc {
  margin-top: 6px;
  font-size: 13px;
  color: #64748b;
}

.state-block {
  padding: 24px 0;
}
</style>
