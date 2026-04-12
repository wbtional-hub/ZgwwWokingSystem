<template>
  <AppPageShell title="首页" description="登录后可从这里快速进入各业务模块与 AI 工作台。">
    <template #actions>
      <div class="action-row">
        <van-button type="primary" @click="handleRefresh">刷新首页</van-button>
        <van-button plain type="success" @click="goProfile">查看个人信息</van-button>
      </div>
    </template>

    <PageSkeletonSection title="登录摘要" description="先确认当前账号、角色和当前可访问范围。">
      当前登录人：{{ displayName }}，角色：{{ roleLabel }}，当前可见菜单 {{ visibleMenuCount }} 项。
    </PageSkeletonSection>

    <section class="hero-grid">
      <div class="hero-panel">
        <div class="hero-kicker">Workspace Overview</div>
        <h3>统一办公入口</h3>
        <p>围绕日常办公、AI 工作台和移动端咨询体验，组织当前账号的核心入口与工作状态。</p>
        <div class="hero-meta">最近刷新：{{ lastRefreshText }}</div>
      </div>

      <div class="hero-stats">
        <div class="stat-card">
          <span>当前登录人</span>
          <strong>{{ displayName }}</strong>
        </div>
        <div class="stat-card">
          <span>角色</span>
          <strong>{{ roleLabel }}</strong>
        </div>
        <div class="stat-card">
          <span>菜单范围</span>
          <strong>{{ menuScopeText }}</strong>
        </div>
        <div class="stat-card">
          <span>用户 ID</span>
          <strong>{{ userIdText }}</strong>
        </div>
      </div>
    </section>

    <PageSkeletonSection title="当前工作提示" description="基于当前角色，优先推荐下一步入口。">
      {{ focusTip }}
    </PageSkeletonSection>

    <section class="spotlight-section">
      <button type="button" class="spotlight-card" @click="goPath('/policy-consultant')">
        <span class="spotlight-card__eyebrow">Mobile Consultant</span>
        <span class="spotlight-card__title">手机端政策咨询</span>
        <span class="spotlight-card__desc">打开面向客户的移动端智能体页面，直接进入政策问答场景。</span>
      </button>
    </section>

    <PageSkeletonSection title="快捷入口" description="常用工作入口统一放在首页，减少模块切换成本。">
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

    <PageSkeletonSection title="可访问模块" description="列出当前账号当前可访问的功能模块。">
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

const adminModules = [
  '首页',
  '手机端政策咨询',
  '单位管理',
  '参数管理',
  '知识库中心',
  'Skills 中心',
  'AI 工作台',
  '咨询台账',
  '月度报表',
  '专家台账',
  'AI 接入区',
  'AI 权限配置',
  '操作日志',
  '组织架构',
  '签到管理',
  '周报管理',
  '工作评分',
  '统计分析',
  '个人中心',
  '用户管理'
]

const userModules = [
  '首页',
  '手机端政策咨询',
  '签到管理',
  '周报管理',
  '个人中心'
]

const visibleMenuCount = computed(() => (roleCode.value === 'ADMIN' ? adminModules.length : userModules.length))
const menuScopeText = computed(() => (roleCode.value === 'ADMIN' ? '可访问后台与运营模块' : '仅访问个人可用模块'))
const focusTip = computed(() =>
  roleCode.value === 'ADMIN'
    ? '建议优先进入手机端政策咨询页面做客户演示，再继续到知识库、Skills 和权限配置完成运营闭环。'
    : '建议优先进入手机端政策咨询页面体验问答，再进入签到、周报和个人中心完成日常流程。'
)

const shortcutItems = computed(() =>
  roleCode.value === 'ADMIN'
    ? [
        { path: '/policy-consultant', title: '手机端政策咨询', description: '直接打开客户演示使用的移动端政策顾问页面。' },
        { path: '/users', title: '用户管理', description: '继续用户维护、角色检查和账号管理。' },
        { path: '/units', title: '单位管理', description: '维护单位信息与启停状态。' },
        { path: '/org-tree', title: '组织架构', description: '继续节点维护与层级查看。' },
        { path: '/statistics', title: '统计分析', description: '查看组织排名与趋势数据。' }
      ]
    : [
        { path: '/policy-consultant', title: '手机端政策咨询', description: '直接进入面向客户的移动端智能体页面。' },
        { path: '/attendance', title: '签到管理', description: '进入当前账号签到流程。' },
        { path: '/weekly-work', title: '周报管理', description: '补录、暂存或提交周报。' },
        { path: '/profile', title: '个人中心', description: '查看当前账号信息与个人设置。' }
      ]
)

const accessibleModules = computed(() => (roleCode.value === 'ADMIN' ? adminModules : userModules))

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
  gap: 12px;
}

.hero-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(0, 1fr);
  gap: 24px;
  margin-bottom: 24px;
}

.hero-panel,
.stat-card,
.spotlight-card,
.shortcut-card,
.module-tag {
  border-radius: 14px;
}

.hero-panel {
  padding: 24px;
  border: 1px solid #eef2f7;
  background:
    radial-gradient(circle at top right, rgba(59, 130, 246, 0.1), transparent 22%),
    #ffffff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.hero-kicker {
  display: inline-flex;
  align-items: center;
  min-height: 24px;
  padding: 0 10px;
  border-radius: 999px;
  background: #eef2ff;
  color: #3b82f6;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.hero-panel h3 {
  margin: 16px 0 10px;
  color: #111827;
  font-size: 24px;
  font-weight: 600;
}

.hero-panel p,
.hero-meta {
  color: #6b7280;
  font-size: 14px;
  line-height: 1.5;
}

.hero-meta {
  margin-top: 16px;
}

.hero-stats {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.stat-card {
  padding: 20px;
  border: 1px solid #eef2f7;
  background: #ffffff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.stat-card span {
  display: block;
  color: #6b7280;
  font-size: 12px;
}

.stat-card strong {
  display: block;
  margin-top: 10px;
  color: #111827;
  font-size: 16px;
  font-weight: 600;
  line-height: 1.5;
}

.spotlight-section {
  margin-bottom: 24px;
}

.spotlight-card {
  width: 100%;
  display: grid;
  gap: 10px;
  padding: 24px;
  border: 1px solid #dbeafe;
  background: linear-gradient(135deg, #1c2e40, #24415f 62%, #3b82f6);
  color: #ffffff;
  text-align: left;
  cursor: pointer;
  box-shadow: 0 18px 34px rgba(28, 46, 64, 0.16);
}

.spotlight-card__eyebrow {
  font-size: 12px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  opacity: 0.82;
}

.spotlight-card__title {
  font-size: 28px;
  font-weight: 600;
}

.spotlight-card__desc {
  max-width: 36rem;
  font-size: 14px;
  line-height: 1.5;
  opacity: 0.92;
}

.shortcut-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.shortcut-card {
  border: 1px solid #eef2f7;
  background: #ffffff;
  padding: 20px;
  text-align: left;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.shortcut-title {
  display: block;
  color: #111827;
  font-size: 16px;
  font-weight: 600;
}

.shortcut-desc {
  display: block;
  margin-top: 8px;
  color: #6b7280;
  font-size: 14px;
  line-height: 1.5;
}

.module-tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.module-tag {
  display: inline-flex;
  align-items: center;
  min-height: 32px;
  padding: 0 12px;
  background: #eef2ff;
  color: #3b82f6;
  font-size: 12px;
  font-weight: 600;
}

:deep(.van-button) {
  min-height: 40px;
  border-radius: 10px;
  font-weight: 600;
}

:deep(.van-button--primary) {
  border-color: #2563eb;
  background: linear-gradient(135deg, #3b82f6, #2563eb);
  box-shadow: 0 8px 20px rgba(59, 130, 246, 0.18);
}

@media (max-width: 960px) {
  .hero-grid,
  .shortcut-grid {
    grid-template-columns: 1fr;
  }

  .hero-stats {
    grid-template-columns: 1fr;
  }

  .spotlight-card__title {
    font-size: 24px;
  }
}
</style>
