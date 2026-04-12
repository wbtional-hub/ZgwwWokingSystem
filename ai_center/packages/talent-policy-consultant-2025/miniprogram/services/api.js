const { request } = require('../utils/request');

function wechatMiniLogin(data) {
  return request({
    url: '/auth/wechat-mini-login',
    method: 'POST',
    data,
    auth: false
  });
}

function queryCurrentUser() {
  return request({ url: '/auth/me' });
}

function queryCurrentUserModulePermissions() {
  return request({ url: '/user-module-permissions/current' });
}

function queryCurrentAiPermission() {
  return request({
    url: '/ai/permission/current',
    method: 'POST',
    data: {}
  });
}

function querySkillList(data) {
  return request({
    url: '/skill/list',
    method: 'POST',
    data
  });
}

function createAgentSession(data) {
  return request({
    url: '/agent/session/create',
    method: 'POST',
    data
  });
}

function queryAgentSessions(data) {
  return request({
    url: '/agent/session/list',
    method: 'POST',
    data
  });
}

function queryAgentMessages(sessionId) {
  return request({ url: `/agent/session/${sessionId}/messages` });
}

function sendAgentQuestion(data) {
  return request({
    url: '/agent/chat',
    method: 'POST',
    data
  });
}

function queryCurrentAttendanceLocation() {
  return request({ url: '/attendance/current-location' });
}

function checkIn(data) {
  return request({
    url: '/attendance/check-in',
    method: 'POST',
    data
  });
}

function queryAttendanceList(data) {
  return request({
    url: '/attendance/query',
    method: 'POST',
    data
  });
}

function queryWeeklyWorkList(data) {
  return request({
    url: '/weekly-work/query',
    method: 'POST',
    data
  });
}

function queryWeeklyWorkDetail(id) {
  return request({ url: `/weekly-work/detail/${id}` });
}

function saveWeeklyWorkDraft(data) {
  return request({
    url: '/weekly-work/save-draft',
    method: 'POST',
    data
  });
}

function submitWeeklyWork(data) {
  return request({
    url: '/weekly-work/submit',
    method: 'POST',
    data
  });
}

module.exports = {
  wechatMiniLogin,
  queryCurrentUser,
  queryCurrentUserModulePermissions,
  queryCurrentAiPermission,
  querySkillList,
  createAgentSession,
  queryAgentSessions,
  queryAgentMessages,
  sendAgentQuestion,
  queryCurrentAttendanceLocation,
  checkIn,
  queryAttendanceList,
  queryWeeklyWorkList,
  queryWeeklyWorkDetail,
  saveWeeklyWorkDraft,
  submitWeeklyWork
};
