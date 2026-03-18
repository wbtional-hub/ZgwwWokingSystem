<template>
  <AppPageShell title="周报管理" description="当前页已接入最小可用周报工作流：列表、提交、编辑、详情和审核。">
    <template #actions>
      <div class="action-row">
        <van-button plain type="primary" :loading="listLoading" :disabled="pageBusy" @click="fetchList">刷新列表</van-button>
        <van-button type="primary" :disabled="pageBusy" @click="openCreateForm">新建周报</van-button>
        <van-button type="success" :loading="submitting" :disabled="!editorForm.weekNo || pageBusy" @click="handleSubmitCurrent">
          提交当前编辑
        </van-button>
      </div>
    </template>

    <section class="panel">
      <div class="panel-title">查询区</div>
      <div class="query-grid">
        <van-field v-model.trim="queryForm.weekNo" label="周次" placeholder="如：2026-W12" :disabled="pageBusy" />
        <div class="select-field">
          <span class="select-label">状态</span>
          <select v-model="queryForm.status" :disabled="pageBusy">
            <option value="">全部</option>
            <option value="DRAFT">草稿</option>
            <option value="SUBMITTED">待审核</option>
            <option value="APPROVED">已通过</option>
            <option value="RETURNED">已退回</option>
          </select>
        </div>
      </div>
      <div class="panel-actions">
        <van-button size="small" type="primary" :loading="listLoading" :disabled="pageBusy" @click="handleSearch">查询</van-button>
        <van-button size="small" plain :disabled="pageBusy" @click="handleResetQuery">重置</van-button>
      </div>
      <div v-if="querySummary" class="panel-hint">当前筛选：{{ querySummary }}</div>
    </section>

    <section class="panel">
      <div class="panel-title">编辑区</div>
      <div class="panel-hint">
        {{ editorMode === 'edit' ? `正在编辑周报 #${editorForm.id}` : '当前为新建周报模式' }}
      </div>
      <van-field v-model.trim="editorForm.weekNo" label="周次" placeholder="如：2026-W12" :disabled="pageBusy" />
      <van-field
        v-model="editorForm.workPlan"
        label="工作计划"
        type="textarea"
        rows="3"
        autosize
        placeholder="请输入本周工作计划"
        :disabled="pageBusy"
      />
      <van-field
        v-model="editorForm.workContent"
        label="工作内容"
        type="textarea"
        rows="4"
        autosize
        placeholder="请输入本周工作内容"
        :disabled="pageBusy"
      />
      <van-field
        v-model="editorForm.remark"
        label="备注"
        type="textarea"
        rows="2"
        autosize
        placeholder="可选"
        :disabled="pageBusy"
      />
      <div class="panel-actions">
        <van-button size="small" type="primary" :loading="submitting" :disabled="pageBusy" @click="handleSaveDraft">暂存</van-button>
        <van-button size="small" type="success" :loading="submitting" :disabled="pageBusy || !editorForm.weekNo" @click="handleSubmitCurrent">
          提交
        </van-button>
        <van-button size="small" plain :disabled="pageBusy" @click="resetEditor">清空</van-button>
      </div>
    </section>

    <section class="panel">
      <div class="panel-title">周报列表</div>
      <div class="panel-hint">共 {{ list.length }} 条{{ list.length ? `，最近一条周次：${list[0].weekNo}` : '' }}</div>

      <van-loading v-if="listLoading" size="24px" vertical class="state-block">加载中...</van-loading>

      <van-empty v-else-if="!list.length" description="暂无周报数据">
        <template #default>
          <div class="empty-tip">可先点击“新建周报”，填写后暂存或直接提交。</div>
        </template>
      </van-empty>

      <div v-else class="list-wrap">
        <van-card v-for="item in list" :key="item.id" class="report-card">
          <template #title>
            <div class="report-title">
              <span>{{ item.weekNo }} · {{ item.realName || item.username }}</span>
              <van-tag :type="statusTagType(item.status)">{{ statusLabel(item.status) }}</van-tag>
            </div>
          </template>
          <template #desc>
            <div class="report-meta">账号：{{ item.username || '-' }}</div>
            <div class="report-meta">用户ID：{{ item.userId }}</div>
            <div class="report-meta">提交时间：{{ formatDateTime(item.submitTime) }}</div>
            <div class="report-meta">创建时间：{{ formatDateTime(item.createTime) }}</div>
            <div class="report-preview">计划：{{ item.workPlan || '未填写' }}</div>
            <div class="report-preview">内容：{{ item.workContent || '未填写' }}</div>
          </template>
          <template #footer>
            <div class="card-actions">
              <van-button size="small" plain type="primary" :disabled="pageBusy" @click="openDetail(item)">详情</van-button>
              <van-button size="small" plain type="warning" :disabled="pageBusy || !canEdit(item)" @click="openEditForm(item)">
                编辑
              </van-button>
              <van-button
                size="small"
                plain
                type="success"
                :loading="submitTargetId === item.id"
                :disabled="pageBusy || !canSubmit(item)"
                @click="handleSubmitFromList(item)"
              >
                提交
              </van-button>
              <van-button
                size="small"
                plain
                type="success"
                :loading="reviewTargetId === item.id && reviewAction === 'APPROVE'"
                :disabled="pageBusy || !canReview(item)"
                @click="handleReview(item, 'APPROVE')"
              >
                通过
              </van-button>
              <van-button
                size="small"
                plain
                type="danger"
                :loading="reviewTargetId === item.id && reviewAction === 'RETURN'"
                :disabled="pageBusy || !canReview(item)"
                @click="handleReview(item, 'RETURN')"
              >
                退回
              </van-button>
            </div>
          </template>
        </van-card>
      </div>
    </section>

    <van-popup v-model:show="detailVisible" position="bottom" round :close-on-click-overlay="!detailLoading">
      <div class="detail-panel">
        <div class="detail-title">周报详情</div>
        <van-loading v-if="detailLoading" size="24px" vertical class="state-block">加载中...</van-loading>
        <template v-else-if="detailRecord">
          <div class="detail-item">周报ID：{{ detailRecord.id }}</div>
          <div class="detail-item">周次：{{ detailRecord.weekNo }}</div>
          <div class="detail-item">状态：{{ statusLabel(detailRecord.status) }}</div>
          <div class="detail-item">用户：{{ detailRecord.realName || detailRecord.username || '-' }}</div>
          <div class="detail-item">用户ID：{{ detailRecord.userId }}</div>
          <div class="detail-item">提交时间：{{ formatDateTime(detailRecord.submitTime) }}</div>
          <div class="detail-item">创建时间：{{ formatDateTime(detailRecord.createTime) }}</div>
          <div class="detail-block">
            <div class="detail-label">工作计划</div>
            <div class="detail-value">{{ detailRecord.workPlan || '未填写' }}</div>
          </div>
          <div class="detail-block">
            <div class="detail-label">工作内容</div>
            <div class="detail-value">{{ detailRecord.workContent || '未填写' }}</div>
          </div>
          <div class="detail-block">
            <div class="detail-label">备注</div>
            <div class="detail-value">{{ detailRecord.remark || '无' }}</div>
          </div>
          <div class="panel-actions">
            <van-button size="small" plain type="warning" :disabled="pageBusy || !canEdit(detailRecord)" @click="openEditForm(detailRecord)">
              编辑
            </van-button>
            <van-button size="small" plain type="success" :disabled="pageBusy || !canReview(detailRecord)" @click="handleReview(detailRecord, 'APPROVE')">
              审核通过
            </van-button>
            <van-button size="small" plain type="danger" :disabled="pageBusy || !canReview(detailRecord)" @click="handleReview(detailRecord, 'RETURN')">
              审核退回
            </van-button>
          </div>
        </template>
      </div>
    </van-popup>
  </AppPageShell>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { showConfirmDialog, showToast } from 'vant'
import AppPageShell from '@/components/layout/AppPageShell.vue'
import { useUserStore } from '@/stores/user'
import {
  queryWeeklyWorkDetail,
  queryWeeklyWorkList,
  reviewWeeklyWork,
  saveWeeklyWorkDraft,
  submitWeeklyWork
} from '@/api/weeklywork'

const userStore = useUserStore()

const queryForm = reactive({
  weekNo: '',
  status: ''
})

const editorForm = reactive(createEmptyEditor())
const editorMode = ref('create')
const list = ref([])
const listLoading = ref(false)
const submitting = ref(false)
const detailLoading = ref(false)
const detailVisible = ref(false)
const detailRecord = ref(null)
const submitTargetId = ref(null)
const reviewTargetId = ref(null)
const reviewAction = ref('')

const currentUserId = computed(() => Number(userStore.userInfo?.userId || 0))
const pageBusy = computed(() => {
  return listLoading.value || submitting.value || detailLoading.value || reviewTargetId.value !== null || submitTargetId.value !== null
})
const querySummary = computed(() => {
  const summary = []
  if (queryForm.weekNo) {
    summary.push(`周次 ${queryForm.weekNo}`)
  }
  if (queryForm.status) {
    summary.push(`状态 ${statusLabel(queryForm.status)}`)
  }
  return summary.join('，')
})

function createEmptyEditor() {
  return {
    id: null,
    weekNo: getCurrentWeekNo(),
    workPlan: '',
    workContent: '',
    remark: ''
  }
}

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

function statusLabel(status) {
  return {
    DRAFT: '草稿',
    SUBMITTED: '待审核',
    APPROVED: '已通过',
    RETURNED: '已退回'
  }[status] || status || '-'
}

function statusTagType(status) {
  return {
    DRAFT: 'primary',
    SUBMITTED: 'warning',
    APPROVED: 'success',
    RETURNED: 'danger'
  }[status] || 'default'
}

function formatDateTime(value) {
  if (!value) {
    return '-'
  }
  return String(value).replace('T', ' ')
}

function fillEditor(record) {
  editorForm.id = record.id ?? null
  editorForm.weekNo = record.weekNo || getCurrentWeekNo()
  editorForm.workPlan = record.workPlan || ''
  editorForm.workContent = record.workContent || ''
  editorForm.remark = record.remark || ''
}

function resetEditor() {
  Object.assign(editorForm, createEmptyEditor())
  editorMode.value = 'create'
}

function openCreateForm() {
  resetEditor()
  showToast('已切换到新建周报')
}

async function fetchList() {
  listLoading.value = true
  try {
    const response = await queryWeeklyWorkList({
      weekNo: queryForm.weekNo || undefined,
      status: queryForm.status || undefined
    })
    list.value = Array.isArray(response.data) ? response.data : []
  } catch (error) {
    showToast(error.message || '周报列表加载失败')
  } finally {
    listLoading.value = false
  }
}

function handleSearch() {
  fetchList()
}

function handleResetQuery() {
  queryForm.weekNo = ''
  queryForm.status = ''
  fetchList()
}

function canEdit(item) {
  return Number(item.userId) === currentUserId.value && ['DRAFT', 'RETURNED'].includes(item.status)
}

function canSubmit(item) {
  return Number(item.userId) === currentUserId.value && ['DRAFT', 'RETURNED'].includes(item.status)
}

function canReview(item) {
  return Number(item.userId) !== currentUserId.value && item.status === 'SUBMITTED'
}

async function handleSaveDraft() {
  if (!editorForm.weekNo) {
    showToast('请输入周次')
    return null
  }
  submitting.value = true
  try {
    const response = await saveWeeklyWorkDraft({
      weekNo: editorForm.weekNo,
      workPlan: editorForm.workPlan,
      workContent: editorForm.workContent,
      remark: editorForm.remark
    })
    const savedId = response.data
    editorForm.id = savedId
    editorMode.value = 'edit'
    await fetchList()
    showToast('周报已暂存')
    return savedId
  } catch (error) {
    showToast(error.message || '暂存失败')
    return null
  } finally {
    submitting.value = false
  }
}

async function handleSubmitCurrent() {
  let targetId = editorForm.id
  if (!targetId) {
    targetId = await handleSaveDraft()
  }
  if (!targetId) {
    return
  }
  submitting.value = true
  try {
    await submitWeeklyWork({ id: targetId })
    await fetchList()
    await openDetailById(targetId)
    showToast('周报已提交')
  } catch (error) {
    showToast(error.message || '提交失败')
  } finally {
    submitting.value = false
  }
}

async function openEditForm(item) {
  if (!canEdit(item)) {
    showToast('当前周报不可编辑')
    return
  }
  fillEditor(item)
  editorMode.value = 'edit'
  if (item.id) {
    try {
      const response = await queryWeeklyWorkDetail(item.id)
      fillEditor(response.data || item)
    } catch (error) {
      showToast(error.message || '周报详情加载失败')
    }
  }
}

async function openDetailById(id, sourceItem = null) {
  detailVisible.value = true
  detailLoading.value = true
  try {
    const response = await queryWeeklyWorkDetail(id)
    detailRecord.value = {
      ...(sourceItem || {}),
      ...(response.data || {})
    }
  } catch (error) {
    detailVisible.value = false
    showToast(error.message || '详情加载失败')
  } finally {
    detailLoading.value = false
  }
}

function openDetail(item) {
  openDetailById(item.id, item)
}

async function handleSubmitFromList(item) {
  if (!canSubmit(item)) {
    showToast('当前周报不可提交')
    return
  }
  submitTargetId.value = item.id
  try {
    await showConfirmDialog({
      title: '确认提交',
      message: `确认提交 ${item.weekNo} 的周报吗？`
    })
    await submitWeeklyWork({ id: item.id })
    await fetchList()
    await openDetailById(item.id, item)
    showToast('周报已提交')
  } catch (error) {
    if (error?.message && error.message !== 'cancel') {
      showToast(error.message)
    }
  } finally {
    submitTargetId.value = null
  }
}

async function handleReview(item, action) {
  if (!canReview(item)) {
    showToast('当前周报不可审核')
    return
  }
  reviewTargetId.value = item.id
  reviewAction.value = action
  try {
    await showConfirmDialog({
      title: action === 'APPROVE' ? '确认通过' : '确认退回',
      message: `${action === 'APPROVE' ? '通过' : '退回'} ${item.weekNo} 的周报？`
    })
    await reviewWeeklyWork({
      id: item.id,
      action
    })
    await fetchList()
    await openDetailById(item.id, item)
    showToast(action === 'APPROVE' ? '审核通过' : '已退回')
  } catch (error) {
    if (error?.message && error.message !== 'cancel') {
      showToast(error.message)
    }
  } finally {
    reviewTargetId.value = null
    reviewAction.value = ''
  }
}

onMounted(() => {
  fetchList()
})
</script>

<style scoped>
.action-row,
.panel-actions,
.card-actions {
  display: flex;
  flex-wrap: wrap;
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
.empty-tip,
.report-meta,
.report-preview,
.detail-item,
.detail-value {
  font-size: 13px;
  color: #4b5563;
  line-height: 1.6;
}

.query-grid {
  display: grid;
  gap: 10px;
  margin-bottom: 10px;
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
  flex: 0 0 56px;
  font-size: 14px;
  color: #323233;
}

.select-field select {
  flex: 1;
  border: 0;
  background: transparent;
  font-size: 14px;
  color: #323233;
}

.list-wrap {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.report-card {
  overflow: hidden;
}

.report-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.report-preview {
  margin-top: 4px;
}

.state-block {
  padding: 20px 0;
}

.detail-panel {
  padding: 16px;
}

.detail-title {
  margin-bottom: 12px;
  font-size: 16px;
  font-weight: 600;
}

.detail-block {
  margin-top: 12px;
}

.detail-label {
  margin-bottom: 4px;
  font-size: 13px;
  color: #111827;
  font-weight: 600;
}
</style>
