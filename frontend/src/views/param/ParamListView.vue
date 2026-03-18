<template>
  <AppPageShell title="参数配置" description="当前页已接入最小可用参数工作流：列表、查询、新增、编辑和删除/启停。">
    <template #actions>
      <div class="action-row">
        <van-button type="primary" :disabled="pageBusy" @click="openCreateForm">新增参数</van-button>
        <van-button plain type="primary" :loading="state.loading" :disabled="pageBusy" @click="fetchList">刷新列表</van-button>
      </div>
    </template>

    <section class="panel">
      <div class="panel-title">查询区</div>
      <van-field v-model.trim="state.queryForm.keywords" label="关键词" placeholder="按编码或名称搜索" :disabled="pageBusy" />
      <div class="select-field">
        <span class="select-label">状态</span>
        <select v-model="state.queryForm.status" :disabled="pageBusy">
          <option value="">全部</option>
          <option value="1">启用</option>
          <option value="0">停用</option>
        </select>
      </div>
      <div class="action-row">
        <van-button size="small" type="primary" :loading="state.loading" :disabled="pageBusy" @click="handleSearch">查询</van-button>
        <van-button size="small" plain :disabled="pageBusy" @click="handleReset">重置</van-button>
      </div>
    </section>

    <section class="panel">
      <div class="panel-title">{{ state.form.id ? `编辑参数 #${state.form.id}` : '新增参数' }}</div>
      <van-field v-model.trim="state.form.paramCode" label="参数编码" placeholder="请输入参数编码" :readonly="Boolean(state.form.id)" :disabled="pageBusy" />
      <van-field v-model.trim="state.form.paramName" label="参数名称" placeholder="请输入参数名称" :disabled="pageBusy" />
      <van-field v-model="state.form.paramValue" label="参数值" type="textarea" rows="3" autosize placeholder="请输入参数值" :disabled="pageBusy" />
      <van-field v-model="state.form.remark" label="备注" type="textarea" rows="2" autosize placeholder="可选" :disabled="pageBusy" />
      <div class="select-field">
        <span class="select-label">状态</span>
        <select v-model="state.form.status" :disabled="pageBusy">
          <option :value="1">启用</option>
          <option :value="0">停用</option>
        </select>
      </div>
      <div class="action-row">
        <van-button size="small" type="primary" :loading="state.submitting" :disabled="pageBusy" @click="handleSave">保存</van-button>
        <van-button size="small" plain :disabled="pageBusy" @click="resetForm">清空</van-button>
      </div>
    </section>

    <section class="panel">
      <div class="panel-title">参数列表</div>
      <div class="panel-hint">共 {{ state.list.length }} 条参数记录</div>

      <van-loading v-if="state.loading" class="state-block" size="24px" vertical>加载中...</van-loading>
      <van-empty v-else-if="!state.list.length" description="暂无参数数据">
        <template #default>
          <div class="panel-hint">可先新增一条参数，保存后会立即出现在列表中。</div>
        </template>
      </van-empty>

      <div v-else class="list-wrap">
        <van-card v-for="item in state.list" :key="item.id" class="param-card">
          <template #title>
            <div class="param-title">
              <span>{{ item.paramName }}</span>
              <van-tag :type="Number(item.status) === 1 ? 'success' : 'danger'">{{ Number(item.status) === 1 ? '启用' : '停用' }}</van-tag>
            </div>
          </template>
          <template #desc>
            <div class="param-meta">编码：{{ item.paramCode }}</div>
            <div class="param-meta">值：{{ item.paramValue || '-' }}</div>
            <div class="param-meta">备注：{{ item.remark || '-' }}</div>
            <div class="param-meta">更新时间：{{ formatDateTime(item.updateTime) }}</div>
          </template>
          <template #footer>
            <div class="action-row">
              <van-button size="small" plain type="warning" :disabled="pageBusy" @click="openEditForm(item)">编辑</van-button>
              <van-button
                size="small"
                plain
                :type="Number(item.status) === 1 ? 'danger' : 'success'"
                :loading="state.togglingId === item.id"
                :disabled="pageBusy"
                @click="handleToggleStatus(item)"
              >
                {{ Number(item.status) === 1 ? '停用' : '启用' }}
              </van-button>
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
import { deleteParam, queryParamList, saveParam, toggleParamStatus } from '@/api/param'

const state = reactive({
  loading: false,
  submitting: false,
  deletingId: null,
  togglingId: null,
  list: [],
  queryForm: {
    keywords: '',
    status: ''
  },
  form: createEmptyForm()
})

const pageBusy = computed(() => {
  return state.loading || state.submitting || state.deletingId !== null || state.togglingId !== null
})

function createEmptyForm() {
  return {
    id: null,
    paramCode: '',
    paramName: '',
    paramValue: '',
    status: 1,
    remark: ''
  }
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
    const response = await queryParamList({
      keywords: state.queryForm.keywords || undefined,
      status: state.queryForm.status === '' ? undefined : Number(state.queryForm.status)
    })
    state.list = Array.isArray(response.data) ? response.data : []
  } catch (error) {
    showToast(error.message || '参数列表加载失败')
  } finally {
    state.loading = false
  }
}

function handleSearch() {
  fetchList()
}

function handleReset() {
  state.queryForm.keywords = ''
  state.queryForm.status = ''
  fetchList()
}

function resetForm() {
  state.form = createEmptyForm()
}

function openCreateForm() {
  resetForm()
  showToast('已切换到新增参数')
}

function openEditForm(item) {
  state.form = {
    id: item.id,
    paramCode: item.paramCode || '',
    paramName: item.paramName || '',
    paramValue: item.paramValue || '',
    status: Number(item.status) || 0,
    remark: item.remark || ''
  }
}

async function handleSave() {
  if (!state.form.paramCode.trim()) {
    showToast('请输入参数编码')
    return
  }
  if (!state.form.paramName.trim()) {
    showToast('请输入参数名称')
    return
  }

  state.submitting = true
  try {
    await saveParam({
      id: state.form.id || undefined,
      paramCode: state.form.paramCode.trim(),
      paramName: state.form.paramName.trim(),
      paramValue: state.form.paramValue,
      status: Number(state.form.status),
      remark: state.form.remark
    })
    showToast(state.form.id ? '参数编辑成功' : '参数新增成功')
    resetForm()
    await fetchList()
  } catch (error) {
    showToast(error.message || '参数保存失败')
  } finally {
    state.submitting = false
  }
}

async function handleDelete(item) {
  state.deletingId = item.id
  try {
    await showConfirmDialog({
      title: '确认删除',
      message: `确认删除参数 ${item.paramName} 吗？`
    })
    await deleteParam(item.id)
    showToast('参数删除成功')
    if (state.form.id === item.id) {
      resetForm()
    }
    await fetchList()
  } catch (error) {
    if (error?.message && error.message !== 'cancel') {
      showToast(error.message || '参数删除失败')
    }
  } finally {
    state.deletingId = null
  }
}

async function handleToggleStatus(item) {
  const nextStatus = Number(item.status) === 1 ? 0 : 1
  state.togglingId = item.id
  try {
    await showConfirmDialog({
      title: nextStatus === 1 ? '确认启用' : '确认停用',
      message: `${nextStatus === 1 ? '启用' : '停用'}参数 ${item.paramName} 吗？`
    })
    await toggleParamStatus({
      id: item.id,
      status: nextStatus
    })
    showToast(nextStatus === 1 ? '参数已启用' : '参数已停用')
    await fetchList()
  } catch (error) {
    if (error?.message && error.message !== 'cancel') {
      showToast(error.message || '参数状态修改失败')
    }
  } finally {
    state.togglingId = null
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
.param-meta {
  font-size: 13px;
  color: #4b5563;
  line-height: 1.6;
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

.param-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.state-block {
  padding: 20px 0;
}
</style>
