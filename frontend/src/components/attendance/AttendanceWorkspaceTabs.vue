<template>
  <div class="attendance-tabs">
    <button
      v-for="item in visibleTabs"
      :key="item.path"
      type="button"
      class="attendance-tab"
      :class="{ 'attendance-tab--active': route.path === item.path }"
      @click="goTab(item.path)"
    >
      <span class="attendance-tab__title">{{ item.label }}</span>
      <span class="attendance-tab__desc">{{ item.description }}</span>
    </button>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import request from '@/utils/request'

const route = useRoute()
const router = useRouter()

const STORAGE_KEY = 'attendance_tab_permission_codes'

const tabs = [
  {
    code: 'attendance_workbench',
    path: '/attendance',
    label: '工作台',
    description: '今日打卡与团队视图'
  },
  {
    code: 'attendance_stats',
    path: '/attendance/stats',
    label: '统计',
    description: '应到、异常与本周概览'
  },
  {
    code: 'attendance_patch_apply',
    path: '/attendance/patch-apply',
    label: '考勤申请',
    description: '提交补打卡、取证申请'
  },
  {
    code: 'attendance_patch_approvals',
    path: '/attendance/patch-approvals',
    label: '考勤审批',
    description: '审批考勤申请'
  },
  {
    code: 'attendance_rules',
    path: '/attendance/rules',
    label: '考勤规则',
    description: '配置上下班与宽限时间'
  }
]

const permissionCodes = ref(loadCachedPermissionCodes())
const permissionLoaded = ref(permissionCodes.value.size > 0)

const visibleTabs = computed(() => {
  if (permissionCodes.value.size > 0) {
    return tabs.filter((item) => {
      return permissionCodes.value.has(item.code) || permissionCodes.value.has(item.path)
    })
  }

  // 首次加载权限前，不再先显示全部按钮，避免“先显示后隐藏”的闪烁
  // 这里只保留当前页面对应按钮，权限加载完成后再补齐用户真正有权限的按钮
  return tabs.filter((item) => item.path === route.path)
})

onMounted(() => {
  loadCurrentUserModules()
})

function goTab(path) {
  if (route.path === path) {
    return
  }
  router.push(path)
}

async function loadCurrentUserModules() {
  try {
    const response = await request.get('/user-module-permissions/current')
    const data = response?.data || response
    const rawModules = resolveModuleList(data)
    const codes = rawModules
      .map((item) => resolveModuleCode(item))
      .filter(Boolean)

    permissionCodes.value = new Set(codes)
    cachePermissionCodes(codes)
  } catch (error) {
    // 权限接口异常时，不展示全部按钮，避免越权按钮闪现
    // 只保留当前路由按钮，保证页面不空、不影响当前功能
    permissionCodes.value = new Set(
      tabs
        .filter((item) => item.path === route.path)
        .map((item) => item.code)
    )
  } finally {
    permissionLoaded.value = true
  }
}

function resolveModuleList(data) {
  if (Array.isArray(data)) {
    return data
  }

  if (Array.isArray(data?.data)) {
    return data.data
  }

  if (Array.isArray(data?.modules)) {
    return data.modules
  }

  if (Array.isArray(data?.moduleCodes)) {
    return data.moduleCodes
  }

  if (Array.isArray(data?.permissions)) {
    return data.permissions
  }

  if (Array.isArray(data?.list)) {
    return data.list
  }

  if (Array.isArray(data?.data?.modules)) {
    return data.data.modules
  }

  if (Array.isArray(data?.data?.moduleCodes)) {
    return data.data.moduleCodes
  }

  if (Array.isArray(data?.data?.permissions)) {
    return data.data.permissions
  }

  if (Array.isArray(data?.data?.list)) {
    return data.data.list
  }

  return []
}

function resolveModuleCode(item) {
  if (!item) {
    return ''
  }

  if (typeof item === 'string') {
    return item
  }

  return (
    item.code ||
    item.moduleCode ||
    item.module_code ||
    item.moduleKey ||
    item.module_key ||
    item.permissionCode ||
    item.permission_code ||
    item.path ||
    ''
  )
}

function loadCachedPermissionCodes() {
  try {
    const raw = sessionStorage.getItem(STORAGE_KEY)
    const list = raw ? JSON.parse(raw) : []
    return new Set(Array.isArray(list) ? list.filter(Boolean) : [])
  } catch (error) {
    return new Set()
  }
}

function cachePermissionCodes(codes) {
  try {
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(codes))
  } catch (error) {
    // 忽略缓存失败，不影响页面功能
  }
}
</script>

<style scoped>
.attendance-tabs {
  display: flex;
  flex-wrap: wrap;
  align-items: stretch;
  gap: 10px;
  margin-bottom: 18px;
}

.attendance-tab {
  flex: 0 0 220px;
  min-height: 74px;
  padding: 12px 14px;
  border: 1px solid #dbe3ef;
  border-radius: 16px;
  background: linear-gradient(180deg, #ffffff, #f8fafc);
  text-align: left;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.04);
  cursor: pointer;
}

.attendance-tab--active {
  border-color: #2563eb;
  background: linear-gradient(180deg, #eff6ff, #ffffff);
  box-shadow: 0 12px 26px rgba(37, 99, 235, 0.14);
}

.attendance-tab__title {
  display: block;
  font-size: 14px;
  font-weight: 700;
  color: #0f172a;
}

.attendance-tab__desc {
  display: block;
  margin-top: 6px;
  font-size: 12px;
  line-height: 1.6;
  color: #64748b;
}

@media (max-width: 768px) {
  .attendance-tab {
    flex: 1 1 calc(50% - 10px);
  }
}

@media (max-width: 480px) {
  .attendance-tab {
    flex: 1 1 100%;
  }
}
</style>