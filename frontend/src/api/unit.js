import request from '@/utils/request'

export function queryUnitList() {
  return request.get('/unit/list')
}

export function createUnit(data) {
  return request.post('/unit/create', data)
}

export function updateUnit(data) {
  return request.post('/unit/update', data)
}

export function toggleUnitStatus(data) {
  return request.post('/unit/toggle-status', data)
}

export function deleteUnit(id) {
  return request.delete(`/unit/${id}`)
}
