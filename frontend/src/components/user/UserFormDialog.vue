<template>
  <van-popup :show="modelValue" position="bottom" round :close-on-click-overlay="!saving" @update:show="handleVisibleChange">
    <div class="dialog-body">
      <div class="dialog-title">
        {{ mode === 'create' ? '新增用户' : '编辑用户' }}
        <span v-if="mode === 'edit' && props.formData?.realName" class="dialog-title-sub">· {{ props.formData.realName }}</span>
      </div>
      <div class="dialog-tip">
        用户管理页仅维护基础资料；`parentUserId / levelNo / treePath` 由组织架构模块维护。
      </div>
      <div v-if="mode === 'edit' && formSummary" class="dialog-summary">
        当前编辑：{{ formSummary }}
      </div>
      <van-form @submit="handleSubmit">
        <van-field
          v-if="mode === 'create'"
          v-model="localForm.unitId"
          label="单位ID"
          type="digit"
          clearable
          :disabled="saving"
          @blur="normalizeField('unitId')"
          placeholder="请输入所属单位ID"
          required
        />
        <div v-if="mode === 'create'" class="field-hint">
          请填写该用户所属单位的主键 ID；如需核对，可先在列表或详情中复制现有单位 ID。
        </div>
        <van-field
          v-if="mode === 'create'"
          v-model="localForm.username"
          label="账号"
          clearable
          :disabled="saving"
          @blur="normalizeField('username')"
          maxlength="30"
          show-word-limit
          placeholder="请输入登录账号"
          required
        />
        <div v-if="mode === 'create'" class="field-hint">
          建议使用工号、姓名拼音或便于识别的短账号；保存时会自动去掉首尾空格。
        </div>
        <van-field
          v-if="mode === 'create'"
          v-model="localForm.password"
          label="密码"
          :type="showPassword ? 'text' : 'password'"
          clearable
          :disabled="saving"
          @blur="normalizeField('password')"
          maxlength="30"
          show-word-limit
          placeholder="请输入初始密码"
          required
        >
          <template #button>
            <van-button size="mini" plain type="primary" :disabled="saving" @click.prevent="togglePasswordVisible">
              {{ showPassword ? '隐藏' : '显示' }}
            </van-button>
          </template>
        </van-field>
        <div v-if="mode === 'create'" class="password-hint">
          建议录入一个便于首次通知的初始密码；如需恢复，可在用户列表中直接执行“重置密码”。
        </div>
        <van-field
          v-model="localForm.realName"
          label="姓名"
          clearable
          :disabled="saving"
          @blur="normalizeField('realName')"
          maxlength="20"
          show-word-limit
          placeholder="请输入姓名"
          required
        />
        <div class="field-hint">
          姓名会直接用于列表卡片、详情标题和确认弹框展示，建议填写常用称呼。
        </div>
        <van-field
          v-model="localForm.jobTitle"
          label="岗位名称"
          clearable
          :disabled="saving"
          @blur="normalizeField('jobTitle')"
          maxlength="30"
          show-word-limit
          placeholder="请输入岗位名称"
        />
        <div class="field-hint">
          岗位名称可留空；如填写，会直接显示在列表和详情中，便于区分同名用户。
        </div>
        <van-field
          v-model="localForm.mobile"
          label="手机号"
          clearable
          :disabled="saving"
          @blur="normalizeField('mobile')"
          maxlength="11"
          show-word-limit
          placeholder="请输入手机号"
        />
        <div class="field-hint">
          手机号可后补；如填写，需为 11 位手机号，便于后续联系与验收核对。
        </div>
        <template v-if="mode === 'edit'">
          <van-field :model-value="props.formData?.unitId != null ? String(props.formData.unitId) : '-'" label="单位ID" readonly />
          <van-field :model-value="props.formData?.username || '-'" label="账号" readonly />
          <van-field :model-value="readonlyOrgInfo.parentUserId" label="上级ID" readonly />
          <van-field :model-value="readonlyOrgInfo.levelNo" label="层级" readonly />
          <van-field :model-value="readonlyOrgInfo.treePath" label="树路径" readonly />
          <div class="field-hint">
            当前仅展示组织树只读信息；如需调整上级、层级或树路径，请前往组织架构模块处理。
          </div>
        </template>
        <van-field
          v-model="localForm.status"
          is-link
          readonly
          :disabled="saving"
          label="状态"
          placeholder="请选择状态"
          @click="openStatusPicker"
        />
        <div class="status-hint">
          当前状态：{{ localForm.status }}。{{ mode === 'create' ? '新建用户默认建议保持启用，后续如需停用可再编辑调整。' : '' }}停用后用户将不再作为启用用户参与日常维护入口展示。<template v-if="mode === 'edit' && isCurrentLoginUser">当前登录用户不能在此停用自己。</template>
        </div>

        <div class="dialog-actions">
          <van-button plain block :disabled="saving" @click="handleCancel">取消</van-button>
          <van-button type="primary" block native-type="submit" :loading="saving" :disabled="!canSubmit">
            保存
          </van-button>
        </div>
        <van-button v-if="mode === 'create'" plain block class="reset-button" :disabled="saving || !isDirty" @click="clearCreateForm">
          清空输入
        </van-button>
        <div v-if="mode === 'create'" class="reset-hint">清空后将恢复到创建表单的初始状态。</div>
        <van-button v-if="mode === 'edit'" plain block class="reset-button" :disabled="saving || !isDirty" @click="resetToOriginal">
          恢复原值
        </van-button>
        <div v-if="mode === 'edit'" class="reset-hint">恢复原值会回退到当前打开弹层时的原始数据。</div>
        <div v-if="submitHint" class="submit-hint">{{ submitHint }}</div>
      </van-form>
    </div>

    <van-popup :show="showStatusPicker" position="bottom" round :close-on-click-overlay="!saving" @update:show="showStatusPicker = $event">
      <van-picker
        :columns="statusColumns"
        @confirm="handleConfirmStatus"
        @cancel="showStatusPicker = false"
      />
    </van-popup>
  </van-popup>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { showConfirmDialog, showToast } from 'vant'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  mode: {
    type: String,
    default: 'create'
  },
  formData: {
    type: Object,
    default: () => ({})
  },
  saving: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:modelValue', 'submit'])

const localForm = reactive({
  unitId: '',
  username: '',
  password: '',
  realName: '',
  jobTitle: '',
  mobile: '',
  status: '启用'
})

const showStatusPicker = ref(false)
const showPassword = ref(false)
const statusColumns = [
  { text: '启用', value: 1 },
  { text: '停用', value: 0 }
]
const mobilePattern = /^1\d{10}$/
const positiveIntegerPattern = /^[1-9]\d*$/

const normalizedStatus = computed(() => (localForm.status === '停用' ? 0 : 1))
const isCurrentLoginUser = computed(() => Boolean(props.formData?.isCurrentLoginUser))
const normalizedForm = computed(() => ({
  unitId: localForm.unitId.trim(),
  username: localForm.username.trim(),
  password: localForm.password.trim(),
  realName: localForm.realName.trim(),
  jobTitle: localForm.jobTitle.trim(),
  mobile: localForm.mobile.trim(),
  status: normalizedStatus.value
}))
const initialForm = computed(() => ({
  unitId: props.formData?.unitId != null ? String(props.formData.unitId).trim() : '',
  username: props.formData?.username?.trim?.() || '',
  password: '',
  realName: props.formData?.realName?.trim?.() || '',
  jobTitle: props.formData?.jobTitle?.trim?.() || '',
  mobile: props.formData?.mobile?.trim?.() || '',
  status: Number(props.formData?.status) === 0 ? 0 : 1
}))
const isDirty = computed(() => JSON.stringify(normalizedForm.value) !== JSON.stringify(initialForm.value))
const readonlyOrgInfo = computed(() => ({
  parentUserId: props.formData?.parentUserId ?? '未接入',
  levelNo: props.formData?.levelNo ?? '未接入',
  treePath: props.formData?.treePath || '未接入'
}))
const canSubmit = computed(() => {
  if (!localForm.realName.trim()) {
    return false
  }
  if (props.mode === 'create') {
    if (!localForm.unitId.trim() || !localForm.username.trim() || !localForm.password.trim()) {
      return false
    }
    if (!positiveIntegerPattern.test(localForm.unitId.trim())) {
      return false
    }
  }
  if (localForm.mobile.trim() && !mobilePattern.test(localForm.mobile.trim())) {
    return false
  }
  if (props.mode === 'edit' && isCurrentLoginUser.value && normalizedStatus.value === 0) {
    return false
  }
  if (props.mode === 'edit' && !isDirty.value) {
    return false
  }
  return true
})
const submitHint = computed(() => {
  if (!localForm.realName.trim()) {
    return '请先填写姓名'
  }
  if (props.mode === 'create') {
    if (!localForm.unitId.trim()) {
      return '请先填写单位ID'
    }
    if (!positiveIntegerPattern.test(localForm.unitId.trim())) {
      return '单位ID需为正整数'
    }
    if (!localForm.username.trim()) {
      return '请先填写登录账号'
    }
    if (!localForm.password.trim()) {
      return '请先填写初始密码'
    }
  }
  if (localForm.mobile.trim() && !mobilePattern.test(localForm.mobile.trim())) {
    return '手机号格式不正确，需为 11 位手机号'
  }
  if (props.mode === 'edit' && isCurrentLoginUser.value && normalizedStatus.value === 0) {
    return '当前登录用户不能在用户管理页停用自己'
  }
  if (props.mode === 'edit' && !isDirty.value) {
    return '尚未修改内容'
  }
  return ''
})
const formSummary = computed(() => {
  if (props.mode !== 'edit') {
    return ''
  }
  const parts = []
  if (props.formData?.realName) {
    parts.push(props.formData.realName)
  }
  if (props.formData?.username) {
    parts.push(`账号 ${props.formData.username}`)
  }
  if (props.formData?.levelNo != null) {
    parts.push(`层级 ${props.formData.levelNo}`)
  }
  if (props.formData?.status != null) {
    parts.push(`状态 ${Number(props.formData.status) === 1 ? '启用' : '停用'}`)
  }
  return parts.join('，')
})

watch(
  () => props.formData,
  (value) => {
    localForm.unitId = value?.unitId != null ? String(value.unitId) : ''
    localForm.username = value?.username || ''
    localForm.password = ''
    showPassword.value = false
    localForm.realName = value?.realName || ''
    localForm.jobTitle = value?.jobTitle || ''
    localForm.mobile = value?.mobile || ''
    localForm.status = Number(value?.status) === 0 ? '停用' : '启用'
  },
  { immediate: true, deep: true }
)

watch(
  () => props.modelValue,
  (value) => {
    if (value) {
      return
    }
    showStatusPicker.value = false
    showPassword.value = false
  }
)

watch(
  () => props.saving,
  (value) => {
    if (value) {
      showStatusPicker.value = false
    }
  }
)

async function handleVisibleChange(value) {
  if (value) {
    emit('update:modelValue', true)
    return
  }
  await requestClose()
}

async function handleCancel() {
  await requestClose()
}

function togglePasswordVisible() {
  if (props.saving) {
    return
  }
  showPassword.value = !showPassword.value
}

function normalizeField(field) {
  if (props.saving || typeof localForm[field] !== 'string') {
    return
  }
  localForm[field] = localForm[field].trim()
}

function openStatusPicker() {
  if (props.saving) {
    return
  }
  showStatusPicker.value = true
}

function handleConfirmStatus({ selectedOptions }) {
  if (props.saving) {
    showStatusPicker.value = false
    return
  }
  const nextText = selectedOptions[0]?.text || '启用'
  if (props.mode === 'edit' && isCurrentLoginUser.value && nextText === '停用') {
    showToast('当前登录用户不能停用自己')
    showStatusPicker.value = false
    return
  }
  localForm.status = nextText
  showStatusPicker.value = false
}

function resetToOriginal() {
  if (props.saving || !isDirty.value) {
    return
  }
  const value = props.formData || {}
  localForm.unitId = value?.unitId != null ? String(value.unitId) : ''
  localForm.username = value?.username || ''
  localForm.password = ''
  showPassword.value = false
  localForm.realName = value?.realName || ''
  localForm.jobTitle = value?.jobTitle || ''
  localForm.mobile = value?.mobile || ''
  localForm.status = Number(value?.status) === 0 ? '停用' : '启用'
}

function clearCreateForm() {
  if (props.saving || !isDirty.value) {
    return
  }
  localForm.unitId = ''
  localForm.username = ''
  localForm.password = ''
  showPassword.value = false
  localForm.realName = ''
  localForm.jobTitle = ''
  localForm.mobile = ''
  localForm.status = '启用'
}

async function requestClose() {
  if (props.saving) {
    return
  }
  if (isDirty.value) {
    try {
      await showConfirmDialog({
        title: '放弃修改确认',
        message: '当前表单还有未保存内容，确认直接关闭吗？'
      })
    } catch (error) {
      return
    }
  }
  emit('update:modelValue', false)
}

function handleSubmit() {
  if (props.saving) {
    return
  }
  if (!localForm.realName.trim()) {
    showToast('请输入姓名')
    return
  }
  if (props.mode === 'create') {
    if (!localForm.unitId.trim()) {
      showToast('请输入单位ID')
      return
    }
    if (!positiveIntegerPattern.test(localForm.unitId.trim())) {
      showToast('单位ID需为正整数')
      return
    }
    if (!localForm.username.trim()) {
      showToast('请输入登录账号')
      return
    }
    if (!localForm.password.trim()) {
      showToast('请输入初始密码')
      return
    }
  }
  if (localForm.mobile.trim() && !mobilePattern.test(localForm.mobile.trim())) {
    showToast('请输入正确的11位手机号')
    return
  }
  if (props.mode === 'edit' && isCurrentLoginUser.value && normalizedStatus.value === 0) {
    showToast('当前登录用户不能停用自己')
    return
  }

  emit('submit', {
    unitId: localForm.unitId ? Number(localForm.unitId) : undefined,
    username: localForm.username.trim(),
    password: localForm.password.trim(),
    realName: localForm.realName.trim(),
    jobTitle: localForm.jobTitle.trim(),
    mobile: localForm.mobile.trim(),
    status: normalizedStatus.value
  })
}
</script>

<style scoped>
.dialog-body {
  padding: 20px 16px 28px;
}

.dialog-title {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 16px;
}

.dialog-title-sub {
  color: #667085;
  font-size: 14px;
  font-weight: 400;
}

.dialog-tip {
  margin-bottom: 16px;
  padding: 10px 12px;
  border-radius: 10px;
  background: #f5f7fb;
  color: #576b95;
  font-size: 13px;
  line-height: 1.6;
}

.dialog-summary {
  margin-bottom: 16px;
  padding: 10px 12px;
  border-radius: 10px;
  background: #fff7e8;
  color: #8a5a00;
  font-size: 13px;
  line-height: 1.6;
}

.dialog-actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  margin-top: 20px;
}

.reset-button {
  margin-top: 12px;
}

.reset-hint {
  margin-top: 8px;
  font-size: 12px;
  color: #667085;
  line-height: 1.5;
}

.submit-hint {
  margin-top: 10px;
  font-size: 12px;
  color: #8a5a00;
  line-height: 1.5;
}

.status-hint {
  margin-top: 10px;
  font-size: 12px;
  color: #576b95;
  line-height: 1.5;
}

.password-hint {
  margin-top: 10px;
  font-size: 12px;
  color: #576b95;
  line-height: 1.5;
}

.field-hint {
  margin-top: 10px;
  font-size: 12px;
  color: #667085;
  line-height: 1.5;
}
</style>
