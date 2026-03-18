<template>
  <div class="app-layout">
    <aside class="layout-sidebar">
      <div class="layout-brand">讲师团日志管理系统</div>
      <nav class="layout-menu">
        <router-link
          v-for="item in menuItems"
          :key="item.path"
          :to="item.path"
          class="layout-menu-item"
          :class="{ 'layout-menu-item--active': route.path === item.path }"
        >
          <span class="layout-menu-title">{{ item.title }}</span>
          <span class="layout-menu-desc">{{ item.description }}</span>
        </router-link>
      </nav>
    </aside>

    <div class="layout-main">
      <header class="layout-header">
        <div class="layout-header-title">{{ currentPageTitle }}</div>
        <div class="layout-header-actions">
          <span class="layout-user-text">{{ welcomeText }}</span>
          <button type="button" class="layout-logout" @click="handleLogout">退出登录</button>
        </div>
      </header>
      <main class="layout-content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { showConfirmDialog, showToast } from 'vant'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const allMenuItems = [
  { path: '/home', title: '首页', description: '系统欢迎页' },
  { path: '/units', title: '单位管理', description: '单位模板页入口', adminOnly: true },
  { path: '/params', title: '参数管理', description: '参数模板页入口', adminOnly: true },
  { path: '/operation-logs', title: '操作日志', description: '日志模板页入口', adminOnly: true },
  { path: '/org-tree', title: '组织架构', description: '组织树维护', adminOnly: true },
  { path: '/attendance', title: '签到管理', description: '出勤签到' },
  { path: '/weekly-work', title: '周报管理', description: '周工作填报' },
  { path: '/scores', title: '工作评分', description: '每周评分结果' },
  { path: '/statistics', title: '统计分析', description: '统计概览' },
  { path: '/profile', title: '个人中心', description: '个人资料与账号信息' },
  { path: '/users', title: '用户管理', description: '模板模块入口', adminOnly: true }
]

const isAdmin = computed(() => {
  const role = userStore.userInfo?.role || (userStore.userInfo?.superAdmin ? 'ADMIN' : 'USER')
  return role === 'ADMIN'
})

const menuItems = computed(() => allMenuItems.filter((item) => !item.adminOnly || isAdmin.value))

const currentPageTitle = computed(() => allMenuItems.find((item) => item.path === route.path)?.title || '后台管理')
const welcomeText = computed(() => `你好，${userStore.userInfo?.realName || userStore.userInfo?.username || '管理员'}${isAdmin.value ? '（ADMIN）' : '（USER）'}`)

async function handleLogout() {
  try {
    await showConfirmDialog({
      title: '退出确认',
      message: '确认退出当前登录状态吗？'
    })
    userStore.clearLogin()
    showToast('已退出登录')
    router.push('/login')
  } catch (error) {
    return
  }
}
</script>

<style scoped>
.app-layout {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr);
  background: #f5f7fa;
}

.layout-sidebar {
  background: #001529;
  color: #fff;
  padding: 20px 16px;
}

.layout-brand {
  font-size: 18px;
  font-weight: 600;
  line-height: 1.5;
}

.layout-menu {
  margin-top: 20px;
  display: grid;
  gap: 8px;
}

.layout-menu-item {
  display: block;
  padding: 12px;
  border-radius: 10px;
  text-decoration: none;
  background: rgba(255, 255, 255, 0.04);
}

.layout-menu-item--active {
  background: #1677ff;
}

.layout-menu-title {
  display: block;
  color: #fff;
  font-size: 14px;
  font-weight: 600;
}

.layout-menu-desc {
  display: block;
  margin-top: 4px;
  color: rgba(255, 255, 255, 0.75);
  font-size: 12px;
}

.layout-main {
  min-width: 0;
  display: grid;
  grid-template-rows: 64px minmax(0, 1fr);
}

.layout-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
}

.layout-header-title {
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
}

.layout-header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.layout-user-text {
  color: #4b5563;
  font-size: 14px;
}

.layout-logout {
  border: none;
  border-radius: 8px;
  padding: 8px 12px;
  background: #1677ff;
  color: #fff;
  cursor: pointer;
}

.layout-content {
  min-width: 0;
  padding: 20px;
}

@media (max-width: 900px) {
  .app-layout {
    grid-template-columns: 1fr;
  }

  .layout-sidebar {
    padding-bottom: 12px;
  }
}
</style>
