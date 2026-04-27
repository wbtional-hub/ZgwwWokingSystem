import { createRouter, createWebHistory } from 'vue-router'
import LoginView from '@/views/auth/LoginView.vue'
import HomeView from '@/views/home/HomeView.vue'
import NotFoundView from '@/views/error/NotFoundView.vue'
import NoPermissionView from '@/views/permission/NoPermissionView.vue'
import AppLayout from '@/layouts/AppLayout.vue'
import UnitListView from '@/views/unit/UnitListView.vue'
import ParamListView from '@/views/param/ParamListView.vue'
import OperationLogListView from '@/views/operationlog/OperationLogListView.vue'
import LogCenterView from '@/views/operationlog/LogCenterView.vue'
import OrgTreeView from '@/views/orgtree/OrgTreeView.vue'
import AttendanceCheckInView from '@/views/attendance/AttendanceCheckInView.vue'
import WeeklyWorkEditView from '@/views/weeklywork/WeeklyWorkEditView.vue'
import StatisticsOverviewView from '@/views/statistics/StatisticsOverviewView.vue'
import ScoreView from '@/views/score/ScoreView.vue'
import UserEditView from '@/views/user/UserEditView.vue'
import ProfileView from '@/views/profile/ProfileView.vue'
import MobileWorkspaceView from '@/views/mobile/MobileWorkspaceView.vue'
import MobileQrConfirmView from '@/views/mobile/MobileQrConfirmView.vue'
import KnowledgeBaseListView from '@/views/knowledge/KnowledgeBaseListView.vue'
import AIProviderConfigView from '@/views/ai/AIProviderConfigView.vue'
import AIPermissionConfigView from '@/views/ai/AIPermissionConfigView.vue'
import AICapabilityMapView from '@/views/ai/AICapabilityMapView.vue'
import SkillListView from '@/views/skill/SkillListView.vue'
import AIWorkbenchView from '@/views/agent/AIWorkbenchView.vue'
import AIConsultationLedgerView from '@/views/agent/AIConsultationLedgerView.vue'
import AIConsultationMonthlyReportView from '@/views/agent/AIConsultationMonthlyReportView.vue'
import AIResultFlowView from '@/views/agent/AIResultFlowView.vue'
import MobilePolicyConsultantView from '@/views/agent/MobilePolicyConsultantView.vue'
import ExpertListView from '@/views/expert/ExpertListView.vue'
import AiUserProfileView from '@/views/ai/AiUserProfileView.vue'
import { useUserStore } from '@/stores/user'
import { queryCurrentUserApi } from '@/api/auth'
import { queryCurrentUserModulePermissionsApi } from '@/api/user-module-permission'
import {
  AI_FLOW_MODULE_CODES,
  APP_MENU_ITEMS,
  MODULE_CODES,
  buildAccessContext,
  findFirstAccessiblePath,
  hasAnyModuleAccess
} from '@/constants/modules'
import { findFirstMobileWorkspacePath, MOBILE_WORKSPACE_PATH } from '@/constants/mobile-workspace'
import { isMobileClient } from '@/utils/device'

const WECHAT_LOGIN_RECOVERING_KEY = 'wechat_login_recovering'
const WECHAT_MOBILE_AUTH_ENTRY_PATHS = new Set(['/mobile-workspace', '/attendance'])

const routes = [
  { path: '/login', component: LoginView, meta: { public: true, title: '登录' } },
  {
    path: '/mobile',
    name: 'MobileEntry',
    component: () => import('@/views/MobileEntryView.vue'),
    meta: { public: true, title: '微信入口', enforceWechatBrowser: true, entryRedirectPath: MOBILE_WORKSPACE_PATH }
  },
  {
    path: '/mobile-dev',
    name: 'MobileDevEntry',
    component: () => import('@/views/MobileEntryView.vue'),
    meta: { public: true, title: '移动调试入口', enforceWechatBrowser: false, entryRedirectPath: MOBILE_WORKSPACE_PATH }
  },
  {
    path: '/policy-consultant',
    component: MobilePolicyConsultantView,
    meta: { moduleCode: MODULE_CODES.POLICY_CONSULTANT, title: '政策咨询' }
  },
  {
    path: '/',
    component: AppLayout,
    children: [
      { path: '', redirect: '/home' },
      { path: 'home', component: HomeView, meta: { title: '首页' } },
      { path: 'mobile-workspace', component: MobileWorkspaceView, meta: { title: 'Mobile Workspace' } },
      { path: 'units', component: UnitListView, meta: { adminOnly: true, moduleCode: MODULE_CODES.UNIT } },
      { path: 'params', component: ParamListView, meta: { adminOnly: true, moduleCode: MODULE_CODES.PARAM } },
      { path: 'ai-map', component: AICapabilityMapView, meta: { title: 'AI能力地图', accessAnyModuleCodes: AI_FLOW_MODULE_CODES } },
      {
  path: 'ai/user-profile',
  name: 'AiUserProfile',
  component: AiUserProfileView,
  meta: {
    title: '我的画像'
  }
},
      { path: 'ai-provider', component: AIProviderConfigView, meta: { adminOnly: true, moduleCode: MODULE_CODES.AI_PROVIDER } },
      { path: 'ai-permissions', component: AIPermissionConfigView, meta: { adminOnly: true, moduleCode: MODULE_CODES.AI_PERMISSION } },
      { path: 'knowledge', component: KnowledgeBaseListView, meta: { moduleCode: MODULE_CODES.KNOWLEDGE } },
      { path: 'skills', component: SkillListView, meta: { moduleCode: MODULE_CODES.SKILL } },
      { path: 'ai-workbench', component: AIWorkbenchView, meta: { moduleCode: MODULE_CODES.AI_WORKBENCH } },
      { path: 'ai-result-flow', component: AIResultFlowView, meta: { title: 'AI结果回流', accessAnyModuleCodes: AI_FLOW_MODULE_CODES } },
      { path: 'ai-ledger', component: AIConsultationLedgerView, meta: { moduleCode: MODULE_CODES.AI_LEDGER } },
      { path: 'ai-monthly-report', component: AIConsultationMonthlyReportView, meta: { moduleCode: MODULE_CODES.AI_MONTHLY_REPORT } },
      { path: 'experts', component: ExpertListView, meta: { moduleCode: MODULE_CODES.EXPERT } },
      { path: 'operation-logs', component: OperationLogListView, meta: { adminOnly: true, moduleCode: MODULE_CODES.OPERATION_LOG } },
      { path: 'log-center', component: LogCenterView, meta: { adminOnly: true, moduleCode: MODULE_CODES.LOG_CENTER, title: '日志中台' } },
      { path: 'users', component: UserEditView, meta: { adminOnly: true, moduleCode: MODULE_CODES.USER } },
      { path: 'org-tree', component: OrgTreeView, meta: { adminOnly: true, moduleCode: MODULE_CODES.ORG_TREE } },
      { path: 'attendance', component: AttendanceCheckInView, meta: { moduleCode: 'attendance_workbench', title: '考勤工作台' } },
{ path: 'attendance/stats', component: () => import('@/views/attendance/AttendanceStatsView.vue'), meta: { moduleCode: 'attendance_stats', title: '考勤统计' } },
{ path: 'attendance/patch-apply', component: () => import('@/views/attendance/AttendancePatchApplyView.vue'), meta: { moduleCode: 'attendance_patch_apply', title: '考勤申请' } },
{ path: 'attendance/patch-approvals', component: () => import('@/views/attendance/AttendancePatchApprovalView.vue'), meta: { moduleCode: 'attendance_patch_approvals', title: '考勤审批' } },
{ path: 'attendance/rules', component: () => import('@/views/attendance/AttendanceRuleConfigView.vue'), meta: { moduleCode: 'attendance_rules', title: '考勤规则' } },
      { path: 'weekly-work', component: WeeklyWorkEditView, meta: { moduleCode: MODULE_CODES.WEEKLY_WORK, title: '周报管理' } },
      { path: 'weekly-work/editor', component: WeeklyWorkEditView, meta: { moduleCode: MODULE_CODES.WEEKLY_WORK, title: '周报管理' } },
      { path: 'scores', component: ScoreView, meta: { adminOnly: true, moduleCode: MODULE_CODES.SCORE } },
      { path: 'statistics', component: StatisticsOverviewView, meta: { moduleCode: MODULE_CODES.STATISTICS } },
      { path: 'profile', component: ProfileView, meta: { title: '个人中心' } },
      { path: '403', component: NoPermissionView },
      { path: ':pathMatch(.*)*', component: NotFoundView }
    ]
  }
]

routes.splice(1, 0, {
  path: '/mobile/qr-confirm',
  alias: ['/mobile-dev/qr-confirm'],
  component: MobileQrConfirmView,
  meta: { public: true, title: '扫码登录确认' }
})

const router = createRouter({
  history: createWebHistory(),
  routes
})

let accessContextLoadingPromise = null

function clearWechatLoginRecoveryFlag() {
  if (typeof window === 'undefined') {
    return
  }
  try {
    window.sessionStorage.removeItem(WECHAT_LOGIN_RECOVERING_KEY)
  } catch (error) {
    // ignore sessionStorage access errors
  }
}

function setWechatLoginRecoveryFlag() {
  if (typeof window === 'undefined') {
    return
  }
  try {
    window.sessionStorage.setItem(WECHAT_LOGIN_RECOVERING_KEY, '1')
  } catch (error) {
    // ignore sessionStorage access errors
  }
}

function isWechatLoginRecovering() {
  if (typeof window === 'undefined') {
    return false
  }
  try {
    return window.sessionStorage.getItem(WECHAT_LOGIN_RECOVERING_KEY) === '1'
  } catch (error) {
    return false
  }
}

function isWechatBrowser() {
  if (typeof navigator === 'undefined') {
    return false
  }
  return /micromessenger/i.test(navigator.userAgent || '')
}

function normalizeEntryRedirect(value, fallback = MOBILE_WORKSPACE_PATH) {
  const redirect = Array.isArray(value) ? value[0] : value
  if (typeof redirect !== 'string' || !redirect.startsWith('/') || redirect.startsWith('//')) {
    return fallback
  }
  if (redirect === '/mobile' || redirect.startsWith('/mobile?') || redirect === '/login') {
    return fallback
  }
  return redirect
}

function applyMobileEntryRedirect(to) {
  if (to.path !== '/mobile') {
    return
  }
  to.meta.entryRedirectPath = normalizeEntryRedirect(to.query?.redirect, MOBILE_WORKSPACE_PATH)
}

function shouldStartWechatMobileAuth(to, from) {
  return WECHAT_MOBILE_AUTH_ENTRY_PATHS.has(to.path)
    && isWechatBrowser()
    && isMobileClient()
    && !isWechatLoginRecovering()
    && (!from || from.matched.length === 0)
}

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
        const userInfo = ensureSuccess(userResponse, '获取当前用户信息失败') || {}
        const moduleData = ensureSuccess(moduleResponse, '获取当前用户模块权限失败') || {}
        userStore.setAccessContext({
          userInfo,
          moduleCodes: Array.isArray(moduleData.moduleCodes) ? moduleData.moduleCodes : []
        })
        clearWechatLoginRecoveryFlag()
      })
      .finally(() => {
        accessContextLoadingPromise = null
      })
  }
  return accessContextLoadingPromise
}

router.beforeEach(async (to, from) => {
  if (to.path.startsWith('/api/auth/wechat-mp-callback')) {
    return '/login'
  }

  applyMobileEntryRedirect(to)

  const userStore = useUserStore()
  if (shouldStartWechatMobileAuth(to, from)) {
    setWechatLoginRecoveryFlag()
    userStore.clearLogin()
    return {
      path: '/mobile',
      query: { redirect: normalizeEntryRedirect(to.fullPath, MOBILE_WORKSPACE_PATH) }
    }
  }

  if (to.meta.public) {
    if (to.path === '/login' && userStore.token) {
      if (userStore.forcePasswordChange) {
        return '/profile?forcePasswordChange=1'
      }
      await ensureAccessContext(userStore)
      const accessContext = buildAccessContext(userStore.userInfo)
      return isMobileClient() ? findFirstMobileWorkspacePath(accessContext) : findFirstAccessiblePath(accessContext)
    }
    return true
  }

  if (!userStore.token) {
    return {
      path: '/login',
      query: { redirect: to.fullPath }
    }
  }

  if (userStore.forcePasswordChange) {
    return to.path === '/profile' ? true : '/profile?forcePasswordChange=1'
  }

  await ensureAccessContext(userStore)
  const accessContext = buildAccessContext(userStore.userInfo)

  if (isMobileClient() && to.path === '/home') {
    return findFirstMobileWorkspacePath(accessContext)
  }

  if (to.meta.adminOnly && !accessContext.isAdmin) {
    return '/403'
  }

  if (Array.isArray(to.meta.accessAnyModuleCodes) && to.meta.accessAnyModuleCodes.length) {
    if (!hasAnyModuleAccess(accessContext, to.meta.accessAnyModuleCodes)) {
      return '/403'
    }
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
