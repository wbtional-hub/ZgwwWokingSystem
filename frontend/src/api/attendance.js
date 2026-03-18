import request from '@/utils/request'

export function checkInApi(data) {
  return request.post('/attendance/check-in', data)
}

export function queryAttendanceListApi(data) {
  return request.post('/attendance/query', data)
}

export function saveAttendanceApi(data) {
  return request.post('/attendance/save', data)
}

export function deleteAttendanceApi(id) {
  return request.delete(`/attendance/${id}`)
}
