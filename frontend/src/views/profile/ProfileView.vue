<template>
  <AppPageShell title="个人中心" description="查看当前账号、AI 授权和账号安全信息。">
    <template #actions>
      <div class="action-row">
        <van-button type="primary" @click="copyUsername">复制账号</van-button>
        <van-button plain type="success" @click="copyUserId">复制用户 ID</van-button>
        <van-button plain type="warning" @click="openPasswordPopup">修改密码</van-button>
      </div>
    </template>

    <PageSkeletonSection title="当前账号" description="快速确认当前登录身份和基础信息。">
      当前登录账号：{{ profile.username }}，角色：{{ profile.roleLabel }}。
    </PageSkeletonSection>

    <PageSkeletonSection title="资料概览" description="展示当前登录用户的常用资料。">
      <div class="profile-grid">
        <div class="profile-card">
          <div class="profile-label">姓名</div>
          <div class="profile-value">{{ profile.realName }}</div>
        </div>
        <div class="profile-card">
          <div class="profile-label">账号</div>
          <div class="profile-value">{{ profile.username }}</div>
        </div>
        <div class="profile-card">
          <div class="profile-label">用户 ID</div>
          <div class="profile-value">{{ profile.userId }}</div>
        </div>
        <div class="profile-card">
          <div class="profile-label">当前角色</div>
          <div class="profile-value">{{ profile.roleLabel }}</div>
        </div>
      </div>
    </PageSkeletonSection>

    <PageSkeletonSection title="账号安全" description="支持当前登录用户修改自己的登录密码。">
      <div class="status-list">
        <div class="status-item">
          <span class="status-name">密码管理</span>
          <span class="status-value">可修改当前账号密码</span>
        </div>
        <div class="status-item">
          <span class="status-name">安全建议</span>
          <span class="status-value">修改成功后将自动退出并重新登录</span>
        </div>
      </div>
      <div v-if="mustChangePassword" class="force-password-tip">
        首次登录必须先修改密码，修改完成后才能继续进入系统。
      </div>
      <div class="security-actions">
        <van-button type="warning" block @click="openPasswordPopup">打开修改密码</van-button>
      </div>
    </PageSkeletonSection>

    <PageSkeletonSection title="AI 授权概览" description="展示当前用户拥有的 AI、知识库和技能授权数量。">
      <div class="profile-grid" data-guide="profile-ai-summary">
        <div class="profile-card">
          <div class="profile-label">AI 接入授权</div>
          <div class="profile-value">{{ permissionSummary.aiCount }}</div>
        </div>
        <div class="profile-card">
          <div class="profile-label">知识库授权</div>
          <div class="profile-value">{{ permissionSummary.knowledgeCount }}</div>
        </div>
        <div class="profile-card">
          <div class="profile-label">技能授权</div>
          <div class="profile-value">{{ permissionSummary.skillCount }}</div>
        </div>
        <div class="profile-card">
          <div class="profile-label">专家身份</div>
          <div class="profile-value">{{ state.expertList.length }}</div>
        </div>
      </div>
    </PageSkeletonSection>

    <PageSkeletonSection title="专家身份" description="展示当前账号已绑定的专家技能。">
      <van-loading v-if="state.loadingExperts" class="state-block" size="24px" vertical>加载中...</van-loading>
      <van-empty v-else-if="!state.expertList.length" description="当前还没有专家身份" />
      <div v-else class="expert-grid" data-guide="profile-expert-summary">
        <div v-for="item in state.expertList" :key="item.id" class="expert-card">
          <div class="expert-title">{{ item.skillName }}</div>
          <div class="meta-line">专家等级：{{ item.expertLevel || '-' }}</div>
          <div class="meta-line">版本：{{ item.versionNo || '-' }}</div>
          <div class="meta-line">状态：{{ Number(item.status) === 1 ? '启用' : '停用' }}</div>
        </div>
      </div>
    </PageSkeletonSection>

    <PageSkeletonSection title="当前状态" description="帮助你快速确认当前账号的可用范围。">
      <div class="status-list" data-guide="profile-ai-status">
        <div class="status-item">
          <span class="status-name">登录状态</span>
          <span class="status-value">{{ loginStateText }}</span>
        </div>
        <div class="status-item">
          <span class="status-name">菜单范围</span>
          <span class="status-value">{{ menuScopeText }}</span>
        </div>
        <div class="status-item">
          <span class="status-name">AI 工作台</span>
          <span class="status-value">{{ workbenchText }}</span>
        </div>
      </div>
    </PageSkeletonSection>
  </AppPageShell>

  <van-popup
    :show="state.passwordPopupVisible"
    position="bottom"
    round
    :closeable="!state.submittingPassword && !mustChangePassword"
    :close-on-click-overlay="!state.submittingPassword && !mustChangePassword"
    class="password-popup"
    @update:show="handlePopupVisibleChange"
  >
    <div class="password-popup__body">
      <div class="password-popup__title">修改密码</div>
      <div class="password-popup__desc">修改成功后会自动退出，请使用新密码重新登录。</div>

      <van-field
        v-model="state.passwordForm.oldPassword"
        type="password"
        label="原密码"
        placeholder="请输入原密码"
        autocomplete="current-password"
      />
      <van-field
        v-model="state.passwordForm.newPassword"
        type="password"
        label="新密码"
        placeholder="请输入新密码"
        autocomplete="new-password"
      />
      <van-field
        v-model="state.passwordForm.confirmPassword"
        type="password"
        label="确认新密码"
        placeholder="请再次输入新密码"
        autocomplete="new-password"
      />

      <div class="password-popup__actions">
        <van-button block plain :disabled="state.submittingPassword" @click="closePasswordPopup">取消</van-button>
        <van-button
          block
          type="primary"
          :loading="state.submittingPassword"
          :disabled="state.submittingPassword"
          @click="submitPasswordChange"
        >
          确认修改
        </van-button>
      </div>
    </div>
  </van-popup>
</template>

<script setup>
import { computed, onMounted, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast } from 'vant'
import AppPageShell from '@/components/layout/AppPageShell.vue'
import PageSkeletonSection from '@/components/layout/PageSkeletonSection.vue'
import { queryCurrentAiPermission } from '@/api/ai'
import { changePasswordApi } from '@/api/auth'
import { queryExpertList } from '@/api/expert'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const mustChangePassword = computed(() => Boolean(userStore.forcePasswordChange || route.query.forcePasswordChange === '1'))
const state = reactive({
  permissions: null,
  expertList: [],
  loadingExperts: false,
  passwordPopupVisible: false,
  submittingPassword: false,
  passwordForm: {
    oldPassword: '',
    newPassword: '',
    confirmPassword: ''
  }
})

const profile = computed(() => {
  const userInfo = userStore.userInfo || {}
  const roleCode = userInfo.role || (userInfo.superAdmin ? 'ADMIN' : 'USER')
  return {
    userId: userInfo.userId || '-',
    username: userInfo.username || '-',
    realName: userInfo.realName || userInfo.username || '未命名用户',
    roleLabel: roleCode === 'ADMIN' ? '管理员' : '普通用户'
  }
})

const permissionSummary = computed(() => ({
  aiCount: (state.permissions?.aiPermissions || []).length,
  knowledgeCount: (state.permissions?.knowledgePermissions || []).length,
  skillCount: (state.permissions?.skillPermissions || []).length
}))

const loginStateText = computed(() => (userStore.token ? '已登录' : '未登录'))
const menuScopeText = computed(() => (profile.value.roleLabel === '管理员' ? '全部后台模块' : '个人可用模块'))
const workbenchText = computed(() => {
  const canUseAgent = Boolean(state.permissions?.admin || (state.permissions?.aiPermissions || []).some((item) => item.canUseAgent))
  return canUseAgent ? '可使用' : '未授权'
})

function ensureSuccess(response, fallback = '请求失败') {
  if (!response || response.code !== 0) {
    throw new Error(response?.message || fallback)
  }
  return response.data
}

async function fetchPermissions() {
  state.permissions = ensureSuccess(await queryCurrentAiPermission(), '权限信息加载失败')
}

async function fetchExperts() {
  state.loadingExperts = true
  try {
    const data = ensureSuccess(await queryExpertList({ status: 1 }), '专家身份加载失败')
    state.expertList = Array.isArray(data) ? data : []
  } catch (error) {
    showToast(error.message || '专家身份加载失败')
  } finally {
    state.loadingExperts = false
  }
}

async function copyText(value, successText) {
  try {
    await navigator.clipboard.writeText(String(value))
    showToast(successText)
  } catch (error) {
    showToast('复制失败，请稍后重试')
  }
}

function copyUsername() {
  copyText(profile.value.username, '账号已复制')
}

function copyUserId() {
  copyText(profile.value.userId, '用户 ID 已复制')
}

function resetPasswordForm() {
  state.passwordForm.oldPassword = ''
  state.passwordForm.newPassword = ''
  state.passwordForm.confirmPassword = ''
}

function openPasswordPopup() {
  state.passwordPopupVisible = true
}

function closePasswordPopup() {
  if (state.submittingPassword || mustChangePassword.value) {
    return
  }
  state.passwordPopupVisible = false
  resetPasswordForm()
}

function handlePopupVisibleChange(visible) {
  if (state.submittingPassword || mustChangePassword.value) {
    return
  }
  state.passwordPopupVisible = visible
  if (!visible) {
    resetPasswordForm()
  }
}

function validatePasswordForm() {
  const oldPassword = state.passwordForm.oldPassword.trim()
  const newPassword = state.passwordForm.newPassword.trim()
  const confirmPassword = state.passwordForm.confirmPassword.trim()

  if (!oldPassword) {
    showToast('原密码不能为空')
    return false
  }
  if (!newPassword) {
    showToast('新密码不能为空')
    return false
  }
  if (!confirmPassword) {
    showToast('确认新密码不能为空')
    return false
  }
  if (newPassword.length < 8) {
    showToast('新密码长度至少 8 位')
    return false
  }
  if (newPassword === oldPassword) {
    showToast('新密码不能与原密码相同')
    return false
  }
  if (newPassword !== confirmPassword) {
    showToast('新密码与确认新密码必须一致')
    return false
  }
  return true
}

async function submitPasswordChange() {
  if (!validatePasswordForm() || state.submittingPassword) {
    return
  }
  const payload = {
    oldPassword: state.passwordForm.oldPassword.trim(),
    newPassword: state.passwordForm.newPassword.trim(),
    confirmPassword: state.passwordForm.confirmPassword.trim()
  }
  state.submittingPassword = true
  try {
    ensureSuccess(await changePasswordApi(payload), '密码修改失败')
    showToast('密码修改成功')
    closePasswordPopup()
    userStore.clearLogin()
    await router.replace('/login')
  } catch (error) {
    if (error?.response) {
      return
    }
    showToast(error.message || '密码修改失败')
  } finally {
    state.submittingPassword = false
  }
}

onMounted(async () => {
  if (mustChangePassword.value) {
    openPasswordPopup()
    return
  }
  try {
    await Promise.all([fetchPermissions(), fetchExperts()])
  } catch (error) {
    showToast(error.message || '个人中心加载失败')
  }
})
</script>

<style scoped>
.action-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.profile-grid,
.expert-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.profile-card,
.expert-card {
  border-radius: 12px;
  border: 1px solid #e5e7eb;
  padding: 16px;
  background: #f9fafb;
}

.profile-label,
.meta-line {
  color: #6b7280;
  font-size: 13px;
}

.profile-value,
.expert-title {
  margin-top: 10px;
  color: #111827;
  font-size: 18px;
  font-weight: 600;
  line-height: 1.5;
}

.status-list {
  display: grid;
  gap: 12px;
}

.status-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border-radius: 12px;
  border: 1px solid #e5e7eb;
  padding: 14px 16px;
  background: #fff;
}

.status-name {
  color: #6b7280;
  font-size: 13px;
}

.status-value {
  color: #111827;
  font-size: 14px;
  font-weight: 600;
  text-align: right;
}

.state-block {
  padding: 20px 0;
}

.security-actions {
  margin-top: 16px;
}

.force-password-tip {
  margin-top: 16px;
  border-radius: 12px;
  padding: 12px 14px;
  background: #fff7ed;
  border: 1px solid #fdba74;
  color: #9a3412;
  line-height: 1.6;
}

.password-popup {
  max-width: 720px;
  margin: 0 auto;
}

.password-popup__body {
  padding: 24px 16px 20px;
}

.password-popup__title {
  color: #111827;
  font-size: 18px;
  font-weight: 700;
}

.password-popup__desc {
  margin: 8px 0 16px;
  color: #6b7280;
  font-size: 13px;
  line-height: 1.6;
}

.password-popup__actions {
  display: grid;
  gap: 12px;
  margin-top: 18px;
}

@media (max-width: 900px) {
  .profile-grid,
  .expert-grid {
    grid-template-columns: 1fr;
  }

  .status-item {
    align-items: flex-start;
    flex-direction: column;
  }

  .status-value {
    text-align: left;
  }
}
</style>
