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
    title: '政策智能体',
    shortTitle: '智能体',
    path: '/policy-consultant',
    moduleCode: MODULE_CODES.POLICY_CONSULTANT,
    badge: 'AI',
    heroTitle: '政策智能体',
    heroDescription: '面向人才政策、产业政策与申报服务的智能咨询助手，支持政策匹配、申报指引和依据追溯。',
    actionText: '启动智能体',
    sceneText: '适合政策匹配、材料清单、申报流程与省市边界咨询',
    accentClass: 'mobile-accent-policy',
    initials: 'AI',
    matchPaths: ['/policy-consultant']
  },
  {
    key: 'knowledge',
    title: '知识库中心',
    shortTitle: '知识库',
    path: '/knowledge',
    moduleCode: MODULE_CODES.KNOWLEDGE,
    badge: 'KB',
    heroTitle: '知识库中心',
    heroDescription: '按授权查看和使用知识库。',
    actionText: '查看知识库',
    sceneText: '知识条目、分类与内容查看',
    accentClass: 'mobile-accent-knowledge',
    initials: 'ZS',
    matchPaths: ['/knowledge']
  },
  {
    key: 'expert',
    title: '专家台账',
    shortTitle: '专家',
    path: '/experts',
    moduleCode: MODULE_CODES.EXPERT,
    badge: 'Expert',
    heroTitle: '专家台账',
    heroDescription: '查看专家身份与技能归属。',
    actionText: '查看专家',
    sceneText: '专家信息与技能归属',
    accentClass: 'mobile-accent-expert',
    initials: 'ZJ',
    matchPaths: ['/experts']
  },
  {
    key: 'attendance',
    title: '考勤工作台',
    shortTitle: '考勤',
    path: '/attendance',
    moduleCode: MODULE_CODES.ATTENDANCE_WORKBENCH,
    badge: 'Work',
    heroTitle: '考勤工作台',
    heroDescription: '进入后即可定位打卡，兼容浏览器定位和微信端定位来源。',
    actionText: '进入考勤',
    sceneText: '上班签到、下班打卡、查看当前考勤状态',
    accentClass: 'mobile-accent-attendance',
    initials: 'KQ',
    matchPaths: ['/attendance']
  },
  {
    key: 'attendance-stats',
    title: '考勤统计',
    shortTitle: '统计',
    path: '/attendance/stats',
    moduleCode: MODULE_CODES.ATTENDANCE_STATS,
    badge: 'Work',
    heroTitle: '考勤统计',
    heroDescription: '查看团队应到、异常与本周概览。',
    actionText: '查看统计',
    sceneText: '团队应到、异常、周统计',
    accentClass: 'mobile-accent-attendance',
    initials: 'TJ',
    matchPaths: ['/attendance/stats']
  },
  {
    key: 'attendance-patch-apply',
    title: '补打卡申请',
    shortTitle: '补卡',
    path: '/attendance/patch-apply',
    moduleCode: MODULE_CODES.ATTENDANCE_PATCH_APPLY,
    badge: 'Work',
    heroTitle: '补打卡申请',
    heroDescription: '提交并查看我的补打卡申请。',
    actionText: '提交申请',
    sceneText: '补上班卡、补下班卡、查看申请进度',
    accentClass: 'mobile-accent-attendance',
    initials: 'BK',
    matchPaths: ['/attendance/patch-apply']
  },
  {
    key: 'attendance-patch-approvals',
    title: '补打卡审批',
    shortTitle: '审批',
    path: '/attendance/patch-approvals',
    moduleCode: MODULE_CODES.ATTENDANCE_PATCH_APPROVALS,
    badge: 'Work',
    heroTitle: '补打卡审批',
    heroDescription: '审批待处理的补打卡申请。',
    actionText: '进入审批',
    sceneText: '审批待处理补卡申请',
    accentClass: 'mobile-accent-attendance',
    initials: 'SP',
    matchPaths: ['/attendance/patch-approvals']
  },
  {
    key: 'attendance-rules',
    title: '考勤规则',
    shortTitle: '规则',
    path: '/attendance/rules',
    moduleCode: MODULE_CODES.ATTENDANCE_RULES,
    badge: 'Work',
    heroTitle: '考勤规则',
    heroDescription: '查看和维护考勤规则配置。',
    actionText: '查看规则',
    sceneText: '上下班时间、宽限设置、规则查看',
    accentClass: 'mobile-accent-attendance',
    initials: 'GZ',
    matchPaths: ['/attendance/rules']
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
    moduleCode: MODULE_CODES.PROFILE,
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
