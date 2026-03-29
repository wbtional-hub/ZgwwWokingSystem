<template>
  <AppPageShell title="AI接入区" description="在这里配置智能体接入地址、Token 和默认模型，并测试该接入是否可用。">
    <template #actions>
      <div class="action-row">
        <van-button plain type="primary" :loading="state.loading" @click="fetchProviders">刷新列表</van-button>
      </div>
    </template>

    <section class="panel-grid" data-guide="ai-provider-config">
      <section class="panel">
        <div class="panel-title">接入查询</div>
        <van-field v-model.trim="state.query.keywords" label="关键字" placeholder="按编码或名称查询" />
        <div class="select-field">
          <span class="select-label">状态</span>
          <select v-model="state.query.status">
            <option value="">全部</option>
            <option value="1">启用</option>
            <option value="0">停用</option>
          </select>
        </div>
        <div class="select-field">
          <span class="select-label">连通性</span>
          <select v-model="state.query.connectStatus">
            <option value="">全部</option>
            <option value="SUCCESS">成功</option>
            <option value="FAILED">失败</option>
            <option value="PENDING">待测试</option>
          </select>
        </div>
        <div class="action-row">
          <van-button size="small" type="primary" :loading="state.loading" @click="fetchProviders">查询</van-button>
          <van-button size="small" plain @click="resetQuery">重置</van-button>
        </div>
      </section>

      <section class="panel">
        <div class="panel-title">接入维护</div>
        <van-field v-model.trim="state.form.providerCode" label="接入编码" placeholder="例如：openai_main" :readonly="Boolean(state.form.id)" />
        <van-field v-model.trim="state.form.providerName" label="接入名称" placeholder="例如：OpenAI 主接入" />
        <van-field v-model.trim="state.form.apiBaseUrl" label="Base URL" placeholder="例如：https://api.openai.com/v1" />
        <van-field v-model.trim="state.form.apiToken" label="API Token" type="password" placeholder="编辑时留空则保持原值" />
        <van-field v-model.trim="state.form.defaultModel" label="默认模型" placeholder="例如：gpt-5.4" />
        <van-field v-model="state.form.remark" label="说明" type="textarea" rows="3" autosize placeholder="接入说明或使用范围" />
        <div class="select-field">
          <span class="select-label">状态</span>
          <select v-model="state.form.status">
            <option :value="1">启用</option>
            <option :value="0">停用</option>
          </select>
        </div>
        <div class="action-row">
          <van-button size="small" type="primary" :loading="state.submitting" @click="handleSave">保存接入</van-button>
          <van-button size="small" plain @click="resetForm">清空</van-button>
        </div>
      </section>
    </section>

    <section class="panel" data-guide="ai-provider-list">
      <div class="panel-title">接入列表</div>
      <van-loading v-if="state.loading" class="state-block" size="24px" vertical>加载中...</van-loading>
      <van-empty v-else-if="!state.list.length" description="暂无 AI 接入配置" />
      <div v-else class="card-list">
        <van-card v-for="item in state.list" :key="item.id" class="provider-card">
          <template #title>
            <div class="card-title-row">
              <div class="title-with-badge">
                <span>{{ item.providerName }}</span>
                <van-tag :type="connectType(item.connectStatus)">{{ item.connectStatus || 'PENDING' }}</van-tag>
              </div>
              <van-tag :type="Number(item.status) === 1 ? 'success' : 'danger'">{{ Number(item.status) === 1 ? '启用' : '停用' }}</van-tag>
            </div>
          </template>
          <template #desc>
            <div class="meta-line">编码：{{ item.providerCode }}</div>
            <div class="meta-line">Base URL：{{ item.apiBaseUrl }}</div>
            <div class="meta-line">Token：{{ item.tokenMask || '-' }}</div>
            <div class="meta-line">默认模型：{{ item.defaultModel || '-' }}</div>
            <div class="meta-line">模型数：{{ item.modelCount || 0 }}</div>
            <div class="meta-line">说明：{{ item.remark || '-' }}</div>
            <div class="meta-line">更新时间：{{ formatDateTime(item.updateTime) }}</div>
            <div v-if="item.models && item.models.length" class="model-wrap">
              <van-tag v-for="model in item.models" :key="model.modelCode" plain type="primary">{{ model.modelCode }}</van-tag>
            </div>
          </template>
          <template #footer>
            <div class="action-row">
              <van-button size="small" plain type="warning" @click="fillForm(item)">编辑</van-button>
              <van-button size="small" plain type="primary" :loading="state.testingId === item.id" @click="handleTest(item)">测试连通</van-button>
              <van-button size="small" plain :type="Number(item.status) === 1 ? 'danger' : 'success'" :loading="state.togglingId === item.id" @click="handleToggleStatus(item)">
                {{ Number(item.status) === 1 ? '停用' : '启用' }}
              </van-button>
            </div>
          </template>
        </van-card>
      </div>
    </section>

    <section v-if="state.lastTestResult" class="panel" data-guide="ai-provider-test-result">
      <div class="panel-title">最近一次测试结果</div>
      <div class="meta-line">接入ID：{{ state.lastTestResult.providerConfigId }}</div>
      <div class="meta-line">状态：{{ state.lastTestResult.connectStatus }}</div>
      <div class="meta-line">说明：{{ state.lastTestResult.message || '-' }}</div>
      <div class="meta-line">识别模型数：{{ state.lastTestResult.modelCount || 0 }}</div>
      <div v-if="state.lastTestResult.models && state.lastTestResult.models.length" class="model-wrap">
        <van-tag v-for="model in state.lastTestResult.models" :key="model.modelCode" plain type="success">{{ model.modelCode }}</van-tag>
      </div>
    </section>
  </AppPageShell>
</template>

<script setup>
import { onMounted, reactive } from 'vue'
import { showConfirmDialog, showToast } from 'vant'
import AppPageShell from '@/components/layout/AppPageShell.vue'
import { queryAiProviderList, saveAiProvider, testAiProvider, toggleAiProviderStatus } from '@/api/ai'

const state = reactive({
  loading: false,
  submitting: false,
  togglingId: null,
  testingId: null,
  list: [],
  lastTestResult: null,
  query: {
    keywords: '',
    status: '',
    connectStatus: ''
  },
  form: createEmptyForm()
})

function createEmptyForm() {
  return {
    id: null,
    providerCode: '',
    providerName: '',
    apiBaseUrl: '',
    apiToken: '',
    defaultModel: '',
    remark: '',
    status: 1
  }
}

function ensureSuccess(response, fallback = '请求失败') {
  if (!response || response.code !== 0) {
    throw new Error(response?.message || fallback)
  }
  return response.data
}

function formatDateTime(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ')
}

function connectType(status) {
  if (status === 'SUCCESS') return 'success'
  if (status === 'FAILED') return 'danger'
  return 'warning'
}

function resetForm() {
  state.form = createEmptyForm()
}

function resetQuery() {
  state.query.keywords = ''
  state.query.status = ''
  state.query.connectStatus = ''
  fetchProviders()
}

function fillForm(item) {
  state.form = {
    id: item.id,
    providerCode: item.providerCode || '',
    providerName: item.providerName || '',
    apiBaseUrl: item.apiBaseUrl || '',
    apiToken: '',
    defaultModel: item.defaultModel || '',
    remark: item.remark || '',
    status: Number(item.status) === 1 ? 1 : 0
  }
}

async function fetchProviders() {
  state.loading = true
  try {
    const data = ensureSuccess(await queryAiProviderList({
      keywords: state.query.keywords || undefined,
      status: state.query.status === '' ? undefined : Number(state.query.status),
      connectStatus: state.query.connectStatus || undefined
    }), 'AI 接入列表加载失败')
    state.list = Array.isArray(data) ? data : []
  } catch (error) {
    showToast(error.message || 'AI 接入列表加载失败')
  } finally {
    state.loading = false
  }
}

async function handleSave() {
  if (!state.form.providerCode.trim()) {
    showToast('请输入接入编码')
    return
  }
  if (!state.form.providerName.trim()) {
    showToast('请输入接入名称')
    return
  }
  if (!state.form.apiBaseUrl.trim()) {
    showToast('请输入 API Base URL')
    return
  }
  if (!state.form.id && !state.form.apiToken.trim()) {
    showToast('新增接入时必须填写 API Token')
    return
  }
  state.submitting = true
  try {
    ensureSuccess(await saveAiProvider({
      id: state.form.id || undefined,
      providerCode: state.form.providerCode.trim(),
      providerName: state.form.providerName.trim(),
      apiBaseUrl: state.form.apiBaseUrl.trim(),
      apiToken: state.form.apiToken.trim() || undefined,
      defaultModel: state.form.defaultModel.trim() || undefined,
      remark: state.form.remark || undefined,
      status: Number(state.form.status)
    }), 'AI 接入保存失败')
    showToast(state.form.id ? 'AI 接入已更新' : 'AI 接入已创建')
    resetForm()
    await fetchProviders()
  } catch (error) {
    showToast(error.message || 'AI 接入保存失败')
  } finally {
    state.submitting = false
  }
}

async function handleToggleStatus(item) {
  const nextStatus = Number(item.status) === 1 ? 0 : 1
  state.togglingId = item.id
  try {
    await showConfirmDialog({
      title: nextStatus === 1 ? '确认启用' : '确认停用',
      message: `${nextStatus === 1 ? '启用' : '停用'}接入 ${item.providerName} 吗？`
    })
    ensureSuccess(await toggleAiProviderStatus({ id: item.id, status: nextStatus }), '状态切换失败')
    showToast(nextStatus === 1 ? 'AI 接入已启用' : 'AI 接入已停用')
    await fetchProviders()
  } catch (error) {
    if (error?.message && error.message !== 'cancel') {
      showToast(error.message || '状态切换失败')
    }
  } finally {
    state.togglingId = null
  }
}

async function handleTest(item) {
  state.testingId = item.id
  try {
    const data = ensureSuccess(await testAiProvider({ id: item.id }), '连通测试失败')
    state.lastTestResult = data
    showToast(data.connectStatus === 'SUCCESS' ? '连接成功' : '连接失败')
    await fetchProviders()
  } catch (error) {
    showToast(error.message || '连通测试失败')
  } finally {
    state.testingId = null
  }
}

onMounted(() => {
  fetchProviders()
})
</script>

<style scoped>
.panel-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 16px;
}

.panel {
  margin-bottom: 16px;
  padding: 16px;
  background: #fff;
  border-radius: 12px;
  border: 1px solid #e5e7eb;
}

.panel-title {
  margin-bottom: 8px;
  font-size: 16px;
  font-weight: 600;
  color: #111827;
}

.action-row,
.model-wrap {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
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
  flex: 0 0 72px;
  font-size: 14px;
  color: #323233;
}

.select-field select {
  flex: 1;
  border: 0;
  background: transparent;
  font-size: 14px;
}

.meta-line {
  color: #4b5563;
  font-size: 13px;
  line-height: 1.7;
}

.card-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.card-title-row,
.title-with-badge {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.provider-card {
  border-radius: 12px;
  overflow: hidden;
}

.state-block {
  padding: 20px 0;
}

@media (max-width: 960px) {
  .panel-grid {
    grid-template-columns: 1fr;
  }
}
</style>
