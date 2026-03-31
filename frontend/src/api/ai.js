import request from '@/utils/request'

export function queryAiProviderList(data) {
  return request.post('/ai/provider/list', data)
}

export function saveAiProvider(data) {
  return request.post('/ai/provider/save', data)
}

export function toggleAiProviderStatus(data) {
  return request.post('/ai/provider/toggle-status', data)
}

export function testAiProvider(data) {
  return request.post('/ai/provider/test', data)
}

export function queryUserAiPermissionList(data) {
  return request.post('/ai/permission/user-ai/list', data)
}

export function saveUserAiPermission(data) {
  return request.post('/ai/permission/user-ai/save', data)
}

export function queryUserKnowledgePermissionList(data) {
  return request.post('/ai/permission/user-knowledge/list', data)
}

export function queryGrantableKnowledgeUsers(baseId) {
  return request.get(`/ai/permission/user-knowledge/grantable-users/${baseId}`)
}

export function saveUserKnowledgePermission(data) {
  return request.post('/ai/permission/user-knowledge/save', data)
}

export function queryUserSkillPermissionList(data) {
  return request.post('/ai/permission/user-skill/list', data)
}

export function saveUserSkillPermission(data) {
  return request.post('/ai/permission/user-skill/save', data)
}

export function queryCurrentAiPermission() {
  return request.post('/ai/permission/current')
}
