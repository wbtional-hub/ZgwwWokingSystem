<template>
  <AppPageShell
    title="节假日维护"
    description="维护年度法定节假日、调整休息日与补班工作日。AI 只生成候选，人工确认后才生效。"
    help-key="attendance"
  >
    <AttendanceWorkspaceTabs />

    <section class="holiday-panel">
      <div class="toolbar">
        <label class="year-field">
          <span>年度</span>
          <input v-model.number="selectedYear" type="number" min="2000" max="2100">
        </label>
        <van-button plain type="primary" :loading="loading" @click="fetchList">查询</van-button>
        <van-button type="primary" :loading="generating" @click="handleGenerateAi">AI 生成年度节假日</van-button>
        <van-button plain type="success" @click="openEditor()">新增</van-button>
        <van-button type="success" :loading="confirming" @click="handleConfirmYear">确认本年度生效</van-button>
      </div>

      <div v-if="!loading && records.length === 0" class="empty-tip">
        当前年度暂无节假日数据，可使用 AI 生成候选数据。
      </div>

      <div class="holiday-table">
        <div class="holiday-table__head">
          <span>日期</span>
          <span>星期</span>
          <span>类型</span>
          <span>名称</span>
          <span>生效</span>
          <span>来源</span>
          <span>状态</span>
          <span>备注</span>
          <span>操作</span>
        </div>

        <div v-for="item in records" :key="item.id || item.calendarDate" class="holiday-row">
          <span class="date-cell">{{ item.calendarDate }}</span>
          <span>{{ item.weekday || '-' }}</span>
          <span>
            <em class="type-badge" :class="`type-badge--${item.dayType || 'UNKNOWN'}`">
              {{ item.dayTypeLabel || item.dayType || '-' }}
            </em>
          </span>
          <span>{{ item.name || '-' }}</span>
          <span>{{ item.enabled ? '已生效' : '候选' }}</span>
          <span>{{ sourceLabel(item.source) }}</span>
          <span>{{ statusLabel(item.status) }}</span>
          <span class="remark-cell">{{ item.remark || '-' }}</span>
          <span class="row-actions">
            <button type="button" @click="openEditor(item)">编辑</button>
            <button type="button" class="danger" @click="handleDelete(item)">删除</button>
          </span>
        </div>
      </div>
    </section>

    <van-popup v-model:show="editorVisible" position="bottom" round>
      <div class="editor">
        <div class="editor__title">{{ form.id ? '编辑节假日' : '新增节假日' }}</div>
        <label class="editor-field">
          <span>日期</span>
          <input v-model="form.calendarDate" type="date">
        </label>
        <label class="editor-field">
          <span>类型</span>
          <select v-model="form.dayType">
            <option value="HOLIDAY">法定节假日</option>
            <option value="WEEKDAY_REST">调整休息日</option>
            <option value="MAKEUP_WORKDAY">补班工作日</option>
          </select>
        </label>
        <label class="editor-field">
          <span>名称</span>
          <input v-model.trim="form.name" type="text" placeholder="例如：劳动节假期">
        </label>
        <label class="editor-field">
          <span>是否生效</span>
          <select v-model="form.enabled">
            <option :value="true">生效</option>
            <option :value="false">候选</option>
          </select>
        </label>
        <label class="editor-field">
          <span>来源</span>
          <select v-model="form.source">
            <option value="MANUAL">人工</option>
            <option value="AI">AI</option>
          </select>
        </label>
        <label class="editor-field">
          <span>状态</span>
          <select v-model="form.status">
            <option value="DRAFT">候选</option>
            <option value="CONFIRMED">已确认</option>
          </select>
        </label>
        <label class="editor-field">
          <span>备注</span>
          <textarea v-model.trim="form.remark" rows="3" placeholder="补充说明"></textarea>
        </label>
        <div class="editor-actions">
          <van-button plain @click="editorVisible = false">取消</van-button>
          <van-button type="primary" :loading="saving" @click="handleSave">保存</van-button>
        </div>
      </div>
    </van-popup>
  </AppPageShell>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { showConfirmDialog, showToast } from 'vant'
import AppPageShell from '@/components/layout/AppPageShell.vue'
import AttendanceWorkspaceTabs from '@/components/attendance/AttendanceWorkspaceTabs.vue'
import {
  confirmAttendanceHolidayCalendarYearApi,
  deleteAttendanceHolidayCalendarApi,
  generateAttendanceHolidayCalendarAiApi,
  queryAttendanceHolidayCalendarApi,
  saveAttendanceHolidayCalendarApi
} from '@/api/attendance'

const currentYear = new Date().getFullYear()
const selectedYear = ref(currentYear)
const records = ref([])
const loading = ref(false)
const generating = ref(false)
const saving = ref(false)
const confirming = ref(false)
const editorVisible = ref(false)

const form = reactive({
  id: null,
  calendarDate: '',
  dayType: 'HOLIDAY',
  name: '',
  enabled: true,
  source: 'MANUAL',
  status: 'CONFIRMED',
  remark: ''
})

onMounted(fetchList)

async function fetchList() {
  loading.value = true
  try {
    const response = await queryAttendanceHolidayCalendarApi({ year: selectedYear.value })
    records.value = ensureSuccess(response) || []
  } finally {
    loading.value = false
  }
}

async function handleGenerateAi() {
  try {
    await showConfirmDialog({
      title: 'AI 生成候选数据',
      message: 'AI 结果仅供参考，需人工确认后才会生效。是否继续？'
    })
  } catch (error) {
    return
  }
  generating.value = true
  try {
    const response = await generateAttendanceHolidayCalendarAiApi(selectedYear.value)
    records.value = ensureSuccess(response) || []
    showToast('AI 候选数据已生成，请人工核对')
  } catch (error) {
    showToast(resolveErrorMessage(error))
  } finally {
    generating.value = false
  }
}

function openEditor(item) {
  form.id = item?.id || null
  form.calendarDate = item?.calendarDate || `${selectedYear.value}-01-01`
  form.dayType = item?.dayType || 'HOLIDAY'
  form.name = item?.name || ''
  form.enabled = item?.enabled ?? true
  form.source = item?.source || 'MANUAL'
  form.status = item?.status || (form.enabled ? 'CONFIRMED' : 'DRAFT')
  form.remark = item?.remark || ''
  editorVisible.value = true
}

async function handleSave() {
  if (!form.calendarDate) {
    showToast('请先选择日期')
    return
  }
  if (!form.dayType) {
    showToast('请先选择类型')
    return
  }
  saving.value = true
  try {
    const response = await saveAttendanceHolidayCalendarApi({
      id: form.id,
      calendarDate: form.calendarDate,
      dayType: form.dayType,
      name: form.name,
      enabled: form.enabled,
      source: form.source,
      status: form.status,
      remark: form.remark
    })
    ensureSuccess(response)
    showToast('已保存')
    editorVisible.value = false
    await fetchList()
  } finally {
    saving.value = false
  }
}

async function handleDelete(item) {
  if (!item?.id) {
    return
  }
  try {
    await showConfirmDialog({
      title: '删除确认',
      message: `确定删除 ${item.calendarDate} 的节假日规则吗？`
    })
  } catch (error) {
    return
  }
  const response = await deleteAttendanceHolidayCalendarApi(item.id)
  ensureSuccess(response)
  showToast('已删除')
  await fetchList()
}

async function handleConfirmYear() {
  try {
    await showConfirmDialog({
      title: '确认生效',
      message: `确认 ${selectedYear.value} 年节假日数据生效吗？确认后将作为考勤工作日判断依据。`
    })
  } catch (error) {
    return
  }
  confirming.value = true
  try {
    const response = await confirmAttendanceHolidayCalendarYearApi(selectedYear.value)
    records.value = ensureSuccess(response) || []
    showToast('本年度数据已确认生效')
  } finally {
    confirming.value = false
  }
}

function sourceLabel(value) {
  return value === 'AI' ? 'AI' : '人工'
}

function statusLabel(value) {
  return value === 'CONFIRMED' ? '已确认' : '候选'
}

function ensureSuccess(response) {
  if (!response || response.code !== 0) {
    throw new Error(response?.message || '请求失败')
  }
  return response.data
}

function resolveErrorMessage(error) {
  return error?.response?.data?.message
    || error?.response?.data?.msg
    || error?.response?.data?.error
    || error?.message
    || '请求失败，请稍后重试'
}
</script>

<style scoped>
.holiday-panel {
  padding: 16px;
  border: 1px solid #e2e8f0;
  border-radius: 18px;
  background: #fff;
}

.toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
}

.year-field {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border: 1px solid #dbe3ef;
  border-radius: 12px;
  background: #f8fafc;
  color: #475569;
  font-size: 13px;
}

.year-field input {
  width: 92px;
  border: 0;
  outline: 0;
  background: transparent;
  color: #0f172a;
  font-weight: 700;
}

.empty-tip {
  margin-top: 14px;
  padding: 18px;
  border-radius: 16px;
  background: #f8fafc;
  color: #64748b;
  text-align: center;
}

.holiday-table {
  margin-top: 14px;
  overflow-x: auto;
}

.holiday-table__head,
.holiday-row {
  display: grid;
  grid-template-columns: 120px 80px 120px 140px 80px 70px 80px minmax(160px, 1fr) 110px;
  min-width: 980px;
  align-items: center;
  gap: 10px;
  padding: 12px 10px;
  border-bottom: 1px solid #e2e8f0;
  font-size: 13px;
}

.holiday-table__head {
  color: #64748b;
  font-weight: 700;
  background: #f8fafc;
  border-radius: 12px;
}

.date-cell {
  font-weight: 700;
  color: #0f172a;
}

.type-badge {
  display: inline-flex;
  padding: 4px 9px;
  border-radius: 999px;
  font-style: normal;
  font-weight: 700;
  background: #eff6ff;
  color: #2563eb;
}

.type-badge--HOLIDAY {
  background: #fee2e2;
  color: #dc2626;
}

.type-badge--WEEKDAY_REST {
  background: #fef3c7;
  color: #b45309;
}

.type-badge--MAKEUP_WORKDAY {
  background: #dcfce7;
  color: #15803d;
}

.remark-cell {
  color: #64748b;
}

.row-actions {
  display: flex;
  gap: 8px;
}

.row-actions button {
  border: 0;
  background: transparent;
  color: #2563eb;
  cursor: pointer;
}

.row-actions .danger {
  color: #dc2626;
}

.editor {
  padding: 18px;
  background: #fff;
}

.editor__title {
  margin-bottom: 14px;
  color: #0f172a;
  font-size: 16px;
  font-weight: 800;
}

.editor-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 12px;
  color: #64748b;
  font-size: 13px;
}

.editor-field input,
.editor-field select,
.editor-field textarea {
  width: 100%;
  box-sizing: border-box;
  padding: 10px 12px;
  border: 1px solid #dbe3ef;
  border-radius: 12px;
  background: #fff;
  color: #0f172a;
}

.editor-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

@media (max-width: 768px) {
  .holiday-panel {
    padding: 12px;
  }

  .toolbar {
    align-items: stretch;
  }

  .toolbar :deep(.van-button),
  .year-field {
    flex: 1 1 100%;
  }
}
</style>
