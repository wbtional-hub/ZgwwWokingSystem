function formatDateTime(value) {
  if (!value) {
    return '-';
  }
  return String(value).replace('T', ' ');
}

function formatDate(value) {
  if (!value) {
    return '-';
  }
  if (value instanceof Date) {
    return `${value.getFullYear()}-${String(value.getMonth() + 1).padStart(2, '0')}-${String(value.getDate()).padStart(2, '0')}`;
  }
  return String(value).slice(0, 10);
}

function getIsoWeek(date) {
  const target = new Date(date.getTime());
  const dayNumber = (date.getDay() + 6) % 7;
  target.setDate(target.getDate() - dayNumber + 3);
  const firstThursday = new Date(target.getFullYear(), 0, 4);
  const firstDayNumber = (firstThursday.getDay() + 6) % 7;
  firstThursday.setDate(firstThursday.getDate() - firstDayNumber + 3);
  const diff = target.getTime() - firstThursday.getTime();
  return 1 + Math.round(diff / 604800000);
}

function getCurrentWeekNo(date) {
  const current = date || new Date();
  return `${current.getFullYear()}-W${String(getIsoWeek(current)).padStart(2, '0')}`;
}

function formatPercent(value) {
  if (value === null || value === undefined || Number.isNaN(Number(value))) {
    return '-';
  }
  return `${(Number(value) * 100).toFixed(1)}%`;
}

function truncate(text, maxLength) {
  if (!text) {
    return '';
  }
  const content = String(text);
  if (content.length <= maxLength) {
    return content;
  }
  return `${content.slice(0, maxLength)}...`;
}

function resolveAttendanceStatusText(status) {
  const statusMap = {
    CHECK_IN_SUCCESS: '签到成功',
    CHECK_OUT_SUCCESS: '签退成功',
    LOCATION_REQUIRED: '需要定位',
    LOCATION_INVALID: '定位无效',
    LOCATION_WEAK: '定位较弱',
    OUT_OF_RANGE: '超出范围',
    ALREADY_FINISHED: '今日已完成'
  };
  return statusMap[status] || status || '-';
}

function resolveWeeklyStatusText(status) {
  const statusMap = {
    DRAFT: '草稿',
    SUBMITTED: '已提交',
    PENDING_SECTION_CHIEF: '待科长审批',
    PENDING_DEPUTY_LEADER: '待分管领导审批',
    PENDING_LEGION_LEADER: '待主官审批',
    RETURNED: '已退回',
    APPROVED: '已通过'
  };
  return statusMap[status] || status || '-';
}

function resolveWeeklyNodeText(nodeCode) {
  const nodeMap = {
    STAFF: '本人完善',
    SECTION_CHIEF: '科长审批',
    DEPUTY_LEADER: '分管领导审批',
    LEGION_LEADER: '主官审批',
    APPROVED: '流程完成'
  };
  return nodeMap[nodeCode] || nodeCode || '-';
}

module.exports = {
  formatDateTime,
  formatDate,
  getCurrentWeekNo,
  formatPercent,
  truncate,
  resolveAttendanceStatusText,
  resolveWeeklyStatusText,
  resolveWeeklyNodeText
};
