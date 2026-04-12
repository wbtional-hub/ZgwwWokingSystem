<template>
  <AppPageShell title="日志中台" description="统一查看前端异常、接口失败、定位诊断与后端业务异常，支持 traceId 串联与一键复制给 AI。">
    <template #actions>
      <div class="action-row">
        <van-button type="primary" :loading="state.loading" :disabled="state.loading" @click="fetchList">刷新日志</van-button>
      </div>
    </template>

    <section class="panel">
      <div class="panel-title">查询区</div>
      <div class="query-grid">
        <van-field v-model.trim="state.queryForm.traceId" label="traceId" placeholder="输入 traceId" :disabled="state.loading" />
        <van-field v-model.trim="state.queryForm.keyword" label="关键字" placeholder="标题 / 摘要 / 错误码 / 页面" :disabled="state.loading" />
        <van-field v-model.trim="state.queryForm.userKeyword" label="用户" placeholder="用户名 / 用户ID / 组织" :disabled="state.loading" />
        <div class="select-field">
          <span class="select-label">类型</span>
          <select v-model="state.queryForm.logType" :disabled="state.loading">
            <option value="">全部</option>
            <option v-for="item in LOG_TYPE_OPTIONS" :key="item.value" :value="item.value">{{ item.label }}</option>
          </select>
        </div>
        <div class="select-field">
          <span class="select-label">模块</span>
          <select v-model="state.queryForm.module" :disabled="state.loading">
            <option value="">全部</option>
            <option v-for="item in MODULE_OPTIONS" :key="item.value" :value="item.value">{{ item.label }}</option>
          </select>
        </div>
        <div class="select-field">
          <span class="select-label">级别</span>
          <select v-model="state.queryForm.level" :disabled="state.loading">
            <option value="">全部</option>
            <option value="ERROR">ERROR</option>
            <option value="WARN">WARN</option>
            <option value="INFO">INFO</option>
          </select>
        </div>
        <van-field v-model="state.queryForm.startTime" label="开始时间" type="datetime-local" :disabled="state.loading" />
        <van-field v-model="state.queryForm.endTime" label="结束时间" type="datetime-local" :disabled="state.loading" />
      </div>
      <div class="panel-actions">
        <van-button size="small" type="primary" :loading="state.loading" :disabled="state.loading" @click="handleSearch">查询</van-button>
        <van-button size="small" plain :disabled="state.loading" @click="handleReset">重置</van-button>
      </div>
      <div class="panel-hint">当前筛选：{{ querySummaryText }}</div>
    </section>

    <section class="panel">
      <div class="panel-title">日志列表</div>
      <div class="panel-hint">共 {{ state.total }} 条日志记录</div>
      <van-loading v-if="state.loading" class="state-block" size="24px" vertical>加载中...</van-loading>
      <van-empty v-else-if="!state.list.length" description="暂无日志">
        <template #default>
          <div class="panel-hint">当前条件下暂无日志，可调整时间范围、traceId 或关键字重试。</div>
        </template>
      </van-empty>

      <div v-else class="list-wrap">
        <van-card v-for="item in state.list" :key="item.id" class="log-card">
          <template #title>
            <div class="log-title">
              <span>{{ item.title || '未命名日志' }}</span>
              <van-tag :type="levelTagType(item.level)">{{ item.level || 'ERROR' }}</van-tag>
            </div>
          </template>
          <template #desc>
            <div class="log-meta">类型：{{ logTypeLabel(item.logType) }}</div>
            <div class="log-meta">模块：{{ joinText(item.module, item.subModule) }}</div>
            <div class="log-meta">用户：{{ joinText(item.userName, item.orgName) || '-' }}</div>
            <div class="log-meta">错误码：{{ item.errorCode || '-' }}</div>
            <div class="log-meta">traceId：{{ item.traceId || '-' }}</div>
            <div class="log-meta">时间：{{ formatDateTime(item.createTime) }}</div>
            <div class="log-content">{{ item.summary || item.message || '-' }}</div>
            <div class="log-diagnosis">{{ item.diagnosis || '暂无诊断结论' }}</div>
          </template>
          <template #footer>
            <div class="action-row">
              <van-button size="small" plain type="primary" @click="openDetail(item)">查看详情</van-button>
              <van-button size="small" plain type="success" :disabled="!item.traceId" @click="applyTraceIdFilter(item.traceId)">按 traceId 串联</van-button>
            </div>
          </template>
        </van-card>
        <van-pagination
          v-if="state.total > state.pageSize"
          v-model="state.pageNo"
          :total-items="state.total"
          :items-per-page="state.pageSize"
          mode="simple"
          @change="handlePageChange"
        />
      </div>
    </section>

    <van-popup v-model:show="state.detailVisible" position="right" class="detail-popup">
      <div class="detail-panel">
        <div class="detail-header">
          <div>
            <div class="panel-title">日志详情</div>
            <div class="panel-hint">支持格式化查看与一键复制给 AI</div>
          </div>
          <div class="action-row">
            <van-button size="small" plain type="primary" :loading="state.detailLoading" :disabled="!state.detail" @click="handleCopyAi">
              复制给 AI
            </van-button>
            <van-button size="small" plain @click="state.detailVisible = false">关闭</van-button>
          </div>
        </div>

        <van-loading v-if="state.detailLoading" class="state-block" size="24px" vertical>详情加载中...</van-loading>
        <template v-else-if="state.detail">
          <section class="detail-section">
            <div class="detail-section__title">基本信息</div>
            <div class="detail-grid">
              <div class="detail-item"><span>标题</span><strong>{{ state.detail.title || '-' }}</strong></div>
              <div class="detail-item"><span>类型</span><strong>{{ logTypeLabel(state.detail.logType) }}</strong></div>
              <div class="detail-item"><span>模块</span><strong>{{ joinText(state.detail.module, state.detail.subModule) || '-' }}</strong></div>
              <div class="detail-item"><span>级别</span><strong>{{ state.detail.level || '-' }}</strong></div>
              <div class="detail-item"><span>用户</span><strong>{{ joinText(state.detail.userName, state.detail.userId) || '-' }}</strong></div>
              <div class="detail-item"><span>组织</span><strong>{{ joinText(state.detail.orgName, state.detail.orgId) || '-' }}</strong></div>
              <div class="detail-item detail-item--full"><span>traceId</span><strong>{{ state.detail.traceId || '-' }}</strong></div>
              <div class="detail-item detail-item--full"><span>发生时间</span><strong>{{ formatDateTime(state.detail.createTime) }}</strong></div>
            </div>
          </section>

          <section class="detail-section">
            <div class="detail-section__title">请求信息</div>
            <div class="detail-grid">
              <div class="detail-item detail-item--full"><span>请求地址</span><strong>{{ state.detail.requestUrl || '-' }}</strong></div>
              <div class="detail-item"><span>请求方法</span><strong>{{ state.detail.requestMethod || '-' }}</strong></div>
              <div class="detail-item"><span>响应状态</span><strong>{{ state.detail.responseStatus ?? '-' }}</strong></div>
              <div class="detail-item"><span>环境</span><strong>{{ state.detail.env || '-' }}</strong></div>
              <div class="detail-item"><span>设备</span><strong>{{ joinText(state.detail.deviceType, state.detail.platform) || '-' }}</strong></div>
              <div class="detail-item detail-item--full"><span>页面</span><strong>{{ state.detail.pageUrl || '-' }}</strong></div>
            </div>
            <pre class="json-block">{{ state.detail.requestParams || '{}' }}</pre>
          </section>

          <section class="detail-section">
            <div class="detail-section__title">错误信息</div>
            <div class="detail-text-block"><strong>错误码：</strong>{{ state.detail.errorCode || '-' }}</div>
            <div class="detail-text-block"><strong>摘要：</strong>{{ state.detail.summary || '-' }}</div>
            <div class="detail-text-block"><strong>消息：</strong>{{ state.detail.message || '-' }}</div>
          </section>

          <section class="detail-section">
            <div class="detail-section__title">诊断建议</div>
            <div class="detail-text-block">{{ state.detail.diagnosis || '暂无诊断建议' }}</div>
          </section>

          <section class="detail-section">
            <div class="detail-section__title">原始 JSON</div>
            <pre class="json-block">{{ state.detail.rawData || '{}' }}</pre>
          </section>
        </template>
      </div>
    </van-popup>
  </AppPageShell>
</template>

<script setup>
import { computed, onMounted, reactive } from 'vue'
import { showToast } from 'vant'
import AppPageShell from '@/components/layout/AppPageShell.vue'
import { queryLogCenterDetailApi, queryLogCenterPageApi } from '@/api/log-center'

const LOG_TYPE_OPTIONS = [
  { label: '前端 JS 异常', value: 'FRONTEND_JS_ERROR' },
  { label: '前端接口异常', value: 'FRONTEND_API_ERROR' },
  { label: '前端定位异常', value: 'FRONTEND_LOCATION_ERROR' },
  { label: '微信 JS-SDK 异常', value: 'WECHAT_JSAPI_ERROR' },
  { label: '后端业务异常', value: 'BACKEND_BUSINESS_ERROR' },
  { label: '后端未捕获异常', value: 'BACKEND_UNCAUGHT_EXCEPTION' },
  { label: '登录失败', value: 'LOGIN_FAILURE' },
  { label: '打卡失败', value: 'CHECK_IN_FAILURE' },
  { label: '审批失败', value: 'APPROVAL_FAILURE' }
]

const MODULE_OPTIONS = [
  { label: '认证', value: 'AUTH' },
  { label: '考勤', value: 'ATTENDANCE' },
  { label: '周报', value: 'WEEKLY_WORK' },
  { label: '微信', value: 'WECHAT' },
  { label: '日志中台', value: 'LOG_CENTER' },
  { label: '前端', value: 'FRONTEND' },
  { label: '系统', value: 'SYSTEM' }
]

const state = reactive({
  loading: false,
  detailLoading: false,
  detailVisible: false,
  list: [],
  total: 0,
  pageNo: 1,
  pageSize: 20,
  detail: null,
  queryForm: {
    traceId: '',
    logType: '',
    module: '',
    level: '',
    userKeyword: '',
    keyword: '',
    startTime: '',
    endTime: ''
  }
})

const querySummaryText = computed(() => {
  const parts = []
  if (state.queryForm.traceId) {
    parts.push(`traceId ${state.queryForm.traceId}`)
  }
  if (state.queryForm.logType) {
    parts.push(`类型 ${logTypeLabel(state.queryForm.logType)}`)
  }
  if (state.queryForm.module) {
    parts.push(`模块 ${state.queryForm.module}`)
  }
  if (state.queryForm.userKeyword) {
    parts.push(`用户 ${state.queryForm.userKeyword}`)
  }
  if (state.queryForm.keyword) {
    parts.push(`关键字 ${state.queryForm.keyword}`)
  }
  if (state.queryForm.startTime || state.queryForm.endTime) {
    parts.push(`时间 ${formatRangeText(state.queryForm.startTime, state.queryForm.endTime)}`)
  }
  return parts.join(' / ') || '全部日志'
})

function normalizeResponse(response, fallbackMessage) {
  if (!response || response.code !== 0) {
    throw new Error(response?.message || fallbackMessage)
  }
  return response.data
}

function levelTagType(level) {
  if (level === 'WARN') {
    return 'warning'
  }
  if (level === 'INFO') {
    return 'primary'
  }
  return 'danger'
}

function logTypeLabel(value) {
  return LOG_TYPE_OPTIONS.find(item => item.value === value)?.label || value || '-'
}

function joinText(...parts) {
  return parts.filter(Boolean).join(' / ')
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

function toBackendDateTime(value) {
  if (!value) {
    return undefined
  }
  return `${value.replace('T', ' ')}:00`
}

async function fetchList() {
  state.loading = true
  try {
    const data = normalizeResponse(await queryLogCenterPageApi({
      pageNo: state.pageNo,
      pageSize: state.pageSize,
      traceId: state.queryForm.traceId || undefined,
      logType: state.queryForm.logType || undefined,
      module: state.queryForm.module || undefined,
      level: state.queryForm.level || undefined,
      userKeyword: state.queryForm.userKeyword || undefined,
      keyword: state.queryForm.keyword || undefined,
      startTime: toBackendDateTime(state.queryForm.startTime),
      endTime: toBackendDateTime(state.queryForm.endTime)
    }), '日志加载失败')
    state.list = Array.isArray(data.list) ? data.list : []
    state.total = Number(data.total || 0)
  } catch (error) {
    showToast(error.message || '日志加载失败')
  } finally {
    state.loading = false
  }
}

async function openDetail(item) {
  state.detailVisible = true
  state.detailLoading = true
  state.detail = null
  try {
    state.detail = normalizeResponse(await queryLogCenterDetailApi(item.id), '日志详情加载失败')
  } catch (error) {
    showToast(error.message || '日志详情加载失败')
  } finally {
    state.detailLoading = false
  }
}

function handleSearch() {
  state.pageNo = 1
  fetchList()
}

function handleReset() {
  state.pageNo = 1
  state.queryForm.traceId = ''
  state.queryForm.logType = ''
  state.queryForm.module = ''
  state.queryForm.level = ''
  state.queryForm.userKeyword = ''
  state.queryForm.keyword = ''
  state.queryForm.startTime = ''
  state.queryForm.endTime = ''
  fetchList()
}

function handlePageChange(pageNo) {
  state.pageNo = pageNo
  fetchList()
}

function applyTraceIdFilter(traceId) {
  if (!traceId) {
    return
  }
  state.queryForm.traceId = traceId
  state.pageNo = 1
  fetchList()
}

async function handleCopyAi() {
  if (!state.detail?.aiAnalysisText) {
    showToast('暂无可复制内容')
    return
  }
  try {
    await navigator.clipboard.writeText(state.detail.aiAnalysisText)
    showToast('已复制 AI 分析文本')
  } catch (error) {
    showToast('复制失败，请检查浏览器剪贴板权限')
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
  display: grid;
  gap: 12px;
  margin-top: 16px;
}

.log-card {
  border-radius: 14px;
  box-shadow: none;
  border: 1px solid #eef2f7;
}

.log-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.log-meta {
  margin-top: 4px;
  color: #6b7280;
  font-size: 12px;
}

.log-content {
  margin-top: 10px;
  color: #1f2937;
  line-height: 1.6;
}

.log-diagnosis {
  margin-top: 8px;
  padding: 10px 12px;
  border-radius: 10px;
  background: #f8fafc;
  color: #475569;
  font-size: 13px;
  line-height: 1.6;
}

.detail-popup {
  width: min(720px, 100vw);
  height: 100vh;
  background: #f6f8fb;
}

.detail-panel {
  height: 100%;
  padding: 20px 16px 28px;
  overflow-y: auto;
}

.detail-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.detail-section {
  margin-top: 16px;
  padding: 16px;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.06);
}

.detail-section__title {
  font-size: 15px;
  font-weight: 600;
  color: #111827;
}

.detail-grid {
  display: grid;
  gap: 12px;
  margin-top: 12px;
}

.detail-item {
  display: grid;
  gap: 4px;
}

.detail-item span {
  color: #6b7280;
  font-size: 12px;
}

.detail-item strong {
  color: #1f2937;
  font-size: 13px;
  line-height: 1.6;
  word-break: break-all;
}

.detail-item--full {
  grid-column: 1 / -1;
}

.detail-text-block {
  margin-top: 12px;
  color: #1f2937;
  line-height: 1.7;
  word-break: break-word;
}

.json-block {
  margin: 12px 0 0;
  padding: 12px;
  border-radius: 10px;
  background: #0f172a;
  color: #e2e8f0;
  font-size: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}
</style>
