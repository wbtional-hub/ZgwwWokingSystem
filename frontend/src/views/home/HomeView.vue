<template>
  <AppPageShell title="首页" description="首页已补齐登录摘要，可直接查看当前账号、角色与可见菜单范围。">
    <template #actions>
      <div class="action-row">
        <van-button type="primary" @click="handleRefresh">刷新首页</van-button>
        <van-button plain type="success" @click="goProfile">查看个人信息</van-button>
      </div>
    </template>

    <PageSkeletonSection title="登录摘要" description="先补齐首页最常用的登录态说明，进入系统后可直接确认当前身份和可访问范围。">
      当前登录人：{{ displayName }}，角色：{{ roleLabel }}，当前可见菜单 {{ visibleMenuCount }} 项。
    </PageSkeletonSection>

    <PageSkeletonSection title="当前账号概览" description="进入系统后先确认当前账号、角色、用户ID和默认工作范围。">
      <div class="home-grid">
        <div class="home-card">
          <div class="home-card-label">欢迎语</div>
          <div class="home-card-value">欢迎进入讲师团日志管理系统</div>
        </div>
        <div class="home-card">
          <div class="home-card-label">当前登录人</div>
          <div class="home-card-value">{{ displayName }}</div>
        </div>
        <div class="home-card">
          <div class="home-card-label">角色</div>
          <div class="home-card-value">{{ roleLabel }}</div>
        </div>
        <div class="home-card">
          <div class="home-card-label">用户 ID</div>
          <div class="home-card-value">{{ userIdText }}</div>
        </div>
        <div class="home-card">
          <div class="home-card-label">菜单访问范围</div>
          <div class="home-card-value">{{ menuScopeText }}</div>
        </div>
        <div class="home-card">
          <div class="home-card-label">首页状态</div>
          <div class="home-card-value">{{ lastRefreshText }}</div>
        </div>
      </div>
    </PageSkeletonSection>

    <PageSkeletonSection title="当前工作提示" description="首页先给出当前角色的默认工作面建议，方便进入系统后直接继续主流程。">
      {{ focusTip }}
    </PageSkeletonSection>

    <PageSkeletonSection title="快捷入口" description="首页补齐最常用工作入口，进入系统后可直接跳到当前角色最常访问的页面。">
      <div class="shortcut-grid">
        <button
          v-for="item in shortcutItems"
          :key="item.path"
          type="button"
          class="shortcut-card"
          @click="goPath(item.path)"
        >
          <span class="shortcut-title">{{ item.title }}</span>
          <span class="shortcut-desc">{{ item.description }}</span>
        </button>
      </div>
    </PageSkeletonSection>

    <PageSkeletonSection title="可访问模块" description="首页直接列出当前账号可见模块，避免切换角色后还要逐个点菜单确认范围。">
      <div class="module-tag-list">
        <span v-for="item in accessibleModules" :key="item" class="module-tag">{{ item }}</span>
      </div>
    </PageSkeletonSection>
  </AppPageShell>
</template>

<script setup>
import { computed, ref } from 'vue'
import { showToast } from 'vant'
import { useRouter } from 'vue-router'
import AppPageShell from '@/components/layout/AppPageShell.vue'
import PageSkeletonSection from '@/components/layout/PageSkeletonSection.vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const lastRefreshText = ref(formatDateTime(new Date()))

const displayName = computed(() => userStore.userInfo?.realName || userStore.userInfo?.username || '管理员')
const roleCode = computed(() => userStore.userInfo?.role || (userStore.userInfo?.superAdmin ? 'ADMIN' : 'USER'))
const roleLabel = computed(() => (roleCode.value === 'ADMIN' ? '管理员' : '普通用户'))
const userIdText = computed(() => userStore.userInfo?.userId || '-')
const visibleMenuCount = computed(() => (roleCode.value === 'ADMIN' ? 10 : 6))
const menuScopeText = computed(() => (roleCode.value === 'ADMIN' ? '可访问全部后台模块' : '仅访问个人可用模块'))
const focusTip = computed(() =>
  roleCode.value === 'ADMIN'
    ? '建议优先从用户管理、单位管理或组织架构进入当前验收主线。'
    : '建议优先进入签到、周报、评分结果和个人中心，完成个人侧日常闭环。'
)
const shortcutItems = computed(() =>
  roleCode.value === 'ADMIN'
    ? [
        { path: '/users', title: '用户管理', description: '继续用户维护与列表验收' },
        { path: '/units', title: '单位管理', description: '维护单位信息与启停状态' },
        { path: '/org-tree', title: '组织架构', description: '继续节点维护与层级查看' },
        { path: '/statistics', title: '统计分析', description: '查看组织排名与趋势' }
      ]
    : [
        { path: '/attendance', title: '签到管理', description: '进入当前账号签到流程' },
        { path: '/weekly-work', title: '周报管理', description: '补录、暂存或提交周报' },
        { path: '/scores', title: '工作评分', description: '查看个人评分结果' },
        { path: '/profile', title: '个人中心', description: '查看当前账号信息' }
      ]
)
const accessibleModules = computed(() =>
  roleCode.value === 'ADMIN'
    ? ['首页', '单位管理', '参数管理', '操作日志', '组织架构', '签到管理', '周报管理', '工作评分', '统计分析', '个人中心', '用户管理']
    : ['首页', '签到管理', '周报管理', '工作评分', '统计分析', '个人中心']
)

function formatDateTime(value) {
  const date = value instanceof Date ? value : new Date(value)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hour = String(date.getHours()).padStart(2, '0')
  const minute = String(date.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day} ${hour}:${minute}`
}

function handleRefresh() {
  lastRefreshText.value = formatDateTime(new Date())
  showToast('首页信息已刷新')
}

function goProfile() {
  router.push('/profile')
}

function goPath(path) {
  router.push(path)
}
</script>

<style scoped>
.action-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.home-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.home-card {
  border-radius: 12px;
  border: 1px solid #e5e7eb;
  padding: 16px;
  background: #f9fafb;
}

.home-card-label {
  color: #6b7280;
  font-size: 13px;
}

.home-card-value {
  margin-top: 10px;
  color: #111827;
  font-size: 18px;
  font-weight: 600;
  line-height: 1.5;
}

.shortcut-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.shortcut-card {
  border: 1px solid #dbeafe;
  border-radius: 12px;
  background: #eff6ff;
  padding: 16px;
  text-align: left;
  cursor: pointer;
}

.shortcut-title {
  display: block;
  color: #1d4ed8;
  font-size: 15px;
  font-weight: 600;
}

.shortcut-desc {
  display: block;
  margin-top: 8px;
  color: #475569;
  font-size: 13px;
  line-height: 1.6;
}

.module-tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.module-tag {
  display: inline-flex;
  align-items: center;
  min-height: 34px;
  padding: 0 12px;
  border-radius: 999px;
  background: #f3f4f6;
  color: #374151;
  font-size: 13px;
}

@media (max-width: 900px) {
  .home-grid {
    grid-template-columns: 1fr;
  }

  .shortcut-grid {
    grid-template-columns: 1fr;
  }
}
</style>
