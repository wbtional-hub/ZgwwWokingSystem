import { createRouter, createWebHistory } from 'vue-router'
import LoginView from '@/views/auth/LoginView.vue'
import HomeView from '@/views/home/HomeView.vue'
import NotFoundView from '@/views/error/NotFoundView.vue'
import NoPermissionView from '@/views/permission/NoPermissionView.vue'
import AppLayout from '@/layouts/AppLayout.vue'
import UnitListView from '@/views/unit/UnitListView.vue'
import ParamListView from '@/views/param/ParamListView.vue'
import OperationLogListView from '@/views/operationlog/OperationLogListView.vue'
import OrgTreeView from '@/views/orgtree/OrgTreeView.vue'
import AttendanceCheckInView from '@/views/attendance/AttendanceCheckInView.vue'
import WeeklyWorkEditView from '@/views/weeklywork/WeeklyWorkEditView.vue'
import StatisticsOverviewView from '@/views/statistics/StatisticsOverviewView.vue'
import ScoreView from '@/views/score/ScoreView.vue'
import UserEditView from '@/views/user/UserEditView.vue'
import ProfileView from '@/views/profile/ProfileView.vue'
import KnowledgeBaseListView from '@/views/knowledge/KnowledgeBaseListView.vue'
import AIProviderConfigView from '@/views/ai/AIProviderConfigView.vue'
import AIPermissionConfigView from '@/views/ai/AIPermissionConfigView.vue'
import SkillListView from '@/views/skill/SkillListView.vue'
import AIWorkbenchView from '@/views/agent/AIWorkbenchView.vue'
import AIConsultationLedgerView from '@/views/agent/AIConsultationLedgerView.vue'
import AIConsultationMonthlyReportView from '@/views/agent/AIConsultationMonthlyReportView.vue'
import ExpertListView from '@/views/expert/ExpertListView.vue'
import { useUserStore } from '@/stores/user'
import { queryCurrentUserApi } from '@/api/auth'
import { queryCurrentUserModulePermissionsApi } from '@/api/user-module-permission'
import { APP_MENU_ITEMS, MODULE_CODES, buildAccessContext, findFirstAccessiblePath } from '@/constants/modules'

const routes = [
  { path: '/login', component: LoginView, meta: { public: true } },
  {
    path: '/',
    component: AppLayout,
    children: [
      { path: '', redirect: '/home' },
      { path: 'home', component: HomeView },
      { path: 'units', component: UnitListView, meta: { adminOnly: true, moduleCode: MODULE_CODES.UNIT } },
      { path: 'params', component: ParamListView, meta: { adminOnly: true, moduleCode: MODULE_CODES.PARAM } },
      { path: 'knowledge', component: KnowledgeBaseListView, meta: { moduleCode: MODULE_CODES.KNOWLEDGE } },
      { path: 'skills', component: SkillListView, meta: { moduleCode: MODULE_CODES.SKILL } },
      { path: 'ai-workbench', component: AIWorkbenchView, meta: { moduleCode: MODULE_CODES.AI_WORKBENCH } },
      { path: 'ai-ledger', component: AIConsultationLedgerView, meta: { moduleCode: MODULE_CODES.AI_LEDGER } },
      { path: 'ai-monthly-report', component: AIConsultationMonthlyReportView, meta: { moduleCode: MODULE_CODES.AI_MONTHLY_REPORT } },
      { path: 'experts', component: ExpertListView, meta: { moduleCode: MODULE_CODES.EXPERT } },
      { path: 'ai-provider', component: AIProviderConfigView, meta: { adminOnly: true, moduleCode: MODULE_CODES.AI_PROVIDER } },
      { path: 'ai-permissions', component: AIPermissionConfigView, meta: { adminOnly: true, moduleCode: MODULE_CODES.AI_PERMISSION } },
      { path: 'operation-logs', component: OperationLogListView, meta: { adminOnly: true, moduleCode: MODULE_CODES.OPERATION_LOG } },
      { path: 'users', component: UserEditView, meta: { adminOnly: true, moduleCode: MODULE_CODES.USER } },
      { path: 'org-tree', component: OrgTreeView, meta: { adminOnly: true, moduleCode: MODULE_CODES.ORG_TREE } },
      { path: 'attendance', component: AttendanceCheckInView, meta: { moduleCode: MODULE_CODES.ATTENDANCE } },
      { path: 'weekly-work', component: WeeklyWorkEditView, meta: { moduleCode: MODULE_CODES.WEEKLY_WORK } },
      { path: 'weekly-work/editor', component: WeeklyWorkEditView, meta: { moduleCode: MODULE_CODES.WEEKLY_WORK } },
      { path: 'scores', component: ScoreView, meta: { adminOnly: true, moduleCode: MODULE_CODES.SCORE } },
      { path: 'statistics', component: StatisticsOverviewView, meta: { moduleCode: MODULE_CODES.STATISTICS } },
      { path: 'profile', component: ProfileView },
      { path: '403', component: NoPermissionView },
      { path: ':pathMatch(.*)*', component: NotFoundView }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

let accessContextLoadingPromise = null

function ensureSuccess(response, fallbackMessage) {
  if (!response || response.code !== 0) {
    throw new Error(response?.message || fallbackMessage)
  }
  return response.data
}

async function ensureAccessContext(userStore) {
  if (!userStore.token) {
    return
  }
  if (userStore.userInfo?.userId && userStore.accessReady) {
    return
  }
  if (!accessContextLoadingPromise) {
    accessContextLoadingPromise = Promise.all([
      queryCurrentUserApi(),
      queryCurrentUserModulePermissionsApi()
    ])
      .then(([userResponse, moduleResponse]) => {
        const userInfo = ensureSuccess(userResponse, '用户信息加载失败') || {}
        const moduleData = ensureSuccess(moduleResponse, '模块权限加载失败') || {}
        userStore.setAccessContext({
          userInfo,
          moduleCodes: Array.isArray(moduleData.moduleCodes) ? moduleData.moduleCodes : []
        })
      })
      .finally(() => {
        accessContextLoadingPromise = null
      })
  }
  return accessContextLoadingPromise
}

router.beforeEach(async (to) => {
  const userStore = useUserStore()
  if (to.meta.public) {
    if (to.path === '/login' && userStore.token) {
      await ensureAccessContext(userStore)
      return findFirstAccessiblePath(buildAccessContext(userStore.userInfo))
    }
    return true
  }

  if (!userStore.token) {
    return '/login'
  }

  await ensureAccessContext(userStore)
  const accessContext = buildAccessContext(userStore.userInfo)

  if (to.meta.adminOnly && !accessContext.isAdmin) {
    return '/403'
  }

  if (to.meta.moduleCode) {
    const menuItem = APP_MENU_ITEMS.find((item) => item.moduleCode === to.meta.moduleCode && item.path === to.path)
      || APP_MENU_ITEMS.find((item) => item.moduleCode === to.meta.moduleCode)
    const hasAccess = accessContext.isAdmin || accessContext.moduleCodes.includes(to.meta.moduleCode)
    if (menuItem && !menuItem.alwaysVisible && !hasAccess) {
      return '/403'
    }
  }

  return true
})

export default router
