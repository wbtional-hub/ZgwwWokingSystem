<template>
  <AppPageShell title="考勤管理" description="当前页已接入最小可用考勤工作流：列表、查询、打卡、补录编辑和删除。">
    <template #actions>
      <div class="action-row">
        <van-button type="primary" :loading="state.checkingIn" :disabled="pageBusy" @click="handleCheckIn">立即打卡</van-button>
        <van-button plain type="primary" :loading="state.loading" :disabled="pageBusy" @click="fetchList">刷新列表</van-button>
      </div>
    </template>

    <section class="panel">
      <div class="panel-title">查询区</div>
      <div class="query-grid">
        <van-field v-model.trim="state.queryForm.keywords" label="姓名/账号" placeholder="请输入姓名或账号" :disabled="pageBusy" />
        <van-field v-model.trim="state.queryForm.unitName" label="组织名称" placeholder="请输入组织名称" :disabled="pageBusy" />
        <van-field v-model="state.queryForm.dateFrom" label="开始日期" type="date" :disabled="pageBusy" />
        <van-field v-model="state.queryForm.dateTo" label="结束日期" type="date" :disabled="pageBusy" />
      </div>
      <div class="panel-actions">
        <van-button size="small" type="primary" :loading="state.loading" :disabled="pageBusy" @click="handleSearch">查询</van-button>
        <van-button size="small" plain :disabled="pageBusy" @click="handleReset">重置</van-button>
      </div>
    </section>

    <section class="panel">
      <div class="panel-title">{{ state.form.id ? `编辑/补录考勤 #${state.form.id}` : '补录考勤' }}</div>
      <div class="panel-hint">统一 save 接口支持新增补录和回填修改。</div>
      <van-field v-model.trim="state.form.userId" label="用户ID" placeholder="默认当前登录用户" :disabled="pageBusy" />
      <van-field v-model="state.form.attendanceDate" label="考勤日期" type="date" :disabled="pageBusy" />
      <van-field v-model="state.form.checkInTime" label="上班时间" type="datetime-local" :disabled="pageBusy" />
      <van-field v-model="state.form.checkOutTime" label="下班时间" type="datetime-local" :disabled="pageBusy" />
      <van-field v-model="state.form.checkInAddress" label="上班地点" placeholder="请输入上班地点" :disabled="pageBusy" />
      <van-field v-model="state.form.checkOutAddress" label="下班地点" placeholder="请输入下班地点" :disabled="pageBusy" />
      <div class="select-field">
        <span class="select-label">状态</span>
        <select v-model="state.form.validFlag" :disabled="pageBusy">
          <option :value="1">有效</option>
          <option :value="0">无效</option>
        </select>
      </div>
      <div class="panel-actions">
        <van-button size="small" type="primary" :loading="state.saving" :disabled="pageBusy" @click="handleSave">保存</van-button>
        <van-button size="small" plain :disabled="pageBusy" @click="resetForm">清空</van-button>
      </div>
    </section>

    <section class="panel">
      <div class="panel-title">考勤列表</div>
      <div class="panel-hint">共 {{ state.list.length }} 条考勤记录</div>

      <van-loading v-if="state.loading" class="state-block" size="24px" vertical>加载中...</van-loading>
      <van-empty v-else-if="!state.list.length" description="暂无考勤数据">
        <template #default>
          <div class="panel-hint">可直接点击“立即打卡”，或在上方表单补录一条考勤。</div>
        </template>
      </van-empty>

      <div v-else class="list-wrap">
        <van-card v-for="item in state.list" :key="item.id" class="attendance-card">
          <template #title>
            <div class="attendance-title">
              <span>{{ item.realName || item.username }} · {{ item.attendanceDate || '-' }}</span>
              <van-tag :type="Number(item.validFlag) === 1 ? 'success' : 'danger'">{{ Number(item.validFlag) === 1 ? '有效' : '无效' }}</van-tag>
            </div>
          </template>
          <template #desc>
            <div class="attendance-meta">用户ID：{{ item.userId }}</div>
            <div class="attendance-meta">账号：{{ item.username || '-' }}</div>
            <div class="attendance-meta">组织：{{ item.unitName || '-' }}</div>
            <div class="attendance-meta">上班时间：{{ formatDateTime(item.checkInTime) }}</div>
            <div class="attendance-meta">下班时间：{{ formatDateTime(item.checkOutTime) }}</div>
            <div class="attendance-meta">上班地点：{{ item.checkInAddress || '-' }}</div>
            <div class="attendance-meta">下班地点：{{ item.checkOutAddress || '-' }}</div>
          </template>
          <template #footer>
            <div class="action-row">
              <van-button size="small" plain type="warning" :disabled="pageBusy" @click="openEditForm(item)">编辑/补录</van-button>
              <van-button
                size="small"
                plain
                type="danger"
                :loading="state.deletingId === item.id"
                :disabled="pageBusy"
                @click="handleDelete(item)"
              >
                删除
              </van-button>
            </div>
          </template>
        </van-card>
      </div>
    </section>
  </AppPageShell>
</template>

<script setup>
import { computed, onMounted, reactive } from 'vue'
import { showConfirmDialog, showToast } from 'vant'
import AppPageShell from '@/components/layout/AppPageShell.vue'
import { useUserStore } from '@/stores/user'
import { checkInApi, deleteAttendanceApi, queryAttendanceListApi, saveAttendanceApi } from '@/api/attendance'

const userStore = useUserStore()

const state = reactive({
  loading: false,
  saving: false,
  checkingIn: false,
  deletingId: null,
  list: [],
  queryForm: {
    keywords: '',
    unitName: '',
    dateFrom: '',
    dateTo: ''
  },
  form: createEmptyForm()
})

const pageBusy = computed(() => {
  return state.loading || state.saving || state.checkingIn || state.deletingId !== null
})

function createEmptyForm() {
  return {
    id: null,
    userId: '',
    attendanceDate: toInputDate(new Date()),
    checkInTime: '',
    checkOutTime: '',
    checkInAddress: '',
    checkOutAddress: '',
    validFlag: 1
  }
}

function toInputDate(value) {
  const date = value instanceof Date ? value : new Date(value)
  if (Number.isNaN(date.getTime())) {
    return ''
  }
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function toInputDateTime(value) {
  const date = value instanceof Date ? value : new Date(value)
  if (Number.isNaN(date.getTime())) {
    return ''
  }
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day}T${hours}:${minutes}`
}

function toBackendDateTime(value) {
  if (!value) {
    return ''
  }
  return `${value.replace('T', ' ')}:00`
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
    const response = await queryAttendanceListApi({
      keywords: state.queryForm.keywords || undefined,
      unitName: state.queryForm.unitName || undefined,
      dateFrom: state.queryForm.dateFrom || undefined,
      dateTo: state.queryForm.dateTo || undefined
    })
    state.list = Array.isArray(response.data) ? response.data : []
  } catch (error) {
    showToast(error.message || '考勤列表加载失败')
  } finally {
    state.loading = false
  }
}

function handleSearch() {
  fetchList()
}

function handleReset() {
  state.queryForm.keywords = ''
  state.queryForm.unitName = ''
  state.queryForm.dateFrom = ''
  state.queryForm.dateTo = ''
  fetchList()
}

function resetForm() {
  state.form = createEmptyForm()
}

async function handleCheckIn() {
  state.checkingIn = true
  try {
    const response = await checkInApi({
      address: '页面快捷打卡'
    })
    const action = response.data?.action
    showToast(action === 'CHECK_OUT' ? '下班时间已补齐' : '上班打卡成功')
    await fetchList()
  } catch (error) {
    showToast(error.message || '打卡失败')
  } finally {
    state.checkingIn = false
  }
}

function openEditForm(item) {
  state.form = {
    id: item.id,
    userId: item.userId ? String(item.userId) : '',
    attendanceDate: item.attendanceDate || toInputDate(new Date()),
    checkInTime: item.checkInTime ? toInputDateTime(item.checkInTime) : '',
    checkOutTime: item.checkOutTime ? toInputDateTime(item.checkOutTime) : '',
    checkInAddress: item.checkInAddress || '',
    checkOutAddress: item.checkOutAddress || '',
    validFlag: Number(item.validFlag) || 0
  }
}

async function handleSave() {
  if (!state.form.attendanceDate) {
    showToast('请选择考勤日期')
    return
  }

  state.saving = true
  try {
    await saveAttendanceApi({
      id: state.form.id || undefined,
      userId: state.form.userId ? Number(state.form.userId) : Number(userStore.userInfo?.userId || 0),
      attendanceDate: state.form.attendanceDate,
      checkInTime: state.form.checkInTime ? toBackendDateTime(state.form.checkInTime) : undefined,
      checkOutTime: state.form.checkOutTime ? toBackendDateTime(state.form.checkOutTime) : undefined,
      checkInAddress: state.form.checkInAddress || undefined,
      checkOutAddress: state.form.checkOutAddress || undefined,
      validFlag: Number(state.form.validFlag)
    })
    showToast(state.form.id ? '考勤修改成功' : '考勤补录成功')
    resetForm()
    await fetchList()
  } catch (error) {
    showToast(error.message || '考勤保存失败')
  } finally {
    state.saving = false
  }
}

async function handleDelete(item) {
  state.deletingId = item.id
  try {
    await showConfirmDialog({
      title: '确认删除',
      message: `确认删除 ${item.realName || item.username} 在 ${item.attendanceDate} 的考勤吗？`
    })
    await deleteAttendanceApi(item.id)
    showToast('考勤删除成功')
    if (state.form.id === item.id) {
      resetForm()
    }
    await fetchList()
  } catch (error) {
    if (error?.message && error.message !== 'cancel') {
      showToast(error.message || '考勤删除失败')
    }
  } finally {
    state.deletingId = null
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
.attendance-meta {
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

.list-wrap {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.attendance-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.state-block {
  padding: 20px 0;
}
</style>
