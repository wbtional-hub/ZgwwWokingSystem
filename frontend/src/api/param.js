import request from '@/utils/request'

export function queryParamList(data) {
  return request.post('/param/list', data)
}

export function saveParam(data) {
  return request.post('/param/save', data)
}

export function deleteParam(id) {
  return request.delete(`/param/${id}`)
}

export function toggleParamStatus(data) {
  return request.post('/param/toggle-status', data)
}
