<template>
  <div class="app-layout">
    <aside class="layout-sidebar">
      <div class="layout-brand">讲师团工作管理系统</div>
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
import { APP_MENU_ITEMS, buildAccessContext, filterMenuItems, findMenuItemByPath } from '@/constants/modules'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const accessContext = computed(() => buildAccessContext(userStore.userInfo))
const isAdmin = computed(() => accessContext.value.isAdmin)

const menuItems = computed(() => filterMenuItems(APP_MENU_ITEMS, accessContext.value))
const currentPageTitle = computed(() => findMenuItemByPath(route.path)?.title || '后台管理')
const welcomeText = computed(() => {
  const name = userStore.userInfo?.realName || userStore.userInfo?.username || '用户'
  return `${name}${isAdmin.value ? '（ADMIN）' : '（USER）'}`
})

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
