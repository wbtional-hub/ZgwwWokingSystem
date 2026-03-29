<template>
  <AppPageShell title="个人中心" description="查看当前账号、AI 能力授权和专家身份。">
    <template #actions>
      <div class="action-row">
        <van-button type="primary" @click="copyUsername">复制账号</van-button>
        <van-button plain type="success" @click="copyUserId">复制用户 ID</van-button>
      </div>
    </template>

    <PageSkeletonSection title="当前账号" description="快速确认当前登录身份和基础信息。">
      当前登录账号：{{ profile.username }}，角色：{{ profile.roleLabel }}。
    </PageSkeletonSection>

    <PageSkeletonSection title="资料概览" description="展示当前登录用户的最常用资料。">
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
</template>

<script setup>
import { computed, onMounted, reactive } from 'vue'
import { showToast } from 'vant'
import AppPageShell from '@/components/layout/AppPageShell.vue'
import PageSkeletonSection from '@/components/layout/PageSkeletonSection.vue'
import { queryCurrentAiPermission } from '@/api/ai'
import { queryExpertList } from '@/api/expert'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const state = reactive({
  permissions: null,
  expertList: [],
  loadingExperts: false
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

onMounted(async () => {
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
