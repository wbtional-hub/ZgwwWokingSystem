import request from '@/utils/request'

export function queryCurrentAttendanceLocationApi() {
  return request.get('/attendance/current-location')
}

export function checkInApi(data) {
  return request.post('/attendance/check-in', data)
}

export function queryAttendanceListApi(data) {
  return request.post('/attendance/query', data)
}

export function queryAttendanceSummaryApi(data) {
  return request.post('/attendance/summary', data)
}

export function queryAttendanceAbnormalMonitorApi(data) {
  return request.post('/attendance/abnormal-monitor', data)
}

export function queryAttendanceAbnormalTrendApi(data) {
  return request.post('/attendance/abnormal-trend', data)
}

export function queryAttendanceAbnormalUserSummaryApi(data) {
  return request.post('/attendance/abnormal-user-summary', data)
}

export function saveAttendanceApi(data) {
  return request.post('/attendance/save', data)
}

export function deleteAttendanceApi(id) {
  return request.delete(`/attendance/${id}`)
}

export function submitAttendancePatchApplyApi(data) {
  return request.post('/attendance/patch-apply/submit', data)
}

export function queryMyAttendancePatchApplyPageApi(data) {
  return request.post('/attendance/patch-apply/my-page', data)
}

export function queryPendingAttendancePatchApplyPageApi(data) {
  return request.post('/attendance/patch-apply/pending-page', data)
}

export function queryAttendancePatchApplyDetailApi(id) {
  return request.get(`/attendance/patch-apply/${id}`)
}

export function approveAttendancePatchApplyApi(id, data) {
  return request.post(`/attendance/patch-apply/${id}/approve`, data)
}

export function rejectAttendancePatchApplyApi(id, data) {
  return request.post(`/attendance/patch-apply/${id}/reject`, data)
}

export function queryCurrentAttendanceRuleApi() {
  return request.get('/attendance/rule/current')
}

export function saveAttendanceRuleApi(data) {
  return request.post('/attendance/rule/save', data)
}

export function queryAttendanceTeamStatisticsApi(data) {
  return request.post('/attendance/team-statistics/query', data)
}
