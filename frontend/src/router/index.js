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
import { useUserStore } from '@/stores/user'

const routes = [
  { path: '/login', component: LoginView, meta: { public: true } },
  {
    path: '/',
    component: AppLayout,
    children: [
      { path: '', redirect: '/home' },
      { path: 'home', component: HomeView },
      { path: 'units', component: UnitListView, meta: { adminOnly: true } },
      { path: 'params', component: ParamListView, meta: { adminOnly: true } },
      { path: 'operation-logs', component: OperationLogListView, meta: { adminOnly: true } },
      { path: 'users', component: UserEditView, meta: { adminOnly: true } },
      { path: 'org-tree', component: OrgTreeView, meta: { adminOnly: true } },
      { path: 'attendance', component: AttendanceCheckInView },
      { path: 'weekly-work', component: WeeklyWorkEditView },
      { path: 'weekly-work/editor', component: WeeklyWorkEditView },
      { path: 'scores', component: ScoreView },
      { path: 'statistics', component: StatisticsOverviewView },
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

router.beforeEach((to) => {
  const userStore = useUserStore()
  const role = userStore.userInfo?.role || (userStore.userInfo?.superAdmin ? 'ADMIN' : 'USER')
  const isAdmin = role === 'ADMIN'
  if (to.meta.public) {
    if (to.path === '/login' && userStore.token) {
      return '/home'
    }
    return true
  }

  if (!userStore.token) {
    return '/login'
  }

  if (to.meta.adminOnly && !isAdmin) {
    return '/403'
  }

  return true
})

export default router
