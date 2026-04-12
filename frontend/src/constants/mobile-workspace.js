import { MODULE_CODES } from '@/constants/modules'

export const MOBILE_WORKSPACE_PATH = '/mobile-workspace'

export const MOBILE_WORKSPACE_ITEMS = [
  {
    key: 'workspace',
    title: '工作台',
    shortTitle: '工作台',
    path: MOBILE_WORKSPACE_PATH,
    alwaysVisible: true,
    badge: 'Mobile',
    heroTitle: '手机工作台',
    heroDescription: '集中展示手机端已授权功能，后续新增模块也可以直接接入这里。',
    actionText: '查看全部',
    accentClass: 'mobile-accent-workspace',
    initials: 'WK'
  },
  {
    key: 'policy',
    title: '政策咨询',
    shortTitle: '咨询',
    path: '/policy-consultant',
    moduleCode: MODULE_CODES.POLICY_CONSULTANT,
    badge: 'AI',
    heroTitle: '政策咨询',
    heroDescription: '按账号权限进入政策咨询智能体，有 AI 时优先智能回答，没有 AI 时自动走知识库兜底。',
    actionText: '开始咨询',
    sceneText: '适合人才认定、补贴、住房、创业等咨询',
    accentClass: 'mobile-accent-policy',
    initials: 'ZX',
    matchPaths: ['/policy-consultant']
  },
  {
    key: 'attendance',
    title: '签到管理',
    shortTitle: '签到',
    path: '/attendance',
    moduleCode: MODULE_CODES.ATTENDANCE,
    badge: 'Work',
    heroTitle: '签到打卡',
    heroDescription: '进入后即可定位打卡，兼容浏览器定位和微信端定位来源。',
    actionText: '进入打卡',
    sceneText: '上班签到、下班打卡、查看当前考勤状态',
    accentClass: 'mobile-accent-attendance',
    initials: 'QD',
    matchPaths: ['/attendance']
  },
  {
    key: 'weekly',
    title: '周报管理',
    shortTitle: '周报',
    path: '/weekly-work',
    moduleCode: MODULE_CODES.WEEKLY_WORK,
    badge: 'Flow',
    heroTitle: '周报处理',
    heroDescription: '手机端可填写任务、保存草稿、提交审核，也方便快速查看当前周报状态。',
    actionText: '填写周报',
    sceneText: '填写本周进展、补充任务、提交审批',
    accentClass: 'mobile-accent-weekly',
    initials: 'ZB',
    matchPaths: ['/weekly-work', '/weekly-work/editor']
  },
  {
    key: 'profile',
    title: '个人中心',
    shortTitle: '我的',
    path: '/profile',
    alwaysVisible: true,
    badge: 'Me',
    heroTitle: '个人中心',
    heroDescription: '查看当前账号资料、绑定关系和个人系统信息。',
    actionText: '查看资料',
    sceneText: '查看账号资料、确认当前登录身份',
    accentClass: 'mobile-accent-profile',
    initials: 'ME',
    matchPaths: ['/profile']
  }
]

export function canAccessMobileWorkspaceItem(item, accessContext) {
  if (!item) {
    return false
  }
  if (item.alwaysVisible) {
    return true
  }
  if (!item.moduleCode) {
    return true
  }
  return Boolean(accessContext?.isAdmin || accessContext?.moduleCodes?.includes(item.moduleCode))
}

export function resolveMobileWorkspaceItems(accessContext) {
  return MOBILE_WORKSPACE_ITEMS.filter((item) => canAccessMobileWorkspaceItem(item, accessContext))
}

export function findFirstMobileWorkspacePath(accessContext) {
  return resolveMobileWorkspaceItems(accessContext)[0]?.path || MOBILE_WORKSPACE_PATH
}

export function findMobileWorkspaceItemByPath(path) {
  return MOBILE_WORKSPACE_ITEMS.find((item) => {
    const matchPaths = Array.isArray(item.matchPaths) ? item.matchPaths : [item.path]
    return matchPaths.includes(path)
  }) || null
}
