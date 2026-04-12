<template>
  <div class="app-layout" :class="{ 'app-layout--mobile': isMobile }">
    <aside v-if="!isMobile" class="layout-sidebar">
      <div class="layout-brand">
        <div class="layout-brand-chip">组工万维></div>
        <div class="layout-brand-title">智慧OA系统</div>
        <div class="layout-brand-subtitle">AI Workbench & Skills Center</div>
      </div>

      <nav class="layout-menu">
        <router-link
          v-for="item in menuItems"
          :key="item.path"
          :to="item.path"
          class="layout-menu-item"
          :class="{ 'layout-menu-item--active': route.path === item.path }"
        >
          <component :is="resolveMenuIcon(item.path)" class="layout-menu-icon" />
          <div class="layout-menu-copy">
          <span class="layout-menu-title">{{ item.title }}</span>
          <span class="layout-menu-desc">{{ item.description }}</span>
          </div>
        </router-link>
      </nav>
    </aside>

    <div class="layout-main">
      <header class="layout-header" :class="{ 'layout-header--mobile': isMobile }">
        <div class="layout-header-primary">
          <div v-if="isMobile" class="layout-header-eyebrow">Mini Program Workspace</div>
          <div v-else class="layout-header-eyebrow">Enterprise Workspace</div>
          <div class="layout-header-title">{{ currentPageTitle }}</div>
        </div>

        <div class="layout-header-actions">
          <button
            v-if="isMobile && route.path !== '/mobile-workspace'"
            type="button"
            class="layout-mobile-home"
            @click="router.push('/mobile-workspace')"
          >
            工作台
          </button>
          <div class="layout-user-badge">
            <UserRound class="header-icon" />
            <span>{{ welcomeText }}</span>
          </div>
          <button type="button" class="layout-logout" @click="handleLogout">退出登录</button>
        </div>
      </header>

      <main class="layout-content" :class="{ 'layout-content--mobile': isMobile }">
        <router-view />
      </main>

      <MobileTabBar v-if="isMobile" :items="mobileNavItems" />
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import {
  BookOpenText,
  Bot,
  Building2,
  ChartColumn,
  ClipboardCheck,
  FileBarChart2,
  FileClock,
  FolderKanban,
  Home,
  KeyRound,
  LayoutGrid,
  Network,
  NotebookPen,
  ScrollText,
  Settings2,
  ShieldCheck,
  Sparkles,
  SquareUserRound,
  UserCog,
  UserRound,
  Users
} from 'lucide-vue-next'
import { showConfirmDialog, showToast } from 'vant'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { APP_MENU_ITEMS, buildAccessContext, filterMenuItems, findMenuItemByPath } from '@/constants/modules'
import { resolveMobileWorkspaceItems } from '@/constants/mobile-workspace'
import MobileTabBar from '@/components/mobile/MobileTabBar.vue'
import { isMobileClient } from '@/utils/device'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const isMobile = isMobileClient()

const accessContext = computed(() => buildAccessContext(userStore.userInfo))
const isAdmin = computed(() => accessContext.value.isAdmin)

const menuItems = computed(() => filterMenuItems(APP_MENU_ITEMS, accessContext.value))
const mobileNavItems = computed(() => resolveMobileWorkspaceItems(accessContext.value))
const currentPageTitle = computed(() => route.meta?.title || findMenuItemByPath(route.path)?.title || '后台管理')
const welcomeText = computed(() => {
  const name = userStore.userInfo?.realName || userStore.userInfo?.username || '用户'
  if (isMobile) {
    return name
  }
  return `${name}${isAdmin.value ? '（ADMIN）' : '（USER）'}`
})

const menuIconMap = {
  '/home': Home,
  '/policy-consultant': Sparkles,
  '/units': Building2,
  '/params': Settings2,
  '/knowledge': BookOpenText,
  '/skills': FolderKanban,
  '/ai-workbench': Bot,
  '/ai-ledger': FileClock,
  '/ai-monthly-report': FileBarChart2,
  '/experts': Users,
  '/ai-provider': KeyRound,
  '/ai-permissions': ShieldCheck,
  '/log-center': ScrollText,
  '/operation-logs': ScrollText,
  '/org-tree': Network,
  '/attendance': ClipboardCheck,
  '/weekly-work': NotebookPen,
  '/scores': ChartColumn,
  '/statistics': LayoutGrid,
  '/profile': SquareUserRound,
  '/users': UserCog
}

function resolveMenuIcon(path) {
  return menuIconMap[path] || LayoutGrid
}

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
  grid-template-columns: 260px minmax(0, 1fr);
  background: var(--app-bg);
}

.app-layout--mobile {
  grid-template-columns: 1fr;
  background:
    radial-gradient(circle at top right, rgba(197, 139, 58, 0.12), transparent 24%),
    linear-gradient(180deg, #f7f0e6, #f5f7fa 34%, #f6f3ee 100%);
}

.layout-sidebar {
  display: flex;
  flex-direction: column;
  gap: 24px;
  padding: 24px 16px 20px;
  background: #1c2e40;
  color: #fff;
  box-shadow: inset -1px 0 0 rgba(255, 255, 255, 0.05);
}

.layout-brand {
  padding: 6px 6px 4px;
}

.layout-brand-chip {
  display: inline-flex;
  align-items: center;
  padding: 5px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.08);
  color: rgba(255, 255, 255, 0.8);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.16em;
}

.layout-brand-title {
  margin-top: 16px;
  color: #f6f9fd;
  font-size: 24px;
  font-weight: 600;
  letter-spacing: -0.02em;
}

.layout-brand-subtitle {
  margin-top: 8px;
  color: rgba(255, 255, 255, 0.6);
  font-size: 12px;
  line-height: 1.6;
  letter-spacing: 0.08em;
}

.layout-menu {
  display: grid;
  gap: 10px;
  align-content: start;
}

.layout-menu-item {
  display: grid;
  grid-template-columns: 20px minmax(0, 1fr);
  gap: 12px;
  padding: 12px 14px;
  border-radius: 12px;
  text-decoration: none;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid transparent;
  transition: transform 0.2s ease, background-color 0.2s ease, border-color 0.2s ease;
}

.layout-menu-item:hover {
  transform: translateY(-1px);
  background: rgba(255, 255, 255, 0.08);
  border-color: rgba(170, 196, 236, 0.16);
}

.layout-menu-item--active {
  background: #3b82f6;
  border-color: #3b82f6;
  box-shadow: 0 12px 24px rgba(7, 19, 40, 0.18);
}

.layout-menu-icon {
  width: 18px;
  height: 18px;
  margin-top: 2px;
  color: rgba(255, 255, 255, 0.72);
}

.layout-menu-copy {
  min-width: 0;
}

.layout-menu-item--active .layout-menu-icon {
  color: #ffffff;
}

.layout-menu-title {
  display: block;
  color: #f4f8fd;
  font-size: 14px;
  font-weight: 600;
}

.layout-menu-desc {
  display: block;
  margin-top: 5px;
  color: rgba(210, 222, 239, 0.72);
  font-size: 12px;
  line-height: 1.55;
}

.layout-main {
  min-width: 0;
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto;
}

.layout-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  min-height: 64px;
  padding: 0 24px;
  background: #ffffff;
  border-bottom: 1px solid var(--app-border);
}

.layout-header--mobile {
  padding: 14px 16px 12px;
  min-height: 74px;
  background: rgba(255, 252, 248, 0.86);
  border-bottom-color: rgba(17, 24, 39, 0.06);
}

.layout-header-primary {
  min-width: 0;
}

.layout-header-eyebrow {
  font-size: 11px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: #7085a0;
  font-weight: 700;
}

.layout-header-title {
  margin-top: 6px;
  color: var(--app-title);
  font-size: 24px;
  font-weight: 600;
  letter-spacing: -0.02em;
}

.layout-header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.layout-mobile-home {
  border: none;
  border-radius: 999px;
  padding: 8px 12px;
  background: rgba(15, 108, 99, 0.08);
  color: #0f6c63;
  font-size: 12px;
  font-weight: 700;
}

.layout-user-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-height: 38px;
  padding: 0 14px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.8);
  border: 1px solid var(--app-border);
  color: var(--app-text);
  font-size: 13px;
  font-weight: 600;
}

.header-icon {
  width: 16px;
  height: 16px;
  color: var(--app-muted);
}

.layout-logout {
  border: 1px solid var(--app-border);
  border-radius: 10px;
  padding: 10px 14px;
  background: #ffffff;
  color: var(--app-text);
  cursor: pointer;
  font-size: 13px;
  font-weight: 600;
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;
}

.layout-logout:hover {
  transform: translateY(-1px);
  border-color: var(--app-primary);
  box-shadow: 0 8px 18px rgba(59, 130, 246, 0.12);
}

.layout-content {
  min-width: 0;
  padding: 24px;
}

.layout-content--mobile {
  padding: 14px 14px 10px;
}

@media (max-width: 1100px) {
  .app-layout {
    grid-template-columns: 232px minmax(0, 1fr);
  }

  .layout-content {
    padding: 20px;
  }
}

@media (max-width: 900px) {
  .app-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 520px) {
  .layout-header {
    padding-inline: 14px;
  }

  .layout-header-actions {
    gap: 8px;
  }

  .layout-user-badge {
    max-width: 120px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .layout-logout {
    padding: 9px 11px;
  }
}
</style>
