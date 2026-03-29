export const MODULE_CODES = Object.freeze({
  UNIT: 'unit',
  PARAM: 'param',
  KNOWLEDGE: 'knowledge',
  SKILL: 'skill',
  AI_WORKBENCH: 'ai_workbench',
  AI_LEDGER: 'ai_ledger',
  AI_MONTHLY_REPORT: 'ai_monthly_report',
  EXPERT: 'expert',
  AI_PROVIDER: 'ai_provider',
  AI_PERMISSION: 'ai_permission',
  OPERATION_LOG: 'operationlog',
  ORG_TREE: 'orgtree',
  ATTENDANCE: 'attendance',
  WEEKLY_WORK: 'weeklywork',
  SCORE: 'score',
  STATISTICS: 'statistics',
  USER: 'user'
})

export const APP_MENU_ITEMS = [
  { path: '/home', title: '首页', description: '系统欢迎页', alwaysVisible: true },
  { path: '/units', title: '单位管理', description: '维护单位基础信息', adminOnly: true, moduleCode: MODULE_CODES.UNIT },
  { path: '/params', title: '参数管理', description: '维护系统参数', adminOnly: true, moduleCode: MODULE_CODES.PARAM },
  { path: '/knowledge', title: '知识库中心', description: '按授权查看和使用知识库', moduleCode: MODULE_CODES.KNOWLEDGE },
  { path: '/skills', title: 'Skills 中心', description: '训练、验证和发布技能', moduleCode: MODULE_CODES.SKILL },
  { path: '/ai-workbench', title: 'AI 工作台', description: '按技能进行问答和分析', moduleCode: MODULE_CODES.AI_WORKBENCH },
  { path: '/ai-ledger', title: '咨询台账', description: '查看 AI 咨询记录与统计', moduleCode: MODULE_CODES.AI_LEDGER },
  { path: '/ai-monthly-report', title: '月度报表', description: '查看 AI 咨询月度经营看板', moduleCode: MODULE_CODES.AI_MONTHLY_REPORT },
  { path: '/experts', title: '专家台账', description: '查看专家身份与技能归属', moduleCode: MODULE_CODES.EXPERT },
  { path: '/ai-provider', title: 'AI 接入区', description: '配置模型接入与 Token', adminOnly: true, moduleCode: MODULE_CODES.AI_PROVIDER },
  { path: '/ai-permissions', title: 'AI 权限配置', description: '分配 AI、知识库和技能权限', adminOnly: true, moduleCode: MODULE_CODES.AI_PERMISSION },
  { path: '/operation-logs', title: '操作日志', description: '查看关键操作记录', adminOnly: true, moduleCode: MODULE_CODES.OPERATION_LOG },
  { path: '/org-tree', title: '组织架构', description: '维护组织树结构', adminOnly: true, moduleCode: MODULE_CODES.ORG_TREE },
  { path: '/attendance', title: '签到管理', description: '日常签到与考勤', moduleCode: MODULE_CODES.ATTENDANCE },
  { path: '/weekly-work', title: '周报管理', description: '填写和查看周报', moduleCode: MODULE_CODES.WEEKLY_WORK },
  { path: '/scores', title: '工作评分', description: '查看评分结果', adminOnly: true, moduleCode: MODULE_CODES.SCORE },
  { path: '/statistics', title: '统计分析', description: '系统统计看板', moduleCode: MODULE_CODES.STATISTICS },
  { path: '/profile', title: '个人中心', description: '查看个人资料与账号信息', alwaysVisible: true },
  { path: '/users', title: '用户管理', description: '维护系统用户', adminOnly: true, moduleCode: MODULE_CODES.USER }
]

export function buildAccessContext(userInfo) {
  const role = userInfo?.role || (userInfo?.superAdmin ? 'ADMIN' : 'USER')
  return {
    isAdmin: role === 'ADMIN',
    moduleCodes: Array.isArray(userInfo?.moduleCodes) ? userInfo.moduleCodes : []
  }
}

export function canAccessMenuItem(item, accessContext) {
  if (!item) {
    return false
  }
  if (item.alwaysVisible) {
    return true
  }
  if (item.adminOnly && !accessContext?.isAdmin) {
    return false
  }
  if (!item.moduleCode) {
    return true
  }
  return Boolean(accessContext?.isAdmin || accessContext?.moduleCodes?.includes(item.moduleCode))
}

export function filterMenuItems(menuItems, accessContext) {
  return (menuItems || []).filter((item) => canAccessMenuItem(item, accessContext))
}

export function findFirstAccessiblePath(accessContext) {
  return filterMenuItems(APP_MENU_ITEMS, accessContext)[0]?.path || '/home'
}

export function findMenuItemByPath(path) {
  return APP_MENU_ITEMS.find((item) => item.path === path) || null
}
