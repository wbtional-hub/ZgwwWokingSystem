<template>
  <AppPageShell title="考勤规则" description="为当前单位配置上下班时间，以及迟到、早退的宽限分钟。" help-key="attendance">
    <template #actions>
      <van-button plain type="primary" :disabled="pageBusy" @click="fetchRule">刷新规则</van-button>
    </template>

    <AttendanceWorkspaceTabs />

    <section class="panel">
      <div class="panel-title">当前单位规则</div>
      <div class="panel-subtitle">单位：{{ rule.unitName || '-' }}</div>
      <div class="panel-grid">
        <label class="field">
          <span class="field-label">上班时间</span>
          <input v-model="form.workStartTime" class="field-input" type="time" :disabled="pageBusy">
        </label>
        <label class="field">
          <span class="field-label">下班时间</span>
          <input v-model="form.workEndTime" class="field-input" type="time" :disabled="pageBusy">
        </label>
        <label class="field">
          <span class="field-label">迟到宽限（分钟）</span>
          <input v-model.number="form.lateGraceMinutes" class="field-input" type="number" min="0" :disabled="pageBusy">
        </label>
        <label class="field">
          <span class="field-label">早退宽限（分钟）</span>
          <input v-model.number="form.earlyLeaveGraceMinutes" class="field-input" type="number" min="0" :disabled="pageBusy">
        </label>
        <label class="field">
          <span class="field-label">规则状态</span>
          <select v-model.number="form.status" class="field-input" :disabled="pageBusy">
            <option :value="1">启用</option>
            <option :value="0">停用</option>
          </select>
        </label>
      </div>
      <div class="panel-actions">
        <van-button type="primary" :loading="saving" :disabled="pageBusy" @click="handleSave">保存规则</van-button>
      </div>
      <div class="rule-note">
        说明：统计模块会优先按本单位规则判断迟到和早退。本轮不会强制回算历史数据，只影响后续查看口径。
      </div>
    </section>
  </AppPageShell>
</template>

<script setup>
import { onMounted, reactive, ref, computed } from 'vue'
import { showToast } from 'vant'
import AppPageShell from '@/components/layout/AppPageShell.vue'
import AttendanceWorkspaceTabs from '@/components/attendance/AttendanceWorkspaceTabs.vue'
import { queryCurrentAttendanceRuleApi, saveAttendanceRuleApi } from '@/api/attendance'

const loading = ref(false)
const saving = ref(false)

const rule = reactive({
  unitName: '',
  unitId: null
})

const form = reactive({
  workStartTime: '09:00',
  workEndTime: '18:00',
  lateGraceMinutes: 0,
  earlyLeaveGraceMinutes: 0,
  status: 1
})

const pageBusy = computed(() => loading.value || saving.value)

onMounted(() => {
  fetchRule()
})

async function fetchRule() {
  loading.value = true
  try {
    const response = await queryCurrentAttendanceRuleApi()
    const data = ensureSuccess(response) || {}
    rule.unitName = data.unitName || ''
    rule.unitId = data.unitId || null
    form.workStartTime = normalizeTime(data.workStartTime) || '09:00'
    form.workEndTime = normalizeTime(data.workEndTime) || '18:00'
    form.lateGraceMinutes = Number(data.lateGraceMinutes || 0)
    form.earlyLeaveGraceMinutes = Number(data.earlyLeaveGraceMinutes || 0)
    form.status = Number(data.status ?? 1)
  } finally {
    loading.value = false
  }
}

async function handleSave() {
  if (!form.workStartTime || !form.workEndTime) {
    showToast('请先填写上下班时间')
    return
  }
  saving.value = true
  try {
    const response = await saveAttendanceRuleApi({
      workStartTime: ensureTimeSeconds(form.workStartTime),
      workEndTime: ensureTimeSeconds(form.workEndTime),
      lateGraceMinutes: Number(form.lateGraceMinutes || 0),
      earlyLeaveGraceMinutes: Number(form.earlyLeaveGraceMinutes || 0),
      status: Number(form.status ?? 1)
    })
    ensureSuccess(response)
    showToast('考勤规则已保存')
    await fetchRule()
  } finally {
    saving.value = false
  }
}

function normalizeTime(value) {
  if (!value) {
    return ''
  }
  return String(value).slice(0, 5)
}

function ensureTimeSeconds(value) {
  return value.length === 5 ? `${value}:00` : value
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

.panel-subtitle {
  margin-top: 6px;
  font-size: 13px;
  color: #64748b;
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
}

.field-label {
  font-size: 12px;
  font-weight: 600;
  color: #64748b;
}

.field-input {
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

.rule-note {
  margin-top: 14px;
  padding: 12px;
  border-radius: 14px;
  background: #eff6ff;
  color: #1d4ed8;
  font-size: 13px;
  line-height: 1.7;
}
</style>
