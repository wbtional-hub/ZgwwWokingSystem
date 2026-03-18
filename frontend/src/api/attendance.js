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
