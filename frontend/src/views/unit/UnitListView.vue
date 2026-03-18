<template>
  <AppPageShell title="单位管理" description="当前页提供单位列表、新增、编辑、删除和启停的最小可维护版本。">
    <template #actions>
      <div class="page-actions">
        <van-button type="primary" :loading="state.loading" @click="loadUnits">刷新</van-button>
        <van-button type="success" @click="openCreateDialog">新增单位</van-button>
      </div>
    </template>

    <div class="page-tip">单位用于管理、筛选、统计和导出；当前阶段仍以组织架构作为主关系树。</div>

    <div class="query-row">
      <van-search
        v-model="state.keyword"
        placeholder="按单位名称或编码搜索"
        @search="normalizeKeyword"
        @blur="normalizeKeyword"
      />
    </div>

    <div v-if="filteredUnits.length" class="summary-row">
      当前共 {{ filteredUnits.length }} 个单位，已启用 {{ enabledCount }} 个，停用 {{ disabledCount }} 个。
    </div>

    <van-loading v-if="state.loading" class="page-loading" size="24px" vertical>单位列表加载中...</van-loading>
    <van-empty v-else-if="!filteredUnits.length" description="当前没有可展示的单位数据">
      <template #default>
        <div class="empty-tip">可先新增单位，后续在组织架构中为节点选择所属单位。</div>
      </template>
    </van-empty>

    <div v-else class="unit-list">
      <div v-for="item in filteredUnits" :key="item.id" class="unit-card">
        <div class="unit-title">
          <span>{{ item.unitName }}</span>
          <van-tag :type="Number(item.status) === 1 ? 'success' : 'danger'">
            {{ Number(item.status) === 1 ? '启用' : '停用' }}
          </van-tag>
        </div>
        <div class="unit-meta">单位ID：{{ item.id }}</div>
        <div class="unit-meta">单位编码：{{ item.unitCode }}</div>
        <div class="unit-meta">管理员：{{ item.adminRealName || item.adminUsername || '-' }}</div>
        <div class="unit-meta">创建时间：{{ formatDateTime(item.createTime) }}</div>
        <div class="unit-actions">
          <van-button size="small" plain type="primary" @click="openEditDialog(item)">编辑</van-button>
          <van-button
            size="small"
            plain
            :type="Number(item.status) === 1 ? 'warning' : 'success'"
            @click="handleToggleStatus(item)"
          >
            {{ Number(item.status) === 1 ? '停用' : '启用' }}
          </van-button>
          <van-button size="small" plain type="danger" @click="handleDelete(item)">删除</van-button>
        </div>
      </div>
    </div>

    <van-popup v-model:show="state.dialogVisible" position="bottom" round>
      <div class="dialog-body">
        <div class="dialog-title">{{ state.dialogMode === 'create' ? '新增单位' : '编辑单位' }}</div>
        <van-form @submit="handleSubmit">
          <van-field v-model="state.form.unitName" label="单位名称" placeholder="请输入单位名称" required />
          <van-field v-model="state.form.unitCode" label="单位编码" placeholder="请输入单位编码" required />
          <van-field
            :model-value="Number(state.form.status) === 1 ? '启用' : '停用'"
            label="状态"
            readonly
            is-link
            @click="state.statusPickerVisible = true"
          />
          <div class="dialog-actions">
            <van-button block plain @click="state.dialogVisible = false">取消</van-button>
            <van-button block type="primary" native-type="submit" :loading="state.submitting">保存</van-button>
          </div>
        </van-form>
      </div>
    </van-popup>

    <van-popup v-model:show="state.statusPickerVisible" position="bottom" round>
      <van-picker :columns="statusColumns" @confirm="handleStatusConfirm" @cancel="state.statusPickerVisible = false" />
    </van-popup>
  </AppPageShell>
</template>

<script setup>
import { computed, onMounted, reactive } from 'vue'
import { showConfirmDialog, showToast } from 'vant'
import AppPageShell from '@/components/layout/AppPageShell.vue'
import { createUnit, deleteUnit, queryUnitList, toggleUnitStatus, updateUnit } from '@/api/unit'

const statusColumns = [
  { text: '启用', value: 1 },
  { text: '停用', value: 0 }
]

const state = reactive({
  loading: false,
  submitting: false,
  keyword: '',
  units: [],
  dialogVisible: false,
  dialogMode: 'create',
  statusPickerVisible: false,
  form: {
    id: null,
    unitName: '',
    unitCode: '',
    status: 1
  }
})

const filteredUnits = computed(() => {
  const keyword = state.keyword.trim()
  if (!keyword) {
    return state.units
  }
  return state.units.filter((item) =>
    String(item.unitName || '').includes(keyword) || String(item.unitCode || '').includes(keyword)
  )
})

const enabledCount = computed(() => filteredUnits.value.filter((item) => Number(item.status) === 1).length)
const disabledCount = computed(() => filteredUnits.value.filter((item) => Number(item.status) !== 1).length)

function ensureSuccess(response, fallback = '请求失败') {
  if (!response || response.code !== 0) {
    throw new Error(response?.message || fallback)
  }
  return response.data
}

async function loadUnits() {
  state.loading = true
  try {
    state.units = ensureSuccess(await queryUnitList(), '单位列表加载失败') || []
  } catch (error) {
    showToast(error.message || '单位列表加载失败')
  } finally {
    state.loading = false
  }
}

function normalizeKeyword() {
  state.keyword = state.keyword.trim()
}

function openCreateDialog() {
  state.dialogMode = 'create'
  state.form.id = null
  state.form.unitName = ''
  state.form.unitCode = ''
  state.form.status = 1
  state.dialogVisible = true
}

function openEditDialog(item) {
  state.dialogMode = 'edit'
  state.form.id = item.id
  state.form.unitName = item.unitName || ''
  state.form.unitCode = item.unitCode || ''
  state.form.status = Number(item.status) === 1 ? 1 : 0
  state.dialogVisible = true
}

async function handleSubmit() {
  const payload = {
    id: state.form.id,
    unitName: state.form.unitName.trim(),
    unitCode: state.form.unitCode.trim(),
    status: state.form.status
  }
  if (!payload.unitName || !payload.unitCode) {
    showToast('请先填写单位名称和单位编码')
    return
  }
  state.submitting = true
  try {
    if (state.dialogMode === 'create') {
      ensureSuccess(await createUnit(payload), '新增单位失败')
      showToast('新增成功')
    } else {
      ensureSuccess(await updateUnit(payload), '编辑单位失败')
      showToast('保存成功')
    }
    state.dialogVisible = false
    await loadUnits()
  } catch (error) {
    showToast(error.message || '单位保存失败')
  } finally {
    state.submitting = false
  }
}

async function handleToggleStatus(item) {
  try {
    await showConfirmDialog({
      title: Number(item.status) === 1 ? '停用单位' : '启用单位',
      message: `确认${Number(item.status) === 1 ? '停用' : '启用'}单位“${item.unitName}”吗？`
    })
    ensureSuccess(await toggleUnitStatus({ id: item.id, status: Number(item.status) === 1 ? 0 : 1 }), '更新状态失败')
    showToast('状态已更新')
    await loadUnits()
  } catch (error) {
    if (error?.message) {
      showToast(error.message)
    }
  }
}

async function handleDelete(item) {
  try {
    await showConfirmDialog({
      title: '删除单位',
      message: `确认删除单位“${item.unitName}”吗？`
    })
    ensureSuccess(await deleteUnit(item.id), '删除单位失败')
    showToast('删除成功')
    await loadUnits()
  } catch (error) {
    if (error?.message) {
      showToast(error.message)
    }
  }
}

function handleStatusConfirm(option) {
  state.form.status = Number(Array.isArray(option) ? option[0]?.selectedValues?.[0] : option.selectedValues?.[0] ?? option.value)
  state.statusPickerVisible = false
}

function formatDateTime(value) {
  if (!value) {
    return '-'
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  const month = `${date.getMonth() + 1}`.padStart(2, '0')
  const day = `${date.getDate()}`.padStart(2, '0')
  const hour = `${date.getHours()}`.padStart(2, '0')
  const minute = `${date.getMinutes()}`.padStart(2, '0')
  return `${month}-${day} ${hour}:${minute}`
}

onMounted(() => {
  loadUnits()
})
</script>

<style scoped>
.page-actions,
.unit-actions,
.dialog-actions {
  display: flex;
  gap: 12px;
}

.page-tip,
.summary-row,
.empty-tip {
  margin: 12px 0;
  color: #666;
  font-size: 14px;
}

.unit-list {
  display: grid;
  gap: 12px;
}

.unit-card,
.dialog-body {
  padding: 16px;
  background: #fff;
  border-radius: 12px;
}

.unit-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  font-size: 16px;
  font-weight: 600;
}

.unit-meta {
  margin-top: 4px;
  color: #666;
  font-size: 13px;
}

.unit-actions,
.dialog-actions {
  margin-top: 16px;
}

.dialog-title {
  margin-bottom: 16px;
  font-size: 16px;
  font-weight: 600;
}
</style>
