<template>
  <AppPageShell title="操作日志" description="当前页已接入最小可用操作日志工作流：查询、刷新和关键操作追溯。">
    <template #actions>
      <div class="action-row">
        <van-button type="primary" :loading="state.loading" :disabled="state.loading" @click="fetchList">刷新日志</van-button>
      </div>
    </template>

    <section class="panel">
      <div class="panel-title">查询区</div>
      <div class="query-grid">
        <div class="select-field">
          <span class="select-label">模块</span>
          <select v-model="state.queryForm.moduleName" :disabled="state.loading">
            <option value="">全部</option>
            <option value="USER">用户管理</option>
            <option value="WEEKLY_WORK">周报管理</option>
            <option value="ATTENDANCE">考勤管理</option>
            <option value="WORK_SCORE">工作评分</option>
          </select>
        </div>
        <van-field v-model.trim="state.queryForm.operatorName" label="操作人" placeholder="请输入操作人" :disabled="state.loading" />
        <van-field v-model="state.queryForm.startTime" label="开始时间" type="datetime-local" :disabled="state.loading" />
        <van-field v-model="state.queryForm.endTime" label="结束时间" type="datetime-local" :disabled="state.loading" />
      </div>
      <div class="panel-actions">
        <van-button size="small" type="primary" :loading="state.loading" :disabled="state.loading" @click="handleSearch">查询</van-button>
        <van-button size="small" plain :disabled="state.loading" @click="handleReset">重置</van-button>
      </div>
    </section>

    <section class="panel">
      <div class="panel-title">日志列表</div>
      <div class="panel-hint">共 {{ state.list.length }} 条日志记录</div>
      <div class="panel-hint">当前查询工作面：{{ querySummaryText }}</div>

      <van-loading v-if="state.loading" class="state-block" size="24px" vertical>加载中...</van-loading>
      <van-empty v-else-if="!state.list.length" description="暂无操作日志">
        <template #default>
          <div class="panel-hint">完成用户新增、周报审核、考勤补录或评分计算后，这里会出现可追溯日志。</div>
          <div class="panel-hint">当前查询工作面：{{ querySummaryText }}</div>
          <div v-if="hasQueryFilters" class="panel-actions">
            <van-button size="small" plain type="primary" :disabled="state.loading" @click="handleReset">
              清空当前查询条件
            </van-button>
          </div>
        </template>
      </van-empty>

      <div v-else class="list-wrap">
        <van-card v-for="item in state.list" :key="item.id" class="log-card">
          <template #title>
            <div class="log-title">
              <span>{{ moduleLabel(item.moduleName) }} · {{ actionLabel(item.actionName) }}</span>
              <van-tag type="primary">{{ item.operatorName || 'system' }}</van-tag>
            </div>
          </template>
          <template #desc>
            <div class="log-meta">日志ID：{{ item.id }}</div>
            <div class="log-meta">业务ID：{{ item.bizId || '-' }}</div>
            <div class="log-meta">时间：{{ formatDateTime(item.createTime) }}</div>
            <div class="log-content">{{ item.content || '-' }}</div>
          </template>
        </van-card>
      </div>
    </section>
  </AppPageShell>
</template>

<script setup>
import { computed, onMounted, reactive } from 'vue'
import { showToast } from 'vant'
import AppPageShell from '@/components/layout/AppPageShell.vue'
import { queryOperationLogListApi } from '@/api/operationlog'

const state = reactive({
  loading: false,
  list: [],
  queryForm: {
    moduleName: '',
    operatorName: '',
    startTime: '',
    endTime: ''
  }
})

const hasQueryFilters = computed(() => {
  return Boolean(
    state.queryForm.moduleName ||
    state.queryForm.operatorName ||
    state.queryForm.startTime ||
    state.queryForm.endTime
  )
})

const querySummaryText = computed(() => {
  const parts = []
  if (state.queryForm.moduleName) {
    parts.push(`模块 ${moduleLabel(state.queryForm.moduleName)}`)
  }
  if (state.queryForm.operatorName) {
    parts.push(`操作人 ${state.queryForm.operatorName}`)
  }
  if (state.queryForm.startTime || state.queryForm.endTime) {
    parts.push(`时间 ${formatRangeText(state.queryForm.startTime, state.queryForm.endTime)}`)
  }
  return parts.join(' / ') || '全部模块 / 全部操作人 / 全部时间'
})

function moduleLabel(value) {
  const map = {
    USER: '用户管理',
    WEEKLY_WORK: '周报管理',
    ATTENDANCE: '考勤管理',
    WORK_SCORE: '工作评分'
  }
  return map[value] || value || '未知模块'
}

function actionLabel(value) {
  const map = {
    CREATE: '新增',
    UPDATE: '编辑',
    REVIEW: '审核',
    SAVE: '补录',
    CALCULATE: '计算'
  }
  return map[value] || value || '操作'
}

function toBackendDateTime(value) {
  if (!value) {
    return undefined
  }
  return `${value.replace('T', ' ')}:00`
}

function formatDateTime(value) {
  return value ? String(value).replace('T', ' ') : '-'
}

function formatRangeText(startTime, endTime) {
  if (startTime && endTime) {
    return `${startTime.replace('T', ' ')} 至 ${endTime.replace('T', ' ')}`
  }
  if (startTime) {
    return `${startTime.replace('T', ' ')} 起`
  }
  if (endTime) {
    return `截至 ${endTime.replace('T', ' ')}`
  }
  return '全部时间'
}

async function fetchList() {
  state.loading = true
  try {
    const response = await queryOperationLogListApi({
      moduleName: state.queryForm.moduleName || undefined,
      operatorName: state.queryForm.operatorName || undefined,
      startTime: toBackendDateTime(state.queryForm.startTime),
      endTime: toBackendDateTime(state.queryForm.endTime)
    })
    state.list = Array.isArray(response.data) ? response.data : []
  } catch (error) {
    showToast(error.message || '日志加载失败')
  } finally {
    state.loading = false
  }
}

function handleSearch() {
  fetchList()
}

function handleReset() {
  state.queryForm.moduleName = ''
  state.queryForm.operatorName = ''
  state.queryForm.startTime = ''
  state.queryForm.endTime = ''
  fetchList()
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

.panel-hint {
  margin-top: 8px;
  font-size: 13px;
  color: #6b7280;
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

.select-field {
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 44px;
  padding: 0 12px;
  border: 1px solid #ebedf0;
  border-radius: 8px;
}

.select-label {
  flex-shrink: 0;
  color: #646566;
}

.select-field select {
  flex: 1;
  border: none;
  background: transparent;
  font-size: 14px;
  color: #323233;
  outline: none;
}

.state-block {
  padding: 24px 0;
}

.list-wrap {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 12px;
}

.log-card {
  border-radius: 12px;
  overflow: hidden;
}

.log-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.log-meta,
.log-content {
  margin-top: 6px;
  font-size: 13px;
  line-height: 1.6;
  color: #4b5563;
}
</style>
