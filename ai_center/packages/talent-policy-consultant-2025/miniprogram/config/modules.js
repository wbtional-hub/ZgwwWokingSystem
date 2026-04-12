const MODULE_CODES = Object.freeze({
  POLICY_CONSULTANT: 'policy_consultant',
  ATTENDANCE: 'attendance',
  WEEKLY_WORK: 'weeklywork'
});

const WORKSPACE_ITEMS = [
  {
    key: 'policy',
    title: '政策咨询',
    badge: 'AI',
    description: '基于知识库和 AI 能力进行政策问答与依据引用。',
    path: '/pages/policy/index',
    moduleCode: MODULE_CODES.POLICY_CONSULTANT
  },
  {
    key: 'attendance',
    title: '考勤签到',
    badge: 'Work',
    description: '查看打卡点信息，获取定位并直接完成签到或签退。',
    path: '/pages/attendance/index',
    moduleCode: MODULE_CODES.ATTENDANCE
  },
  {
    key: 'weekly',
    title: '周报管理',
    badge: 'Flow',
    description: '查看本人周报，保存草稿并按流程提交审核。',
    path: '/pages/weekly/index',
    moduleCode: MODULE_CODES.WEEKLY_WORK
  },
  {
    key: 'profile',
    title: '个人中心',
    badge: 'Me',
    description: '查看当前登录身份、模块权限并安全退出登录。',
    path: '/pages/profile/index',
    alwaysVisible: true
  }
];

function buildAccessContext(userInfo, moduleCodes) {
  const role = userInfo && (userInfo.role || (userInfo.superAdmin ? 'ADMIN' : 'USER'));
  return {
    isAdmin: role === 'ADMIN',
    moduleCodes: Array.isArray(moduleCodes) ? moduleCodes : []
  };
}

function canAccessItem(item, accessContext) {
  if (!item) {
    return false;
  }
  if (item.alwaysVisible) {
    return true;
  }
  if (!item.moduleCode) {
    return true;
  }
  return Boolean(accessContext.isAdmin || accessContext.moduleCodes.includes(item.moduleCode));
}

function resolveWorkspaceItems(accessContext) {
  return WORKSPACE_ITEMS.filter((item) => canAccessItem(item, accessContext));
}

module.exports = {
  MODULE_CODES,
  WORKSPACE_ITEMS,
  buildAccessContext,
  canAccessItem,
  resolveWorkspaceItems
};
