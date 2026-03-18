import request from '@/utils/request'

export function queryOrgTree() {
  return request.get('/orgtree/tree')
}

export function queryOrgChildren(userId) {
  return request.get(`/orgtree/children/${userId}`)
}

export function queryOrgAncestors(userId) {
  return request.get(`/orgtree/ancestors/${userId}`)
}

export function createChildUser(data) {
  return request.post('/orgtree/create', data)
}

export function moveOrgNode(data) {
  return request.post('/orgtree/move', data)
}

export function updateOrgNode(data) {
  return request.post('/orgtree/update', data)
}

export function deleteOrgNode(userId) {
  return request.delete(`/orgtree/${userId}`)
}

export function toggleOrgNodeStatus(data) {
  return request.post('/orgtree/toggle-status', data)
}

export function queryUnitOptions() {
  return request.get('/unit/list')
}
